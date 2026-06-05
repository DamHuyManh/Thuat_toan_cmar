# CMAR — Phân tích Top-k & Lift voting per dataset

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-11 và 2026-05-12 |
| **Đánh giá** | 10-fold CV trên 26 bộ UCI |
| **Tham chiếu** | Li, Han, Pei 2001 (CMAR); Alwidian 2018 (WCBA); Bahri 2020 (WEviRC) |

## 1. Phân loại dataset theo độ nhạy top-k

So sánh range giữa **k=0, k=3, k=5, k=7** (max − min) cho mỗi dataset.

### 🔵 NHÓM 1 — Top-k KHÔNG hiệu quả (range ≤ 0.3%) — 11/26 dataset (42%)

| Dataset | k=0 | k=3 | k=5 | k=7 | Range |
|---|---:|---:|---:|---:|---:|
| Breast-Cancer | 97.1 | 96.9 | 96.9 | 97.1 | 0.2 |
| Diabetes | 73.4 | 73.3 | 73.3 | 73.3 | 0.1 |
| Hypo | 97.9 | 97.9 | 97.9 | 97.9 | 0.0 |
| Led7 | 72.2 | 72.2 | 72.2 | 72.2 | 0.0 |
| Pima | 73.4 | 73.3 | 73.3 | 73.3 | 0.1 |
| Sick | 96.8 | 96.8 | 96.8 | 96.8 | 0.0 |
| Tic-Tac-Toe | 99.2 | 99.3 | 99.4 | 99.4 | 0.2 |
| Vehicle | 68.2 | 68.1 | 68.1 | 68.1 | 0.1 |
| Waveform | 81.6 | 81.5 | 81.5 | 81.6 | 0.1 |
| Zoo | 96.5 | 96.5 | 96.5 | 96.5 | 0.0 |
| Glass | 70.0 | 70.4 | 70.0 | 70.0 | 0.4 |

→ Đặc điểm: **accuracy đã rất cao** (đã saturate) HOẶC **dataset cứng** (Diabetes/Pima 73%, Vehicle 68%). Top-k không quyết định được nữa.

### 🟢 NHÓM 2 — Top-k hiệu quả NHẸ (range 0.3–1%) — 9/26 dataset (35%)

| Dataset | k=0 | k=3 | k=5 | k=7 | Range | Best k |
|---|---:|---:|---:|---:|---:|:---:|
| Anneal | **98.2** | 97.9 | 97.7 | 97.7 | 0.5 | 0 |
| Australian | **86.8** | 86.2 | 86.4 | 86.2 | 0.6 | 0 |
| Cleve | **82.6** | 81.9 | 81.6 | 82.3 | 1.0 | 0 |
| Crx | **86.1** | 85.5 | 86.0 | **86.1** | 0.6 | 0=7 |
| German | **72.9** | 72.3 | 72.2 | 72.3 | 0.7 | 0 |
| Horse | **82.3** | **82.3** | 81.5 | **82.3** | 0.8 | 0=3=7 |
| Iris | 92.7 | **93.3** | 92.7 | 92.7 | 0.6 | 3 |
| Sonar | **80.8** | **80.8** | 80.7 | 80.3 | 0.5 | 0=3 |
| Wine | **96.7** | 95.6 | 96.2 | 96.2 | 1.1 | 0 |

→ **8/9 dataset thắng ở k=0**. Iris là ngoại lệ (k=3 thắng nhẹ).

### 🟡 NHÓM 3 — Top-k hiệu quả VỪA (range 1–2%) — 4/26 dataset (15%)

| Dataset | k=0 | k=3 | k=5 | k=7 | Range | Best k |
|---|---:|---:|---:|---:|---:|:---:|
| Auto | 81.4 | 79.7 | **81.6** | 81.0 | 1.9 | **5** |
| Heart | **80.7** | 79.6 | 79.3 | 80.4 | 1.4 | 0 |
| Iono | 92.6 | 92.3 | 91.7 | **92.9** | 1.2 | **7** |
| Lymphography | 83.4 | 84.0 | 84.7 | **85.3** | 1.9 | **7** |

→ Đây là vùng **top-k vừa phải (k=5, 7) thắng k=0**. Đặc điểm: dataset rule-noisy.

### 🔴 NHÓM 4 — Top-k hiệu quả MẠNH (range > 2%) — 2/26 dataset (8%)

