package cmar;

import java.util.*;
import cmar.util.PhaseTimer;

/**
 * CMAR Classifier - Classification based on Multiple Association Rules.
 * Paper-aligned implementation (Li, Han, Pei 2001).
 *
 * Classification: highest-confidence-first + weighted chi-square group voting.
 * Weight = chi² (raw chi-square value, paper Section 4).
 */
public class CMARClassifier {
    private int minSupport;
    private double minConfidence;
    private double chiSquareThreshold;
    private int maxCoverageCount;
    private int maxRulesPerClass;
    private int maxAntecedentLength;
    // Top-k global matched rules voting. 0 = use all matched rules (paper-faithful).
    private int topKGlobal;
    // Hybrid: when true, use HM as voting weight (but keep CMAR sort for pruning).
    public static boolean useHMWeightOnly = false;
    // Alternative: use Lift as voting weight (WEviRC-inspired).
    // Lift>1 = positive correlation; stronger correlation → larger vote.
    public static boolean useLiftWeight = false;
    // Composite weight = χ² × Lift  (statistical significance × correlation strength)
    public static boolean useChiLiftWeight = false;
    // Composite weight = confidence × Lift  (predictive accuracy × correlation)
    public static boolean useConfLiftWeight = false;
    // CPAR-style: average weights per class (instead of sum) — fair across class sizes.
    public static boolean useAvgVote = false;
    // CPAR-style: take top-k of EACH class's matched rules (instead of global top-k).
    // 0 = disabled (use topKGlobal).
    public static int perClassTopK = 0;

    private CRTree crTree;
    private int defaultClass;
    private int maxItem;
    /** Words in instance / rule bitmaps: (maxItem >> 6) + 1 */
    private int bitmapWords;
    private boolean fitted;

    /** Phase 16: reuse scratch bitmap in predict() to cut allocation churn. */
    private static final ThreadLocal<long[]> TL_INSTANCE_BITMAP = new ThreadLocal<>();

    private int totalRulesMined;
    private int totalRulesAfterPrune;
    private long trainingTimeMs;

    public CMARClassifier() {
        // Paper defaults: minSup=1%, minConf=50%, chi²=3.841(p=0.05), delta=4
        this(2, 0.50, 3.841, 4, 80000, 6, 0);
    }

    public CMARClassifier(int minSupport, double minConfidence,
                          double chiSquareThreshold, int maxCoverageCount,
                          int maxRulesPerClass) {
        this(minSupport, minConfidence, chiSquareThreshold, maxCoverageCount, maxRulesPerClass, 6, 0);
    }

    public CMARClassifier(int minSupport, double minConfidence,
                          double chiSquareThreshold, int maxCoverageCount,
                          int maxRulesPerClass, int maxAntecedentLength) {
        this(minSupport, minConfidence, chiSquareThreshold, maxCoverageCount, maxRulesPerClass, maxAntecedentLength, 0);
    }

