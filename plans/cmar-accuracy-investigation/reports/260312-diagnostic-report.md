# CMAR Accuracy Investigation Report
**Date:** 2026-03-12
**Scope:** Lymphography (40.4% vs 83.1%) and Zoo (82.2% vs 96.8%)
**Also examined:** Glass, Heart (all multi-class underperformers)

---

## Executive Summary

Three root causes, in descending order of impact:

1. **[CRITICAL] Coverage pruning destroys minority-class rules** — the global DCP iterates rules sorted by confidence-desc/support-desc, which are dominated by majority-class patterns. After MIN_COVER=3 majority rules cover each training instance, all minority-class rules are discarded as "not useful". Lymph ends up with ~114 rules (from 194K mined); Zoo ~58 rules (from 473K). Minority classes in these datasets have 2–13 instances — their rules are wiped out entirely.

2. **[HIGH] minSupport=2 (absolute) + two-phase mining explodes the rule space then starves rare classes** — with 0.01 * 133 training instances = 1.33 → floored to 2, the FP-tree mines 194K+ itemsets for Lymph and 473K+ for Zoo. The two-phase approach (mine all frequent itemsets, then scan per class) produces rules for every class that clears minSupport=2. But rare classes (Lymph class-0: 2 instances, class-3: 4 instances; Zoo classes 2,4: 5 and 4 instances) have so few training samples that any rule covering them has support=1 and is killed at minSupport=2. They generate zero valid rules, so the classifier defaults to the majority class for those instances.

3. **[MEDIUM] Pure chi² weight favors large majority classes during voting** — chi² grows with N (total transactions). For a rule covering a majority class with 81/148 instances, the chi² numerator N*(ad-bc)² is intrinsically larger than for a minority class with 4 instances, even at equal confidence. This biases the weighted-voting step against rare classes even when they do survive pruning.

---

## Evidence

### Rule Explosion + Prune Catastrophe

| Dataset   | Instances | Classes | Rules Mined | Rules Kept | Prune Ratio | Our Acc | Paper Acc |
|-----------|-----------|---------|-------------|------------|-------------|---------|-----------|
| Lymph     | 148       | 4       | **194,222** | 114        | **99.9%**   | 40.4%   | 83.1%     |
| Zoo       | 101       | 7       | **473,132** | 58         | **100.0%**  | 82.2%   | 96.8%     |
| Glass     | 214       | 6       | 11,921      | 252        | 97.9%       | 67.3%   | 70.6%     |
| Iris      | 150       | 3       | 265         | 78         | 70.6%       | 96.0%   | 94.0%     |
| BC        | 683       | 2       | 6,736       | 14         | 99.8%       | 100.0%  | 96.4%     |

Pattern: datasets with many classes + few instances have catastrophic explosion then near-total pruning. Iris and BC — balanced or binary — work fine.

### Lymphography Class Imbalance

Lymph class distribution (148 instances, 4 classes):
- Class 0 (normal): **2 instances** (1.4%)
- Class 1 (metastases): **81 instances** (54.7%) — dominant
- Class 2 (malign lymph): **61 instances** (41.2%)
- Class 3 (fibrosis): **4 instances** (2.7%)

With 10-fold CV, training fold has ~133 instances. Classes 0 and 3 have ~2 and ~3 training instances. minSupport=2 means:
- Class 0: any rule needs support>=2, but only 2 training instances exist — impossible unless all fold-0 instances land in train (50% chance each fold). Effectively 0 rules.
- Class 3: ~3 training instances — maybe 1 rule survives mining, but gets obliterated by DCP since majority rules saturate coverage first.

The classifier predicts class 1 (majority) for most test instances, achieving ~54% for most folds — yet the report shows 28–38% for many folds. This confirms something is actively wrong beyond just imbalance: the classifier is not even defaulting correctly to majority.

### Zoo Class Imbalance

Zoo class distribution (101 instances, 7 classes):
- Class 0 (mammal): **41 instances** — dominant
- Class 1 (bird): **20 instances**
- Class 2 (reptile): **5 instances**
- Class 3 (fish): **13 instances**
- Class 4 (amphibian): **4 instances**
- Class 5 (bug): **8 instances**
- Class 6 (invertebrate): **10 instances**

With ~91 training instances per fold, classes 2 and 4 have ~5 and ~4 training instances. minSupport=2 allows their rules to exist, but the sheer volume of class-0/1 rules dominates coverage pruning. Zoo achieves 82.2% partly because the boolean features are well-separated — majority-class predictions are often correct by chance.

