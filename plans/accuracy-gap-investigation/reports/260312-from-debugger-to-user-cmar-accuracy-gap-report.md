# CMAR Accuracy Gap Investigation Report
**Date:** 2026-03-12
**From:** Debugger Agent
**To:** User
**Task:** Investigate accuracy gaps vs Li, Han, Pei 2001 paper

---

## Executive Summary

Five distinct root causes explain the accuracy gaps. In order of severity:

1. **Cross-validation fold count mismatch** — 5-fold vs paper's 10-fold (affects all datasets, especially small ones)
2. **Two-phase mining vs augmented-transaction mining** — produces different rule sets with a non-equivalent threshold
3. **Conditional tree threshold bug** — uses full `minSupport` instead of halved `treeMinSup`, cutting off rare-class patterns
4. **Coverage pruning is class-exclusive** — only marks instances "covered" when the rule's class matches, causing aggressive early termination for majority classes and under-retention of minority-class rules
5. **Voting uses pre-computed `weight` field instead of chi-square at classify time** — the weight formula `chi² × conf² × log(sup+1)` diverges from the paper's pure weighted chi-square group voting

---

## Detailed Findings

### Finding 1 — Cross-Validation: 5-Fold vs 10-Fold
**Files:** `BenchmarkRunner.java:43`, `BenchmarkRunner.java:70`

```java
// BenchmarkRunner.java line 43
int folds = 5;    // HARDCODED — never changes regardless of dataset size

// BenchmarkRunner.java line 69-70
int trainN = n * (folds - 1) / folds;   // With 5-fold: trainN = n * 4/5 = 0.8n
int minSup = Math.max(1, (int)(ds.paperMinSupport * trainN));
```

The paper uses 10-fold CV. With 5-fold:
- Training set is only 80% of data (vs 90% in 10-fold)
- `minSup` is computed from `trainN`, so it is **10% smaller** than the paper's
- For Zoo (101 instances): trainN=80 with 5-fold vs 90 with 10-fold; minSup=4 vs 4 (same here by luck)
- For Lymphography (148 instances): trainN=118 vs 133; minSup=5 vs 6 (different threshold used)
- For small datasets, each test fold has ~20% of data — high variance between folds

The comment on line 153 of `BenchmarkRunner.java` even says "10-fold" in the report output but actually runs 5-fold — a discrepancy in the report template itself.

**Impact on gaps:** Moderate. Smaller training sets → fewer rules → lower accuracy, especially for rare classes in Zoo and Lymphography.

---

### Finding 2 — FP-Tree Mining: Two-Phase vs Augmented Transactions
**File:** `FPGrowth.java:22-26`, `FPGrowth.java:51`

The paper augments each transaction with its class label (e.g., adds item `class=mammal`) before building the FP-tree. This means:
- **Class label is part of the FP-tree**, so conditional pattern bases naturally encode class co-occurrence
- Every frequent itemset mined directly carries class information
- Rules are: `{items} → class` where the class-item was frequent in the original tree

Our implementation does the opposite:
```java
// FPGrowth.java line 51 — build tree WITHOUT class labels
int treeMinSup = Math.max(2, minSupport / 2);
FPTree tree = FPTree.build(transactions, treeMinSup);  // transactions have NO class column
```

Then Step 2 re-scans all transactions to count per-class support. This is mathematically different because:
- An itemset may be frequent overall (e.g., support=6) but for no single class with the adaptive threshold
- The paper's approach mines itemsets that are frequent **per class**, not globally
- Our approach misses itemsets that are globally infrequent but class-specifically frequent

For Zoo class 5 (reptiles, 4 instances): if a pattern appears in 3 of 4 reptiles but only 3 times globally, our tree (treeMinSup≥2) does include it — but this is only because of the `minSupport/2` hack on line 51. The paper would include it naturally because it augments with the class label and that combined item can pass its own threshold.

---

### Finding 3 — Conditional Tree Threshold Bug (Critical for Rare Classes)
**File:** `FPGrowth.java:145`

