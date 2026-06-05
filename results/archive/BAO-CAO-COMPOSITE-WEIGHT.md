# Báo cáo: Cải tiến thuật toán — TRỌNG SỐ VOTE TỔ HỢP

> Ngày chạy: 2026-05-14
> Trả lời cô: *"Cải tiến thuật toán, chạy rồi show kết quả, nói rõ đang thay đổi cái gì."*

---

## 1. EM THAY ĐỔI GÌ?

### Ý tưởng cốt lõi

Paper CMAR vote bằng **MỘT chỉ số duy nhất**: χ². Bản em thử trước chỉ vote bằng MỘT chỉ số: Lift.

→ Cải tiến mới: **NHÂN 2 chỉ số** để mỗi luật được đánh giá trên **2 mặt cùng lúc**:
- Có ý nghĩa thống kê cao **VÀ** tương quan mạnh
- HOẶC: dự đoán chính xác cao **VÀ** tương quan mạnh

### Bảng 4 biến thể em test

| # | Sắp xếp | Lọc | **Vote weight (CHỖ EM ĐỔI)** | Top-K |
|:---:|---|---|---|:---:|
| **V7** | conf→sup→len (giữ paper) | χ²≥3.841 (giữ paper) | **χ² × Lift** | 0 (tất cả) |
| **V8** | conf→sup→len (giữ paper) | χ²≥3.841 (giữ paper) | **χ² × Lift** | **5** |
| **V9** | conf→sup→len (giữ paper) | χ²≥3.841 (giữ paper) | **conf × Lift** | 0 (tất cả) |
| **V10** | conf→sup→len (giữ paper) | χ²≥3.841 (giữ paper) | **conf × Lift** | **5** |

**Lưu ý**:
- 4 biến thể này **không đụng vào sắp xếp/lọc** → an toàn (đã học từ thí nghiệm trước, sort/filter dễ phá kết quả)
- Chỉ đổi **bước cuối** — cách tính phiếu của mỗi luật

### Trực quan công thức

```
Paper CMAR (mặc định):
   Phiếu(luật) = χ² chuẩn-hóa
   → Chỉ đo "ý nghĩa thống kê"

V7 — χ² × Lift:
   Phiếu(luật) = χ²_chuẩn-hóa × Lift
   → Đo "ý nghĩa thống kê" × "độ tương quan"
   → Luật vừa significant cao + vừa correlated mạnh → nặng cân HƠN

V9 — conf × Lift:
   Phiếu(luật) = confidence × Lift
   → Đo "độ chính xác" × "độ tương quan"
   → Luật vừa precise cao + vừa correlated mạnh → nặng cân HƠN
```

---

## 2. KẾT QUẢ — TRÊN TOÀN BỘ 26 DATASET

| Cấu hình | Avg | vs Paper (85.2%) |
|---|---:|---:|
| Paper CMAR | 85.2% | — |
| Default Improved (χ² thuần) | 85.3% | +0.1% |
| **V7 — χ² × Lift, topK=0** | **85.2%** | **±0.0%** |
| V8 — χ² × Lift, topK=5 | 85.0% | −0.2% |
| **V9 — conf × Lift, topK=0** | **85.2%** | **±0.0%** |
| V10 — conf × Lift, topK=5 | 85.0% | −0.2% |

→ Trên trung bình toàn cục, V7/V9 **bằng paper**, V8/V10 thua chút. Không đột phá.

---

## 3. ⭐ KẾT QUẢ — TRÊN 11 DATA KHÓ (chiều cao, ít mẫu)

| Dataset | Paper | Default Improved | **V7 (χ²×Lift)** | **V9 (conf×Lift)** | V8 (χ²×Lift, k=5) | V10 (conf×Lift, k=5) |
|---|---:|---:|---:|---:|---:|---:|
| Auto | 78.1 | 81.4 | **82.5** | **82.5** | 82.1 | 82.1 |
| Hepatitis | 80.5 | 83.3 | **84.8** | **84.8** | 82.6 | 82.6 |
| Iono | 91.5 | 92.6 | **93.2** | **93.2** | 93.1 | 93.1 |
| Sonar | 79.4 | 80.8 | **81.3** | **81.3** | 80.7 | 80.7 |
| Wine | 95.0 | **96.7** | 95.6 | 95.6 | 96.2 | 96.2 |
| Crx | 84.9 | 86.1 | 85.5 | **85.7** | 86.1 | 86.0 |
| Anneal | 97.3 | **98.2** | 97.9 | 97.9 | 98.1 | 98.1 |
| Breast-Cancer | 96.4 | 97.1 | 97.2 | **97.4** | 97.1 | 97.1 |
| Australian | 86.1 | 86.8 | 86.5 | **86.7** | 86.4 | 86.4 |
| Cleve | 82.2 | 82.6 | 82.6 | **82.6** | 81.6 | 81.9 |
| Labor | 89.7 | 91.7 | **91.7** | **91.7** | 86.7 | 86.7 |
| **Avg 11 hard** | **87.4** | 88.6 | **89.0** ⭐ | **89.0** ⭐⭐ | 88.2 | 88.3 |
| **Δ vs Paper** | — | +1.2% | **+1.6%** | **+1.7%** | +0.9% | +0.9% |

### 🥇 Cấu hình mới TỐT NHẤT trên data khó

