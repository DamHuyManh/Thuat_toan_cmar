package cmar;

/** Manual verification test for Metrics.compute() — confirms F1/Recall/Precision are correct. */
public final class MetricsVerify {

    public static void main(String[] args) {
        System.out.println("=== TEST 1: Perfect prediction (all correct) ===");
        int[] truth1 = {0, 0, 1, 1, 2, 2};
        int[] pred1  = {0, 0, 1, 1, 2, 2};
        Metrics m1 = Metrics.compute(pred1, truth1);
        System.out.println("  Accuracy = " + m1.accuracy + "  (expected 1.0)");
        System.out.println("  F1 macro = " + m1.f1Macro + "  (expected 1.0)");
        System.out.println("  Recall   = " + m1.recallMacro + "  (expected 1.0)");
        System.out.println();

        System.out.println("=== TEST 2: All wrong (predict class 0 for everything) ===");
        int[] truth2 = {0, 0, 1, 1, 2, 2};
        int[] pred2  = {0, 0, 0, 0, 0, 0};
        Metrics m2 = Metrics.compute(pred2, truth2);
        // class 0: TP=2, FP=4, FN=0 → P=2/6=0.333, R=2/2=1.0, F1=2*0.333*1/(1.333)=0.5
        // class 1: TP=0, FP=0, FN=2 → P=0, R=0, F1=0
        // class 2: TP=0, FP=0, FN=2 → P=0, R=0, F1=0
        // Macro: P=0.111, R=0.333, F1=0.167
        System.out.println("  Accuracy = " + m2.accuracy + "  (expected 0.333)");
        System.out.println("  P macro  = " + m2.precisionMacro + "  (expected 0.111)");
        System.out.println("  R macro  = " + m2.recallMacro + "  (expected 0.333)");
        System.out.println("  F1 macro = " + m2.f1Macro + "  (expected 0.167)");
        System.out.println();

        System.out.println("=== TEST 3: Imbalanced binary (German-like) ===");
        // 7 "good" (0), 3 "bad" (1)
        // Predict all as "good"
        int[] truth3 = {0, 0, 0, 0, 0, 0, 0, 1, 1, 1};
        int[] pred3  = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Metrics m3 = Metrics.compute(pred3, truth3);
        // class 0: TP=7, FP=3, FN=0 → P=7/10=0.7, R=7/7=1.0, F1=2*0.7*1/(1.7)=0.8235
        // class 1: TP=0, FP=0, FN=3 → P=0, R=0, F1=0
        // Macro: P=0.35, R=0.5, F1=0.4118
        System.out.println("  Accuracy = " + m3.accuracy + "  (expected 0.7)");
        System.out.println("  P macro  = " + m3.precisionMacro + "  (expected 0.35)");
        System.out.println("  R macro  = " + m3.recallMacro + "  (expected 0.5)");
        System.out.println("  F1 macro = " + m3.f1Macro + "  (expected 0.412)");
        System.out.println();

        System.out.println("=== TEST 4: 80% correct, balanced ===");
        // 4 class 0, 4 class 1; predict 3+1 vs 1+3 = 6 correct out of 8
        int[] truth4 = {0, 0, 0, 0, 1, 1, 1, 1};
        int[] pred4  = {0, 0, 0, 1, 1, 1, 1, 0};
        Metrics m4 = Metrics.compute(pred4, truth4);
        // class 0: TP=3, FP=1, FN=1 → P=0.75, R=0.75, F1=0.75
        // class 1: TP=3, FP=1, FN=1 → P=0.75, R=0.75, F1=0.75
        // Macro: 0.75 everything
        System.out.println("  Accuracy = " + m4.accuracy + "  (expected 0.75)");
        System.out.println("  F1 macro = " + m4.f1Macro + "  (expected 0.75)");
        System.out.println();

        System.out.println("ALL TESTS DONE — verify manually above.");
    }
}
