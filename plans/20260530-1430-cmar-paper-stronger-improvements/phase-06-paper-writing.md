# Phase 06 — Paper Writing

## Context links
- [plan.md](plan.md) · phases 01-05
- Landscape: `research/researcher-02-paper-landscape.md` (related-work table, venues, eval norms,
  competitive bar, honest-framing thesis)
- Techniques: `research/researcher-01-improvement-techniques.md` (citations for fuzzy/CAIM/cost)

## Overview
- **Date:** 2026-05-30
- **Description:** Write a publishable LaTeX paper presenting Fuzzy CMAR + cost-sensitive rule
  generation, with honest F1/Recall-focused results on imbalanced data, full benchmark tables,
  Friedman/Nemenyi significance, and a reproducibility appendix. For an international student
  competition / open-access mid-tier venue.
- **Priority:** HIGH (the deliverable)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- Thesis (honest, modern): "Accuracy is misleading under imbalance. Classic CMAR optimizes
  confidence → majority bias. Fuzzy membership on continuous attributes + cost-sensitive rule
  generation recover minority Recall/F1 on medical/imbalanced data at preserved accuracy and full
  rule interpretability." Promise significant imbalance-metric gain, NOT large accuracy jumps.
- Differentiators that beat 90% of student AC work: Friedman+Nemenyi+CD diagram; per-component
  ablation; rule-count + runtime reporting; full reproducibility (seed, deterministic, memory cap,
  per-fold leak-free discretization, public UCI CSVs).
- Anchor triplet from one apples-to-apples table: C4.5 83.34 / CBA 84.69 / CMAR 85.22 (Li et al.
  ICDM 2001, 26 sets, 10-fold). Use these as the literature reference.

## Requirements
1. LaTeX source compiling to PDF (template = MDPI Mathematics or PeerJ CS — decide in step 1).
2. Sections: Abstract, Intro, Related Work (table from landscape), Background (CMAR), Method
   (Fuzzy CMAR + cost-sensitive rule gen + CAIM ablation), Experimental Setup, Results (per-dataset
   + ablation + significance), Discussion (honest limits), Conclusion, Reproducibility Appendix.
3. ALL numbers from Phase 5 `results/` — zero invented values; ECBA-EX removed (Phase 1).
4. Figures: CD diagram, weak-set F1/Recall bar chart, fuzzy-vs-crisp boundary illustration.

## Architecture / paper structure
- `paper/` dir: `main.tex`, `refs.bib`, `figures/`, `tables/` (auto-included from results where
  possible).
- Tables generated/copied from `results/*.csv` (Phase 5) → keep a script or manual step that turns
  CSV into LaTeX `tabular` to avoid transcription errors.

## Related code files / artifacts
- `results/ablation-*.md`, combined CSV (Phase 5) — table source
- `src/cmar/stats/FriedmanNemenyi.java` output — CD diagram values
- `src/cmar/stats/ModernBaselines.java` — literature acc baselines (no ECBA-EX)
- NEW `paper/main.tex`, `paper/refs.bib`, `paper/figures/*`

## Implementation Steps
1. Pick venue/template (Mathematics MDPI vs PeerJ CS); scaffold `paper/main.tex` + `refs.bib`.
2. Write Related Work using the landscape table (CBA, CMAR, CPAR, MCAR, ACAC, ACCF, CACA, WCBA,
   ACPRISM, etc.); mark unverified reported numbers as "as reported" — keep honesty.
3. Background: CMAR (weighted chi-square multi-rule, FP-growth/CR-tree, coverage pruning).
4. Method: (a) triangular fuzzy membership formula + fuzzy support/confidence (from Phase 2);
   (b) CAIM criterion + as fuzzy-center seed (Phase 3); (c) cost-sensitive rule score
   `conf·lift·cost(c)` + score-ordered coverage pruning (Phase 4). Include the existing real
   improvements (stratified coverage, cost-vote, bagging, adaptive minSup) as the strong baseline.
5. Experimental Setup: 26 UCI sets (list + imbalance ratios), 10-fold CV, seed=42, deterministic,
   `-Xmx950m -XX:+UseSerialGC`, leak-free per-fold discretization, metrics (Acc, macro-F1,
   macro-Recall, minority-Recall, G-mean, balanced acc, rule count, runtime).
6. Results: per-dataset table; ablation table (A0..A5); imbalanced-subset averages; Friedman/Nemenyi
   + CD diagram. Lead with F1/Recall; report acc honestly.
7. Discussion: where gains concentrate (continuous-medical), where neutral (categorical), honest
   limits (fuzzy chi² approximation, sub-1–2% acc, dataset-specific CAIM).
8. Reproducibility Appendix: exact commands, seed, JVM flags, dataset provenance (real UCI), code
   availability statement.
9. Figures: CD diagram; weak-set F1/Recall bars; one fuzzy-vs-crisp boundary schematic.
10. Proofread for honesty: no claim unsupported by a `results/` number; no synthetic data; no
    ECBA-EX.

## Todo list
- [ ] Pick venue + scaffold LaTeX (main.tex, refs.bib)
- [ ] Related Work table + citations
- [ ] Background (CMAR)
- [ ] Method (fuzzy + CAIM + cost-rulegen formulas)
- [ ] Experimental Setup (protocol, reproducibility, metrics)
- [ ] Results tables from Phase-5 CSV (no transcription errors)
- [ ] Friedman/Nemenyi + CD diagram figure
- [ ] Weak-set F1/Recall + fuzzy-boundary figures
- [ ] Discussion + honest limitations
- [ ] Reproducibility Appendix (commands, seed, JVM flags, data provenance)
- [ ] Honesty pass; compile to PDF

## Success Criteria
- PDF compiles; all tables/figures populated from real Phase-5 results.
- Headline claim supported: significant macro-F1/Recall gain on imbalanced subset at preserved
  accuracy, with Friedman/Nemenyi backing and a CD diagram.
- No synthetic data, no ECBA-EX, no unverifiable number anywhere.
- Reproducibility appendix lets a reviewer regenerate every number with the given commands.

## Risk Assessment
- **Med:** results too modest to look novel. Mitigation: lead with the honest imbalance thesis +
  significance + interpretability; novelty = Fuzzy CMAR + cost-sensitive rule generation combo.
- **Low:** LaTeX template friction. Mitigation: use a known MDPI/PeerJ class early.
- **Low:** transcription errors. Mitigation: generate tables from CSV.

## Security Considerations
- Cite real UCI sources; include data-availability + code-availability statements; no private data.

## Next steps
Submit. Keep `results/` + code tagged at the exact commit used for the paper for auditability.
