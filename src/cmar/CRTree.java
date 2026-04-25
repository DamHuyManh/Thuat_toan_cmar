package cmar;

import java.util.*;

/**
 * CR-Tree (Classification Rule Tree) - hash-indexed rule storage.
 * Partitioned by class label for fast retrieval during classification.
 * Uses HashMap for O(1) amortized lookup by first antecedent item.
 */
public class CRTree {
    // class label -> (first item of antecedent -> list of rules)
    private Map<Integer, Map<Integer, List<Rule>>> index;
    private List<Rule> allRules;
    private Set<Integer> classLabels;

    public CRTree() {
        this.index = new HashMap<>();
        this.allRules = new ArrayList<>();
        this.classLabels = new HashSet<>();
    }

    /**
     * Build CR-Tree from pruned rules.
     */
    public void build(List<Rule> rules) {
        allRules = new ArrayList<>(rules);
        index.clear();
        classLabels.clear();

        for (Rule rule : rules) {
            classLabels.add(rule.classLabel);
            index.computeIfAbsent(rule.classLabel, k -> new HashMap<>())
                 .computeIfAbsent(rule.antecedent[0], k -> new ArrayList<>())
                 .add(rule);
        }
    }

    /**
     * Find all rules matching a given instance (bitmap representation).
     * Returns rules grouped by class label.
     */
    public Map<Integer, List<Rule>> findMatchingRules(long[] bitmap) {
        Map<Integer, List<Rule>> matched = new HashMap<>();

        for (Map.Entry<Integer, Map<Integer, List<Rule>>> classEntry : index.entrySet()) {
            int classLabel = classEntry.getKey();
            List<Rule> classMatches = new ArrayList<>();

            for (Map.Entry<Integer, List<Rule>> itemEntry : classEntry.getValue().entrySet()) {
                int firstItem = itemEntry.getKey();
                // Quick check: if first item not in bitmap, skip all rules
                int idx = firstItem >> 6;
                int bit = firstItem & 63;
                if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0) continue;

                for (Rule rule : itemEntry.getValue()) {
                    if (rule.matchesBitmap(bitmap)) {
                        classMatches.add(rule);
                    }
                }
            }

            if (!classMatches.isEmpty()) {
                matched.put(classLabel, classMatches);
            }
        }

        return matched;
    }

    /**
     * Phase 02: Find all matching rules — dùng hash index thay vì linear scan.
     * Chỉ check rules mà firstItem xuất hiện trong bitmap.
     */
    public List<Rule> findAllMatching(long[] bitmap) {
        List<Rule> matched = new ArrayList<>();
        for (Map<Integer, List<Rule>> byFirst : index.values()) {
            for (Map.Entry<Integer, List<Rule>> e : byFirst.entrySet()) {
                int firstItem = e.getKey();
                int idx = firstItem >> 6;
                int bit = firstItem & 63;
                if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0) continue;
                for (Rule r : e.getValue()) {
                    if (r.matchesBitmap(bitmap)) matched.add(r);
                }
            }
        }
        return matched;
    }

    public Set<Integer> getClassLabels() {
        return classLabels;
    }

    public List<Rule> getAllRules() {
        return Collections.unmodifiableList(allRules);
    }

    public int size() {
        return allRules.size();
    }
}
