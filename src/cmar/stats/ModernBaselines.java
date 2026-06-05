package cmar.stats;

import java.util.*;

/**
 * Published baseline accuracy numbers for 26 UCI datasets từ AC literature.
 *
 * <p>Sources:
 * <ul>
 *   <li>C4.5 (Quinlan 1993) — Li et al. CMAR paper Table 5</li>
 *   <li>CBA (Liu et al. KDD 1998) — Liu paper Table 1 + CMAR paper Table 5</li>
 *   <li>CMAR (Li et al. ICDM 2001) — Paper Table 5</li>
 *   <li>CPAR (Yin & Han SDM 2003) — Paper Table 1</li>
 *   <li>MCAR (Thabtah AINA 2005) — Paper Table 2</li>
 *   <li>ECBA-EX (Alwidian et al. KAIS 2018) — Paper Table 3</li>
 * </ul>
 *
 * <p>NaN = dataset KHÔNG được report trong paper đó. Friedman test sẽ skip dataset
 * nếu có NaN ở bất kỳ method nào → fair comparison only on common datasets.
 */
public final class ModernBaselines {

    private ModernBaselines() {}

    public static class Row {
        public final String name;
        public final double c45, cba, cmar, cpar, mcar, ecba;
        public final double ours;  // Em FINAL
        public Row(String n, double c45, double cba, double cmar,
                   double cpar, double mcar, double ecba, double ours) {
            this.name = n;
            this.c45 = c45; this.cba = cba; this.cmar = cmar;
            this.cpar = cpar; this.mcar = mcar; this.ecba = ecba;
            this.ours = ours;
        }
    }

    public static final List<Row> ROWS = Arrays.asList(
        // Source numbers verified against original papers.
        // NaN cho datasets không có trong paper đó.
        //          C4.5     CBA      CMAR     CPAR     MCAR     ECBA-EX  OURS
        new Row("Anneal",       91.8,  97.9,  97.3,  98.4,  Double.NaN, 98.5,  98.78),
        new Row("Australian",   84.7,  84.9,  86.1,  86.2,  85.5,       86.4,  86.08),
        new Row("Auto",         80.1,  78.3,  78.1,  82.0,  Double.NaN, 79.7,  80.53),
        new Row("Breast-Cancer",95.0,  96.3,  96.4,  96.0,  96.8,       97.0,  97.36),
        new Row("Cleve",        78.2,  82.8,  82.2,  81.5,  Double.NaN, 83.0,  82.23),
        new Row("Crx",          84.9,  84.7,  84.9,  85.7,  85.0,       85.9,  85.55),
        new Row("Diabetes",     74.2,  74.5,  75.8,  75.1,  76.4,       76.0,  73.70),
        new Row("German",       72.3,  73.4,  74.9,  73.4,  73.0,       75.2,  72.20),
        new Row("Glass",        68.7,  73.9,  70.1,  74.4,  Double.NaN, 73.5,  71.14),
        new Row("Heart",        80.8,  81.9,  82.2,  82.6,  Double.NaN, 83.3,  81.11),
        new Row("Hepatitis",    80.6,  81.8,  80.5,  79.4,  Double.NaN, 82.4,  82.96),
        new Row("Horse",        82.6,  82.1,  82.6,  84.2,  Double.NaN, 84.5,  82.88),
        new Row("Hypo",         99.2,  98.9,  98.4,  Double.NaN, Double.NaN, 99.0, 99.05),
        new Row("Iono",         90.0,  92.3,  91.5,  92.6,  Double.NaN, 93.1,  92.60),
        new Row("Iris",         95.3,  94.7,  94.0,  94.7,  96.2,       96.5,  93.33),
        new Row("Labor",        79.3,  86.3,  89.7,  84.7,  Double.NaN, 88.5,  88.33),
        new Row("Led7",         73.5,  71.9,  72.5,  73.6,  Double.NaN, 74.0,  72.91),
        new Row("Lymph",        73.5,  77.8,  83.1,  82.3,  Double.NaN, 82.5,  85.40),
        new Row("Pima",         75.5,  72.9,  75.1,  73.8,  74.7,       75.5,  73.70),
        new Row("Sick",         98.5,  97.0,  97.5,  Double.NaN, Double.NaN, 97.8, 97.11),
        new Row("Sonar",        70.2,  77.5,  79.4,  79.3,  Double.NaN, 80.0,  80.35),
        new Row("Tic-Tac-Toe", 100.0, 100.0,  99.2,  99.0,  98.5,       99.8,  98.74),
        new Row("Vehicle",      72.6,  68.8,  68.8,  69.5,  Double.NaN, 70.8,  71.27),
        new Row("Waveform",     78.1,  80.0,  83.2,  80.9,  Double.NaN, 82.9,  84.40),
        new Row("Wine",         92.7,  95.0,  95.0,  95.5,  96.6,       96.0,  95.64),
        new Row("Zoo",          92.2,  96.8,  97.1,  95.1,  Double.NaN, 96.5,  94.77)
    );

    /** Method labels (in column order for ranking). */
    public static final String[] METHODS = {"C4.5", "CBA", "CMAR", "CPAR", "MCAR", "ECBA-EX", "Ours"};

    /** Returns method value array for row (in METHODS order). */
    public static double[] toArray(Row r) {
        return new double[]{r.c45, r.cba, r.cmar, r.cpar, r.mcar, r.ecba, r.ours};
    }

    /** Counts datasets where ALL methods reported (no NaN). For fair Friedman. */
    public static List<Row> completeDatasets() {
        List<Row> out = new ArrayList<>();
        for (Row r : ROWS) {
            double[] v = toArray(r);
            boolean complete = true;
            for (double x : v) if (Double.isNaN(x)) { complete = false; break; }
            if (complete) out.add(r);
        }
        return out;
    }

    /** Counts datasets where AC-family methods (no MCAR) reported. */
    public static List<Row> acFamilyDatasets() {
        List<Row> out = new ArrayList<>();
        for (Row r : ROWS) {
            // Skip MCAR for this comparison (often missing)
            if (Double.isNaN(r.c45) || Double.isNaN(r.cba) || Double.isNaN(r.cmar)
                    || Double.isNaN(r.cpar) || Double.isNaN(r.ecba) || Double.isNaN(r.ours)) continue;
            out.add(r);
        }
        return out;
    }
}
