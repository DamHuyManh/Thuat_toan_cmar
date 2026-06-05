# Phase 02 — Fuzzy CMAR (headline improvement)

## Context links
- [plan.md](plan.md) · [phase-01](phase-01-honesty-cleanup.md)
- Research TIER-1 #1: `research/researcher-01-improvement-techniques.md` (Fuzzy AC, FACA)
- Scout weak spots: continuous-medical (Diabetes/Heart/Pima/Cleve) lose at MDL bin boundaries.

## Overview
- **Date:** 2026-05-30
- **Description:** Replace crisp MDL bin membership on continuous attributes with triangular fuzzy
  membership. An instance partially belongs to ≤2 adjacent fuzzy sets, so more minority-covering
  rules fire near class boundaries → higher minority recall & macro-F1. This is the paper's headline
  contribution and directly targets the weak datasets.
- **Priority:** HIGH (headline)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- A glucose of 126 is "high", 125 "normal" under crisp MDL — info is lost at the cut. Fuzzy sets let
  125 be 0.6 normal / 0.4 high, so rules from BOTH bins can match → recovers borderline minority
  patients.
- CMAR transactions are crisp `int[]` (one item per attr). Cleanest KISS integration that keeps the
  whole downstream (FP-Growth, chi², voting) almost unchanged: **keep one crisp item per attr per
  transaction = the fuzzy set with HIGHEST membership (top-1)**, AND carry a per-(transaction,attr)
  membership **weight** in `[0,1]`. Mining then uses **fuzzy support = Σ membership** instead of a
  raw count. This is the (a)-lite option from plan unresolved-Q1.
- Fuzzy support (Σ weights) is fractional. Confidence = Σ(weights of tx matching rule AND in class) /
  Σ(weights of tx matching antecedent). minSupport stays an absolute count but compared against the
  fuzzy (fractional) support, rounded down for the FP-tree frequency threshold.
- Only CONTINUOUS attrs (`isNumeric && !treatAsCat`) get fuzzified. Categorical/ordinal items keep
  weight 1.0. So non-medical datasets are barely affected — gain concentrates where expected.

## Triangular fuzzy membership (the formula)
Given cut points `c_0 < c_1 < ... < c_{k-1}` (k cuts → k+1 bins) learned per training fold (reuse
MDL cuts from `encodeFold`, or CAIM centers from Phase 3). Define bin centers and triangular sets.
For a value `x` in bin `j` with neighbouring centers `m_{j-1}, m_j, m_{j+1}`:

Triangular MF for set j: 
```
μ_j(x) = max(0, min( (x - m_{j-1})/(m_j - m_{j-1}),  (m_{j+1} - x)/(m_{j+1} - m_j) ))
```
Edge sets use shoulders (left set saturates to 1 below m_0; right set saturates to 1 above m_{last}).
Centers `m_j` = midpoint of bin j's value range OR the cut points themselves used as set boundaries
(simpler: place triangle apexes AT cut points and bin midpoints). For each x compute the two largest
μ over sets; normalize so they sum to 1 (partition of unity). top-1 set → the crisp item; its μ → the
weight. (Optionally emit top-2 as two weighted items — gated behind a flag, evaluate both.)

**Fuzzy support of itemset I for class c:** `FS(I→c) = Σ_{tx in c, tx⊇I} Π_{i∈I} w(tx,i)`
(product t-norm over the items in the antecedent). For single-item index we just store w(tx,item).

## Requirements
1. New `FuzzyDiscretizer.java`: given training values + cut points → per-value (binIndex, weight)
   and (optionally) a 2nd (binIndex2, weight2).
2. Extend the per-fold encoding to produce, alongside `int[][] trainTx`, a parallel
   `double[][] trainWeights` (same shape) with membership weight per (tx, attr); 1.0 for categorical.
3. FP-Growth / support counting must accept weights → fuzzy support & fuzzy confidence.
4. A `--fuzzy` CLI flag to toggle Fuzzy CMAR vs crisp MDL (for ablation in Phase 5).
5. Must run under `-Xmx950m -XX:+UseSerialGC` without OOM at T=10.

## Architecture
- `cmar.FuzzyDiscretizer` (new): static `Membership[] memberships(double[] trainVals, List<Double>
  cutPoints)` returning per-bin apex centers; and `double[] membership(double x)` → weights per bin.
  KISS: top-1 item + weight is the default path.
- `DataLoader.FoldData`: add `double[][] trainWeights, testWeights` (null when not fuzzy).
  `encodeFold(raw, train, test, boolean fuzzy)` overload computes weights for continuous attrs.
- `FPGrowthOptimized.mineRules(int[][] tx, int[] labels)` → add overload
  `mineRules(int[][] tx, int[] labels, double[][] weights)`. When weights==null, behaves exactly as
  today (DRY — current path untouched). When present, the inverted index stores fuzzy contribution;
  support/conf use Σ weight. Implementation detail: keep `BitSet itemIndex` for membership tests but
  add `Map<Integer,double[]> itemWeight` or store weight in a parallel `double[]` per transaction;
  compute support by summing weights over set bits. minSupport threshold compares to ceil/floor of
  fuzzy support.
