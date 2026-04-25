package cmar;

import java.util.*;
import cmar.util.PhaseTimer;

/**
 * Rule pruning (Li, Han, Pei 2001, Section 3):
 * 1. Chi-Square Pruning (CSP): remove insignificant/negatively correlated rules
 * 2. General-to-Specific Pruning: remove redundant specific rules
 * 3. Database Coverage Pruning (DCP): keep rules covering uncovered instances
 *
 * Paper defaults: chi²=3.841 (p=0.05), delta=3
 */
public class RulePruner {
    private double chiSquareThreshold;
    private int maxCoverageCount; // delta in paper (default 3)
    private double minConfidence;

    public RulePruner(double chiSquareThreshold, int maxCoverageCount, double minConfidence) {
        this.chiSquareThreshold = chiSquareThreshold;
        this.maxCoverageCount = maxCoverageCount;
        this.minConfidence = minConfidence;
    }

    public RulePruner() {
        this(3.841, 4, 0.50); // delta=4 cho accuracy tốt hơn
    }

    /**
     * Compute chi-square statistic for a rule using 2x2 contingency table.
     */
    public static double computeChiSquare(int ruleSupport, int antecedentSupport,
                                          int classSupport, int totalTransactions) {
        int N = totalTransactions;
        double a = ruleSupport;
        double b = antecedentSupport - ruleSupport;
        double c = classSupport - ruleSupport;
        double d = N - antecedentSupport - classSupport + ruleSupport;

        double rowA = a + b;
        double rowB = c + d;
        double colA = a + c;
        double colB = b + d;

        if (rowA == 0 || rowB == 0 || colA == 0 || colB == 0) return 0;

        double ad_bc = a * d - b * c;
        return (N * ad_bc * ad_bc) / (rowA * rowB * colA * colB);
    }

    /**
     * Phase 1: Chi-Square Pruning (paper Section 3.1).
     * Phase 04 OPTIMIZATION: dùng BitSet matchMatrix pre-computed (shared với coveragePrune).
     */
    public List<Rule> chiSquarePrune(List<Rule> rules, int[][] transactions, int[] labels,
                                     long[][] bitmaps, BitSet[] matchMatrix,
                                     Map<Integer, BitSet> classMasks, Map<Integer, Integer> classSupports) {
        int N = transactions.length;

        List<Rule> pruned = new ArrayList<>();
        for (int r = 0; r < rules.size(); r++) {
            Rule rule = rules.get(r);
            BitSet match = matchMatrix[r];
            int antSupport = match.cardinality();
            if (antSupport == 0) continue;

            // exactSupport = |match AND classMask[rule.class]|
            BitSet cmask = classMasks.get(rule.classLabel);
            int exactSupport = 0;
            if (cmask != null) {
                // Avoid cloning: iterate set bits of smaller set
                BitSet tmp = (BitSet) match.clone();
                tmp.and(cmask);
                exactSupport = tmp.cardinality();
            }

            rule.support = exactSupport;
            rule.antecedentSupport = antSupport;
            rule.confidence = (double) exactSupport / antSupport;

            if (rule.confidence < minConfidence) continue;

            int clsSupport = classSupports.getOrDefault(rule.classLabel, 0);
            double chi2 = computeChiSquare(exactSupport, antSupport, clsSupport, N);
            rule.chiSquare = chi2;

            double priorProb = (double) clsSupport / N;
            if (chi2 >= chiSquareThreshold && rule.confidence > priorProb) {
                pruned.add(rule);
            }
        }

        Collections.sort(pruned);
        return pruned;
    }

    // Backward-compatible wrapper (for callers that still pass old signature)
    public List<Rule> chiSquarePrune(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        Map<Integer, Integer> classSupports = new HashMap<>();
        for (int label : labels) classSupports.merge(label, 1, Integer::sum);
        Map<Integer, BitSet> classMasks = buildClassMasks(labels);
        BitSet[] matchMatrix = precomputeMatchMatrix(rules, bitmaps);
        return chiSquarePrune(rules, transactions, labels, bitmaps, matchMatrix, classMasks, classSupports);
    }

