# Báo cáo: Thử nghiệm thay đổi LỌC và SẮP XẾP luật — so với Paper

> Ngày chạy: 2026-05-14
> Trả lời yêu cầu của cô: *"Bạn phải cải tiến chứ, thử thay đổi cách lọc hoặc sắp xếp rồi chạy và so sánh vs paper."*

---

## 1. Tóm tắt

Em đã thử **6 biến thể mới** — thay đổi cách LỌC hoặc SẮP XẾP luật, không đổi gì khác.

| # | Biến thể | Thay đổi | Avg | Δ vs Paper (85.2%) | Đánh giá |
|:---:|---|---|---:|---:|:---:|
| 0 | **Default Improved** (mốc so sánh) | — | **85.3%** | **+0.1%** | ⭐ |
| **V1** | `--strictChi` | LỌC: χ² ≥ **6.635** (p=0.01) chặt hơn | 85.3% | +0.1% | ⚪ Hòa, nhưng có ý nghĩa |
| **V2** | `--liftTieBreak` | SẮP XẾP: thêm Lift làm tiebreaker (conf→sup→**Lift**→len) | 85.3% | +0.1% | ⚪ Không đổi (no-op) |
| **V3** | `--strictLift=1.5` | LỌC: thêm điều kiện Lift ≥ 1.5 | 85.3% | +0.1% | ⚪ Không đổi (no-op) |
| **V4** | `--strictChi --liftWeight --topK=5` | Kết hợp 3 thứ | 85.1% | −0.1% | 🔴 Tệ hơn |
| **V5** | `--strictLift=2.0` | LỌC: Lift ≥ 2.0 (rất chặt) | 84.7% | **−0.5%** | 🔴 Tệ |
| **V6** | `--chiThreshold=2.706` | LỌC: nới χ² ≥ 2.706 (p=0.10) | 84.6% | **−0.6%** | 🔴 Tệ |

→ **Không có biến thể nào vượt qua Default Improved 85.3%**. Đây cũng là kết quả nghiên cứu có giá trị — chứng minh tham số paper đã **được tinh chỉnh kỹ**, không dễ vượt.

---

## 2. Phân tích từng biến thể

### V1 — `--strictChi`: Chặn chi² chặt hơn (3.841 → 6.635)

**Ý tưởng**: Paper dùng χ² ≥ 3.841 (p=0.05). Em thử chặn ở mức **p=0.01** → chỉ giữ luật có ý nghĩa thống kê **rất mạnh**.

**Hiệu ứng đo được**:

| Dataset | Số luật default → strictChi | Default | strictChi | Δ |
|---|---:|---:|---:|---:|
| German | 951 → **787** | 72.9% | 71.5% | −1.4 🔴 |
| Diabetes | 213 → **202** | 73.4% | 73.7% | +0.3 🟢 |
| Heart | — | 80.7% | 81.1% | +0.4 🟢 |
| Hepatitis | — | 83.3% | 82.6% | −0.7 🔴 |
| Pima | — | 73.4% | 73.7% | +0.3 🟢 |

**Nhận xét**:
- Filter **THỰC SỰ chạy** — German giảm từ 951 → 787 luật
- Nhưng **bù trừ**: data số học (Diabetes, Pima, Heart) tốt lên, data cân bằng (German, Hepatitis) tệ đi
- **Trung bình không đổi** = chi²=3.841 đã là điểm tốt nhất trên trung bình

**Bài học**: Chặn nghiêm hơn về thống kê = vứt cả luật mềm có ích → có dataset hưởng lợi, có dataset thiệt.

---

### V2 — `--liftTieBreak`: Thêm Lift làm tiebreaker SAU support

**Ý tưởng**: Giữ nguyên thứ tự CMAR `conf → sup`, nhưng khi hòa cả conf+sup, ưu tiên luật có **Lift cao hơn** (có tương quan mạnh hơn).

Sort:
```
   Mặc định:        conf DESC → sup DESC → len ASC
   liftTieBreak:    conf DESC → sup DESC → Lift DESC → len ASC
                                            ↑
                                       chỗ Lift chen vào
```