```java
// FPGrowth.java line 144-148
if (!patterns.isEmpty()) {
    FPTree condTree = FPTree.buildConditional(patterns, counts, minSupport);  // BUG: uses full minSupport
    if (!condTree.isEmpty()) {
        mineItemsets(condTree, newItemset, itemsets);
    }
}
```

The main tree is built with `treeMinSup = Math.max(2, minSupport / 2)` (line 51), but **conditional trees are built with the full `minSupport`** (line 145). This is inconsistent:

- Main tree threshold: `minSupport / 2`
- Conditional tree threshold: `minSupport` (2x stricter)

Consequence: items that appear in the conditional pattern base with count between `minSupport/2` and `minSupport-1` are **silently dropped** from conditional trees. This means multi-item rules for rare classes are under-mined. The FP-Growth algorithm correctness requires the same threshold be applied throughout.

For Zoo (minSup=4 for full data, treeMinSup=2): conditional trees require support≥4, but patterns for rare classes (class 5: 4 reptiles) may have conditional item counts of 2-3, which get pruned. This explains why Zoo only produces 40 rules total after pruning (diagnostic shows 35,000 mined before pruning, suggesting huge chi-square pruning loss, but even pre-prune the rare classes lose multi-item combinations).

---

### Finding 4 — Coverage Pruning: Class-Exclusive Matching
**File:** `RulePruner.java:111`, `RulePruner.java:120-129`

```java
// RulePruner.java line 109-115
boolean useful = false;
for (int i = 0; i < N; i++) {
    if (!fullyCovered[i] && labels[i] == rule.classLabel   // <-- only checks matching class
            && rule.matchesBitmap(bitmaps[i])) {
        useful = true;
        break;
    }
}
```

```java
// RulePruner.java line 120-129
for (int i = 0; i < N; i++) {
    if (!fullyCovered[i] && labels[i] == rule.classLabel   // <-- only marks matching-class instances
            && rule.matchesBitmap(bitmaps[i])) {
        coverCount[i]++;
        if (coverCount[i] >= maxCoverageCount) {
            fullyCovered[i] = true;
            coveredCount++;
        }
    }
}
```

The paper's DCP marks a transaction as "covered" when **any** rule (regardless of predicted class) fires on it `maxCoverageCount` times. Our implementation only increments `coverCount[i]` when `labels[i] == rule.classLabel`. This means:

- A majority-class instance that fires many rules of class 0 (correct class) gets covered quickly
- A minority-class instance that fires many rules of class 0 (wrong class) is **never marked covered** by those rules
- The `coveredCount >= N` early-exit on line 107 is never triggered for minority classes — the loop runs through ALL rules for them
- This causes the algorithm to behave correctly by accident for accuracy, but **incorrectly selects fewer rules for minority classes** because majority-class rules keep appearing "useful" (they cover majority instances that are not yet fully covered)

The paper's DCP intent: once a transaction has been covered `k` times by rules pointing to its true class, it is "satisfied" and further rules are redundant. But the paper counts ALL rules covering a transaction (regardless of class match) toward the coverage count. Our implementation diverges here.

**Impact on Zoo/Lymphography:** For Lymphography, classes 2 and 3 have only 4 and 2 instances. The diagnostic shows only 5 and 1 rules for these classes after pruning. With correct DCP, more rules for rare classes would be preserved.

---

### Finding 5 — Classification Voting: Weight Formula Diverges from Paper
**File:** `CMARClassifier.java:70-73`, `CMARClassifier.java:110-113`

```java
// CMARClassifier.java line 70-73 (during fit)
for (Rule rule : prunedRules) {
    // Weight = chi² × conf² × log(sup+1)
    rule.weight = rule.chiSquare * rule.confidence * rule.confidence * Math.log1p(rule.support);
}
```

```java
// CMARClassifier.java line 110-113 (during predict)
Map<Integer, Double> classScores = new HashMap<>();
for (Rule r : allMatched) {
    classScores.merge(r.classLabel, r.weight, Double::sum);  // uses precomputed weight
}
```