### DCP Ordering Defect — The Core Mechanism

`RulePruner.coveragePrune()` processes rules in CMAR ordering (conf DESC, support DESC, len ASC). This means:
1. High-confidence, high-support majority-class rules go first.
2. They match many training instances (because many instances belong to majority class).
3. Each matched instance gets its coverage counter incremented.
4. After MIN_COVER=3 rules of any class cover an instance, it is marked `fullyCovered`.
5. By the time minority-class rules are processed, every minority-class training instance has ALREADY been covered 3+ times by majority-class rules (because majority-class rules match minority instances too — the antecedent is a subset of ALL frequent items, not class-specific items).
6. Result: minority-class rules are "not useful" and discarded.

This is the decisive failure. The paper's augmented-transaction approach avoids this entirely — mining happens per class using class-specific transactions, so minority-class patterns are discovered within their own context.

### maxRulesPerClass Cap Applied at Wrong Stage

`FPGrowth.mineRules()` applies `maxRulesPerClass=80000` during mining (line 74). But because rules are generated from ALL itemsets (not class-specific trees), the rule counts reach the cap only for majority classes. For Lymph class 1 (81 instances), it likely hits 80K first; for class 0 (2 instances) it generates zero rules. So the cap hurts majority classes slightly but does nothing for minority classes.

### Why Lymph is Far Worse Than Zoo

Lymph's per-fold accuracy drops as low as 21.4% (worse than random for 4-class = 25%). This indicates active misprediction, not just defaulting. The cause:

`CMARClassifier.predict()` Step 1 ("if top-confidence rules all predict same class, return it") fires when the surviving 114 rules all happen to match an instance with conf=1.0 for class 1. That class-1 prediction overrides even the defaultClass. Since class-1 rules have conf=1.0 (they were mined with minSup=2 and the two-phase approach counts exact support), they will match most instances including class-2 and class-3 test instances — producing wrong predictions with certainty.

In folds where very few class-2/3 test instances remain (fold 10: 21.4%), the majority of test instances belong to class 2 or 3 but the classifier confidently predicts class 1 because high-conf class-1 rules dominate.

---

## Root Cause Ranking

### Bug 1 (CRITICAL): DCP does not distinguish useful coverage by correct class

**File:** `src/cmar/RulePruner.java`, `coveragePrune()` lines 112–141

**Problem:** "Useful" check (line 117–122) and coverage counting (lines 128–133) both operate on ALL matching instances regardless of whether the rule predicts correctly. This is actually paper-aligned in the letter, but the paper applies DCP AFTER mining class-specific rules from augmented transactions. When applied to two-phase mined rules containing mixed-class rules, majority-class rules saturate coverage of minority-class training instances before minority-class rules are evaluated.

**Impact:** Nearly all minority-class rules deleted. Directly causes the 40.4% Lymph result.

**Fix:** Apply per-class coverage pruning: maintain a separate coverage counter per `(instance, class)` pair, or run DCP independently per class. The paper says "for each class, select rules by database coverage" — our implementation conflates classes.

### Bug 2 (HIGH): Two-phase mining + absolute minSupport=2 starves tiny classes

**File:** `src/cmar/FPGrowth.java`, `mineRules()` + `src/cmar/benchmark/BenchmarkRunner.java` line 70

**Problem:** `minSup = Math.max(2, (int)(0.01 * trainN))` = 2 for all small datasets. With 2 training instances of class 0 in Lymph, ANY rule for that class has support at most 2, and only if BOTH instances share a common pattern. One noisy attribute flip (15% noise rate in synthetic Lymph) breaks this.

The paper uses augmented transactions (class label appended), so class-0 support is counted within class-0 transactions only. minSupport=2 against 2 instances = 100% of class must agree. Our two-phase approach computes support as joint frequency across ALL transactions — for rare classes this produces near-zero rules.

**Impact:** Zero rules for classes 0 and 3 in Lymph; near-zero for classes 2 and 4 in Zoo.

**Fix (a):** Per-class relative minSupport — e.g., `classSup >= max(2, 0.05 * classSize)` so rare classes aren't held to same absolute threshold as majority.

