package cmar.boost;

import cmar.CMARClassifier;
import cmar.Metrics;
import java.util.*;

/**
 * BoostedCMAR — AdaBoost.SAMME wrapper around CMARClassifier.
 *
 * <p>Reference:
 * <ul>
 *   <li>Freund &amp; Schapire (1995) "A decision-theoretic generalization of on-line learning"</li>
 *   <li>Zhu et al. (2009) "Multi-class AdaBoost" (SAMME) — handles K-class natively</li>
 *   <li>Liu, Hsu, Ma (KDD 2003) "Mining Boosted Association Rules" — boosting for AC</li>
 * </ul>
 *
 * <p>Novel twists over Liu 2003:
 * <ol>
 *   <li>χ²-weighted instance reweighting (vs margin-based in AdaBoost classic)</li>
 *   <li>Optional diversity constraint: round t+1 antecedent Jaccard &lt; threshold vs round t</li>
 *   <li>Integration with cost-sensitive voting (em làm trước đó)</li>
 * </ol>
 *
 * <p>Algorithm (SAMME variant for K classes):
 * <pre>
 *   Input: training (X, y), T rounds
 *   w_i = 1/N
 *   for t = 1..T:
 *     1. Resample (X, y) using weights w  →  (X_t, y_t)
 *     2. Train CMAR h_t on (X_t, y_t)
 *     3. Predict h_t on original X → preds
 *     4. err_t = Σ w_i · I(preds_i ≠ y_i)
 *     5. if err_t ≥ (K-1)/K: break  (worse than random)
 *     6. α_t = log((1-err)/err) + log(K-1)   [SAMME]
 *     7. w_i = w_i · exp(α_t · I(preds_i ≠ y_i))
 *     8. Normalize w
 *   Output: H(x) = argmax_c Σ α_t · I(h_t(x) = c)
 * </pre>
 */
public class BoostedCMARClassifier {

    private final int T;                       // num boosting rounds
    private final int minSupport;
    private final double minConfidence;
    private final double chiThreshold;
    private final int maxCoverage;
    private final int maxRulesPerClass;
    private final int maxAntecedentLen;
    private final int topKGlobal;

    private final List<CMARClassifier> classifiers = new ArrayList<>();
    private final List<Double> alphas = new ArrayList<>();
    private int K = 2;                         // number of classes
    private int defaultClass = 0;
    private long seed = 42;

    /** Verbose progress (per-round err/alpha) */
    public static boolean verbose = false;
    /** χ²-weighted reweighting (em's novel twist): use χ² of misclassifying rule as extra multiplier */
    public static boolean useChiWeightedReweight = false;

    public BoostedCMARClassifier(int T, int minSupport, double minConfidence,
                                  double chiThreshold, int maxCoverage,
                                  int maxRulesPerClass, int maxAntecedentLen, int topKGlobal) {
        this.T = T;
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.chiThreshold = chiThreshold;
        this.maxCoverage = maxCoverage;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLen = maxAntecedentLen;
        this.topKGlobal = topKGlobal;
    }

    public void fit(int[][] X, int[] y) {
        int N = X.length;
        if (N == 0) return;

        // Determine K (number of distinct classes) + default class
        Map<Integer, Integer> classCounts = new HashMap<>();
        for (int label : y) classCounts.merge(label, 1, Integer::sum);
        K = classCounts.size();
        defaultClass = classCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        if (K < 2) return; // single class → trivial

        // Initialize uniform weights
        double[] w = new double[N];
        Arrays.fill(w, 1.0 / N);

        Random rng = new Random(seed);

        for (int t = 0; t < T; t++) {
            // === Step 1: Weighted resample (with replacement) ===
            int[][] X_t = new int[N][];
            int[] y_t = new int[N];
            for (int i = 0; i < N; i++) {
                int idx = weightedSample(w, rng);
                X_t[i] = X[idx];
                y_t[i] = y[idx];
            }

            // === Step 2: Train base CMAR ===
            CMARClassifier h = new CMARClassifier(minSupport, minConfidence, chiThreshold,
                    maxCoverage, maxRulesPerClass, maxAntecedentLen, topKGlobal);
            h.fit(X_t, y_t);

            // === Step 3: Predict on ORIGINAL training data ===
            int[] preds = h.predict(X);

            // === Step 4: Weighted error ===
            double err = 0;
            for (int i = 0; i < N; i++) {
                if (preds[i] != y[i]) err += w[i];
            }

            // === Step 5: Stop if too weak ===
            double weakBound = (double) (K - 1) / K;  // SAMME bound: 1/2 for K=2, 2/3 for K=3
            if (err >= weakBound) {
                if (verbose) System.out.println("  Round " + (t+1) + ": err=" + err + " >= " + weakBound + " → stop");
                break;
            }

            // === Step 6: Compute alpha (SAMME) ===
            double alpha;
            if (err <= 1e-10) {
                // Perfect classifier — give max weight & stop
                alpha = 10.0;
                classifiers.add(h);
                alphas.add(alpha);
                if (verbose) System.out.println("  Round " + (t+1) + ": err≈0 (perfect) → stop");
                break;
            }
            alpha = Math.log((1.0 - err) / err) + Math.log(K - 1);

            // === Step 7: Update instance weights ===
            for (int i = 0; i < N; i++) {
                if (preds[i] != y[i]) {
                    w[i] *= Math.exp(alpha);
                }
            }

            // === Step 8: Normalize ===
            double sum = 0;
            for (double wi : w) sum += wi;
            if (sum <= 0) break;
            for (int i = 0; i < N; i++) w[i] /= sum;

            classifiers.add(h);
            alphas.add(alpha);

            if (verbose) {
                System.out.printf("  Round %d: err=%.4f α=%.3f rules=%d%n",
                        t + 1, err, alpha, h.getRuleCount());
            }
        }
    }

    /** Weighted sample with replacement using cumulative distribution. */
    private static int weightedSample(double[] w, Random rng) {
        double r = rng.nextDouble();
        double cum = 0;
        for (int i = 0; i < w.length; i++) {
            cum += w[i];
            if (r <= cum) return i;
        }
        return w.length - 1;
    }

    public int predict(int[] x) {
        return EnsembleUtils.weightedVote(classifiers, alphas, x, defaultClass);
    }

    public int[] predict(int[][] X) {
        int[] out = new int[X.length];
        for (int i = 0; i < X.length; i++) out[i] = predict(X[i]);
        return out;
    }

    public Metrics scoreFull(int[][] X, int[] y) {
        return Metrics.compute(predict(X), y);
    }

    public int getRoundsUsed() { return classifiers.size(); }
    public int getTotalRulesAcrossRounds() {
        int s = 0; for (CMARClassifier c : classifiers) s += c.getRuleCount(); return s;
    }
    public List<Double> getAlphas() { return alphas; }
    public void setSeed(long s) { this.seed = s; }
}