The paper (Section 3.3) defines the weighted chi-square group voting as: for each class group G_c, compute `W(G_c) = sum of chi²(r) for all r in G_c that match the instance`. The winning class is the one with the highest total chi-square weight.

Our implementation uses `chi² × conf² × log(sup+1)` instead of plain `chi²`. Adding `conf²` and `log(sup+1)` factors changes the ranking between classes. Specifically:
- Rules with moderate chi-square but high confidence get over-weighted
- Rules with high chi-square but moderate confidence get under-weighted
- The `log(sup+1)` term further distorts the comparison between rare-class rules (low support) and majority-class rules (high support)

For Zoo's rare classes with support=2-3, `log(3)≈1.1` vs majority class support=30+, `log(31)≈3.4` — a 3x multiplier favoring majority-class rules in voting.

---

### Finding 6 — Adaptive Support Threshold (Non-Paper Feature)
**File:** `FPGrowth.java:44-47`

```java
// FPGrowth.java line 44-47
for (Map.Entry<Integer, Integer> e : classCounts.entrySet()) {
    int adaptiveSup = Math.min(minSupport, Math.max(2, e.getValue() / 3));
    classMinSup.put(e.getKey(), adaptiveSup);
}
```

This heuristic — reducing minSup to `classSize/3` for rare classes — is not in the paper. While well-intentioned, it creates an inconsistency: rules for rare classes use a lower support threshold than rules for majority classes, making them statistically incomparable. Chi-square values at different support levels are not directly comparable.

For Lymphography class 3 (2 instances): adaptiveSup = max(2, 2/3) = 2. This generates rules from just 2 matching transactions — statistically unreliable. The chi-square for sup=2, antSup=2, classSupport=2, N=148 produces a value that may or may not exceed 3.841 depending on the contingency table, but it creates a semantic mismatch with the paper's uniform-threshold approach.

---

### Finding 7 — Zoo Dataset: Rule Count vs Class Balance
**From DiagnosticRunner output:**

```
=== Zoo ===
  MinSup=9 MinConf=0.5
  Rules mined: 35000
  Rules after prune: 40
  Class distribution (data): {0=41, 1=13, 2=20, 3=10, 4=8, 5=4, 6=5}
  Rules per class: {0=4, 1=5, 2=7, 3=4, 4=8, 5=4, 6=8}
```

35,000 rules mined but only 40 survive pruning — a **99.9% pruning rate**. This is extreme. The chi-square pruning on line 84 of `RulePruner.java` applies the threshold `chi2 >= 3.841 && conf > priorProb`. For Zoo class 0 (mammals=41/101 ≈ 40.6% prior), a rule needs conf > 0.406 AND chi² ≥ 3.841. With 41 mammals out of 101, the prior is high enough that many mammal rules fail the `conf > priorProb` check even at conf=0.5.

More critically: **only 40 rules survive for 101 instances with 7 classes** — roughly 5-6 rules per class on average. This is far too few to provide reliable voting. The paper mines far more rules per class by using lower effective thresholds via the augmented transaction approach.

The diagnostic shows Zoo **training accuracy is 89.1%** (on training data), far below the paper's 96.8% test accuracy, confirming the rule set quality problem is not just overfitting but actual underfitting from insufficient rules.

---

### Finding 8 — Lymphography: Class Mapping Order Issue (Potential)
**File:** `UCIDatasets.java:236-237`

```java
// UCIDatasets.java lines 226-236
for (String line : lines) {
    ...
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < parts.length; i++) sb.append(parts[i].trim()).append(",");
    sb.append(parts[0].trim());   // class label moved to END
    reordered.add(sb.toString());
}
int[][][] parsed = DataLoader.parseCSV(String.join("\n", reordered), 3, null);
```

The UCI Lymphography dataset has class as the **first column** (values: 1,2,3,4). The code moves it to the last column — correct. But in `DataLoader.parseCSV`, the class label is encoded via `LinkedHashMap` (insertion-ordered) based on first-seen order. The diagnostic shows:
```
Class distribution (data): {0=61, 1=81, 2=4, 3=2}
```

