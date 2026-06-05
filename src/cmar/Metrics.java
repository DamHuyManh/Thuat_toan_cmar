package cmar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classification metrics: accuracy + macro-averaged Precision/Recall/F1.
 *
 * <p>Macro avg = unweighted mean across classes — fair for imbalanced data.
 * Each class c:
 *   Precision(c) = TP_c / (TP_c + FP_c)
 *   Recall(c)    = TP_c / (TP_c + FN_c)
 *   F1(c)        = 2 · P(c) · R(c) / (P(c) + R(c))</p>
 *
 * Macro: average over all classes (treats classes equally).
 * Weighted: average weighted by class size.
 */
public final class Metrics {

    public final double accuracy;
    public final double precisionMacro;
    public final double recallMacro;
    public final double f1Macro;
    public final double precisionWeighted;
    public final double recallWeighted;
    public final double f1Weighted;
    public final int n;

    private Metrics(double acc, double pM, double rM, double fM,
                    double pW, double rW, double fW, int n) {
        this.accuracy = acc;
        this.precisionMacro = pM;
        this.recallMacro = rM;
        this.f1Macro = fM;
        this.precisionWeighted = pW;
        this.recallWeighted = rW;
        this.f1Weighted = fW;
        this.n = n;
    }

    /** Compute all metrics from predictions + true labels. */
    public static Metrics compute(int[] predictions, int[] truths) {
        int n = truths.length;
        int correct = 0;

        // Per-class counts
        Map<Integer, Integer> tp = new HashMap<>();
        Map<Integer, Integer> fp = new HashMap<>();
        Map<Integer, Integer> fn = new HashMap<>();
        Map<Integer, Integer> support = new HashMap<>(); // # of true instances of class c
        Set<Integer> classes = new HashSet<>();

        for (int i = 0; i < n; i++) {
            int t = truths[i];
            int p = predictions[i];
            classes.add(t);
            classes.add(p);
            support.merge(t, 1, Integer::sum);
            if (p == t) {
                correct++;
                tp.merge(t, 1, Integer::sum);
            } else {
                fp.merge(p, 1, Integer::sum);
                fn.merge(t, 1, Integer::sum);
            }
        }

        double accuracy = n > 0 ? (double) correct / n : 0.0;
        double sumP = 0, sumR = 0, sumF = 0;
        double sumPw = 0, sumRw = 0, sumFw = 0;
        int classCount = classes.size();
        for (int c : classes) {
            int tp_c = tp.getOrDefault(c, 0);
            int fp_c = fp.getOrDefault(c, 0);
            int fn_c = fn.getOrDefault(c, 0);
            int sup_c = support.getOrDefault(c, 0);

            double p = (tp_c + fp_c) > 0 ? (double) tp_c / (tp_c + fp_c) : 0.0;
            double r = (tp_c + fn_c) > 0 ? (double) tp_c / (tp_c + fn_c) : 0.0;
            double f = (p + r) > 0 ? 2.0 * p * r / (p + r) : 0.0;

            sumP += p;
            sumR += r;
            sumF += f;

            // weighted by class support
            double w = n > 0 ? (double) sup_c / n : 0.0;
            sumPw += p * w;
            sumRw += r * w;
            sumFw += f * w;
        }
        double pM = classCount > 0 ? sumP / classCount : 0.0;
        double rM = classCount > 0 ? sumR / classCount : 0.0;
        double fM = classCount > 0 ? sumF / classCount : 0.0;
        return new Metrics(accuracy, pM, rM, fM, sumPw, sumRw, sumFw, n);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US,
                "acc=%.4f  P_macro=%.4f  R_macro=%.4f  F1_macro=%.4f",
                accuracy, precisionMacro, recallMacro, f1Macro);
    }
}
