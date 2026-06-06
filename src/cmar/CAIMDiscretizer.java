package cmar;

import java.util.*;

/**
 * CAIM (Class-Attribute Interdependence Maximization) discretization.
 *
 * <p>Reference: Kurgan &amp; Cios, "CAIM Discretization Algorithm", IEEE TKDE 16(2), 2004.
 *
 * <p>Supervised, top-down: greedily inserts the boundary that maximizes the CAIM criterion until
 * adding more no longer improves it (bounded by a max-interval cap). Class-aware cut points fit
 * continuous attributes better than purely entropy-based MDL on some medical datasets.
 *
 * <p>CAIM criterion for a discretization scheme with n intervals:
 * <pre>
 *   CAIM = (1/n) · Σ_r ( max_r² / M_r )
 * </pre>
 * where for interval r: max_r = count of the most frequent class in r, M_r = total samples in r.
 * Higher = better class purity per interval.
 */
public final class CAIMDiscretizer {

    private CAIMDiscretizer() {}

    private static final int MAX_INTERVALS = 8;  // bound rules/memory

    public static List<Double> findCutPoints(double[] values, int[] labels) {
        int n = values.length;
        if (n < 2) return Collections.emptyList();

        // Sort by value (carry labels)
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(values[a], values[b]));
        double[] v = new double[n];
        int[] y = new int[n];
        for (int i = 0; i < n; i++) { v[i] = values[idx[i]]; y[i] = labels[idx[i]]; }

        // Class index map
        Map<Integer, Integer> clsIdx = new HashMap<>();
        for (int lbl : y) clsIdx.putIfAbsent(lbl, clsIdx.size());
        int K = clsIdx.size();
        if (K < 2) return Collections.emptyList();

        // Candidate boundaries = midpoints between consecutive DISTINCT values
        List<Double> candidates = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (v[i] != v[i - 1]) candidates.add((v[i] + v[i - 1]) / 2.0);
        }
        if (candidates.isEmpty()) return Collections.emptyList();

        // Greedy CAIM maximization
        TreeSet<Double> scheme = new TreeSet<>();      // chosen interior boundaries
        double globalCaim = caim(v, y, K, clsIdx, scheme);
        int maxAdds = Math.min(MAX_INTERVALS - 1, candidates.size());

        for (int added = 0; added < maxAdds; added++) {
            double bestB = Double.NaN;
            double bestCaim = globalCaim;
            for (double b : candidates) {
                if (scheme.contains(b)) continue;
                scheme.add(b);
                double c = caim(v, y, K, clsIdx, scheme);
                scheme.remove(b);
                if (c > bestCaim) { bestCaim = c; bestB = b; }
            }
            if (Double.isNaN(bestB)) break;            // no improvement → stop
            scheme.add(bestB);
            globalCaim = bestCaim;
        }

        return new ArrayList<>(scheme);
    }

    /** Compute CAIM for the given interior boundaries. */
    private static double caim(double[] v, int[] y, int K, Map<Integer,Integer> clsIdx, TreeSet<Double> bounds) {
        int n = v.length;
        int nIntervals = bounds.size() + 1;
        double[] boundArr = new double[bounds.size()];
        int bi = 0;
        for (double b : bounds) boundArr[bi++] = b;

        int[][] quanta = new int[nIntervals][K];     // counts per interval × class
        for (int i = 0; i < n; i++) {
            int r = 0;
            while (r < boundArr.length && v[i] > boundArr[r]) r++;
            quanta[r][clsIdx.get(y[i])]++;
        }

        double sum = 0;
        for (int r = 0; r < nIntervals; r++) {
            int M = 0, max = 0;
            for (int k = 0; k < K; k++) { M += quanta[r][k]; if (quanta[r][k] > max) max = quanta[r][k]; }
            if (M > 0) sum += (double) max * max / M;
        }
        return sum / nIntervals;
    }
}
