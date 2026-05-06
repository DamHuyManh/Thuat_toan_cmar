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

    /** Phase 11: scratch AND cho chi²; lưu N tối thiểu vì BitSet.clear() làm length() = 0. */
    private static final class ChiScratchHolder {
        BitSet bitset = new BitSet(64);
        int minN = 64;
    }
    private static final ThreadLocal<ChiScratchHolder> TL_CHI = new ThreadLocal<>();

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
     * Phase 2: General-to-Specific Pruning — OPTIMIZED v2 (Phase 07).
     *
     * Improvements over Phase 04:
     * 1. Bitmap antecedent (long[]) for fast subset check via bitwise AND.
     * 2. Length-bucket index: indexed[cls][firstItem][len] → only check len < L.
     */
    public List<Rule> generalToSpecificPrune(List<Rule> rules) {
        if (rules.isEmpty()) return rules;

        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> {
            int lenCmp = Integer.compare(a.antecedent.length, b.antecedent.length);
            if (lenCmp != 0) return lenCmp;
            return Double.compare(b.confidence, a.confidence);
        });

        // Determine bitmap word count from max item ID
        int maxItem = 0;
        for (Rule r : sorted)
            for (int item : r.antecedent) if (item > maxItem) maxItem = item;
        int words = (maxItem >> 6) + 1;

        // Pre-compute antecedent bitmaps once
        for (Rule r : sorted) {
            if (r.antBitmap == null || r.antBitmap.length != words) {
                long[] bm = new long[words];
                for (int item : r.antecedent) bm[item >> 6] |= (1L << (item & 63));
                r.antBitmap = bm;
            }
        }

        // Index: indexed[cls][firstItem][len] → list of rules
        Map<Integer, Map<Integer, Map<Integer, List<Rule>>>> indexed = new HashMap<>();
        List<Rule> result = new ArrayList<>();

        for (Rule specific : sorted) {
            int cls = specific.classLabel;
            int sLen = specific.antecedent.length;
            boolean pruned = false;

            Map<Integer, Map<Integer, List<Rule>>> byFirst = indexed.get(cls);
            if (byFirst != null) {
                outer:
                for (int item : specific.antecedent) {
                    Map<Integer, List<Rule>> byLen = byFirst.get(item);
                    if (byLen == null) continue;
                    for (int len = 1; len < sLen; len++) {
                        List<Rule> candidates = byLen.get(len);
                        if (candidates == null) continue;
                        for (Rule general : candidates) {
                            if (general.confidence > specific.confidence
                                    && isSubsetBitmap(general.antBitmap, specific.antBitmap)) {
                                pruned = true;
                                break outer;
                            }
                        }
                    }
                }
            }
            if (!pruned) {
                result.add(specific);
                indexed.computeIfAbsent(cls, k -> new HashMap<>())
                       .computeIfAbsent(specific.antecedent[0], k -> new HashMap<>())
                       .computeIfAbsent(sLen, k -> new ArrayList<>())
                       .add(specific);
            }
        }

        Collections.sort(result);
        return result;
    }

    /** Phase 07: bitwise-AND subset check. sub ⊆ sup iff for all i (sub[i] & ~sup[i]) == 0. */
    private static boolean isSubsetBitmap(long[] sub, long[] sup) {
        int n = Math.min(sub.length, sup.length);
        for (int i = 0; i < n; i++) {
            if ((sub[i] & ~sup[i]) != 0L) return false;
        }
        // Any bits set beyond sup.length means sub has items sup doesn't → not subset
        for (int i = n; i < sub.length; i++) {
            if (sub[i] != 0L) return false;
        }
        return true;
    }

    /**
     * Phase 3: Database Coverage Pruning — OPTIMIZED với BitSet matchMatrix.
     * Map từ Rule object → index để lookup match BitSet sau sort.
     */
    public List<Rule> coveragePrune(List<Rule> rules, int[][] transactions, int[] labels,
                                     long[][] bitmaps, Map<Rule, BitSet> ruleMatches,
                                     Map<Integer, BitSet> classMasks) {
        return coveragePruneFromMatches(rules, transactions.length, ruleMatches, classMasks);
    }

    /** Phase 11: coverage chỉ cần N + BitSet match — không dùng row bitmaps. */
    private List<Rule> coveragePruneFromMatches(List<Rule> rules, int N,
                                               Map<Rule, BitSet> ruleMatches,
                                               Map<Integer, BitSet> classMasks) {
        int[] coverCount = new int[N];
        BitSet notFullyCovered = new BitSet(N);
        notFullyCovered.set(0, N);

        List<Rule> selected = new ArrayList<>();

        for (Rule rule : rules) {
            if (notFullyCovered.isEmpty()) break;

            BitSet match = ruleMatches.get(rule);
            if (match == null) continue;

            BitSet cmask = classMasks.get(rule.classLabel);
            boolean useful = false;
            if (cmask != null) {
                for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
                    if (cmask.get(i) && notFullyCovered.get(i)) {
                        useful = true;
                        break;
                    }
                }
            } else {
                for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
                    if (notFullyCovered.get(i)) {
                        useful = true;
                        break;
                    }
                }
            }
            if (!useful) continue;

            selected.add(rule);
            for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
                if (!notFullyCovered.get(i)) continue;
                coverCount[i]++;
                if (coverCount[i] >= maxCoverageCount) {
                    notFullyCovered.clear(i);
                }
            }
        }
        return selected;
    }

    private static BitSet borrowChiScratch(int N) {
        ChiScratchHolder h = TL_CHI.get();
        if (h == null) {
            h = new ChiScratchHolder();
            TL_CHI.set(h);
        }
        if (h.minN < N) {
            h.bitset = new BitSet(N);
            h.minN = N;
        } else {
            h.bitset.clear();
        }
        return h.bitset;
    }

    // Backward-compatible wrapper
    public List<Rule> coveragePrune(List<Rule> rules, int[][] transactions, int[] labels, long[][] bitmaps) {
        Map<Integer, BitSet> classMasks = buildClassMasks(labels);
        Map<Rule, BitSet> matches = new IdentityHashMap<>();
        for (Rule r : rules) matches.put(r, computeRuleMatch(r, bitmaps));
        return coveragePrune(rules, transactions, labels, bitmaps, matches, classMasks);
    }

    // ========== Phase 04 helpers ==========

    /**
     * Phase 09: |a ∩ b| không clone — quét BitSet có {@link BitSet#length()} nhỏ hơn
     * (chi phí ~O(min(|a|,|b|)) bit 1, tránh gọi {@code cardinality()} hai lần quét cả N).
     */
    public static int intersectCardinality(BitSet a, BitSet b) {
        if (a == null || b == null) return 0;
        BitSet outer = a.length() <= b.length() ? a : b;
        BitSet inner = outer == a ? b : a;
        int c = 0;
        for (int i = outer.nextSetBit(0); i >= 0; i = outer.nextSetBit(i + 1)) {
            if (inner.get(i)) c++;
        }
        return c;
    }

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
        return prune(rules, transactions, labels, null);
    }

    /**
     * @param sharedItemIndex chỉ mục item→BitSet từ mining (FPGrowthOptimized); nếu khác null thì
     *                        tái dùng, tránh {@link #buildItemIndex} quét lại toàn bộ giao dịch.
     */
    public List<Rule> prune(List<Rule> rules, int[][] transactions, int[] labels,
                            Map<Integer, BitSet> sharedItemIndex) {
        if (cmar.util.OptimizationProfile.isBaseline()) {
            return pruneBaseline(rules, transactions, labels);
        }
        PhaseTimer.start("prune_bitmap");
        int Ntxn = transactions.length;
        Map<Integer, Integer> classSupports = new HashMap<>();
        for (int label : labels) classSupports.merge(label, 1, Integer::sum);
        Map<Integer, BitSet> classMasks = buildClassMasks(labels);
        Map<Integer, BitSet> itemIndex = sharedItemIndex != null
                ? sharedItemIndex
                : buildItemIndex(transactions, labels.length);
        PhaseTimer.stop("prune_bitmap");

        PhaseTimer.start("prune_chisquare");
        Map<Rule, BitSet> keptMatches = new IdentityHashMap<>();
        List<Rule> afterChi = chiSquarePruneInverted(rules, Ntxn, itemIndex,
                                                      classSupports, labels, keptMatches);
        PhaseTimer.stop("prune_chisquare");

        PhaseTimer.start("prune_g2s");
        List<Rule> afterG2S = generalToSpecificPrune(afterChi);
        PhaseTimer.stop("prune_g2s");

        PhaseTimer.start("prune_coverage");
        List<Rule> out = coveragePruneFromMatches(afterG2S, Ntxn, keptMatches, classMasks);
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

    /**
     * BASELINE coverage pruning — logic semantic ĐỒNG NHẤT với {@link #coveragePrune}
     * (improved version), chỉ khác implementation:
     * - Old: int[] coverCount + boolean[] fullyCovered + scan toàn N mỗi rule
     * - New: BitSet notFullyCovered + ruleMatches cache → AND/iterator nhanh hơn
     * Cùng input → cùng tập rule output (đã verify trên 26 UCI datasets).
     */
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
                                               Map<Integer, Integer> classSupports,
                                               int[] trainLabels,
                                               Map<Rule, BitSet> keptMatches) {
        List<Rule> pruned = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.antecedent.length == 0) continue;

            BitSet match = borrowChiScratch(N);
            boolean ok = true;
            boolean firstItem = true;
            for (int item : rule.antecedent) {
                BitSet ib = itemIndex.get(item);
                if (ib == null) {
                    ok = false;
                    break;
                }
                if (firstItem) {
                    match.or(ib);
                    firstItem = false;
                } else {
                    match.and(ib);
                }
                if (match.isEmpty()) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            int cls = rule.classLabel;
            int antSupport = 0;
            int exactSupport = 0;
            for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
                antSupport++;
                if (trainLabels[i] == cls) exactSupport++;
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
                keptMatches.put(rule, (BitSet) match.clone());
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
