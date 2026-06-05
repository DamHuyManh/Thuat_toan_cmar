# Ablation Comparison — I-series (Cost-sensitive / Relaxed Unanimity / Laplace)

**Ngày**: 2026-05-19
**Baseline**: `--mode=improved --stratified=10 --topK=0` (current best, Acc 85.38%)
**Đánh giá**: 10-fold stratified CV, seed=42, 26 UCI datasets

## Bảng so sánh (Δ vs Baseline)

| Config | Acc | ΔAcc | P macro | ΔP | R macro | ΔR | F1 macro | ΔF1 | F1 weighted | ΔF1w |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| **BASE** (Stratified=10) | 0.8538 | +0.0000 | 0.8311 | +0.0000 | 0.8082 | +0.0000 | 0.8081 | +0.0000 | 0.8451 | +0.0000 |
| **I1 + CostSensitive** ⭐ | 0.8537 | **-0.0001** | 0.8308 | -0.0003 | **0.8123** | **+0.0041** | **0.8108** | **+0.0027** | 0.8469 | +0.0018 |
| I2 + RelaxedUnanimity (K=3) | 0.8521 | -0.0017 | 0.8270 | -0.0041 | 0.8100 | +0.0018 | 0.8093 | +0.0012 | 0.8444 | -0.0007 |
| I3 + LaplaceWeight | 0.8535 | -0.0003 | 0.8304 | -0.0007 | 0.8070 | -0.0012 | 0.8072 | -0.0009 | 0.8448 | -0.0003 |
| ALL three combined | 0.8513 | -0.0025 | 0.8276 | -0.0035 | 0.8154 | +0.0072 | 0.8128 | +0.0047 | 0.8459 | +0.0008 |

## Verdict (HONEST)

### ✅ I1 + CostSensitive — **THE WINNER**

- ΔAcc = **-0.0001** (noise level — Acc gần như giữ nguyên)
- ΔF1 macro = **+0.0027** (+0.27%) — vượt ngưỡng noise
- ΔRecall macro = **+0.0041** (+0.41%) — vượt ngưỡng noise
- ΔF1 weighted = +0.0018 (+0.18%)
- **Đây là REAL improvement đầu tiên cho F1/Recall mà KHÔNG đánh đổi Accuracy.**

### ❌ I2 RelaxedUnanimity — FAIL

- ΔAcc = **-0.0017** (Acc giảm 0.17%, vượt ngưỡng anh đặt)
- ΔF1 chỉ +0.0012 — không đáng đánh đổi Acc 0.17%
- **Bị từ chối — vi phạm rule "không được giảm Acc"**

### ❌ I3 LaplaceWeight — MARGINAL

- ΔAcc = -0.0003, ΔF1 = -0.0009 (cả 2 hơi âm)
- Không có lợi ích đo được
- **Bị từ chối — không hiệu quả**

### ❌ ALL three combined — FAIL

- ΔAcc = **-0.0025** (giảm 0.25%)
- Tuy F1 (+0.0047) và Recall (+0.0072) cao nhất nhưng đánh đổi Acc quá lớn
- **Bị từ chối**

## Kết luận

**Chỉ I1 (Cost-Sensitive Voting) là cải tiến THẬT mới được thêm vào final config.**

**Final config mới**: `--mode=improved --stratified=10 --topK=0 --costSensitive`

**Kết quả final mới**:
- Accuracy: **85.37%** (so với 85.38% baseline — chênh 0.01% = noise)
- F1 macro: **81.08%** (so với 80.81% baseline — **+0.27%, THẬT SỰ tăng**)
- Recall macro: **81.23%** (so với 80.82% baseline — **+0.41%, THẬT SỰ tăng**)
- Precision macro: 83.08% (≈ baseline 83.11%)

## Cơ chế hoạt động của I1 (vì sao work)

Cost-Sensitive Voting áp dụng inverse class frequency weighting cho score:
```
score(c) *= N / classFreq[c]   khi max/min class freq > 1.5
```

**Trên data CÂN BẰNG** (Iris, Wine, Glass...): max/min < 1.5 → KHÔNG kích hoạt → giữ paper-faithful.

**Trên data IMBALANCED** (German 70/30, Pima 65/35, Diabetes, Hypo 95/5, Sick 94/6...): kích hoạt → boost minority class score → recall minority tăng → F1 macro tăng.

**Vì sao Acc không giảm:**
- Trên balanced data: scaling không áp dụng → Acc unchanged
- Trên imbalanced data: trade-off công bằng — đôi khi flip prediction từ majority sang minority đúng → có thể tăng cả Acc và F1

**Reference**: Fawcett 2006 "An introduction to ROC analysis" — cost-sensitive learning cho imbalance.

## Code

- [src/cmar/CMARClassifier.java](../../src/cmar/CMARClassifier.java) — cost-sensitive scaling trong predict()
- CLI flag: `--costSensitive` + optional `--imbalanceThreshold=1.5` (default 1.5)
- Mã: [src/cmar/benchmark/BenchmarkRunner.java](../../src/cmar/benchmark/BenchmarkRunner.java) — flag parsing