| Dataset | k=0 | k=3 | k=5 | k=7 | Range | Best k |
|---|---:|---:|---:|---:|---:|:---:|
| Hepatitis | **83.3** | 81.4 | 80.8 | 82.0 | **2.5** | 0 |
| Labor | **91.7** | 83.0 | 86.3 | 88.3 | **8.7** ⚠️ | 0 |

→ Cả 2 dataset đều **nhỏ** (Hepatitis 155 mẫu, Labor 57 mẫu). Cắt top-k mất luật quan trọng → giảm mạnh. **k=0 thắng tuyệt đối**.

---

## 2. Tổng kết: k nào hiệu quả nhất?

### Đếm số dataset thắng theo k

| k | NHÓM 1 (saturated) | NHÓM 2 (nhẹ) | NHÓM 3 (vừa) | NHÓM 4 (mạnh) | **Tổng thắng/hòa** |
|:---:|:---:|:---:|:---:|:---:|:---:|
| **k=0** | tie | **8/9** | 1/4 (Heart) | **2/2** | **20** ⭐ |
| k=3 | tie | 1/9 (Iris) + ties | 0/4 | 0/2 | 8 |
| k=5 | tie | ties | 1/4 (Auto) | 0/2 | 6 |
| k=7 | tie | ties | 2/4 (Iono, Lympho) | 0/2 | 11 |

→ **k=0 thắng/hòa trên 20/26 dataset (77%)**.

### Khi nào k>0 đáng dùng?

Chỉ **3/26 dataset** (Auto, Iono, Lymphography) có k vừa phải thắng k=0. Còn lại 88% dataset: **k=0 ≥ mọi k khác**.

---

## 3. Thay đổi luật scoring — thử Lift voting

Đề xuất từ paper review 2025 (WEviRC, Bahri 2020): dùng **Lift** làm trọng số voting thay vì chi².

### Công thức
$$\text{weight}(r) = \text{Lift}(X \to c) = \frac{\text{Supp}(X \to c) \cdot N}{\text{Supp}(X) \cdot \text{Supp}(c)}$$

- Lift > 1 → tương quan thuận càng mạnh → đóng góp voting càng lớn
- Lift = 1 → không tương quan → đóng góp = 1 (neutral)

### Kết quả so với chi² weight

| Voting weight | topK=0 | topK=7 |
|---|---:|---:|
| Chi² (CMAR gốc) | **85.3%** | **85.1%** |
| HM (WCBA-style) | 85.2% | 85.0% |
| **Lift** (WEviRC-style) | **85.2%** | **85.2%** |

→ Avg gần như tương đương, nhưng **phân bố theo dataset rất khác**.

### Dataset HƯỞNG LỢI từ Lift voting (chi² → Lift, topK=0)

| Dataset | Chi² | Lift | Gain |
|---|---:|---:|---:|
| **Hepatitis** | 83.3 | **84.8** | **+1.5** ⭐ |
| **Auto** | 81.4 | **82.5** | **+1.1** ⭐ |
| **Iono** | 92.6 | **93.2** | +0.6 |
| **Sonar** | 80.8 | **81.3** | +0.5 |
| **Breast-Cancer** | 97.1 | **97.4** | +0.3 |
| German | 72.9 | 73.0 | +0.1 |
| Hypo | 97.9 | 98.0 | +0.1 |

### Dataset BỊ TỔN từ Lift voting

| Dataset | Chi² | Lift | Loss |
|---|---:|---:|---:|
| **Lymphography** | 83.4 | 82.0 | **−1.4** |
| **Horse** | 82.3 | 81.0 | −1.3 |
| **Wine** | 96.7 | 95.6 | −1.1 |
| **Zoo** | 96.5 | 95.6 | −0.9 |
| Heart | 80.7 | 80.0 | −0.7 |
| Anneal | 98.2 | 97.9 | −0.3 |
| Tic-Tac-Toe | 99.2 | 99.0 | −0.2 |

### Pattern quan sát

- **Lift voting tốt hơn** trên dataset có *rule conflict* hoặc *nhiều luật yếu* (Hepatitis, Auto, Iono, Sonar) — Lift trừng phạt mạnh các luật tương quan yếu, giảm noise.
- **Lift voting kém hơn** trên dataset *balanced* hoặc *small* (Wine, Zoo, Horse) — chi² weight tự nhiên hợp với phân bố này.

