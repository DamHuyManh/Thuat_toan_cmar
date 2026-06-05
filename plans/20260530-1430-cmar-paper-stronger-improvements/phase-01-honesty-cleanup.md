# Phase 01 — Honesty Cleanup

## Context links
- [plan.md](plan.md)
- Scout: `scout/scout-01-codebase.md` (synthetic dead code, unverified ECBA-EX, seed non-determinism)
- Landscape: `research/researcher-02-paper-landscape.md` (ECBA-EX venue/numbers don't verify)

## Overview
- **Date:** 2026-05-30
- **Description:** Remove all fake-data pathways and unverified baseline numbers; lock reproducible
  seed and deterministic mining order. This is the user's #1 requirement — paper goes to an
  international competition, every number must trace to real code on real UCI CSVs.
- **Priority:** CRITICAL (must be first; blocks honest reporting in all later phases)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- `UCIDatasets.java` has 8 `create*Synthetic()` methods + a `discretize(double[][],...)` helper used
  ONLY by synthetic builders. Several `load*()` methods fall back to synthetic on load failure
  (Iris, Wine, Breast-Cancer, Zoo, Glass, Tic-Tac-Toe, Lymphography, Heart). All 28 CSVs exist in
  `datasets/`, so fallbacks are dead — but their mere presence undermines the "no fake data" claim.
- `ModernBaselines.java` ROWS contain an `ecba` column ("ECBA-EX, Alwidian et al. KAIS 2018").
  Research could NOT verify the venue or the numbers. Per user: FIX or REMOVE. Removing is safest
  and KISS — drop the ECBA-EX column entirely; keep C4.5/CBA/CMAR/CPAR/MCAR (those came from
  primary tables) + Ours.
- Determinism: seed=42 already set in `BoostedBenchmarkRunner` (line ~115) and `Rule.compareTo`
  already has full tie-breakers (line ~44). But scout notes per-dataset numbers wobble across runs
  due to parallel mining tie-break. For paper reproducibility, force single-thread mining for the
  final benchmark run (deterministic), and document it.

## Requirements
1. Delete every `create*Synthetic()` method and the synthetic-only `discretize(double[][]...)` helper
   from `UCIDatasets.java`.
2. Replace each synthetic fallback (`System.out.println("synthetic"); return create...Synthetic();`)
   with a hard failure: print `FAILED to load <name> — real CSV required` and `return null` (matches
   the already-failing-loud datasets like Pima/Australian).
3. Remove the `ecba` field/column from `ModernBaselines.java` and update `METHODS`, `toArray`,
   `Row` ctor, `acFamilyDatasets()`.
4. Add a deterministic-mode flag so the FINAL paper run is single-threaded & reproducible.
5. Update class-level Javadoc comments that claim "Falls back to ... synthetic data".

## Architecture
No new classes. Pure deletion + small signature change in `ModernBaselines.Row`.
Determinism: add `public static boolean DETERMINISTIC = false;` to `FPGrowthOptimized`; when true,
mine top-level items sequentially (skip the `ForkJoinPool`/parallel branch). Wire a
`--deterministic` CLI flag in `BoostedBenchmarkRunner` that sets it.

## Related code files
- `src/cmar/benchmark/UCIDatasets.java` (delete synthetic; harden fallbacks; lines 8-9 javadoc,
  922-1109 synthetic block, and the 8 fallback call sites ~549,585,621,655,694,716,753,786)
- `src/cmar/stats/ModernBaselines.java` (drop ECBA-EX: lines 26-35 ctor, 38-68 ROWS, 71 METHODS,
  74-76 toArray, 91-100 acFamilyDatasets)
- `src/cmar/FPGrowthOptimized.java` (add DETERMINISTIC flag; gate parallel branch ~line 90+)
- `src/cmar/boost/BoostedBenchmarkRunner.java` (add `--deterministic` flag parsing ~line 42-70)
- `src/cmar/stats/FriedmanNemenyi.java` (verify it reads METHODS generically — adjust if it
  hard-codes 7 methods)

## Implementation Steps
1. In `UCIDatasets.java`: delete lines 922-1109 (`// ===== SYNTHETIC FALLBACKS =====` through end
   of `createHeartSynthetic`, `discretize`, `contains`). Keep nothing synthetic.
2. For each of the 8 `load*` methods ending in `System.out.println("synthetic"); return create*();`,
   replace with: `System.out.println("FAILED to load — real CSV required"); return null;`.
3. Remove URL `fetchURL` fallbacks too? KEEP `fetchURL` (it fetches REAL UCI data, not fake) but it
   is unreachable when local CSV exists. Leave as-is (DRY/no harm) but note in javadoc that local
   CSV is authoritative.
4. Fix class javadoc lines 6-8: remove "Falls back to high-fidelity synthetic data...".
5. `ModernBaselines.java`: delete `ecba` from `Row` fields, ctor param, all 26 ROWS entries, the
   `"ECBA-EX"` entry in `METHODS`, the `r.ecba` in `toArray`, and the `Double.isNaN(r.ecba)` check
   in `acFamilyDatasets`. Update class javadoc bullet list (remove ECBA-EX source line).
6. `FPGrowthOptimized.java`: add `public static volatile boolean DETERMINISTIC = false;`. Find the
   parallel mining branch (uses `ForkJoinPool`/`CompletableFuture`, `PARALLEL_MIN_TX`). Wrap:
   `if (!DETERMINISTIC && N >= PARALLEL_MIN_TX) { parallel } else { sequential }`.
7. `BoostedBenchmarkRunner.java`: parse `if (a.equalsIgnoreCase("--deterministic")) FPGrowthOptimized.DETERMINISTIC = true;`.
8. Compile: `javac -d out (all sources)`. Fix any references to deleted synthetic methods/ecba.
9. Run a quick smoke benchmark on 3 datasets to confirm no NPE and identical output across 2 runs
   with `--deterministic`.

## Todo list
- [ ] Delete synthetic methods + helpers from UCIDatasets.java
- [ ] Harden 8 fallback sites to return null with clear message
- [ ] Strip class javadoc synthetic claims
- [ ] Remove ECBA-EX column from ModernBaselines.java + dependents
- [ ] Verify FriedmanNemenyi compiles with new METHODS length
- [ ] Add DETERMINISTIC flag to FPGrowthOptimized + --deterministic CLI
- [ ] javac full compile, fix errors
- [ ] Smoke run x2 with --deterministic → byte-identical results

## Success Criteria
- `grep -ri synthetic src/` returns nothing in `UCIDatasets.java` (only acceptable hits: none).
- No `ecba`/`ECBA-EX` token anywhere in `src/`.
- Project compiles clean with `javac 21`.
- Two consecutive runs with `--deterministic` produce identical per-dataset acc/F1/recall.
- All 26 datasets still load as "real data (N rows)" (none print FAILED).

## Risk Assessment
- **Med:** deleting synthetic may break a load path that silently relied on it (e.g. a CSV that
  parses to too few rows → `enforcePaperSize` skip → null). Mitigation: run full load, confirm 26
  datasets present; if any null, fix the CSV/parse, NOT re-add synthetic.
- **Low:** removing ECBA-EX changes Friedman ranking k from 7→6 methods. Acceptable & more honest.
- **Low:** forcing sequential mining slows the final run. Acceptable (only final reproducible run).

## Security Considerations
- `fetchURL` makes outbound HTTP to archive.ics.uci.edu — for the offline reproducible run, prefer
  local CSV only; no secrets, no injection surface. No new external input introduced.

## Next steps
Proceed to Phase 02 (Fuzzy CMAR) on a clean, honest, reproducible baseline.
