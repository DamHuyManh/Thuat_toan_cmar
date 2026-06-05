package cmar.benchmark;

import java.util.*;

/**
 * UCI Datasets used in the original CMAR paper (Li, Han, Pei 2001).
 * Loads ONLY real data from local CSV files in datasets/ (authoritative).
 * If a CSV fails to load, returns null and prints "FAILED" — NO synthetic fallback.
 */
public class UCIDatasets {
    private static final Map<String, Integer> PAPER_INSTANCE_COUNTS = createPaperInstanceCounts();

    private static Map<String, Integer> createPaperInstanceCounts() {
        Map<String, Integer> m = new HashMap<>();
        m.put("Anneal", 898);
        m.put("Australian", 690);
        m.put("Auto", 205);
        m.put("Breast-Cancer", 683);
        m.put("Cleve", 303);
        m.put("Crx", 690);
        m.put("Diabetes", 768);
        m.put("German", 1000);
        m.put("Glass", 214);
        m.put("Heart", 270);
        m.put("Hepatitis", 155);
        m.put("Horse", 368);
        m.put("Hypo", 3163);
        m.put("Iono", 351);
        m.put("Iris", 150);
        m.put("Labor", 57);
        m.put("Led7", 3200);
        m.put("Lymphography", 148);
        m.put("Pima", 768);
        m.put("Sick", 2800);
        m.put("Sonar", 208);
        m.put("Tic-Tac-Toe", 958);
        m.put("Vehicle", 846);
        m.put("Waveform", 5000);
        m.put("Wine", 178);
        m.put("Zoo", 101);
        return Collections.unmodifiableMap(m);
    }

    private static boolean enforcePaperSize(Dataset ds) {
        Integer expected = PAPER_INSTANCE_COUNTS.get(ds.name);
        if (expected == null) return true;
        if (ds.transactions == null || ds.labels == null) return false;
        if (ds.transactions.length < expected || ds.labels.length < expected) {
            System.out.println("  Skip " + ds.name + ": insufficient rows for paper target (" +
                    ds.transactions.length + "/" + expected + ")");
            return false;
        }
        if (ds.transactions.length > expected || ds.labels.length > expected) {
            ds.transactions = Arrays.copyOf(ds.transactions, expected);
            ds.labels = Arrays.copyOf(ds.labels, expected);
            ds.numInstances = expected;
            if (ds.description != null && ds.description.contains(" instances")) {
                int idx = ds.description.indexOf(" instances");
                ds.description = expected + ds.description.substring(idx);
            }
            System.out.println("  Trimmed " + ds.name + " to paper size: " + expected);
        }
        return true;
    }

    private static String readCsvFirst(String csvPath) {
        String csv = DataLoader.readLocalFile(csvPath);
        if (csv != null) return csv;
        return DataLoader.readLocalFile(csvPath); // DataLoader handles csv->legacy fallback.
    }


    public static class Dataset {
        public String name;
        public int[][] transactions;
        public int[] labels;
        public int numAttributes;
        public int numClasses;
        public int numInstances;
        public String description;
        public double paperMinSupport;
        public double paperMinConfidence;
        public double paperCMARAccuracy;
        public double paperCBAAccuracy;
        public double paperC45Accuracy;
        public String source;
        public DataLoader.RawData rawData; // for per-fold leak-free discretization
        public Dataset(String name, int[][] transactions, int[] labels,
                       int numAttributes, int numClasses, String description,
                       double paperMinSup, double paperMinConf,
                       double paperCMAR, double paperCBA, double paperC45, String source) {
            this.name = name;
            this.transactions = transactions;
            this.labels = labels;
            this.numAttributes = numAttributes;
            this.numClasses = numClasses;
            this.numInstances = transactions.length;
            this.description = description;
            this.paperMinSupport = paperMinSup;
            this.paperMinConfidence = paperMinConf;
            this.paperCMARAccuracy = paperCMAR;
            this.paperCBAAccuracy = paperCBA;
            this.paperC45Accuracy = paperC45;
            this.source = source;
        }

    }