    /**
     * Phase 2: General-to-Specific Pruning — OPTIMIZED.
     * Phase 04: partition theo class + first-item để giảm outer scan.
     * Không còn skip khi rules>10K.
     */
    public List<Rule> generalToSpecificPrune(List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> {
            int lenCmp = Integer.compare(a.antecedent.length, b.antecedent.length);
            if (lenCmp != 0) return lenCmp;
            return Double.compare(b.confidence, a.confidence);
        });

        // Index kept rules by (class, first-item) để giảm subset check
        Map<Integer, Map<Integer, List<Rule>>> indexed = new HashMap<>();
        List<Rule> result = new ArrayList<>();

        for (Rule specific : sorted) {
            boolean pruned = false;
            int cls = specific.classLabel;
            Map<Integer, List<Rule>> byFirst = indexed.get(cls);
            if (byFirst != null) {
                // General rule phải có first-item thuộc specific's antecedent
                for (int item : specific.antecedent) {
                    List<Rule> candidates = byFirst.get(item);
                    if (candidates == null) continue;
                    for (Rule general : candidates) {
                        if (general.antecedent.length < specific.antecedent.length
                                && general.confidence > specific.confidence
                                && isSubset(general.antecedent, specific.antecedent)) {
                            pruned = true;
                            break;
                        }
                    }
                    if (pruned) break;
                }
            }
            if (!pruned) {
                result.add(specific);
                indexed.computeIfAbsent(cls, k -> new HashMap<>())
                       .computeIfAbsent(specific.antecedent[0], k -> new ArrayList<>())
                       .add(specific);
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Phase 3: Database Coverage Pruning — OPTIMIZED với BitSet matchMatrix.
     * Map từ Rule object → index để lookup match BitSet sau sort.
     */
    public List<Rule> coveragePrune(List<Rule> rules, int[][] transactions, int[] labels,
                                     long[][] bitmaps, Map<Rule, BitSet> ruleMatches,
                                     Map<Integer, BitSet> classMasks) {
        int N = transactions.length;
        int[] coverCount = new int[N];
        BitSet notFullyCovered = new BitSet(N);
        notFullyCovered.set(0, N);

        List<Rule> selected = new ArrayList<>();

        for (Rule rule : rules) {
            if (notFullyCovered.isEmpty()) break;

            BitSet match = ruleMatches.get(rule);
            if (match == null) continue;

            // useful = match AND classMask AND notFullyCovered non-empty
            BitSet usefulSet = (BitSet) match.clone();
            BitSet cmask = classMasks.get(rule.classLabel);
            if (cmask != null) usefulSet.and(cmask);
            usefulSet.and(notFullyCovered);
            if (usefulSet.isEmpty()) continue;

            selected.add(rule);
            // Increment coverCount cho mọi match chưa fully covered
            BitSet activeMatch = (BitSet) match.clone();
            activeMatch.and(notFullyCovered);
            for (int i = activeMatch.nextSetBit(0); i >= 0; i = activeMatch.nextSetBit(i + 1)) {
                coverCount[i]++;
                if (coverCount[i] >= maxCoverageCount) {
                    notFullyCovered.clear(i);
                }
            }
        }
        return selected;
    }

    // Backward-compatible wrapper
    public List<Rule> coveragePrune(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        Map<Integer, BitSet> classMasks = buildClassMasks(labels);
        Map<Rule, BitSet> matches = new IdentityHashMap<>();
        for (Rule r : rules) matches.put(r, computeRuleMatch(r, bitmaps));
        return coveragePrune(rules, transactions, labels, bitmaps, matches, classMasks);
    }

    // ========== Phase 04 helpers ==========

    /** Pre-compute BitSet matching mỗi rule → transactions. O(rules × N × words). */
    public static BitSet[] precomputeMatchMatrix(List<Rule> rules, long[][] bitmaps) {
        int N = bitmaps.length;
        BitSet[] out = new BitSet[rules.size()];
        for (int r = 0; r < rules.size(); r++) {
            BitSet bs = new BitSet(N);
            Rule rule = rules.get(r);
            for (int i = 0; i < N; i++) {
                if (rule.matchesBitmap(bitmaps[i])) bs.set(i);
            }
            out[r] = bs;
        }
        return out;
    }

    public static BitSet computeRuleMatch(Rule rule, long[][] bitmaps) {
        BitSet bs = new BitSet(bitmaps.length);
        for (int i = 0; i < bitmaps.length; i++) {
            if (rule.matchesBitmap(bitmaps[i])) bs.set(i);
        }
        return bs;
    }

    public static Map<Integer, BitSet> buildClassMasks(int[] labels) {
        Map<Integer, BitSet> out = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            out.computeIfAbsent(labels[i], k -> new BitSet(labels.length)).set(i);
        }
        return out;
    }

    private boolean isSubset(int[] sub, int[] sup) {
        int j = 0;
        for (int item : sub) {
            while (j < sup.length && sup[j] < item) j++;
            if (j >= sup.length || sup[j] != item) return false;
            j++;
        }
        return true;
    }

    /**
     * Full pruning pipeline (paper Section 3):
     * 1. Chi-square pruning
     * 2. General-to-specific pruning
     * 3. Database coverage pruning
     */
    public List<Rule> prune(List<Rule> rules, int[][] transactions, int[] labels) {
        if (cmar.util.OptimizationProfile.isBaseline()) {
            return pruneBaseline(rules, transactions, labels);
        }
        PhaseTimer.start("prune_bitmap");
        long[][] bitmaps = buildBitmaps(transactions);
        Map<Integer, Integer> classSupports = new HashMap<>();
        for (int label : labels) classSupports.merge(label, 1, Integer::sum);
        Map<Integer, BitSet> classMasks = buildClassMasks(labels);
        Map<Integer, BitSet> itemIndex = buildItemIndex(transactions, labels.length);
        PhaseTimer.stop("prune_bitmap");

        PhaseTimer.start("prune_chisquare");
        Map<Rule, BitSet> keptMatches = new IdentityHashMap<>();
        List<Rule> afterChi = chiSquarePruneInverted(rules, bitmaps.length, itemIndex,
                                                      classMasks, classSupports, keptMatches);
        PhaseTimer.stop("prune_chisquare");

        PhaseTimer.start("prune_g2s");
        List<Rule> afterG2S = generalToSpecificPrune(afterChi);
        PhaseTimer.stop("prune_g2s");

        PhaseTimer.start("prune_coverage");
        List<Rule> out = coveragePrune(afterG2S, transactions, labels, bitmaps,
                                        keptMatches, classMasks);
        PhaseTimer.stop("prune_coverage");
        return out;
    }

    /** BASELINE pruning path: original chi² + G2S + coverage (no BitSet opt). */
    public List<Rule> pruneBaseline(List<Rule> rules, int[][] transactions, int[] labels) {
        PhaseTimer.start("prune_bitmap");
        long[][] bitmaps = buildBitmaps(transactions);
        PhaseTimer.stop("prune_bitmap");
        PhaseTimer.start("prune_chisquare");
        List<Rule> afterChi = chiSquarePruneOld(rules, transactions, labels, bitmaps);
        PhaseTimer.stop("prune_chisquare");
        PhaseTimer.start("prune_g2s");
        List<Rule> afterG2S = generalToSpecificPruneOld(afterChi);
        PhaseTimer.stop("prune_g2s");
        PhaseTimer.start("prune_coverage");
        List<Rule> out = coveragePruneOld(afterG2S, transactions, labels, bitmaps);
        PhaseTimer.stop("prune_coverage");
        return out;
    }

    private List<Rule> chiSquarePruneOld(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        int N = transactions.length;
        Map<Integer, Integer> classSupports = new HashMap<>();
        for (int label : labels) classSupports.merge(label, 1, Integer::sum);
        List<Rule> pruned = new ArrayList<>();
        for (Rule rule : rules) {
            int antSupport = 0, exactSupport = 0;
            for (int i = 0; i < N; i++) {
                if (rule.matchesBitmap(bitmaps[i])) {
                    antSupport++;
                    if (labels[i] == rule.classLabel) exactSupport++;
                }
            }
            if (antSupport == 0) continue;
            rule.support = exactSupport;
            rule.antecedentSupport = antSupport;
            rule.confidence = (double) exactSupport / antSupport;
            if (rule.confidence < minConfidence) continue;
            int clsSupport = classSupports.getOrDefault(rule.classLabel, 0);
            double chi2 = computeChiSquare(exactSupport, antSupport, clsSupport, N);
            rule.chiSquare = chi2;
            double priorProb = (double) clsSupport / N;
            if (chi2 >= chiSquareThreshold && rule.confidence > priorProb) pruned.add(rule);
        }
        Collections.sort(pruned);
        return pruned;
    }

    private List<Rule> generalToSpecificPruneOld(List<Rule> rules) {
        if (rules.size() > 10000) return rules;
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> {
            int lenCmp = Integer.compare(a.antecedent.length, b.antecedent.length);
            if (lenCmp != 0) return lenCmp;
            return Double.compare(b.confidence, a.confidence);
        });
        List<Rule> result = new ArrayList<>();
        for (Rule specific : sorted) {
            boolean pruned = false;
            for (Rule general : result) {
                if (general.classLabel == specific.classLabel
                        && general.antecedent.length < specific.antecedent.length
                        && general.confidence > specific.confidence
                        && isSubset(general.antecedent, specific.antecedent)) {
                    pruned = true; break;
                }
            }
            if (!pruned) result.add(specific);
        }
        Collections.sort(result);
        return result;
    }

