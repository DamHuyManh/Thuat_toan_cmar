# Phase 02: FP-Growth Mining

## Context
- **Parent:** [plan.md](plan.md)
- **Dependencies:** [Phase 01](phase-01-core-data-structures.md) (FPTree, BitVectorDB)
- **Research:** [researcher-01](research/researcher-01-report.md) sec 4, [researcher-02](research/researcher-02-report.md) sec 1

## Overview
- **Date:** 2026-03-12
- **Description:** Optimized FP-growth with lazy pruning, compressed conditional trees, class-distribution-aware mining
- **Priority:** Critical
- **Implementation Status:** pending
- **Review Status:** pending

## Key Insights
- Prune infrequent items from conditional pattern bases BEFORE building conditional FP-trees (40-70% size reduction)
- Two-pass scan: first pass counts item frequencies, second builds tree with items sorted by frequency desc
- Class-distribution tracking at leaf nodes enables early class-association rule extraction
- Single-path optimization: when conditional FP-tree is single path, enumerate all subsets directly (no recursion)
- Early termination: stop mining prefix when remaining support below threshold

## Requirements
**Functional:**
- Mine all class-association rules (CARs) meeting min_support and min_confidence
- Support min_support as absolute count or relative frequency
- Return rules as (antecedent, class_label, support, confidence) tuples
- Handle multi-class datasets

**Non-functional:**
- Target: 10M itemsets/sec on single core
- Memory: conditional trees should not exceed 2x base tree size total
- Scale to 1M transactions, 1000 items

## Architecture
```
Transactions --> Item Frequency Count --> Sort Items
     |
     v
Build FP-Tree (Phase 01 structure)
     |
     v
For each item (least frequent first):
  Extract conditional pattern base
  Prune infrequent items
  Build conditional FP-tree (compressed)
  If single-path: enumerate subsets
  Else: recurse
     |
     v
Emit CARs with class distribution
```

## Related Code Files
| File | Action | Description |
|------|--------|-------------|
| `src/cmar/fp_growth.py` | create | FPGrowthMiner class |
| `src/cmar/data_structures.py` | modify | Add class-distribution tracking to FPTree |
| `tests/test_fp_growth.py` | create | Mining correctness and performance tests |

## Implementation Steps

1. **Implement item frequency counting**
   - `count_item_frequencies(transactions)`: return dict[item_id -> count]
   - Filter items below min_support
   - Sort items by frequency descending -> create item ordering

2. **Implement transaction preprocessing**
   - `preprocess(transactions, item_order, class_labels)`: sort items in each transaction by frequency order, pair with class label
   - Remove infrequent items from transactions
   - Use NumPy argsort for vectorized sorting where possible

3. **Implement class-aware FP-tree construction**
   - Extend FPTree with `class_counts: np.ndarray[int32, (n_nodes, n_classes)]` at leaf nodes
   - During insertion, propagate class label to leaf node counter
   - This enables computing class-conditional support directly from tree

4. **Implement conditional pattern base extraction**
   - `get_conditional_pattern_base(item_id)`: for each node in header table chain for item_id, walk to root collecting (prefix_path, count)
   - Aggregate duplicate prefix paths by summing counts
   - Prune items from prefix paths that fall below min_support (lazy pruning)

5. **Implement conditional FP-tree construction**
   - `build_conditional_tree(pattern_base, min_support)`: build new FPTree from filtered pattern base
   - Track class distribution through conditional trees
   - Single-path detection: check if every node has at most one child

6. **Implement single-path enumeration**
   - When conditional tree is single path, generate all 2^k subsets of path items
   - Each subset's support = minimum count along path
   - Emit CARs directly without recursion
   - Use bit manipulation for subset enumeration (fast for k < 20)

7. **Implement recursive FP-growth**
   - `mine(tree, prefix, min_support, min_confidence)`:
     - For each item in header table (ascending frequency):
       - New pattern = prefix + {item}
       - Calculate support from tree
       - If support >= min_support: compute confidence per class, emit CARs meeting min_confidence
       - Build conditional tree, recurse if non-empty
   - Add depth limit parameter (default 10) to prevent pathological recursion

8. **Implement FPGrowthMiner public interface**
   - `__init__(min_support, min_confidence, max_rule_length=10)`
   - `fit(X, y)`: preprocess, build tree, mine rules, return list[Rule]
   - `get_rules()`: return mined rules sorted by (confidence desc, support desc)

9. **Add Numba optimization for hot paths**
   - JIT compile `_insert_transaction`, prefix path extraction
   - Use `@numba.njit(cache=True)` for repeated calls
   - Benchmark JIT vs pure NumPy; keep whichever is faster

10. **Write tests**
    - Verify against known frequent itemsets from textbook examples
    - Test single-path optimization produces same results as full recursion
    - Test class distribution tracking correctness
    - Benchmark: 100K transactions, measure time and memory

## Todo List
- [ ] Implement item frequency counting and filtering
- [ ] Implement transaction preprocessing with class labels
- [ ] Extend FPTree with class-distribution tracking
- [ ] Implement conditional pattern base extraction with lazy pruning
- [ ] Implement conditional FP-tree construction
- [ ] Implement single-path subset enumeration
- [ ] Implement recursive FP-growth mining
- [ ] Create FPGrowthMiner public interface
- [ ] Add Numba JIT for hot paths
- [ ] Write correctness tests
- [ ] Write performance benchmarks

## Success Criteria
- Mines correct CARs on iris, mushroom datasets (verified against reference implementation)
- 100K transactions mined in <5s
- No conditional tree exceeds 2x base tree node count
- Single-path optimization activates correctly (verify via logging)

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Exponential conditional trees on dense data | OOM | Max rule length cap; prune aggressively |
| Numba incompatibility with dict/frozenset | Can't JIT critical paths | Use integer arrays for item representation internally |
| Class distribution tracking doubles tree memory | Exceeds memory targets | Sparse class counters (only store non-zero) |

## Security Considerations
- Validate min_support > 0 and min_confidence in (0, 1]
- Cap max_rule_length to prevent combinatorial explosion
- No external network calls

## Next Steps
- Rules feed into Phase 03 for pruning and chi-square computation
- Profile on target datasets to tune min_support defaults