    public CMARClassifier(int minSupport, double minConfidence,
                          double chiSquareThreshold, int maxCoverageCount,
                          int maxRulesPerClass, int maxAntecedentLength,
                          int topKGlobal) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.chiSquareThreshold = chiSquareThreshold;
        this.maxCoverageCount = maxCoverageCount;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLength = maxAntecedentLength;
        this.topKGlobal = Math.max(0, topKGlobal);
        this.fitted = false;
    }

    public void fit(int[][] transactions, int[] labels) {
        long start = System.nanoTime();

        maxItem = 0;
        for (int[] txn : transactions)
            for (int item : txn) maxItem = Math.max(maxItem, item);
        bitmapWords = (maxItem >> 6) + 1;

        Map<Integer, Integer> classCounts = new HashMap<>();
        for (int label : labels) classCounts.merge(label, 1, Integer::sum);
        defaultClass = classCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        // Share class frequencies for dominant-class sort tie-breaker
        Rule.CLASS_FREQS = classCounts;
        Rule.TOTAL_N = transactions.length;

        // Phase 1: Mine rules using FP-Growth (paper Section 2)
        // Phase 06 IMPROVED: class-aware mining (drop useless itemsets early)
        PhaseTimer.start("mining");
        List<Rule> rules;
        Map<Integer, BitSet> sharedItemIndex = null;
        if (cmar.util.OptimizationProfile.isImproved()) {
            FPGrowthOptimized miner = new FPGrowthOptimized(minSupport, minConfidence, maxRulesPerClass, maxAntecedentLength);
            rules = miner.mineRules(transactions, labels);
            sharedItemIndex = miner.getItemIndex();
        } else {
            FPGrowth miner = new FPGrowth(minSupport, minConfidence, maxRulesPerClass, maxAntecedentLength);
            rules = miner.mineRules(transactions, labels);
        }
        totalRulesMined = rules.size();
        PhaseTimer.stop("mining");

        // Phase 2: Prune (paper Section 3)
        PhaseTimer.start("pruning");
        RulePruner pruner = new RulePruner(chiSquareThreshold, maxCoverageCount, minConfidence);
        List<Rule> prunedRules = pruner.prune(rules, transactions, labels, sharedItemIndex);
        totalRulesAfterPrune = prunedRules.size();
        PhaseTimer.stop("pruning");

        // Phase 16: one antecedent bitmap per rule sized to training item universe — fast CR-Tree match
        for (Rule rule : prunedRules) {
            rule.ensureAntBitmap(maxItem);
        }

        // Paper Section 4: weight = chi²/max_chi² (normalized)
        // Hybrid: in HM modes weight = HM (WCBA-style F1 voting).
        int N = transactions.length;
        for (Rule rule : prunedRules) {
            if (useChiLiftWeight) {
                // χ² × Lift — combine statistical significance and correlation magnitude
                double chiNorm = computeNormalizedChiSquare(rule, N, classCounts);
                rule.weight = chiNorm * rule.lift;
            } else if (useConfLiftWeight) {
                // confidence × Lift — combine predictive accuracy with correlation strength
                rule.weight = rule.confidence * rule.lift;
            } else if (useLiftWeight) {
                rule.weight = rule.lift;
            } else if (Rule.useHMLift || useHMWeightOnly) {
                rule.weight = rule.hm;
            } else {
                rule.weight = computeNormalizedChiSquare(rule, N, classCounts);
            }
        }

        // Phase 3: Index in CR-tree
        PhaseTimer.start("indexing");
        crTree = new CRTree();
        crTree.build(prunedRules);
        PhaseTimer.stop("indexing");

        trainingTimeMs = (System.nanoTime() - start) / 1_000_000;
        fitted = true;
    }

    /**
     * CMAR classification (Li, Han, Pei 2001, Section 4):
     * 1. If highest-confidence rules all predict same class -> that class
     * 2. Otherwise, weighted chi-square group voting:
     *    sum chi² of matched rules per class, class with highest sum wins
     *
     * Phase 15 (optional): top-K global voting — only sum weights of top-K rules
     * in CMAR sort order. Set topKGlobal=0 to preserve paper-faithful behavior.
     */
    public int predict(int[] instance) {
        if (!fitted) throw new IllegalStateException("Not fitted");

        long[] bitmap = borrowInstanceBitmap(instance);
        List<Rule> allMatched = crTree.findAllMatching(bitmap);
        if (allMatched.isEmpty()) return defaultClass;

        // Sort by CMAR ordering: conf desc, sup desc, len asc
        Collections.sort(allMatched);

        // Step 1: If highest-priority rules all predict the same class, use it.
        // Priority = HM in hybrid mode, confidence in CMAR-standard mode.
        double bestPrio = Rule.useHMLift ? allMatched.get(0).hm : allMatched.get(0).confidence;
        int unanimousClass = allMatched.get(0).classLabel;
        for (Rule r : allMatched) {
            double prio = Rule.useHMLift ? r.hm : r.confidence;
            if (prio < bestPrio - 1e-9) break;
            if (r.classLabel != unanimousClass) {
                unanimousClass = Integer.MIN_VALUE;
                break;
            }
        }
        if (unanimousClass != Integer.MIN_VALUE) {
            return unanimousClass;
        }

        // Step 2: Weighted group voting (paper Section 4 + CPAR-style extensions)
        Map<Integer, Double> classScores = new HashMap<>();
        Map<Integer, Integer> classCounts = new HashMap<>();

        if (perClassTopK > 0) {
            // CPAR-style: keep top-k rules per class, then sum/avg those
            Map<Integer, Integer> perClassSeen = new HashMap<>();
            for (Rule r : allMatched) {
                int seen = perClassSeen.getOrDefault(r.classLabel, 0);
                if (seen >= perClassTopK) continue;
                classScores.merge(r.classLabel, r.weight, Double::sum);
                classCounts.merge(r.classLabel, 1, Integer::sum);
                perClassSeen.put(r.classLabel, seen + 1);
            }
        } else {
            int limit = allMatched.size();
            if (topKGlobal > 0) limit = Math.min(limit, topKGlobal);
            for (int i = 0; i < limit; i++) {
                Rule r = allMatched.get(i);
                classScores.merge(r.classLabel, r.weight, Double::sum);
                classCounts.merge(r.classLabel, 1, Integer::sum);
            }
        }
        if (useAvgVote) {
            for (Map.Entry<Integer, Integer> e : classCounts.entrySet()) {
                int cnt = e.getValue();
                if (cnt > 0) {
                    Double sum = classScores.get(e.getKey());
                    if (sum != null) classScores.put(e.getKey(), sum / cnt);
                }
            }
        }

        int bestClass = defaultClass;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : classScores.entrySet()) {
            double s = e.getValue();
            if (s > bestScore) {
                bestScore = s;
                bestClass = e.getKey();
            }
        }
        return bestClass;
    }

    public int[] predict(int[][] instances) {
        int[] predictions = new int[instances.length];
        for (int i = 0; i < instances.length; i++) {
            predictions[i] = predict(instances[i]);
        }
        return predictions;
    }

    public double score(int[][] testData, int[] testLabels) {
        int[] predictions = predict(testData);
        int correct = 0;
        for (int i = 0; i < testLabels.length; i++) {
            if (predictions[i] == testLabels[i]) correct++;
        }
        return (double) correct / testLabels.length;
    }

    /**
     * Phase 16: ThreadLocal scratch — same dimensions as training bitmaps; cleared per predict.
     */
    private long[] borrowInstanceBitmap(int[] items) {
        int w = bitmapWords;
        long[] b = TL_INSTANCE_BITMAP.get();
        if (b == null || b.length < w) {
            b = new long[w];
            TL_INSTANCE_BITMAP.set(b);
        } else {
            Arrays.fill(b, 0, w, 0L);
        }
        for (int item : items) {
            if (item >= 0 && item <= maxItem) {
                b[item >> 6] |= (1L << (item & 63));
            }
        }
        return b;
    }

    /**
     * Paper's Weighted Chi-Square (WCS), Section 4:
     *   weight(r) = (chi²)² / max_chi²
     * where max_chi² is the upper bound of chi² for rule r.
     * Summing these weights per class gives the WCS voting score.
     */
    private double computeNormalizedChiSquare(Rule rule, int N, Map<Integer, Integer> classCounts) {
        if (rule.chiSquare <= 0 || N <= 0) return 0;
        int supP = rule.antecedentSupport;
        int supC = classCounts.getOrDefault(rule.classLabel, 0);
        if (supP <= 0 || supC <= 0) return rule.chiSquare;

        double expected = (double) supP * supC / N;
        double maxDev = Math.min(supP, supC) - expected;
        if (maxDev <= 0) return rule.chiSquare;

        int rowB = N - supP, colB = N - supC;
        if (rowB <= 0 || colB <= 0) return rule.chiSquare;

        double e = (double) N * (1.0 / (supP * supC) + 1.0 / (supP * colB)
                + 1.0 / (rowB * supC) + 1.0 / (rowB * colB));
        double maxChi2 = maxDev * maxDev * e;
        if (maxChi2 <= 0) return rule.chiSquare;

        return (rule.chiSquare) / maxChi2;
    }

    // --- Stats ---
    public int getTotalRulesMined() { return totalRulesMined; }
    public int getTotalRulesAfterPrune() { return totalRulesAfterPrune; }
    public long getTrainingTimeMs() { return trainingTimeMs; }
    public int getRuleCount() { return crTree != null ? crTree.size() : 0; }
    public int getDefaultClass() { return defaultClass; }
    public List<Rule> getRules() { return crTree != null ? crTree.getAllRules() : Collections.emptyList(); }

    public void printStats() {
        System.out.println("=== CMAR Stats ===");
        System.out.println("Rules mined:        " + totalRulesMined);
        System.out.println("Rules after prune:  " + totalRulesAfterPrune);
        System.out.println("Training time:      " + trainingTimeMs + " ms");
        System.out.println("Default class:      " + defaultClass);
    }

    public int getTopKGlobal() { return topKGlobal; }

    @Override
    public String toString() {
        return "CMARClassifier{rules=" + (crTree != null ? crTree.size() : 0)
                + ", minSup=" + minSupport + ", minConf=" + minConfidence + "}";
    }
}
