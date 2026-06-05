# Báo Cáo Chi Tiết Cải Tiến Hiệu Năng CMAR

**Ngày chạy:** 2026-04-27 (sau code cleanup M1-M5)
**Bài báo gốc:** Li, Han, Pei. "CMAR" (IEEE ICDM 2001)
**Đánh giá:** 10-fold stratified CV, seed=42, 26 UCI datasets

---

## 1. KẾT QUẢ TỔNG HỢP

| Chỉ số | Baseline | Improved | Cải thiện |
|--------|----------|----------|-----------|
| **Tổng thời gian train** | 23,546 ms | **4,624 ms** | **5.09× nhanh hơn** |
| **Thời gian mining** | 17,138 ms | 2,943 ms | **5.83× nhanh hơn** |
| **Thời gian pruning** | 6,428 ms | 1,700 ms | **3.78× nhanh hơn** |
| **Peak memory trung bình** | 88 MB | **76 MB** | **14% giảm** |
| **Accuracy trung bình** | 85.1% | 85.3% | Cải thiện nhẹ |

> **Đạt và vượt mục tiêu:** Plan đặt ra 2× speedup, thực tế đạt **5.09×**.

---

## 2. CHI TIẾT TỪNG DATASET

| # | Dataset | N | Paper | Cũ (Baseline) | Mới (Improved) | Time Cũ (ms) | Time Mới (ms) | **Speedup** |
|---|---------|---|-------|---------------|----------------|--------------|---------------|-------------|
| 1 | Anneal | 898 | 97.3% | 97.7% | **98.2%** | 3,637 | 712 | **5.11×** |
| 2 | Australian | 690 | 86.1% | 86.7% | **86.8%** | 215 | 35 | **6.14×** |
| 3 | Auto | 205 | 78.1% | 81.4% | 81.4% | 606 | 565 | 1.07× |
| 4 | Breast-Cancer | 683 | 96.4% | 97.1% | 97.1% | 23 | 4 | **5.75×** |
| 5 | Cleve | 303 | 82.2% | 82.6% | 82.6% | 80 | 12 | **6.67×** |
| 6 | Crx | 690 | 84.9% | 86.0% | **86.1%** | 261 | 34 | **7.68×** |
| 7 | Diabetes | 768 | 75.8% | 73.4% | 73.4% | 14 | 2 | **7.00×** |
| 8 | **German** | 1000 | 74.9% | 72.9% | 72.9% | 1,456 | 126 | **🚀 11.56×** |
| 9 | Glass | 214 | 70.1% | 70.0% | 70.0% | 6 | 2 | 3.00× |
| 10 | Heart | 270 | 82.2% | 80.7% | 80.7% | 72 | 11 | **6.55×** |
| 11 | Hepatitis | 155 | 80.5% | 83.3% | 83.3% | 110 | 46 | 2.39× |
| 12 | Horse | 368 | 82.6% | 80.7% | **82.3%** | 687 | 169 | 4.07× |
| 13 | **Hypo** | 3163 | 98.4% | 98.0% | 98.0% | 2,421 | 101 | **🚀 23.97×** |
| 14 | Iono | 351 | 91.5% | 92.0% | **92.6%** | 1,165 | 349 | 3.34× |
| 15 | Iris | 150 | 94.0% | 92.7% | 92.7% | 0 | 0 | — |
| 16 | Labor | 57 | 89.7% | 91.7% | 91.7% | 84 | 22 | 3.82× |
| 17 | Led7 | 3200 | 72.5% | 71.2% | 71.2% | 27 | 3 | **9.00×** |
| 18 | Lymphography | 148 | 83.1% | 83.4% | 83.4% | 177 | 88 | 2.01× |
| 19 | Pima | 768 | 75.1% | 73.4% | 73.4% | 15 | 2 | **7.50×** |
| 20 | **Sick** | 2800 | 97.5% | 96.5% | **96.8%** | 3,907 | 147 | **🚀 26.58×** |
| 21 | Sonar | 208 | 79.4% | 78.4% | **80.8%** | 2,868 | 1,184 | 2.42× |
| 22 | Tic-Tac-Toe | 958 | 99.2% | 99.2% | 99.2% | 62 | 9 | **6.89×** |
| 23 | Vehicle | 846 | 68.8% | 68.2% | 68.2% | 473 | 61 | **7.75×** |
| 24 | Waveform | 5000 | 83.2% | 81.6% | 81.6% | 5,112 | 899 | **5.69×** |
| 25 | Wine | 178 | 95.0% | 96.7% | 96.7% | 39 | 23 | 1.70× |
| 26 | Zoo | 101 | 97.1% | 96.5% | 96.5% | 29 | 18 | 1.61× |
| | **TỔNG / TB** | | **85.2%** | **85.1%** | **85.3%** | **23,546** | **4,624** | **5.09×** |

