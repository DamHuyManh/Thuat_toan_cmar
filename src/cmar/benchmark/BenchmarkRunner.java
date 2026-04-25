package cmar.benchmark;

import cmar.CMARClassifier;
import cmar.util.PhaseTimer;
import cmar.util.MemorySampler;
import java.io.*;
import java.util.*;

/**
 * Benchmark runner: runs CMAR on all UCI datasets from the original paper,
 * compares results, and generates markdown reports.
 */
public class BenchmarkRunner {

    static final String RESULTS_DIR = "results";
    static final double TUNE_TRIGGER_DIFF = 2.0; // tune when gap vs paper is larger than 2%
    static final int TUNE_FOLDS = 2; // fast tuning with 2 folds

    public static void main(String[] args) throws IOException {
        // Phase 05: parse --mode=baseline|improved (default=improved)
        String mode = "improved";
        for (String arg : args) {
            if (arg.startsWith("--mode=")) mode = arg.substring(7).toLowerCase();
        }
        if (mode.equals("baseline")) {
            cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.BASELINE);
            System.out.println("=== CMAR Benchmark — BASELINE (original algorithm) ===\n");
        } else {
            cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.IMPROVED);
            System.out.println("=== CMAR Benchmark — IMPROVED (inverted index + BitSet AND) ===\n");
        }

        new File(RESULTS_DIR).mkdirs();

        List<UCIDatasets.Dataset> datasets = UCIDatasets.getAllDatasets();
        List<DatasetResult> results = new ArrayList<>();

        for (UCIDatasets.Dataset ds : datasets) {
            if (ds == null) continue;
            System.out.println("Running: " + ds.name + " ...");
            try {
                DatasetResult result = runBenchmark(ds);
                results.add(result);
                writeDatasetReport(result);
                System.out.println("  Done: accuracy=" + String.format("%.1f%%", result.accuracy * 100)
                        + " (paper=" + String.format("%.1f%%", ds.paperCMARAccuracy) + ")");
            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
            }
        }

        // Write summary report
        writeSummaryReport(results);
        writeProfilingCsv(results);
        writeProfilingReport(results);
        System.out.println("\nAll reports saved to " + RESULTS_DIR + "/");
    }

    /** Phase 01 — emit CSV profiling (time per phase + peak memory). */
    static void writeProfilingCsv(List<DatasetResult> results) throws IOException {
        String f = RESULTS_DIR + "/profiling-metrics.csv";
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("dataset,instances,attrs,classes,accuracy,trainMs,mineMs,pruneMs,chiSqMs,g2sMs,covMs,bmapMs,indexMs,predictMs,peakMemMB,rulesMined,rulesPruned\n");
            for (DatasetResult r : results) {
                fw.write(String.format(Locale.US,
                        "%s,%d,%d,%d,%.4f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d%n",
                        r.dataset.name, r.dataset.numInstances, r.dataset.numAttributes,
                        r.dataset.numClasses, r.accuracy,
                        r.avgTrainTimeMs, r.avgMineMs, r.avgPruneMs,
                        r.avgChiSqMs, r.avgG2SMs, r.avgCovMs, r.avgBmapMs,
                        r.avgIndexMs, r.avgPredictTimeMs,
                        r.peakMemMB, r.avgRulesMined, r.avgRulesPruned));
            }
        }
    }

    /** Phase 01 — emit markdown profiling report. */
    static void writeProfilingReport(List<DatasetResult> results) throws IOException {
        String f = RESULTS_DIR + "/profiling-metrics.md";
        StringBuilder sb = new StringBuilder();
        sb.append("# CMAR Profiling Metrics — Baseline\n\n");
        sb.append("**Date:** 2026-04-23\n");
        sb.append("**Source:** Phase 01 Baseline Measurement Infrastructure\n\n");
        sb.append("## Per-phase timing + peak memory\n\n");
        sb.append("| Dataset | N | Train (ms) | Mine (ms) | Prune (ms) | ChiSq (ms) | G2S (ms) | Cov (ms) | Bmap (ms) | Index (ms) | Predict (ms) | Peak MB | Rules (raw→pruned) |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (DatasetResult r : results) {
            sb.append(String.format(Locale.US,
                    "| %s | %d | %d | %.1f | %.1f | %.1f | %.1f | %.1f | %.1f | %.1f | %d | %d | %d→%d |%n",
                    r.dataset.name, r.dataset.numInstances,
                    r.avgTrainTimeMs, r.avgMineMs, r.avgPruneMs,
                    r.avgChiSqMs, r.avgG2SMs, r.avgCovMs, r.avgBmapMs,
                    r.avgIndexMs, r.avgPredictTimeMs,
                    r.peakMemMB, r.avgRulesMined, r.avgRulesPruned));
        }
        // Aggregates
        double totMine=0, totPrune=0, totTrain=0; long totMem=0;
        for (DatasetResult r : results) {
            totMine += r.avgMineMs; totPrune += r.avgPruneMs;
            totTrain += r.avgTrainTimeMs; totMem += r.peakMemMB;
        }
        sb.append(String.format(Locale.US,
                "\n## Aggregate\n\n- Total train (sum): %.0f ms\n- Total mining: %.0f ms (%.0f%% of train)\n- Total pruning: %.0f ms (%.0f%% of train)\n- Average peak mem: %d MB\n",
                totTrain, totMine, 100.0*totMine/Math.max(1,totTrain),
                totPrune, 100.0*totPrune/Math.max(1,totTrain),
                totMem/Math.max(1,results.size())));
        try (FileWriter fw = new FileWriter(f)) { fw.write(sb.toString()); }
    }

    /**
     * Run cross-validation on a dataset.
     * Uses 10-fold for larger datasets, 5-fold for small ones (<150 instances).
     */
    static DatasetResult runBenchmark(UCIDatasets.Dataset ds) {
        int n = ds.numInstances;
        int folds = 10;

        // Stratified k-fold: group indices by class, then distribute evenly
        long seed = 42;
        Random rng = new Random(seed);
        Map<Integer, List<Integer>> byClass = new HashMap<>();
        for (int i = 0; i < n; i++)
            byClass.computeIfAbsent(ds.labels[i], k -> new ArrayList<>()).add(i);
        // Shuffle within each class
        for (List<Integer> list : byClass.values()) Collections.shuffle(list, rng);

        // Assign each instance to a fold, cycling through classes
        int[] foldAssignment = new int[n];
        for (List<Integer> classIndices : byClass.values()) {
            for (int i = 0; i < classIndices.size(); i++) {
                foldAssignment[classIndices.get(i)] = i % folds;
            }
        }

        int trainN = n * (folds - 1) / folds;
        ParamConfig base = ParamConfig.base(ds, trainN);
        EvalResult baseEval = evaluateConfig(ds, folds, foldAssignment, base, folds);

        EvalResult best = baseEval;
        double baseDiff = Math.abs(baseEval.accuracy * 100 - ds.paperCMARAccuracy);
        // Skip tuning for very small datasets or high-dimensional ones (too slow)
        boolean canTune = false; // Paper dùng CÙNG params cho tất cả datasets
        if (canTune && baseDiff > TUNE_TRIGGER_DIFF) {
            ParamConfig tuned = tuneConfig(ds, folds, foldAssignment, base);
            EvalResult tunedEval = evaluateConfig(ds, folds, foldAssignment, tuned, folds);

            double tunedDiff = Math.abs(tunedEval.accuracy * 100 - ds.paperCMARAccuracy);
            if (tunedDiff < baseDiff || (Math.abs(tunedDiff - baseDiff) < 1e-9 && tunedEval.accuracy > baseEval.accuracy)) {
                best = tunedEval;
            }
        }

        DatasetResult result = new DatasetResult();
        result.dataset = ds;
        result.accuracy = best.accuracy;
        result.foldAccuracies = best.foldAccuracies;
        result.avgTrainTimeMs = best.avgTrainTimeMs;
        result.avgPredictTimeMs = best.avgPredictTimeMs;
        result.avgRulesMined = best.avgRulesMined;
        result.avgRulesPruned = best.avgRulesPruned;
        result.minSupport = best.minSupport;
        result.minConfidence = best.minConfidence;
        result.avgMineMs = best.avgMineMs;
        result.avgPruneMs = best.avgPruneMs;
        result.avgChiSqMs = best.avgChiSqMs;
        result.avgG2SMs = best.avgG2SMs;
        result.avgCovMs = best.avgCovMs;
        result.avgBmapMs = best.avgBmapMs;
        result.avgIndexMs = best.avgIndexMs;
        result.peakMemMB = best.peakMemMB;
        return result;
    }

    private static EvalResult evaluateConfig(UCIDatasets.Dataset ds, int folds, int[] foldAssignment, ParamConfig cfg, int evalFoldCount) {
        int n = ds.numInstances;
        double totalAccuracy = 0;
        long totalTrainTime = 0;
        long totalPredictTime = 0;
        int totalRulesMined = 0;
        int totalRulesPruned = 0;
        double[] foldAccuracies = new double[folds];
        // Phase 01 profiling accumulators
        double result_mineMs = 0, result_pruneMs = 0, result_chiSqMs = 0;
        double result_g2sMs = 0, result_covMs = 0, result_bmapMs = 0, result_indexMs = 0;
        long result_peakMB = 0;

        int actualFolds = Math.max(1, Math.min(folds, evalFoldCount));
        for (int f = 0; f < actualFolds; f++) {
            // Split using stratified assignment
            List<Integer> trainIdx = new ArrayList<>();
            List<Integer> testIdx = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (foldAssignment[i] == f) testIdx.add(i);
                else trainIdx.add(i);
            }

            int trainSize = trainIdx.size();
            int testSize = testIdx.size();

            int[][] trainData;
            int[] trainLabels;
            int[][] testData;
            int[] testLabels;

            if (ds.rawData != null) {
                // Paper-faithful: MDL cut points học CHỈ từ train fold (không leak test)
                int[] trainIdxArr = trainIdx.stream().mapToInt(Integer::intValue).toArray();
                int[] testIdxArr  = testIdx.stream().mapToInt(Integer::intValue).toArray();
                DataLoader.FoldData fold = DataLoader.encodeFold(ds.rawData, trainIdxArr, testIdxArr);
                trainData   = fold.trainTx;
                trainLabels = fold.trainLabels;
                testData    = fold.testTx;
                testLabels  = fold.testLabels;
            } else {
                trainData   = new int[trainSize][];
                trainLabels = new int[trainSize];
                testData    = new int[testSize][];
                testLabels  = new int[testSize];
                for (int i = 0; i < trainSize; i++) {
                    trainData[i]   = ds.transactions[trainIdx.get(i)];
                    trainLabels[i] = ds.labels[trainIdx.get(i)];
                }
                for (int i = 0; i < testSize; i++) {
                    testData[i]   = ds.transactions[testIdx.get(i)];
                    testLabels[i] = ds.labels[testIdx.get(i)];
                }
            }

            // Train - cap rules for high-dimensional datasets
            CMARClassifier cmar = new CMARClassifier(
                    cfg.minSupport, cfg.minConfidence, cfg.chiThreshold,
                    cfg.maxCoverageCount, cfg.maxRules, cfg.maxAntecedentLen);

            PhaseTimer.reset();
            MemorySampler mem = new MemorySampler(20);
            mem.start();
            long t0 = System.nanoTime();
            cmar.fit(trainData, trainLabels);
            long trainTime = (System.nanoTime() - t0) / 1_000_000;

            // Predict
            long t1 = System.nanoTime();
            double acc = cmar.score(testData, testLabels);
            long predictTime = (System.nanoTime() - t1) / 1_000_000;
            mem.stop();

            // Aggregate phase timings
            result_mineMs   += PhaseTimer.getMillis("mining");
            result_pruneMs  += PhaseTimer.getMillis("pruning");
            result_chiSqMs  += PhaseTimer.getMillis("prune_chisquare");
            result_g2sMs    += PhaseTimer.getMillis("prune_g2s");
            result_covMs    += PhaseTimer.getMillis("prune_coverage");
            result_bmapMs   += PhaseTimer.getMillis("prune_bitmap");
            result_indexMs  += PhaseTimer.getMillis("indexing");
            result_peakMB    = Math.max(result_peakMB, mem.deltaMB());

            foldAccuracies[f] = acc;
            totalAccuracy += acc;
            totalTrainTime += trainTime;
            totalPredictTime += predictTime;
            totalRulesMined += cmar.getTotalRulesMined();
            totalRulesPruned += cmar.getTotalRulesAfterPrune();
        }

        EvalResult result = new EvalResult();
        result.accuracy = totalAccuracy / actualFolds;
        result.foldAccuracies = foldAccuracies;
        result.avgTrainTimeMs = totalTrainTime / actualFolds;
        result.avgPredictTimeMs = totalPredictTime / actualFolds;
        result.avgRulesMined = totalRulesMined / actualFolds;
        result.avgRulesPruned = totalRulesPruned / actualFolds;
        result.minSupport = cfg.minSupport;
        result.minConfidence = cfg.minConfidence;
        result.avgMineMs     = result_mineMs / actualFolds;
        result.avgPruneMs    = result_pruneMs / actualFolds;
        result.avgChiSqMs    = result_chiSqMs / actualFolds;
        result.avgG2SMs      = result_g2sMs / actualFolds;
        result.avgCovMs      = result_covMs / actualFolds;
        result.avgBmapMs     = result_bmapMs / actualFolds;
        result.avgIndexMs    = result_indexMs / actualFolds;
        result.peakMemMB     = result_peakMB;
        return result;
    }

    private static ParamConfig tuneConfig(UCIDatasets.Dataset ds, int folds, int[] foldAssignment, ParamConfig base) {
        ParamConfig bestCfg = base;
        EvalResult bestEval = evaluateConfig(ds, folds, foldAssignment, base, TUNE_FOLDS);
        double bestDiff = Math.abs(bestEval.accuracy * 100 - ds.paperCMARAccuracy);

        // Stage 1: search support/confidence around paper defaults.
        double[] supportScales = {0.90, 1.00, 1.10};
        double[] confDeltas = {-0.05, 0.0, 0.05};
        for (double scale : supportScales) {
            for (double delta : confDeltas) {
                ParamConfig candidate = base.with(
                        clamp(base.minSupportRatio * scale, 0.005, 0.20),
                        base.minConfidence + delta,
                        base.chiThreshold,
                        base.maxCoverageCount,
                        base.maxAntecedentLen,
                        base.maxRules,
                        ds.numInstances,
                        folds);
                EvalResult eval = evaluateConfig(ds, folds, foldAssignment, candidate, TUNE_FOLDS);
                double diff = Math.abs(eval.accuracy * 100 - ds.paperCMARAccuracy);
                if (diff < bestDiff || (Math.abs(diff - bestDiff) < 1e-9 && eval.accuracy > bestEval.accuracy)) {
                    bestDiff = diff;
                    bestEval = eval;
                    bestCfg = candidate;
                }
            }
        }

        // Stage 2: local search over pruning/model complexity near stage-1 best.
        double[] chiThresholds = {3.841};
        int[] coverages = {3, 4};
        int[] antLens = {3, 4, 5};
        for (double chi : chiThresholds) {
            for (int coverage : coverages) {
                for (int antLen : antLens) {
                    ParamConfig candidate = bestCfg.with(
                            bestCfg.minSupportRatio,
                            bestCfg.minConfidence,
                            chi,
                            coverage,
                            antLen,
                            bestCfg.maxRules,
                            ds.numInstances,
                            folds);
                    EvalResult eval = evaluateConfig(ds, folds, foldAssignment, candidate, TUNE_FOLDS);
                    double diff = Math.abs(eval.accuracy * 100 - ds.paperCMARAccuracy);
                    if (diff < bestDiff || (Math.abs(diff - bestDiff) < 1e-9 && eval.accuracy > bestEval.accuracy)) {
                        bestDiff = diff;
                        bestEval = eval;
                        bestCfg = candidate;
                    }
                }
            }
        }

        return bestCfg;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Write individual dataset report.
     */
    static void writeDatasetReport(DatasetResult r) throws IOException {
        String filename = RESULTS_DIR + "/" + r.dataset.name.toLowerCase().replace(" ", "-") + "-report.md";
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(r.dataset.name).append(" - CMAR Benchmark Report\n\n");
        sb.append("## Dataset Info\n");
        sb.append("| Property | Value |\n|---|---|\n");
        sb.append("| Description | ").append(r.dataset.description).append(" |\n");
        sb.append("| Instances | ").append(r.dataset.numInstances).append(" |\n");
        sb.append("| Attributes | ").append(r.dataset.numAttributes).append(" |\n");
        sb.append("| Classes | ").append(r.dataset.numClasses).append(" |\n\n");

        sb.append("## Parameters\n");
        sb.append("| Parameter | Value |\n|---|---|\n");
        sb.append("| Min Support (absolute) | ").append(r.minSupport).append(" |\n");
        sb.append("| Min Support (ratio) | ").append(String.format("%.2f", r.dataset.paperMinSupport)).append(" |\n");
        sb.append("| Min Confidence | ").append(String.format("%.2f", r.minConfidence)).append(" |\n");
        sb.append("| Chi-Square Threshold | 3.841 (p=0.05) |\n");
        sb.append("| Max Coverage Count | 3 |\n");
        sb.append("| Cross-Validation | 10-fold |\n\n");

        sb.append("## Results\n\n");
        sb.append("### Our CMAR vs Paper Results\n");
        sb.append("| Classifier | Accuracy |\n|---|---|\n");
        sb.append("| **Our CMAR (Java)** | **").append(String.format("%.1f%%", r.accuracy * 100)).append("** |\n");
        sb.append("| Paper CMAR | ").append(String.format("%.1f%%", r.dataset.paperCMARAccuracy)).append(" |\n");
        sb.append("| Paper CBA | ").append(String.format("%.1f%%", r.dataset.paperCBAAccuracy)).append(" |\n");
        sb.append("| Paper C4.5 | ").append(String.format("%.1f%%", r.dataset.paperC45Accuracy)).append(" |\n\n");

        double diff = r.accuracy * 100 - r.dataset.paperCMARAccuracy;
        sb.append("**Difference vs Paper CMAR:** ");
        if (diff >= 0) sb.append("+");
        sb.append(String.format("%.1f%%", diff)).append("\n\n");

        sb.append("### Per-Fold Accuracy\n");
        sb.append("| Fold | Accuracy |\n|---|---|\n");
        for (int i = 0; i < r.foldAccuracies.length; i++) {
            sb.append("| Fold ").append(i + 1).append(" | ")
              .append(String.format("%.1f%%", r.foldAccuracies[i] * 100)).append(" |\n");
        }
        sb.append("| **Average** | **").append(String.format("%.1f%%", r.accuracy * 100)).append("** |\n\n");

        sb.append("### Performance\n");
        sb.append("| Metric | Value |\n|---|---|\n");
        sb.append("| Avg Training Time | ").append(r.avgTrainTimeMs).append(" ms |\n");
        sb.append("| Avg Prediction Time | ").append(r.avgPredictTimeMs).append(" ms |\n");
        sb.append("| Avg Rules Mined | ").append(r.avgRulesMined).append(" |\n");
        sb.append("| Avg Rules After Pruning | ").append(r.avgRulesPruned).append(" |\n");
        sb.append("| Pruning Ratio | ");
        if (r.avgRulesMined > 0) {
            sb.append(String.format("%.1f%%", (1.0 - (double)r.avgRulesPruned / r.avgRulesMined) * 100));
        } else {
            sb.append("N/A");
        }
        sb.append(" |\n");

        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(sb.toString());
        }
    }

    /**
     * Write overall summary report.
     */
    static void writeSummaryReport(List<DatasetResult> results) throws IOException {
        String filename = RESULTS_DIR + "/summary-report.md";
        StringBuilder sb = new StringBuilder();

        sb.append("# CMAR Benchmark Summary Report\n\n");
        sb.append("**Date:** 2026-03-12\n\n");
        sb.append("**Reference Paper:** Li, Han, Pei. \"CMAR: Accurate and Efficient Classification ");
        sb.append("Based on Multiple Class-Association Rules\" (IEEE ICDM 2001)\n\n");
        sb.append("**Implementation:** Java (optimized with bitmap matching, hash-indexed CR-tree, ");
        sb.append("chi-square + coverage pruning)\n\n");
        sb.append("**Evaluation:** 10-fold cross-validation\n\n");

        // Main comparison table
        sb.append("## Accuracy Comparison\n\n");
        sb.append("| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |\n");
        sb.append("|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|\n");

        double totalOurs = 0, totalPaper = 0, totalCBA = 0, totalC45 = 0;
        int count = 0;
        for (DatasetResult r : results) {
            double ours = r.accuracy * 100;
            double diff = ours - r.dataset.paperCMARAccuracy;
            sb.append(String.format("| %s | %d | %d | %d | **%.1f%%** | %.1f%% | %.1f%% | %.1f%% | %s%.1f%% |\n",
                    r.dataset.name, r.dataset.numInstances, r.dataset.numAttributes, r.dataset.numClasses,
                    ours, r.dataset.paperCMARAccuracy, r.dataset.paperCBAAccuracy, r.dataset.paperC45Accuracy,
                    diff >= 0 ? "+" : "", diff));
            totalOurs += ours;
            totalPaper += r.dataset.paperCMARAccuracy;
            totalCBA += r.dataset.paperCBAAccuracy;
            totalC45 += r.dataset.paperC45Accuracy;
            count++;
        }
        double avgDiff = totalOurs / count - totalPaper / count;
        sb.append(String.format("| **Average** | | | | **%.1f%%** | %.1f%% | %.1f%% | %.1f%% | %s%.1f%% |\n\n",
                totalOurs / count, totalPaper / count, totalCBA / count, totalC45 / count,
                avgDiff >= 0 ? "+" : "", avgDiff));

        // Performance table
        sb.append("## Performance Metrics\n\n");
        sb.append("| Dataset | Train Time | Predict Time | Rules Mined | Rules Pruned | Prune Ratio |\n");
        sb.append("|---------|-----------|-------------|-------------|-------------|-------------|\n");
        for (DatasetResult r : results) {
            double pruneRatio = (r.avgRulesMined > 0)
                    ? (1.0 - (double)r.avgRulesPruned / r.avgRulesMined) * 100 : 0;
            sb.append(String.format("| %s | %d ms | %d ms | %d | %d | %.1f%% |\n",
                    r.dataset.name, r.avgTrainTimeMs, r.avgPredictTimeMs,
                    r.avgRulesMined, r.avgRulesPruned, pruneRatio));
        }

        sb.append("\n## Parameters Used\n\n");
        sb.append("| Dataset | Min Support (ratio) | Min Support (abs) | Min Confidence |\n");
        sb.append("|---------|--------------------|--------------------|----------------|\n");
        for (DatasetResult r : results) {
            sb.append(String.format("| %s | %.2f | %d | %.2f |\n",
                    r.dataset.name, r.dataset.paperMinSupport, r.minSupport, r.minConfidence));
        }

        sb.append("\n## Key Observations\n\n");
        int wins = 0, ties = 0, losses = 0;
        for (DatasetResult r : results) {
            double diff = r.accuracy * 100 - r.dataset.paperCMARAccuracy;
            if (diff > 0.5) wins++;
            else if (diff < -0.5) losses++;
            else ties++;
        }
        sb.append("- **Wins** (our > paper by >0.5%): ").append(wins).append("/").append(count).append("\n");
        sb.append("- **Ties** (within 0.5%): ").append(ties).append("/").append(count).append("\n");
        sb.append("- **Losses** (our < paper by >0.5%): ").append(losses).append("/").append(count).append("\n");
        sb.append("- **Average accuracy difference:** ").append(String.format("%s%.1f%%", avgDiff >= 0 ? "+" : "", avgDiff)).append("\n\n");

        sb.append("## Optimizations Applied\n\n");
        sb.append("1. **Bitmap rule matching** - bitwise AND for O(1) antecedent subset testing\n");
        sb.append("2. **Hash-indexed CR-tree** - class-partitioned with first-item pruning\n");
        sb.append("3. **Chi-square pruning (CSP)** - removes statistically insignificant rules (p<0.05)\n");
        sb.append("4. **Database coverage pruning (DCP)** - eliminates redundant rules\n");
        sb.append("5. **Single-path FP-tree optimization** - direct subset enumeration\n");
        sb.append("6. **Weighted voting** - weight = chi² × confidence, top-5 per class\n");
        sb.append("7. **Per-class adaptive minSupport** - rare classes (≤10 instances) use support floor of 1\n");
        sb.append("8. **Max antecedent length** - capped at 4 items to reduce noise\n");

        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(sb.toString());
        }
    }

    static class DatasetResult {
        UCIDatasets.Dataset dataset;
        double accuracy;
        double[] foldAccuracies;
        long avgTrainTimeMs;
        long avgPredictTimeMs;
        int avgRulesMined;
        int avgRulesPruned;
        int minSupport;
        double minConfidence;
        // Phase 01 profiling
        double avgMineMs, avgPruneMs, avgChiSqMs, avgG2SMs, avgCovMs, avgBmapMs, avgIndexMs;
        long peakMemMB;
    }

    static class EvalResult {
        double accuracy;
        double[] foldAccuracies;
        long avgTrainTimeMs;
        long avgPredictTimeMs;
        int avgRulesMined;
        int avgRulesPruned;
        int minSupport;
        double minConfidence;
        // Phase 01 profiling
        double avgMineMs, avgPruneMs, avgChiSqMs, avgG2SMs, avgCovMs, avgBmapMs, avgIndexMs;
        long peakMemMB;
    }

    static class ParamConfig {
        final double minSupportRatio;
        final int minSupport;
        final double minConfidence;
        final double chiThreshold;
        final int maxCoverageCount;
        final int maxRules;
        final int maxAntecedentLen;

        ParamConfig(double minSupportRatio, int minSupport, double minConfidence,
                    double chiThreshold, int maxCoverageCount, int maxRules, int maxAntecedentLen) {
            this.minSupportRatio = minSupportRatio;
            this.minSupport = minSupport;
            this.minConfidence = minConfidence;
            this.chiThreshold = chiThreshold;
            this.maxCoverageCount = maxCoverageCount;
            this.maxRules = maxRules;
            this.maxAntecedentLen = maxAntecedentLen;
        }

        static ParamConfig base(UCIDatasets.Dataset ds, int trainN) {
            // PAPER SETUP: dùng minSup/minConf theo paper per-dataset (Table 3),
            // giữ chi²=3.841 (p=0.05), delta(coverage)=4, maxAntLen=4 cố định.
            double minSupRatio = ds.paperMinSupport;
            double minConf = ds.paperMinConfidence;
            int minSup = Math.max(2, (int)(minSupRatio * trainN));
            double chi = 3.841;
            int coverage = 4; // delta=4 cho accuracy tốt hơn
            int antLen = 4;
            return new ParamConfig(minSupRatio, minSup, minConf, chi, coverage, 80000, antLen);
        }

        ParamConfig with(double minSupportRatio, double minConfidence,
                         double chiThreshold, int maxCoverageCount,
                         int maxAntecedentLen, int maxRules,
                         int numInstances, int folds) {
            double safeConf = clamp(minConfidence, 0.30, 0.95);
            int trainN = Math.max(1, numInstances * (folds - 1) / folds);
            int minSup = Math.max(2, (int)(minSupportRatio * trainN));
            return new ParamConfig(minSupportRatio, minSup, safeConf,
                    chiThreshold, maxCoverageCount, maxRules, maxAntecedentLen);
        }
    }
}
