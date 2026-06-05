package cmar.stats;

import java.util.*;

/**
 * Published baseline accuracy numbers for UCI datasets from AC literature.
 *
 * <p>Sources (per-dataset numbers from original paper tables):
 * <ul>
 *   <li>C4.5 (Quinlan 1993) — via Li et al. CMAR paper Table 5</li>
 *   <li>CBA (Liu et al. KDD 1998) — Liu Table 1 + CMAR paper Table 5</li>
 *   <li>CMAR (Li et al. ICDM 2001) — Paper Table 5</li>
 *   <li>CPAR (Yin &amp; Han SDM 2003) — Paper Table 1</li>
 *   <li>MCAR (Thabtah AINA 2005) — Paper Table 2 (partial coverage)</li>
 * </ul>
 *
 * <p><b>IMPORTANT (honesty)</b>: The ECBA-EX column was REMOVED — its venue/numbers could not be
 * verified against a primary source. Before paper submission, cross-check every remaining
 * per-dataset value against the original paper tables; treat any value you cannot trace as
 * provisional. NaN = dataset not reported in that paper → Friedman skips it for fairness.
 */
public final class ModernBaselines {

    private ModernBaselines() {}

    public static class Row {
        public final String name;
        public final double c45, cba, cmar, cpar, mcar;
        public final double ours;  // Our improved CMAR (verified live run)
        public Row(String n, double c45, double cba, double cmar,
                   double cpar, double mcar, double ours) {
            this.name = n;
            this.c45 = c45; this.cba = cba; this.cmar = cmar;
            this.cpar = cpar; this.mcar = mcar;
            this.ours = ours;
        }
    }

    public static final List<Row> ROWS = Arrays.asList(
        //          C4.5     CBA      CMAR     CPAR     MCAR        OURS
        new Row("Anneal",       91.8,  97.9,  97.3,  98.4,  Double.NaN, 98.66),
        new Row("Australian",   84.7,  84.9,  86.1,  86.2,  85.5,       86.08),
        new Row("Auto",         80.1,  78.3,  78.1,  82.0,  Double.NaN, 81.53),
        new Row("Breast-Cancer",95.0,  96.3,  96.4,  96.0,  96.8,       97.22),
        new Row("Cleve",        78.2,  82.8,  82.2,  81.5,  Double.NaN, 82.58),
        new Row("Crx",          84.9,  84.7,  84.9,  85.7,  85.0,       84.97),
        new Row("Diabetes",     74.2,  74.5,  75.8,  75.1,  76.4,       73.70),
        new Row("German",       72.3,  73.4,  74.9,  73.4,  73.0,       73.20),
        new Row("Glass",        68.7,  73.9,  70.1,  74.4,  Double.NaN, 71.14),
        new Row("Heart",        80.8,  81.9,  82.2,  82.6,  Double.NaN, 80.37),
        new Row("Hepatitis",    80.6,  81.8,  80.5,  79.4,  Double.NaN, 84.21),
        new Row("Horse",        82.6,  82.1,  82.6,  84.2,  Double.NaN, 82.89),
        new Row("Hypo",         99.2,  98.9,  98.4,  Double.NaN, Double.NaN, 99.15),
        new Row("Iono",         90.0,  92.3,  91.5,  92.6,  Double.NaN, 92.29),
        new Row("Iris",         95.3,  94.7,  94.0,  94.7,  96.2,       93.33),
        new Row("Labor",        79.3,  86.3,  89.7,  84.7,  Double.NaN, 88.33),
        new Row("Led7",         73.5,  71.9,  72.5,  73.6,  Double.NaN, 72.91),
        new Row("Lymph",        73.5,  77.8,  83.1,  82.3,  Double.NaN, 84.69),
        new Row("Pima",         75.5,  72.9,  75.1,  73.8,  74.7,       73.70),
        new Row("Sick",         98.5,  97.0,  97.5,  Double.NaN, Double.NaN, 97.14),
        new Row("Sonar",        70.2,  77.5,  79.4,  79.3,  Double.NaN, 80.80),
        new Row("Tic-Tac-Toe", 100.0, 100.0,  99.2,  99.0,  98.5,       98.74),
        new Row("Vehicle",      72.6,  68.8,  68.8,  69.5,  Double.NaN, 71.15),
        new Row("Waveform",     78.1,  80.0,  83.2,  80.9,  Double.NaN, 83.96),
        new Row("Wine",         92.7,  95.0,  95.0,  95.5,  96.6,       96.20),
        new Row("Zoo",          92.2,  96.8,  97.1,  95.1,  Double.NaN, 95.61)
    );

    /** Method labels (in column order for ranking). */
    public static final String[] METHODS = {"C4.5", "CBA", "CMAR", "CPAR", "MCAR", "Ours"};

    /** Returns method value array for row (in METHODS order). */
    public static double[] toArray(Row r) {
        return new double[]{r.c45, r.cba, r.cmar, r.cpar, r.mcar, r.ours};
    }

    /** Datasets where ALL methods reported (no NaN). For fair Friedman. */
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

    /** Datasets where AC-family methods (C4.5/CBA/CMAR/CPAR/Ours, no MCAR) reported. */
    public static List<Row> acFamilyDatasets() {
        List<Row> out = new ArrayList<>();
        for (Row r : ROWS) {
            if (Double.isNaN(r.c45) || Double.isNaN(r.cba) || Double.isNaN(r.cmar)
                    || Double.isNaN(r.cpar) || Double.isNaN(r.ours)) continue;
            out.add(r);
        }
        return out;
    }
}
