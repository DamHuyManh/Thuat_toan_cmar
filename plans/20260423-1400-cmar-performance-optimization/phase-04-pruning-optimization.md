# Phase 04 — Pruning Optimization

## Context
- Parent: [plan.md](./plan.md)
- Refs: [researcher-02-pruning-mining-optimization.md](./research/researcher-02-pruning-mining-optimization.md), [scout-01-current-implementation.md](./scout/scout-01-current-implementation.md)

## Overview
- **Date:** 2026-04-23
- **Description:** Tấn công 3 bottleneck pruning lớn nhất: (1) bitmap rebuild trùng lặp, (2) rule-txn match matrix tái sử dụng, (3) bitmap-based G2S subset check thay O(n²) linear.
- **Priority:** P1 (highest ROI)
- **Effort:** 7-10 ngày
- **Impl Status:** Not started
- **Review Status:** Pending

## Key Insights
- Scout: `RulePruner:204-215` rebuild bitmaps đã tồn tại từ mining — pure waste.
- Scout: `RulePruner:54-90` chi² scan O(rules × N × items_per_rule), Anneal = 156K × 898 ≈ 140M ops. Cùng scan lặp lại trong coverage.
- Scout: G2S `RulePruner:114-128` O(n²), skip khi rules > 10K ⇒ German 89K bị bỏ qua, chất lượng rule set thấp hơn.
- Research-02 rec #3: trie-based dedup 1.5-2x nhanh + 35% memory giảm.
- Research-02 rec #2: incremental chi² upper bound sớm loại 30-50% pattern.

## Requirements
- Share bitmaps giữa mining và pruning (pass reference, không clone).
- Pre-compute `BitSet[] ruleMatches` (matches[ruleId] = txns-matched) 1 lần, reuse cho cả 3 pruning phases.
- Chuyển G2S từ linear subset check sang bitmap AND hoặc antecedent-trie dedup; bỏ ngưỡng skip >10K.
- (Optional stretch) incremental chi² upper bound trong mining loop để prune sớm.
- Accuracy không giảm quá 0.5% so với baseline phase-01.

## Architecture
- Thêm `MiningContext` object chứa `long[][] bitmaps`, `int[] itemSupport`, `int N` — truyền từ FPGrowth sang RulePruner.
- `BitSet[] ruleTxnMatches` build ngay sau mining, trước chi² phase.
- Chi² phase: lookup matches[ruleId] → cardinality, không scan N lần nữa.
- G2S phase: encode antecedent thành `BitSet itemMask[ruleId]`; subset test = `(a AND b) == a`. Optional: build antecedent trie; chỉ giữ frontier rules.
- Coverage phase: dùng matches[ruleId] đã có; iterate sorted rules, track covered txns qua shared BitSet.

## Related code files
- `RulePruner.java:54-94` — chi² phase (refactor)
- `RulePruner.java:101-133` — G2S phase (rewrite)
- `RulePruner.java:143-179` — coverage phase (adapt)
- `RulePruner.java:204-215` — bitmap rebuild (delete, pass from mining)
- `FPGrowth.java:42` — expose bitmaps qua getter/return value
- `CMARClassifier.java:~40-80` — wire MiningContext qua pipeline
- `Rule.java:56-64` — bitmap match helper (reuse)

## Implementation Steps
1. Định nghĩa `MiningContext` class; refactor `FPGrowth.mine()` trả về context thay vì chỉ itemsets.
2. `CMARClassifier.fit()` pass context xuống `RulePruner.prune(rules, context)`.
3. Xóa bitmap rebuild trong RulePruner:204-215.
4. Build `BitSet[] ruleTxnMatches` ngay sau mining, trong RulePruner constructor.
5. Refactor chi² phase: `support = ruleTxnMatches[i].cardinality()` thay O(N) scan.
6. Refactor G2S: build `BitSet[] antecedentMasks`; 2-pointer sorted-by-length loop với bitmap AND; bỏ ngưỡng 10K.
7. Refactor coverage: reuse ruleTxnMatches, không rescan txns.
8. (Optional) antecedent-trie dedup thay G2S bitmap nếu memory căng.
9. (Optional stretch) incremental chi² upper bound trong FPGrowth mining loop.
10. Validate accuracy trên 26 datasets vs Phase 01 baseline.
11. Đo per-phase time + peak memory, emit `reports/phase-04-metrics.md`.

## Todo
- [ ] MiningContext class
- [ ] FPGrowth return context
- [ ] Xóa bitmap rebuild
- [ ] ruleTxnMatches pre-compute
- [ ] Refactor chi² using cardinality
- [ ] Refactor G2S with BitSet AND, bỏ skip threshold
- [ ] Refactor coverage reuse matches
- [ ] (Optional) antecedent trie dedup
- [ ] (Optional) incremental chi² upper bound
- [ ] Accuracy validation 26 datasets
- [ ] Metrics report

## Success Criteria
- Pruning phase time: >= 3x speedup trên Anneal, Sonar, German, Waveform (datasets có >50K raw rules).
- Peak memory pruning phase: >= 20% reduction (do không double bitmap).
- G2S chạy được trên German (89K rules) thay vì skip.
- Accuracy delta |<= 0.5%| cho từng dataset vs Phase 01.
- Chi² phase: ops count giảm >= 50% (đo qua counter hoặc JFR).

## Risk Assessment
- **BitSet cho antecedentMasks tốn memory nếu max-item lớn** → cap bitmap width = maxItem observed; dùng RoaringBitmap nếu sparse.
- **G2S bật lại có thể làm rule set quá thưa** → A/B test accuracy; nếu giảm >1% giữ rule thêm qua confidence tiebreak.
- **Incremental chi² bound (stretch) có thể cắt nhầm** → validate bằng full-search parity test trên Iris/Glass trước khi bật.
- **Shared mutable BitSet giữa phase** → document ownership; clone defensive ở boundary nếu cần.

## Security Considerations
N/A — internal data structures, không I/O user input.

## Next steps
→ Phase 03 (Mining Optimization) nếu mining vẫn chiếm >40% total time theo Phase 04 metrics.