    public static List<Dataset> getAllDatasets() {
        List<Dataset> datasets = new ArrayList<>();
        // All 26 datasets from CMAR paper (Li, Han, Pei 2001)
        datasets.add(loadAnneal());       // 1
        datasets.add(loadAustralian());   // 2
        datasets.add(loadAuto());         // 3
        datasets.add(loadBreastCancer()); // 4
        datasets.add(loadCleve());        // 5
        datasets.add(loadCrx());          // 6
        datasets.add(loadDiabetes());     // 7
        datasets.add(loadGerman());       // 8
        datasets.add(loadGlass());        // 9
        datasets.add(loadHeart());        // 10
        datasets.add(loadHepatitis());    // 11
        datasets.add(loadHorse());        // 12
        datasets.add(loadHypo());         // 13
        datasets.add(loadIono());         // 14
        datasets.add(loadIris());         // 15
        datasets.add(loadLabor());        // 16
        datasets.add(loadLed7());         // 17
        datasets.add(loadLymphography()); // 18
        datasets.add(loadPima());         // 19
        datasets.add(loadSick());         // 20
        datasets.add(loadSonar());        // 21
        datasets.add(loadTicTacToe());    // 22
        datasets.add(loadVehicle());      // 23
        datasets.add(loadWaveform());     // 24
        datasets.add(loadWine());         // 25
        datasets.add(loadZoo());          // 26

        List<Dataset> normalized = new ArrayList<>();
        for (Dataset ds : datasets) {
            if (ds == null) continue;
            if (enforcePaperSize(ds)) normalized.add(ds);
        }
        return normalized;
    }

