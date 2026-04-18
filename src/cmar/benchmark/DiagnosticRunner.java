package cmar.benchmark;

import cmar.*;
import java.util.*;

/**
 * Diagnostic tool to analyze rule mining and classification behavior.
 */
public class DiagnosticRunner {
    public static void main(String[] args) {
        List<UCIDatasets.Dataset> datasets = UCIDatasets.getAllDatasets();

        for (UCIDatasets.Dataset ds : datasets) {
            System.out.println("\n=== " + ds.name + " ===");
            int n = ds.numInstances;
            int trainN = n * 9 / 10;
            int minSup = Math.max(1, (int)(ds.paperMinSupport * trainN));

            // Train on all data for diagnostic
            CMARClassifier cmar = new CMARClassifier(minSup, ds.paperMinConfidence, 3.841, 4, 5000);
            cmar.fit(ds.transactions, ds.labels);

            List<Rule> rules = cmar.getRules();
            System.out.println("  MinSup=" + minSup + " MinConf=" + ds.paperMinConfidence);
            System.out.println("  Rules mined: " + cmar.getTotalRulesMined());
            System.out.println("  Rules after prune: " + cmar.getTotalRulesAfterPrune());

            // Per-class rule distribution
            Map<Integer, Integer> rulesByClass = new TreeMap<>();
            Map<Integer, Double> avgConfByClass = new TreeMap<>();
            for (Rule r : rules) {
                rulesByClass.merge(r.getClassLabel(), 1, Integer::sum);
                avgConfByClass.merge(r.getClassLabel(), r.getConfidence(), Double::sum);
            }

            // Class distribution in data
            Map<Integer, Integer> classDist = new TreeMap<>();
            for (int l : ds.labels) classDist.merge(l, 1, Integer::sum);

            System.out.println("  Class distribution (data): " + classDist);
            System.out.println("  Rules per class: " + rulesByClass);

            for (Map.Entry<Integer, Double> e : avgConfByClass.entrySet()) {
                int cls = e.getKey();
                int cnt = rulesByClass.getOrDefault(cls, 1);
                System.out.printf("  Class %d: %d rules, avg conf=%.3f%n", cls, cnt, e.getValue() / cnt);
            }

            // Check prediction accuracy
            double acc = cmar.score(ds.transactions, ds.labels);
            System.out.printf("  Training accuracy: %.1f%%%n", acc * 100);
        }
    }
}
