# Phase 01 — Baseline Measurement Infrastructure

## Context
- Parent: [plan.md](./plan.md)
- Refs: [scout-01-current-implementation.md](./scout/scout-01-current-implementation.md)

## Overview
- **Date:** 2026-04-23
- **Description:** Thêm profiling (time per phase + peak memory) vào BenchmarkRunner. Chạy toàn bộ 26 UCI datasets lấy baseline metrics làm tham chiếu so sánh cho các phase sau.
- **Priority:** P0 (mandatory)
- **Effort:** 3-5 ngày
- **Impl Status:** Not started
- **Review Status:** Pending

## Key Insights
- Scout summary hiện có accuracy, train time, rules mined/pruned nhưng KHÔNG có per-phase breakdown (Mining / Pruning / Classification) và KHÔNG có peak memory.
- Không có số liệu gốc chi tiết ⇒ không chứng minh được improvement.
- Paper CMAR comparison có sẵn (+3.6% / -2.4% dao động) nhưng đó là accuracy, không phải perf.

## Requirements
- Đo per-phase wall-clock: `fitMining`, `rulePruning` (3 sub-phase: chi², g2s, coverage), `crTreeIndexing`, `predictTotal`, `predictAvgPerInstance`.
- Đo peak heap memory qua `ManagementFactory.getMemoryMXBean()` + `System.gc()` trước snapshot.
- Đo rule-count: beforePrune, afterChiSquare, afterG2S, afterCoverage.
- Chạy 3 lần/dataset, lấy median. Warm-up 1 run không tính.
- Output: `baseline-metrics.md` + CSV `baseline-metrics.csv` đặt trong `reports/`.
- Không thay đổi thuật toán. Chỉ thêm instrumentation.

## Architecture
- Tạo `PhaseTimer` utility class (ThreadLocal Map<String,Long>, start/stop/report).
- Tạo `MemorySampler` class (MemoryMXBean wrapper, sample peak qua NotificationListener hoặc manual sampling).
- Inject timer calls vào: `CMARClassifier.fit()`, `FPGrowth.mine()`, `RulePruner.prune()` (3 sub-sections), `CRTree.build()`, `CMARClassifier.predict()`.
- BenchmarkRunner: aggregate qua multiple runs, emit markdown + CSV.

## Related code files
- `CMARClassifier.java:1-205` — pipeline chính, inject 5 timer hooks
- `FPGrowth.java:42,71-76,105-155` — mining hot loop
- `RulePruner.java:54-94` (chi²), `:101-133` (g2s), `:143-179` (coverage), `:204-215` (bitmap rebuild)
- `CRTree.java:1-95` — indexing + predict lookup
- `BenchmarkRunner.java` (file chưa xác định path cụ thể — confirm đầu phase)

## Implementation Steps
1. Tạo `util/PhaseTimer.java` và `util/MemorySampler.java`.
2. Inject timer start/stop ở 5 điểm chính (CMARClassifier + RulePruner sub-phases).
3. Thêm `--runs=N --warmup=M` flag vào BenchmarkRunner.
4. Thêm memory sampling thread (100ms interval) trong lúc chạy 1 dataset; record peak.
5. Emit `reports/baseline-metrics.md` + `.csv` (1 hàng/dataset, cột: name, N, attrs, trainMs, mineMs, chiSqMs, g2sMs, covMs, indexMs, predictMs, peakMB, rulesRaw, rulesPruned, accuracy).
6. Chạy full suite 26 datasets. Commit baseline artifact.
7. (Decision) Thống nhất giữ `maxCoverageCount=4` HAY align với paper `delta=3`. Document quyết định ở đầu baseline report.

## Todo
- [ ] PhaseTimer + MemorySampler utility
- [ ] Inject timer hooks (5 vị trí)
- [ ] Multi-run averaging logic + warm-up
- [ ] Memory peak sampler thread
- [ ] Emit MD + CSV
- [ ] Run 26 datasets x 3 runs
- [ ] Quyết định param `maxCoverageCount`
- [ ] Commit `baseline-metrics.md` + `baseline-metrics.csv`

## Success Criteria
- CSV có đủ 26 rows, mỗi row có đủ 14 cột, không NaN.
- Accuracy mỗi dataset khớp ±0.5% với summary-report.md hiện tại (confirm instrumentation không làm sai kết quả).
- Peak memory có variance < 10% giữa 3 runs (sampling ổn định).
- Per-phase time cộng lại xấp xỉ total train time (delta < 5%).

## Risk Assessment
- **Sampling 100ms có thể miss transient spike** → tăng lên 20ms nếu cần; chấp nhận overhead ~1-2%.
- **GC bias ảnh hưởng wall time** → force `System.gc()` giữa các runs; dùng `-XX:+UseG1GC` cố định.
- **Dataset nhỏ (Iris, Glass) có train time ~0ms** → dùng `System.nanoTime()` thay `currentTimeMillis()`.

## Security Considerations
N/A — purely measurement infra, không đụng input/output.

## Next steps
→ Phase 04 (pruning optimization, biggest ROI per scout).
