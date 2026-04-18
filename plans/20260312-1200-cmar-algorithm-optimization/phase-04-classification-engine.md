# Phase 04: Classification Engine

## Context
- **Parent:** [plan.md](plan.md)
- **Dependencies:** [Phase 01](phase-01-core-data-structures.md) (CRTree), [Phase 03](phase-03-rule-management.md) (pruned weighted rules)
- **Research:** [researcher-02](research/researcher-02-report.md) sec 3,4

## Overview
- **Date:** 2026-03-12
- **Description:** Weighted chi-square classifier with bitwise matching, lazy evaluation, class-partitioned rule lookup
- **Priority:** High
- **Implementation Status:** pending
- **Review Status:** pending

## Key Insights
- CMAR groups matching rules by class, sums chi-square weights per class, predicts class with highest total weight
- Bitmap antecedent representation enables 50-100x speedup for rule matching via bitwise AND
- Lazy evaluation: score only top-k rules by confidence first, compute full chi-square only for competitive classes
- Class-partitioned CRTree subtrees reduce search space 5-20x
- Early termination: stop when remaining rules can't change winning class

## Requirements
**Functional:**
- Given new instance, find all matching rules from CRTree
- Group matching rules by class label
- Sum weighted chi-square per class group
- Predict class with highest weighted sum
- Support predict_proba (normalized weights as pseudo-probabilities)
- Handle ties (fall back to highest support class)

**Non-functional:**
- Classification: <1ms per instance (cached CRTree)
- Batch prediction: vectorized where possible
- Support datasets with 100+ classes

## Architecture
```
New Instance
     |
     v
Encode as bitmap (BitVectorDB format)
     |
     v
CRTree Lookup (class-partitioned)
  - Bitwise AND: instance_bitmap & rule_antecedent_bitmap == rule_antecedent_bitmap
     |
     v
Group matching rules by class
     |
     v
Lazy Evaluation:
  - Take top-k rules per class by confidence
  - Sum chi-square weights per class
  - Early terminate if winner is clear
     |
     v
Predict class with max weighted sum
```

## Related Code Files
| File | Action | Description |
|------|--------|-------------|
| `src/cmar/classifier.py` | create | CMARClassifier class |
| `src/cmar/data_structures.py` | modify | Add bitmap antecedent to Rule, bitmap matching to CRTree |
| `tests/test_classifier.py` | create | Classification accuracy and speed tests |

## Implementation Steps

1. **Add bitmap antecedent representation to rules**
   - During CRTree construction (Phase 03), convert each rule's antecedent frozenset to numpy uint8 bitmap
   - `Rule.antecedent_bitmap: np.ndarray[uint8]` - one bit per possible item
   - Precompute once during fit, reuse during predict

2. **Implement bitmap-based rule matching**
   - `match_rules(instance_bitmap, rules)`: vectorized matching
   ```python
   # For batch: stack rule bitmaps into matrix (n_rules, n_bitmap_bytes)
   # result = (rule_bitmaps & instance_bitmap) == rule_bitmaps
   # matched = np.all(result, axis=1)
   ```
   - Return indices of matching rules
   - Use NumPy broadcasting for batch instances

3. **Implement class-partitioned lookup**
   - CRTree stores rules partitioned by class label
   - For each class partition, run bitmap matching independently
   - Skip class partitions that have no rules with items present in instance

4. **Implement weighted chi-square aggregation**
   - For each class with matching rules:
     - Sum `rule.weight` for all matching rules (weight = chi_sq by default)
     - Store as `class_scores[class_label] = total_weight`
   - Predict: `argmax(class_scores)`

5. **Implement lazy evaluation optimization**
   - Pre-sort rules within each class partition by confidence descending
   - For each class, evaluate only top-k rules (default k=10)
   - If top class has weight > 2x second class after k rules: early terminate
   - Configurable via `lazy_k` parameter

6. **Implement early termination**
   - After processing each class partition:
     - Compute maximum possible remaining weight (sum of all unevaluated rule weights)
     - If current leader cannot be overtaken: stop
   - Requires pre-computed total weight per class partition

7. **Implement predict and predict_proba**
   - `predict(X)`: return array of predicted class labels
   - `predict_proba(X)`: normalize class_scores to sum to 1.0 per instance
   - Handle edge cases: no matching rules -> predict majority class from training
   - Vectorize: process instances in batches for NumPy efficiency

8. **Implement batch prediction**
   - Convert all test instances to bitmap matrix at once
   - Use matrix operations for matching: `(rule_matrix & instance_matrix[:, None]) == rule_matrix`
   - Aggregate scores using NumPy advanced indexing
   - Target: process 1000 instances simultaneously

9. **Implement CMARClassifier public interface**
   - Scikit-learn compatible: `fit(X, y)`, `predict(X)`, `predict_proba(X)`, `score(X, y)`
   - `__init__(min_support=0.01, min_confidence=0.5, chi_sq_threshold=3.841, weighting='chi_square', lazy_k=10)`
   - `fit()` internally calls FPGrowthMiner and RuleManager
   - Store fitted state: CRTree, class priors, majority class, item encoder

10. **Write tests**
    - Test on iris (small, multi-class), mushroom (binary, categorical), adult (mixed types)
    - Compare accuracy against scikit-learn DecisionTreeClassifier (baseline)
    - Verify predict_proba sums to 1.0
    - Benchmark: 10K predictions, measure latency distribution
    - Test edge cases: unseen items, empty antecedent matches, single-class data

## Todo List
- [ ] Add bitmap antecedent to Rule dataclass
- [ ] Implement bitmap-based vectorized rule matching
- [ ] Implement class-partitioned CRTree lookup
- [ ] Implement weighted chi-square aggregation
- [ ] Implement lazy evaluation (top-k per class)
- [ ] Implement early termination
- [ ] Implement predict and predict_proba
- [ ] Implement batch prediction with matrix ops
- [ ] Create CMARClassifier with scikit-learn API
- [ ] Write accuracy tests on UCI datasets
- [ ] Write performance benchmarks

## Success Criteria
- Accuracy >= DecisionTreeClassifier on iris, mushroom, adult datasets
- Classification latency: <1ms per instance (median)
- Batch 10K instances in <1s
- predict_proba values valid (sum=1, non-negative)
- Lazy evaluation reduces computation by 50%+ vs exhaustive (measure via counter)

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Bitmap memory for wide datasets (10K+ items) | 1.2KB per rule bitmap | Sparse bitmap representation for >1000 items |
| No matching rules for some instances | Classification failure | Default to majority class; log warning |
| Lazy-k too small misses important rules | Accuracy drop | Adaptive k based on class count; configurable |
| Batch prediction memory for large test sets | OOM | Process in chunks of 1000 |

## Security Considerations
- Input validation: X must match training feature dimensions
- No information leakage from training data in predict_proba
- Sanitize inputs: handle NaN, missing values gracefully

## Next Steps
- Phase 05 wraps this into clean API and benchmarking suite
- Hyperparameter tuning (min_support, min_confidence, lazy_k) deferred to Phase 05
