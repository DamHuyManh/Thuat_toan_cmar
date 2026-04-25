# Phase 05 — Demo, Comparative Benchmark, Thesis Chapter

## Context
- Parent: [plan.md](./plan.md)
- Refs: phases 01-04 metrics reports

## Overview
- **Date:** 2026-04-23
- **Description:** Gói đầu ra luận văn: demo chọn baseline/improved, bảng so sánh 26 UCI datasets, chapter viết sẵn.
- **Priority:** P0 (mandatory deliverable)
- **Effort:** 5-7 ngày
- **Impl Status:** Not started
- **Review Status:** Pending

## Key Insights
- Student task spec yêu cầu 3 deliverable cụ thể: demo, comparison report, thesis chapter.
- Gating behind flag cho phép demo side-by-side, cũng bảo vệ baseline cho reproducibility.
- Thesis cần problem analysis + proposed solution + evaluation — phases 01-04 đã tạo ra toàn bộ số liệu cần.

## Requirements
- CLI flag `--mode=baseline|improved` (default improved). Baseline = code path Phase 01, improved = code path Phase 02+03+04 kết hợp.
- Run cả 2 modes trên 26 UCI datasets, capture: train time, peak memory, predict time, accuracy, rules before/after prune.
- `reports/comparison.md` với bảng so sánh đầy đủ + tóm tắt speedup/memory reduction/accuracy delta.
- `reports/comparison.csv` cho tái xử lý.
- ASCII bar chart hoặc markdown table trong thesis; matplotlib PNG chỉ nếu dư thời gian.
- Thesis chapter (VN) có 4 mục: (1) Phân tích bài toán + bottleneck, (2) Giải pháp đề xuất, (3) Đánh giá thực nghiệm, (4) Kết luận.

## Architecture
- `CMARClassifier` nhận `OptimizationProfile` enum = {BASELINE, IMPROVED}; các hook (mining, pruning, storage) dispatch theo profile.
- BenchmarkRunner: loop modes × datasets × runs; emit combined CSV.
- Report generator: Java hoặc Python script đọc CSV → markdown table + tóm tắt.

## Related code files
- `CMARClassifier.java:1-205` — thêm profile param
- `RulePruner.java`, `FPGrowth.java`, `CRTree.java` — các điểm branch theo profile
- `BenchmarkRunner.java` — CLI flag + multi-mode loop
- `reports/baseline-metrics.md` (Phase 01), `phase-0X-metrics.md` (Phase 02-04) — input

## Implementation Steps
1. Thêm enum `OptimizationProfile`; gate toàn bộ improved paths behind switch.
2. Double-check baseline code path vẫn identical với pre-Phase-01 behavior (regression test 26 datasets).
3. CLI arg parsing; default improved.
4. Run comparison suite: 2 modes × 26 datasets × 3 runs = 156 runs. Commit raw CSV.
5. Summary generator: aggregate median, compute speedup/memory ratio per dataset + geomean.
6. Viết `thesis/chapter-performance-improvement.md` (VN):
   - 4.1 Phân tích hiện trạng + bottleneck (cite scout-01, dẫn file:line)
   - 4.2 Giải pháp: shared bitmaps, BitSet G2S, class-partitioned + K_max, inverted index (cite research-01, -02)
   - 4.3 Thực nghiệm: bảng 26 datasets, geomean, case study Anneal/Waveform/German
   - 4.4 Kết luận + hướng phát triển (GPU, Roaring cho rule sets >100K)
7. Review self-contained: student chạy `java -jar cmar.jar --mode=improved --dataset=all` cho reproduction.

## Todo
- [ ] OptimizationProfile enum + gates
- [ ] Baseline regression test
- [ ] CLI flag parsing
- [ ] Full suite run 2×26×3
- [ ] comparison.md + comparison.csv
- [ ] ASCII charts (hoặc matplotlib nếu dư)
- [ ] Thesis chapter draft
- [ ] Self-contained repro instructions (README update)

## Success Criteria
- Cả 2 modes chạy được từ 1 jar/command.
- Baseline accuracy trùng Phase 01 ±0.1%.
- Geomean end-to-end speedup >= 2x across 26 datasets.
- Geomean peak memory reduction >= 25% datasets lớn (N>1000 hoặc raw rules >50K).
- Thesis chapter >= 10 trang, có ít nhất 3 bảng + 2 chart.
- README có câu lệnh reproduction + phiên bản Java/flags.

## Risk Assessment
- **Improved mode regress trên dataset nào đó (accuracy tụt)** → thêm per-dataset override profile; document trade-off.
- **Time eo hẹp → thesis viết vội** → bắt đầu chapter song song với Phase 04 implementation.
- **Reproducibility fail do seed/GC khác máy** → pin `-Xmx`, fixed seed, document JDK version.

## Security Considerations
N/A — benchmark tool, no external input.

## Next steps
Hoàn tất dự án → nộp luận văn. Future work ghi trong thesis §4.4.
