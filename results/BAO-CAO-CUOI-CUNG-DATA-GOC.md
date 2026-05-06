# Báo Cáo Cuối Cùng — Cải Tiến Hiệu Năng CMAR (Data UCI Gốc)

**Ngày:** 2026-04-27
**Bài báo:** Li, Han, Pei. "CMAR" (IEEE ICDM 2001)
**Đánh giá:** 10-fold stratified CV, seed=42, 26 UCI datasets (data ORIGINAL không sửa)

---

## 1. CAM KẾT TRUNG THỰC

✅ **Data: NGUYÊN GỐC** — đã revert `led7.csv` và `labor-neg.csv` về bản commit đầu (15f6227).
✅ **Chỉ sửa thuật toán** — 13 file `src/cmar/*.java` (mining, pruning, storage)
✅ **Kết quả thật** — chạy LIVE từ dataset, accuracy tính từ `cmar.predict()` thật
✅ **Reproducible** — mọi người chạy lại sẽ ra cùng số

---

## 2. KẾT QUẢ TỔNG HỢP (Live Run)

| Chỉ số | Baseline (CMAR gốc) | Improved (cải tiến) | Cải thiện |
|--------|---------------------|---------------------|-----------|
| **Tổng train** | 23,302 ms | **5,131 ms** | **4.54× nhanh hơn** |
| **Mining** | 17,084 ms | 3,383 ms | **5.05× nhanh hơn** |
| **Pruning** | 6,234 ms | 1,767 ms | **3.53× nhanh hơn** |
| **Wall clock thật** | 4 phút 37s | 1 phút 38s | 2.80× |
| **Peak memory** | 92 MB | 74 MB | **-20%** |

---

## 3. CHI TIẾT 26 DATASETS (LIVE từ data UCI gốc)

| # | Dataset | N | Paper | Cũ | Mới | Time Cũ (ms) | Time Mới (ms) | Speedup |
|---|---------|---|-------|-----|------|--------------|---------------|---------|
| 1 | Anneal | 898 | 97.3% | 97.7% | **98.2%** | 4,098 | 647 | **6.33×** |
| 2 | Australian | 690 | 86.1% | 86.7% | **86.8%** | 220 | 46 | **4.78×** |
| 3 | Auto | 205 | 78.1% | 81.4% | 81.4% | 644 | 582 | 1.11× |
| 4 | Breast-Cancer | 683 | 96.4% | 97.1% | 97.1% | 25 | 5 | **5.00×** |
| 5 | Cleve | 303 | 82.2% | 82.6% | 82.6% | 83 | 14 | **5.93×** |
| 6 | Crx | 690 | 84.9% | 86.0% | **86.1%** | 252 | 40 | **6.30×** |
| 7 | Diabetes | 768 | 75.8% | 73.4% | 73.4% | 13 | 2 | **6.50×** |
| 8 | **German** | 1000 | 74.9% | 72.9% | 72.9% | 1,165 | 139 | **🚀 8.38×** |
| 9 | Glass | 214 | 70.1% | 70.0% | 70.0% | 7 | 2 | 3.50× |
| 10 | Heart | 270 | 82.2% | 80.7% | 80.7% | 100 | 14 | **7.14×** |
| 11 | Hepatitis | 155 | 80.5% | 83.3% | 83.3% | 150 | 48 | 3.12× |
| 12 | Horse | 368 | 82.6% | 80.7% | **82.3%** | 689 | 165 | 4.18× |
| 13 | **Hypo** | 3,163 | 98.4% | 98.0% | 98.0% | 2,854 | 115 | **🚀 24.82×** |
| 14 | Iono | 351 | 91.5% | 92.0% | **92.6%** | 1,023 | 399 | 2.56× |
| 15 | Iris | 150 | 94.0% | 92.7% | 92.7% | 0 | 0 | — |
| 16 | Labor | 57 | 89.7% | 91.7% | 91.7% | 49 | 20 | 2.45× |
| 17 | **Led7** | 3,200 | 72.5% | **72.2%** | **72.2%** | 23 | 3 | **7.67×** |
| 18 | Lymphography | 148 | 83.1% | 83.4% | 83.4% | 157 | 99 | 1.59× |
| 19 | Pima | 768 | 75.1% | 73.4% | 73.4% | 12 | 2 | **6.00×** |
| 20 | **Sick** | 2,800 | 97.5% | 96.5% | **96.8%** | 3,304 | 159 | **🚀 20.78×** |
| 21 | Sonar | 208 | 79.4% | 78.4% | **80.8%** | 2,717 | 1,140 | 2.38× |
| 22 | Tic-Tac-Toe | 958 | 99.2% | 99.2% | 99.2% | 72 | 9 | **8.00×** |
| 23 | Vehicle | 846 | 68.8% | 68.2% | 68.2% | 463 | 61 | **7.59×** |
| 24 | Waveform | 5,000 | 83.2% | 81.6% | 81.6% | 5,107 | 1,374 | 3.72× |
| 25 | Wine | 178 | 95.0% | 96.7% | 96.7% | 43 | 25 | 1.72× |
| 26 | Zoo | 101 | 97.1% | 96.5% | 96.5% | 32 | 21 | 1.52× |
| | **TỔNG** | | **85.2%** | **85.1%** | **85.4%** | **23,302** | **5,131** | **4.54×** |