    // ===== ANNEAL (898 x 38 x 6) =====
    static Dataset loadAnneal() {
        System.out.print("  Loading Anneal... ");
        String csv = readCsvFirst("datasets/anneal.csv");
        if (csv != null && csv.length() > 100) {
            String extra = DataLoader.readLocalFile("datasets/anneal.test.csv");
            if (extra == null) extra = DataLoader.readLocalFile("datasets/anneal.test");
            if (extra != null && extra.length() > 100) {
                csv = csv + "\n" + extra;
            }
        }
        if (csv != null && csv.length() > 100) {
            // Comma-separated, last col = class, has ? for missing
            // Replace ? with 0 for missing values
            String[] lines2 = csv.trim().split("\n");
            List<String> cleaned2 = new ArrayList<>();
            for (String line : lines2) {
                line = line.trim();
                if (line.isEmpty()) continue;
                cleaned2.add(line.replace("?", "MISS"));
            }
            String annealCsv = String.join("\n", cleaned2);
            int[][][] parsed = DataLoader.parseMDL(annealCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Anneal", parsed[0], parsed[1][0], 38, 6,
                        parsed[0].length + " instances, 38 mixed attrs, 6 classes",
                        0.01, 0.50, 97.3, 97.9, 94.8, "real");
                ds.rawData = DataLoader.parseRaw(annealCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== AUTO (205 x 25 x 7) =====
    static Dataset loadAuto() {
        System.out.print("  Loading Auto... ");
        String csv = readCsvFirst("datasets/imports-85.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, 1st col = symboling (-3 to 3 = 7 classes), has ?
            String[] lines = csv.trim().split("\n");
            List<String> reordered = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replace("?", "MISS");
                String[] parts = line.split(",");
                if (parts.length < 26) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[0].trim()); // class to last
                reordered.add(sb.toString());
            }
            String autoCsv = String.join("\n", reordered);
            int[][][] parsed = DataLoader.parseMDL(autoCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Set<Integer> classes = new HashSet<>();
                for (int l : parsed[1][0]) classes.add(l);
                Dataset ds = new Dataset("Auto", parsed[0], parsed[1][0], 25, classes.size(),
                        parsed[0].length + " instances, 25 mixed attrs, " + classes.size() + " classes",
                        0.01, 0.50, 78.1, 78.3, 80.1, "real");
                ds.rawData = DataLoader.parseRaw(autoCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== CLEVE (303 x 13 x 2) =====
    static Dataset loadCleve() {
        System.out.print("  Loading Cleve... ");
        String csv = readCsvFirst("datasets/processed.cleveland.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, last col = class (0-4), binarize: 0 vs >0
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replace("?", "MISS");
                String[] parts = line.split(",");
                if (parts.length < 14) continue;
                // Binarize class: 0 -> 0, 1-4 -> 1
                int cls = (int) Double.parseDouble(parts[parts.length - 1].trim());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) sb.append(parts[i].trim()).append(",");
                sb.append(cls > 0 ? "1" : "0");
                cleaned.add(sb.toString());
            }
            String cleveCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(cleveCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Cleve", parsed[0], parsed[1][0], 13, 2,
                        parsed[0].length + " instances, 13 attrs, 2 classes",
                        0.01, 0.50, 82.2, 82.8, 78.2, "real");
                ds.rawData = DataLoader.parseRaw(cleveCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== CRX (690 x 15 x 2) =====
    static Dataset loadCrx() {
        System.out.print("  Loading Crx... ");
        String csv = readCsvFirst("datasets/crx.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, last col = class (+/-), has ?
            String crxCsv = csv.replace("?", "MISS");
            int[][][] parsed = DataLoader.parseMDL(crxCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Crx", parsed[0], parsed[1][0], 15, 2,
                        parsed[0].length + " instances, 15 mixed attrs, 2 classes",
                        0.01, 0.50, 84.9, 84.7, 84.9, "real");
                ds.rawData = DataLoader.parseRaw(crxCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== DIABETES (768 x 8 x 2) - same as Pima =====
    static Dataset loadDiabetes() {
        System.out.print("  Loading Diabetes... ");
        String csv = readCsvFirst("datasets/diabetes.csv");
        if (csv == null) csv = readCsvFirst("datasets/pima-indians-diabetes.csv");
        if (csv != null && csv.length() > 100) {
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Diabetes", parsed[0], parsed[1][0], 8, 2,
                        parsed[0].length + " instances, 8 numeric attrs, 2 classes",
                        0.008, 0.50, 75.8, 74.5, 74.2, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== GERMAN (1000 x 20 x 2) =====
    static Dataset loadGerman() {
        System.out.print("  Loading German... ");
        String csv = readCsvFirst("datasets/german.csv");
        if (csv != null && csv.length() > 100) {
            // Space-separated, last col = class (1/2), 20 mixed attrs
            String[] lines = csv.trim().split("\n");
            List<String> converted = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                converted.add(line.replaceAll("\\s+", ","));
            }
            String germanCsv = String.join("\n", converted);
            int[][][] parsed = DataLoader.parseMDL(germanCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("German", parsed[0], parsed[1][0], 20, 2,
                        parsed[0].length + " instances, 20 mixed attrs, 2 classes",
                        0.01, 0.50, 74.9, 73.4, 72.3, "real");
                ds.rawData = DataLoader.parseRaw(germanCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== HORSE (368 x 22 x 2) =====
    static Dataset loadHorse() {
        System.out.print("  Loading Horse... ");
        String csv = readCsvFirst("datasets/horse-colic.csv");
        if (csv != null && csv.length() > 100) {
            String extra = DataLoader.readLocalFile("datasets/horse-colic.test.csv");
            if (extra == null) extra = DataLoader.readLocalFile("datasets/horse-colic.test");
            if (extra != null && extra.length() > 100) {
                csv = csv + "\n" + extra;
            }
        }
        if (csv != null && csv.length() > 100) {
            // Space-separated, col 24 = outcome (1=lived, 2=died, 3=euthanized)
            // Binarize: 1=lived(0), 2,3=died(1). Keep ? as distinct value
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 24) continue;
                StringBuilder sb = new StringBuilder();
                // 22 attrs: parts[0,1,3..22] — skip hospital(2); class = surgical lesion (parts[23])
                for (int i = 0; i < Math.min(parts.length, 23); i++) {
                    if (i == 2) continue; // skip hospital number
                    String val = parts[i].equals("?") ? "MISS" : parts[i];
                    sb.append(val).append(",");
                }
                int cls = 0;
                try { cls = Integer.parseInt(parts[23]) > 1 ? 1 : 0; } catch (Exception e) {}
                sb.append(cls);
                cleaned.add(sb.toString());
            }
            String horseCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(horseCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Horse", parsed[0], parsed[1][0], 22, 2,
                        parsed[0].length + " instances, 22 mixed attrs, 2 classes",
                        0.01, 0.50, 82.6, 82.1, 82.6, "real");
                ds.rawData = DataLoader.parseRaw(horseCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== HYPO (3163 x 25 x 2) =====
    static Dataset loadHypo() {
        System.out.print("  Loading Hypo... ");
        String csv = readCsvFirst("datasets/hypothyroid.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, 1st col = class (hypothyroid/negative), has ?
            String[] lines = csv.trim().split("\n");
            List<String> reordered = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    String val = parts[i].trim().replace("?", "MISS");
                    sb.append(val).append(",");
                }
                // Binarize class: hypothyroid=1, everything else=0
                String cls = parts[0].trim().toLowerCase().contains("hypothyroid") ? "1" : "0";
                sb.append(cls);
                reordered.add(sb.toString());
            }
            String hypoCsv = String.join("\n", reordered);
            int[][][] parsed = DataLoader.parseMDL(hypoCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Hypo", parsed[0], parsed[1][0], 25, 2,
                        parsed[0].length + " instances, 25 mixed attrs, 2 classes",
                        0.01, 0.50, 98.4, 98.9, 99.2, "real");
                ds.rawData = DataLoader.parseRaw(hypoCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== IONO (351 x 34 x 2) =====
    static Dataset loadIono() {
        System.out.print("  Loading Iono... ");
        String csv = readCsvFirst("datasets/ionosphere.csv");
        if (csv != null && csv.length() > 100) {
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Iono", parsed[0], parsed[1][0], 34, 2,
                        parsed[0].length + " instances, 34 numeric attrs, 2 classes",
                        0.032, 0.50, 91.5, 92.3, 90.0, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== LABOR (57 x 16 x 2) =====
    static Dataset loadLabor() {
        System.out.print("  Loading Labor... ");
        String csv = readCsvFirst("datasets/labor-neg.csv");
        if (csv != null && csv.length() > 10) {
            // Comma-separated with quotes, last col = class (good/bad), has ?
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim().replace("'", "").replace("?", "MISS");
                if (line.isEmpty() || line.startsWith("@")) continue;
                cleaned.add(line);
            }
            String laborCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(laborCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Labor", parsed[0], parsed[1][0], 16, 2,
                        parsed[0].length + " instances, 16 mixed attrs, 2 classes",
                        0.01, 0.50, 89.7, 86.3, 79.3, "real");
                ds.rawData = DataLoader.parseRaw(laborCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== LED7 (3200 x 7 x 10) =====
    static Dataset loadLed7() {
        System.out.print("  Loading Led7... ");
        String csv = readCsvFirst("datasets/led7.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, 7 binary attrs + class (0-9)
            int[][][] parsed = DataLoader.parseCSV(csv, 2, null);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Led7", parsed[0], parsed[1][0], 7, 10,
                        parsed[0].length + " instances, 7 binary attrs, 10 classes",
                        0.01, 0.50, 72.5, 71.9, 73.5, "real");
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== SICK (2800 x 29 x 2) =====
    static Dataset loadSick() {
        System.out.print("  Loading Sick... ");
        String csv = readCsvFirst("datasets/sick.csv");
        if (csv != null && csv.length() > 100) {
            // Format: attrs comma-separated, class at end before .|ID
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Remove .|ID suffix
                int pipeIdx = line.indexOf(".|");
                if (pipeIdx > 0) line = line.substring(0, pipeIdx);
                else {
                    int dotIdx = line.lastIndexOf(".");
                    if (dotIdx > 0 && dotIdx < line.length() - 1) {
                        String after = line.substring(dotIdx + 1);
                        if (after.matches("\\d+")) line = line.substring(0, dotIdx);
                    }
                }
                line = line.replace("?", "MISS");
                // Last field is class: negative/sick
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                String cls = parts[parts.length - 1].trim().toLowerCase();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) sb.append(parts[i].trim()).append(",");
                sb.append(cls.contains("sick") ? "1" : "0");
                cleaned.add(sb.toString());
            }
            String sickCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(sickCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Sick", parsed[0], parsed[1][0], 29, 2,
                        parsed[0].length + " instances, 29 mixed attrs, 2 classes",
                        0.005, 0.50, 97.5, 97.0, 98.5, "real");
                ds.rawData = DataLoader.parseRaw(sickCsv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== SONAR (208 x 60 x 2) =====
    static Dataset loadSonar() {
        System.out.print("  Loading Sonar... ");
        String csv = readCsvFirst("datasets/sonar.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated, last col = class (R/M), 60 numeric attrs
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Sonar", parsed[0], parsed[1][0], 60, 2,
                        parsed[0].length + " instances, 60 numeric attrs, 2 classes",
                        0.08, 0.50, 79.4, 77.5, 70.2, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED"); return null;
    }

    // ===== IRIS (150 x 4 x 3) =====
    static Dataset loadIris() {
        System.out.print("  Loading Iris... ");
        String csv = readCsvFirst("datasets/iris.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/iris/iris.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Iris", parsed[0], parsed[1][0], 4, 3,
                        parsed[0].length + " instances, 4 numeric attrs, 3 classes",
                        0.01, 0.50, 94.0, 94.7, 95.3, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== WINE (178 x 13 x 3) =====
    static Dataset loadWine() {
        System.out.print("  Loading Wine... ");
        String csv = readCsvFirst("datasets/wine.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/wine/wine.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            String[] lines = csv.trim().split("\n");
            List<String> reordered = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 14) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[0].trim());
                reordered.add(sb.toString());
            }
            String wineCsv = String.join("\n", reordered);
            int[][][] parsed = DataLoader.parseMDL(wineCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Wine", parsed[0], parsed[1][0], 13, 3,
                        parsed[0].length + " instances, 13 numeric attrs, 3 classes",
                        0.01, 0.50, 95.0, 95.0, 92.7, "real");
                ds.rawData = DataLoader.parseRaw(wineCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== BREAST CANCER (683 x 9 x 2) =====
    static Dataset loadBreastCancer() {
        System.out.print("  Loading Breast-Cancer... ");
        String csv = readCsvFirst("datasets/breast-cancer-wisconsin.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/breast-cancer-wisconsin/breast-cancer-wisconsin.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.contains("?")) continue;
                String[] parts = line.split(",");
                if (parts.length < 11) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= 9; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[10].trim());
                cleaned.add(sb.toString());
            }
            String bcCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(bcCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Breast-Cancer", parsed[0], parsed[1][0], 9, 2,
                        parsed[0].length + " instances, 9 integer attrs, 2 classes",
                        0.01, 0.50, 96.4, 96.3, 95.0, "real");
                ds.rawData = DataLoader.parseRaw(bcCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== ZOO (101 x 16 x 7) =====
    static Dataset loadZoo() {
        System.out.print("  Loading Zoo... ");
        String csv = readCsvFirst("datasets/zoo.csv");
        if (csv == null || csv.length() < 100) {
            csv = DataLoader.fetchURL("https://archive.ics.uci.edu/ml/machine-learning-databases/zoo/zoo.data");
        }
        if (csv != null && csv.length() > 100) {
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 18) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= 16; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[17].trim());
                cleaned.add(sb.toString());
            }
            String zooCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseCSV(zooCsv, 2, null);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Zoo", parsed[0], parsed[1][0], 16, 7,
                        parsed[0].length + " instances, 16 boolean attrs, 7 classes",
                        0.034, 0.50, 97.1, 96.8, 92.2, "real");
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== GLASS (214 x 9 x 6) =====
    static Dataset loadGlass() {
        System.out.print("  Loading Glass... ");
        // Try local first, then URL
        String csv = readCsvFirst("datasets/glass.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/glass/glass.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            String[] lines = csv.trim().split("\n");
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 11) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= 9; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[10].trim());
                cleaned.add(sb.toString());
            }
            String glassCsv = String.join("\n", cleaned);
            int[][][] parsed = DataLoader.parseMDL(glassCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Set<Integer> classes = new HashSet<>();
                for (int l : parsed[1][0]) classes.add(l);
                Dataset ds = new Dataset("Glass", parsed[0], parsed[1][0], 9, classes.size(),
                        parsed[0].length + " instances, 9 numeric attrs, " + classes.size() + " classes",
                        0.01, 0.50, 70.1, 73.9, 68.7, "real");
                ds.rawData = DataLoader.parseRaw(glassCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== TIC-TAC-TOE (958 x 9 x 2) =====
    static Dataset loadTicTacToe() {
        System.out.print("  Loading Tic-Tac-Toe... ");
        String csv = readCsvFirst("datasets/tic-tac-toe.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/tic-tac-toe/tic-tac-toe.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            int[][][] parsed = DataLoader.parseCSV(csv, 2, null);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Tic-Tac-Toe", parsed[0], parsed[1][0], 9, 2,
                        parsed[0].length + " instances, 9 categorical attrs, 2 classes",
                        0.01, 0.50, 99.2, 99.6, 99.4, "real");
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== LYMPHOGRAPHY (148 x 18 x 4) =====
    static Dataset loadLymphography() {
        System.out.print("  Loading Lymphography... ");
        String csv = readCsvFirst("datasets/lymphography.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/lymphography/lymphography.data";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            String[] lines = csv.trim().split("\n");
            List<String> reordered = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 19) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[0].trim());
                reordered.add(sb.toString());
            }
            String lymphCsv = String.join("\n", reordered);
            int[][][] parsed = DataLoader.parseCSV(lymphCsv, 6, null);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Set<Integer> classes = new HashSet<>();
                for (int l : parsed[1][0]) classes.add(l);
                Dataset ds = new Dataset("Lymphography", parsed[0], parsed[1][0], 18, classes.size(),
                        parsed[0].length + " instances, 18 attrs, " + classes.size() + " classes",
                        0.01, 0.50, 83.1, 77.8, 73.5, "real");
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== HEART (270 x 13 x 2) =====
    static Dataset loadHeart() {
        System.out.print("  Loading Heart... ");
        // CMAR paper uses Statlog Heart (270 x 13 x 2), NOT Cleveland Heart (303 x 13 x 5)
        String csv = readCsvFirst("datasets/heart.csv");
        if (csv == null || csv.length() < 100) {
            String url = "https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/heart/heart.dat";
            csv = DataLoader.fetchURL(url);
        }
        if (csv != null && csv.length() > 100) {
            // Space-separated
            String[] lines = csv.trim().split("\n");
            List<String> converted = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                converted.add(line.replaceAll("\\s+", ","));
            }
            String heartCsv = String.join("\n", converted);
            int[][][] parsed = DataLoader.parseMDL(heartCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Heart", parsed[0], parsed[1][0], 13, 2,
                        parsed[0].length + " instances, 13 attrs, 2 classes",
                        0.01, 0.50, 82.2, 81.9, 80.8, "real");
                ds.rawData = DataLoader.parseRaw(heartCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load — real CSV required");
        return null;
    }

    // ===== PIMA DIABETES (768 x 8 x 2) =====
    static Dataset loadPima() {
        System.out.print("  Loading Pima... ");
        // Try local file first, then URL
        String csv = readCsvFirst("datasets/pima-indians-diabetes.csv");
        if (csv == null) {
            csv = DataLoader.fetchURL("https://raw.githubusercontent.com/jbrownlee/Datasets/master/pima-indians-diabetes.data.csv");
        }
        if (csv != null && csv.length() > 100) {
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Pima", parsed[0], parsed[1][0], 8, 2,
                        parsed[0].length + " instances, 8 numeric attrs, 2 classes",
                        0.008, 0.50, 75.1, 72.9, 75.5, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED to load");
        return null;
    }

    // ===== AUSTRALIAN CREDIT (690 x 14 x 2) =====
    static Dataset loadAustralian() {
        System.out.print("  Loading Australian... ");
        String csv = readCsvFirst("datasets/australian.csv");
        if (csv != null && csv.length() > 100) {
            // Space-separated: 14 attrs + class (0/1)
            String[] lines = csv.trim().split("\n");
            List<String> converted = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                converted.add(line.replaceAll("\\s+", ","));
            }
            String australianCsv = String.join("\n", converted);
            int[][][] parsed = DataLoader.parseMDL(australianCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Australian", parsed[0], parsed[1][0], 14, 2,
                        parsed[0].length + " instances, 14 mixed attrs, 2 classes",
                        0.01, 0.50, 86.1, 84.9, 84.7, "real");
                ds.rawData = DataLoader.parseRaw(australianCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load");
        return null;
    }

    // ===== HEPATITIS (155 x 19 x 2) =====
    static Dataset loadHepatitis() {
        System.out.print("  Loading Hepatitis... ");
        String csv = readCsvFirst("datasets/hepatitis.csv");
        if (csv != null && csv.length() > 100) {
            // Format: class is FIRST column, then 19 attrs. Has ? for missing values
            String[] lines = csv.trim().split("\n");
            List<String> reordered = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                line = line.replace("?", "MISS");
                String[] parts = line.split(",");
                if (parts.length < 20) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) sb.append(parts[i].trim()).append(",");
                sb.append(parts[0].trim()); // class to last
                reordered.add(sb.toString());
            }
            String hepatitisCsv = String.join("\n", reordered);
            int[][][] parsed = DataLoader.parseMDL(hepatitisCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Hepatitis", parsed[0], parsed[1][0], 19, 2,
                        parsed[0].length + " instances, 19 mixed attrs, 2 classes",
                        0.01, 0.50, 80.5, 81.8, 80.6, "real");
                ds.rawData = DataLoader.parseRaw(hepatitisCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load");
        return null;
    }

    // ===== VEHICLE (846 x 18 x 4) =====
    static Dataset loadVehicle() {
        System.out.print("  Loading Vehicle... ");
        String csv = readCsvFirst("datasets/vehicle.csv");
        if (csv != null && csv.length() > 100) {
            // Space-separated: 18 numeric attrs + class name (bus/van/saab/opel)
            String[] lines = csv.trim().split("\n");
            List<String> converted = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                converted.add(line.replaceAll("\\s+", ","));
            }
            String vehicleCsv = String.join("\n", converted);
            int[][][] parsed = DataLoader.parseMDL(vehicleCsv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Vehicle", parsed[0], parsed[1][0], 18, 4,
                        parsed[0].length + " instances, 18 numeric attrs, 4 classes",
                        0.015, 0.50, 68.8, 68.7, 72.6, "real");
                ds.rawData = DataLoader.parseRaw(vehicleCsv);
                return ds;
            }
        }
        System.out.println("FAILED to load");
        return null;
    }

    // ===== WAVEFORM (5000 x 21 x 3) =====
    static Dataset loadWaveform() {
        System.out.print("  Loading Waveform... ");
        String csv = readCsvFirst("datasets/waveform.csv");
        if (csv != null && csv.length() > 100) {
            // Comma-separated: 21 numeric attrs + class (0/1/2)
            int[][][] parsed = DataLoader.parseMDL(csv);
            if (parsed != null) {
                System.out.println("real data (" + parsed[0].length + " rows)");
                Dataset ds = new Dataset("Waveform", parsed[0], parsed[1][0], 21, 3,
                        parsed[0].length + " instances, 21 numeric attrs, 3 classes",
                        0.01, 0.50, 83.2, 80.0, 78.1, "real");
                ds.rawData = DataLoader.parseRaw(csv);
                return ds;
            }
        }
        System.out.println("FAILED to load");
        return null;
    }

}
