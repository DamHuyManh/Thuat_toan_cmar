package cmar.stats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * So sánh topK 5/7/10 vs baseline topK=0 — per-dataset Acc/F1/Recall + delta.
 *
 * <p>Đọc 4 file: TOPK-0.md (baseline), TOPK-5.md, TOPK-7.md, TOPK-10.md
 * Mỗi dòng dataset: | name | paper% | acc% | dAcc% | f1 | recall | rounds | rules |
 *
 * <p>Xuất bảng per-dataset với:
 *   - Acc + ΔAcc (so baseline topK=0)
 *   - F1 + ΔF1
 *   - Recall + ΔRecall
 */
public final class TopKCompare {

    static class Row {
        String name;
        double acc, f1, recall;
    }

    public static void main(String[] args) throws IOException {
        Map<String, Row> base = parse("results/TOPK-0.md");
        Map<String, Row> k5   = parse("results/TOPK-5.md");
        Map<String, Row> k7   = parse("results/TOPK-7.md");
        Map<String, Row> k10  = parse("results/TOPK-10.md");

        StringBuilder sb = new StringBuilder();
        sb.append("# So sánh Top-K = 5 / 7 / 10 (per-dataset)\n\n");
        sb.append("**Baseline cho Δ**: topK=0 (vote tất cả luật). Δ = config − baseline.\n");
        sb.append("**Cấu hình chung**: Bagging T=10, fs=1.0, stratified=10, costSensitive, adaptMinSup sqrt, minSupScale=0.3\n\n");

        // Per-dataset F1 table
        sb.append("## F1-macro per dataset\n\n");
        sb.append("| Dataset | topK=0 | topK=5 | ΔF1(5) | topK=7 | ΔF1(7) | topK=10 | ΔF1(10) |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        List<String> names = new ArrayList<>(base.keySet());
        for (String n : names) {
            double b = base.get(n).f1;
            double f5 = k5.containsKey(n) ? k5.get(n).f1 : Double.NaN;
            double f7 = k7.containsKey(n) ? k7.get(n).f1 : Double.NaN;
            double f10 = k10.containsKey(n) ? k10.get(n).f1 : Double.NaN;
            sb.append(String.format(Locale.US, "| %s | %.4f | %.4f | %s | %.4f | %s | %.4f | %s |%n",
                    n, b, f5, d(f5 - b), f7, d(f7 - b), f10, d(f10 - b)));
        }
        sb.append(avgRow("F1", base, k5, k7, k10, r -> r.f1));

        // Per-dataset Recall table
        sb.append("\n## Recall-macro per dataset\n\n");
        sb.append("| Dataset | topK=0 | topK=5 | ΔR(5) | topK=7 | ΔR(7) | topK=10 | ΔR(10) |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (String n : names) {
            double b = base.get(n).recall;
            double r5 = k5.containsKey(n) ? k5.get(n).recall : Double.NaN;
            double r7 = k7.containsKey(n) ? k7.get(n).recall : Double.NaN;
            double r10 = k10.containsKey(n) ? k10.get(n).recall : Double.NaN;
            sb.append(String.format(Locale.US, "| %s | %.4f | %.4f | %s | %.4f | %s | %.4f | %s |%n",
                    n, b, r5, d(r5 - b), r7, d(r7 - b), r10, d(r10 - b)));
        }
        sb.append(avgRow("Recall", base, k5, k7, k10, r -> r.recall));

        // Per-dataset Acc table
        sb.append("\n## Accuracy per dataset\n\n");
        sb.append("| Dataset | topK=0 | topK=5 | ΔAcc(5) | topK=7 | ΔAcc(7) | topK=10 | ΔAcc(10) |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (String n : names) {
            double b = base.get(n).acc;
            double a5 = k5.containsKey(n) ? k5.get(n).acc : Double.NaN;
            double a7 = k7.containsKey(n) ? k7.get(n).acc : Double.NaN;
            double a10 = k10.containsKey(n) ? k10.get(n).acc : Double.NaN;
            sb.append(String.format(Locale.US, "| %s | %.2f%% | %.2f%% | %s | %.2f%% | %s | %.2f%% | %s |%n",
                    n, b, a5, dp(a5 - b), a7, dp(a7 - b), a10, dp(a10 - b)));
        }
        sb.append(avgRowAcc(base, k5, k7, k10));

        // Summary
        sb.append("\n## Tóm tắt trung bình 26 datasets\n\n");
        sb.append("| Config | Avg Acc | ΔAcc | Avg F1 | ΔF1 | Avg Recall | ΔRecall |\n");
        sb.append("|---|---:|---:|---:|---:|---:|---:|\n");
        double bAcc = avg(base, r -> r.acc), bF1 = avg(base, r -> r.f1), bR = avg(base, r -> r.recall);
        summaryLine(sb, "topK=0 (baseline)", bAcc, 0, bF1, 0, bR, 0);
        summaryLine(sb, "topK=5", avg(k5, r->r.acc), avg(k5,r->r.acc)-bAcc, avg(k5,r->r.f1), avg(k5,r->r.f1)-bF1, avg(k5,r->r.recall), avg(k5,r->r.recall)-bR);
        summaryLine(sb, "topK=7", avg(k7, r->r.acc), avg(k7,r->r.acc)-bAcc, avg(k7,r->r.f1), avg(k7,r->r.f1)-bF1, avg(k7,r->r.recall), avg(k7,r->r.recall)-bR);
        summaryLine(sb, "topK=10", avg(k10, r->r.acc), avg(k10,r->r.acc)-bAcc, avg(k10,r->r.f1), avg(k10,r->r.f1)-bF1, avg(k10,r->r.recall), avg(k10,r->r.recall)-bR);

        String out = "results/TOPK-COMPARE.md";
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
        }
        System.out.println(sb);
        System.out.println("Saved: " + out);
    }