    private List<Rule> coveragePruneOld(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        int N = transactions.length;
        int[] coverCount = new int[N];
        boolean[] fullyCovered = new boolean[N];
        int coveredCount = 0;
        List<Rule> selected = new ArrayList<>();
        for (Rule rule : rules) {
            if (coveredCount >= N) break;
            boolean useful = false;
            for (int i = 0; i < N; i++) {
                if (!fullyCovered[i] && labels[i] == rule.classLabel && rule.matchesBitmap(bitmaps[i])) {
                    useful = true; break;
                }
            }
            if (useful) {
                selected.add(rule);
                for (int i = 0; i < N; i++) {
                    if (!fullyCovered[i] && rule.matchesBitmap(bitmaps[i])) {
                        coverCount[i]++;
                        if (coverCount[i] >= maxCoverageCount) {
                            fullyCovered[i] = true; coveredCount++;
                        }
                    }
                }
            }
        }
        return selected;
    }

    /** Build inverted index: item → BitSet of transactions containing item. */
    public static Map<Integer, BitSet> buildItemIndex(int[][] transactions, int N) {
        Map<Integer, BitSet> idx = new HashMap<>();
        for (int i = 0; i < transactions.length; i++) {
            for (int item : transactions[i]) {
                idx.computeIfAbsent(item, k -> new BitSet(N)).set(i);
            }
        }
        return idx;
    }

