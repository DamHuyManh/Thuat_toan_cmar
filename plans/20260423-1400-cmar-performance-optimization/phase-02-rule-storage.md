# Phase 02 — Rule Storage Optimization (CR-tree)

## Context
- Parent: [plan.md](./plan.md)
- Refs: [researcher-01-rule-storage-structures.md](./research/researcher-01-rule-storage-structures.md), [scout-01-current-implementation.md](./scout/scout-01-current-implementation.md)

## Overview
- **Date:** 2026-04-23
- **Description:** Giảm memory CR-tree + tăng tốc predict. Approach KISS: multi-item inverted index trước, (optional) Roaring Bitmap sau.
- **Priority:** P3 (lower ROI — predict không phải bottleneck lớn nhất)
- **Effort:** 5-7 ngày
- **Impl Status:** Not started
- **Review Status:** Pending

## Key Insights
- Scout: `CRTree:74-82` findAllMatching linear scan khi rules không cùng first-item — prediction bottleneck.
- Scout: nested HashMap `classLabel → firstItem → List<Rule>` hiện tại chỉ index theo first-item.
- Research-01 rec #1 (low effort): HashMap node-link thay linked list, 10-20% memory win.
- Research-01 rec #3 (higher effort): Roaring Bitmap chỉ justified khi >10K rules final — UCI thường <3K sau prune, vậy SKIP theo YAGNI.
- Radix Trie: research-01 đề xuất nhưng implementation 200-300 LOC, ROI vừa phải — chỉ làm nếu thời gian cho phép.

## Requirements
- Multi-item inverted index: `Map<item, List<Rule>>` để predict query theo bất kỳ item nào trong instance.
- Prediction: intersect candidate rule lists từ các item của instance → O(k·log n) thay O(rules).
- Peak memory CRTree: giảm >= 20% qua primitive int[] antecedent shared pool.
- Accuracy không đổi.

## Architecture
- `CRTree` expose thêm `Map<Integer, List<Rule>> itemIndex` (item → rules containing it).
- `predict(instance)`: for mỗi item trong instance, union candidate rules; dedup; rank theo weighted-χ² (logic cũ).
- `Rule.antecedent`: int[] (đã có), nhưng dedupe qua `IntArrayPool` nếu trùng antecedent giữa rules cùng class.
- (Optional) Radix-trie variant chỉ khi final rule set >5K và memory vẫn cao.

## Related code files
- `CRTree.java:1-95` — toàn file refactor
- `CRTree.java:42-68` — findMatchingRules (rewrite)
- `CRTree.java:74-82` — findAllMatching fallback (loại)
- `Rule.java:1-90` — antecedent storage + bitmap match
- `CMARClassifier.java:99` — predict entry point

## Implementation Steps
1. Thêm `itemIndex` vào CRTree, populate trong `build()`.
2. Rewrite `findMatchingRules(instance)` dùng itemIndex union.
3. Benchmark predict latency trước/sau.
4. (Optional) IntArrayPool cho antecedent dedup, measure memory.
5. (Optional) Radix-trie experiment chỉ khi datasets lớn vẫn nhiều rules final.
6. Accuracy validation 26 datasets.
7. Emit `reports/phase-02-metrics.md`.

## Todo
- [ ] Multi-item inverted index
- [ ] Refactor findMatchingRules
- [ ] Bỏ linear fallback
- [ ] (Optional) IntArrayPool dedup
- [ ] (Optional) Radix-trie variant
- [ ] Accuracy validation
- [ ] Metrics report

## Success Criteria
- Predict avg-per-instance: >= 1.5x speedup trên datasets >500 final rules.
- CRTree peak memory: >= 15% giảm.
- Accuracy không đổi (delta < 0.1%).
- Không còn linear-scan fallback path.

## Risk Assessment
- **Inverted index tăng memory cho sparse datasets nhỏ (Iris)** → cap: chỉ bật khi rules > 100.
- **Radix trie implementation phức tạp, ROI nhỏ cho UCI** → skip nếu thời gian eo hẹp (YAGNI).

## Security Considerations
N/A.

## Next steps
→ Phase 05 (Demo + Thesis).
