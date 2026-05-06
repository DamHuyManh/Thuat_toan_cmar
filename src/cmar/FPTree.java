package cmar;

import java.util.*;

/**
 * FP-Tree with header table and lazy pruning.
 * Optimized: items sorted by frequency descending for maximum compression.
 */
public class FPTree {
    FPNode root;
    Map<Integer, FPNode> headerTable; // item -> first node in linked list
    // Phase 12: tail pointer per item to append header links in O(1)
    Map<Integer, FPNode> headerTail;  // item -> last node in linked list
    Map<Integer, Integer> itemCounts;  // item -> total frequency

    public FPTree() {
        this.root = new FPNode(-1, 0, null);
        this.headerTable = new HashMap<>();
        this.headerTail = new HashMap<>();
        this.itemCounts = new HashMap<>();
    }

    /**
     * Build FP-tree from transactions with min support filtering.
     * Items sorted by frequency descending for maximum prefix sharing.
     */
    public static FPTree build(int[][] transactions, int minSupport) {
        // Pass 1: count item frequencies
        Map<Integer, Integer> freq = new HashMap<>();
        for (int[] txn : transactions) {
            for (int item : txn) {
                freq.merge(item, 1, Integer::sum);
            }
        }

        // Filter infrequent items
        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) return new FPTree();

        // Sort order: descending frequency, then ascending item for tie-break
        FPTree tree = new FPTree();
        tree.itemCounts = freq;

        // Pass 2: insert sorted transactions
        int[] sortedItems = new int[freq.size()];
        for (int[] txn : transactions) {
            int len = 0;
            for (int item : txn) {
                if (freq.containsKey(item)) {
                    sortedItems[len++] = item;
                }
            }
            if (len == 0) continue;

            // Phase 14: sort in-place, avoid Arrays.copyOf per transaction
            sortByFrequency(sortedItems, len, freq);
            tree.insertTransaction(sortedItems, len, 1);
        }
        return tree;
    }

    /**
     * Build FP-tree from weighted pattern base (for conditional trees).
     */
    public static FPTree buildConditional(List<int[]> patterns, List<Integer> counts, int minSupport) {
        // Count frequencies with weights
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < patterns.size(); i++) {
            int count = counts.get(i);
            for (int item : patterns.get(i)) {
                freq.merge(item, count, Integer::sum);
            }
        }

        freq.entrySet().removeIf(e -> e.getValue() < minSupport);
        if (freq.isEmpty()) return new FPTree();

        FPTree tree = new FPTree();
        tree.itemCounts = freq;

        for (int i = 0; i < patterns.size(); i++) {
            int[] pattern = patterns.get(i);
            int count = counts.get(i);

            // Filter and sort
            int len = 0;
            int[] filtered = new int[pattern.length];
            for (int item : pattern) {
                if (freq.containsKey(item)) {
                    filtered[len++] = item;
                }
            }
            if (len == 0) continue;

            // Phase 14: sort in-place, avoid Arrays.copyOf per pattern
            sortByFrequency(filtered, len, freq);
            tree.insertTransaction(filtered, len, count);
        }
        return tree;
    }

    private void insertTransaction(int[] items) {
        insertTransaction(items, 1);
    }

    // Phase 13: package-private để miner build conditional tree trực tiếp (tránh patterns+counts list).
    void insertTransaction(int[] items, int count) {
        insertTransaction(items, items.length, count);
    }

    // Phase 14: insert prefix of array (avoid per-txn array allocation)
    void insertTransaction(int[] items, int len, int count) {
        FPNode current = root;
        for (int i = 0; i < len; i++) {
            int item = items[i];
            FPNode child = current.getChild(item);
            if (child != null) {
                child.count += count;
                current = child;
            } else {
                child = new FPNode(item, count, current);
                current.children.put(item, child);
                // Update header table link
                FPNode tail = headerTail.get(item);
                if (tail != null) {
                    tail.link = child;
                    headerTail.put(item, child);
                } else {
                    headerTable.put(item, child);
                    headerTail.put(item, child);
                }
                current = child;
            }
        }
    }

    /**
     * Check if tree has a single path (optimization for mining).
     */
    public boolean isSinglePath() {
        FPNode node = root;
        while (!node.children.isEmpty()) {
            if (node.children.size() > 1) return false;
            node = node.children.values().iterator().next();
        }
        return true;
    }

    /**
     * Get all items on the single path.
     */
    public List<int[]> getSinglePathItems() {
        List<int[]> items = new ArrayList<>();
        FPNode node = root;
        while (!node.children.isEmpty()) {
            node = node.children.values().iterator().next();
            items.add(new int[]{node.item, node.count});
        }
        return items;
    }

    /**
     * Phase 17: ghi id item trên single path vào {@code buf} (gốc → lá). Điều kiện: {@link #isSinglePath()}.
     * Nếu {@code buf} ngắn hơn độ sâu path, chỉ ghi tới hết mảng; giá trị trả về = độ sâu thật (để caller mở rộng buffer).
     */
    public int collectSinglePathItemIds(int[] buf) {
        int len = 0;
        FPNode node = root;
        while (!node.children.isEmpty()) {
            node = node.children.values().iterator().next();
            if (buf != null && len < buf.length) buf[len] = node.item;
            len++;
        }
        return len;
    }

    public boolean isEmpty() {
        return headerTable.isEmpty();
    }

    private static void sortByFrequency(int[] items, int len, Map<Integer, Integer> freq) {
        // Simple insertion sort (items arrays are typically short)
        for (int i = 1; i < len; i++) {
            int key = items[i];
            int keyFreq = freq.getOrDefault(key, 0);
            int j = i - 1;
            while (j >= 0) {
                int jFreq = freq.getOrDefault(items[j], 0);
                if (jFreq < keyFreq || (jFreq == keyFreq && items[j] > key)) {
                    items[j + 1] = items[j];
                    j--;
                } else break;
            }
            items[j + 1] = key;
        }
    }

    private static void sortByFrequency(int[] items, Map<Integer, Integer> freq) {
        sortByFrequency(items, items.length, freq);
    }
}
