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

        // Run demos
        demoSmallDataset();
        System.out.println();
        demoBenchmark();
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

    /**
     * Benchmark with synthetic data.
     */
    static void demoBenchmark() {
        System.out.println("--- Benchmark: Synthetic Dataset ---");

        int numTransactions = 5000;
        int numItems = 50;
        int numClasses = 5;
        int itemsPerTransaction = 8;

        Random rng = new Random(42);

        // Generate synthetic data
        int[][] transactions = new int[numTransactions][];
        int[] labels = new int[numTransactions];

        for (int i = 0; i < numTransactions; i++) {
            Set<Integer> items = new HashSet<>();
            int classLabel = rng.nextInt(numClasses);
            labels[i] = classLabel;

            // Add some class-correlated items
            items.add(classLabel * 3);
            items.add(classLabel * 3 + 1);

            // Add random items
            while (items.size() < itemsPerTransaction) {
                items.add(rng.nextInt(numItems));
            }

            transactions[i] = items.stream().mapToInt(Integer::intValue).toArray();
        }

        // Split 80/20
        int trainSize = (int) (numTransactions * 0.8);
        int[][] trainData = Arrays.copyOf(transactions, trainSize);
        int[] trainLabels = Arrays.copyOf(labels, trainSize);
        int[][] testData = Arrays.copyOfRange(transactions, trainSize, numTransactions);
        int[] testLabels = Arrays.copyOfRange(labels, trainSize, numTransactions);

        // Train & evaluate
        CMARClassifier cmar = new CMARClassifier(10, 0.4, 3.841, 4, 2000);

        long startTrain = System.nanoTime();
        cmar.fit(trainData, trainLabels);
        long trainTime = (System.nanoTime() - startTrain) / 1_000_000;

        long startPred = System.nanoTime();
        double accuracy = cmar.score(testData, testLabels);
        long predTime = (System.nanoTime() - startPred) / 1_000_000;

        System.out.println("Dataset:            " + numTransactions + " transactions, "
                + numItems + " items, " + numClasses + " classes");
        System.out.println("Train/Test split:   " + trainSize + "/" + (numTransactions - trainSize));
        cmar.printStats();
        System.out.println("Test accuracy:      " + String.format("%.1f%%", accuracy * 100));
        System.out.println("Training time:      " + trainTime + " ms");
        System.out.println("Prediction time:    " + predTime + " ms ("
                + String.format("%.3f", (double) predTime / testData.length) + " ms/instance)");
    }
}
