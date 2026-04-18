# Phase 05: API & Benchmarks

## Context
- **Parent:** [plan.md](plan.md)
- **Dependencies:** [Phase 04](phase-04-classification-engine.md) (CMARClassifier)
- **Research:** [researcher-01](research/researcher-01-report.md), [researcher-02](research/researcher-02-report.md) sec 7

## Overview
- **Date:** 2026-03-12
- **Description:** Clean public API, comprehensive benchmarking suite, comparison against existing implementations and scikit-learn baselines
- **Priority:** Medium
- **Implementation Status:** pending
- **Review Status:** pending

## Key Insights
- Scikit-learn API compatibility (fit/predict/score) enables drop-in comparison with other classifiers
- UCI benchmark datasets are standard for associative classifier comparison
- Memory profiling (tracemalloc) catches regressions early
- Cross-validation essential for honest accuracy comparison

## Requirements
**Functional:**
- Clean `__init__.py` exporting CMARClassifier and key utilities
- Benchmarking scripts: accuracy, speed, memory across multiple datasets
- Comparison against: DecisionTreeClassifier, RandomForest, existing CMAR (if available)
- Hyperparameter tuning utility (grid search over min_support, min_confidence, chi_sq_threshold)
- Data loading utilities for UCI datasets

**Non-functional:**
- API should require <5 lines for basic usage
- Benchmark suite runs in <10 minutes total
- Results output as CSV + console summary

## Architecture
```
src/cmar/
  __init__.py          # CMARClassifier, version
  data_structures.py   # Internal
  fp_growth.py         # Internal
  rule_manager.py      # Internal
  classifier.py        # Internal
  utils.py             # DictEncoder (public), helpers

benchmarks/
  bench_mining.py      # FP-growth speed/memory
  bench_classification.py  # Prediction speed
  bench_comparison.py  # vs other classifiers
  datasets.py          # UCI data loading
  results/             # CSV output directory
```

## Related Code Files
| File | Action | Description |
|------|--------|-------------|
| `src/cmar/__init__.py` | modify | Public API exports, version, docstring |
| `src/cmar/utils.py` | modify | Add data loading, validation helpers |
| `benchmarks/datasets.py` | create | UCI dataset loading (via sklearn.datasets + fetch) |
| `benchmarks/bench_mining.py` | create | FP-growth benchmarks |
| `benchmarks/bench_classification.py` | create | Classification benchmarks |
| `benchmarks/bench_comparison.py` | create | Multi-classifier comparison |
| `tests/test_integration.py` | create | End-to-end tests |
| `pyproject.toml` | modify | Add benchmark deps, entry points |

## Implementation Steps

1. **Finalize public API in __init__.py**
   ```python
   from cmar.classifier import CMARClassifier
   from cmar.utils import DictEncoder
   __version__ = "0.1.0"
   __all__ = ["CMARClassifier", "DictEncoder"]
   ```
   - Add module-level docstring with usage example
   - Ensure CMARClassifier.__init__ has sensible defaults

2. **Create dataset loading utilities**
   - `benchmarks/datasets.py`:
     - `load_uci(name)`: load iris, mushroom, adult, nursery via sklearn or UCI ML repo
     - Discretize continuous features (equal-width binning, configurable n_bins=5)
     - Return (X_encoded, y, feature_names)
   - Use sklearn.datasets where available; fetch_openml for others

3. **Create mining benchmarks**
   - `benchmarks/bench_mining.py`:
     - Vary dataset size (1K, 10K, 100K, 500K transactions)
     - Measure: wall time, peak memory (tracemalloc), rule count
     - Vary min_support (0.01, 0.05, 0.1, 0.2)
     - Output CSV: dataset, n_transactions, min_support, time_s, memory_mb, n_rules

4. **Create classification benchmarks**
   - `benchmarks/bench_classification.py`:
     - Measure per-instance latency (median, p95, p99)
     - Measure batch throughput (instances/sec)
     - Vary: dataset size, number of rules, lazy_k
     - Output CSV: dataset, n_instances, n_rules, latency_median_ms, throughput_ips

5. **Create comparison benchmarks**
   - `benchmarks/bench_comparison.py`:
     - Classifiers: CMARClassifier, DecisionTree, RandomForest, GaussianNB
     - Metrics: accuracy, F1-macro, fit_time, predict_time, memory
     - 5-fold stratified cross-validation
     - Output: comparison table (CSV + formatted console print)
     - Datasets: iris, mushroom, adult, nursery

6. **Write integration tests**
   - `tests/test_integration.py`:
     - End-to-end: raw data -> fit -> predict -> score
     - Test on iris: accuracy > 0.90
     - Test on mushroom: accuracy > 0.95
     - Test serialization: pickle CMARClassifier, reload, same predictions
     - Test with missing values, unseen categories
     - Test reproducibility: same data -> same results

7. **Add hyperparameter tuning utility**
   - Simple grid search in `src/cmar/utils.py`:
     - `grid_search(X, y, param_grid, cv=5, scoring='accuracy')`
     - param_grid: dict of lists (min_support, min_confidence, chi_sq_threshold, weighting)
     - Return best params and CV scores
   - Keep simple; no need to replicate sklearn.GridSearchCV

8. **Add documentation strings**
   - All public classes and methods get docstrings
   - Include parameter descriptions, return types, examples
   - Keep concise; no separate docs files needed for v0.1

9. **Create run script**
   - `benchmarks/run_all.py`: orchestrate all benchmarks, save results to `benchmarks/results/`
   - Print summary table to console
   - Exit code 1 if any accuracy below threshold (regression detection)

## Todo List
- [ ] Finalize __init__.py public API
- [ ] Create dataset loading utilities
- [ ] Create FP-growth mining benchmarks
- [ ] Create classification benchmarks
- [ ] Create multi-classifier comparison benchmarks
- [ ] Write end-to-end integration tests
- [ ] Add grid search utility
- [ ] Add docstrings to all public interfaces
- [ ] Create benchmark runner script
- [ ] Run full benchmark suite, verify targets met

## Success Criteria
- `CMARClassifier` usable in 3 lines: init, fit, predict
- Integration tests pass on iris (>0.90 acc), mushroom (>0.95 acc)
- Benchmark suite completes in <10 minutes
- CMARClassifier competitive with DecisionTree on all test datasets
- Pickle serialization roundtrip produces identical predictions
- Memory usage within 500MB for 1M instance datasets

## Risk Assessment
| Risk | Impact | Mitigation |
|------|--------|------------|
| UCI datasets not available offline | Benchmarks fail without internet | Cache downloaded data; include small test fixtures |
| Discretization affects accuracy | Unfair comparison with continuous-capable classifiers | Document limitation; test multiple bin counts |
| sklearn API incompatibilities | Can't use sklearn cross_val_score | Implement minimal compatible interface; test with sklearn utilities |

## Security Considerations
- Dataset downloads: use HTTPS only, verify checksums if available
- No credentials or API keys in benchmark scripts
- Benchmark results are not sensitive but keep output local

## Next Steps
- After benchmarks: identify bottlenecks for further optimization
- Consider: GPU acceleration (CuPy), parallel mining, ensemble CMAR
- Publish to PyPI if results are competitive

## Unresolved Questions
- Optimal discretization strategy for continuous features (equal-width vs equal-frequency vs MDLP)?
- Include comparison against R or Java CMAR implementations?
- Target PyPI packaging in v0.1 or defer?