    /**
     * Phase 04: Chi² prune with inverted index.
     * Rule match = AND of item BitSets — O(k * N/64) per rule.
     */
    private List<Rule> chiSquarePruneInverted(List<Rule> rules, int N,
                                               Map<Integer, BitSet> itemIndex,
                                               Map<Integer, BitSet> classMasks,
                                               Map<Integer, Integer> classSupports,
                                               Map<Rule, BitSet> keptMatches) {
        List<Rule> pruned = new ArrayList<>();
        for (Rule rule : rules) {
            // Compute match as AND of item BitSets
            BitSet match = null;
            boolean ok = true;
            for (int item : rule.antecedent) {
                BitSet ib = itemIndex.get(item);
                if (ib == null) { ok = false; break; }
                if (match == null) match = (BitSet) ib.clone();
                else match.and(ib);
                if (match.isEmpty()) { ok = false; break; }
            }
            if (!ok || match == null) continue;
            int antSupport = match.cardinality();
            if (antSupport == 0) continue;

            BitSet cmask = classMasks.get(rule.classLabel);
            int exactSupport = 0;
            if (cmask != null) {
                BitSet exact = (BitSet) match.clone();
                exact.and(cmask);
                exactSupport = exact.cardinality();
            }

            rule.support = exactSupport;
            rule.antecedentSupport = antSupport;
            rule.confidence = (double) exactSupport / antSupport;
            if (rule.confidence < minConfidence) continue;

            int clsSupport = classSupports.getOrDefault(rule.classLabel, 0);
            double chi2 = computeChiSquare(exactSupport, antSupport, clsSupport, N);
            rule.chiSquare = chi2;

            double priorProb = (double) clsSupport / N;
            if (chi2 >= chiSquareThreshold && rule.confidence > priorProb) {
                pruned.add(rule);
                keptMatches.put(rule, match); // reuse trong coverage
            }
        }
        Collections.sort(pruned);
        return pruned;
    }