    interface Getter { double get(Row r); }

    static double avg(Map<String, Row> m, Getter g) {
        double s = 0; int c = 0;
        for (Row r : m.values()) { s += g.get(r); c++; }
        return c > 0 ? s / c : 0;
    }

    static void summaryLine(StringBuilder sb, String name, double acc, double dAcc,
                            double f1, double dF1, double rec, double dR) {
        sb.append(String.format(Locale.US, "| **%s** | %.2f%% | %s | %.4f | %s | %.4f | %s |%n",
                name, acc, dp(dAcc), f1, d(dF1), rec, d(dR)));
    }

    static String avgRow(String label, Map<String,Row> base, Map<String,Row> k5,
                         Map<String,Row> k7, Map<String,Row> k10, Getter g) {
        double b = avg(base, g), v5 = avg(k5, g), v7 = avg(k7, g), v10 = avg(k10, g);
        return String.format(Locale.US,
                "| **AVG** | **%.4f** | **%.4f** | **%s** | **%.4f** | **%s** | **%.4f** | **%s** |%n",
                b, v5, d(v5-b), v7, d(v7-b), v10, d(v10-b));
    }

    static String avgRowAcc(Map<String,Row> base, Map<String,Row> k5,
                            Map<String,Row> k7, Map<String,Row> k10) {
        double b = avg(base, r->r.acc), v5 = avg(k5, r->r.acc), v7 = avg(k7, r->r.acc), v10 = avg(k10, r->r.acc);
        return String.format(Locale.US,
                "| **AVG** | **%.2f%%** | **%.2f%%** | **%s** | **%.2f%%** | **%s** | **%.2f%%** | **%s** |%n",
                b, v5, dp(v5-b), v7, dp(v7-b), v10, dp(v10-b));
    }

    /** Format delta (4 decimal, with sign). */
    static String d(double v) {
        if (Double.isNaN(v)) return "—";
        return (v >= 0 ? "+" : "") + String.format(Locale.US, "%.4f", v);
    }
    /** Format delta percent. */
    static String dp(double v) {
        if (Double.isNaN(v)) return "—";
        return (v >= 0 ? "+" : "") + String.format(Locale.US, "%.2f", v);
    }

    /** Parse benchmark MD: | name | paper% | acc% | dAcc% | f1 | recall | rounds | rules | */
    static Map<String, Row> parse(String path) throws IOException {
        Map<String, Row> out = new LinkedHashMap<>();
        Path p = Paths.get(path);
        if (!Files.exists(p)) { System.out.println("(missing) " + path); return out; }
        Pattern row = Pattern.compile(
                "^\\|\\s*([A-Za-z0-9\\-]+)\\s*\\|\\s*[0-9.]+%\\s*\\|\\s*\\*?\\*?([0-9.]+)%\\*?\\*?\\s*\\|" +
                "\\s*[+\\-][0-9.]+%\\s*\\|\\s*([0-9.]+)\\s*\\|\\s*([0-9.]+)\\s*\\|");
        for (String line : Files.readAllLines(p)) {
            if (line.contains("Average") || line.contains("Dataset")) continue;
            Matcher m = row.matcher(line.trim());
            if (m.find()) {
                Row r = new Row();
                r.name = m.group(1);
                r.acc = Double.parseDouble(m.group(2));
                r.f1 = Double.parseDouble(m.group(3));
                r.recall = Double.parseDouble(m.group(4));
                out.put(r.name, r);
            }
        }
        return out;
    }
}