---

## 3. PHÂN TÍCH SPEEDUP

### 3.1 Top 5 datasets cải thiện mạnh nhất 🚀

| # | Dataset | Speedup | Lý do |
|---|---------|---------|-------|
| 1 | **Sick** | **26.58×** | Class imbalance 94/6 → mining + pruning trên class hiếm rất hiệu quả với BitSet AND |
| 2 | **Hypo** | **23.97×** | Dataset lớn (3163 mẫu) × nhiều rule (87K) → BitSet AND tận dụng tối đa |
| 3 | **German** | **11.56×** | 1000 mẫu × 20 attrs hỗn hợp → inverted index lợi lớn |
| 4 | **Led7** | **9.00×** | 3200 mẫu, 7 binary → BitSet nhỏ, AND nhanh |
| 5 | **Vehicle** | **7.75×** | 846 mẫu × 18 attrs → AND BitSet rất hiệu quả |

### 3.2 Tổng quan phân bố speedup

| Speedup | Số datasets | % |
|---------|-------------|---|
| ≥10× | 3 (Sick, Hypo, German) | 12% |
| 5×-10× | 11 | 42% |
| 2×-5× | 9 | 35% |
| 1×-2× | 2 (Auto, Wine) | 8% |
| <1× hoặc Iris (~0ms) | 1 | 4% |

### 3.3 Datasets không cải thiện nhiều

| Dataset | Speedup | Lý do |
|---------|---------|-------|
| **Auto** | 1.07× | 25 thuộc tính hỗn hợp + nhiều missing → vẫn tốn thời gian Phase 07 G2S subset check |
| **Wine** | 1.70× | Dataset nhỏ (178), overhead BitSet allocation > savings |
| **Zoo** | 1.61× | 101 mẫu, BitSet ~2 words → AND không lợi nhiều |
| **Lymphography** | 2.01× | 148 mẫu nhỏ |

→ **Quy luật:** Dataset càng lớn (>1000 mẫu) thì speedup càng cao.

---

## 4. ACCURACY KHÔNG SUY GIẢM (CÒN CẢI THIỆN)

**6 datasets accuracy CẢI THIỆN với improved version:**

| Dataset | Baseline | Improved | Cải thiện |
|---------|----------|----------|-----------|
| Anneal | 97.7% | **98.2%** | +0.5% |
| Horse | 80.7% | **82.3%** | +1.6% |
| Iono | 92.0% | **92.6%** | +0.6% |
| Sick | 96.5% | **96.8%** | +0.3% |
| Sonar | 78.4% | **80.8%** | +2.4% |
| Australian | 86.7% | **86.8%** | +0.1% |
| Crx | 86.0% | **86.1%** | +0.1% |

**Lý do:** Phase 07 G2S optimized không skip rules >10K → giữ được nhiều rule chất lượng cao hơn.

**Không có dataset nào suy giảm accuracy.**

---

## 5. KỸ THUẬT CẢI TIẾN

### 5.1 Phase 03 — FP-Growth Optimized (`FPGrowthOptimized.java`)
- **Inverted Index**: `item → BitSet(transactions chứa item)`
- **Itemset support = AND của BitSet** thay vì scan N transactions
- Class-aware mining: drop early itemsets không có class support
- → **Mining 5.83× nhanh hơn**

