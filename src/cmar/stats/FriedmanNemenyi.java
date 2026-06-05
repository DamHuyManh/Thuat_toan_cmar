package cmar.stats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Friedman + Nemenyi statistical test cho multi-method comparison.
 *
 * <p>Reference: Demšar (2006) "Statistical Comparisons of Classifiers over Multiple Data Sets" JMLR.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Rank methods per dataset (lower rank = higher accuracy).</li>
 *   <li>Compute average rank R_j per method.</li>
 *   <li>Friedman statistic: χ²_F = (12N / (k(k+1))) · (Σ R_j² − k(k+1)²/4)</li>
 *   <li>F_F = (N-1)·χ²_F / (N·(k-1) − χ²_F), distributed F(k-1, (k-1)(N-1))</li>
 *   <li>If reject H₀, Nemenyi CD = q_α · sqrt(k(k+1) / (6N))</li>
 *   <li>Two methods significantly differ if |R_i − R_j| > CD.</li>
 * </ol>
 *
 * <p>Critical values q_α for two-tailed Nemenyi (α=0.05):
 *   k=2: 1.960, k=3: 2.343, k=4: 2.569, k=5: 2.728, k=6: 2.850,
 *   k=7: 2.949, k=8: 3.031, k=9: 3.102, k=10: 3.164.
 */
public final class FriedmanNemenyi {

    /** Nemenyi critical values q_α for α=0.05 (two-tailed), index by k. */
    private static final double[] Q_005 = {
            0,     0,     1.960, 2.343, 2.569, 2.728, 2.850, 2.949, 3.031, 3.102, 3.164
    };

