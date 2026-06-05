package cmar;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

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
 *
 * Phase 17: emitRules chỉ duyệt lớp xuất hiện trong match (dirty histogram);
 * mineSinglePath tái dùng BitSet + path buffer, không tạo list path mỗi lần.
 */
public class FPGrowthOptimized {
    private final int minSupport;
    private final double minConfidence;
    private final int maxRulesPerClass;
    private final int maxAntecedentLength;
    private long miningStartTime;
    private static final long MAX_MINING_MS = 600000;
    private static final int MAX_SINGLE_PATH_ITEMS = 15; // chống combinatorial explosion 2^n

    private int N;
    private Map<Integer, BitSet> itemIndex;
    private Map<Integer, BitSet> classMasks;

    /** Phase 10: nhãn giao dịch; histogram emit dùng ThreadLocal (an toàn với mining song song). */
    private int[] trainLabels;
    private int labelHistSize;
    private static final ThreadLocal<int[]> TL_LABEL_HIST = new ThreadLocal<>();
    private static final ThreadLocal<int[]> TL_LABEL_DIRTY = new ThreadLocal<>();
    // Phase 13: buffer tái dùng để đọc path từ FP-node về root (giảm cấp phát List<Integer>)
    private static final ThreadLocal<int[]> TL_PATH_BUF = new ThreadLocal<>();
    /** Phase 17: BitSet scratch cho mineSinglePath (tránh clone mỗi mask). */
    private static final ThreadLocal<BitSet> TL_SINGLE_PATH_MATCH = new ThreadLocal<>();

    // Phase 08: parallel threshold — only parallelize when mining is heavy enough to amortize thread overhead.
    // For small datasets, sequential is faster.
    private static final int PARALLEL_MIN_TX = 200;

    /** When true, force single-thread mining for byte-reproducible per-dataset results (paper run). */
    public static volatile boolean DETERMINISTIC = false;

