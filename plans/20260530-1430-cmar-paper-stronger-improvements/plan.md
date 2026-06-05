# Plan — Stronger CMAR Improvements + Publishable Paper

**Date:** 2026-05-30  **Owner:** manhcanda04@gmail.com  **Target:** international student science competition.

## Goal
Improve CMAR so BOTH macro-F1 AND macro-Recall rise (not just accuracy) beyond current
+2.17% F1 / +2.54% Recall, focused on weak continuous-medical datasets (Diabetes, Heart, Pima,
Cleve). Headline lever = **Fuzzy CMAR** (triangular fuzzy membership on continuous attrs).
Secondary = CAIM/CACC discretization + cost-sensitive rule generation. Then write the paper.

## Hard constraints (non-negotiable)
- NO fake/synthetic data. Every number from real Java code on real UCI CSVs in `datasets/`.
- DELETE synthetic fallback code in `UCIDatasets.java` (prove no fake data).
- FIX/REMOVE unverified ECBA-EX baselines in `ModernBaselines.java`.
- Java only, no Python. Run with `-Xmx950m -XX:+UseSerialGC` (memory-constrained).
- Honest gains: sub-1% to ~2% acc; real story = F1/Recall on imbalanced data.
- Reproducible: fixed seed=42, leak-free per-fold discretization (already in `encodeFold`).

## Baseline (verified live, honest config)
Acc 85.47%, F1 82.84%, Recall 83.48%. Config: `bagging T=10, fs=1.0, stratified=10,
costSensitive, adaptMinSup sqrt, minSupScale=0.3`, NO topK.

## Phases
| # | Phase | Status | Link |
|---|-------|--------|------|
| 1 | Honesty cleanup (del synthetic, fix baselines, lock seed) | NOT STARTED | [phase-01](phase-01-honesty-cleanup.md) |
| 2 | Fuzzy CMAR (FuzzyDiscretizer + fuzzy support in FP-Growth) | NOT STARTED | [phase-02](phase-02-fuzzy-cmar.md) |
| 3 | CAIM/CACC discretization + ablation vs MDL vs Fuzzy | NOT STARTED | [phase-03](phase-03-caim-discretization.md) |
| 4 | Cost-sensitive rule generation (cost into coverage pruning) | NOT STARTED | [phase-04](phase-04-cost-sensitive-rulegen.md) |
| 5 | Full benchmark + ablation + Friedman/Nemenyi | NOT STARTED | [phase-05](phase-05-benchmark-ablation.md) |
| 6 | Paper writing (LaTeX, related work, reproducibility) | NOT STARTED | [phase-06](phase-06-paper-writing.md) |

## Sequencing
Phase 1 FIRST (user's #1 concern; unblocks honest reporting). Phase 2 is the headline; do before
3 & 4. Phase 3 and 4 are independent of each other (both depend on 1; 3 can reuse 2's plumbing).
Phase 5 needs 1-4 done. Phase 6 needs 5's tables.

## Key references (from research)
- Fuzzy AC: Sowan et al. ESWA 41(13) 2014; FACA, Applied Soft Computing 48 (2016) 729-734.
- CAIM: Kurgan & Cios, IEEE TKDE 16(2) 2004. CACC: Tsai et al., Inf. Sci. 178 (2008).
- Cost-sensitive AC (SSCR): PAKDD 2014, LNCS 8643. Elkan IJCAI 2001.
- CMAR anchor: Li, Han, Pei, ICDM 2001 (C4.5 83.34 / CBA 84.69 / CMAR 85.22, 26 sets, 10-fold).
- Stats: Friedman + Nemenyi + CD diagram (modern AC venue expectation).

## Unresolved questions (decide during execution)
1. Fuzzy membership: how to assign one instance to multiple fuzzy bins downstream? CMAR transactions
   are crisp `int[]` (one item per attr). Options: (a) expand each continuous attr into ≤3 fuzzy
   items per instance with weights (changes `int[][]` → weighted items — bigger refactor); (b) keep
   crisp item per instance but use fuzzy SUPPORT counting at mining time. Plan picks (a)-lite:
   emit the top-1 or top-2 membership items per attr, carry per-(transaction,item) weight. Confirm
   which yields F1 gain without OOM. **Needs empirical pick in Phase 2.**
2. Fuzzy breakpoints: percentiles (33/66) vs MDL-cut-points-as-centers vs CAIM-centers. Pick by
   ablation in Phase 3 (cheap — CAIM seeds fuzzy centers).
3. CACC vs CAIM: implement CAIM first (simpler); add CACC only if CAIM underperforms MDL. YAGNI.
4. Cost matrix source: inverse class frequency vs tuned. Default = inverse freq (parameter-free,
   honest). Confirm it doesn't hurt majority recall too much.
5. Which 15-20 datasets are "imbalanced" enough to headline? Candidates: Sick, Hypo, Hepatitis,
   Pima, Diabetes, German, Breast-Cancer. Compute imbalance ratio in Phase 5, report it.
6. Number of fuzzy ensemble members vs runtime under `-Xmx950m`. Keep T=10; verify no OOM.
7. Paper venue/template: MDPI Mathematics vs PeerJ CS LaTeX class. Pick in Phase 6.