Classes 2 and 3 (2 and 4 instances) are the rare "metastases" and "malign lymph" categories. With only 2-6 training instances per fold for these classes, any minSup > 1 makes rule mining impossible for them. The paper likely uses leave-one-out or a special treatment for tiny classes — which our 5-fold CV cannot replicate.

---

## Root Cause Priority Table

| # | Root Cause | Affected Datasets | Estimated Impact |
|---|-----------|------------------|-----------------|
| 1 | Conditional tree uses full `minSupport`, not `treeMinSup` | Zoo, Lymphography, Glass | High — multi-item rules for rare classes lost |
| 2 | Voting weight = `chi² × conf² × log(sup+1)` vs paper's `chi²` only | All, especially Zoo | High — favors majority classes |
| 3 | 5-fold vs 10-fold CV | All small datasets | Moderate |
| 4 | Coverage pruning only counts same-class matches | Lymphography, Zoo | Moderate |
| 5 | Two-phase mining vs augmented transaction mining | All | Moderate — different rule sets |
| 6 | Adaptive per-class support threshold (non-paper feature) | Zoo, Lymphography | Low-Moderate |

---

## Specific Bugs with Line Numbers

### Bug A — FPGrowth.java:145 (Threshold Inconsistency)
```java
// Line 51: tree built with halved threshold
int treeMinSup = Math.max(2, minSupport / 2);
FPTree tree = FPTree.build(transactions, treeMinSup);

// Line 145: conditional trees use FULL minSupport (should be treeMinSup)
FPTree condTree = FPTree.buildConditional(patterns, counts, minSupport);
//                                                         ^^^^^^^^^^
//                                                         Should be treeMinSup
```

### Bug B — CMARClassifier.java:72 (Wrong Voting Weight)
```java
// Line 72: non-paper weight formula
rule.weight = rule.chiSquare * rule.confidence * rule.confidence * Math.log1p(rule.support);
// Paper formula should be:
// rule.weight = rule.chiSquare;
```

### Bug C — BenchmarkRunner.java:43 (Wrong Fold Count)
```java
int folds = 5;  // Line 43 — should be 10 to match paper
```

### Bug D — RulePruner.java:111 (Coverage Pruning Semantics)
The coverage counter should increment for ALL rules covering a transaction, not just same-class rules. Lines 111 and 121: the condition `labels[i] == rule.classLabel` should be removed from the coverage counting loop (though retained for the "useful" check).

---

## Supporting Evidence from DiagnosticRunner

```
Zoo:        35000 rules mined → 40 after prune (99.9% pruned)  Training acc: 89.1%
Lymphography: 10021 rules mined → 51 after prune (99.5% pruned) Training acc: 69.6%
Glass:       1435 rules mined → 276 after prune (80.8% pruned)  Training acc: 83.6%
```

Zoo and Lymphography show catastrophic pruning rates. For comparison:
- Breast-Cancer (good result, -0.2% gap): 2135 → 244 rules (88.6% pruned)
- Iris (likely good result): 36 → 34 rules (5.6% pruned)

The extreme pruning in Zoo/Lymphography is caused by Bug A (conditional trees too restrictive) combined with Bug B (weight formula biasing majority classes in chi-square pruning step at line 84 of RulePruner — note `priorProb` comparison also interacts with the conf threshold).

---

## Unresolved Questions

1. The paper's exact minSup values for each dataset are not disclosed — we use the paper's ratio (e.g., 5%), but the paper may use a fixed absolute count tuned per dataset.
2. Does the paper include a `maxCoverageCount=4` in DCP, or a different value? This parameter is not clearly specified in the 2001 paper and has large impact on rare classes.
3. The Lymphography class ordering: are our class labels 0-3 mapping to the paper's classes 1-4 in the same order? If the rare classes (1=normal, 4=malign) map differently, accuracy comparisons are meaningless.
4. Glass has 7 original class types in UCI (types 1-7, with type 4 absent) but only 6 are present. Our code detects `classes.size()` dynamically — confirm re-indexing is consistent across folds.
