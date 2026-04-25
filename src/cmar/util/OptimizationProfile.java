package cmar.util;

/**
 * Flag để switch giữa baseline CMAR vs improved version.
 * Dùng cho demo so sánh trong báo cáo.
 */
public final class OptimizationProfile {
    public enum Mode { BASELINE, IMPROVED }

    private static volatile Mode mode = Mode.IMPROVED;

    public static void setMode(Mode m) { mode = m; }
    public static Mode getMode() { return mode; }
    public static boolean isImproved() { return mode == Mode.IMPROVED; }
    public static boolean isBaseline() { return mode == Mode.BASELINE; }
}
