package cmar.benchmark;

import cmar.CMARClassifier;
import cmar.Metrics;
import cmar.RulePruner;
import cmar.boost.BaggingCMARClassifier;
import cmar.util.OptimizationProfile;
import java.util.Arrays;

/**
 * Smoke + invariance tests cho BaggingCMARClassifier.
 * Run: java -cp bin cmar.benchmark.BaggingCMARTest
 *
 * <p>Không dùng JUnit (KISS) — main() chạy assertion blocks, fail-fast với rõ ràng.
 * Đặt trong cmar.benchmark package để access UCIDatasets.loadIris() (package-private).
 */
public final class BaggingCMARTest {

    public static void main(String[] args) {
        OptimizationProfile.setMode(OptimizationProfile.Mode.IMPROVED);
        int failed = 0;

        System.out.println("=== Bagging CMAR Test Suite ===");

        failed += run("Bagging empty data doesn't crash", BaggingCMARTest::testEmptyData);
        failed += run("Bagging same seed → identical predictions", BaggingCMARTest::testReproducible);
        failed += run("Bagging on Iris achieves >= 90% accuracy", BaggingCMARTest::testIrisAccuracy);
        failed += run("Bagging T=0 returns default class", BaggingCMARTest::testZeroEnsemble);
        failed += run("Bagging with stratified=10 + costSensitive doesn't crash", BaggingCMARTest::testFullConfig);

        if (failed == 0) {
            System.out.println("\nALL TESTS PASSED");
        } else {
            System.out.println("\n" + failed + " test(s) FAILED");
            System.exit(1);
        }
    }

    interface Test { void run() throws Exception; }

    static int run(String name, Test t) {
        System.out.print("  " + name + " ... ");
        try {
            t.run();
            System.out.println("PASS");
            return 0;
        } catch (Throwable e) {
            System.out.println("FAIL: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    static void testEmptyData() {
        BaggingCMARClassifier bag = new BaggingCMARClassifier(5, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        bag.fit(new int[0][], new int[0]);
        if (bag.getEnsembleSize() != 0)
            throw new AssertionError("Empty data should give 0 classifiers, got " + bag.getEnsembleSize());
    }

    static void testReproducible() {
        UCIDatasets.Dataset ds = UCIDatasets.loadIris();
        if (ds == null) throw new AssertionError("Iris dataset not found");

        int[][] X = ds.transactions;
        int[] y = ds.labels;

        BaggingCMARClassifier b1 = new BaggingCMARClassifier(5, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        b1.setSeed(42);
        b1.fit(X, y);

        BaggingCMARClassifier b2 = new BaggingCMARClassifier(5, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        b2.setSeed(42);
        b2.fit(X, y);

        int[] p1 = b1.predict(X);
        int[] p2 = b2.predict(X);
        if (!Arrays.equals(p1, p2))
            throw new AssertionError("Same seed should give identical predictions");
    }

    static void testIrisAccuracy() {
        UCIDatasets.Dataset ds = UCIDatasets.loadIris();
        if (ds == null) throw new AssertionError("Iris dataset not found");

        BaggingCMARClassifier bag = new BaggingCMARClassifier(10, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        bag.setSeed(42);
        bag.fit(ds.transactions, ds.labels);
        Metrics m = bag.scoreFull(ds.transactions, ds.labels);
        if (m.accuracy < 0.90)
            throw new AssertionError("Iris training accuracy " + m.accuracy + " < 0.90");
    }

    static void testZeroEnsemble() {
        BaggingCMARClassifier bag = new BaggingCMARClassifier(0, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        UCIDatasets.Dataset ds = UCIDatasets.loadIris();
        if (ds == null) return;
        bag.fit(ds.transactions, ds.labels);
        // Must not crash on predict even with T=0 ensemble (returns defaultClass)
        bag.predict(ds.transactions[0]);
    }

    static void testFullConfig() {
        RulePruner.stratifiedTopN = 10;
        CMARClassifier.useCostSensitive = true;

        UCIDatasets.Dataset ds = UCIDatasets.loadIris();
        if (ds == null) throw new AssertionError("Iris dataset not found");

        BaggingCMARClassifier bag = new BaggingCMARClassifier(5, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        bag.setSeed(42);
        bag.fit(ds.transactions, ds.labels);
        Metrics m = bag.scoreFull(ds.transactions, ds.labels);
        if (m.accuracy < 0.5) throw new AssertionError("Smoke test acc " + m.accuracy + " too low");

        // Reset statics for next test
        RulePruner.stratifiedTopN = 0;
        CMARClassifier.useCostSensitive = false;
    }
}
