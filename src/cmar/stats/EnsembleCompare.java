package cmar.stats;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Compare all ensemble experiment results vs baseline. */
public final class EnsembleCompare {

    static class Row {
        String label;
        double acc, prec, recall, f1Macro, f1Weighted;
        Row(String l) { label = l; }
    }

    public static void main(String[] args) throws IOException {
        String[][] files = {
                {"results/ablation-i/BASE-stratified.md", "BASE (Stratified=10)"},
                {"results/ablation-i/I1-cost.md",          "+ CostSensitive"},
                {"results/boost/boosted-T5.md",            "Boosted T=5 (resample)"},
                {"results/boost/bagging-T10-fs07.md",      "Bagging T=10 fs=0.7"},
                {"results/boost/bagging-T10-fs10.md",      "Bagging T=10 fs=1.0"},
                {"results/boost/bagging-T20-fs10.md",      "Bagging T=20 fs=1.0"},
                {"results/boost/bayesian.md",              "Bayesian Voting"},
                {"results/boost/hyperbag-T15.md",          "HyperRandomBag T=15"},
                {"results/boost/bagging-T10-strat15.md",   "Bagging T=10 + Strat=15"},
                {"results/boost/bagging-T7.md",             "Bagging T=7"},
                {"results/boost/bagging-T10-br07.md",       "Bagging T=10 + BR=0.7"},
                {"results/boost/bagging-T10-adaptSup.md",   "Bagging T=10 + AdaptSup ⭐ WINNER"},
        };

        List<Row> rows = new ArrayList<>();
        for (String[] f : files) {
            Path p = Paths.get(f[0]);
            if (!Files.exists(p)) {
                System.out.println("(missing) " + f[0]);
                continue;
            }
            Row r = parse(p, f[1]);
            if (r != null) rows.add(r);
        }
        if (rows.isEmpty()) return;

        Row base = rows.stream().filter(r -> r.label.startsWith("+ CostSensitive")).findFirst().orElse(rows.get(0));

        StringBuilder sb = new StringBuilder();
        sb.append("# Ensemble Methods — Full Comparison vs Baseline\n\n");
        sb.append("**Baseline**: ").append(base.label)
          .append(String.format(Locale.US, "  (Acc=%.4f, F1=%.4f, R=%.4f)%n%n",
                  base.acc, base.f1Macro, base.recall));
        sb.append("| Config | Acc | ΔAcc | F1 macro | ΔF1 | Recall | ΔR | F1w | Verdict |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (Row r : rows) {
            double dA = r.acc - base.acc;
            double dF = r.f1Macro - base.f1Macro;
            double dR = r.recall - base.recall;
            String verdict;
            if (r == base) verdict = "(baseline)";
            else if (dA >= -0.001 && dF > 0.005) verdict = "✅ WIN (F1↑ ≥0.5%, Acc±noise)";
            else if (dA >= -0.001 && dF > 0.002) verdict = "🟡 Mild gain";
            else if (dA < -0.005) verdict = "❌ FAIL Acc giảm";
            else if (dF < -0.005) verdict = "❌ FAIL F1 giảm";
            else verdict = "⚪ Marginal";
            sb.append(String.format(Locale.US,
                    "| %s | %.4f | %s%.4f | %.4f | %s%.4f | %.4f | %s%.4f | %.4f | %s |%n",
                    r.label, r.acc, sign(dA), dA,
                    r.f1Macro, sign(dF), dF,
                    r.recall, sign(dR), dR,
                    r.f1Weighted, verdict));
        }
        try (FileWriter fw = new FileWriter("results/boost/ENSEMBLE-COMPARE-ALL.md")) {
            fw.write(sb.toString());
        }
        System.out.println(sb);
    }

    static String sign(double v) { return v >= 0 ? "+" : ""; }

    static Row parse(Path p, String label) throws IOException {
        String text = new String(Files.readAllBytes(p));
        // Try aggregate metrics block
        Pattern aggPat = Pattern.compile(
                "\\|\\s*Accuracy\\s*\\|\\s*([0-9.]+)\\s*\\|.*?" +
                "\\|\\s*Precision macro\\s*\\|\\s*([0-9.]+)\\s*\\|.*?" +
                "\\|\\s*\\*\\*Recall macro\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|.*?" +
                "\\|\\s*\\*\\*F1 macro\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|.*?" +
                "\\|\\s*F1 weighted\\s*\\|\\s*([0-9.]+)\\s*\\|",
                Pattern.DOTALL);
        Matcher m = aggPat.matcher(text);
        if (m.find()) {
            Row r = new Row(label);
            r.acc = Double.parseDouble(m.group(1));
            r.prec = Double.parseDouble(m.group(2));
            r.recall = Double.parseDouble(m.group(3));
            r.f1Macro = Double.parseDouble(m.group(4));
            r.f1Weighted = Double.parseDouble(m.group(5));
            return r;
        }
        // Fallback: Average row
        Pattern avgPat = Pattern.compile(
                "\\|\\s*\\*\\*Average\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|");
        Matcher m2 = avgPat.matcher(text);
        if (m2.find()) {
            Row r = new Row(label);
            r.acc = Double.parseDouble(m2.group(1));
            r.prec = Double.parseDouble(m2.group(2));
            r.recall = Double.parseDouble(m2.group(3));
            r.f1Macro = Double.parseDouble(m2.group(4));
            r.f1Weighted = Double.parseDouble(m2.group(5));
            return r;
        }
        return null;
    }
}