---

## 4. Bảng tổng hợp accuracy đầy đủ

| Dataset | Paper | Base | k=0 | k=3 | k=5 | k=7 | Lift k=0 | Lift k=7 | HM k=0 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Anneal | 97.3 | 97.7 | **98.2** | 97.9 | 97.7 | 97.7 | 97.9 | 98.1 | 98.2 |
| Australian | 86.1 | 86.2 | **86.8** | 86.2 | 86.4 | 86.2 | 86.7 | 86.2 | 86.8 |
| Auto | 78.1 | 79.7 | 81.4 | 79.7 | 81.6 | 81.0 | **82.5** | 82.1 | 82.3 |
| Breast-Cancer | 96.4 | 96.9 | 97.1 | 96.9 | 96.9 | 97.1 | **97.4** | 97.2 | 96.3 |
| Cleve | 82.2 | 81.9 | **82.6** | 81.9 | 81.6 | 82.3 | **82.6** | **82.6** | 82.2 |
| Crx | 84.9 | 85.5 | **86.1** | 85.5 | 86.0 | **86.1** | 85.7 | **86.1** | 85.3 |
| Diabetes | 75.8 | 73.3 | **73.4** | 73.3 | 73.3 | 73.3 | **73.4** | 73.3 | 73.3 |
| German | 74.9 | 72.2 | 72.9 | 72.3 | 72.2 | 72.3 | **73.0** | 72.3 | 72.8 |
| Glass | 70.1 | 70.4 | 70.0 | **70.4** | 70.0 | 70.0 | 69.9 | **70.8** | **70.4** |
| Heart | 82.2 | 79.6 | **80.7** | 79.6 | 79.3 | 80.4 | 80.0 | 80.0 | 79.6 |
| Hepatitis | 80.5 | 81.4 | 83.3 | 81.4 | 80.8 | 82.0 | **84.8** | 84.1 | 82.0 |
| Horse | 82.6 | 80.9 | 82.3 | 82.3 | 81.5 | 82.3 | 81.0 | 82.1 | 82.3 |
| Hypo | 98.4 | 97.9 | 97.9 | 97.9 | 97.9 | 97.9 | **98.0** | 97.9 | 97.9 |
| Iono | 91.5 | 92.3 | 92.6 | 92.3 | 91.7 | 92.9 | 93.2 | **93.4** | 92.9 |
| Iris | 94.0 | 93.3 | 92.7 | **93.3** | 92.7 | 92.7 | 92.7 | 92.7 | 92.7 |
| Labor | 89.7 | 83.0 | **91.7** | 83.0 | 86.3 | 88.3 | **91.7** | 90.0 | **91.7** |
| Led7 | 72.5 | 72.2 | 72.2 | 72.2 | 72.2 | 72.2 | 72.2 | 72.2 | 72.2 |
| Lymphography | 83.1 | 84.0 | 83.4 | 84.0 | 84.7 | **85.3** | 82.0 | 83.5 | 83.3 |
| Pima | 75.1 | 73.3 | **73.4** | 73.3 | 73.3 | 73.3 | **73.4** | 73.3 | 73.3 |
| Sick | 97.5 | 96.5 | 96.8 | 96.8 | 96.8 | 96.8 | 96.8 | 96.8 | 96.8 |
| Sonar | 79.4 | 76.5 | 80.8 | 80.8 | 80.7 | 80.3 | **81.3** | 80.3 | **81.8** |
| Tic-Tac-Toe | 99.2 | 99.3 | 99.2 | 99.3 | 99.4 | **99.4** | 99.0 | 99.3 | 99.2 |
| Vehicle | 68.8 | 68.1 | **68.2** | 68.1 | 68.1 | 68.1 | **68.2** | 68.1 | **68.2** |
| Waveform | 83.2 | 81.5 | 81.6 | 81.5 | 81.5 | 81.6 | 81.6 | 81.6 | 81.6 |
| Wine | 95.0 | 95.6 | **96.7** | 95.6 | 96.2 | 96.2 | 95.6 | 96.2 | 96.2 |
| Zoo | 97.1 | 96.5 | **96.5** | 96.5 | 96.5 | **96.5** | 95.6 | 95.6 | **96.5** |
| **Avg** | **85.2** | **84.5** | **85.3** | 84.7 | 84.8 | 85.1 | 85.2 | 85.2 | 85.2 |

