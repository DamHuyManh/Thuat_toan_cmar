# Phase 03 — CAIM/CACC Discretization + Ablation

## Context links
- [plan.md](plan.md) · [phase-01](phase-01-honesty-cleanup.md) · [phase-02](phase-02-fuzzy-cmar.md)
- Research TIER-1 #2: `research/researcher-01-improvement-techniques.md` (CAIM/CACC)

## Overview
- **Date:** 2026-05-30
- **Description:** Add CAIM supervised discretizer as a drop-in alternative to MDL, and as a
  principled seed for fuzzy set centers. Provides a clean discretization ablation (MDL vs CAIM vs
  Fuzzy) for the paper. CACC only if CAIM underperforms MDL (YAGNI).
- **Priority:** MEDIUM (cheap ablation + may lift specific datasets + feeds Phase 2 breakpoints)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- CAIM is a simple greedy top-down algorithm; same plug point as MDL in `encodeFold` (numBins==0
  branch). Drop-in: everything downstream unchanged.
- Fuzzy strictly generalizes crisp bins, so CAIM's main value here is (a) a fair ablation arm and
  (b) better apex centers for triangular fuzzy sets than MDL on some medical sets.
- Gains are dataset-specific (MDL wins some, CAIM others) — report per-dataset, don't overclaim.

## CAIM criterion (the formula)
Quanta matrix: for a discretization scheme D with intervals (columns) and S classes (rows), let
`q_{ir}` = count of class i in interval r, `M_{+r}` = total in interval r, `max_r` = max over
classes in column r. CAIM:
```
CAIM(C,D) = ( Σ_{r=1..n} (max_r^2 / M_{+r}) ) / n
```
where n = number of intervals. Greedy: start with one big interval; among all candidate boundaries
(class-change midpoints), insert the boundary that MAXIMIZES CAIM; stop when CAIM stops increasing
or interval count exceeds S(#classes)-based cap (standard CAIM stopping: keep adding while CAIM
grows AND n < some limit; typical cap ≈ S to a small multiple).

## Requirements
1. `CAIMDiscretizer.java`: `static List<Double> findCutPoints(double[] vals, int[] labels)` — same
   signature shape as `MDLDiscretizer.findCutPoints` so it is a literal drop-in.
2. `--disc=mdl|caim` CLI flag selecting the discretizer in `encodeFold`.
3. Fuzzy can consume CAIM cut points as apex centers (`--fuzzyCenters=mdl|caim`).
4. Per-fold, train-only (no leak) — learn cuts from trainIdx values only, like MDL.

## Architecture
- `cmar.CAIMDiscretizer` (new), mirrors `MDLDiscretizer` API (DRY: same `discretize(values,cuts)`
  bin-assignment can be reused — move/share it, or duplicate the tiny loop).
- `DataLoader.encodeFold`: where it calls `MDLDiscretizer.findCutPoints` (line ~605), branch on a
  static `Discretizer` selector enum {MDL, CAIM}. Default MDL (preserves current results).
- Phase-2 `FuzzyDiscretizer` takes cut points as input → unchanged; just feed CAIM cuts when selected.

## Related code files
- NEW `src/cmar/CAIMDiscretizer.java`
- `src/cmar/benchmark/DataLoader.java` (encodeFold discretizer selection; ~line 593-606)
- `src/cmar/FuzzyDiscretizer.java` (accept externally-supplied cut points — already does)
- `src/cmar/boost/BoostedBenchmarkRunner.java` (`--disc=`, `--fuzzyCenters=` flags)
- `src/cmar/MDLDiscretizer.java` (reference for the shared `discretize` bin-assign helper)

## Implementation Steps
1. Implement quanta-matrix builder over sorted (value,label) pairs.
2. Implement greedy boundary insertion maximizing CAIM; candidate boundaries = midpoints where the
   sorted label changes. Stop when best insertion does not increase global CAIM (and respect a max
   interval cap, e.g. `min(distinctVals-1, 8)` to bound rules/memory).
3. Return sorted cut points; reuse MDL's `discretize(values, cutPoints)` to assign bins.
4. Add `DataLoader.DiscMode {MDL, CAIM}` static field; switch in `encodeFold`.
5. Wire `--disc` and `--fuzzyCenters` flags.
6. Ablation runs (deterministic, `-Xmx950m`):
   - A: crisp MDL (current baseline)
   - B: crisp CAIM
   - C: Fuzzy + MDL centers
   - D: Fuzzy + CAIM centers
   Record acc/F1/Recall per dataset; pick best fuzzy-center source for the headline config.
7. If CAIM (B) and Fuzzy+CAIM (D) both underperform MDL/Fuzzy+MDL on the weak set, keep CAIM only as
   a reported ablation arm; do NOT implement CACC (YAGNI). If CAIM clearly wins somewhere, note it.

## Todo list
- [ ] CAIMDiscretizer quanta matrix + CAIM criterion + greedy
- [ ] DiscMode selector in encodeFold + --disc flag
- [ ] --fuzzyCenters flag feeding FuzzyDiscretizer
- [ ] Run 4-arm ablation A/B/C/D
- [ ] Decide headline fuzzy-center source; document
- [ ] (Conditional) CACC only if CAIM materially helps and is needed

## Success Criteria
- CAIM produces valid cut points (1..cap intervals) on all continuous attrs without exception.
- Ablation table (A/B/C/D) reproducible under `--deterministic`.
- Headline fuzzy config uses whichever center source gives best weak-set macro-F1.
- No downstream code changed beyond the selector (proves clean drop-in).

## Risk Assessment
- **Med:** CAIM over-splits on noisy attrs → rule explosion/OOM. Mitigation: hard interval cap +
  minSup unchanged.
- **Low:** CAIM no better than MDL — acceptable; still a valuable ablation arm for the paper.
- **Low:** train-only leak. Mitigation: learn cuts from trainIdx only (same pattern as MDL path).

## Security Considerations
- Pure in-memory computation on training labels/values; no I/O, no untrusted input.

## Next steps
Phase 04 adds cost-sensitive rule generation, independent of discretizer choice.
