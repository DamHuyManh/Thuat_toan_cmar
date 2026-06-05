package cmar.stats;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parse 5 ablation summary-reports and emit comparison table.
 * Usage: java -cp bin cmar.stats.AblationCompareI <dir>
 *
 * Looks for files: BASE-stratified.md, I1-cost.md, I2-relaxed.md, I3-laplace.md, ALL-three.md
 * Extracts "Average" row from "F1 / Precision / Recall" section.
 */
public final class AblationCompareI {

    static class Row {
        String label;
        double acc, prec, recall, f1Macro, f1Weighted;
        Row(String label) { this.label = label; }
    }

    public static void main(String[] args) throws IOException {
        String dir = args.length > 0 ? args[0] : "results/ablation-i";
        String[] files = {
                "BASE-stratified.md|BASE: Stratified=10 (current best)",
                "I1-cost.md|I1 + CostSensitive",
                "I2-relaxed.md|I2 + RelaxedUnanimity (K=3)",
                "I3-laplace.md|I3 + LaplaceWeight",
                "ALL-three.md|ALL three combined"
        };
        List<Row> rows = new ArrayList<>();
        for (String f : files) {
            String[] parts = f.split("\\|");
            Path p = Paths.get(dir, parts[0]);
            if (!Files.exists(p)) { System.out.println("Missing: " + p); continue; }
            Row r = parse(p, parts[1]);
            if (r != null) rows.add(r);
        }
        if (rows.isEmpty()) { System.out.println("No data parsed."); return; }

        Row base = rows.get(0);
        System.out.println();
        System.out.println("# Ablation Comparison — I-series (Cost-sensitive / Relaxed Unanimity / Laplace)");
        System.out.println();
        System.out.println("Baseline = " + base.label);
        System.out.printf(Locale.US, "Baseline metrics: Acc=%.4f  P=%.4f  R=%.4f  F1m=%.4f  F1w=%.4f%n",
                base.acc, base.prec, base.recall, base.f1Macro, base.f1Weighted);
        System.out.println();
        System.out.println("## Δ vs Baseline (positive = improved)");
        System.out.println();
        System.out.println("| Config | Acc | ΔAcc | P macro | ΔP | R macro | ΔR | F1 macro | ΔF1 | F1 weighted | ΔF1w |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|");
        for (Row r : rows) {
            System.out.printf(Locale.US,
                    "| %s | %.4f | %s%.4f | %.4f | %s%.4f | %.4f | %s%.4f | %.4f | %s%.4f | %.4f | %s%.4f |%n",
                    r.label,
                    r.acc, sign(r.acc - base.acc), r.acc - base.acc,
                    r.prec, sign(r.prec - base.prec), r.prec - base.prec,
                    r.recall, sign(r.recall - base.recall), r.recall - base.recall,
                    r.f1Macro, sign(r.f1Macro - base.f1Macro), r.f1Macro - base.f1Macro,
                    r.f1Weighted, sign(r.f1Weighted - base.f1Weighted), r.f1Weighted - base.f1Weighted);
        }
        System.out.println();
        System.out.println("## Verdict (honest)");
        for (int i = 1; i < rows.size(); i++) {
            Row r = rows.get(i);
            double dAcc = r.acc - base.acc;
            double dF1 = r.f1Macro - base.f1Macro;
            double dR = r.recall - base.recall;
            String tag;
            if (dAcc > 0.001 && dF1 > 0.001) tag = "✅ REAL improvement (Acc + F1 both up >0.1%)";
            else if (dAcc >= -0.0005 && dF1 > 0.001) tag = "🟡 F1 up >0.1% with no Acc loss — useful";
            else if (dAcc >= -0.0005 && dR > 0.005) tag = "🟡 Recall up >0.5% with no Acc loss — useful";
            else if (dAcc < -0.001) tag = "❌ Acc dropped >0.1% — NOT acceptable";
            else tag = "❌ Marginal (delta < noise level)";
            System.out.println("- **" + r.label + "**: ΔAcc=" + fmt(dAcc) + "  ΔF1=" + fmt(dF1) + "  ΔR=" + fmt(dR) + "  → " + tag);
        }
    }

    static String sign(double v) { return v >= 0 ? "+" : ""; }
    static String fmt(double v) { return String.format(Locale.US, "%+.4f", v); }

    static Row parse(Path p, String label) throws IOException {
        String text = new String(Files.readAllBytes(p));
        // Find F1 / Precision / Recall section's Average row
        Pattern avgPat = Pattern.compile(
                "\\|\\s*\\*\\*Average\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|\\s*\\*\\*([0-9.]+)\\*\\*\\s*\\|");
        Matcher m = avgPat.matcher(text);
        if (!m.find()) {
            System.out.println("Cannot find Average row in " + p);
            return null;
        }
        Row r = new Row(label);
        r.acc = Double.parseDouble(m.group(1));
        r.prec = Double.parseDouble(m.group(2));
        r.recall = Double.parseDouble(m.group(3));
        r.f1Macro = Double.parseDouble(m.group(4));
        r.f1Weighted = Double.parseDouble(m.group(5));
        return r;
    }
}
