package cmar;

import java.util.List;

/**
 * Triangular fuzzy membership for continuous attributes (Fuzzy CMAR).
 *
 * <p>Motivation: crisp MDL bins lose information at cut boundaries. A glucose of 125 is "normal"
 * and 126 is "high" under a hard cut at 125.5 — but clinically they are nearly identical. Fuzzy
 * sets let a borderline value belong PARTIALLY to two adjacent bins, so rules from BOTH bins can
 * fire near class boundaries → recovers borderline minority patients → higher macro-Recall / F1.
 *
 * <p>Sets: triangular, apexes at bin CENTERS (midpoints between consecutive cut points; edge bins
 * extend by the mean gap). For a value v we return the two bins with highest membership and their
 * weights (partition of unity: w1 + w2 = 1). The caller (encoder) emits the top-1 bin item always,
 * and additionally the top-2 bin item when w2 exceeds a threshold tau — so only genuinely borderline
 * values get expanded, keeping support inflation bounded.
 *
 * <p>Reference: Kuok, Fu, Wong (1998) fuzzy association rules; Sowan et al. ESWA 2014 fuzzy AC;
 * FACA (Applied Soft Computing 2016).
 */
public final class FuzzyDiscretizer {

    private FuzzyDiscretizer() {}

    /** Result of fuzzifying one value: two candidate bins + their membership weights (w1 >= w2). */
    public static final class FuzzyBins {
        public final int bin1;
        public final double w1;
        public final int bin2;   // -1 if none
        public final double w2;
        FuzzyBins(int bin1, double w1, int bin2, double w2) {
            this.bin1 = bin1; this.w1 = w1; this.bin2 = bin2; this.w2 = w2;
        }
    }

    /**
     * Compute bin centers from cut points. k cuts → k+1 bins.
     * Interior center_j = midpoint(cut[j-1], cut[j]). Edge bins extend by mean gap.
     */
    public static double[] centers(List<Double> cuts) {
        int k = cuts.size();
        int bins = k + 1;
        double[] c = new double[bins];
        if (k == 0) { c[0] = 0; return c; }
        // mean gap between cut points (fallback for single cut)
        double meanGap;
        if (k == 1) {
            meanGap = Math.max(1e-9, Math.abs(cuts.get(0)) * 0.5 + 1e-6);
        } else {
            double sum = 0;
            for (int j = 1; j < k; j++) sum += (cuts.get(j) - cuts.get(j - 1));
            meanGap = Math.max(1e-9, sum / (k - 1));
        }
        c[0] = cuts.get(0) - meanGap / 2.0;                 // left edge bin
        for (int j = 1; j < bins - 1; j++) {
            c[j] = (cuts.get(j - 1) + cuts.get(j)) / 2.0;   // interior
        }
        c[bins - 1] = cuts.get(k - 1) + meanGap / 2.0;      // right edge bin
        return c;
    }

    /**
     * Fuzzify value v given precomputed bin centers.
     * Triangular partition of unity: v lies between two adjacent centers; weights are the linear
     * interpolation. Beyond the edge centers → full membership in the edge bin.
     */
    public static FuzzyBins fuzzify(double v, double[] centers) {
        int bins = centers.length;
        if (bins <= 1) return new FuzzyBins(0, 1.0, -1, 0.0);
        if (v <= centers[0]) return new FuzzyBins(0, 1.0, -1, 0.0);
        if (v >= centers[bins - 1]) return new FuzzyBins(bins - 1, 1.0, -1, 0.0);
        // find j such that centers[j] <= v < centers[j+1]
        int j = 0;
        while (j < bins - 1 && v >= centers[j + 1]) j++;
        double span = centers[j + 1] - centers[j];
        double wRight = span <= 1e-12 ? 0.0 : (v - centers[j]) / span;
        double wLeft = 1.0 - wRight;
        if (wLeft >= wRight) return new FuzzyBins(j, wLeft, j + 1, wRight);
        else return new FuzzyBins(j + 1, wRight, j, wLeft);
    }
}
