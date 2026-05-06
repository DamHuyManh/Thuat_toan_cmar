package cmar.util;

import java.util.*;

/**
 * Phase timer dùng ThreadLocal để đo wall-clock từng giai đoạn CMAR pipeline.
 *
 * <b>Trade-off ThreadLocal:</b>
 * - Hiện tại CMAR chạy single-thread mỗi fold → ThreadLocal đơn giản, không khoá.
 * - Nếu sau này song song hoá (parallel FP-growth, multi-fold concurrent) thì
 *   metrics sẽ tách per-thread → cần aggregator gộp lại trước khi report.
 * - Caller bắt buộc gọi {@link #reset()} đầu mỗi fold (BenchmarkRunner đã làm).
 *
 * Phase 01 Baseline Measurement.
 */
public class PhaseTimer {
    private static final ThreadLocal<Map<String, Long>> TIMES =
            ThreadLocal.withInitial(LinkedHashMap::new);
    private static final ThreadLocal<Map<String, Long>> STARTS =
            ThreadLocal.withInitial(HashMap::new);

    public static void start(String phase) {
        STARTS.get().put(phase, System.nanoTime());
    }

    public static long stop(String phase) {
        Long s = STARTS.get().remove(phase);
        if (s == null) return 0;
        long elapsed = System.nanoTime() - s;
        TIMES.get().merge(phase, elapsed, Long::sum);
        return elapsed;
    }

    public static long getNanos(String phase) {
        return TIMES.get().getOrDefault(phase, 0L);
    }

    public static double getMillis(String phase) {
        return TIMES.get().getOrDefault(phase, 0L) / 1_000_000.0;
    }

    public static Map<String, Double> snapshotMillis() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : TIMES.get().entrySet()) {
            out.put(e.getKey(), e.getValue() / 1_000_000.0);
        }
        return out;
    }

    public static void reset() {
        TIMES.get().clear();
        STARTS.get().clear();
    }
}
