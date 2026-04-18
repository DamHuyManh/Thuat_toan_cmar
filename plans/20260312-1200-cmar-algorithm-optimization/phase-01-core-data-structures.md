# Phase 01: Core Data Structures

## Context
- **Parent:** [plan.md](plan.md)
- **Dependencies:** None (foundation phase)
- **Research:** [researcher-01](research/researcher-01-report.md), [researcher-02](research/researcher-02-report.md)

## Overview
- **Date:** 2026-03-12
- **Description:** Implement FP-tree (array-based), CR-tree (hash-indexed trie), bit-vector transaction DB, and dictionary encoding
- **Priority:** Critical
- **Implementation Status:** pending
- **Review Status:** pending

## Key Insights
- Array-based FP-trees beat pointer-based for cache locality on datasets <10M transactions
- Bit-vector transactions: 8 bits/item vs 32+ for integers; enables bitwise AND for pattern matching
- Dictionary encoding compresses non-numeric domains 50-80%
- Node pooling (pre-allocate bulk) prevents allocation overhead during tree construction

## Requirements
**Functional:**
- FP-tree: insert transactions, build header table, traverse prefix paths, get conditional pattern bases
- CR-tree: insert rules, lookup rules by item subset, prune rules, class-partitioned subtrees
- BitVectorDB: encode transactions as bit matrices, support count via popcount, subset testing via AND
- Dictionary encoder: map categorical values to integers, inverse mapping

**Non-functional:**
- FP-tree insert: O(avg_itemset_length) per transaction
- CR-tree lookup: O(1) amortized via hash indexing
- Memory: bit-vector DB should use <1/4 memory of raw integer representation

## Architecture
```
BitVectorDB (transaction storage)
     |
     v
FP-Tree (array-based, header table)
     |
     v
CR-Tree (hash-indexed trie, class-partitioned)
```

**FP-Tree internal layout (array-based):**
```python
# Parallel arrays instead of node objects
item_ids: np.ndarray[int32]      # item at each node
counts: np.ndarray[int32]        # support count
parents: np.ndarray[int32]       # parent index (-1 for root)
first_child: np.ndarray[int32]   # first child index (-1 for leaf)
next_sibling: np.ndarray[int32]  # next sibling index (-1 for none)
header_next: np.ndarray[int32]   # next node with same item (header table link)
```

**CR-Tree layout:**
```python
# Hash-indexed rule storage
rules: dict[frozenset, list[Rule]]  # antecedent -> rules
class_index: dict[int, list[int]]   # class_label -> rule indices
```

## Related Code Files
| File | Action | Description |
|------|--------|-------------|
| `src/cmar/__init__.py` | create | Package init, public exports |
| `src/cmar/data_structures.py` | create | FPTree, CRTree, BitVectorDB, DictEncoder |
| `src/cmar/utils.py` | create | Common helpers, type aliases |
| `tests/test_data_structures.py` | create | Unit tests for all data structures |

## Implementation Steps

1. **Create project skeleton**
   - Create `src/cmar/` package with `__init__.py`
   - Create `tests/` directory
   - Create `pyproject.toml` with deps: numpy, numba, pytest, scikit-learn

2. **Implement DictEncoder**
   - `fit(data)`: scan columns, build value->int mapping
   - `transform(data)`: convert categorical to integer-encoded
   - `inverse_transform(encoded)`: reverse mapping
   - Store mapping as `dict[str, dict[Any, int]]` per column

3. **Implement BitVectorDB**
   - `__init__(n_items)`: initialize empty bit matrix
   - `encode(transactions)`: convert list[list[int]] to np.ndarray[uint8] bit matrix
   - `support_count(itemset)`: AND columns, popcount result
   - `subset_mask(itemset)`: return boolean mask of transactions containing itemset
   - Use `np.packbits` / `np.unpackbits` for storage efficiency
   - Numba-jit the popcount inner loop

4. **Implement FPTree (array-based)**
   - Pre-allocate arrays with `initial_capacity=10000`, double on overflow
   - `__init__(min_support)`: create root node (index 0)
   - `build(transactions, support_counts)`: sort items by frequency desc, insert each
   - `_insert_transaction(items, count)`: traverse/extend tree, update counts
   - `_find_child(node_idx, item_id)`: scan children via first_child/next_sibling
   - `get_header_table()`: return dict[item_id -> first_node_idx]
   - `get_prefix_path(node_idx)`: walk parents to root, collect items+counts
   - `get_conditional_pattern_base(item_id)`: collect all prefix paths for item
   - Use `@numba.njit` for `_insert_transaction` and `_find_child`

5. **Implement CRTree (hash-indexed)**
   - `Rule` dataclass: antecedent (frozenset), consequent (int), support, confidence, chi_sq, weight
   - `insert(rule)`: add to hash index and class index
   - `lookup(items)`: find all rules whose antecedent is subset of items
   - `prune(rule_id)`: remove rule from all indices
   - `get_rules_for_class(class_label)`: return rules from class index
   - For lookup: iterate candidate rules, use frozenset.issubset() check
   - Bitmap-based antecedent representation for fast subset testing (Phase 4 optimization)

6. **Write unit tests**
   - Test FPTree: build from known transactions, verify counts, prefix paths
   - Test CRTree: insert/lookup/prune rules, verify correctness
   - Test BitVectorDB: encode, support count, subset mask
   - Test DictEncoder: roundtrip encoding

## Todo List
- [ ] Create project skeleton (pyproject.toml, src/cmar/, tests/)
- [ ] Implement DictEncoder
- [ ] Implement BitVectorDB with NumPy bit matrix
- [ ] Implement array-based FPTree with Numba JIT
- [ ] Implement hash-indexed CRTree
- [ ] Write unit tests for all data structures
- [ ] Profile memory usage vs targets

## Success Criteria
- All unit tests pass
- FPTree builds 100K transactions in <2s
- BitVectorDB uses <25% memory vs integer arrays
- CRTree lookup returns correct rules for arbitrary item subsets

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| Numba JIT compilation of array-based tree | May not support all NumPy ops | Fallback to pure NumPy; keep Numba-compatible subset small |
| Array reallocation overhead | Slow inserts if capacity underestimated | Double capacity strategy; profile initial sizes on target datasets |
| Frozenset hashing cost for large antecedents | Slow CRTree insert/lookup | Bounded antecedent length (max 10 items); bitmap fallback |

## Security Considerations
- No external data ingestion in this phase; data structures are internal
- Input validation: reject negative support counts, empty transactions
- No file I/O except test fixtures

## Next Steps
- Phase 02 depends on FPTree and BitVectorDB
- Phase 03 depends on CRTree and Rule dataclass
- Profile data structures on UCI datasets before proceeding
