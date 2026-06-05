package cmar.benchmark;

import cmar.MDLDiscretizer;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Parses real UCI ML datasets from local CSV files (authoritative).
 * No synthetic data — if parsing fails, the caller returns null.
 */
public class DataLoader {

    /** Fuzzy CMAR: emit top-2 fuzzy bin items for borderline continuous values. */
    public static boolean FUZZY = false;
    /** Emit the 2nd fuzzy bin item only when its membership weight exceeds this threshold. */
    public static double FUZZY_TAU = 0.35;

    /**
     * Load dataset from CSV string. Handles numeric (discretize) and categorical (encode) attrs.
     * Last column is class label.
     */
    public static int[][][] parseCSV(String csv, int numBins, String missingMarker) {
        String[] lines = csv.trim().split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("@") || line.startsWith("%") || line.startsWith("#")) continue;
            if (missingMarker != null && line.contains(missingMarker)) continue; // skip missing
            rows.add(line.split(","));
        }

        if (rows.isEmpty()) return null;

        int numCols = rows.get(0).length;
        int numAttrs = numCols - 1;
        int n = rows.size();

        // Detect if each attribute is numeric or categorical
        // "MISS" values are ignored for type detection
        boolean[] isNumeric = new boolean[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            isNumeric[a] = true;
            boolean hasAnyNumeric = false;
            for (String[] row : rows) {
                String val = row[a].trim();
                if (val.equals("MISS")) continue; // skip missing for type detection
                try {
                    Double.parseDouble(val);
                    hasAnyNumeric = true;
                } catch (NumberFormatException e) {
                    isNumeric[a] = false;
                    break;
                }
            }
            if (!hasAnyNumeric) isNumeric[a] = false;
        }

        // Encode class labels
        Map<String, Integer> classMap = new LinkedHashMap<>();
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            String cls = rows.get(i)[numCols - 1].trim();
            classMap.putIfAbsent(cls, classMap.size());
            labels[i] = classMap.get(cls);
        }

        // Build item encoding
        // For numeric: discretize into bins, item = offset + bin
        // For categorical: item = offset + category_index
        int[] offsets = new int[numAttrs];
        int[] numValues = new int[numAttrs];
        double[] mins = new double[numAttrs];
        double[] maxs = new double[numAttrs];

        // First pass: determine ranges, unique values, and categories
        @SuppressWarnings("unchecked")
        Map<String, Integer>[] catMaps = new Map[numAttrs];
        boolean[] treatAsCategorical = new boolean[numAttrs]; // numeric with few unique values

        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a]) {
                // Count unique values to decide discretization strategy
                Set<String> uniqueVals = new LinkedHashSet<>();
                mins[a] = Double.MAX_VALUE;
                maxs[a] = -Double.MAX_VALUE;
                for (String[] row : rows) {
                    String val = row[a].trim();
                    if (val.equals("MISS")) continue;
                    uniqueVals.add(val);
                    double v = Double.parseDouble(val);
                    mins[a] = Math.min(mins[a], v);
                    maxs[a] = Math.max(maxs[a], v);
                }
                // If few unique values (e.g. binary 0/1, small integers), treat as categorical
                if (uniqueVals.size() <= numBins + 1) {
                    treatAsCategorical[a] = true;
                    catMaps[a] = new LinkedHashMap<>();
                    for (String val : uniqueVals) {
                        catMaps[a].putIfAbsent(val, catMaps[a].size());
                    }
                    numValues[a] = catMaps[a].size();
                } else {
                    // Collect all values for quantile-based binning
                    numValues[a] = numBins;
                }
            } else {
                catMaps[a] = new LinkedHashMap<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
                numValues[a] = catMaps[a].size();
            }
        }

        // Compute offsets
        int totalOffset = 0;
        for (int a = 0; a < numAttrs; a++) {
            offsets[a] = totalOffset;
            totalOffset += numValues[a];
        }

        // Precompute quantile boundaries and median for numeric attributes
        double[][] quantileBounds = new double[numAttrs][];
        double[] medians = new double[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a] && !treatAsCategorical[a]) {
                List<Double> vals = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    String val = rows.get(i)[a].trim();
                    if (!val.equals("MISS")) vals.add(Double.parseDouble(val));
                }
                if (vals.isEmpty()) { medians[a] = 0; continue; }
                Collections.sort(vals);
                medians[a] = vals.get(vals.size() / 2);
                int vn = vals.size();
                quantileBounds[a] = new double[numBins - 1];
                for (int b = 1; b < numBins; b++) {
                    int idx = (int)((double) b / numBins * vn);
                    quantileBounds[a][b - 1] = vals.get(Math.min(idx, vn - 1));
                }
            }
        }

        // Encode transactions using quantile binning for numeric, direct mapping for categorical
        int[][] transactions = new int[n][numAttrs];
        for (int i = 0; i < n; i++) {
            for (int a = 0; a < numAttrs; a++) {
                String val = rows.get(i)[a].trim();
                int encoded;
                if (isNumeric[a] && !treatAsCategorical[a]) {
                    double v = val.equals("MISS") ? medians[a] : Double.parseDouble(val);
                    int bin = 0;
                    for (int b = 0; b < quantileBounds[a].length; b++) {
                        if (v > quantileBounds[a][b]) bin = b + 1;
                    }
                    encoded = offsets[a] + Math.min(bin, numBins - 1);
                } else {
                    Integer catIdx = catMaps[a].get(val);
                    encoded = offsets[a] + (catIdx != null ? catIdx : 0);
                }
                transactions[i][a] = encoded;
            }
        }

        return new int[][][]{transactions, new int[][]{labels}};
    }

    /**
     * Parse CSV với MDL discretization (Fayyad & Irani 1993) - giống C4.5.
     * Supervised discretization dựa trên entropy + MDL criterion.
     */
    public static int[][][] parseMDL(String csv) {
        String[] lines = csv.trim().split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("@") || line.startsWith("%") || line.startsWith("#")) continue;
            rows.add(line.split(","));
        }
        if (rows.isEmpty()) return null;

        int numCols = rows.get(0).length;
        int numAttrs = numCols - 1;
        int n = rows.size();

        // Detect numeric/categorical (skip "0" placeholder for missing)
        boolean[] isNumeric = new boolean[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            isNumeric[a] = true;
            boolean hasAny = false;
            for (String[] row : rows) {
                String val = row[a].trim();
                if (val.equals("MISS")) continue;
                try { Double.parseDouble(val); hasAny = true; }
                catch (NumberFormatException e) { isNumeric[a] = false; break; }
            }
            if (!hasAny) isNumeric[a] = false;
        }

        // Encode class labels
        Map<String, Integer> classMap = new LinkedHashMap<>();
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            String cls = rows.get(i)[numCols - 1].trim();
            classMap.putIfAbsent(cls, classMap.size());
            labels[i] = classMap.get(cls);
        }

        // For each numeric attr: MDL discretization; for categorical: direct encoding
        @SuppressWarnings("unchecked")
        Map<String, Integer>[] catMaps = new Map[numAttrs];
        List<Double>[] cutPointsList = new List[numAttrs];
        int[] numValues = new int[numAttrs];
        boolean[] treatAsCat = new boolean[numAttrs];

        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a]) {
                // Check if few unique values → treat as categorical
                Set<String> uniq = new LinkedHashSet<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    if (!val.equals("MISS")) uniq.add(val);
                }
                if (uniq.size() <= 10) { // ≤10 unique → categorical (binary, ordinal...)
                    treatAsCat[a] = true;
                    catMaps[a] = new LinkedHashMap<>();
                    for (String val : uniq) catMaps[a].putIfAbsent(val, catMaps[a].size());
                    numValues[a] = catMaps[a].size();
                } else {
                    // MDL discretization
                    double[] vals = new double[n];
                    double median = 0;
                    List<Double> nonMiss = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        String val = rows.get(i)[a].trim();
                        if (!val.equals("MISS")) nonMiss.add(Double.parseDouble(val));
                    }
                    if (!nonMiss.isEmpty()) {
                        Collections.sort(nonMiss);
                        median = nonMiss.get(nonMiss.size() / 2);
                    }
                    for (int i = 0; i < n; i++) {
                        String val = rows.get(i)[a].trim();
                        vals[i] = val.equals("MISS") ? median : Double.parseDouble(val);
                    }
                    cutPointsList[a] = MDLDiscretizer.findCutPoints(vals, labels);
                    numValues[a] = Math.max(1, cutPointsList[a].size() + 1);
                }
            } else {
                catMaps[a] = new LinkedHashMap<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
                numValues[a] = catMaps[a].size();
            }
        }

        // Compute offsets
        int[] offsets = new int[numAttrs];
        int totalOffset = 0;
        for (int a = 0; a < numAttrs; a++) {
            offsets[a] = totalOffset;
            totalOffset += numValues[a];
        }

        // Cache medians for missing value imputation
        double[] medians = new double[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a] && !treatAsCat[a]) {
                List<Double> nonMiss = new ArrayList<>();
                for (String[] row : rows) {
                    String v = row[a].trim();
                    if (!v.equals("MISS")) nonMiss.add(Double.parseDouble(v));
                }
                if (!nonMiss.isEmpty()) {
                    Collections.sort(nonMiss);
                    medians[a] = nonMiss.get(nonMiss.size() / 2);
                }
            }
        }

        // Encode transactions
        int[][] transactions = new int[n][numAttrs];
        for (int i = 0; i < n; i++) {
            for (int a = 0; a < numAttrs; a++) {
                String val = rows.get(i)[a].trim();
                int encoded;
                if (isNumeric[a] && !treatAsCat[a]) {
                    double v = val.equals("MISS") ? medians[a] : Double.parseDouble(val);
                    int bin = 0;
                    for (double cp : cutPointsList[a]) {
                        if (v > cp) bin++;
                        else break;
                    }
                    encoded = offsets[a] + Math.min(bin, numValues[a] - 1);
                } else {
                    Integer catIdx = catMaps[a].get(val);
                    encoded = offsets[a] + (catIdx != null ? catIdx : 0);
                }
                transactions[i][a] = encoded;
            }
        }

        return new int[][][]{transactions, new int[][]{labels}};
    }

    /**
     * Read local file as string.
     */
    public static String readLocalFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                // Prefer CSV datasets, but keep backward compatibility with legacy extensions.
                File csvCandidate = toCsvCandidate(path);
                if (csvCandidate != null && csvCandidate.exists()) {
                    f = csvCandidate;
                } else {
                    File legacyCandidate = toLegacyCandidate(path);
                    if (legacyCandidate != null && legacyCandidate.exists()) {
                        f = legacyCandidate;
                    } else {
                        return null;
                    }
                }
            }
            BufferedReader reader = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static File toCsvCandidate(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return null;
        // Only convert known legacy data extensions to .csv.
        // Avoid mapping unrelated files like ".test" or ".names" to a .csv sibling.
        if (!(lower.endsWith(".data") || lower.endsWith(".dat") || lower.endsWith(".all-data"))) {
            return null;
        }
        if (lower.endsWith(".all-data")) {
            return new File(path.substring(0, path.length() - ".all-data".length()) + ".csv");
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return new File(path + ".csv");
        }
        return new File(path.substring(0, dot) + ".csv");
    }

    private static File toLegacyCandidate(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".csv")) return null;
        String base = path.substring(0, path.length() - 4);
        File data = new File(base + ".data");
        if (data.exists()) return data;
        File dat = new File(base + ".dat");
        if (dat.exists()) return dat;
        File allData = new File(base + ".all-data");
        if (allData.exists()) return allData;
        return null;
    }

    // ===== PER-FOLD CV SUPPORT =====

    /**
     * Raw (pre-discretization) data for per-fold CV discretization.
     * numBins==0 → per-fold supervised MDL (for parseMDL datasets).
     * numBins> 0 → per-fold equal-frequency quantile with that many bins (for parseCSV datasets).
     */
    public static class RawData {
        public final String[][] rawVals;  // [n][numAttrs], "MISS" for missing
        public final boolean[] isNumeric;
        public final boolean[] treatAsCat; // numeric but few unique vals → direct encode
        @SuppressWarnings("unchecked")
        public final Map<String, Integer>[] catMaps; // for categorical + treatAsCat attrs
        public final int[] labels;
        public final int n;
        public final int numAttrs;
        public int numBins; // 0 = MDL, >0 = quantile with numBins bins

        @SuppressWarnings("unchecked")
        public RawData(String[][] rawVals, boolean[] isNumeric, boolean[] treatAsCat,
                       Map<String, Integer>[] catMaps, int[] labels, int n, int numAttrs,
                       int numBins) {
            this.rawVals = rawVals;
            this.isNumeric = isNumeric;
            this.treatAsCat = treatAsCat;
            this.catMaps = catMaps;
            this.labels = labels;
            this.n = n;
            this.numAttrs = numAttrs;
            this.numBins = numBins;
        }
    }

    /** Train/test encoded transactions for a single CV fold. */
    public static class FoldData {
        public final int[][] trainTx;
        public final int[][] testTx;
        public final int[] trainLabels;
        public final int[] testLabels;

        public FoldData(int[][] trainTx, int[][] testTx, int[] trainLabels, int[] testLabels) {
            this.trainTx = trainTx;
            this.testTx = testTx;
            this.trainLabels = trainLabels;
            this.testLabels = testLabels;
        }
    }

    /**
     * Parse CSV to RawData without applying MDL discretization.
     * Use with encodeFold() for leak-free per-fold discretization in CV.
     */
    public static RawData parseRaw(String csv) {
        String[] lines = csv.trim().split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("@") || line.startsWith("%") || line.startsWith("#")) continue;
            rows.add(line.split(","));
        }
        if (rows.isEmpty()) return null;

        int numCols = rows.get(0).length;
        int numAttrs = numCols - 1;
        int n = rows.size();

        // Detect numeric vs categorical per attribute
        boolean[] isNumeric = new boolean[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            isNumeric[a] = true;
            boolean hasAny = false;
            for (String[] row : rows) {
                String val = row[a].trim();
                if (val.equals("MISS")) continue;
                try { Double.parseDouble(val); hasAny = true; }
                catch (NumberFormatException e) { isNumeric[a] = false; break; }
            }
            if (!hasAny) isNumeric[a] = false;
        }

        // Encode class labels
        Map<String, Integer> classMap = new LinkedHashMap<>();
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            String cls = rows.get(i)[numCols - 1].trim();
            classMap.putIfAbsent(cls, classMap.size());
            labels[i] = classMap.get(cls);
        }

        // Determine treatAsCat and build catMaps (global vocabulary)
        boolean[] treatAsCat = new boolean[numAttrs];
        @SuppressWarnings("unchecked")
        Map<String, Integer>[] catMaps = new Map[numAttrs];

        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a]) {
                Set<String> uniq = new LinkedHashSet<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    if (!val.equals("MISS")) uniq.add(val);
                }
                if (uniq.size() <= 10) {
                    treatAsCat[a] = true;
                    catMaps[a] = new LinkedHashMap<>();
                    for (String val : uniq) catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
                // else: catMaps[a] = null → will use per-fold MDL
            } else {
                catMaps[a] = new LinkedHashMap<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
            }
        }

        // Store raw string values
        String[][] rawVals = new String[n][numAttrs];
        for (int i = 0; i < n; i++)
            for (int a = 0; a < numAttrs; a++)
                rawVals[i][a] = rows.get(i)[a].trim();

        return new RawData(rawVals, isNumeric, treatAsCat, catMaps, labels, n, numAttrs, 0);
    }

    /**
     * Parse CSV to RawData for per-fold equal-frequency quantile binning.
     * Mirrors parseCSV logic (treatAsCat threshold = numBins+1) but stores raw values
     * so cut points can be learned from training fold only.
     * Use missingMarker=null when the CSV has no missing values.
     */
    public static RawData parseRaw(String csv, int numBins, String missingMarker) {
        String[] lines = csv.trim().split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("@") || line.startsWith("%") || line.startsWith("#")) continue;
            if (missingMarker != null && line.contains(missingMarker)) continue;
            rows.add(line.split(","));
        }
        if (rows.isEmpty()) return null;

        int numCols = rows.get(0).length;
        int numAttrs = numCols - 1;
        int n = rows.size();
        int treatAsCatThreshold = numBins + 1; // match parseCSV behavior

        boolean[] isNumeric = new boolean[numAttrs];
        for (int a = 0; a < numAttrs; a++) {
            isNumeric[a] = true;
            boolean hasAny = false;
            for (String[] row : rows) {
                String val = row[a].trim();
                if (val.equals("MISS")) continue;
                try { Double.parseDouble(val); hasAny = true; }
                catch (NumberFormatException e) { isNumeric[a] = false; break; }
            }
            if (!hasAny) isNumeric[a] = false;
        }

        Map<String, Integer> classMap = new LinkedHashMap<>();
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            String cls = rows.get(i)[numCols - 1].trim();
            classMap.putIfAbsent(cls, classMap.size());
            labels[i] = classMap.get(cls);
        }

        boolean[] treatAsCat = new boolean[numAttrs];
        @SuppressWarnings("unchecked")
        Map<String, Integer>[] catMaps = new Map[numAttrs];

        for (int a = 0; a < numAttrs; a++) {
            if (isNumeric[a]) {
                Set<String> uniq = new LinkedHashSet<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    if (!val.equals("MISS")) uniq.add(val);
                }
                if (uniq.size() <= treatAsCatThreshold) {
                    treatAsCat[a] = true;
                    catMaps[a] = new LinkedHashMap<>();
                    for (String val : uniq) catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
            } else {
                catMaps[a] = new LinkedHashMap<>();
                for (String[] row : rows) {
                    String val = row[a].trim();
                    catMaps[a].putIfAbsent(val, catMaps[a].size());
                }
            }
        }

        String[][] rawVals = new String[n][numAttrs];
        for (int i = 0; i < n; i++)
            for (int a = 0; a < numAttrs; a++)
                rawVals[i][a] = rows.get(i)[a].trim();

        return new RawData(rawVals, isNumeric, treatAsCat, catMaps, labels, n, numAttrs, numBins);
    }

    /**
     * Encode a train/test split using MDL cut points learned from trainIdx only.
     * Eliminates data leakage from global pre-discretization.
     */
    public static FoldData encodeFold(RawData raw, int[] trainIdx, int[] testIdx) {
        int numAttrs = raw.numAttrs;

        // Per continuous-numeric attribute: learn MDL cut points from training rows
        @SuppressWarnings("unchecked")
        List<Double>[] cutPoints = new List[numAttrs];
        double[] medians = new double[numAttrs];

        for (int a = 0; a < numAttrs; a++) {
            if (raw.isNumeric[a] && !raw.treatAsCat[a]) {
                List<Double> nonMiss = new ArrayList<>();
                for (int idx : trainIdx) {
                    String v = raw.rawVals[idx][a];
                    if (!v.equals("MISS")) nonMiss.add(Double.parseDouble(v));
                }
                if (!nonMiss.isEmpty()) {
                    Collections.sort(nonMiss);
                    medians[a] = nonMiss.get(nonMiss.size() / 2);
                }
                if (raw.numBins > 0) {
                    // Equal-frequency quantile cut points from training only
                    cutPoints[a] = quantileCuts(nonMiss, raw.numBins);
                } else {
                    // Supervised MDL cut points from training only (numBins == 0)
                    double[] vals = new double[trainIdx.length];
                    int[] lbls = new int[trainIdx.length];
                    for (int i = 0; i < trainIdx.length; i++) {
                        String v = raw.rawVals[trainIdx[i]][a];
                        vals[i] = v.equals("MISS") ? medians[a] : Double.parseDouble(v);
                        lbls[i] = raw.labels[trainIdx[i]];
                    }
                    cutPoints[a] = cmar.MDLDiscretizer.findCutPoints(vals, lbls);
                }
            } else {
                cutPoints[a] = Collections.emptyList();
            }
        }

        // Compute offsets using this fold's cut points
        int[] numValues = new int[numAttrs];
        int[] offsets = new int[numAttrs];
        int totalOffset = 0;
        for (int a = 0; a < numAttrs; a++) {
            offsets[a] = totalOffset;
            if (raw.isNumeric[a] && !raw.treatAsCat[a]) {
                numValues[a] = Math.max(1, cutPoints[a].size() + 1);
            } else {
                numValues[a] = Math.max(1, raw.catMaps[a] != null ? raw.catMaps[a].size() : 1);
            }
            totalOffset += numValues[a];
        }

        int[][] trainTx = encodeRows(raw, trainIdx, offsets, numValues, cutPoints, medians);
        int[][] testTx  = encodeRows(raw, testIdx,  offsets, numValues, cutPoints, medians);

        int[] trainLabels = new int[trainIdx.length];
        for (int i = 0; i < trainIdx.length; i++) trainLabels[i] = raw.labels[trainIdx[i]];
        int[] testLabels = new int[testIdx.length];
        for (int i = 0; i < testIdx.length; i++) testLabels[i] = raw.labels[testIdx[i]];

        return new FoldData(trainTx, testTx, trainLabels, testLabels);
    }

    private static int[][] encodeRows(RawData raw, int[] indices, int[] offsets,
                                       int[] numValues, List<Double>[] cutPoints, double[] medians) {
        int numAttrs = raw.numAttrs;

        // Precompute fuzzy bin centers per continuous attr (only when FUZZY on).
        double[][] fuzzyCenters = null;
        if (FUZZY) {
            fuzzyCenters = new double[numAttrs][];
            for (int a = 0; a < numAttrs; a++) {
                if (raw.isNumeric[a] && !raw.treatAsCat[a] && cutPoints[a].size() >= 1) {
                    fuzzyCenters[a] = cmar.FuzzyDiscretizer.centers(cutPoints[a]);
                }
            }
        }

        int[][] result = new int[indices.length][];
        // reusable buffer: at most numAttrs + (#continuous attrs) items per transaction
        int[] buf = new int[numAttrs * 2];
        for (int i = 0; i < indices.length; i++) {
            int row = indices[i];
            int len = 0;
            for (int a = 0; a < numAttrs; a++) {
                String val = raw.rawVals[row][a];
                if (raw.isNumeric[a] && !raw.treatAsCat[a]) {
                    double v = val.equals("MISS") ? medians[a] : Double.parseDouble(val);
                    int bin = 0;
                    for (double cp : cutPoints[a]) {
                        if (v > cp) bin++;
                        else break;
                    }
                    bin = Math.min(bin, numValues[a] - 1);
                    if (FUZZY && fuzzyCenters != null && fuzzyCenters[a] != null) {
                        // Fuzzy: emit top-1 membership bin + top-2 bin when borderline (w2 > tau)
                        cmar.FuzzyDiscretizer.FuzzyBins fb = cmar.FuzzyDiscretizer.fuzzify(v, fuzzyCenters[a]);
                        int b1 = Math.min(fb.bin1, numValues[a] - 1);
                        buf[len++] = offsets[a] + b1;
                        if (fb.bin2 >= 0 && fb.w2 > FUZZY_TAU) {
                            int b2 = Math.min(fb.bin2, numValues[a] - 1);
                            if (b2 != b1) buf[len++] = offsets[a] + b2;
                        }
                    } else {
                        buf[len++] = offsets[a] + bin;
                    }
                } else {
                    Integer catIdx = raw.catMaps[a] != null ? raw.catMaps[a].get(val) : null;
                    buf[len++] = offsets[a] + (catIdx != null ? catIdx : 0);
                }
            }
            result[i] = java.util.Arrays.copyOf(buf, len);
        }
        return result;
    }

    /** Compute equal-frequency quantile cut points from a sorted list of training values. */
    private static List<Double> quantileCuts(List<Double> sortedVals, int numBins) {
        int n = sortedVals.size();
        if (n == 0 || numBins <= 1) return Collections.emptyList();
        List<Double> cuts = new ArrayList<>();
        for (int b = 1; b < numBins; b++) {
            int idx = (int)((double) b / numBins * n);
            cuts.add(sortedVals.get(Math.min(idx, n - 1)));
        }
        return cuts;
    }

    public static String fetchURL(String urlStr) {
        try {
            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Java-CMAR-Benchmark/1.0");

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("  Download failed: " + e.getMessage());
            return null;
        }
    }
}