**V9: weight = confidence × Lift, topK=0 → +1.7% so với Paper trên 11 data khó**

→ Đây là cải tiến **mới**, vượt qua Default Improved (+1.2%) và Lift-weight trước đó (+1.2%).

---

## 4. PHÂN TÍCH — TẠI SAO V7/V9 THẮNG?

### So với Paper CMAR (vote chỉ bằng χ²)
- χ² đo *liên quan* nhưng có thể bị "thổi phồng" trên mẫu lớn
- Khi nhân thêm Lift → ép luật phải vừa có ý nghĩa **VÀ** vừa tương quan dương mạnh
- Luật giả significant (chi² cao do trùng hợp) bị "đè xuống" vì Lift thấp

### So với chỉ Lift (vote thuần Lift)
- Lift thuần dễ ưu ái luật ít mẫu nhưng tương quan cực mạnh (Lift = 5–10)
- Khi nhân thêm χ²/conf → buộc luật phải có **đủ độ tin cậy** (chi² hoặc confidence cao)
- Giảm noise của Lift ở luật siêu hiếm

### So với V8/V10 (thêm topK=5)
- TopK=5 cắt bớt luật → mất thông tin trên data có nhiều lớp
- Nhất là Labor (chỉ 57 mẫu, 2 lớp): V7/V9 (k=0) = **91.7%**, V8/V10 (k=5) = **86.7%** (−5%)
- → Nhân composite KHÔNG cần cắt thêm topK, vì composite đã ngầm khuếch đại luật tốt

---

## 5. SO SÁNH 3 PHƯƠNG ÁN VOTE — TỔNG KẾT

| Phương án | Avg 26 DS | Avg 11 hard | Ưu điểm | Nhược điểm |
|---|---:|---:|---|---|
| **χ² (Paper)** | 85.2% | 87.4% | Chuẩn paper, ổn định | Yếu trên data nhiễu/chiều cao |
| **Lift thuần** | 85.2% | 88.6% | Tốt trên data khó | Noise ở luật hiếm |
| **V7 χ²×Lift** ⭐ | 85.2% | **89.0%** | Cân bằng significance + correlation | Tính phức tạp hơn |
| **V9 conf×Lift** ⭐⭐ | 85.2% | **89.0%** | Đơn giản hơn V7, hiệu quả nhất | Không có lợi thế trên data dễ |

→ **V9 = đề xuất chính thức cho data khó**.

---

## 6. CÔNG THỨC CUỐI CÙNG EM ĐỀ XUẤT

### Cấu hình mặc định (data thường, 26 bộ)
```
Sắp xếp:     conf DESC → sup DESC → len ASC      (như paper)
Lọc:         χ² ≥ 3.841, conf ≥ 0.5              (như paper)
Vote weight: χ² chuẩn-hóa                        (như paper)
TopK:        0 (tất cả luật khớp)                (như paper)
```
→ Accuracy: 85.3% (vượt paper +0.1%)

### Cấu hình data khó (chiều cao, nhiễu) ⭐
```
Sắp xếp:     conf DESC → sup DESC → len ASC      (giữ paper)
Lọc:         χ² ≥ 3.841, conf ≥ 0.5              (giữ paper)
Vote weight: confidence × Lift                   ← CHỖ THAY ĐỔI
TopK:        0 (tất cả luật khớp)                (giữ paper)
```
→ Accuracy trên 11 data khó: 89.0% (vượt paper +1.7%)

---

## 7. MỘT CÂU CHO CÔ

> *Em cải tiến thuật toán bằng cách thay **trọng số vote** từ "một chỉ số" (χ² hoặc Lift) sang **TÍCH 2 chỉ số** (chi²×Lift hoặc conf×Lift). Em giữ nguyên sắp xếp và lọc theo paper, chỉ đổi cách tính phiếu cho mỗi luật. Kết quả: **cấu hình `--weightConfLift --topK=0` thắng paper +1.7% trên 11 data khó** (Auto, Hepatitis, Iono, Sonar, Wine, Crx, Anneal, Breast-Cancer, Australian, Cleve, Labor), trong đó **Labor +2.0%**, **Hepatitis +4.3%**, **Auto +4.4%**, **Sonar +1.9%**. Đây là cải tiến mới — chưa từng có trong các biến thể trước đó.*

---

## 8. CÁCH TÁI TẠO

```powershell
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

# V7 — χ² × Lift, topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --weightChiLift --topK=0

# V9 — conf × Lift, topK=0  ⭐ ĐỀ XUẤT
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --weightConfLift --topK=0

# V8 — χ² × Lift, topK=5
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --weightChiLift --topK=5

# V10 — conf × Lift, topK=5
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --weightConfLift --topK=5
```

## 9. File số liệu

| Biến thể | File |
|---|---|
| V7 χ²×Lift k=0 | [summary-report-chiLift-topk0.md](summary-report-chiLift-topk0.md) |
| V8 χ²×Lift k=5 | [summary-report-chiLift-topk5.md](summary-report-chiLift-topk5.md) |
| **V9 conf×Lift k=0** ⭐ | [summary-report-confLift-topk0.md](summary-report-confLift-topk0.md) |
| V10 conf×Lift k=5 | [summary-report-confLift-topk5.md](summary-report-confLift-topk5.md) |
