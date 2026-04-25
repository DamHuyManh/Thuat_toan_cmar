package cmar;

import java.util.*;

/**
 * FP-Growth for Class Association Rule mining (CMAR paper-aligned).
 * Uses consistent minSupport threshold throughout FP-tree mining.
 * Two-phase: mine frequent itemsets, then generate class rules via bitmap scan.
 */
public class FPGrowth {
    private int minSupport;
    private double minConfidence;
    private int maxRulesPerClass;
    private int maxAntecedentLength;
    private static final int MAX_ITEMSETS = 5000000; // safety cap
    private long miningStartTime;
    private static final long MAX_MINING_MS = 600000; // 10 phút

    public FPGrowth(int minSupport, double minConfidence, int maxRulesPerClass) {
        this(minSupport, minConfidence, maxRulesPerClass, 4);
    }

    public FPGrowth(int minSupport, double minConfidence, int maxRulesPerClass, int maxAntecedentLength) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLength = maxAntecedentLength;
    }

    /**
     * Mine class association rules from dataset.
     */
    public List<Rule> mineRules(int[][] transactions, int[] labels) {
        int N = transactions.length;

        // Step 1: Mine frequent itemsets via FP-Growth
        FPTree tree = FPTree.build(transactions, minSupport);
        if (tree.isEmpty()) return Collections.emptyList();

        List<int[]> frequentItemsets = new ArrayList<>();
        miningStartTime = System.currentTimeMillis();
        mineItemsets(tree, new int[0], frequentItemsets);

        if (cmar.util.OptimizationProfile.isBaseline()) {
            return generateRulesBaseline(frequentItemsets, transactions, labels, N);
        }

        // Phase 03 OPTIMIZATION: Build inverted index for fast itemset support counting.
        // itemIndex[item] = BitSet of txns containing item. Per-class masks too.
        Map<Integer, BitSet> itemIndex = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int item : transactions[i]) {
                itemIndex.computeIfAbsent(item, k -> new BitSet(N)).set(i);
            }
        }
        Map<Integer, BitSet> classMasks = new HashMap<>();
        for (int i = 0; i < N; i++) {
            classMasks.computeIfAbsent(labels[i], k -> new BitSet(N)).set(i);
        }

        // Step 2: Generate class association rules via BitSet AND
        List<Rule> rules = new ArrayList<>();
        Map<Integer, Integer> ruleCountPerClass = new HashMap<>();

        for (int[] itemset : frequentItemsets) {
            if (itemset.length == 0) continue;

            // itemset support = AND of item BitSets — O(k * N/64)
            BitSet match = null;
            boolean ok = true;
            for (int item : itemset) {
                BitSet ib = itemIndex.get(item);
                if (ib == null) { ok = false; break; }
                if (match == null) match = (BitSet) ib.clone();
                else match.and(ib);
                if (match.isEmpty()) { ok = false; break; }
            }
            if (!ok || match == null) continue;
            int totalMatches = match.cardinality();
            if (totalMatches == 0) continue;

            // Per-class support = |match AND classMask|
            for (Map.Entry<Integer, BitSet> e : classMasks.entrySet()) {
                int classLabel = e.getKey();
                BitSet inter = (BitSet) match.clone();
                inter.and(e.getValue());
                int clsSup = inter.cardinality();
                if (clsSup == 0) continue;
                double conf = (double) clsSup / totalMatches;

                if (clsSup >= minSupport && conf >= minConfidence) {
                    int count = ruleCountPerClass.getOrDefault(classLabel, 0);
                    if (maxRulesPerClass > 0 && count >= maxRulesPerClass) continue;

                    Rule rule = new Rule(itemset.clone(), classLabel, clsSup, conf);
                    rules.add(rule);
                    ruleCountPerClass.merge(classLabel, 1, Integer::sum);
                }
            }
        }

        Collections.sort(rules);
        return rules;
    }

    /**
     * Mine all frequent itemsets from FP-tree.
     */
    /** BASELINE path: generate rules via per-itemset linear bitmap scan (original CMAR). */
    private List<Rule> generateRulesBaseline(List<int[]> frequentItemsets,
                                              int[][] transactions, int[] labels, int N) {
        int maxItem = 0;
        for (int[] txn : transactions)
            for (int item : txn) maxItem = Math.max(maxItem, item);
        int words = (maxItem >> 6) + 1;
        long[][] bitmaps = new long[N][words];
        for (int i = 0; i < N; i++)
            for (int item : transactions[i])
                bitmaps[i][item >> 6] |= (1L << (item & 63));

        List<Rule> rules = new ArrayList<>();
        Map<Integer, Integer> ruleCountPerClass = new HashMap<>();
        for (int[] itemset : frequentItemsets) {
            if (itemset.length == 0) continue;
            Map<Integer, Integer> classSupport = new HashMap<>();
            int totalMatches = 0;
            for (int i = 0; i < N; i++) {
                boolean ok = true;
                for (int item : itemset) {
                    int idx = item >> 6;
                    if (idx >= words || (bitmaps[i][idx] & (1L << (item & 63))) == 0) { ok = false; break; }
                }
                if (ok) {
                    totalMatches++;
                    classSupport.merge(labels[i], 1, Integer::sum);
                }
            }
            if (totalMatches == 0) continue;
            for (Map.Entry<Integer, Integer> e : classSupport.entrySet()) {
                int classLabel = e.getKey();
                int clsSup = e.getValue();
                double conf = (double) clsSup / totalMatches;
                if (clsSup >= minSupport && conf >= minConfidence) {
                    int count = ruleCountPerClass.getOrDefault(classLabel, 0);
                    if (maxRulesPerClass > 0 && count >= maxRulesPerClass) continue;
                    rules.add(new Rule(itemset.clone(), classLabel, clsSup, conf));
                    ruleCountPerClass.merge(classLabel, 1, Integer::sum);
                }
            }
        }
        Collections.sort(rules);
        return rules;
    }

    private void mineItemsets(FPTree tree, int[] prefix, List<int[]> itemsets) {
        if (tree.isEmpty()) return;
        if (prefix.length >= maxAntecedentLength) return;
        if (itemsets.size() >= MAX_ITEMSETS) return;
        if (System.currentTimeMillis() - miningStartTime > MAX_MINING_MS) return;

        if (tree.isSinglePath()) {
            mineSinglePathItemsets(tree, prefix, itemsets);
            return;
        }

        List<Map.Entry<Integer, Integer>> items = new ArrayList<>(tree.itemCounts.entrySet());
        items.sort(Map.Entry.comparingByValue());

        for (Map.Entry<Integer, Integer> entry : items) {
            int item = entry.getKey();

            int[] newItemset = Arrays.copyOf(prefix, prefix.length + 1);
            newItemset[prefix.length] = item;
            itemsets.add(newItemset);

            // Build conditional pattern base
            List<int[]> patterns = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            FPNode node = tree.headerTable.get(item);

            while (node != null) {
                List<Integer> path = new ArrayList<>();
                FPNode p = node.parent;
                while (p != null && !p.isRoot()) {
                    path.add(p.item);
                    p = p.parent;
                }
                if (!path.isEmpty()) {
                    int[] pattern = new int[path.size()];
                    for (int i = 0; i < path.size(); i++)
                        pattern[i] = path.get(path.size() - 1 - i);
                    patterns.add(pattern);
                    counts.add(node.count);
                }
                node = node.link;
            }

            if (!patterns.isEmpty()) {
                // Use same minSupport for conditional trees (FP-Growth correctness requirement)
                FPTree condTree = FPTree.buildConditional(patterns, counts, minSupport);
                if (!condTree.isEmpty()) {
                    mineItemsets(condTree, newItemset, itemsets);
                }
            }
        }
    }

    private void mineSinglePathItemsets(FPTree tree, int[] prefix, List<int[]> itemsets) {
        List<int[]> pathItems = tree.getSinglePathItems();
        int maxExtra = maxAntecedentLength - prefix.length;
        int n = Math.min(pathItems.size(), Math.min(15, maxExtra > 0 ? 15 : 0));
        if (n <= 0) return;

        for (int mask = 1; mask < (1 << n); mask++) {
            if (itemsets.size() >= MAX_ITEMSETS) return;
            if (Integer.bitCount(mask) + prefix.length > maxAntecedentLength) continue;

            List<Integer> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(pathItems.get(i)[0]);
                }
            }

            int[] newItemset = new int[prefix.length + subset.size()];
            System.arraycopy(prefix, 0, newItemset, 0, prefix.length);
            for (int i = 0; i < subset.size(); i++)
                newItemset[prefix.length + i] = subset.get(i);
            itemsets.add(newItemset);
        }
    }

}
