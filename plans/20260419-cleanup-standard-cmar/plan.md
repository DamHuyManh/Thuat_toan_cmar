# Plan: Clean Up CMAR to Standard Li/Han/Pei 2001 Algorithm

**Date:** 2026-04-19
**Scope:** Remove non-standard additions from benchmark layer only. Core algorithm files are untouched.

## Goal

Revert `UCIDatasets.java` and `BenchmarkRunner.java` to standard CMAR as described in the paper:
- No per-fold MDL discretization
- No per-dataset parameter overrides
- Hardcoded defaults: chi²=3.841, coverage=4, antLen=4 for all datasets

## Files to Change

| File | Changes |
|------|---------|
| `UCIDatasets.java` | Remove `rawData` field, `withOptimal()` methods, `replacePimaZeros()`, all their call sites |
| `BenchmarkRunner.java` | Remove `optimalChi/Coverage/AntLen` reads in `ParamConfig.base()` |

## Files Unchanged

`CMARClassifier.java`, `FPGrowth.java`, `FPTree.java`, `FPNode.java`, `Rule.java`, `CRTree.java`,
`RulePruner.java`, `MDLDiscretizer.java`, `DataLoader.java`, `Main.java`

## What Each Non-Standard Addition Did

1. **`ds.rawData`** — stored raw (pre-discretized) CSV so `BenchmarkRunner.evaluateConfig()` could re-run MDL per fold on train-only data. Not in paper; paper uses global pre-discretized data.
2. **`ds.withOptimal(...)`** — set `optimalAntLen/Coverage/Chi` on a per-dataset basis after manual tuning. Not in paper; paper uses fixed params for all datasets.
3. **`replacePimaZeros()`** — converted zero values in Pima cols 1–5 to "MISS" before discretization, treating domain-invalid zeros as missing values. Not mentioned in paper.
4. **`ParamConfig.base()` lines 448–450** — read `ds.optimalChi/Coverage/AntLen` and fell back to defaults only if 0. After cleanup, always use hardcoded defaults.

## Phases

- **Phase 01** — Remove non-standard code (see `phase-01-remove-nonstd.md`)

## Verification After Cleanup

Run `BenchmarkRunner.main()`. Confirm:
- No `rawData`/`withOptimal`/`replacePimaZeros` references remain in either file
- `ParamConfig.base()` uses literal `3.841`, `4`, `4` with no dataset field reads
- All datasets that previously used per-fold MDL now use the pre-discretized global path (the `else` branch in `evaluateConfig`)
