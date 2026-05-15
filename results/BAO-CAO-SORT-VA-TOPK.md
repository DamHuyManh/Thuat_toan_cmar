# Đổi sort sang "Larger Lift first" — Kết quả

> Thử nghiệm: thay vì sort luật theo **CMAR gốc** (confidence DESC), sort theo **Lift DESC** (luật có Lift lớn nhất xếp đầu). Sort áp dụng cho cả pruning lẫn predict.

---

## 1. Thiết kế thí nghiệm

### Sort CMAR gốc (default)
```
confidence DESC → support DESC → length ASC
```

### Sort "Larger Lift first" (mới)
```
Lift DESC → confidence DESC → length ASC
```

→ Rule có Lift cao nhất sẽ ở **top**, ưu tiên được coverage prune giữ lại, và vote đầu khi predict.

### Voting weight giữ nguyên = **χ² chuẩn hóa** (chỉ đổi SORT, không đổi WEIGHT).

```powershell
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftSort --topK=N
```

---

## 2. Kết quả tổng hợp Avg Accuracy

| Top-k | CMAR sort (default) | **Lift sort** | Δ |
|:---:|---:|---:|---:|
| k=0 | **85.3%** | **82.3%** | **−3.0%** ❌ |
| k=3 | 84.7% | 80.2% | **−4.5%** ❌ |
| k=5 | 84.8% | 81.3% | **−3.5%** ❌ |
| k=7 | 85.1% | 81.9% | **−3.2%** ❌ |

### Tổng kết

→ **Lift sort tệ hơn CMAR sort 3–5% trên TẤT CẢ giá trị top-k**.

→ Cấu hình tốt nhất với Lift sort = `topK=0` (82.3%) **vẫn thua paper 2.9%** và **thua Baseline 2.2%**.

---

## 3. Tại sao Lift sort tệ?

### Nguyên nhân: phá vỡ Database Coverage Pruning

**Coverage prune** duyệt luật theo thứ tự sort, giữ luật nào phủ instance mới:

```
Sort theo CMAR (conf DESC):
  Rule 1: conf=1.0, cover 50 instances → giữ, mark 50 covered
  Rule 2: conf=0.9, cover 30, 20 mới → giữ, mark 20 covered
  ...
  → Luật conf cao được xét trước, giữ lại tốt nhất
```

```
Sort theo Lift DESC:
  Rule 1: Lift=22.5 (rare→rare), cover 5 instances → giữ, mark 5 covered
  Rule 2: Lift=15.0, cover 3 → giữ, mark 3 covered
  ...
  Rule 100: Lift=2.0, cover 200 → BỎ vì nhiều instances đã covered
  → Luật mạnh, phủ rộng bị BỎ vì xét sau
```

→ Lift sort ưu tiên **rule lift cao** (thường niche, cover ít) trước, **loại luật phủ rộng**. Kết quả là **mất luật mạnh** cần cho generalize.

### Số rule còn lại giảm mạnh

| Dataset | CMAR sort (rules) | Lift sort (rules) |
|---|---:|---:|
| Anneal | 159 | ~14 |
| Australian | 456 | ~16 |

→ Coverage prune cắt aggressive hơn nhiều khi sort theo Lift.

---

## 4. Bảng chi tiết per dataset (CMAR sort vs Lift sort, topK=0)

| Dataset | CMAR sort | **Lift sort** | Δ | Đánh giá |
|---|---:|---:|---:|---|
| Anneal | 98.2 | 96.9 | −1.3 | |
| Australian | 86.8 | 77.8 | **−9.0** | 🔴 |
| **Auto** | 81.4 | **81.4** | 0.0 | hòa |
| Breast-Cancer | 97.1 | 96.3 | −0.8 | |
| Cleve | 82.6 | 79.6 | −3.0 | |
| **Crx** | 86.1 | 79.6 | **−6.5** | 🔴 |
| **Diabetes** | 73.4 | 63.7 | **−9.7** | 🔴🔴 |
| **German** | 72.9 | 54.2 | **−18.7** | 🔴🔴🔴 |
| **Glass** | 70.0 | **72.1** | **+2.1** | 🟢 |
| Heart | 80.7 | 79.6 | −1.1 | |
| Hepatitis | 83.3 | 80.1 | −3.2 | |
| Horse | 82.3 | 78.6 | −3.7 | |
| Hypo | 97.9 | 97.5 | −0.4 | |
| **Iono** | 92.6 | **92.9** | **+0.3** | 🟢 |
| Iris | 92.7 | 92.7 | 0.0 | hòa |
| **Labor** | 91.7 | **91.7** | 0.0 | hòa |
| Led7 | 72.2 | 72.2 | 0.0 | hòa |
| Lymphography | 83.4 | 82.2 | −1.2 | |
| **Pima** | 73.4 | 63.7 | **−9.7** | 🔴🔴 |
| Sick | 96.8 | 96.5 | −0.3 | |
| Sonar | 80.8 | 76.4 | −4.4 | |
| **Tic-Tac-Toe** | 99.2 | 91.3 | **−7.9** | 🔴 |
| Vehicle | 68.2 | 68.0 | −0.2 | |
| Waveform | 81.6 | 81.8 | +0.2 | |
| **Wine** | 96.7 | **96.7** | 0.0 | hòa |
| Zoo | 96.5 | 96.5 | 0.0 | hòa |
| **Avg** | **85.3** | **82.3** | **−3.0** | ❌ |