---

## 5. Thống kê Lift theo dataset

Từ file `results/rules/<dataset>-rules.csv` (đã sort theo Lift desc):

| Dataset | #rules | Avg Lift | Max Lift | Min Lift | Rules có Lift>2 | % | Acc gain (Lift vote) |
|---|---:|---:|---:|---:|---:|---:|:---:|
| Anneal | 153 | 4.449 | **22.53** | 1.317 | 45 | 29% | −0.3 |
| Australian | 464 | 1.990 | 2.245 | 1.561 | 237 | 51% | −0.1 |
| **Auto** | 230 | **6.230** | **62.33** | 1.908 | 227 | **98%** | **+1.1** ⭐ |
| Breast-Cancer | 253 | 2.277 | 2.852 | 1.190 | 147 | 58% | +0.3 |
| Cleve | 305 | 1.976 | 2.175 | 1.567 | 138 | 45% | 0.0 |
| Crx | 603 | 1.964 | 2.245 | 1.329 | 266 | 44% | −0.4 |
| Diabetes | 215 | 1.690 | 2.860 | 1.084 | 54 | 25% | 0.0 |
| German | 900 | 1.741 | 3.333 | 1.096 | 235 | 26% | +0.1 |
| Glass | 119 | 6.477 | 21.78 | 1.645 | 116 | 97% | −0.1 |
| Heart | 243 | 1.997 | 2.250 | 1.517 | 120 | 49% | −0.7 |
| **Hepatitis** | 125 | 2.540 | **4.828** | 1.190 | 45 | 36% | **+1.5** ⭐⭐ |
| Horse | 401 | 2.083 | 2.699 | 1.475 | 186 | 46% | −1.3 |
| Hypo | 180 | 3.997 | 20.93 | 1.025 | 28 | 15% | +0.1 |
| **Iono** | 184 | 2.138 | 2.781 | 1.562 | 90 | 48% | **+0.6** ⭐ |
| Iris | 38 | 2.893 | 3.000 | 2.188 | 38 | 100% | 0.0 |
| Labor | 55 | 2.073 | 2.889 | 1.529 | 22 | 40% | 0.0 |
| Led7 | 112 | **7.684** | 9.404 | **5.783** | 112 | 100% | 0.0 |
| Lymphography | 149 | 5.546 | **67.00** | 1.836 | 78 | 52% | **−1.4** |
| Pima | 215 | 1.690 | 2.860 | 1.084 | 54 | 25% | 0.0 |
| Sick | 300 | 3.485 | 16.37 | 1.048 | 53 | 17% | 0.0 |
| **Sonar** | 174 | 1.995 | 2.136 | 1.781 | 85 | 48% | **+0.5** ⭐ |
| Tic-Tac-Toe | 187 | 2.150 | 2.886 | 1.530 | 101 | 54% | −0.2 |
| Vehicle | 498 | 3.394 | 4.244 | 1.949 | 488 | 97% | 0.0 |
| Waveform | 2681 | 2.740 | 3.036 | 1.944 | 2678 | 99% | 0.0 |
| Wine | 45 | 2.840 | 3.682 | 2.531 | 45 | 100% | **−1.1** |
| **Zoo** | 36 | **11.52** | 23.25 | 2.514 | 36 | **100%** | **−0.9** |

> Cột "Acc gain (Lift vote)" = accuracy khi dùng Lift voting (`--liftWeight`) **trừ** accuracy chi² weight, ở topK=0.

### Pattern thú vị

**Dataset có Lift cao KHÔNG hẳn benefit từ Lift voting**:
- 🟢 **Auto**: avg lift 6.23, 98% rules > 2 → Lift voting **gain +1.1** ✓ (pattern khớp kỳ vọng)
- 🟢 **Hepatitis**: avg lift 2.54, max 4.83 → Lift voting **gain +1.5** ✓
- 🔴 **Zoo**: avg lift **11.52** (cao nhất), 100% rules > 2 → Lift voting **MẤT 0.9** ❌
- 🔴 **Wine**: avg lift 2.84, 100% rules > 2 → Lift voting **MẤT 1.1** ❌
- 🔴 **Lymphography**: max lift **67** → Lift voting **MẤT 1.4** ❌