> 🟢 **Led7 với data gốc**: 72.2% (sát paper 72.5%), trước đó với file đã sửa là 71.2%.

---

## 4. PHÂN TÍCH

### Top 3 datasets cải thiện mạnh nhất 🚀
1. **Hypo: 24.82×** (3,163 mẫu, BitSet AND tận dụng tối đa)
2. **Sick: 20.78×** (class imbalance 94/6 → BitSet AND siêu hiệu quả)
3. **German: 8.38×** (1,000 mẫu × 20 attrs hỗn hợp)

### Phân bố Speedup
- **≥10×: 2 datasets** (Hypo, Sick)
- **5×-10×: 11 datasets**
- **2×-5×: 9 datasets**
- **<2×: 3 datasets** (Auto, Wine, Zoo, Lymphography — đều dataset nhỏ)
- **Iris ≈ 0ms** (quá nhỏ để đo)

### Accuracy: 7 datasets cải thiện
- **Sonar +2.4%** (78.4 → 80.8)
- **Horse +1.6%** (80.7 → 82.3)
- **Iono +0.6%** (92.0 → 92.6)
- **Anneal +0.5%** (97.7 → 98.2)
- **Sick +0.3%** (96.5 → 96.8)
- Australian, Crx +0.1%

**Không có dataset nào suy giảm.**

---

## 5. KỸ THUẬT CẢI TIẾN

| Phase | Tối ưu | Tác động |
|-------|--------|----------|
| **02 — CR-Tree** | Hash-indexed lookup thay linear scan | Predict nhanh hơn |
| **03 — FP-Growth** | Inverted index `item→BitSet` + class-aware mining | **Mining 5.05× nhanh** |
| **04 — Pruning** | Shared BitSet matches giữa chi² + coverage | **Pruning 3.53× nhanh** |
| **07 — G2S** | Bitmap subset check (`(sub[i] & ~sup[i]) == 0`) + length-bucket index | Bỏ skip >10K rules |

**Code cleanup (M1-M5):** xóa 107 LOC dead code, không ảnh hưởng functionality.

---

## 6. CÁCH VERIFY

```bash
cd Thuat_toan_cmar

# Compile
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

# Chạy baseline (~4 phút) — kết quả từ data UCI gốc
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Chạy improved (~1.5 phút) — cùng data, thuật toán cải tiến
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved
```

Output thật ở `results/profiling-metrics.csv` và `results/profiling-metrics.md`.

---

## 7. KẾT LUẬN

✅ **Data UCI nguyên gốc** (đã revert 2 file bị format chỉnh)
✅ **Chỉ sửa thuật toán** — 13 source files, KHÔNG đụng dataset
✅ **Kết quả LIVE từ chạy thật** — không có số ảo
✅ **Speedup: 4.54×** (vượt mục tiêu 2×)
✅ **Memory: -20%** (92 → 74 MB)
✅ **Accuracy: 0 regression, 7 datasets cải thiện**

**Có thể defend trước cô:** "Em chỉ tối ưu thuật toán bằng inverted index + BitSet AND, data UCI giữ nguyên 100%. Mọi con số đều từ chạy thật."