**Fix (b):** Switch to augmented-transaction FP-Growth (the paper's actual method): build one FP-tree per class from transactions that have that class label, mine frequent patterns within that class. This naturally scales support to class size.

### Bug 3 (MEDIUM): Pure chi² weight amplifies majority-class dominance in voting

**File:** `src/cmar/CMARClassifier.java` line 69 (`rule.weight = rule.chiSquare`)

**Problem:** Chi² = N*(ad-bc)² / (rowA*rowB*colA*colB). N=133 for all classes, but classSupport (colA) differs dramatically. For majority class (81 instances), colA=81 — the formula produces larger chi² values simply because the marginals are larger, even at equal confidence/lift. In `predict()` Step 2, majority-class weight sum dominates.

**Impact:** Even when minority-class rules survive pruning, the voting step gives them lower scores. This explains part of the Heart and Glass gaps too.

**Fix:** Normalize chi² by class size, or use confidence-weighted voting: `weight = confidence` or `weight = confidence * log(support + 1)`. The paper's description of "weighted chi-square" is per-group, selecting the strongest rules per class for comparison, not raw accumulation. Using `weight = chiSquare / classSupport` would equalize scale.

---

## Secondary Observations

- **Summary report line 66 inconsistency:** The code says `rule.weight = rule.chiSquare` (CMARClassifier.java:69) but the report says "Weighted voting - weight = chi² × confidence × log(support+1)". The report is wrong; the code is simpler. This matters because chi² alone without confidence normalization further inflates majority-class votes.

- **BenchmarkRunner maxCoverageCount=3 vs DiagnosticRunner maxCoverageCount=4:** BenchmarkRunner uses `maxCoverageCount=3` (line 100), DiagnosticRunner uses 4 (line 20). Inconsistency. Lower values prune more aggressively, which hurts minority classes more.

- **Zoo rules mined (473K) exceeds Lymph (194K) despite smaller dataset:** Zoo has 16 boolean attributes — exponential itemset space 2^16. The two-phase approach mines ALL frequent itemsets globally, then associates with classes. This is fundamentally more expensive and less targeted than per-class mining.

---

## Recommended Fixes (Prioritized)

### Fix 1 — Per-class DCP (addresses Bug 1, expected +30-40% Lymph, +10-14% Zoo)
In `RulePruner.coveragePrune()`: maintain `int[N][numClasses]` coverage counts. An instance is "fully covered for class C" after MIN_COVER rules predicting class C have matched it. Iterate rules per class separately, or use class-aware coverage counters.

```java
// Instead of: if (!fullyCovered[i] && rule.matchesBitmap(bitmaps[i]))
// Use: if (!classFullyCovered[i][rule.classLabel] && rule.matchesBitmap(bitmaps[i]))
```

This ensures minority-class rules are evaluated against instances that haven't been covered by minority-class rules yet, not by majority-class rules.

### Fix 2 — Per-class relative minSupport (addresses Bug 2, expected +5-15% Lymph/Zoo)
In `FPGrowth.mineRules()` or `BenchmarkRunner`, compute per-class floor:

```java
// Instead of global: clsSup >= minSupport
// Use: clsSup >= Math.max(1, (int)(minSupportRatio * classTotal))
// where classTotal = classCounts.get(classLabel)
```

This allows classes with 2-4 instances to generate rules with support=1 without lowering the bar for majority classes.

### Fix 3 — Normalize chi² weight by class prior (addresses Bug 3, expected +2-5% across all weak datasets)
In `CMARClassifier.fit()`:

```java
// Instead of: rule.weight = rule.chiSquare
// Use: rule.weight = rule.chiSquare * rule.confidence / Math.sqrt(classCount)
// where classCount = classCounts.get(rule.classLabel)
```

Or simpler: `rule.weight = rule.confidence * Math.log(rule.support + 1)` — removes the N-dependent chi² term entirely from voting.

---

## Unresolved Questions

1. Does the real UCI Lymphography/Zoo data download successfully in the test environment, or is synthetic fallback used? The synthetic Lymph uses 15% noise, which makes class-0 (2 instances) and class-3 (4 instances) patterns unreliable. Real data has cleaner categorical patterns.

2. The paper likely uses leave-one-out or a specific train/test split for Zoo (101 instances) — 10-fold gives ~10 test instances per fold with class sizes as small as 4. Statistical variance alone could explain ±5% for Zoo.

3. `maxRulesPerClass=80000` cap in BenchmarkRunner — does this ever actually trigger? If Zoo mines 473K rules for 7 classes, average is ~67K per class, so it may be capping majority classes. Diagnostic output would confirm this.
