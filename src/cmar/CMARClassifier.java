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

    private CRTree crTree;
    private int defaultClass;
    private int maxItem;
    private boolean fitted;

    private int totalRulesMined;
    private int totalRulesAfterPrune;
    private long trainingTimeMs;

    public CMARClassifier() {
        // Paper defaults: minSup=1%, minConf=50%, chi²=3.841(p=0.05), delta=4
        this(2, 0.50, 3.841, 4, 80000, 6);
    }

    public CMARClassifier(int minSupport, double minConfidence,
                          double chiSquareThreshold, int maxCoverageCount,
                          int maxRulesPerClass) {
        this(minSupport, minConfidence, chiSquareThreshold, maxCoverageCount, maxRulesPerClass, 6);
    }

    public CMARClassifier(int minSupport, double minConfidence,
                          double chiSquareThreshold, int maxCoverageCount,
                          int maxRulesPerClass, int maxAntecedentLength) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.chiSquareThreshold = chiSquareThreshold;
        this.maxCoverageCount = maxCoverageCount;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLength = maxAntecedentLength;
        this.fitted = false;
    }

    public void fit(int[][] transactions, int[] labels) {
        long start = System.nanoTime();

        maxItem = 0;
        for (int[] txn : transactions)
            for (int item : txn) maxItem = Math.max(maxItem, item);

        Map<Integer, Integer> classCounts = new HashMap<>();
        for (int label : labels) classCounts.merge(label, 1, Integer::sum);
        defaultClass = classCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();

        // Phase 1: Mine rules using FP-Growth (paper Section 2)
        PhaseTimer.start("mining");
        FPGrowth miner = new FPGrowth(minSupport, minConfidence, maxRulesPerClass, maxAntecedentLength);
        List<Rule> rules = miner.mineRules(transactions, labels);
        totalRulesMined = rules.size();
        PhaseTimer.stop("mining");

        // Phase 2: Prune (paper Section 3)
        PhaseTimer.start("pruning");
        RulePruner pruner = new RulePruner(chiSquareThreshold, maxCoverageCount, minConfidence);
        List<Rule> prunedRules = pruner.prune(rules, transactions, labels);
        totalRulesAfterPrune = prunedRules.size();
        PhaseTimer.stop("pruning");

        // Paper Section 4: weight = chi²/max_chi² (normalized)
        int N = transactions.length;
        for (Rule rule : prunedRules) {
            rule.weight = computeNormalizedChiSquare(rule, N, classCounts);
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
     *    sum chi² of ALL rules per class, class with highest sum wins
     */
    public int predict(int[] instance) {
        if (!fitted) throw new IllegalStateException("Not fitted");

        long[] bitmap = toBitmap(instance);
        List<Rule> allMatched = crTree.findAllMatching(bitmap);
        if (allMatched.isEmpty()) return defaultClass;

        // Sort by CMAR ordering: conf desc, sup desc, len asc
        Collections.sort(allMatched);

        // Step 1: If highest-confidence rules agree on one class, use it
        double bestConf = allMatched.get(0).confidence;
        Set<Integer> topClasses = new HashSet<>();
        for (Rule r : allMatched) {
            if (r.confidence < bestConf - 1e-9) break;
            topClasses.add(r.classLabel);
        }
        if (topClasses.size() == 1) {
            return topClasses.iterator().next();
        }

        // Step 2: Weighted chi-square group voting (paper Section 4)
        // Sum chi² of ALL rules in each class group
        Map<Integer, Double> classScores = new HashMap<>();
        for (Rule r : allMatched) {
            classScores.merge(r.classLabel, r.weight, Double::sum);
        }

        return classScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(defaultClass);
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

    private long[] toBitmap(int[] items) {
        int words = (maxItem >> 6) + 1;
        long[] bitmap = new long[words];
        for (int item : items) {
            if (item <= maxItem) {
                bitmap[item >> 6] |= (1L << (item & 63));
            }
        }
        return bitmap;
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

    @Override
    public String toString() {
        return "CMARClassifier{rules=" + (crTree != null ? crTree.size() : 0)
                + ", minSup=" + minSupport + ", minConf=" + minConfidence + "}";
    }
}