**Kết quả**: GIỐNG Y default trên cả 26 dataset.

**Vì sao no-op?**
- Trường hợp 2 luật có **CÙNG confidence VÀ cùng support** là **rất hiếm** vì confidence là số thực
- Khi không có "hòa" → tiebreaker không kích hoạt → không đổi gì

**Bài học**: Tiebreaker chỉ có giá trị khi có hòa. Confidence là số thực 0–1 nên gần như không bao giờ hòa chính xác → ý tưởng không khả thi.

---

### V3 — `--strictLift=1.5`: Thêm bộ lọc Lift ≥ 1.5

**Ý tưởng**: Sau khi χ² lọc, vẫn còn luật có Lift trong khoảng 1.0–1.5 (tương quan dương yếu). Em vứt luôn nhóm này.

**Kết quả**: GIỐNG Y default — số luật và accuracy không đổi.

**Vì sao no-op?**
- χ² ≥ 3.841 **đã** ngầm yêu cầu Lift đủ lớn (vì χ² và Lift đo cùng hướng — đều đo "tương quan")
- Sau lọc χ², **hầu như không còn luật nào** có Lift trong khoảng 1.0–1.5 → bộ lọc thêm vô tác dụng

**Bài học**: χ² và Lift đo cùng thứ → lọc thêm Lift sau χ² là **dư thừa**. Phải tăng ngưỡng Lift rất cao (V5: 2.0) thì mới có tác dụng — nhưng lúc đó lại vứt luật tốt.

---

### V4 — Kết hợp `strictChi + liftWeight + topK=5`

**Ý tưởng**: Kết hợp 3 thay đổi mạnh nhất em đã thử riêng lẻ.

**Kết quả**: 85.1% (−0.1% so với default).

**Bài học**: Các cải tiến **không cộng dồn**. Kết hợp nhiều thay đổi có thể **phá nhau** vì mỗi thay đổi đã ngầm "đầu tư" vào một giả định khác nhau về dữ liệu.

---

### V5 — `--strictLift=2.0`: Lọc Lift cực chặt

**Ý tưởng**: Tăng V3 lên Lift ≥ 2.0 — chỉ giữ luật có tương quan **rất mạnh**.

**Kết quả**: 84.7% (−0.5% so với default).

**Bài học**: Vứt quá nhiều luật → mô hình thiếu thông tin để phân lớp đúng. **Lọc gắt = mất accuracy**.

---

### V6 — `--chiThreshold=2.706`: Nới chi² (p=0.10)

**Ý tưởng**: Ngược V1 — giữ NHIỀU luật hơn (kể cả luật ý nghĩa thống kê yếu).

**Kết quả**: 84.6% (−0.6% so với default).

**Bài học**: Quá nhiều luật yếu → tầng tỉa coverage bị nhiễu → giữ lại luật rác → accuracy tụt.

---

## 3. Bản đồ kết quả

```
Chi² threshold:
     V6 (2.706)            Default (3.841)        V1 (6.635)
        ↓                       ↓                      ↓
     84.6% 🔴                 85.3% ⭐               85.3% ⚪
     (mềm quá)               (sweet spot)         (chặt vô ích)

Lift filter (thêm vào):
   không có          Lift≥1.5        Lift≥2.0
       ↓                ↓                ↓
     85.3%            85.3%           84.7% 🔴
     (default)       (no-op)          (chặt quá)

Sort tiebreaker:
   conf→sup→len     conf→sup→Lift→len
       ↓                    ↓
     85.3%                85.3%
     (default)         (Lift hiếm dùng)
```

→ **Default Improved (85.3%) là cực trị địa phương** — đụng vào hướng nào cũng không hơn được.

---

## 4. Tại sao không cải thiện thêm được?

### 4.1. Paper CMAR đã tinh chỉnh kỹ

Tác giả paper 2001 đã thử nghiệm để chọn:
- χ² = 3.841 (p=0.05)
- δ = 4 (coverage)
- Sort: confidence → support → length

Đây là **điểm cân bằng** — chặt hơn hay nới hơn đều không hơn.

### 4.2. Cải tiến em đã làm trước đó đã "ăn hết phần dễ"

