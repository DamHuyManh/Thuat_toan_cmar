package cmar;

import java.util.*;

/**
 * Demo & benchmark for CMAR Classifier.
 * Includes sample datasets and performance measurement.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   CMAR - Optimized Java Edition      ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // Correctness illustration on the standard fixed "weather" toy example.
        // For real benchmarks on 26 UCI datasets, run cmar.boost.BoostedBenchmarkRunner.
        demoSmallDataset();
    }

    /**
     * Small dataset demo - verifies correctness.
     * Simulates: weather dataset (Outlook, Temp, Humidity, Wind => Play?)
     * Items encoded as integers.
     */
    static void demoSmallDataset() {
        System.out.println("--- Demo: Weather Dataset ---");

        // Encoding:
        // Outlook: sunny=0, overcast=1, rain=2
        // Temp: hot=3, mild=4, cool=5
        // Humidity: high=6, normal=7
        // Wind: weak=8, strong=9
        int[][] transactions = {
            {0, 3, 6, 8},  // sunny, hot, high, weak => no
            {0, 3, 6, 9},  // sunny, hot, high, strong => no
            {1, 3, 6, 8},  // overcast, hot, high, weak => yes
            {2, 4, 6, 8},  // rain, mild, high, weak => yes
            {2, 5, 7, 8},  // rain, cool, normal, weak => yes
            {2, 5, 7, 9},  // rain, cool, normal, strong => no
            {1, 5, 7, 9},  // overcast, cool, normal, strong => yes
            {0, 4, 6, 8},  // sunny, mild, high, weak => no
            {0, 5, 7, 8},  // sunny, cool, normal, weak => yes
            {2, 4, 7, 8},  // rain, mild, normal, weak => yes
            {0, 4, 7, 9},  // sunny, mild, normal, strong => yes
            {1, 4, 6, 9},  // overcast, mild, high, strong => yes
            {1, 3, 7, 8},  // overcast, hot, normal, weak => yes
            {2, 4, 6, 9},  // rain, mild, high, strong => no
        };

        // Labels: 0=no, 1=yes
        int[] labels = {0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0};

        // Train
        CMARClassifier cmar = new CMARClassifier(2, 0.3, 2.0, 4, 1000);
        cmar.fit(transactions, labels);
        cmar.printStats();

        // Test on training data
        double accuracy = cmar.score(transactions, labels);
        System.out.println("Training accuracy:  " + String.format("%.1f%%", accuracy * 100));

        // Predict new instance: overcast, mild, normal, weak
        int[] newInstance = {1, 4, 7, 8};
        int prediction = cmar.predict(newInstance);
        System.out.println("Predict {overcast,mild,normal,weak}: class " + prediction
                + " (" + (prediction == 1 ? "yes" : "no") + ")");

        // Show top rules
        List<Rule> rules = cmar.getRules();
        System.out.println("\nTop rules (max 10):");
        for (int i = 0; i < Math.min(10, rules.size()); i++) {
            System.out.println("  " + rules.get(i));
        }
    }

}
