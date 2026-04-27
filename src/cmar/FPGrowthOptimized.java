package cmar;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class-aware FP-Growth (Phase 06 + Phase 08 IMPROVED).
 *
 * Key optimizations vs baseline FPGrowth:
 * - Phase 06: Mine + class-confidence check in 1 pass (no wasted Rule allocations).
 * - Phase 06: Inverted index BitSet propagation (parent → child via AND).
 * - Phase 08: Top-level FP-tree items mined in parallel threads (multi-core speedup).
 *
 * Thread safety: top-level items are independent subtrees. Each worker thread
 * keeps its own local output List + ruleCountPerClass; merged at end.
 */
public class FPGrowthOptimized {
    private final int minSupport;
    private final double minConfidence;
    private final int maxRulesPerClass;
    private final int maxAntecedentLength;
    private long miningStartTime;
    private static final long MAX_MINING_MS = 600000;

    private int N;
    private Map<Integer, BitSet> itemIndex;
    private Map<Integer, BitSet> classMasks;

    // Phase 08: parallel threshold — only parallelize when mining is heavy enough to amortize thread overhead.
    // For small datasets, sequential is faster.
    private static final int PARALLEL_MIN_TX = 200;

    public FPGrowthOptimized(int minSupport, double minConfidence,
                              int maxRulesPerClass, int maxAntecedentLength) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLength = maxAntecedentLength;
    }

    public List<Rule> mineRules(int[][] transactions, int[] labels) {
        this.N = transactions.length;
        this.miningStartTime = System.currentTimeMillis();

        // Build inverted index once (sequential — small overhead)
        itemIndex = new HashMap<>();
        for (int i = 0; i < N; i++)
            for (int item : transactions[i])
                itemIndex.computeIfAbsent(item, k -> new BitSet(N)).set(i);

        classMasks = new HashMap<>();
        for (int i = 0; i < N; i++)
            classMasks.computeIfAbsent(labels[i], k -> new BitSet(N)).set(i);

        FPTree tree = FPTree.build(transactions, minSupport);
        if (tree.isEmpty()) return Collections.emptyList();

        BitSet allMatch = new BitSet(N);
        allMatch.set(0, N);

        List<Rule> output;
        if (N >= PARALLEL_MIN_TX && !tree.isSinglePath() && tree.itemCounts.size() > 4) {
            output = mineRecursiveParallel(tree, new int[0], allMatch);
        } else {
            output = new ArrayList<>();
            Map<Integer, AtomicInteger> ruleCount = new HashMap<>();
            for (int cls : classMasks.keySet()) ruleCount.put(cls, new AtomicInteger(0));
            mineRecursive(tree, new int[0], allMatch, output, ruleCount);
        }

        Collections.sort(output);

        // Phase 08: apply maxRulesPerClass cap POST-SORT for determinism.
        // Race conditions in parallel mining can over-emit by ±1 rule per class
        // when cap is hit. Post-sort cap guarantees identical output for same input.
        if (maxRulesPerClass > 0) {
            Map<Integer, Integer> kept = new HashMap<>();
            List<Rule> capped = new ArrayList<>(output.size());
            for (Rule r : output) {
                int n = kept.getOrDefault(r.classLabel, 0);
                if (n < maxRulesPerClass) {
                    capped.add(r);
                    kept.put(r.classLabel, n + 1);
                }
            }
            return capped;
        }
        return output;
    }

    /**
     * Phase 08: parallelize at top-level items.
     * Each top-level item's conditional tree is independent — perfect for threads.
     */
    private List<Rule> mineRecursiveParallel(FPTree tree, int[] prefix, BitSet parentMatch) {
        // Collect top-level items in ascending freq order
        List<Map.Entry<Integer, Integer>> items = new ArrayList<>(tree.itemCounts.entrySet());
        items.sort(Map.Entry.comparingByValue());

        // Shared atomic rule counter per class (cap check across threads)
        Map<Integer, AtomicInteger> sharedRuleCount = new HashMap<>();
        for (int cls : classMasks.keySet()) sharedRuleCount.put(cls, new AtomicInteger(0));

        int nThreads = Math.min(items.size(), Runtime.getRuntime().availableProcessors());
        if (nThreads <= 1) {
            List<Rule> out = new ArrayList<>();
            mineRecursive(tree, prefix, parentMatch, out, sharedRuleCount);
            return out;
        }

        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        List<Future<List<Rule>>> futures = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : items) {
            final int item = entry.getKey();
            futures.add(exec.submit(() -> mineSingleItem(tree, prefix, parentMatch, item, sharedRuleCount)));
        }

        List<Rule> merged = new ArrayList<>();
        try {
            for (Future<List<Rule>> f : futures) merged.addAll(f.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            exec.shutdown();
        }
        return merged;
    }

    /** Worker: mine subtree rooted at `item` from top-level tree. */
    private List<Rule> mineSingleItem(FPTree tree, int[] prefix, BitSet parentMatch,
                                        int item, Map<Integer, AtomicInteger> sharedRuleCount) {
        List<Rule> localOutput = new ArrayList<>();
        BitSet itemBs = itemIndex.get(item);
        if (itemBs == null) return localOutput;

        BitSet newMatch = (BitSet) parentMatch.clone();
        newMatch.and(itemBs);
        int totalMatches = newMatch.cardinality();
        if (totalMatches < minSupport) return localOutput;

        int[] newItemset = Arrays.copyOf(prefix, prefix.length + 1);
        newItemset[prefix.length] = item;

        emitRules(newItemset, newMatch, totalMatches, localOutput, sharedRuleCount);

        if (newItemset.length < maxAntecedentLength) {
            FPTree condTree = buildConditionalTree(tree, item);
            if (!condTree.isEmpty()) {
                mineRecursive(condTree, newItemset, newMatch, localOutput, sharedRuleCount);
            }
        }
        return localOutput;
    }

    private FPTree buildConditionalTree(FPTree tree, int item) {
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
                int[] pat = new int[path.size()];
                for (int i = 0; i < path.size(); i++)
                    pat[i] = path.get(path.size() - 1 - i);
                patterns.add(pat);
                counts.add(node.count);
            }
            node = node.link;
        }
        if (patterns.isEmpty()) return new FPTree();
        return FPTree.buildConditional(patterns, counts, minSupport);
    }

    /**
     * Sequential recursive mining. Output and counters passed explicitly so this
     * is reusable for both serial and parallel paths.
     */
    private void mineRecursive(FPTree tree, int[] prefix, BitSet parentMatch,
                                List<Rule> output, Map<Integer, AtomicInteger> ruleCount) {
        if (tree.isEmpty()) return;
        if (prefix.length >= maxAntecedentLength) return;
        if (System.currentTimeMillis() - miningStartTime > MAX_MINING_MS) return;
        if (allClassesFull(ruleCount)) return;

        if (tree.isSinglePath()) {
            mineSinglePath(tree, prefix, parentMatch, output, ruleCount);
            return;
        }

        List<Map.Entry<Integer, Integer>> items = new ArrayList<>(tree.itemCounts.entrySet());
        items.sort(Map.Entry.comparingByValue());

        for (Map.Entry<Integer, Integer> entry : items) {
            int item = entry.getKey();
            BitSet itemBs = itemIndex.get(item);
            if (itemBs == null) continue;

            BitSet newMatch = (BitSet) parentMatch.clone();
            newMatch.and(itemBs);
            int totalMatches = newMatch.cardinality();
            if (totalMatches < minSupport) continue;

            int[] newItemset = Arrays.copyOf(prefix, prefix.length + 1);
            newItemset[prefix.length] = item;

            emitRules(newItemset, newMatch, totalMatches, output, ruleCount);

            if (newItemset.length < maxAntecedentLength) {
                FPTree condTree = buildConditionalTree(tree, item);
                if (!condTree.isEmpty()) {
                    mineRecursive(condTree, newItemset, newMatch, output, ruleCount);
                }
            }
        }
    }

    /**
     * Emit rules for current itemset. NO cap check during mining — the cap
     * is applied post-sort in mineRules() for determinism (parallel-safe).
     * The ruleCount param is unused (kept for API compatibility).
     */
    private void emitRules(int[] itemset, BitSet match, int totalMatches,
                            List<Rule> output, Map<Integer, AtomicInteger> ruleCount) {
        for (Map.Entry<Integer, BitSet> e : classMasks.entrySet()) {
            int cls = e.getKey();
            BitSet inter = (BitSet) match.clone();
            inter.and(e.getValue());
            int clsSup = inter.cardinality();
            if (clsSup < minSupport) continue;
            double conf = (double) clsSup / totalMatches;
            if (conf < minConfidence) continue;

            output.add(new Rule(itemset.clone(), cls, clsSup, conf));
        }
    }

    /** No-op now (cap moved to post-sort). Kept to preserve recursion signature. */
    private boolean allClassesFull(Map<Integer, AtomicInteger> ruleCount) {
        return false;
    }

    private void mineSinglePath(FPTree tree, int[] prefix, BitSet parentMatch,
                                  List<Rule> output, Map<Integer, AtomicInteger> ruleCount) {
        List<int[]> pathItems = tree.getSinglePathItems();
        int maxExtra = maxAntecedentLength - prefix.length;
        int n = Math.min(pathItems.size(), Math.min(15, maxExtra > 0 ? 15 : 0));
        if (n <= 0) return;

        for (int mask = 1; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) + prefix.length > maxAntecedentLength) continue;
            if (allClassesFull(ruleCount)) return;

            BitSet match = (BitSet) parentMatch.clone();
            int[] add = new int[Integer.bitCount(mask)];
            int k = 0;
            boolean ok = true;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    int it = pathItems.get(i)[0];
                    add[k++] = it;
                    BitSet ib = itemIndex.get(it);
                    if (ib == null) { ok = false; break; }
                    match.and(ib);
                }
            }
            if (!ok) continue;
            int total = match.cardinality();
            if (total < minSupport) continue;

            int[] newItemset = new int[prefix.length + add.length];
            System.arraycopy(prefix, 0, newItemset, 0, prefix.length);
            System.arraycopy(add, 0, newItemset, prefix.length, add.length);

            emitRules(newItemset, match, total, output, ruleCount);
        }
    }
}