### 5.2 Phase 04 — Pruning với BitSet
- Chi-square + Coverage chia sẻ BitSet matches
- BitSet AND thay vì lặp N transactions
- → **Pruning 3.78× nhanh hơn**

### 5.3 Phase 07 — G2S Pruning với Bitmap Antecedent
- **Bitmap subset check**: `sub ⊆ sup ⟺ ∀i (sub[i] & ~sup[i]) = 0`
- Length-bucket index: `indexed[class][firstItem][len]`
- **KHÔNG còn skip khi >10K rules** → quality cao hơn

### 5.4 Phase 02 — CR-Tree Index Lookup
- `findAllMatching()` dùng class→firstItem index thay vì linear scan
- → Predict nhanh hơn

### 5.5 Phase 01 — Profiling Infrastructure
- `PhaseTimer` (per-phase nano timing)
- `MemorySampler` (peak memory 20ms sampling)

### 5.6 Code Cleanup (M1-M5)
- Xóa `allClassesFull()` no-op trong FPGrowthOptimized
- Xóa `chiSquarePruneLazy` dead code (~42 LOC)
- Xóa `isBaseline()` branch duplicate trong FPGrowth
- Document `ThreadLocal` trade-off
- Comment xác nhận `coveragePruneOld` ≡ `coveragePrune`
- Guard `CRTree.build()` empty antecedent
- `LocalDate.now()` thay hardcode date
- Constant `MAX_SINGLE_PATH_ITEMS = 15`
- **Tổng: -107 LOC dead code**

---

## 6. BỘ NHỚ

| Dataset lớn | Baseline (MB) | Improved (MB) | Tiết kiệm |
|-------------|---------------|---------------|-----------|
| Waveform | ~135 | ~95 | -30% |
| Anneal | ~98 | ~78 | -20% |
| Hypo | ~110 | ~85 | -23% |
| Sick | ~105 | ~75 | -29% |
| **Trung bình** | **88 MB** | **76 MB** | **-14%** |

**Lý do giảm bộ nhớ:**
- Class-aware mining drop sớm itemset không sinh rule → ít object Rule
- BitSet share giữa các phase → không duplicate

---

## 7. CÁCH CHẠY DEMO

```bash
# Compile
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

# Mode 1: Baseline (CMAR gốc, không có cải tiến)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Mode 2: Improved (cải tiến Phase 02-07)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved
```

**Output sau mỗi run:**
- `results/profiling-metrics.csv` — chi tiết per-dataset
- `results/profiling-metrics.md` — markdown đọc trực tiếp
- `results/summary-report.md` — accuracy comparison vs paper

---

## 8. KẾT LUẬN

✅ **Tốc độ:** 5.09× nhanh hơn trung bình, đỉnh **26.58× (Sick)**, **23.97× (Hypo)**, **11.56× (German)**
✅ **Bộ nhớ:** Giảm 14% trung bình (88 → 76 MB)
✅ **Accuracy:** Không suy giảm, còn cải thiện trên 7 datasets
✅ **Demo dual-mode:** Switch baseline/improved qua CLI flag
✅ **Profiling đầy đủ:** PhaseTimer + MemorySampler
✅ **Code sạch:** -107 LOC dead code, qua code review

**Vượt xa mục tiêu plan đặt ra (2× speedup → đạt 5.09×).**

---

## 9. FILES SINH RA

| File | Nội dung |
|------|----------|
| `results/BAO-CAO-HIEU-NANG-RUN2.md` | Báo cáo này |
| `results/profiling-metrics.csv` | CSV chi tiết per-dataset |
| `results/profiling-metrics.md` | MD profiling chi tiết |
| `results/summary-report.md` | Accuracy vs paper |
| `results/CODE-REVIEW-CAI-TIEN.md` | Báo cáo code review |
| `plans/.../reports/run3-baseline.csv` | Baseline metrics fresh |
| `plans/.../reports/run3-improved.csv` | Improved metrics fresh |
