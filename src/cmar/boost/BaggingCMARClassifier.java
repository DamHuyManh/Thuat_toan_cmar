package cmar.boost;

import cmar.CMARClassifier;
import cmar.Metrics;
import java.util.*;

/**
 * Bagging CMAR — T base classifiers, each trained on bootstrap sample,
 * predictions averaged. Doesn't suffer the strong-learner paradox of boosting.
 *
 * <p>Diversity injection:
 * <ul>
 *   <li>Bootstrap sample (with replacement, same N)</li>
 *   <li>Optional: random feature/item subsetting (Random Forest style)</li>
 *   <li>Different random seed per ensemble member → different rule tiebreaks</li>
 * </ul>
 *
 * <p>Voting: weighted majority by each base's training accuracy (out-of-bag).
 * Class with max(Σ w_t · I(h_t(x) = c)) wins.
 *
 * <p>Reference:
 * <ul>
 *   <li>Breiman (1996) "Bagging Predictors"</li>
 *   <li>Bahri et al. (2018) "Random Forest of Classification Association Rules"</li>
 * </ul>
 */
public class BaggingCMARClassifier {

    private final int T;
    private final int minSupport;
    private final double minConfidence;
    private final double chiThreshold;
    private final int maxCoverage;
    private final int maxRulesPerClass;
    private final int maxAntecedentLen;
    private final int topKGlobal;
    private final double featureSubsetRatio; // 0=disabled, 1=all features
    private double bootstrapRatio = 1.0;     // 0.5-1.0 = sample less for more diversity

    private final List<CMARClassifier> classifiers = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private final List<Set<Integer>> activeItems = new ArrayList<>(); // per-classifier feature subset
    private int defaultClass = 0;
    private long seed = 42;

    public static boolean verbose = false;

    public BaggingCMARClassifier(int T, int minSupport, double minConfidence,
                                  double chiThreshold, int maxCoverage,
                                  int maxRulesPerClass, int maxAntecedentLen,
                                  int topKGlobal, double featureSubsetRatio) {
        this.T = T;
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.chiThreshold = chiThreshold;
        this.maxCoverage = maxCoverage;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLen = maxAntecedentLen;
        this.topKGlobal = topKGlobal;
        this.featureSubsetRatio = featureSubsetRatio;
    }

    public void fit(int[][] X, int[] y) {
        int N = X.length;
        if (N == 0) return;
        defaultClass = EnsembleUtils.majorityClass(y);
        int K = EnsembleUtils.numClasses(y);

        // Build item universe for feature subsetting
        Set<Integer> allItems = new HashSet<>();
        for (int[] tx : X) for (int it : tx) allItems.add(it);
        List<Integer> allItemsList = new ArrayList<>(allItems);

        for (int t = 0; t < T; t++) {
            Random rng = new Random(seed + 100L * t);

            // Bootstrap sample
            int sampleSize = Math.max(2, (int) Math.round(N * bootstrapRatio));
            boolean[] inBag = new boolean[N];
            int[] y_t = new int[sampleSize];
            int[][] X_t = EnsembleUtils.bootstrapSample(X, y, y_t, rng, sampleSize, inBag);

            // Optional: subsample features
            Set<Integer> activeSet;
            if (featureSubsetRatio < 1.0 && featureSubsetRatio > 0) {
                List<Integer> shuffled = new ArrayList<>(allItemsList);
                Collections.shuffle(shuffled, rng);
                int keep = Math.max(1, (int) Math.round(allItemsList.size() * featureSubsetRatio));
                activeSet = new HashSet<>(shuffled.subList(0, keep));
                // Filter X_t
                for (int i = 0; i < N; i++) {
                    int[] orig = X_t[i];
                    int cnt = 0;
                    for (int item : orig) if (activeSet.contains(item)) cnt++;
                    int[] filtered = new int[cnt];
                    int j = 0;
                    for (int item : orig) if (activeSet.contains(item)) filtered[j++] = item;
                    X_t[i] = filtered;
                }
            } else {
                activeSet = allItems;
            }

            // Train base CMAR
            CMARClassifier h = new CMARClassifier(minSupport, minConfidence, chiThreshold,
                    maxCoverage, maxRulesPerClass, maxAntecedentLen, topKGlobal);
            h.fit(X_t, y_t);

            // Compute OOB accuracy as ensemble weight (Breiman style)
            int oobCorrect = 0, oobTotal = 0;
            for (int i = 0; i < N; i++) {
                if (!inBag[i]) {
                    int[] xi = X[i];
                    if (featureSubsetRatio < 1.0) {
                        int cnt = 0; for (int item : xi) if (activeSet.contains(item)) cnt++;
                        int[] f = new int[cnt]; int k = 0;
                        for (int item : xi) if (activeSet.contains(item)) f[k++] = item;
                        xi = f;
                    }
                    if (h.predict(xi) == y[i]) oobCorrect++;
                    oobTotal++;
                }
            }
            double oobAcc = oobTotal > 0 ? (double) oobCorrect / oobTotal : 0.5;
            double weight = Math.max(0.01, oobAcc - (1.0 / K));  // accuracy above random

            classifiers.add(h);
            weights.add(weight);
            activeItems.add(activeSet);

            if (verbose) {
                System.out.printf("  Bag %d: OOB=%.3f weight=%.3f rules=%d%n",
                        t + 1, oobAcc, weight, h.getRuleCount());
            }
        }
    }

