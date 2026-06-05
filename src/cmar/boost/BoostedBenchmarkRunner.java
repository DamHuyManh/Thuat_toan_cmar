package cmar.boost;

import cmar.Metrics;
import cmar.benchmark.DataLoader;
import cmar.benchmark.UCIDatasets;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Benchmark runner for BoostedCMARClassifier on 26 UCI datasets.
 * 10-fold stratified CV, seed=42, paper minSup/minConf per dataset.
 *
 * <p>Args:
 * <ul>
 *   <li>--T=5     boosting rounds (default 5)</li>
 *   <li>--stratified=10  enable stratified coverage (recommended)</li>
 *   <li>--costSensitive  enable cost-sensitive voting (recommended)</li>
 *   <li>--verbose        per-round trace</li>
 *   <li>--out=results/boosted-T5.md</li>
 * </ul>
 */
public class BoostedBenchmarkRunner {

    static final String RESULTS_DIR = "results";
    static int T = 5;
    static String OUTFILE = null;
    static String METHOD = "boost"; // boost | bagging | bayesian | hyperbag
    static double FEATURE_SUBSET = 1.0;
    static double BOOTSTRAP_RATIO = 1.0;
    static boolean ADAPT_MINSUP = false;
    static double ADAPT_CAP = 3.0;       // upper cap for minSup divisor
    static String ADAPT_FORMULA = "cap"; // cap | sqrt | log
    static double MINSUP_SCALE = 1.0;     // multiply paper minSup; <1 = more rules
    static double MINCONF_OVERRIDE = -1.0; // <0 = use paper default; else override
    static int MAX_ANT_LEN = 4;            // max antecedent length per rule
    static int MAX_COVERAGE = 4;           // delta in paper
    static int TOP_K = 0;                  // 0 = vote all matched rules (paper-faithful)

