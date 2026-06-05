package cmar.boost;

import cmar.CMARClassifier;
import cmar.Metrics;
import cmar.Rule;
import java.util.*;

/**
 * Bayesian CMAR — replaces sum-weight voting with Bayesian Model Averaging.
 *
 * <p>Standard CMAR voting: score(c) = Σ weight(rule) where rule predicts c.
 * Bayesian voting: P(c|x) ∝ P(c) · Π_{rule matches} P(rule|c) / P(rule)
 *
 * <p>Em compute log-posterior (numerical stability):
 * <pre>
 *   log P(c|x) = log P(c) + Σ_matched_rules [ log(conf · χ²Norm) if rule→c, else log(1-conf · χ²Norm) ]
 * </pre>
 *
 * <p>Independence assumption: rules are conditionally independent given class.
 * Violations expected but Naive Bayes works fine in practice (Domingos 1997).
 *
 * <p>Key innovation: χ²Norm acts as <b>rule reliability prior</b>:
 * - High χ² rule: posterior heavily moved by this rule
 * - Low χ² rule: posterior barely affected (acts like soft prior)
 *
 * <p>Reference:
 * <ul>
 *   <li>Domingos &amp; Pazzani (1997) "On the Optimality of the Simple Bayesian Classifier"</li>
 *   <li>Hoeting et al. (1999) "Bayesian Model Averaging"</li>
 * </ul>
 */
public class BayesianCMARClassifier {

    private CMARClassifier baseCmar;
    private final int minSupport;
    private final double minConfidence;
    private final double chiThreshold;
    private final int maxCoverage;
    private final int maxRulesPerClass;
    private final int maxAntecedentLen;
    private final int topKGlobal;

    private Map<Integer, Double> classLogPrior = new HashMap<>();
    private int defaultClass = 0;
    private double maxChiSq = 1.0;  // for normalization

    public BayesianCMARClassifier(int minSupport, double minConfidence,
                                   double chiThreshold, int maxCoverage,
                                   int maxRulesPerClass, int maxAntecedentLen, int topKGlobal) {
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.chiThreshold = chiThreshold;
        this.maxCoverage = maxCoverage;
        this.maxRulesPerClass = maxRulesPerClass;
        this.maxAntecedentLen = maxAntecedentLen;
        this.topKGlobal = topKGlobal;
    }

    public void fit(int[][] X, int[] y) {
        // Train standard CMAR base — reuse all the mining/pruning logic
        baseCmar = new CMARClassifier(minSupport, minConfidence, chiThreshold,
                maxCoverage, maxRulesPerClass, maxAntecedentLen, topKGlobal);
        baseCmar.fit(X, y);

        // Compute class log-prior P(c) with Laplace smoothing
        int N = y.length;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int label : y) counts.merge(label, 1, Integer::sum);
        int K = counts.size();
        defaultClass = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).get().getKey();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            double p = (e.getValue() + 1.0) / (N + K);
            classLogPrior.put(e.getKey(), Math.log(p));
        }

        // Compute max χ² for normalization
        for (Rule r : baseCmar.getRules()) {
            if (r.getChiSquare() > maxChiSq) maxChiSq = r.getChiSquare();
        }
    }

    public int predict(int[] x) {
        if (baseCmar == null) return defaultClass;
        List<Rule> matched = matchRules(x);
        if (matched.isEmpty()) return defaultClass;

        // Compute log-posterior per class via Bayesian Model Averaging
        Map<Integer, Double> logPost = new HashMap<>(classLogPrior);

        for (Rule r : matched) {
            int cRule = r.getClassLabel();
            double conf = r.getConfidence();
            double chiNorm = Math.min(1.0, r.getChiSquare() / maxChiSq);
            // reliability: rule with high χ² is very reliable → influence ↑
            // reliability ∈ [0, 1]
            double reliability = chiNorm;

            // For class predicted by rule: P(c|rule, match) = conf
            // For other classes: P(c'|rule, match) = (1 - conf) / (K - 1)
            int K = classLogPrior.size();
            double logPC = Math.log(Math.max(1e-9, conf));
            double logPCother = Math.log(Math.max(1e-9, (1.0 - conf) / Math.max(1, K - 1)));

            // Weighted log-likelihood update (reliability damps low-χ² rules)
            for (Integer c : classLogPrior.keySet()) {
                double upd = (c == cRule) ? logPC : logPCother;
                logPost.merge(c, reliability * upd, Double::sum);
            }
        }

        int best = defaultClass;
        double bestLog = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, Double> e : logPost.entrySet()) {
            if (e.getValue() > bestLog) { bestLog = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    private List<Rule> matchRules(int[] x) {
        // Reuse base CMAR's prediction infrastructure to find matched rules.
        // Note: in production we'd expose CRTree.findAllMatching publicly,
        // but for now we filter all rules manually.
        List<Rule> out = new ArrayList<>();
        Set<Integer> instItems = new HashSet<>();
        for (int it : x) instItems.add(it);
        for (Rule r : baseCmar.getRules()) {
            boolean match = true;
            for (int item : r.getAntecedent()) {
                if (!instItems.contains(item)) { match = false; break; }
            }
            if (match) out.add(r);
        }
        return out;
    }

    public int[] predict(int[][] X) {
        int[] o = new int[X.length];
        for (int i = 0; i < X.length; i++) o[i] = predict(X[i]);
        return o;
    }

    public Metrics scoreFull(int[][] X, int[] y) { return Metrics.compute(predict(X), y); }
    public int getRuleCount() { return baseCmar != null ? baseCmar.getRuleCount() : 0; }
}