- `Rule.support` is `int`; add `double fuzzySupport` (and use it in conf/chi² when fuzzy on). Keep
  crisp `support` for categorical-only datasets and for chi² where integer contingency is needed —
  for fuzzy, round fuzzy counts to nearest int for the chi² 2x2 table (documented approximation).

## Related code files
- NEW `src/cmar/FuzzyDiscretizer.java`
- `src/cmar/benchmark/DataLoader.java` (FoldData fields; encodeFold overload; encodeRows weights;
  reuse `MDLDiscretizer.findCutPoints` already called at line ~605)
- `src/cmar/FPGrowthOptimized.java` (weighted mineRules overload; itemIndex weight; support sums)
- `src/cmar/Rule.java` (add `double fuzzySupport`; conf already a field)
- `src/cmar/RulePruner.java` (chi² uses ints — feed rounded fuzzy counts when fuzzy)
- `src/cmar/CMARClassifier.java` (voting uses confidence/chiSquare — unchanged if Rule carries them)
- `src/cmar/boost/BoostedBenchmarkRunner.java` (`--fuzzy` flag; pass to encodeFold + mining)

## Implementation Steps
1. Write `FuzzyDiscretizer`: compute apex centers from cut points (apex_0 = min train val,
   apex_last = max; interior apexes = cut points OR bin midpoints). Implement triangular μ with
   shoulders + partition-of-unity normalization of the top-2.
2. In `DataLoader`: add weight arrays to `FoldData`; new `encodeFold(raw, trainIdx, testIdx, fuzzy)`.
   For continuous attrs when `fuzzy`, after computing `bin`, also compute `weight = μ_topbin(x)`
   (and store the crisp `bin` = argmax membership, which may differ from the hard cut bin — use
   argmax for consistency). Categorical → weight 1.0.
3. `FPGrowthOptimized`: add weighted overload. Simplest correct approach: build `double[] txWeight`
   per item occurrence; during support counting, sum weights over the BitSet intersection instead of
   `cardinality()`. Confidence = fuzzyRuleSupport / fuzzyAntSupport. Tree frequency uses
   `(int)Math.floor(fuzzySupport)` vs minSupport.
4. `Rule`: add `fuzzySupport`; set `confidence` from fuzzy counts when present.
5. `RulePruner.computeChiSquare`: when fuzzy, pass `Math.round` of fuzzy supports into the existing
   integer formula (documented as a standard fuzzy-chi² approximation).
6. Wire `--fuzzy` through `BoostedBenchmarkRunner` → encodeFold + classifier mining.
7. Evaluate top-1 vs top-2 fuzzy items (flag `--fuzzyTop2`): pick whichever gives higher macro-F1 on
   the 4 weak datasets without OOM. Default to the winner.
8. Run `java -Xmx950m -XX:+UseSerialGC ... --fuzzy --deterministic` on Diabetes/Heart/Pima/Cleve;
   compare F1/Recall vs crisp baseline. Iterate breakpoints if no gain.

## Todo list
- [ ] FuzzyDiscretizer triangular MF + partition-of-unity
- [ ] FoldData weight arrays + encodeFold(fuzzy) overload
- [ ] Weighted mineRules overload (fuzzy support/conf)
- [ ] Rule.fuzzySupport + confidence wiring
- [ ] Fuzzy chi² (rounded counts)
- [ ] --fuzzy / --fuzzyTop2 CLI flags
- [ ] Evaluate top-1 vs top-2; lock default
- [ ] Weak-dataset F1/Recall gain confirmed under -Xmx950m

## Success Criteria
- Macro-F1 AND macro-Recall on Diabetes, Heart, Pima improve vs crisp MDL baseline (each ≥0, at
  least 2 of 3 strictly positive; aim ≥ +1.5 F1 pts on the weak set average).
- Overall 26-set macro-F1 ≥ current 82.84% (no regression), ideally higher.
- No accuracy collapse on non-medical sets (categorical sets ≈ unchanged since weight=1.0).
- Runs to completion under `-Xmx950m -XX:+UseSerialGC` at T=10.

## Risk Assessment
- **High:** OOM if weights materialize a large parallel structure. Mitigation: store weight as
  `double[][]` same size as tx (already O(n·attrs)); avoid per-itemset weight maps; reuse BitSets.
- **Med:** fuzzy chi² rounding could distort pruning. Mitigation: only round at the 2x2 table; keep
  fuzzy conf exact. Validate chi² pruning keeps minority rules (inspect rule counts).
- **Med:** breakpoint choice gives no gain. Mitigation: Phase 3 supplies CAIM centers as alt apexes.
- **Low:** non-determinism via weights. Use `--deterministic` (sequential) for final numbers.

## Security Considerations
- No external input; weights derived from training values only (no leak: cut points learned from
  train fold in `encodeFold`). No new I/O or untrusted parsing.

## Next steps
Phase 03 supplies CAIM as an alternative discretizer / fuzzy-center seeder and an ablation arm.