    public static void main(String[] args) throws IOException {
        // Use AC-family complete datasets (no NaN in C4.5, CBA, CMAR, CPAR, ECBA, Ours)
        List<ModernBaselines.Row> rows = ModernBaselines.acFamilyDatasets();
        String[] methods = {"C4.5", "CBA", "CMAR", "CPAR", "ECBA-EX", "Ours"};
        int N = rows.size(), k = methods.length;

        // Build accuracy matrix [N datasets × k methods]
        double[][] acc = new double[N][k];
        for (int i = 0; i < N; i++) {
            ModernBaselines.Row r = rows.get(i);
            acc[i][0] = r.c45;
            acc[i][1] = r.cba;
            acc[i][2] = r.cmar;
            acc[i][3] = r.cpar;
            acc[i][4] = r.ecba;
            acc[i][5] = r.ours;
        }

        // Rank within each dataset (1 = best/highest acc, ties get average rank)
        double[][] ranks = new double[N][k];
        for (int i = 0; i < N; i++) ranks[i] = rankRow(acc[i]);

        // Average rank per method
        double[] avgRank = new double[k];
        for (int j = 0; j < k; j++) {
            for (int i = 0; i < N; i++) avgRank[j] += ranks[i][j];
            avgRank[j] /= N;
        }

        // Friedman χ²_F
        double sumR2 = 0;
        for (double r : avgRank) sumR2 += r * r;
        double chi2F = (12.0 * N / (k * (k + 1.0))) * (sumR2 - k * (k + 1.0) * (k + 1.0) / 4.0);
        double F_F = (N - 1) * chi2F / (N * (k - 1) - chi2F);

        // Nemenyi critical difference (α=0.05)
        double q = Q_005[k];
        double CD = q * Math.sqrt(k * (k + 1.0) / (6.0 * N));

        // Generate report
        StringBuilder sb = new StringBuilder();
        sb.append("# Friedman + Nemenyi Statistical Test\n\n");
        sb.append("**Reference**: Demšar (2006) JMLR.\n\n");
        sb.append("**Datasets used**: ").append(N).append(" datasets where ALL methods reported (no NaN)\n");
        sb.append("**Methods**: ").append(k).append(" methods → ");
        for (String m : methods) sb.append(m).append(", ");
        sb.append("\n\n");

        sb.append("## Per-dataset accuracy + rank\n\n");
        sb.append("| Dataset |");
        for (String m : methods) sb.append(" ").append(m).append(" |");
        sb.append("\n|---|");
        for (int j = 0; j < k; j++) sb.append("---:|");
        sb.append("\n");
        for (int i = 0; i < N; i++) {
            sb.append("| ").append(rows.get(i).name);
            for (int j = 0; j < k; j++) {
                sb.append(String.format(Locale.US, " | %.2f (R%.1f)", acc[i][j], ranks[i][j]));
            }
            sb.append(" |\n");
        }
        sb.append("| **Average rank** |");
        for (double r : avgRank) sb.append(String.format(Locale.US, " **%.3f** |", r));
        sb.append("\n\n");

        sb.append("## Friedman test\n\n");
        sb.append(String.format(Locale.US, "- N (datasets) = %d, k (methods) = %d%n", N, k));
        sb.append(String.format(Locale.US, "- χ²_F = %.4f%n", chi2F));
        sb.append(String.format(Locale.US, "- F_F = %.4f (df1=%d, df2=%d)%n", F_F, k - 1, (k - 1) * (N - 1)));
        sb.append(String.format(Locale.US,
                "- Critical F(α=0.05, df1=%d, df2=%d) ≈ %.3f%n",
                k - 1, (k - 1) * (N - 1), criticalF(k - 1, (k - 1) * (N - 1))));
        boolean reject = F_F > criticalF(k - 1, (k - 1) * (N - 1));
        sb.append("- **H₀ rejected**: ").append(reject ? "YES" : "NO")
                .append(" — methods ").append(reject ? "DO differ significantly" : "may NOT differ").append("\n\n");

        sb.append("## Nemenyi post-hoc (α=0.05)\n\n");
        sb.append(String.format(Locale.US,
                "- Critical Difference **CD = %.3f**%n%n", CD));
        sb.append("| Method A | Method B | \\|R_A - R_B\\| | Significant? |\n");
        sb.append("|---|---|---:|:---:|\n");
        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                double diff = Math.abs(avgRank[i] - avgRank[j]);
                boolean sig = diff > CD;
                sb.append(String.format(Locale.US,
                        "| %s | %s | %.3f | %s |%n",
                        methods[i], methods[j], diff, sig ? "✅ YES" : "—"));
            }
        }
        sb.append("\n");

        sb.append("## Ranked methods (best → worst)\n\n");
        Integer[] order = new Integer[k];
        for (int j = 0; j < k; j++) order[j] = j;
        Arrays.sort(order, (a, b) -> Double.compare(avgRank[a], avgRank[b]));
        for (int r = 0; r < k; r++) {
            sb.append(String.format(Locale.US, "%d. **%s** — avg rank %.3f%n",
                    r + 1, methods[order[r]], avgRank[order[r]]));
        }

        sb.append("\n## Critical Difference diagram (text)\n\n");
        sb.append("```\n");
        sb.append(String.format(Locale.US, "  CD = %.3f%n", CD));
        sb.append("\n  rank: 1.0 ----- 2.0 ----- 3.0 ----- 4.0 ----- 5.0 ----- 6.0\n");
        sb.append("  -----+---------+---------+---------+---------+---------+----->\n");
        for (Integer ix : order) {
            sb.append(String.format(Locale.US, "    %s @ %.3f%n", methods[ix], avgRank[ix]));
        }
        sb.append("```\n");

        String out = "results/FRIEDMAN-NEMENYI.md";
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        System.out.println(sb);
        System.out.println("Report saved: " + out);
    }

    /** Rank values where 1 = highest (best), ties get average rank. */
    static double[] rankRow(double[] vals) {
        int k = vals.length;
        Integer[] idx = new Integer[k];
        for (int i = 0; i < k; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(vals[b], vals[a])); // DESC

        double[] r = new double[k];
        int i = 0;
        while (i < k) {
            int j = i;
            while (j + 1 < k && vals[idx[j + 1]] == vals[idx[i]]) j++;
            double avg = (i + 1 + j + 1) / 2.0;
            for (int m = i; m <= j; m++) r[idx[m]] = avg;
            i = j + 1;
        }
        return r;
    }

    /**
     * Approximate critical F value for α=0.05.
     * Returns crude approximation for common (df1, df2) values.
     * For precise values use scipy.stats.f.ppf(0.95, df1, df2).
     */
    static double criticalF(int df1, int df2) {
        // Lookup table for common cases (df1=5, various df2)
        if (df1 == 5) {
            if (df2 >= 100) return 2.31;
            if (df2 >= 60) return 2.37;
            if (df2 >= 40) return 2.45;
            if (df2 >= 30) return 2.53;
            if (df2 >= 20) return 2.71;
            if (df2 >= 15) return 2.90;
            if (df2 >= 10) return 3.33;
        }
        if (df1 == 6) {
            if (df2 >= 100) return 2.19;
            if (df2 >= 60) return 2.25;
            if (df2 >= 40) return 2.34;
            if (df2 >= 30) return 2.42;
            if (df2 >= 20) return 2.60;
            if (df2 >= 15) return 2.79;
            if (df2 >= 10) return 3.22;
        }
        // Conservative fallback
        return 2.50;
    }
}