    /** Legacy — giữ lại cho backward compat, không dùng trong pipeline mới. */
    @SuppressWarnings("unused")
    private List<Rule> chiSquarePruneLazy(List<Rule> rules, long[][] bitmaps,
                                           Map<Integer, BitSet> classMasks,
                                           Map<Integer, Integer> classSupports,
                                           Map<Rule, BitSet> keptMatches) {
        int N = bitmaps.length;
        List<Rule> pruned = new ArrayList<>();
        for (Rule rule : rules) {
            int antSupport = 0;
            int exactSupport = 0;
            BitSet cmask = classMasks.get(rule.classLabel);
            // Count first pass — lightweight, no BitSet allocation
            for (int i = 0; i < N; i++) {
                if (rule.matchesBitmap(bitmaps[i])) {
                    antSupport++;
                    if (cmask != null && cmask.get(i)) exactSupport++;
                }
            }
            if (antSupport == 0) continue;

            rule.support = exactSupport;
            rule.antecedentSupport = antSupport;
            rule.confidence = (double) exactSupport / antSupport;
            if (rule.confidence < minConfidence) continue;

            int clsSupport = classSupports.getOrDefault(rule.classLabel, 0);
            double chi2 = computeChiSquare(exactSupport, antSupport, clsSupport, N);
            rule.chiSquare = chi2;

            double priorProb = (double) clsSupport / N;
            if (chi2 >= chiSquareThreshold && rule.confidence > priorProb) {
                pruned.add(rule);
                // Pass 2: build BitSet cho rule đã pass (để coverage dùng lại)
                BitSet match = new BitSet(N);
                for (int i = 0; i < N; i++) {
                    if (rule.matchesBitmap(bitmaps[i])) match.set(i);
                }
                keptMatches.put(rule, match);
            }
        }
        Collections.sort(pruned);
        return pruned;
    }

    private long[][] buildBitmaps(int[][] transactions) {
        int maxItem = 0;
        for (int[] txn : transactions)
            for (int item : txn) maxItem = Math.max(maxItem, item);

        int words = (maxItem >> 6) + 1;
        long[][] bitmaps = new long[transactions.length][words];
        for (int i = 0; i < transactions.length; i++)
            for (int item : transactions[i])
                bitmaps[i][item >> 6] |= (1L << (item & 63));
        return bitmaps;
    }
}
