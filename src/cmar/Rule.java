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
    // Global switch: when true, compareTo uses HM/Lift order instead of conf/sup
    public static boolean useHMLift = false;
    // Larger-Lift-first: sort by Lift DESC → confidence DESC → length ASC.
    public static boolean useLiftSort = false;
    // Alternative sort: prefer LONGER (specific) rules instead of shorter (general).
    // λ3/PAM-style "lazy ranking" — specific rules capture more knowledge.
    public static boolean useLongerRules = false;
    // New: Lift as TIEBREAKER between support and length (does NOT replace CMAR primary order).
    // Sort: conf DESC → sup DESC → Lift DESC → length ASC → lexicographic
    public static boolean useLiftTieBreak = false;
    // New: Lift in position 2 (between Confidence and Support).
    // Sort: conf DESC → Lift DESC → sup DESC → length ASC → lexicographic
    public static boolean useLiftSecond = false;
    // S1: chi² as primary sort (replaces confidence).
    // Sort: chi² DESC → confidence DESC → length ASC
    public static boolean useChiFirst = false;
    // S2: composite (conf × Lift) as primary sort key.
    public static boolean useSortCompose = false;
    // S3: composite (chi² × Lift) as primary sort key.
    public static boolean useSortChiLift = false;
    // S4: confidence + alpha*Lift (linear combo, conf-dominant with mild Lift boost).
    public static boolean useConfLinear = false;
    public static double confLinearAlpha = 0.1;
    // S5: conditional - if confidence==1.0 use Lift, else use support.
    public static boolean useCondLift = false;
    // S6: dominant class tie-breaker after CMAR sort.
    // Sort: conf DESC → sup DESC → class_count DESC → length ASC
    public static boolean useDominantClass = false;
    // Class frequencies map (populated externally before sort).
    public static java.util.Map<Integer, Integer> CLASS_FREQS = null;
    // P1: Class-weighted sort — boost confidence of minority class rules.
    // score = confidence × sqrt(N / class_freq(c))  → minority class gets larger weight.
    public static boolean useClassWeightedSort = false;
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
        if (useClassWeightedSort && CLASS_FREQS != null && TOTAL_N > 0) {
            int fA = CLASS_FREQS.getOrDefault(this.classLabel, 1);
            int fB = CLASS_FREQS.getOrDefault(other.classLabel, 1);
            double wA = Math.sqrt((double) TOTAL_N / Math.max(1, fA));
            double wB = Math.sqrt((double) TOTAL_N / Math.max(1, fB));
            double scoreA = this.confidence * wA;
            double scoreB = other.confidence * wB;
            if (scoreA != scoreB) return Double.compare(scoreB, scoreA);
            if (this.support != other.support)
                return Integer.compare(other.support, this.support);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nCW = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nCW; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useConfLinear) {
            double sA = this.confidence + confLinearAlpha * this.lift;
            double sB = other.confidence + confLinearAlpha * other.lift;
            if (sA != sB) return Double.compare(sB, sA);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nCL = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nCL; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useCondLift) {
            if (this.confidence != other.confidence)
                return Double.compare(other.confidence, this.confidence);
            // If both confidence == 1.0 → use Lift; otherwise use support
            boolean bothPerfect = Math.abs(this.confidence - 1.0) < 1e-9;
            if (bothPerfect && this.lift != other.lift)
                return Double.compare(other.lift, this.lift);
            if (this.support != other.support)
                return Integer.compare(other.support, this.support);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nCnd = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nCnd; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useDominantClass) {
            if (this.confidence != other.confidence)
                return Double.compare(other.confidence, this.confidence);
            if (this.support != other.support)
                return Integer.compare(other.support, this.support);
            if (CLASS_FREQS != null) {
                int cA = CLASS_FREQS.getOrDefault(this.classLabel, 0);
                int cB = CLASS_FREQS.getOrDefault(other.classLabel, 0);
                if (cA != cB) return Integer.compare(cB, cA);
            }
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nD = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nD; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useChiFirst) {
            if (this.chiSquare != other.chiSquare)
                return Double.compare(other.chiSquare, this.chiSquare);
            if (this.confidence != other.confidence)
                return Double.compare(other.confidence, this.confidence);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nC = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nC; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useSortCompose) {
            double sA = this.confidence * this.lift;
            double sB = other.confidence * other.lift;
            if (sA != sB) return Double.compare(sB, sA);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nSc = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nSc; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useSortChiLift) {
            double sA = this.chiSquare * this.lift;
            double sB = other.chiSquare * other.lift;
            if (sA != sB) return Double.compare(sB, sA);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int nSx = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < nSx; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (useLiftSort) {
            // Larger-Lift-first: Lift DESC → confidence DESC → length ASC → tie-breakers
            if (this.lift != other.lift)
                return Double.compare(other.lift, this.lift);
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
        if (useHMLift) {
            // WCBA-style ordering: HM desc → Lift desc → length asc → tie-breakers
            if (this.hm != other.hm)
                return Double.compare(other.hm, this.hm);
            if (this.lift != other.lift)
                return Double.compare(other.lift, this.lift);
            if (this.antecedent.length != other.antecedent.length)
                return Integer.compare(this.antecedent.length, other.antecedent.length);
            int n0 = Math.min(this.antecedent.length, other.antecedent.length);
            for (int i = 0; i < n0; i++) {
                if (this.antecedent[i] != other.antecedent[i])
                    return Integer.compare(this.antecedent[i], other.antecedent[i]);
            }
            return Integer.compare(this.classLabel, other.classLabel);
        }
        if (this.confidence != other.confidence)
            return Double.compare(other.confidence, this.confidence);
        // New: Lift in position 2 (after confidence, before support).
        if (useLiftSecond && this.lift != other.lift)
            return Double.compare(other.lift, this.lift);
        if (this.support != other.support)
            return Integer.compare(other.support, this.support);
        // New tiebreaker: when conf+sup tied, prefer rules with higher Lift (more correlated).
        if (useLiftTieBreak && this.lift != other.lift)
            return Double.compare(other.lift, this.lift);
        if (this.antecedent.length != other.antecedent.length) {
            // Default: shorter first (CMAR paper). useLongerRules: longer first (λ3/PAM).
            return useLongerRules
                    ? Integer.compare(other.antecedent.length, this.antecedent.length)
                    : Integer.compare(this.antecedent.length, other.antecedent.length);
        }
        // Tie-breakers for determinism: lexicographic compare of antecedents, then class
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
