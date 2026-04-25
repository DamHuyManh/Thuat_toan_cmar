# Phase 03 — Mining Optimization

## Context
- Parent: [plan.md](./plan.md)
- Refs: [researcher-02-pruning-mining-optimization.md](./research/researcher-02-pruning-mining-optimization.md), [scout-01-current-implementation.md](./scout/scout-01-current-implementation.md)

## Overview
- **Date:** 2026-04-23
- **Description:** Giảm search space FP-growth: class-partitioned mining (nếu chưa có), K_max per-class stopping heuristic, giảm clone overhead.
- **Priority:** P2
- **Effort:** 7-10 ngày
- **Impl Status:** Not started
- **Review Status:** Pending

## Key Insights
- Research-02 rec #1: class-partitioned FP-growth + K_max = 2-3x mining speedup, 30-40% memory, risk thấp.
- Scout says class-partitioning already exists — **cần verify** trong FPGrowth.java trước khi implement.
- Scout: `itemset.clone()` + `Arrays.sort()` tại FPGrowth:91 chạy 100K+ lần → ~10-20% micro win.
- Scout: 5M MAX_ITEMSETS cap (FPGrowth:15), thực tế 1-2M → có headroom cho K_max cap nhỏ hơn theo class.

## Requirements
- Verify class-partitioned mining: nếu đã có, skip step 1. Nếu chưa, refactor FPGrowth build FP-tree per class.
- K_max parameter: stop mining 1 class sau khi generate K_max rules (default 30, configurable).
- Loại clone thừa: pass immutable int[] sorted reference thay clone mỗi rule.
- Accuracy không giảm quá 0.5% vs Phase 01 baseline.

## Architecture
- `FPGrowth.mineByClass(int classLabel, int kMax)` — wrap existing mining logic với class filter + early stop.
- `CMARClassifier.fit()` loop classes, call `mineByClass` từng class, merge itemsets.
- `SortedItemset` wrapper (final int[], cached hash, pre-sorted) thay cho clone+sort mỗi lần.
- K_max check: counter trong mining recursion, throw/break khi đạt ngưỡng.

## Related code files
- `FPGrowth.java:15` — MAX_ITEMSETS (thêm MAX_RULES_PER_CLASS)
- `FPGrowth.java:42` — bitmaps (per-class subsets)
- `FPGrowth.java:71-76` — N-scan hot loop
- `FPGrowth.java:91` — itemset.clone (loại)
- `FPGrowth.java:105-155` — recursive mining (thêm kMax guard)
- `FPTree.java:11-12,24-34,63-97` — header table + conditional tree (per-class instantiation)
- `CMARClassifier.java:~30-60` — fit() orchestration
- `Rule.java:20` — Arrays.sort (loại nếu antecedent đã sorted)

## Implementation Steps
1. Grep FPGrowth/FPTree để confirm class-partitioned hay global mining.
2. Nếu global: refactor build FP-tree per class-label; filter transactions.
3. Thêm `kMax` param và counter, break recursion khi đạt.
4. `SortedItemset` class + loại clone tại FPGrowth:91 và Rule:20.
5. Thêm per-class mining metrics (rules generated, time) vào PhaseTimer.
6. Benchmark: K_max sweep {10, 20, 30, 50, 100, ∞} trên Iris, Waveform, Sonar để chọn default tốt.
7. Run 26 datasets; so sánh mining time + memory + accuracy vs Phase 01.
8. Emit `reports/phase-03-metrics.md`.

## Todo
- [ ] Verify current class-partitioning status
- [ ] (If needed) refactor per-class FP-tree
- [ ] K_max counter + recursion guard
- [ ] SortedItemset wrapper, loại clone
- [ ] K_max sweep experiment
- [ ] Accuracy validation 26 datasets
- [ ] Metrics report

## Success Criteria
- Mining phase: >= 2x speedup trung bình trên datasets >3 classes.
- Rules generated: giảm >= 30% ở skewed datasets (Auto, Led7, Sonar).
- Peak memory mining: >= 20% giảm.
- Accuracy delta |<= 0.5%| per dataset.
- K_max default chosen (justified bằng sweep data).

## Risk Assessment
- **K_max quá thấp → miss important rules → accuracy giảm** → empirical sweep bắt buộc.
- **Class-partitioning trên class cực imbalance (1% minority) → minority class thiếu rules** → giữ min-rules-per-class floor (e.g., 5).
- **Per-class FP-tree nhân đôi memory peak tạm thời** → xây tuần tự, release tree sau mỗi class.

## Security Considerations
N/A.

## Next steps
→ Phase 02 (Rule Storage) nếu predict latency vẫn cao, hoặc → Phase 05 (thesis + demo) nếu gains đã đủ.
