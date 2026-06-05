# Codebase Review — CMAR Project

**Date**: 2026-05-25
**Scope**: Full Java source (`src/cmar/**`)
**Total LOC**: ~7,000 across 30+ files
**Status**: WORKING but has technical debt từ 22 vòng exploration

## Severity overview

| Severity | Count | Items |
|---|---|---|
| 🔴 High | 4 | Global state pollution, code dup ensemble classes, dead code mass, 2 runners overlap |
| 🟡 Medium | 5 | CLI flag explosion, magic numbers, no unit tests, no bounds checks, OOM risks |
| 🟢 Low | 4 | Naming inconsistency, doc gaps in code, error handling, file size bloat |

## Phases

| # | Phase | File | Status | Effort |
|---|---|---|---|---|
| 1 | Dead code removal — failed experiments | [phase-01-dead-code.md](phase-01-dead-code.md) | 🔴 PENDING | 2h |
| 2 | Static state → instance config | [phase-02-state-pollution.md](phase-02-state-pollution.md) | 🔴 PENDING | 4h |
| 3 | DRY ensemble classes | [phase-03-ensemble-dry.md](phase-03-ensemble-dry.md) | 🔴 PENDING | 3h |
| 4 | Unit tests for ensemble classes | [phase-04-tests.md](phase-04-tests.md) | 🟡 PENDING | 4h |
| 5 | Consolidate 2 runners | [phase-05-runner-merge.md](phase-05-runner-merge.md) | 🟡 PENDING | 3h |
| 6 | Documentation in code | [phase-06-docs.md](phase-06-docs.md) | 🟢 PENDING | 2h |

**Total effort**: ~18h (~2-3 ngày)

## Quick wins (can do trước)

1. Remove dead code (failed approaches): -1500 LOC
2. Remove synthetic dataset code: -300 LOC
3. Remove unused CLI flags: -50 LOC

## Tradeoffs

- **DON'T remove ChiMerge / Bayesian / Boosted** — kept for reproducibility of ablation negative results (paper value)
- **DON'T remove Lift/HM static fields in Rule.java** — used trong ablation comparison
- **DO remove**: ChiMerge if not used + dual filter + relaxed unanimity flags không có gain

## Risk

- Refactor có thể break reproducibility (results numbers shift)
- → BEFORE refactor: snapshot all current results in `results/snapshot-pre-refactor/`
- → AFTER refactor: re-run FINAL config, verify identical numbers
