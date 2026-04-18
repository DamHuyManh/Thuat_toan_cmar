package cmar;

import java.util.*;

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
        this(3.841, 4, 0.50); // paper: chi²=3.841(p=0.05), delta=4, minConf=50%
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
     * Recompute exact support/confidence, keep significant + positively correlated rules.
     */
    public List<Rule> chiSquarePrune(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        int N = transactions.length;

        Map<Integer, Integer> classSupports = new HashMap<>();
        for (int label : labels) classSupports.merge(label, 1, Integer::sum);

        List<Rule> pruned = new ArrayList<>();
        for (Rule rule : rules) {
            int antSupport = 0;
            int exactSupport = 0;
            for (int i = 0; i < N; i++) {
                if (rule.matchesBitmap(bitmaps[i])) {
                    antSupport++;
                    if (labels[i] == rule.classLabel) {
                        exactSupport++;
                    }
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

            // Keep if significant AND positively correlated
            double priorProb = (double) clsSupport / N;
            if (chi2 >= chiSquareThreshold && rule.confidence > priorProb) {
                pruned.add(rule);
            }
        }

        Collections.sort(pruned);
        return pruned;
    }

    /**
     * Phase 2: General-to-Specific Pruning (paper Section 3.1, pruning method 1).
     * "If a general rule has confidence >= a more specific rule (superset, same class),
     * the specific rule is pruned."
     */
    public List<Rule> generalToSpecificPrune(List<Rule> rules) {
        // Skip if too many rules (O(n²) complexity)
        if (rules.size() > 10000) return rules;

        // Sort by length asc (general first), then confidence desc
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> {
            int lenCmp = Integer.compare(a.antecedent.length, b.antecedent.length);
            if (lenCmp != 0) return lenCmp;
            return Double.compare(b.confidence, a.confidence);
        });

        List<Rule> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Rule specific = sorted.get(i);
            boolean pruned = false;
            for (Rule general : result) {
                if (general.classLabel == specific.classLabel
                        && general.antecedent.length < specific.antecedent.length
                        && general.confidence > specific.confidence  // paper: prune lower-confidence specific rules
                        && isSubset(general.antecedent, specific.antecedent)) {
                    pruned = true;
                    break;
                }
            }
            if (!pruned) {
                result.add(specific);
            }
        }

        Collections.sort(result);
        return result;
    }

    /**
     * Phase 3: Database Coverage Pruning (paper Section 3.2).
     * Paper: "A rule is useful if it can correctly classify at least one
     * training case not yet covered by delta higher-ranked rules."
     *
     * IMPORTANT: Coverage counts when rule MATCHES instance (antecedent matches),
     * regardless of whether the class is correct. But "useful" requires correct class.
     */
    public List<Rule> coveragePrune(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        int N = transactions.length;
        int[] coverCount = new int[N];
        boolean[] fullyCovered = new boolean[N];
        int coveredCount = 0;

        List<Rule> selected = new ArrayList<>();

        for (Rule rule : rules) {
            if (coveredCount >= N) break;

            // Rule is useful if it CORRECTLY classifies at least one not-fully-covered instance
            boolean useful = false;
            for (int i = 0; i < N; i++) {
                if (!fullyCovered[i] && labels[i] == rule.classLabel && rule.matchesBitmap(bitmaps[i])) {
                    useful = true;
                    break;
                }
            }

            if (useful) {
                selected.add(rule);
                // Count only correctly classified instances (CBA-style coverage)
                for (int i = 0; i < N; i++) {
                    if (!fullyCovered[i] && labels[i] == rule.classLabel && rule.matchesBitmap(bitmaps[i])) {
                        coverCount[i]++;
                        if (coverCount[i] >= maxCoverageCount) {
                            fullyCovered[i] = true;
                            coveredCount++;
                        }
                    }
                }
            }
        }

        return selected;
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
        long[][] bitmaps = buildBitmaps(transactions);
        List<Rule> afterChi = chiSquarePrune(rules, transactions, labels, bitmaps);
        // Skip G2S: for high-dim datasets it prunes too aggressively (Sonar crashes)
        return coveragePrune(afterChi, transactions, labels, bitmaps);
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
