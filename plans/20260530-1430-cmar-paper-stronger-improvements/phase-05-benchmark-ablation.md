# Phase 05 — Full Benchmark + Ablation + Significance

## Context links
- [plan.md](plan.md) · phases 01-04
- Landscape eval protocol: `research/researcher-02-paper-landscape.md` (10-fold CV, Friedman+Nemenyi+CD)

## Overview
- **Date:** 2026-05-30
- **Description:** Run the complete, reproducible benchmark over all 26 UCI sets (10-fold CV,
  seed=42, `--deterministic`), isolate each improvement, produce per-dataset Acc/F1/Recall with
  deltas, and run Friedman + Nemenyi with a CD diagram on the F1 ranking. This produces every number
  the paper reports.
- **Priority:** HIGH (evidence backbone; nothing in the paper without it)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- Reproducibility is the competition differentiator: fixed seed, single-thread mining, per-fold
  leak-free discretization (already in `encodeFold`), `-Xmx950m -XX:+UseSerialGC`.
- Ablation must isolate EACH lever so reviewers see contribution: baseline → +fuzzy → +cost-rulegen →
  +CAIM-centers, plus the existing components (stratified, cost-vote, bagging, adaptMinSup).
- Headline metric = macro-F1 + macro-Recall (+ G-mean / balanced acc for imbalanced subset).
  Accuracy reported but framed honestly (sub-1%–2%).
- Friedman test currently ranks methods from `ModernBaselines` (literature acc). For F1/Recall we
  rank OUR ablation arms across datasets (we have those numbers from our own runs).

## Requirements
1. One scripted entry that runs all ablation arms and writes Markdown + CSV tables to `results/`.
2. Per-dataset table: Acc, macro-F1, macro-Recall, (minority-Recall for binary), rule count, runtime.
3. Imbalance ratio per dataset (max class freq / min class freq) to define the imbalanced subset.
4. Friedman + Nemenyi on the F1 ranking across ablation arms; CD diagram values (ranks + CD).
5. Everything under `--deterministic`; two runs must match.

## Ablation arms (each isolates one lever)
| Arm | Config | Purpose |
|-----|--------|---------|
| A0 paper-CMAR | crisp MDL, no extras | reference |
| A1 current | +stratified+cost-vote+bagging+adaptMinSup+minSupScale (current 82.84 F1) | prior best |
| A2 +fuzzy | A1 + `--fuzzy` (best center source) | Phase 2 effect |
| A3 +cost-rulegen | A1 + `--costRuleGen` | Phase 4 effect |
| A4 +fuzzy+cost-rulegen | A1 + both | combined headline |
| A5 CAIM | A1 with `--disc=caim` | discretizer ablation |
| (opt) fuzzyTop2 | A4 with `--fuzzyTop2` | membership-degree ablation |

## Architecture
- Reuse `BoostedBenchmarkRunner` (it already does 10-fold CV, per-fold encode, writes Markdown).
  Add a small driver `AblationDriver` (or a shell/PowerShell script) that invokes the runner once
  per arm with the right flags and `--out=results/ablation-<arm>.md`.
- Extend the runner's metric output to include macro-Recall (uses `cmar.Metrics`), minority-Recall,
  rule count, runtime if not already emitted.
- `FriedmanNemenyi` already exists; add a path to feed it our per-dataset F1 matrix across arms
  (arms as "methods"). Output ranks + CD.

## Related code files
- `src/cmar/boost/BoostedBenchmarkRunner.java` (metric emission; ensure F1+Recall+ruleCount+runtime)
- `src/cmar/Metrics.java` (macro-F1, macro-Recall, per-class recall, G-mean/balanced acc)
- `src/cmar/stats/FriedmanNemenyi.java` (feed F1 matrix of arms; CD output)
- `src/cmar/stats/ModernBaselines.java` (literature acc table — ECBA-EX already removed in Phase 1)
- NEW (optional) `src/cmar/stats/AblationDriver.java` OR `run-ablation.ps1`
- `run-benchmark.ps1` (existing — extend or mirror)

## Implementation Steps
1. Add macro-Recall, minority-Recall, G-mean, balanced-acc to `Metrics` if missing; emit in runner.
2. Emit rule count + wall-clock per dataset.
3. Write `run-ablation.ps1` running arms A0..A5 (+opt) with `-Xmx950m -XX:+UseSerialGC` and
   `--deterministic`, each to its own `results/ablation-*.md` + a combined CSV.
4. Compute imbalance ratio per dataset (one-off print) → mark imbalanced subset (ratio ≥ ~2 or as
   defined); report subset-averaged F1/Recall separately.
5. Build the per-dataset delta tables (arm − A1, arm − A0) for Acc/F1/Recall.
6. Run Friedman across arms on F1; if significant, Nemenyi post-hoc + CD diagram (output critical
   difference and average ranks; render CD as a small table or asymptote values for the paper).
7. Sanity: re-run A4 twice, confirm identical numbers (reproducibility gate).
8. Archive raw `results/` outputs; these are the paper's source of truth.

## Todo list
- [ ] Metrics: macro-Recall, minority-Recall, G-mean, balanced-acc
- [ ] Runner emits F1/Recall/ruleCount/runtime per dataset
- [ ] run-ablation.ps1 (A0..A5 + opt) deterministic, memory-capped
- [ ] Imbalance ratio + imbalanced-subset averages
- [ ] Delta tables (vs A0, vs A1)
- [ ] Friedman + Nemenyi + CD on F1 ranking of arms
- [ ] Reproducibility gate (A4 x2 identical)
- [ ] Archive results/

## Success Criteria
- Full 26-set run completes under `-Xmx950m -XX:+UseSerialGC` for every arm (no OOM).
- A4 (headline) shows macro-F1 AND macro-Recall ≥ A1, with weak-set (Diabetes/Heart/Pima/Cleve)
  average F1 & Recall strictly improved.
- Friedman p-value + Nemenyi CD computed; headline arm ranks best (or tied-best) on F1.
- Two A4 runs byte-identical (reproducible).
- Every table cell traceable to a `results/*.md`/CSV produced by real runs.

## Risk Assessment
- **Med:** OOM on Waveform(5000)/Hypo(3163)/Led7(3200) with fuzzy weights at T=10. Mitigation:
  serial GC, reuse arrays, lower T only if forced (document). Test big sets first.
- **Med:** improvements wash out in the average (gains concentrated on weak sets). Mitigation: report
  imbalanced-subset averages prominently; that is the honest thesis.
- **Low:** Friedman non-significant across arms (few arms). Mitigation: include literature baselines
  (C4.5/CBA/CMAR/CPAR/MCAR/Ours) for the acc CD diagram too.

## Security Considerations
- Read-only on `datasets/`; writes only under `results/`. No network for the reproducible run
  (local CSVs authoritative). No secrets.

## Next steps
Phase 06 turns these tables into the paper.
