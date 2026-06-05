# Cải tiến thuật toán CMAR

Cài đặt + cải tiến thuật toán **CMAR** (Classification based on Multiple Association Rules — Li, Han, Pei, IEEE ICDM 2001) cho phân lớp dữ liệu bằng luật kết hợp.

## Kết quả chính (verified live, 26 UCI datasets THẬT, 10-fold CV seed=42)

| Chỉ số | Paper CMAR 2001 | Cải tiến | Δ |
|---|---:|---:|---:|
| Accuracy | 85.22% | **85.47%** | +0.25% |
| F1 macro | 80.67% | **82.84%** | **+2.17%** |
| Recall macro | 80.94% | **83.48%** | **+2.54%** |

→ Gain chính ở **F1 và Recall** (xử lý mất cân bằng lớp). Xếp **hạng 2/6** so với 5 baseline (Friedman test, p<0.05).

## 5 cải tiến (so với paper)

1. **Stratified Coverage Pruning** — bảo vệ luật lớp ít mẫu
2. **Cost-Sensitive Voting** — boost điểm lớp thiểu số (adaptive trigger)
3. **Bagging T=10** (full features) — giảm variance
4. **Adaptive MinSup** (sqrt) — hạ ngưỡng cho lớp hiếm
5. **MinSup Scale 0.3** — khai phá nhiều luật cho ensemble

## Tài liệu

| File | Nội dung |
|---|---|
| [BAO-CAO.md](BAO-CAO.md) | **Báo cáo kỹ thuật đầy đủ** (con số canonical) |
| [CAI-TIEN-MOI.md](CAI-TIEN-MOI.md) | Giải thích 5 cải tiến (dễ hiểu) |
| [TRINH-BAY-CO.md](TRINH-BAY-CO.md) | Bản trình bày ngắn |
| [plans/](plans/) | Kế hoạch cải tiến mạnh hơn (Fuzzy CMAR) cho paper quốc tế |

## Chạy benchmark

```bash
# Compile
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java \
    src/cmar/benchmark/*.java src/cmar/boost/*.java

# Chạy (máy ít RAM dùng -Xmx950m -XX:+UseSerialGC)
java -Xmx950m -XX:+UseSerialGC -cp bin cmar.boost.BoostedBenchmarkRunner \
    --method=bagging --T=10 --featureSubset=1.0 \
    --stratified=10 --costSensitive \
    --adaptMinSup --adaptFormula=sqrt --minSupScale=0.3
```

## Cấu trúc

```
src/cmar/          # Core: CMARClassifier, RulePruner, FPGrowthOptimized, MDLDiscretizer
src/cmar/boost/    # Ensemble: Bagging, Boosted, EnsembleUtils
src/cmar/stats/    # Friedman/Nemenyi test, baseline comparison
src/cmar/benchmark/# DataLoader (per-fold MDL, no leak), UCIDatasets, BenchmarkRunner
datasets/          # 26 bộ UCI THẬT (.csv)
```

## Cam kết trung thực

- 26/26 datasets THẬT từ `datasets/*.csv` — không có dữ liệu giả.
- Per-fold MDL discretization — không rò rỉ test sang train.
- Seed=42 cố định — tái lập được.
- Báo cáo cả 7 dataset thua paper (continuous medical: Diabetes, Heart, Pima...).
