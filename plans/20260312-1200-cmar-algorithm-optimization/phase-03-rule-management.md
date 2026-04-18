# Phase 03: Rule Management

## Context
- **Parent:** [plan.md](plan.md)
- **Dependencies:** [Phase 01](phase-01-core-data-structures.md) (CRTree, Rule), [Phase 02](phase-02-fp-growth-mining.md) (mined CARs)
- **Research:** [researcher-01](research/researcher-01-report.md) sec 3,6, [researcher-02](research/researcher-02-report.md) sec 2,3

## Overview
- **Date:** 2026-03-12
- **Description:** Rule pruning pipeline (DCP + CSP + redundancy elimination), vectorized chi-square, rule weighting
- **Priority:** High
- **Implementation Status:** pending
- **Review Status:** pending

## Key Insights
- DCP + CSP combo reduces rule set 40-70% with minimal accuracy loss
- Vectorized chi-square via NumPy broadcasting: 3-10x speedup over scalar loops
- Batch chi-square with broadcasting: 500-1000x faster than per-rule Python loops
- Early termination when chi-square variance stabilizes (no >5% improvement over 100 rules)
- Information gain weighting improves accuracy 3-8% over equal weighting
- Two-phase pruning: fast confidence filter first, then expensive chi-square

## Requirements
**Functional:**
- Compute chi-square statistic for each rule against training data
- Apply DCP: remove rules whose training coverage is subset of higher-ranked rules
- Apply CSP: remove rules below chi-square significance threshold
- Remove redundant rules (strict subset antecedents with lower confidence)
- Assign weights: chi-square, support*confidence, or information gain

**Non-functional:**
- Chi-square computation: 100K rules/sec
- Pruning should reduce rule set by 40%+ on typical datasets
- Total pruning pipeline: <5s for 100K rules

## Architecture
```
Mined CARs (Phase 02)
     |
     v
1. Confidence Filter (fast, eliminates obvious weak rules)
     |
     v
2. Chi-Square Computation (vectorized, batch)
     |
     v
3. CSP - Remove statistically insignificant rules (chi_sq < threshold)
     |
     v
4. Redundancy Elimination (CR-tree subset detection)
     |
     v
5. DCP - Database coverage pruning (iterative)
     |
     v
6. Weight Assignment
     |
     v
Pruned, weighted rules --> CRTree (Phase 01)
```

## Related Code Files
| File | Action | Description |
|------|--------|-------------|
| `src/cmar/rule_manager.py` | create | RuleManager, chi-square, pruning, weighting |
| `src/cmar/data_structures.py` | modify | Add coverage tracking to Rule dataclass |
| `tests/test_rule_manager.py` | create | Pruning correctness, chi-square accuracy tests |

## Implementation Steps

1. **Implement vectorized chi-square computation**
   - Input: rules (list[Rule]), training data (BitVectorDB)
   - For each rule, build 2x2 contingency table: (antecedent present/absent) x (class match/no match)
   - Vectorize: compute all contingency tables as NumPy matrices
   ```python
   # Pseudocode for batch chi-square
   # observed: (n_rules, 2, 2) array
   # expected = row_totals * col_totals / grand_total
   # chi_sq = ((observed - expected)**2 / expected).sum(axis=(1,2))
   ```
   - Handle zero expected values: add Yates correction or skip cell
   - Store chi_sq on Rule objects

2. **Implement chi-square significance pruning (CSP)**
   - Default threshold: 3.841 (p=0.05, df=1 for 2x2 table)
   - `csp_prune(rules, threshold=3.841)`: filter rules where chi_sq >= threshold
   - Support configurable significance levels

3. **Implement redundancy elimination**
   - Sort rules by (confidence desc, support desc, antecedent length asc)
   - For each rule r: if exists rule r' with r'.antecedent subset of r.antecedent AND r'.confidence >= r.confidence AND r'.class == r.class: remove r
   - Use CRTree for O(log k) subset detection

4. **Implement database coverage pruning (DCP)**
   - Sort rules by (chi_sq desc, confidence desc)
   - Maintain coverage array: bool[n_training_instances]
   - For each rule (highest ranked first):
     - Mark training instances matching this rule as covered
     - If rule covers no uncovered instances: prune it
     - Stop when all instances covered or remaining rules exhausted
   - Use BitVectorDB for fast coverage computation (AND + popcount)

5. **Implement early termination**
   - During DCP, track rolling chi-square variance over last 100 rules
   - If variance drops below 5% of mean: stop adding rules
   - Configurable window size and threshold

6. **Implement rule weighting**
   - `weight_chi_square(rule)`: weight = rule.chi_sq (default CMAR)
   - `weight_support_confidence(rule)`: weight = support * confidence
   - `weight_information_gain(rule, class_priors)`: weight = IG of rule antecedent for class
   - Information gain: IG = H(class) - H(class | antecedent)
   - Compute class entropy once, cache it

7. **Implement RuleManager public interface**
   - `__init__(chi_sq_threshold=3.841, weighting='chi_square', early_termination=True)`
   - `fit(rules, X_train, y_train)`: run full pruning pipeline, assign weights, build CRTree
   - `get_rules()`: return pruned, weighted rules
   - `get_cr_tree()`: return populated CRTree

8. **Write tests**
   - Test chi-square against scipy.stats.chi2_contingency (ground truth)
   - Test DCP removes correct rules on known dataset
   - Test CSP removes rules below threshold
   - Test redundancy elimination on hand-crafted overlapping rules
   - Benchmark: 100K rules pruning time

## Todo List
- [ ] Implement vectorized chi-square (NumPy batch)
- [ ] Implement CSP with configurable threshold
- [ ] Implement redundancy elimination via CRTree
- [ ] Implement DCP with coverage tracking
- [ ] Implement early termination heuristic
- [ ] Implement rule weighting (3 schemes)
- [ ] Create RuleManager public interface
- [ ] Validate chi-square against scipy
- [ ] Write pruning correctness tests
- [ ] Benchmark pruning pipeline

## Success Criteria
- Chi-square values match scipy.stats.chi2_contingency within 1e-6
- Pruning reduces rule set 40%+ on mushroom dataset
- 100K rules processed in <5s
- Weighted rules improve classification accuracy vs unweighted (tested in Phase 04)

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Zero cells in contingency table | Division by zero in chi-square | Yates correction; skip rules with zero expected |
| DCP ordering sensitivity | Different orderings produce different rule sets | Deterministic tiebreaker (rule ID); test sensitivity |
| Early termination too aggressive | Loses important minority-class rules | Per-class termination tracking; minimum rules per class |

## Security Considerations
- Chi-square threshold must be positive (validate input)
- No data leakage: pruning uses only training data
- Rule weights bounded to prevent overflow in classification

## Next Steps
- Pruned CRTree feeds into Phase 04 classification engine
- Weighting scheme choice may need tuning per dataset (expose as hyperparameter)
