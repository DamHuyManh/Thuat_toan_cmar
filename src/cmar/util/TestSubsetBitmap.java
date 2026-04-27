package cmar.util;

import cmar.Rule;
import cmar.RulePruner;
import java.util.*;

/**
 * Quick sanity test for Phase 07 G2S optimization.
 * Verifies bitmap-based G2S produces SAME output as paper-defined logic.
 */
public class TestSubsetBitmap {
    public static void main(String[] args) {
        int passed = 0, failed = 0;

        // Test 1: identical antecedents → not strict subset
        Rule g1 = make(new int[]{1,2,3}, 0, 0.9);
        Rule s1 = make(new int[]{1,2,3}, 0, 0.8);
        if (g2sCheck(g1, s1)) { System.out.println("FAIL: identical sets should not prune"); failed++; }
        else { passed++; }

        // Test 2: g={1,2}, s={1,2,3} → g is strict subset, prune s
        Rule g2 = make(new int[]{1,2}, 0, 0.9);
        Rule s2 = make(new int[]{1,2,3}, 0, 0.5);
        if (g2sCheck(g2, s2)) passed++;
        else { System.out.println("FAIL: should prune {1,2,3} given general {1,2}"); failed++; }

        // Test 3: g={1,5}, s={1,2,3} → 5 not in s → not subset
        Rule g3 = make(new int[]{1,5}, 0, 0.9);
        Rule s3 = make(new int[]{1,2,3}, 0, 0.5);
        if (g2sCheck(g3, s3)) { System.out.println("FAIL: {1,5} not subset of {1,2,3}"); failed++; }
        else { passed++; }

        // Test 4: high item IDs (>64) — bitmap with multiple words
        Rule g4 = make(new int[]{100,150}, 0, 0.9);
        Rule s4 = make(new int[]{50,100,150,200}, 0, 0.5);
        if (g2sCheck(g4, s4)) passed++;
        else { System.out.println("FAIL: multi-word bitmap subset"); failed++; }

        // Test 5: different class → don't prune
        Rule g5 = make(new int[]{1,2}, 0, 0.9);
        Rule s5 = make(new int[]{1,2,3}, 1, 0.5);
        if (g2sCheck(g5, s5)) { System.out.println("FAIL: different class should not prune"); failed++; }
        else { passed++; }

        // Test 6: same confidence (not >) → don't prune
        Rule g6 = make(new int[]{1,2}, 0, 0.9);
        Rule s6 = make(new int[]{1,2,3}, 0, 0.9);
        if (g2sCheck(g6, s6)) { System.out.println("FAIL: equal confidence should not prune"); failed++; }
        else { passed++; }

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static Rule make(int[] antecedent, int cls, double conf) {
        Arrays.sort(antecedent);
        Rule r = new Rule(antecedent, cls, 10, conf);
        return r;
    }

    /** Run G2S on [general, specific] and check if specific was pruned. */
    private static boolean g2sCheck(Rule general, Rule specific) {
        RulePruner pruner = new RulePruner(0.0, 4, 0.0);
        List<Rule> in = new ArrayList<>();
        in.add(general);
        in.add(specific);
        List<Rule> out = pruner.generalToSpecificPrune(in);
        boolean specificKept = false;
        for (Rule r : out) {
            if (r.equals(specific)) { specificKept = true; break; }
        }
        return !specificKept; // true if specific was pruned
    }
}
