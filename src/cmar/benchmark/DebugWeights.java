package cmar.benchmark;
import cmar.CMARClassifier;
import cmar.Rule;
import java.util.*;

/**
 * Debug: chạy 1 dataset, in ra weight thực của rules với mode khác nhau.
 * Mục đích: verify --liftWeight vs --weightChiLift CÓ thực sự cho weight khác nhau.
 */
public final class DebugWeights {

    public static void main(String[] args) {
        // Load Iris (small, easy to inspect)
        cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.IMPROVED);
        UCIDatasets.Dataset ds = UCIDatasets.loadIris();
        if (ds == null) {
            System.err.println("Iris failed to load!");
            return;
        }

        // Encode 1 fold (use ALL data as train for simplicity)
        int[] allIdx = new int[ds.numInstances];
        for (int i = 0; i < ds.numInstances; i++) allIdx[i] = i;
        DataLoader.FoldData fold = DataLoader.encodeFold(ds.rawData, allIdx, allIdx);

        System.out.println("=== TEST 1: --liftWeight (A) ===");
        CMARClassifier.useLiftWeight = true;
        CMARClassifier.useChiLiftWeight = false;
        CMARClassifier c1 = new CMARClassifier(2, 0.5, 3.841, 4, 80000, 4);
        c1.fit(fold.trainTx, fold.trainLabels);
        List<Rule> r1 = c1.getRules();
        System.out.println("  Total rules: " + r1.size());
        printTop5(r1, "Lift");

        System.out.println("");
        System.out.println("=== TEST 2: --weightChiLift (C) ===");
        CMARClassifier.useLiftWeight = false;
        CMARClassifier.useChiLiftWeight = true;
        CMARClassifier c2 = new CMARClassifier(2, 0.5, 3.841, 4, 80000, 4);
        c2.fit(fold.trainTx, fold.trainLabels);
        List<Rule> r2 = c2.getRules();
        System.out.println("  Total rules: " + r2.size());
        printTop5(r2, "ChiNorm × Lift");

        System.out.println("");
        System.out.println("=== COMPARE WEIGHTS PER RULE ===");
        // Find matching rules by antecedent + class
        Map<String, Rule> rMap = new HashMap<>();
        for (Rule r : r1) rMap.put(ruleKey(r), r);
        int identical = 0, different = 0;
        for (Rule r : r2) {
            Rule m = rMap.get(ruleKey(r));
            if (m == null) continue;
            if (Math.abs(m.getWeight() - r.getWeight()) < 1e-9) identical++;
            else different++;
        }
        System.out.println("  Rules với weight giống: " + identical);
        System.out.println("  Rules với weight khác: " + different);

        if (different > 0) {
            System.out.println("\n  → Weight THỰC SỰ KHÁC giữa 2 mode!");
            System.out.println("  → Nếu accuracy giống nhau, là do unanimity short-circuit + topK=3");
        } else {
            System.out.println("\n  ⚠️ Weight HOÀN TOÀN GIỐNG — code có thể có bug!");
        }
    }

    static String ruleKey(Rule r) {
        return Arrays.toString(r.getAntecedent()) + "->" + r.getClassLabel();
    }

    static void printTop5(List<Rule> rulesIn, String wName) {
        List<Rule> rules = new ArrayList<>(rulesIn);
        rules.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        System.out.println("  Top 5 rules by weight (" + wName + "):");
        for (int i = 0; i < Math.min(5, rules.size()); i++) {
            Rule r = rules.get(i);
            System.out.printf("    [%d] ant=%s class=%d conf=%.3f lift=%.3f chi=%.2f weight=%.4f%n",
                    i, Arrays.toString(r.getAntecedent()),
                    r.getClassLabel(), r.getConfidence(),
                    r.getLift(), r.getChiSquare(), r.getWeight());
        }
    }
}