    public static void main(String[] args) throws IOException {
        for (String a : args) {
            if (a.startsWith("--T=")) {
                try { T = Math.max(1, Integer.parseInt(a.substring(4))); } catch (Exception ignored) {}
            }
            if (a.startsWith("--stratified=")) {
                try { cmar.RulePruner.stratifiedTopN = Integer.parseInt(a.substring(13)); } catch (Exception ignored) {}
            }
            if (a.equalsIgnoreCase("--costSensitive")) cmar.CMARClassifier.useCostSensitive = true;
            if (a.equalsIgnoreCase("--verbose")) {
                BoostedCMARClassifier.verbose = true;
                BaggingCMARClassifier.verbose = true;
            }
            if (a.startsWith("--out=")) OUTFILE = a.substring(6);
            if (a.startsWith("--method=")) METHOD = a.substring(9).toLowerCase();
            if (a.startsWith("--featureSubset=")) {
                try { FEATURE_SUBSET = Double.parseDouble(a.substring(16)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--bootstrapRatio=")) {
                try { BOOTSTRAP_RATIO = Double.parseDouble(a.substring(17)); } catch (Exception ignored) {}
            }
            if (a.equalsIgnoreCase("--adaptMinSup")) ADAPT_MINSUP = true;
            if (a.startsWith("--adaptCap=")) {
                try { ADAPT_CAP = Double.parseDouble(a.substring(11)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--adaptFormula=")) ADAPT_FORMULA = a.substring(15).toLowerCase();
            if (a.startsWith("--minSupScale=")) {
                try { MINSUP_SCALE = Double.parseDouble(a.substring(14)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--minConfOverride=")) {
                try { MINCONF_OVERRIDE = Double.parseDouble(a.substring(18)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--maxAntLen=")) {
                try { MAX_ANT_LEN = Integer.parseInt(a.substring(12)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--maxCoverage=")) {
                try { MAX_COVERAGE = Integer.parseInt(a.substring(14)); } catch (Exception ignored) {}
            }
            if (a.startsWith("--topK=")) {
                try { TOP_K = Math.max(0, Integer.parseInt(a.substring(7))); } catch (Exception ignored) {}
            }
            if (a.equalsIgnoreCase("--deterministic")) cmar.FPGrowthOptimized.DETERMINISTIC = true;
            if (a.equalsIgnoreCase("--liftWeight")) cmar.CMARClassifier.useLiftWeight = true;
            if (a.equalsIgnoreCase("--weightConfLift")) cmar.CMARClassifier.useConfLiftWeight = true;
            if (a.equalsIgnoreCase("--weightChiLift")) cmar.CMARClassifier.useChiLiftWeight = true;
        }
        cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.IMPROVED);

        new File(RESULTS_DIR).mkdirs();
        System.out.println("=== Boosted CMAR Benchmark — T=" + T
                + " stratified=" + cmar.RulePruner.stratifiedTopN
                + " costSensitive=" + cmar.CMARClassifier.useCostSensitive + " ===\n");

        List<UCIDatasets.Dataset> datasets = UCIDatasets.getAllDatasets();
        List<Row> results = new ArrayList<>();
        for (UCIDatasets.Dataset ds : datasets) {
            if (ds == null) continue;
            try {
                System.out.println("Running: " + ds.name + " ...");
                Row r = runOne(ds);
                results.add(r);
                System.out.printf("  Done: acc=%.4f F1=%.4f R=%.4f (paper=%.2f%%) rounds_avg=%.1f%n",
                        r.acc, r.f1Macro, r.recallMacro, ds.paperCMARAccuracy, r.avgRoundsUsed);
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        writeReport(results);
        System.out.println("\nReport: " + (OUTFILE != null ? OUTFILE : RESULTS_DIR + "/boosted-summary.md"));
    }

    static Row runOne(UCIDatasets.Dataset ds) {
        int n = ds.numInstances;
        int folds = 10;
        long seed = 42;
        Random rng = new Random(seed);

        // Stratified k-fold (same as BenchmarkRunner)
        Map<Integer, List<Integer>> byClass = new HashMap<>();
        for (int i = 0; i < n; i++) byClass.computeIfAbsent(ds.labels[i], k -> new ArrayList<>()).add(i);
        for (List<Integer> list : byClass.values()) Collections.shuffle(list, rng);
        int[] foldAssign = new int[n];
        for (List<Integer> classIdx : byClass.values()) {
            for (int i = 0; i < classIdx.size(); i++) {
                foldAssign[classIdx.get(i)] = i % folds;
            }
        }
        int trainN = n * (folds - 1) / folds;
        int minSup = Math.max(2, (int) Math.round(ds.paperMinSupport * trainN * MINSUP_SCALE));
        double minConf = (MINCONF_OVERRIDE > 0) ? MINCONF_OVERRIDE : ds.paperMinConfidence;

        double accSum = 0, f1mSum = 0, prSum = 0, reSum = 0, f1wSum = 0;
        long trainTimeSum = 0;
        double roundsSum = 0;
        int rulesSum = 0;

        for (int f = 0; f < folds; f++) {
            List<Integer> trainIdx = new ArrayList<>(), testIdx = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (foldAssign[i] == f) testIdx.add(i);
                else trainIdx.add(i);
            }
            int[] trainArr = trainIdx.stream().mapToInt(Integer::intValue).toArray();
            int[] testArr = testIdx.stream().mapToInt(Integer::intValue).toArray();

            int[][] trainData, testData;
            int[] trainLabels, testLabels;
            if (ds.rawData != null) {
                DataLoader.FoldData fold = DataLoader.encodeFold(ds.rawData, trainArr, testArr);
                trainData = fold.trainTx; trainLabels = fold.trainLabels;
                testData = fold.testTx;   testLabels = fold.testLabels;
            } else {
                trainData = new int[trainIdx.size()][]; trainLabels = new int[trainIdx.size()];
                for (int i = 0; i < trainIdx.size(); i++) {
                    trainData[i] = ds.transactions[trainIdx.get(i)];
                    trainLabels[i] = ds.labels[trainIdx.get(i)];
                }
                testData = new int[testIdx.size()][]; testLabels = new int[testIdx.size()];
                for (int i = 0; i < testIdx.size(); i++) {
                    testData[i] = ds.transactions[testIdx.get(i)];
                    testLabels[i] = ds.labels[testIdx.get(i)];
                }
            }

            Metrics m;
            int rounds;
            int rules;
            long t0 = System.nanoTime();
            // Adaptive minSup based on imbalance — only when --adaptMinSup
            int minSupFold = minSup;
            if (ADAPT_MINSUP) {
                Map<Integer, Integer> classCnt = new HashMap<>();
                for (int lbl : trainLabels) classCnt.merge(lbl, 1, Integer::sum);
                int maxFreq = Collections.max(classCnt.values());
                int minFreq = Collections.min(classCnt.values());
                if (minFreq > 0 && (double) maxFreq / minFreq > 1.5) {
                    double imbR = (double) maxFreq / minFreq;
                    double divisor;
                    switch (ADAPT_FORMULA) {
                        case "sqrt":  divisor = Math.sqrt(imbR); break;
                        case "log":   divisor = Math.log(imbR) + 1.0; break;
                        case "cap":
                        default:      divisor = Math.min(ADAPT_CAP, imbR); break;
                    }
                    minSupFold = Math.max(2, (int) Math.round(minSup / Math.max(1.0, divisor)));
                }
            }
            if (METHOD.equals("bagging")) {
                BaggingCMARClassifier bag = new BaggingCMARClassifier(
                        T, minSupFold, minConf, 3.841, MAX_COVERAGE, 80000, MAX_ANT_LEN, TOP_K, FEATURE_SUBSET);
                bag.setSeed(42 + f);
                bag.setBootstrapRatio(BOOTSTRAP_RATIO);
                bag.fit(trainData, trainLabels);
                m = bag.scoreFull(testData, testLabels);
                rounds = bag.getEnsembleSize();
                rules = bag.getTotalRules();
            } else if (METHOD.equals("bayesian")) {
                BayesianCMARClassifier bay = new BayesianCMARClassifier(
                        minSupFold, minConf, 3.841, MAX_COVERAGE, 80000, MAX_ANT_LEN, TOP_K);
                bay.fit(trainData, trainLabels);
                m = bay.scoreFull(testData, testLabels);
                rounds = 1;
                rules = bay.getRuleCount();
            } else if (METHOD.equals("hyperbag")) {
                HyperRandomBaggingCMAR hb = new HyperRandomBaggingCMAR(
                        T, minSupFold, minConf, 3.841, MAX_COVERAGE, 80000, MAX_ANT_LEN, TOP_K);
                hb.setSeed(42 + f);
                hb.fit(trainData, trainLabels);
                m = hb.scoreFull(testData, testLabels);
                rounds = hb.getEnsembleSize();
                rules = hb.getTotalRules();
            } else {
                BoostedCMARClassifier bcmar = new BoostedCMARClassifier(
                        T, minSupFold, minConf, 3.841, MAX_COVERAGE, 80000, MAX_ANT_LEN, TOP_K);
                bcmar.setSeed(42 + f);
                bcmar.fit(trainData, trainLabels);
                m = bcmar.scoreFull(testData, testLabels);
                rounds = bcmar.getRoundsUsed();
                rules = bcmar.getTotalRulesAcrossRounds();
            }
            trainTimeSum += (System.nanoTime() - t0) / 1_000_000;

            accSum += m.accuracy;
            f1mSum += m.f1Macro;
            prSum += m.precisionMacro;
            reSum += m.recallMacro;
            f1wSum += m.f1Weighted;
            roundsSum += rounds;
            rulesSum += rules;
        }
        Row r = new Row();
        r.dataset = ds;
        r.acc = accSum / folds;
        r.f1Macro = f1mSum / folds;
        r.precMacro = prSum / folds;
        r.recallMacro = reSum / folds;
        r.f1Weighted = f1wSum / folds;
        r.avgTrainTime = trainTimeSum / folds;
        r.avgRoundsUsed = roundsSum / folds;
        r.avgRulesTotal = rulesSum / folds;
        return r;
    }

    static void writeReport(List<Row> rows) throws IOException {
        String file = OUTFILE != null ? OUTFILE : (RESULTS_DIR + "/boosted-T" + T + ".md");
        StringBuilder sb = new StringBuilder();
        sb.append("# Boosted CMAR — T=").append(T).append(" — Benchmark Report\n\n");
        sb.append("**Date**: ").append(LocalDate.now()).append("\n");
        sb.append("**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier\n");
        sb.append("**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4");
        if (cmar.RulePruner.stratifiedTopN > 0) sb.append(" stratified=").append(cmar.RulePruner.stratifiedTopN);
        if (cmar.CMARClassifier.useCostSensitive) sb.append(" costSensitive");
        sb.append("\n\n");
        sb.append("## Accuracy / F1 / Recall per dataset\n\n");
        sb.append("| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        double sumAcc = 0, sumF1 = 0, sumR = 0, sumP = 0, sumF1w = 0;
        double sumPaper = 0;
        int n = 0;
        for (Row r : rows) {
            double diff = r.acc * 100 - r.dataset.paperCMARAccuracy;
            sb.append(String.format(Locale.US,
                    "| %s | %.1f%% | **%.2f%%** | %s%.2f%% | %.4f | %.4f | %.1f | %d |%n",
                    r.dataset.name, r.dataset.paperCMARAccuracy, r.acc * 100,
                    diff >= 0 ? "+" : "", diff,
                    r.f1Macro, r.recallMacro, r.avgRoundsUsed, r.avgRulesTotal));
            sumAcc += r.acc; sumF1 += r.f1Macro; sumR += r.recallMacro;
            sumP += r.precMacro; sumF1w += r.f1Weighted;
            sumPaper += r.dataset.paperCMARAccuracy;
            n++;
        }
        if (n > 0) {
            double avgAcc = sumAcc / n, avgPaper = sumPaper / n;
            sb.append(String.format(Locale.US,
                    "| **Average** | **%.2f%%** | **%.2f%%** | **%s%.2f%%** | **%.4f** | **%.4f** | | |%n",
                    avgPaper, avgAcc * 100,
                    (avgAcc * 100 - avgPaper) >= 0 ? "+" : "",
                    avgAcc * 100 - avgPaper, sumF1 / n, sumR / n));
            sb.append("\n## Aggregate metrics\n\n");
            sb.append("| Metric | Boosted CMAR (T=").append(T).append(") |\n|---|---:|\n");
            sb.append(String.format(Locale.US, "| Accuracy | %.4f |%n", sumAcc / n));
            sb.append(String.format(Locale.US, "| Precision macro | %.4f |%n", sumP / n));
            sb.append(String.format(Locale.US, "| **Recall macro** | **%.4f** |%n", sumR / n));
            sb.append(String.format(Locale.US, "| **F1 macro** | **%.4f** |%n", sumF1 / n));
            sb.append(String.format(Locale.US, "| F1 weighted | %.4f |%n", sumF1w / n));
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
    }

    static class Row {
        UCIDatasets.Dataset dataset;
        double acc, f1Macro, precMacro, recallMacro, f1Weighted;
        double avgRoundsUsed;
        long avgTrainTime;
        int avgRulesTotal;
    }
}
