package cmar.boost;

import cmar.CMARClassifier;
import cmar.Metrics;
import java.util.*;

/**
 * Shared helpers for ensemble classifiers (Bagging, Boosted, HyperRandomBag).
 * Avoids copy-paste of bootstrap / vote-aggregation / batch-predict logic.
 */
public final class EnsembleUtils {

    private EnsembleUtils() {}

    /**
     * Bootstrap sample with replacement.
     * @param X original instances
     * @param y original labels
     * @param yOut sized [sampleSize] — filled with sampled labels
     * @param rng random source
     * @param sampleSize number of samples to draw (typically N for paper-faithful)
     * @param inBag optional boolean[N] — marks which originals got sampled (for OOB), null to skip
     * @return sampled instances [sampleSize][...] sharing references with X
     */
    public static int[][] bootstrapSample(int[][] X, int[] y, int[] yOut, Random rng,
                                           int sampleSize, boolean[] inBag) {
        int N = X.length;
        int[][] Xt = new int[sampleSize][];
        for (int i = 0; i < sampleSize; i++) {
            int idx = rng.nextInt(N);
            Xt[i] = X[idx];
            yOut[i] = y[idx];
            if (inBag != null) inBag[idx] = true;
        }
        return Xt;
    }

    /**
     * Weighted majority vote across T base classifiers.
     * @param classifiers trained base classifiers
     * @param weights per-classifier vote weights (e.g. OOB accuracy or AdaBoost α)
     * @param x instance to predict
     * @param defaultClass returned when ensemble empty
     */
    public static int weightedVote(List<CMARClassifier> classifiers, List<Double> weights,
                                    int[] x, int defaultClass) {
        if (classifiers.isEmpty()) return defaultClass;
        Map<Integer, Double> votes = new HashMap<>();
        for (int t = 0; t < classifiers.size(); t++) {
            int pred = classifiers.get(t).predict(x);
            votes.merge(pred, weights.get(t), Double::sum);
        }
        return argmaxVotes(votes, defaultClass);
    }

    /** Argmax over class→score map, with deterministic fallback. */
    public static int argmaxVotes(Map<Integer, Double> votes, int defaultClass) {
        int best = defaultClass;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : votes.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** Determine majority class (defaultClass) from training labels. */
    public static int majorityClass(int[] labels) {
        Map<Integer, Integer> cnt = new HashMap<>();
        for (int lbl : labels) cnt.merge(lbl, 1, Integer::sum);
        return cnt.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    /** Compute number of distinct classes. */
    public static int numClasses(int[] labels) {
        Set<Integer> s = new HashSet<>();
        for (int lbl : labels) s.add(lbl);
        return s.size();
    }

    /** Score test set: Metrics + Accuracy. */
    public static Metrics scoreFull(EnsemblePredict ensemble, int[][] X, int[] y) {
        int[] preds = new int[X.length];
        for (int i = 0; i < X.length; i++) preds[i] = ensemble.predict(X[i]);
        return Metrics.compute(preds, y);
    }

    /** Functional interface for any ensemble that predicts one instance. */
    @FunctionalInterface
    public interface EnsemblePredict {
        int predict(int[] x);
    }
}
