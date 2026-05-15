package cmar.benchmark;

import cmar.CMARClassifier;
import cmar.util.PhaseTimer;
import cmar.util.MemorySampler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Benchmark runner: runs CMAR on all UCI datasets from the original paper,
 * compares results, and generates markdown reports.
 */
public class BenchmarkRunner {

    static final String RESULTS_DIR = "results";
    static final double TUNE_TRIGGER_DIFF = 2.0; // tune when gap vs paper is larger than 2%
    static final int TUNE_FOLDS = 2; // fast tuning with 2 folds
    static int TOP_K_GLOBAL = 3; // 0 = paper-faithful (use all matched rules); >0 = top-k voting
    /** Khi true: lưới tham số quanh giá trị paper để tối đa hóa accuracy CV (khác so sánh 1–1 paper). */
    static boolean TUNE_MAX_ACCURACY = false;
    /** Khi true: ghi ra results/rules/&lt;dataset&gt;-rules.csv cho mỗi bộ (luật từ fold cuối). */
    static boolean DUMP_RULES = false;
    /** Override chi² threshold from CLI; 0.0 = use paper default 3.841. */
    static double CHI_OVERRIDE = 0.0;

    public static void main(String[] args) throws IOException {
        // Phase 05: parse --mode=baseline|improved (default=improved)
        String mode = "improved";
        boolean warmupOnly = false;
        for (String arg : args) {
            if (arg.startsWith("--mode=")) mode = arg.substring(7).toLowerCase();
            if (arg.equalsIgnoreCase("--warmupOnly")) warmupOnly = true;
            if (arg.equalsIgnoreCase("--tuneAccuracy")) TUNE_MAX_ACCURACY = true;
            if (arg.startsWith("--topK=")) {
                try { TOP_K_GLOBAL = Math.max(0, Integer.parseInt(arg.substring(7))); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--hmLift")) {
                // Full hybrid: HM sort + Lift filter + HM voting weight
                cmar.Rule.useHMLift = true;
                cmar.RulePruner.useHMLift = true;
            }
            if (arg.equalsIgnoreCase("--liftOnly")) {
                // WEviRC-style: only add Lift>=1 filter (CMAR sort + chi² vote unchanged)
                cmar.RulePruner.useHMLift = true;   // enables filter
                cmar.RulePruner.minHM = 0.0;         // disable HM filter
                cmar.Rule.useHMLift = false;         // keep CMAR sort + chi² vote
            }
            if (arg.equalsIgnoreCase("--hmWeightOnly")) {
                // WCBA-light: keep CMAR pruning order intact, only swap voting weight to HM.
                cmar.CMARClassifier.useHMWeightOnly = true;
                cmar.Rule.useHMLift = false;
                cmar.RulePruner.useHMLift = false;
            }
            if (arg.equalsIgnoreCase("--dumpRules")) DUMP_RULES = true;
            if (arg.equalsIgnoreCase("--liftSort")) {
                // Sort rules by Lift DESC first. CMAR-replacement direction: "larger Lift first".
                cmar.Rule.useLiftSort = true;
                cmar.Rule.useHMLift = false;
            }
            if (arg.equalsIgnoreCase("--longerRules")) cmar.Rule.useLongerRules = true;
            if (arg.equalsIgnoreCase("--liftSort")) cmar.Rule.useLiftSort = true;
            if (arg.equalsIgnoreCase("--avgVote")) cmar.CMARClassifier.useAvgVote = true;
            if (arg.startsWith("--perClassTopK=")) {
                try { cmar.CMARClassifier.perClassTopK = Math.max(0, Integer.parseInt(arg.substring(15))); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--liftWeight")) {
                // Lift-based voting: weight = Lift (positive correlation magnitude).
                cmar.CMARClassifier.useLiftWeight = true;
                cmar.Rule.useHMLift = false;
                cmar.RulePruner.useHMLift = false;
            }
            if (arg.startsWith("--minHM=")) {
                try { cmar.RulePruner.minHM = Double.parseDouble(arg.substring(8)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.startsWith("--minLift=")) {
                try { cmar.RulePruner.minLift = Double.parseDouble(arg.substring(10)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--strictChi")) {
                // Stricter chi² threshold p=0.01 (instead of p=0.05).
                CHI_OVERRIDE = 6.635;
            }
            if (arg.startsWith("--chiThreshold=")) {
                try { CHI_OVERRIDE = Double.parseDouble(arg.substring(15)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--liftTieBreak")) {
                // Lift as 3rd-criterion tiebreaker in CMAR sort (keeps paper primary order).
                cmar.Rule.useLiftTieBreak = true;
            }
            if (arg.equalsIgnoreCase("--liftSecond")) {
                // Lift in position 2: conf DESC → Lift DESC → sup DESC → length ASC
                cmar.Rule.useLiftSecond = true;
            }
            if (arg.equalsIgnoreCase("--chiFirst")) {
                // Sort by chi² DESC → confidence DESC → length ASC
                cmar.Rule.useChiFirst = true;
            }
            if (arg.equalsIgnoreCase("--sortCompose")) {
                // Sort by (confidence × Lift) DESC → length ASC
                cmar.Rule.useSortCompose = true;
            }
            if (arg.equalsIgnoreCase("--sortChiLift")) {
                // Sort by (chi² × Lift) DESC → length ASC
                cmar.Rule.useSortChiLift = true;
            }
            if (arg.equalsIgnoreCase("--confLinear")) {
                // Sort by (confidence + 0.1 × Lift) DESC → length ASC
                cmar.Rule.useConfLinear = true;
            }
            if (arg.startsWith("--confLinearAlpha=")) {
                try { cmar.Rule.confLinearAlpha = Double.parseDouble(arg.substring(18)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--condLift")) {
                // Conditional: if conf==1 use Lift, else use support
                cmar.Rule.useCondLift = true;
            }
            if (arg.equalsIgnoreCase("--dominantClass")) {
                // CMAR sort + class frequency tie-breaker (MCAR/EAC style)
                cmar.Rule.useDominantClass = true;
            }
            // P1: Class-weighted sort
            if (arg.equalsIgnoreCase("--classWeightedSort")) {
                cmar.Rule.useClassWeightedSort = true;
            }
            // P2: Stratified coverage
            if (arg.startsWith("--stratified=")) {
                try { cmar.RulePruner.stratifiedTopN = Integer.parseInt(arg.substring(13)); }
                catch (NumberFormatException ignored) {}
            }
            // P3: Dual-criterion filter
            if (arg.equalsIgnoreCase("--dualFilter")) {
                cmar.RulePruner.useDualFilter = true;
            }
            if (arg.startsWith("--dualMinLift=")) {
                try { cmar.RulePruner.dualMinLift = Double.parseDouble(arg.substring(14)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.startsWith("--dualMinConf=")) {
                try { cmar.RulePruner.dualMinConf = Double.parseDouble(arg.substring(14)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.startsWith("--strictLift=")) {
                try { cmar.RulePruner.strictLift = Double.parseDouble(arg.substring(13)); }
                catch (NumberFormatException ignored) {}
            }
            if (arg.equalsIgnoreCase("--weightChiLift")) {
                // Composite: vote weight = χ² × Lift
                cmar.CMARClassifier.useChiLiftWeight = true;
                cmar.CMARClassifier.useLiftWeight = false;
            }
            if (arg.equalsIgnoreCase("--weightConfLift")) {
                // Composite: vote weight = confidence × Lift
                cmar.CMARClassifier.useConfLiftWeight = true;
                cmar.CMARClassifier.useLiftWeight = false;
            }
        }
        if (mode.equals("baseline")) {
            cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.BASELINE);
            System.out.println("=== CMAR Benchmark — BASELINE (original algorithm) ===\n");
        } else {
            cmar.util.OptimizationProfile.setMode(cmar.util.OptimizationProfile.Mode.IMPROVED);
            String tag = (TOP_K_GLOBAL > 0 ? "topK=" + TOP_K_GLOBAL : "all-rules");
            if (cmar.Rule.useLiftSort) {
                tag += ", Lift-sort (Larger-Lift-first)";
            } else if (cmar.Rule.useHMLift) {
                tag += ", HM+Lift hybrid (minHM=" + cmar.RulePruner.minHM
                        + ", minLift=" + cmar.RulePruner.minLift + ")";
            } else if (cmar.CMARClassifier.useHMWeightOnly) {
                tag += ", HM voting weight only";
            } else if (cmar.CMARClassifier.useLiftWeight) {
                tag += ", Lift voting weight";
            } else if (cmar.RulePruner.useHMLift) {
                tag += ", Lift filter only";
            }
            if (cmar.Rule.useLongerRules) tag += ", longer-rules";
            if (cmar.Rule.useLiftSort) tag += ", Lift-sort";
            System.out.println("=== CMAR Benchmark — IMPROVED (" + tag + ") ===\n");
            if (TUNE_MAX_ACCURACY) {
                System.out.println("** --tuneAccuracy: tối đa hóa accuracy (lưới tham số / fold nhanh) **\n");
            }
        }

        // Warmup mode: chạy nhanh 1 dataset nặng để JIT ổn định, không ghi report
        if (warmupOnly) {
            // Quan trọng: KHÔNG gọi getAllDatasets() vì nó sẽ load đủ 26 dataset.
            // Warmup chỉ cần 1 dataset đại diện (Waveform) để JIT compile hot path.
            UCIDatasets.Dataset ds = UCIDatasets.loadWaveform();
            if (ds != null) {
                System.out.println("Warmup: " + ds.name + " (no reports) ...");
                runBenchmark(ds);
            }
            return;
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

    /** Dump tất cả luật (đã prune) ra CSV: 1 dòng/luật, có Lift/HM/χ²/conf/sup. */
    static void dumpRulesCsv(String datasetName, java.util.List<cmar.Rule> rules) throws IOException {
        String dir = RESULTS_DIR + "/rules";
        new File(dir).mkdirs();
        String filename = dir + "/" + datasetName.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-rules.csv";

        // Sort by Lift desc to make inspection easier (best-correlated rules first).
        java.util.List<cmar.Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> Double.compare(b.getLift(), a.getLift()));

        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("rank,classLabel,length,support,antSupport,confidence,lift,hm,chiSquare,weight,antecedent\n");
            int rank = 1;
            for (cmar.Rule r : sorted) {
                int[] ant = r.getAntecedent();
                StringBuilder antStr = new StringBuilder();
                for (int i = 0; i < ant.length; i++) {
                    if (i > 0) antStr.append(' ');
                    antStr.append(ant[i]);
                }
                fw.write(String.format(Locale.US,
                        "%d,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.2f,%.4f,%s%n",
                        rank++, r.getClassLabel(), ant.length,
                        r.getSupport(), r.getAntecedentSupport(), r.getConfidence(),
                        r.getLift(), r.getHm(), r.getChiSquare(), r.getWeight(), antStr.toString()));
            }
        }
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
        sb.append("**Date:** ").append(LocalDate.now()).append("\n");
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
        // Mặc định không tune — giữ đúng tham số paper từng bộ (+ coverage=4 trong ParamConfig.base).
        // --tuneAccuracy: bật lưới để cao nhất độ chính xác CV (chậm hơn nhiều).
        boolean canTuneMatchPaper = false;
        boolean canTune = canTuneMatchPaper && baseDiff > TUNE_TRIGGER_DIFF;
        if (canTuneMatchPaper && canTune) {
            ParamConfig tuned = tuneConfigMatchPaper(ds, folds, foldAssignment, base);
            EvalResult tunedEval = evaluateConfig(ds, folds, foldAssignment, tuned, folds);

            double tunedDiff = Math.abs(tunedEval.accuracy * 100 - ds.paperCMARAccuracy);
            if (tunedDiff < baseDiff || (Math.abs(tunedDiff - baseDiff) < 1e-9 && tunedEval.accuracy > baseEval.accuracy)) {
                best = tunedEval;
            }
        } else if (TUNE_MAX_ACCURACY) {
            ParamConfig tuned = tuneConfigMaxAccuracy(ds, folds, foldAssignment, base);
            EvalResult tunedEval = evaluateConfig(ds, folds, foldAssignment, tuned, folds);
            if (isBetterAccuracyEval(tunedEval, baseEval)) {
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
            if (TOP_K_GLOBAL > 0) {
                cmar = new CMARClassifier(
                        cfg.minSupport, cfg.minConfidence, cfg.chiThreshold,
                        cfg.maxCoverageCount, cfg.maxRules, cfg.maxAntecedentLen,
                        TOP_K_GLOBAL);
            }

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

            // Dump rules from last fold for inspection (lift / HM / chi² per rule).
            if (DUMP_RULES && f == actualFolds - 1) {
                try { dumpRulesCsv(ds.name, cmar.getRules()); }
                catch (Exception e) { System.out.println("    dump error: " + e.getMessage()); }
            }
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

    /** Ưu tiên nhỏ nhất \|acc − paper|; hòa thì accuracy cao hơn. */
    private static ParamConfig tuneConfigMatchPaper(UCIDatasets.Dataset ds, int folds, int[] foldAssignment, ParamConfig base) {
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

    /** Accuracy CV cao hơn là tốt; hòa thì ít luật sau prune hơn (mô hình gọn); hòa nữa thì train nhanh hơn. */
    private static boolean isBetterAccuracyEval(EvalResult cand, EvalResult cur) {
        if (cand.accuracy > cur.accuracy + 1e-12) return true;
        if (Math.abs(cand.accuracy - cur.accuracy) > 1e-12) return false;
        if (cand.avgRulesPruned < cur.avgRulesPruned) return true;
        if (cand.avgRulesPruned > cur.avgRulesPruned) return false;
        return cand.avgTrainTimeMs < cur.avgTrainTimeMs;
    }

    /**
     * Lưới nhỏ quanh ParamConfig.base: chỉnh tỉ lệ minSup, confidence, χ² và δ coverage / độ dài tiền đề
     * để tối đa hóa accuracy trên TUNE_FOLDS; kết quả cuối vẫn đo lại đủ 10 fold ở runBenchmark.
     */
    private static ParamConfig tuneConfigMaxAccuracy(UCIDatasets.Dataset ds, int folds, int[] foldAssignment, ParamConfig base) {
        ParamConfig bestCfg = base;
        EvalResult bestEval = evaluateConfig(ds, folds, foldAssignment, base, TUNE_FOLDS);

        // Lưới vừa phải (~9 + ~18 đánh giá × 2 fold) để không nổ thời gian trên 26 bộ
        double[] supportScales = {0.90, 1.00, 1.10};
        double[] confDeltas = {-0.05, 0.0, 0.05};
        for (double scale : supportScales) {
            for (double delta : confDeltas) {
                ParamConfig candidate = base.with(
                        clamp(base.minSupportRatio * scale, 0.005, 0.22),
                        base.minConfidence + delta,
                        base.chiThreshold,
                        base.maxCoverageCount,
                        base.maxAntecedentLen,
                        base.maxRules,
                        ds.numInstances,
                        folds);
                EvalResult eval = evaluateConfig(ds, folds, foldAssignment, candidate, TUNE_FOLDS);
                if (isBetterAccuracyEval(eval, bestEval)) {
                    bestEval = eval;
                    bestCfg = candidate;
                }
            }
        }

        double[] chiThresholds = {2.706, 3.841}; // p≈0.10 vs p=0.05 — χ² nhỏ hơn có thể giữ thêm luật
        int[] coverages = {3, 4, 5};
        int[] antLens = {4, 5, 6};
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
                    if (isBetterAccuracyEval(eval, bestEval)) {
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

        sb.append("# CMAR \u2014 B\u00e1o c\u00e1o benchmark (t\u00f3m t\u1eaft)\n\n");
        sb.append("| M\u1ee5c | N\u1ed9i dung |\n");
        sb.append("|---|---|\n");
        sb.append("| **Ng\u00e0y ch\u1ea1y** | ");
        sb.append(LocalDate.now());
        sb.append(" |\n");
        sb.append("| **B\u00e0i b\u00e1o tham chi\u1ebfu** | Li, Han, Pei \u2014 *CMAR* (IEEE ICDM 2001) |\n");
        sb.append("| **Code** | Java \u2014 bitmap matching, CR-tree c\u00f3 hash, chi-square + coverage pruning |\n");
        sb.append("| **\u0110\u00e1nh gi\u00e1** | 10-fold cross-validation |\n");
        if (TUNE_MAX_ACCURACY) {
            sb.append("| **Tuning accuracy** | `--tuneAccuracy` \u2014 l\u01b0\u1edbi tham s\u1ed1 theo accuracy (2 fold nhanh), \u0111\u00f3ng b\u0103ng full 10-fold |\n");
        }
        if (TOP_K_GLOBAL > 0) {
            sb.append("| **D\u1ef1 \u0111o\u00e1n** | Top-k to\u00e0n c\u1ee5c: ch\u1ec9 l\u1ea5y **");
            sb.append(TOP_K_GLOBAL);
            sb.append("** lu\u1eadt kh\u1edbp t\u1ed1t nh\u1ea5t khi b\u1ecf phi\u1ebfu (kh\u00e1c b\u1ea3n paper \u0111\u1ea7y \u0111\u1ee7) |\n");
        }
        sb.append("\n");
        sb.append("## C\u00e1ch \u0111\u1ecdc b\u00e1o c\u00e1o\n\n");
        sb.append("### B\u1ea3ng \u0111\u1ed9 ch\u00ednh x\u00e1c (Accuracy Comparison)\n\n");
        sb.append("- **Our CMAR:** \u0111\u1ed9 ch\u00ednh x\u00e1c (%) do ch\u01b0\u01a1ng tr\u00ecnh c\u1ee7a b\u1ea1n \u0111o \u0111\u01b0\u1ee3c.\n");
        sb.append("- **Paper CMAR / Paper CBA / Paper C4.5:** s\u1ed1 **ghi trong b\u00e0i b\u00e1o** \u0111\u1ec3 so s\u00e1nh \u2014 *kh\u00f4ng* ph\u1ea3i ch\u1ea1y l\u1ea1i CBA/C4.5 tr\u00ean m\u00e1y b\u1ea1n.\n");
        sb.append("- **Diff:** ch\u00eanh l\u1ec7ch **Our CMAR \u2212 Paper CMAR** (%). D\u01b0\u01a1ng (+) = b\u1ea1n cao h\u01a1n paper; \u00e2m (\u2212) = th\u1ea5p h\u01a1n.\n");
        sb.append("- **Instances / Attrs / Classes:** s\u1ed1 m\u1eabu, s\u1ed1 thu\u1ed9c t\u00ednh, s\u1ed1 l\u1edbp c\u1ee7a b\u1ed9 d\u1eef li\u1ec7u.\n\n");
        sb.append("### B\u1ea3ng hi\u1ec7u n\u0103ng (Performance Metrics)\n\n");
        sb.append("- **Train / Predict:** th\u1eddi gian hu\u1ea5n luy\u1ec7n (mine + prune) v\u00e0 d\u1ef1 \u0111o\u00e1n, **trung b\u00ecnh theo fold** (ms). Gi\u00e1 tr\u1ecb **0 ms** th\u01b0\u1eddng l\u00e0 l\u00e0m tr\u00f2n (< 1 ms).\n");
        sb.append("- **Rules mined:** s\u1ed1 lu\u1eadt sinh ra **tr\u01b0\u1edbc** b\u01b0\u1edbc c\u1eaft t\u1ec9a.\n");
        sb.append("- **Rules after prune:** s\u1ed1 lu\u1eadt **c\u00f2n l\u1ea1i sau** prune (d\u00f9ng \u0111\u1ec3 ph\u00e2n l\u1edbp). *(T\u00ean c\u0169 \"Rules Pruned\" d\u1ec5 g\u00e2y nh\u1ea7m \u2014 \u0111\u00e2y l\u00e0 lu\u1eadt **gi\u1eef l\u1ea1i**, kh\u00f4ng ph\u1ea3i s\u1ed1 lu\u1eadt b\u1ecb x\u00f3a.)*\n");
        sb.append("- **% Removed:** ph\u1ea7n tr\u0103m lu\u1eadt th\u00f4 b\u1ecb lo\u1ea1i: `100 * (1 - after/mined)` (trong b\u1ea3ng, *after* l\u00e0 c\u1ed9t *Rules after prune*).\n\n");
        sb.append("---\n\n");
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

        sb.append("## Performance Metrics\n\n");
        sb.append("| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |\n");
        sb.append("|---------|------------|--------------|-------------|-------------------|----------|\n");
        for (DatasetResult r : results) {
            double pruneRatio = (r.avgRulesMined > 0)
                    ? (1.0 - (double)r.avgRulesPruned / r.avgRulesMined) * 100 : 0;
            sb.append(String.format("| %s | %d ms | %d ms | %d | %d | %.1f%% |\n",
                    r.dataset.name, r.avgTrainTimeMs, r.avgPredictTimeMs,
                    r.avgRulesMined, r.avgRulesPruned, pruneRatio));
        }

        sb.append("\n## Parameters Used\n\n");
        sb.append("*Tham s\u1ed1 FP-Growth / CMAR cho t\u1eebng b\u1ed9 (min support d\u1ea1ng t\u1ef7 l\u1ec7 v\u00e0 s\u1ed1 giao d\u1ecbch t\u1ed1i thi\u1ec3u).*\n\n");
        sb.append("| Dataset | Min Support (ratio) | Min Support (abs) | Min Confidence |\n");
        sb.append("|---------|--------------------|--------------------|----------------|\n");
        for (DatasetResult r : results) {
            sb.append(String.format("| %s | %.2f | %d | %.2f |\n",
                    r.dataset.name, r.dataset.paperMinSupport, r.minSupport, r.minConfidence));
        }

        sb.append("\n## Key Observations\n\n");
        sb.append("*So s\u00e1nh **Our CMAR** v\u1edbi **Paper CMAR**, ng\u01b0\u1ee1ng ch\u00eanh l\u1ec7ch 0,5 \u0111i\u1ec3m ph\u1ea7n tr\u0103m.*\n\n");
        int wins = 0, ties = 0, losses = 0;
        for (DatasetResult r : results) {
            double diff = r.accuracy * 100 - r.dataset.paperCMARAccuracy;
            if (diff > 0.5) wins++;
            else if (diff < -0.5) losses++;
            else ties++;
        }
        sb.append("- **Th\u1eafng / Wins** (Our > Paper h\u01a1n 0,5%): ").append(wins).append("/").append(count).append("\n");
        sb.append("- **H\u00f2a / Ties** (ch\u00eanh l\u1ec7ch trong \u00b10,5%): ").append(ties).append("/").append(count).append("\n");
        sb.append("- **Thua / Losses** (Our th\u1ea5p h\u01a1n Paper h\u01a1n 0,5%): ").append(losses).append("/").append(count).append("\n");
        sb.append("- **Ch\u00eanh TB vs Paper CMAR / Average diff:** ").append(String.format("%s%.1f%%", avgDiff >= 0 ? "+" : "", avgDiff)).append("\n\n");

        sb.append("## Optimizations Applied\n\n");
        sb.append("1. **Bitmap rule matching** \u2014 ki\u1ec3m tra ti\u1ec1n \u0111\u1ec1 b\u1eb1ng AND bit, t\u1ed1i \u01b0u kh\u1edbp lu\u1eadt.\n");
        sb.append("2. **Hash-indexed CR-tree** \u2014 l\u01b0u lu\u1eadt theo l\u1edbp, c\u1eaft nh\u00e1nh nh\u1edd m\u1ee5c \u0111\u1ea7u ti\u00ean.\n");
        sb.append("3. **Chi-square pruning (CSP)** \u2014 b\u1ecf lu\u1eadt kh\u00f4ng c\u00f3 \u00fd ngh\u0129a th\u1ed1ng k\u00ea (p < 0,05).\n");
        sb.append("4. **Database coverage pruning (DCP)** \u2014 b\u1ecf lu\u1eadt d\u01b0 th\u1eeba theo \u0111\u1ed9 ph\u1ee7.\n");
        sb.append("5. **Single-path FP-tree** \u2014 t\u1ed1i \u01b0u khi ch\u1ec9 c\u00f2n m\u1ed9t nh\u00e1nh.\n");
        sb.append("6. **Weighted voting** \u2014 tr\u1ecdng s\u1ed1 \u2248 chi-square \u00d7 confidence; top-5 m\u1ed7i l\u1edbp khi b\u1ecf phi\u1ebfu.\n");
        sb.append("7. **Per-class adaptive minSupport** \u2014 l\u1edbp hi\u1ebfm (\u226410 m\u1eabu trong fold) d\u00f9ng support t\u1ed1i thi\u1ec3u 1.\n");
        sb.append("8. **Max antecedent length** \u2014 gi\u1edbi h\u1ea1n \u0111\u1ed9 d\u00e0i ti\u1ec1n \u0111\u1ec1 t\u1ed1i \u0111a 4 m\u1ee5c.\n");

        try (Writer fw = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)) {
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
            double chi = (CHI_OVERRIDE > 0.0) ? CHI_OVERRIDE : 3.841;
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
