# CMAR Performance Optimization — Master Plan

**Date:** 2026-04-23
**Author:** 22dh112022@gmail.com
**Goal:** Improve runtime + memory của CMAR Java impl (3738 LOC), giữ accuracy >= 85.1% baseline. Deliverables: demo (baseline vs improved), comparison report, thesis chapter.
**Timeline:** 4-6 weeks
**Language:** Vietnamese + English mix (thesis terms VN, code EN)

## Input Research
- `research/researcher-01-rule-storage-structures.md` — CR-tree variants, Radix Trie, Roaring Bitmap, Inverted Index
- `research/researcher-02-pruning-mining-optimization.md` — class-partitioned mining, chi² early term, trie dedup
- `scout/scout-01-current-implementation.md` — code bottlenecks w/ file:line refs

## Phases

| # | Phase | Priority | Effort | Status | Review | File |
|---|-------|----------|--------|--------|--------|------|
| 01 | Baseline Measurement Infrastructure | P0 mandatory | 3-5d | Not started | Pending | [phase-01-baseline-measurement.md](./phase-01-baseline-measurement.md) |
| 02 | Rule Storage Optimization (CR-tree) | P3 | 5-7d | Not started | Pending | [phase-02-rule-storage.md](./phase-02-rule-storage.md) |
| 03 | Mining Optimization | P2 | 7-10d | Not started | Pending | [phase-03-mining-optimization.md](./phase-03-mining-optimization.md) |
| 04 | Pruning Optimization (biggest ROI) | P1 | 7-10d | Not started | Pending | [phase-04-pruning-optimization.md](./phase-04-pruning-optimization.md) |
| 05 | Demo + Comparative Benchmark + Thesis | P0 mandatory | 5-7d | Not started | Pending | [phase-05-demo-benchmark-thesis.md](./phase-05-demo-benchmark-thesis.md) |

## Execution Order
**Recommended:** 01 → 04 → 03 → 02 → 05
Rationale: Phase 01 bắt buộc (cần số liệu gốc). Phase 04 ROI cao nhất (scout chỉ ra O(rules×N) chi² scan + bitmap rebuild là bottleneck lớn nhất). Phase 03 thứ 2 vì ảnh hưởng mining. Phase 02 lợi ích nhỏ hơn (predict latency thường không phải bottleneck trên UCI). Phase 05 bắt buộc cuối cùng.

Each phase independently shippable — student có thể dừng bất kỳ phase nào và vẫn có working improvement + thesis.

## Overall Success Criteria
- Accuracy >= 85.1% baseline (per dataset ±1%)
- End-to-end runtime: >= 2x speedup trung bình trên 26 UCI datasets
- Peak memory: >= 25% reduction trên datasets lớn (Waveform, Sonar, German, Anneal)
- Demo runnable với flag `--mode=baseline|improved`
- Thesis chapter: problem analysis + solutions + evaluation tables/charts

## Guiding Principles
- **YAGNI:** Không implement GPU/compressed suffix array/parallel FP (scout confirm không cần cho UCI scale)
- **KISS:** Prefer HashMap tweak over radix trie nếu cùng lợi ích
- **DRY:** Share bitmap + rule-txn match matrix across mining + pruning phases
- **Reversible:** Improved code gates behind feature flag, baseline path preserved

## Unresolved Questions
1. General-to-specific pruning đang skip khi >10K rules (scout Q) — bật lại ảnh hưởng accuracy? → Phase 04 phải đo.
2. `maxCoverageCount=4` vs paper `delta=3` — giữ nguyên hay align với paper trước khi benchmark? → Quyết định đầu Phase 01.
3. Class imbalance trên 26 UCI datasets — class-partitioned mining effective bao nhiêu dataset? → Phase 03 xác định empirically.
4. Student có quad-core? → Quyết định có thử parallel FP-growth (optional stretch) không.
5. Output format biểu đồ trong thesis: markdown table + ASCII đủ hay cần matplotlib export? → Chốt ở Phase 05.
