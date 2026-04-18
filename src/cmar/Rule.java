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
     */
    @Override
    public int compareTo(Rule other) {
        if (this.confidence != other.confidence)
            return Double.compare(other.confidence, this.confidence);
        if (this.support != other.support)
            return Integer.compare(other.support, this.support);
        return Integer.compare(this.antecedent.length, other.antecedent.length);
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
     * Bitwise match: antecedent items checked against bitmap.
     */
    public boolean matchesBitmap(long[] bitmap) {
        for (int item : antecedent) {
            int idx = item >> 6; // item / 64
            int bit = item & 63; // item % 64
            if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0)
                return false;
        }
        return true;
    }

    public int getClassLabel() { return classLabel; }
    public int getSupport() { return support; }
    public double getConfidence() { return confidence; }
    public double getChiSquare() { return chiSquare; }

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
