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

        // Build bitmaps for fast per-class support counting
        int maxItem = 0;
        for (int[] txn : transactions)
            for (int item : txn) maxItem = Math.max(maxItem, item);

        int words = (maxItem >> 6) + 1;
        long[][] bitmaps = new long[N][words];
        for (int i = 0; i < N; i++)
            for (int item : transactions[i])
                bitmaps[i][item >> 6] |= (1L << (item & 63));

        // Step 1: Mine frequent itemsets via FP-Growth
        // IMPORTANT: Use minSupport consistently for main tree AND all conditional trees
        FPTree tree = FPTree.build(transactions, minSupport);
        if (tree.isEmpty()) return Collections.emptyList();

        List<int[]> frequentItemsets = new ArrayList<>();
        miningStartTime = System.currentTimeMillis();
        mineItemsets(tree, new int[0], frequentItemsets);

        // Step 2: Generate class association rules
        // Per-class relative minSupport: rare classes use lower threshold
        Map<Integer, Integer> classTotals = new HashMap<>();
        for (int label : labels) classTotals.merge(label, 1, Integer::sum);

        List<Rule> rules = new ArrayList<>();
        Map<Integer, Integer> ruleCountPerClass = new HashMap<>();

        for (int[] itemset : frequentItemsets) {
            if (itemset.length == 0) continue;

            // Count per-class support by scanning bitmaps
            Map<Integer, Integer> classSupport = new HashMap<>();
            int totalMatches = 0;

            for (int i = 0; i < N; i++) {
                if (matchesBitmap(itemset, bitmaps[i], words)) {
                    totalMatches++;
                    classSupport.merge(labels[i], 1, Integer::sum);
                }
            }

            if (totalMatches == 0) continue;

            // Create rule for each class with sufficient support and confidence
            for (Map.Entry<Integer, Integer> entry : classSupport.entrySet()) {
                int classLabel = entry.getKey();
                int clsSup = entry.getValue();
                double conf = (double) clsSup / totalMatches;

                // Keep support threshold consistent with paper-style setup.
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

    private static boolean matchesBitmap(int[] itemset, long[] bitmap, int words) {
        for (int item : itemset) {
            int idx = item >> 6;
            if (idx >= words || (bitmap[idx] & (1L << (item & 63))) == 0)
                return false;
        }
        return true;
    }
}