### Phân loại theo mức thiệt hại

| Mức | Số DS | Ví dụ |
|---|:---:|---|
| 🟢 Gain (+) | 3 | Glass +2.1, Iono +0.3, Waveform +0.2 |
| ⚪ Hòa (≤ ±0.1) | 7 | Auto, Iris, Labor, Led7, Vehicle, Wine, Zoo |
| 🟡 Mất nhẹ (0.1–3%) | 8 | Anneal, Breast-Cancer, Cleve, Heart, Hepatitis, Hypo, Lymphography, Sick |
| 🔴 Mất vừa (3–7%) | 4 | Crx, Horse, Sonar, Tic-Tac-Toe |
| 🔴🔴 Mất nặng (> 7%) | 4 | **German −18.7**, Australian −9.0, **Diabetes −9.7**, **Pima −9.7** |

---

## 5. Dataset HƯỞNG LỢI từ Lift sort

Chỉ 3 dataset gain:

| Dataset | CMAR | Lift sort | Gain | Đặc điểm |
|---|---:|---:|---:|---|
| **Glass** | 70.0 | 72.1 | +2.1 | 6 class, dataset nhỏ |
| **Iono** | 92.6 | 92.9 | +0.3 | Binary, rule-noisy |
| **Waveform** | 81.6 | 81.8 | +0.2 | 3 class, lớn |

→ Pattern: dataset **multi-class với class hiếm** hoặc **dataset nhỏ** có thể hưởng lợi khi ưu tiên luật Lift cao (niche rules).

---

## 6. Dataset BỊ TỔN nặng nhất

| Dataset | CMAR | Lift sort | Loss | Tại sao? |
|---|---:|---:|---:|---|
| **German** | 72.9 | 54.2 | **−18.7** | Binary, conf-based vote rất quan trọng |
| **Diabetes** | 73.4 | 63.7 | −9.7 | Binary, support cao quan trọng hơn lift |
| **Pima** | 73.4 | 63.7 | −9.7 | (giống Diabetes) |
| **Australian** | 86.8 | 77.8 | −9.0 | Binary balanced |
| **Tic-Tac-Toe** | 99.2 | 91.3 | −7.9 | Binary với rule conf=100% — Lift sort phá |
| **Crx** | 86.1 | 79.6 | −6.5 | Binary balanced |

→ Pattern: **dataset binary, balanced** (50/50 class distribution) **mất nhiều nhất** vì rule conf=100% là chìa khóa nhưng bị Lift sort xếp sau rule Lift cao.

---

## 7. Kết luận

### ❌ Hướng "Larger Lift first" THẤT BẠI

| | CMAR sort (default) | Lift sort |
|---|---:|---:|
| Avg accuracy | **85.3%** | 82.3% |
| Δ vs Paper | +0.1% | **−2.9%** |
| Δ vs Baseline | +0.8% | −2.2% |
| Số DS thắng paper | **14/26** | **3/26** |
| Coverage prune | ✅ Hiệu quả | ❌ Bị phá |

### Bài học

1. **Không thể "chỉ đổi sort"** — sort là tham số quyết định của coverage pruning. Đổi sort = đổi luật được giữ.
2. **Lift là metric statistic, không phải selection priority** — Lift cao = ý nghĩa thống kê, không có nghĩa = luật quan trọng cho predict.
3. **CMAR sort (conf DESC) phản ánh đúng triết lý classification**: ưu tiên độ **tin cậy** rather than độ **tương quan**.

### Khi nào Lift sort có thể OK?

Chỉ trên **3 dataset niche**: Glass, Iono, Waveform. Nhưng gain tổng cộng < 3% trong khi loss tổng cộng > 50% → **không xứng đáng**.

### Khuyến nghị cuối

→ **Giữ CMAR sort gốc** (`confidence DESC → support DESC → length ASC`).

→ **Lift chỉ nên dùng làm VOTING WEIGHT** (`--liftWeight`) — đã chứng minh cải thiện 0.5–1.5% trên dataset rule-noisy mà không phá pruning.

→ **TUYỆT ĐỐI KHÔNG** dùng Lift làm SORT primary criterion.

---

## File kết quả raw

| File | Cấu hình | Avg |
|---|---|---:|
| [summary-report-liftSort-topk0.md](summary-report-liftSort-topk0.md) | Lift sort + k=0 | **82.3%** |
| [summary-report-liftSort-topk3.md](summary-report-liftSort-topk3.md) | Lift sort + k=3 | 80.2% |
| [summary-report-liftSort-topk5.md](summary-report-liftSort-topk5.md) | Lift sort + k=5 | 81.3% |
| [summary-report-liftSort-topk7.md](summary-report-liftSort-topk7.md) | Lift sort + k=7 | 81.9% |

## Tái tạo

```powershell
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftSort --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftSort --topK=7
```
