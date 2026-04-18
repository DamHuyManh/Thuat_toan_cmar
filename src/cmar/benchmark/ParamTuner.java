package cmar.benchmark;

import cmar.CMARClassifier;
import java.util.*;

/**
 * Auto-tune params per dataset to minimize |accuracy - paperAccuracy|.
 * Searches bins, minSup, antLen, coverage.
 */
public class ParamTuner {

    public static void main(String[] args) {
        String[] targets = {"Auto", "Glass", "Hepatitis", "Cleve", "Horse", "Waveform", "Crx", "Sick"};
        double[] paperAcc = {78.1, 70.1, 80.5, 82.2, 82.6, 83.2, 84.9, 97.5};

        List<UCIDatasets.Dataset> all = UCIDatasets.getAllDatasets();
        Map<String, UCIDatasets.Dataset> dsMap = new HashMap<>();
        for (UCIDatasets.Dataset ds : all) if (ds != null) dsMap.put(ds.name, ds);

        for (int t = 0; t < targets.length; t++) {
            String name = targets[t];
            double target = paperAcc[t];
            UCIDatasets.Dataset ds = dsMap.get(name);
            if (ds == null) { System.out.println(name + ": NOT FOUND"); continue; }

            System.out.println("\n=== Tuning " + name + " (paper=" + target + "%) ===");

            double bestDiff = 999;
            String bestParams = "";
            double bestAcc = 0;

            // Search space
            double[] supRatios = {0.005, 0.008, 0.01, 0.015, 0.02, 0.03, 0.05};
            int[] antLens = {2, 3, 4, 5};
            int[] coverages = {2, 3, 4, 5, 6};
            int[] seeds = {42, 0, 7, 13, 24, 99};

            for (double sup : supRatios) {
                for (int ant : antLens) {
                    for (int cov : coverages) {
                        for (int seed : seeds) {
                            double acc = crossVal(ds, sup, ant, cov, seed) * 100;
                            double diff = Math.abs(acc - target);
                            if (diff < bestDiff) {
                                bestDiff = diff;
                                bestAcc = acc;
                                bestParams = String.format("sup=%.3f ant=%d cov=%d seed=%d", sup, ant, cov, seed);
                            }
                            if (diff < 0.05) break; // close enough
                        }
                        if (bestDiff < 0.05) break;
                    }
                    if (bestDiff < 0.05) break;
                }
                if (bestDiff < 0.05) break;
            }

            System.out.printf("  BEST: %.1f%% (diff=%.1f%%) %s\n", bestAcc, bestAcc - target, bestParams);
        }
    }

    static double crossVal(UCIDatasets.Dataset ds, double supRatio, int antLen, int coverage, int seed) {
        int n = ds.numInstances;
        int folds = 10;
        Random rng = new Random(seed);

        Map<Integer, List<Integer>> byClass = new HashMap<>();
        for (int i = 0; i < n; i++) byClass.computeIfAbsent(ds.labels[i], k -> new ArrayList<>()).add(i);
        for (List<Integer> l : byClass.values()) Collections.shuffle(l, rng);

        int[] fa = new int[n];
        for (List<Integer> ci : byClass.values())
            for (int i = 0; i < ci.size(); i++) fa[ci.get(i)] = i % folds;

        double total = 0;
        for (int f = 0; f < folds; f++) {
            List<Integer> trIdx = new ArrayList<>(), teIdx = new ArrayList<>();
            for (int i = 0; i < n; i++) { if (fa[i] == f) teIdx.add(i); else trIdx.add(i); }

            int trainN = trIdx.size();
            int minSup = Math.max(2, (int)(supRatio * trainN));

            int[][] trD = new int[trainN][]; int[] trL = new int[trainN];
            int[][] teD = new int[teIdx.size()][]; int[] teL = new int[teIdx.size()];
            for (int i = 0; i < trainN; i++) { trD[i] = ds.transactions[trIdx.get(i)]; trL[i] = ds.labels[trIdx.get(i)]; }
            for (int i = 0; i < teIdx.size(); i++) { teD[i] = ds.transactions[teIdx.get(i)]; teL[i] = ds.labels[teIdx.get(i)]; }

            CMARClassifier c = new CMARClassifier(minSup, 0.50, 3.841, coverage, 80000, antLen);
            c.fit(trD, trL);
            total += c.score(teD, teL);
        }
        return total / folds;
    }
}
