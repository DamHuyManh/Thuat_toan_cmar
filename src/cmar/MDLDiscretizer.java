package cmar;

import java.util.*;

/**
 * MDL Discretization (Fayyad & Irani 1993).
 * Entropy-based supervised discretization - cùng phương pháp C4.5 dùng.
 * Tìm cut points tối ưu dựa trên information gain + MDL criterion.
 */
public class MDLDiscretizer {

    /**
     * Tìm cut points cho một attribute dựa trên class labels.
     * @param values giá trị attribute
     * @param labels class labels tương ứng
     * @return danh sách cut points (sorted)
     */
    public static List<Double> findCutPoints(double[] values, int[] labels) {
        int n = values.length;
        if (n <= 1) return Collections.emptyList();

        // Sort theo giá trị, giữ label tương ứng
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(values[a], values[b]));

        double[] sortedVals = new double[n];
        int[] sortedLabels = new int[n];
        for (int i = 0; i < n; i++) {
            sortedVals[i] = values[indices[i]];
            sortedLabels[i] = labels[indices[i]];
        }

        List<Double> cutPoints = new ArrayList<>();
        findCutPointsRecursive(sortedVals, sortedLabels, 0, n - 1, cutPoints);
        Collections.sort(cutPoints);
        return cutPoints;
    }

    private static void findCutPointsRecursive(double[] vals, int[] labels,
                                                int start, int end, List<Double> cutPoints) {
        if (start >= end) return;

        int n = end - start + 1;
        double bestGain = -1;
        int bestSplit = -1;
        double bestCutPoint = 0;

        // Entropy của toàn bộ interval
        double totalEntropy = entropy(labels, start, end);
        if (totalEntropy == 0) return; // Pure interval

        // Tìm split point tốt nhất
        for (int i = start; i < end; i++) {
            // Chỉ split ở boundary (giá trị khác nhau)
            if (vals[i] == vals[i + 1]) continue;

            double cutPoint = (vals[i] + vals[i + 1]) / 2.0;

            int leftSize = i - start + 1;
            int rightSize = end - i;

            double leftEntropy = entropy(labels, start, i);
            double rightEntropy = entropy(labels, i + 1, end);

            double weightedEntropy = ((double) leftSize / n) * leftEntropy
                    + ((double) rightSize / n) * rightEntropy;

            double gain = totalEntropy - weightedEntropy;

            if (gain > bestGain) {
                bestGain = gain;
                bestSplit = i;
                bestCutPoint = cutPoint;
            }
        }

        if (bestSplit < 0) return;

        // MDL criterion (Fayyad & Irani 1993)
        if (!mdlAccepted(labels, start, end, bestSplit, bestGain, totalEntropy)) {
            return;
        }

        cutPoints.add(bestCutPoint);

        // Recursively find more cut points
        findCutPointsRecursive(vals, labels, start, bestSplit, cutPoints);
        findCutPointsRecursive(vals, labels, bestSplit + 1, end, cutPoints);
    }

    /**
     * MDL stopping criterion.
     * Gain > (log2(N-1)/N) + (delta(S,S1,S2)/N)
     * where delta = log2(3^k - 2) - (k*Ent(S) - k1*Ent(S1) - k2*Ent(S2))
     */
    private static boolean mdlAccepted(int[] labels, int start, int end,
                                        int splitPoint, double gain, double totalEntropy) {
        int n = end - start + 1;
        int leftSize = splitPoint - start + 1;
        int rightSize = end - splitPoint;

        // Count distinct classes in each partition
        int k = countDistinctClasses(labels, start, end);
        int k1 = countDistinctClasses(labels, start, splitPoint);
        int k2 = countDistinctClasses(labels, splitPoint + 1, end);

        double leftEntropy = entropy(labels, start, splitPoint);
        double rightEntropy = entropy(labels, splitPoint + 1, end);

        double delta = log2(Math.pow(3, k) - 2)
                - (k * totalEntropy - k1 * leftEntropy - k2 * rightEntropy);

        double threshold = (log2(n - 1) + delta) / n;

        return gain > threshold;
    }

    private static double entropy(int[] labels, int start, int end) {
        int n = end - start + 1;
        if (n <= 0) return 0;

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = start; i <= end; i++) {
            counts.merge(labels[i], 1, Integer::sum);
        }

        double ent = 0;
        for (int count : counts.values()) {
            if (count > 0) {
                double p = (double) count / n;
                ent -= p * log2(p);
            }
        }
        return ent;
    }

    private static int countDistinctClasses(int[] labels, int start, int end) {
        Set<Integer> classes = new HashSet<>();
        for (int i = start; i <= end; i++) {
            classes.add(labels[i]);
        }
        return classes.size();
    }

    private static double log2(double x) {
        if (x <= 0) return 0;
        return Math.log(x) / Math.log(2);
    }

    /**
     * Discretize một attribute dùng cut points đã tìm.
     * @return bin index cho mỗi giá trị
     */
    public static int[] discretize(double[] values, List<Double> cutPoints) {
        int[] bins = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            int bin = 0;
            for (double cp : cutPoints) {
                if (values[i] > cp) bin++;
                else break;
            }
            bins[i] = bin;
        }
        return bins;
    }
}