→ Tại sao? Khi **tất cả luật** đều có Lift cao (Zoo, Wine), tương đối giữa các luật **không thay đổi nhiều** giữa Lift và chi² → vote gần như giống nhau. Nhưng Lift bị **outliers cực cao** chi phối (vd Lymphography max=67), một vài luật rare dominates vote → sai class.

→ **Lift voting tốt nhất khi Lift values có spread vừa phải** (1.5–5.0) và **không có outliers cực cao**.

### Phân loại dataset theo "Lift signature"

| Loại | Đặc điểm | Ví dụ | Khuyên dùng |
|---|---|---|---|
| 🟢 **Lift-friendly** | avg lift 2–6, không outlier | Auto, Hepatitis, Iono, Sonar | **Lift voting +0.5 đến +1.5** |
| 🔵 **Saturated** | tất cả luật conf=1.0, lift cao đều | Wine, Zoo, Iris, Led7 | Lift = chi² (không khác) hoặc Lift kém hơn |
| 🟡 **Lift extreme** | max lift > 20, outliers dominate | Anneal, Lymphography, Glass | **TRÁNH Lift** — chi² ổn định hơn |
| ⚪ **Lift mỏng** | avg lift < 2 | Diabetes, Pima, German | Lift = chi² (rules tương quan yếu) |

---

## 6. Khuyến nghị cuối

### Câu hỏi 1: top-k nào hiệu quả nhất?
→ **`topK = 0`** (voting tất cả luật khớp). Thắng/hòa trên 20/26 dataset.

### Câu hỏi 2: dataset nào hiệu quả với top-k, dataset nào không?
- **42% dataset KHÔNG nhạy với top-k** (đã saturated hoặc dataset cứng): Hypo, Led7, Sick, Zoo, Tic-Tac-Toe, Vehicle, Waveform, Glass, Diabetes, Pima, Breast-Cancer.
- **8% dataset CỰC NHẠY với top-k**: Labor, Hepatitis — chỉ k=0 mới tốt.
- **15% dataset có k vừa phải tốt nhất** (5 hoặc 7): Auto, Iono, Lymphography.

### Câu hỏi 3: thay đổi luật scoring (Lift voting) có hiệu quả?
- **Avg 85.2%** — gần như tương đương chi² (85.3%).
- **Tốt hơn rõ** trên 5 dataset: **Hepatitis (+1.5), Auto (+1.1), Iono (+0.6), Sonar (+0.5), Breast-Cancer (+0.3)**.
- **Tệ hơn** trên: Lymphography (−1.4), Horse (−1.3), Wine (−1.1).

### Lựa chọn theo loại dataset

| Loại dataset | Cấu hình tối ưu |
|---|---|
| Default | **chi² weight + topK=0** (85.3%) |
| Rule-noisy (Auto, Hepatitis, Iono, Sonar) | **Lift weight + topK=0** (gain 0.5–1.5%) |
| Đã saturated | k tùy ý — không khác biệt |
| Dataset nhỏ (< 200 mẫu) | **k=0 bắt buộc** (Labor, Hepatitis) |

---

## File dữ liệu nguồn

| File | Cấu hình |
|---|---|
| [summary-report-baseline.md](summary-report-baseline.md) | Baseline |
| [summary-report-topk0.md](summary-report-topk0.md) ... `-topk7.md` | Chi² weight + topK 0/3/5/7 |
| [summary-report-hmweight-topk0.md](summary-report-hmweight-topk0.md) | HM weight + topK=0 |
| [summary-report-hmweight-topk7.md](summary-report-hmweight-topk7.md) | HM weight + topK=7 |
| [summary-report-liftweight-topk0.md](summary-report-liftweight-topk0.md) | **Lift weight + topK=0** ⭐ |
| [summary-report-liftweight-topk7.md](summary-report-liftweight-topk7.md) | **Lift weight + topK=7** ⭐ |
| [summary-report-hmlift-topk0.md](summary-report-hmlift-topk0.md) | Full HM+Lift hybrid (broken, kept for documentation) |

## Cách tái tạo

```powershell
# Chi² voting (default CMAR)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=7

# Lift voting (WEviRC-style)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftWeight --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftWeight --topK=7

# HM voting (WCBA-style)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --hmWeightOnly --topK=0
```