    public FPGrowthOptimized(int minSupport, double minConfidence,
                              int maxRulesPerClass, int maxAntecedentLength) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLength = maxAntecedentLength;
    }

    /**
     * Phase 10: chỉ mục item sau mining — trùng semantic với {@link RulePruner#buildItemIndex},
     * cho phép pruning tái dùng, tránh quét lại toàn bộ giao dịch.
     */
    public Map<Integer, BitSet> getItemIndex() {
        return itemIndex;
    }

    public List<Rule> mineRules(int[][] transactions, int[] labels) {
        this.N = transactions.length;
        this.trainLabels = labels;
        this.miningStartTime = System.currentTimeMillis();

        int maxLab = 0;
        for (int L : labels) if (L > maxLab) maxLab = L;
        this.labelHistSize = maxLab + 1;

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
        if (!DETERMINISTIC && N >= PARALLEL_MIN_TX && !tree.isSinglePath() && tree.itemCounts.size() > 4) {
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

        // Phase 10: ForkJoinPool.commonPool — tránh tạo/hủy FixedThreadPool mỗi lần mine (giảm overhead OS).
        ForkJoinPool pool = ForkJoinPool.commonPool();
        List<CompletableFuture<List<Rule>>> futures = new ArrayList<>(items.size());
        for (Map.Entry<Integer, Integer> entry : items) {
            final int item = entry.getKey();
            futures.add(CompletableFuture.supplyAsync(
                    () -> mineSingleItem(tree, prefix, parentMatch, item, sharedRuleCount), pool));
        }
        List<Rule> merged = new ArrayList<>();
        for (CompletableFuture<List<Rule>> f : futures) merged.addAll(f.join());
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
        FPNode node = tree.headerTable.get(item);
        if (node == null) return new FPTree();

        // Pass 1: count weighted frequencies in conditional pattern base
        Map<Integer, Integer> freq = new HashMap<>();
        int[] buf = TL_PATH_BUF.get();
        if (buf == null) { buf = new int[64]; TL_PATH_BUF.set(buf); }

        FPNode cur = node;
        while (cur != null) {
            int cnt = cur.count;
            FPNode p = cur.parent;
            int len = 0;
            while (p != null && !p.isRoot()) {
                if (len >= buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                    TL_PATH_BUF.set(buf);
                }
                buf[len++] = p.item;
                p = p.parent;
            }
            for (int i = 0; i < len; i++) {
                freq.merge(buf[i], cnt, Integer::sum);
            }
            cur = cur.link;
        }

        // Filter infrequent items
        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) return new FPTree();

        // Precompute rank map: higher freq first (desc), tie by item asc
        List<Map.Entry<Integer, Integer>> items = new ArrayList<>(freq.entrySet());
        items.sort((a, b) -> {
            int c = Integer.compare(b.getValue(), a.getValue());
            if (c != 0) return c;
            return Integer.compare(a.getKey(), b.getKey());
        });
        Map<Integer, Integer> rank = new HashMap<>(items.size() * 2);
        for (int i = 0; i < items.size(); i++) rank.put(items.get(i).getKey(), i);

        // Pass 2: insert filtered paths into conditional FP-tree
        FPTree out = new FPTree();
        out.itemCounts = freq;
        cur = node;
        while (cur != null) {
            int cnt = cur.count;
            FPNode p = cur.parent;
            int len = 0;
            while (p != null && !p.isRoot()) {
                int it = p.item;
                if (freq.containsKey(it)) {
                    if (len >= buf.length) {
                        buf = Arrays.copyOf(buf, buf.length * 2);
                        TL_PATH_BUF.set(buf);
                    }
                    buf[len++] = it;
                }
                p = p.parent;
            }
            if (len > 0) {
                // reverse buf[0..len) to get root→leaf order, then sort by rank (freq desc)
                for (int i = 0, j = len - 1; i < j; i++, j--) {
                    int t = buf[i]; buf[i] = buf[j]; buf[j] = t;
                }
                // insertion sort by rank (paths are short)
                for (int i = 1; i < len; i++) {
                    int key = buf[i];
                    int keyRank = rank.getOrDefault(key, Integer.MAX_VALUE);
                    int j = i - 1;
                    while (j >= 0) {
                        int jr = rank.getOrDefault(buf[j], Integer.MAX_VALUE);
                        if (jr > keyRank) {
                            buf[j + 1] = buf[j];
                            j--;
                        } else break;
                    }
                    buf[j + 1] = key;
                }
                // Phase 14: avoid Arrays.copyOf; FPTree can insert prefix directly
                out.insertTransaction(buf, len, cnt);
            }
            cur = cur.link;
        }
        return out;
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
        int[] hist = TL_LABEL_HIST.get();
        if (hist == null || hist.length < labelHistSize) {
            hist = new int[labelHistSize];
            TL_LABEL_HIST.set(hist);
        }
        int[] dirtyBuf = TL_LABEL_DIRTY.get();
        if (dirtyBuf == null || dirtyBuf.length < 64) {
            dirtyBuf = new int[64];
            TL_LABEL_DIRTY.set(dirtyBuf);
        }
        // Phase 10: một vòng quét match → histogram nhãn (trùng intersectCardinality vs classMask).
        int dirtyCount = 0;
        boolean histOverflow = false;
        for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
            int lab = trainLabels[i];
            if (hist[lab] == 0) {
                if (dirtyCount >= dirtyBuf.length) {
                    histOverflow = true;
                    break;
                }
                dirtyBuf[dirtyCount++] = lab;
            }
            hist[lab]++;
        }
        if (histOverflow) {
            Arrays.fill(hist, 0);
            for (int i = match.nextSetBit(0); i >= 0; i = match.nextSetBit(i + 1)) {
                hist[trainLabels[i]]++;
            }
        }
        // Phase 17: chỉ thử các lớp có trong match (dirtyBuf), không quét toàn bộ classMasks
        if (histOverflow) {
            for (int cls = 0; cls < labelHistSize; cls++) {
                int clsSup = hist[cls];
                if (clsSup < minSupport) continue;
                double conf = (double) clsSup / totalMatches;
                if (conf < minConfidence) continue;
                output.add(new Rule(itemset.clone(), cls, clsSup, conf));
            }
        } else {
            for (int j = 0; j < dirtyCount; j++) {
                int cls = dirtyBuf[j];
                int clsSup = hist[cls];
                if (clsSup < minSupport) continue;
                double conf = (double) clsSup / totalMatches;
                if (conf < minConfidence) continue;
                output.add(new Rule(itemset.clone(), cls, clsSup, conf));
            }
        }
        if (!histOverflow) {
            for (int j = 0; j < dirtyCount; j++) {
                hist[dirtyBuf[j]] = 0;
            }
        } else {
            Arrays.fill(hist, 0);
        }
    }

    private void mineSinglePath(FPTree tree, int[] prefix, BitSet parentMatch,
                                  List<Rule> output, Map<Integer, AtomicInteger> ruleCount) {
        int[] buf = TL_PATH_BUF.get();
        if (buf == null) {
            buf = new int[64];
            TL_PATH_BUF.set(buf);
        }
        int pathLen;
        while (true) {
            pathLen = tree.collectSinglePathItemIds(buf);
            if (pathLen <= buf.length) break;
            buf = Arrays.copyOf(buf, pathLen + 16);
            TL_PATH_BUF.set(buf);
        }
        int maxExtra = maxAntecedentLength - prefix.length;
        int n = Math.min(pathLen, Math.min(MAX_SINGLE_PATH_ITEMS, maxExtra > 0 ? MAX_SINGLE_PATH_ITEMS : 0));
        if (n <= 0) return;

        BitSet match = TL_SINGLE_PATH_MATCH.get();
        if (match == null) {
            match = new BitSet(N);
            TL_SINGLE_PATH_MATCH.set(match);
        }

        int[] add = new int[MAX_SINGLE_PATH_ITEMS];

        for (int mask = 1; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) + prefix.length > maxAntecedentLength) continue;

            match.clear();
            match.or(parentMatch);
            int k = 0;
            boolean ok = true;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    int it = buf[i];
                    add[k++] = it;
                    BitSet ib = itemIndex.get(it);
                    if (ib == null) { ok = false; break; }
                    match.and(ib);
                }
            }
            if (!ok) continue;
            int total = match.cardinality();
            if (total < minSupport) continue;

            int[] newItemset = new int[prefix.length + k];
            System.arraycopy(prefix, 0, newItemset, 0, prefix.length);
            System.arraycopy(add, 0, newItemset, prefix.length, k);

            emitRules(newItemset, match, total, output, ruleCount);
        }
    }
}