    public int predict(int[] x) {
        if (classifiers.isEmpty()) return defaultClass;
        Map<Integer, Double> votes = new HashMap<>();
        for (int t = 0; t < classifiers.size(); t++) {
            int[] xi = x;
            Set<Integer> active = activeItems.get(t);
            if (featureSubsetRatio < 1.0 && featureSubsetRatio > 0 && active.size() < 100000) {
                int cnt = 0; for (int item : x) if (active.contains(item)) cnt++;
                int[] f = new int[cnt]; int k = 0;
                for (int item : x) if (active.contains(item)) f[k++] = item;
                xi = f;
            }
            int pred = classifiers.get(t).predict(xi);
            votes.merge(pred, weights.get(t), Double::sum);
        }
        int best = defaultClass;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : votes.entrySet()) {
            if (e.getValue() > bestScore) { bestScore = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    public int[] predict(int[][] X) {
        int[] o = new int[X.length];
        for (int i = 0; i < X.length; i++) o[i] = predict(X[i]);
        return o;
    }

    public Metrics scoreFull(int[][] X, int[] y) { return Metrics.compute(predict(X), y); }

    /** Weighted fuzzy inference: each base classifier votes via its weighted predict (assumes fs=1.0). */
    public int predict(int[] x, double[] itemWeights) {
        if (classifiers.isEmpty()) return defaultClass;
        if (itemWeights == null) return predict(x);
        Map<Integer, Double> votes = new HashMap<>();
        for (int t = 0; t < classifiers.size(); t++) {
            int pred = classifiers.get(t).predict(x, itemWeights);
            votes.merge(pred, weights.get(t), Double::sum);
        }
        int best = defaultClass;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : votes.entrySet()) {
            if (e.getValue() > bestScore) { bestScore = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    public Metrics scoreFull(int[][] X, double[][] itemWeights, int[] y) {
        if (itemWeights == null) return scoreFull(X, y);
        int[] preds = new int[X.length];
        for (int i = 0; i < X.length; i++) preds[i] = predict(X[i], itemWeights[i]);
        return Metrics.compute(preds, y);
    }

    public int getEnsembleSize() { return classifiers.size(); }
    public int getTotalRules() { int s = 0; for (CMARClassifier c : classifiers) s += c.getRuleCount(); return s; }
    public void setSeed(long s) { this.seed = s; }
    public void setBootstrapRatio(double r) { this.bootstrapRatio = Math.max(0.1, Math.min(1.0, r)); }
}
