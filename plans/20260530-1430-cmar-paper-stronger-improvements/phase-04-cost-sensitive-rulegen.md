# Phase 04 — Cost-Sensitive Rule Generation

## Context links
- [plan.md](plan.md) · [phase-01](phase-01-honesty-cleanup.md)
- Research TIER-2 #3: `research/researcher-01-improvement-techniques.md` (SSCR, Elkan, MetaCost)
- Scout: cost-sensitivity currently ONLY at voting (`CMARClassifier.useCostSensitive`), NOT at rule
  generation/pruning.

## Overview
- **Date:** 2026-05-30
- **Description:** Push class cost into rule SELECTION and coverage PRUNING (not just voting) so
  minority-class rules survive database-coverage pruning. Complements existing cost-sensitive
  voting; targets minority recall on imbalanced sets.
- **Priority:** MEDIUM (strong imbalance story; complements Phase 2; independent of Phase 3)
- **Implementation status:** NOT STARTED
- **Review status:** NOT REVIEWED

## Key Insights
- CMAR coverage pruning keeps rules in CMAR sort order (conf desc, supp desc, length asc) and drops
  a rule once each covered instance is covered ≥ delta times. Majority rules dominate the sort, so
  minority rules get pruned away → low minority recall. We already added `stratifiedTopN` to protect
  top-N per class — cost-sensitive ranking generalizes/strengthens that.
- SSCR idea: rank rules by estimated misclassification COST, not raw confidence. Embed cost into the
  comparator used BEFORE coverage pruning, and into the per-instance coverage budget (give
  minority-covering rules a higher survival priority).

## Cost-sensitive rule score (the formula)
Cost per class (parameter-free default): `cost(c) = N / (S · freq(c))` (inverse class frequency,
normalized; S=#classes). Minority class → larger cost.
Composite rule score used for ranking before coverage pruning:
```
score(r) = confidence(r) · lift(r) · cost(class(r))
```
(`lift` already on `Rule`; conf already computed.) Coverage pruning iterates rules in DESC `score`
order instead of plain CMAR order. Optionally weight the coverage budget: a rule covering minority
instances decrements their budget slower (`effectiveDelta(c) = ceil(delta / cost_norm(c))`) so more
minority rules are retained. Keep it simple: start with score-ordered pruning + the existing
`stratifiedTopN` protection; add budget weighting only if recall still lags.

## Requirements
1. `cost(c)` computed from TRAIN class frequencies (already available via `Rule.CLASS_FREQS`,
   `Rule.TOTAL_N`).
2. A cost-aware comparator driving coverage pruning order, gated by `--costRuleGen`.
3. Must not regress majority recall badly (monitor macro-Recall, not just minority).
4. Composes with Fuzzy (Phase 2) and either discretizer (Phase 3).

## Architecture
- Add `RulePruner.useCostRuleGen` static flag + a `cost(int classLabel)` helper reading
  `Rule.CLASS_FREQS`/`TOTAL_N`.
- In `coveragePrune`, when `useCostRuleGen`, sort the working rule list by `score(r)` desc (above
  formula) before the coverage sweep, instead of relying on natural `Rule.compareTo`. Keep
  `stratifiedTopN` protection on top (DRY — same protect-then-cover structure).
- `lift` must be populated on rules before pruning. Verify where lift is set; if only set under
  `useHMLift`, compute lift unconditionally for cost scoring (cheap: supp·N/(antSupp·classSupp)).

## Related code files
- `src/cmar/RulePruner.java` (add flag + cost() + score-ordered coverage sweep; existing
  `stratifiedTopN` at line 24, `coveragePrune` method)
- `src/cmar/Rule.java` (`lift`, `CLASS_FREQS`, `TOTAL_N` already present lines 18,23-25; ensure lift
  computed for all rules, not just HMLift path)
- `src/cmar/CMARClassifier.java` (populates CLASS_FREQS in `fit`; verify it runs before pruning)
- `src/cmar/boost/BoostedBenchmarkRunner.java` (`--costRuleGen` flag)

## Implementation Steps
1. Confirm `Rule.CLASS_FREQS`/`TOTAL_N` are set before `RulePruner.coveragePrune` runs (trace from
   `CMARClassifier.fit`). If not, set them in the pruning entry.
2. Ensure `lift` is computed for every rule (move lift calc out of the `useHMLift` guard if needed).
3. Add `RulePruner.useCostRuleGen` + `static double cost(int c)`.
4. In `coveragePrune`: if `useCostRuleGen`, build a comparator on `-score(r)` and sort the candidate
   list (after stratified protection set is reserved). Run the normal coverage sweep on the sorted
   list.
5. Add `--costRuleGen` CLI; in `BoostedBenchmarkRunner` set `RulePruner.useCostRuleGen=true`.
6. Evaluate on imbalanced sets (Sick, Hypo, Hepatitis, German, Pima, Diabetes): compare
   macro-Recall & minority-Recall vs baseline and vs `costSensitive` voting alone, and combined.
7. If minority recall still lags, enable budget weighting `effectiveDelta(c)`; otherwise leave out
   (KISS).

## Todo list
- [ ] Verify/ensure CLASS_FREQS + TOTAL_N set pre-pruning
- [ ] Compute lift unconditionally
- [ ] RulePruner.useCostRuleGen + cost(c) + score()
- [ ] Score-ordered coverage sweep (keep stratified protection)
- [ ] --costRuleGen CLI flag
- [ ] Ablation: voting-cost vs rulegen-cost vs both, on imbalanced sets
- [ ] (Conditional) budget weighting effectiveDelta(c)

## Success Criteria
- On imbalanced datasets, macro-Recall and macro-F1 ≥ baseline; minority-class recall strictly up on
  ≥3 of the imbalanced sets.
- No catastrophic majority-recall drop (overall accuracy within ~0.5% of baseline).
- Combined (fuzzy + cost-rulegen) ≥ each alone on weak-set average (additive or neutral, not harmful).
- Deterministic & reproducible under `--deterministic`.

## Risk Assessment
- **Med:** over-favoring minority tanks majority recall → macro stays flat. Mitigation: inverse-freq
  cost (mild), keep delta; tune only if needed.
- **Med:** lift now computed for all rules adds cost. Cheap (O(rules)); acceptable.
- **Low:** interaction with stratifiedTopN double-protects minority → fine (only helps recall).

## Security Considerations
- Cost derived from train-fold frequencies only; no leakage, no external input.

## Next steps
Phase 05 isolates each component (fuzzy, CAIM, cost-rulegen) in a full ablation + significance tests.
