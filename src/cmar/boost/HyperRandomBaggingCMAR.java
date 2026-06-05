package cmar.boost;

import cmar.CMARClassifier;
import cmar.Metrics;
import java.util.*;

/**
 * HyperRandomBagging — Bagging với DIVERSITY qua RANDOMIZED HYPERPARAMETERS thay vì feature subset.
 *
 * <p>Mỗi bag t:
 * <ul>
 *   <li>Bootstrap sample (with replacement)</li>
 *   <li>Random minSup multiplier ∈ [0.5x, 1.5x]</li>
 *   <li>Random chi² threshold ∈ {2.706 (p=0.10), 3.841 (p=0.05), 5.024 (p=0.025)}</li>
 *   <li>Random maxAntecedentLen ∈ {3, 4, 5}</li>
 * </ul>
 *
 * <p>Vì sao novel? Tránh điểm yếu của feature subset (làm CMAR pattern mining fail)
 * mà vẫn có diversity sâu giữa các base classifiers.
 *
 * <p>Vote: weighted by OOB accuracy như Bagging classic.
 *
 * <p>Reference: gần với "Stochastic Hyperparameter Optimization Bagging"
 * nhưng áp dụng cho Associative Classification (lần đầu).
 */
public class HyperRandomBaggingCMAR {

    private final int T;
    private final int baseMinSup;
    private final double baseMinConf;
    private final int baseCoverage;
    private final int baseMaxRules;
    private final int topKGlobal;

    private final List<CMARClassifier> classifiers = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private int defaultClass = 0;
    private long seed = 42;
    public static boolean verbose = false;

    private static final double[] CHI_OPTIONS = {2.706, 3.841, 5.024};   // p=0.10, 0.05, 0.025
    private static final int[]    ANTLEN_OPTIONS = {3, 4, 5};
    private static final double[] MINSUP_MULT = {0.5, 0.7, 1.0, 1.3, 1.5};

    @SuppressWarnings("unused")
    public HyperRandomBaggingCMAR(int T, int minSup, double minConf,
                                   double chi, int coverage,
                                   int maxRules, int maxAntLen, int topKGlobal) {
        this.T = T;
        this.baseMinSup = minSup;
        this.baseMinConf = minConf;
        this.baseCoverage = coverage;
        this.baseMaxRules = maxRules;
        this.topKGlobal = topKGlobal;
        // chi and maxAntLen overridden per-bag via CHI_OPTIONS / ANTLEN_OPTIONS
    }

    public void fit(int[][] X, int[] y) {
        int N = X.length;
        if (N == 0) return;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int label : y) counts.merge(label, 1, Integer::sum);
        defaultClass = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        int K = counts.size();

        for (int t = 0; t < T; t++) {
            Random rng = new Random(seed + 100L * t);

            // Bootstrap sample
            int[][] X_t = new int[N][];
            int[] y_t = new int[N];
            boolean[] inBag = new boolean[N];
            for (int i = 0; i < N; i++) {
                int idx = rng.nextInt(N);
                X_t[i] = X[idx]; y_t[i] = y[idx]; inBag[idx] = true;
            }

            // Random hyperparameters
            double supMult = MINSUP_MULT[rng.nextInt(MINSUP_MULT.length)];
            int minSup = Math.max(2, (int) Math.round(baseMinSup * supMult));
            double chi = CHI_OPTIONS[rng.nextInt(CHI_OPTIONS.length)];
            int antLen = ANTLEN_OPTIONS[rng.nextInt(ANTLEN_OPTIONS.length)];

            // Train base CMAR with randomized hyperparameters
            CMARClassifier h = new CMARClassifier(minSup, baseMinConf, chi,
                    baseCoverage, baseMaxRules, antLen, topKGlobal);
            h.fit(X_t, y_t);

            // OOB accuracy as weight
            int oobCorrect = 0, oobTotal = 0;
            for (int i = 0; i < N; i++) {
                if (!inBag[i]) {
                    if (h.predict(X[i]) == y[i]) oobCorrect++;
                    oobTotal++;
                }
            }
            double oobAcc = oobTotal > 0 ? (double) oobCorrect / oobTotal : 0.5;
            double weight = Math.max(0.01, oobAcc - (1.0 / K));

            classifiers.add(h);
            weights.add(weight);

            if (verbose) {
                System.out.printf("  HyperBag %d: minSup=%d chi=%.3f antLen=%d  OOB=%.3f w=%.3f rules=%d%n",
                        t + 1, minSup, chi, antLen, oobAcc, weight, h.getRuleCount());
            }
        }
    }

    public int predict(int[] x) {
        return EnsembleUtils.weightedVote(classifiers, weights, x, defaultClass);
    }

    public int[] predict(int[][] X) {
        int[] o = new int[X.length];
        for (int i = 0; i < X.length; i++) o[i] = predict(X[i]);
        return o;
    }

    public Metrics scoreFull(int[][] X, int[] y) { return Metrics.compute(predict(X), y); }
    public int getEnsembleSize() { return classifiers.size(); }
    public int getTotalRules() { int s = 0; for (CMARClassifier c : classifiers) s += c.getRuleCount(); return s; }
    public void setSeed(long s) { this.seed = s; }
}
