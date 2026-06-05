# Scout Report — Codebase hiện trạng CMAR

## Cấu trúc code (Java, no external deps)

| File | Vai trò | Trạng thái |
|---|---|---|
| `src/cmar/CMARClassifier.java` | Classifier chính: voting, cost-sensitive | Có 5 static flag (lift weights kept for ablation) |
| `src/cmar/RulePruner.java` | χ² + G2S (bitmap) + Stratified Coverage | Stratified topN flag |
| `src/cmar/Rule.java` | Rule object + compareTo (sort) | useHMLift/useLiftSort kept (ablation) |
| `src/cmar/FPGrowthOptimized.java` | FP-Growth class-aware, song song | Core mining — chỗ thêm Fuzzy/weighted |
| `src/cmar/MDLDiscretizer.java` | Rời rạc hoá MDL (Fayyad-Irani) | **ĐIỂM YẾU** — thay bằng Fuzzy/CAIM |
| `src/cmar/benchmark/DataLoader.java` | Load CSV, encode per-fold (no leak) | numBins: 0=MDL, >0=quantile |
| `src/cmar/benchmark/UCIDatasets.java` | 26 dataset metadata + paperMinSup/Conf | Có synthetic fallback code (DEAD — CSV đầy đủ) |
| `src/cmar/boost/BaggingCMARClassifier.java` | Bagging T=10 + OOB weight | Winning ensemble |
| `src/cmar/boost/BoostedBenchmarkRunner.java` | Entry point benchmark | ~15 CLI flags |
| `src/cmar/boost/EnsembleUtils.java` | DRY helpers | |
| `src/cmar/stats/ModernBaselines.java` | Số baseline 6 method | ⚠️ **ECBA-EX numbers CHƯA VERIFY** |
| `src/cmar/stats/FriedmanNemenyi.java` | Friedman + Nemenyi test | OK |

## Kết quả hiện tại (verified live, topK=0 honest config)

- Accuracy **85.47%** (+0.25% vs paper 85.22%)
- F1 macro **82.84%** (+2.17% vs 80.67%)
- Recall macro **83.48%** (+2.54% vs 80.94%)
- Cấu hình: `bagging T=10, fs=1.0, stratified=10, costSensitive, adaptMinSup sqrt, minSupScale=0.3` (KHÔNG topK)

## 5 cải tiến THẬT hiện có
1. Stratified Coverage Pruning (pruning)
2. Cost-Sensitive Voting (voting) — chỉ ở voting, CHƯA ở rule generation
3. Bagging T=10 fs=1.0 (ensemble)
4. Adaptive MinSup sqrt (mining)
5. MinSup Scale 0.3 (mining)
(Top-K đã loại — không tăng F1/Recall, chỉ Acc noise)

## ĐIỂM YẾU rõ ràng (cơ hội cải tiến mạnh)

**7 dataset thua paper — TẤT CẢ là continuous medical hoặc tiny**:
| Dataset | Δ Acc | Loại | Nguyên nhân |
|---|---:|---|---|
| Diabetes | -2.10% | Continuous y tế | MDL rời rạc hoá kém ở biên |
| Heart | -1.83% | Continuous y tế | Tương tự |
| Pima | -1.40% | Continuous y tế | Tương tự |
| German | -1.70% | Mixed noisy | Nhãn nhiễu |
| Zoo | -1.49% | Tiny 7-class | Variance cao |
| Labor | -1.37% | Tiny 57 mẫu | Variance cao |

→ **5/7 dataset thua là do RỜI RẠC HOÁ continuous features kém**. Đây là chỗ Fuzzy CMAR / CAIM tấn công trực tiếp.

## Rủi ro & vấn đề cần xử lý

1. ⚠️ **ModernBaselines.java có số ECBA-EX chưa verify** (venue sai) → PHẢI sửa/xoá trước khi nộp paper (user yêu cầu không số ảo).
2. **Synthetic dataset code** trong UCIDatasets.java là dead code → nên xoá để chứng minh "không data ảo".
3. **OOM trên máy** (1.27GB RAM free) → benchmark cần `-Xmx950m -XX:+UseSerialGC`.
4. Per-dataset numbers hơi non-deterministic giữa các lần chạy (parallel mining tie-break) — average ổn định nhưng cần seed control chặt hơn cho paper reproducibility.

## Chỗ cắm cải tiến mới

- **Fuzzy membership**: sửa `MDLDiscretizer` → `FuzzyDiscretizer` (triangular sets) + `FPGrowthOptimized` (fuzzy support = Σ membership thay vì count).
- **CAIM discretization**: drop-in thay MDL trong `DataLoader.encodeFold`.
- **Cost-sensitive rule generation**: thêm cost vào `RulePruner` coverage pruning (giữ luật minority).
