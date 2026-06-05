package cmar;

import java.util.Arrays;

/**
 * Class Association Rule (CAR): antecedent => class label.
 * Stores precomputed chi-square for fast pruning and classification.
 */
public class Rule implements Comparable<Rule> {
    int[] antecedent;  // sorted item IDs
    int classLabel;
    int support;       // absolute support count (rule support = antecedent ∩ class)
    int antecedentSupport; // support of antecedent pattern alone
    double confidence;
    double chiSquare;
    double weight;     // combined weight for classification
    // Hybrid HM+Lift extension (WCBA-style, Alwidian et al. 2018 + WEviRC, Bahri et al. 2020)
    double lift;       // Supp(X→c)·N / (Supp(X)·Supp(c)); >1 = positive correlation
    double hm;         // 2·wSupp·conf / (wSupp+conf), F1-like balance
    // HM/Lift hybrid sort — kept for ablation reproducibility (paper negative results)
    public static boolean useHMLift = false;
    public static boolean useLiftSort = false;
    // Class frequencies map (populated by CMARClassifier.fit) — used by Cost-Sensitive Voting
    public static java.util.Map<Integer, Integer> CLASS_FREQS = null;
    public static int TOTAL_N = 0;
    long[] antBitmap;

    public Rule(int[] antecedent, int classLabel, int support, double confidence) {
        this.antecedent = antecedent;
        Arrays.sort(this.antecedent);
        this.classLabel = classLabel;
        this.support = support;
        this.confidence = confidence;
        this.chiSquare = 0;
        this.weight = 0;
    }

    /**
     * CMAR rule ordering: confidence desc, support desc, rule length asc.
     * Phase 08: extra tie-breakers (item-by-item, then classLabel) so ordering
     * is FULLY DETERMINISTIC regardless of insertion order — needed because
     * parallel mining inserts rules in non-deterministic order.
     */
    @Override
    public int compareTo(Rule other) {
        // Optional WCBA-style HM/Lift ordering (ablation reproducibility)
        if (useHMLift) {
            if (this.hm != other.hm) return Double.compare(other.hm, this.hm);
            if (this.lift != other.lift) return Double.compare(other.lift, this.lift);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int n0 = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < n0; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        // Optional Larger-Lift-first ordering (ablation reproducibility)
        if (useLiftSort) {
            if (this.lift != other.lift) return Double.compare(other.lift, this.lift);
            if (this.confidence != other.confidence)
                return Double.compare(other.confidence, this.confidence);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nL = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nL; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        // Paper-faithful CMAR ordering: confidence desc → support desc → length asc → tie-breakers
        if (this.confidence != other.confidence)
            return Double.compare(other.confidence, this.confidence);
        if (this.support != other.support)
            return Integer.compare(other.support, this.support);
        if (this.antecedent.length != other.antecedent.length)
            return Integer.compare(this.antecedent.length, other.antecedent.length);
        // Deterministic tie-breakers: lexicographic antecedent compare, then class label
        int n = Math.min(this.antecedent.length, other.antecedent.length);
        for (int i = 0; i < n; i++) {
            if (this.antecedent[i] != other.antecedent[i])
                return Integer.compare(this.antecedent[i], other.antecedent[i]);
        }
        return Integer.compare(this.classLabel, other.classLabel);
    }

    /**
     * Check if this rule's antecedent is a subset of the given items (sorted).
     */
    public boolean matches(int[] items) {
        int j = 0;
        for (int item : antecedent) {
            while (j < items.length && items[j] < item) j++;
            if (j >= items.length || items[j] != item) return false;
            j++;
        }
        return true;
    }

    /**
     * Ensure antecedent bitmap exists sized for item universe [0..maxItem].
     * Phase 16: enables word-wise matchesBitmap (fewer ops than per-item when antBitmap set).
     */
    public void ensureAntBitmap(int maxItem) {
        int words = (maxItem >> 6) + 1;
        if (antBitmap != null && antBitmap.length == words) return;
        antBitmap = new long[words];
        for (int item : antecedent) {
            if (item >= 0 && item <= maxItem)
                antBitmap[item >> 6] |= (1L << (item & 63));
        }
    }

    /**
     * Bitwise match: antecedent items checked against bitmap.
     * Phase 16: if antBitmap present, check per 64-bit word: (bitmap[i] & mask) == mask.
     */
    public boolean matchesBitmap(long[] bitmap) {
        long[] ab = antBitmap;
        if (ab != null) {
            for (int i = 0; i < ab.length; i++) {
                long mask = ab[i];
                if (mask == 0L) continue;
                if (i >= bitmap.length) return false;
                if ((bitmap[i] & mask) != mask) return false;
            }
            return true;
        }
        for (int item : antecedent) {
            int idx = item >> 6;
            int bit = item & 63;
            if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0)
                return false;
        }
        return true;
    }

    public int getClassLabel() { return classLabel; }
    public int getSupport() { return support; }
    public int getAntecedentSupport() { return antecedentSupport; }
    public double getConfidence() { return confidence; }
    public double getChiSquare() { return chiSquare; }
    public double getLift() { return lift; }
    public double getHm() { return hm; }
    public double getWeight() { return weight; }
    public int[] getAntecedent() { return antecedent; }

    @Override
    public String toString() {
        return Arrays.toString(antecedent) + " => " + classLabel
                + " [sup=" + support + ", conf=" + String.format("%.3f", confidence)
                + ", χ²=" + String.format("%.2f", chiSquare) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rule)) return false;
        Rule r = (Rule) o;
        return classLabel == r.classLabel && Arrays.equals(antecedent, r.antecedent);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(antecedent) * 31 + classLabel;
    }
}