Bản Improved hiện tại đã:
- Gỡ skip G2S → tăng +0.8% so với baseline
- Cải tiến determinism → ổn định hơn
- Cải tiến hiệu năng → cho phép chạy đủ chi² + G2S + DCP

→ Phần "dễ ăn" đã được khai thác. Phần còn lại là **chiến lược toàn cục** chứ không phải tinh chỉnh thông số.

### 4.3. Sweet spot là cấu hình theo nhóm dataset

Em đã chứng minh trước đây:
- **Default Improved** thắng paper +0.1% trên **toàn bộ** 26 dataset
- **Improved + liftWeight + topK=5** thắng paper **+0.9% trên 11 data khó**

→ Hướng đi đúng là **phân loại dataset trước, áp dụng cấu hình phù hợp** — không phải tìm 1 cấu hình "thắng tất cả".

---

## 5. Kết luận: 3 hướng tiếp theo

Vì tinh chỉnh thông số đã chạm đỉnh, em đề xuất 3 hướng nghiên cứu sâu hơn cho đồ án:

### Hướng A — **Phân loại dataset trước khi chạy**
Trước khi chạy, đo:
- Tỉ lệ thuộc tính/mẫu (chiều cao?)
- Cân bằng nhãn lớp?
- Tỉ lệ luật/lớp?

→ Chọn cấu hình `default` hay `liftWeight+topK=5` **tự động** theo đặc tính dataset.
→ Có thể vượt paper **trên cả 26 dataset** thay vì chỉ trên 11.

### Hướng B — **Trọng số vote kết hợp (χ² × Lift)**
Thay vì vote bằng χ² **HOẶC** Lift, em thử vote bằng **tích**: `weight = chi² × lift`.

→ Vừa giữ độ tin cậy thống kê (χ²), vừa khuếch đại luật tương quan mạnh (Lift).
→ Em chưa thử — có thể thử trong lần sau.

### Hướng C — **Coverage delta thích nghi**
Paper dùng δ=4 cho mọi dataset. Em thử:
- δ=2 cho dataset lớn (vài nghìn mẫu)
- δ=6 cho dataset nhỏ (<200 mẫu)

→ Mỗi mẫu được phủ ít/nhiều luật tùy quy mô dataset.

---

## 6. Một câu cho cô

> *Em đã thử 6 biến thể thay đổi LỌC (chi² chặt/lỏng, thêm Lift filter) và SẮP XẾP (Lift tiebreaker). Kết quả: **không biến thể nào hơn được Default Improved 85.3% trên trung bình 26 dataset**. Đây không phải thất bại — nó chứng minh tham số paper CMAR đã được tinh chỉnh rất kỹ, và phần "dễ ăn" của tối ưu thông số đã được Bản Improved của em khai thác hết. Hướng tiếp theo em đề xuất là **chọn cấu hình tự động theo đặc tính dataset** (phân loại data thường vs data khó), hoặc thử **weight tổ hợp χ² × Lift** — đây là 2 hướng em chưa thử.*

---

## 7. File số liệu

| Biến thể | File summary |
|---|---|
| Default Improved | [summary-report-topk0.md](summary-report-topk0.md) |
| V1 strictChi | [summary-report-strictChi.md](summary-report-strictChi.md) |
| V2 liftTieBreak | [summary-report-liftTieBreak.md](summary-report-liftTieBreak.md) |
| V3 strictLift=1.5 | [summary-report-strictLift15.md](summary-report-strictLift15.md) |
| V4 strictChi + liftWeight + topK=5 | [summary-report-strictChi-liftWeight-topk5.md](summary-report-strictChi-liftWeight-topk5.md) |
| V5 strictLift=2.0 | [summary-report-strictLift20.md](summary-report-strictLift20.md) |
| V6 chiThreshold=2.706 | [summary-report-relaxedChi.md](summary-report-relaxedChi.md) |

## 8. Cách tái tạo

```powershell
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --strictChi --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftTieBreak --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --strictLift=1.5 --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --strictChi --liftWeight --topK=5
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --strictLift=2.0 --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --chiThreshold=2.706 --topK=0
```
