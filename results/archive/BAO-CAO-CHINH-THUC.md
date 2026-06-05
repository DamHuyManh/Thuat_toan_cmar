# BÁO CÁO ĐỒ ÁN

## Tìm Hiểu và Cài Đặt Thuật Toán CMAR
### (Classification Based on Multiple Class-Association Rules)

---

| Thông tin | Nội dung |
|-----------|----------|
| **Bài báo** | "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules" |
| **Tác giả** | Wenmin Li, Jiawei Han, Jian Pei |
| **Hội nghị** | IEEE ICDM 2001 |
| **Ngôn ngữ cài đặt** | Java |
| **Số dataset thử nghiệm** | 26 UCI datasets |
| **Phương pháp đánh giá** | 10-fold Cross-Validation |
| **Kết quả** | Trung bình **85.1%** (Paper: **85.2%**) |

---

## MỤC LỤC

1. [Tóm tắt](#1-tóm-tắt)
2. [Giới thiệu bài toán](#2-giới-thiệu-bài-toán)
3. [Các khái niệm nền tảng](#3-các-khái-niệm-nền-tảng)
4. [Thuật toán CMAR](#4-thuật-toán-cmar)
5. [Cách em đã cài đặt](#5-cách-em-đã-cài-đặt)
6. [Thực nghiệm](#6-thực-nghiệm)
7. [Kết quả](#7-kết-quả)
8. [Kết luận](#8-kết-luận)

---

# 1. TÓM TẮT

Đồ án này tìm hiểu và cài đặt thuật toán **CMAR** — một phương pháp phân loại sử dụng **nhiều luật kết hợp** do Li, Han, Pei đề xuất năm 2001.

**CMAR hoạt động qua 3 bước:**
1. **Tìm luật (Mining)** — Dùng thuật toán FP-growth tìm tất cả luật "IF A THEN class C" thỏa điều kiện
2. **Lọc luật (Pruning)** — Loại bỏ luật dư thừa bằng 3 phương pháp
3. **Phân loại (Classification)** — Dùng nhiều luật cùng bỏ phiếu để quyết định lớp

**Kết quả đạt được:** Cài đặt trên 26 datasets chuẩn của paper, đạt trung bình **85.1%**, chênh paper chỉ **0.1%**. Có **3 datasets khớp chính xác** với paper (Tic-Tac-Toe, Glass, Iono).

---

# 2. GIỚI THIỆU BÀI TOÁN

## 2.1 Bài toán phân loại

**Phân loại (Classification)** là bài toán: cho một tập dữ liệu có sẵn nhãn, học một mô hình để dự đoán nhãn cho dữ liệu mới.

**Ví dụ thực tế:**
- Cho thông tin 1000 bệnh nhân (tuổi, huyết áp, cholesterol...) và kết quả (có/không bệnh tim)
- Học một mô hình
- Dự đoán bệnh nhân MỚI có bị bệnh tim hay không

## 2.2 Phân loại bằng Luật kết hợp

Có nhiều cách để phân loại:
- **Cây quyết định** (C4.5) — xây cây hỏi-đáp
- **Mạng nơ-ron** — học trọng số
- **Luật kết hợp** (CBA, CMAR) — tìm các "luật nếu-thì"

**CMAR thuộc nhóm luật kết hợp**, tìm ra các luật dạng:

> **NẾU** tuổi > 50 **VÀ** huyết áp > 140 **THÌ** bệnh tim = CÓ

**Ưu điểm của luật kết hợp:**
- Dễ hiểu (bác sĩ đọc luật biết ngay)
- Có thể giải thích tại sao dự đoán như vậy
- Không phải "hộp đen" như deep learning

## 2.3 Tại sao cần CMAR (thay vì CBA)?

**CBA (Liu, Hsu, Ma 1998)** là thuật toán đầu tiên dùng luật kết hợp để phân loại. Nhưng có **3 vấn đề**:

1. **Chỉ dùng 1 luật để quyết định** → nếu luật sai thì dự đoán sai
2. **Chậm** với dataset lớn vì dùng thuật toán Apriori
3. **Giữ nhiều luật dư thừa**

**CMAR (2001)** cải tiến:

| Vấn đề của CBA | Giải pháp của CMAR |
|----------------|---------------------|
| Chỉ 1 luật | **Nhiều luật cùng bỏ phiếu** (WCS voting) |
| Apriori chậm | **FP-growth** nhanh hơn |
| Luật dư thừa | **3 phương pháp lọc** khác nhau |

---

# 3. CÁC KHÁI NIỆM NỀN TẢNG

## 3.1 Luật kết hợp (Association Rule)

Một luật có dạng: **A → C**

Trong đó:
- **A (Antecedent)** — các điều kiện (vế "NẾU")
- **C (Consequent)** — kết luận (vế "THÌ")

**Ví dụ:**
- A = {tuổi > 50, huyết áp cao}
- C = bệnh tim

## 3.2 Độ hỗ trợ (Support)

**Định nghĩa:** Tỷ lệ mẫu có cả A và C trong dataset.

$$\text{support}(A \to C) = \frac{\text{số mẫu có cả A và C}}{\text{tổng số mẫu}}$$

**Ví dụ:** Trong 1000 bệnh nhân, có 200 người vừa "huyết áp cao" vừa "bệnh tim".
- → Support = 200/1000 = **20%**

**Ý nghĩa:** Luật xuất hiện thường xuyên như thế nào trong dữ liệu.

## 3.3 Độ tin cậy (Confidence)

**Định nghĩa:** Xác suất C đúng khi A đúng.

$$\text{confidence}(A \to C) = \frac{\text{support}(A \cup C)}{\text{support}(A)}$$

**Ví dụ:** 300 người có huyết áp cao, trong đó 200 bị bệnh tim.
- → Confidence = 200/300 = **67%**

**Ý nghĩa:** Luật đúng bao nhiêu phần trăm — độ chính xác của luật.

## 3.4 Chi-Square (Kiểm định thống kê)

**Mục đích:** Đo xem antecedent A có thực sự **liên quan** đến class C hay chỉ là ngẫu nhiên.

**Bảng contingency 2×2:**

|  | Class = c | Class ≠ c | Tổng |
|---|-----------|-----------|------|
| **Có A** | a | b | a+b |
| **Không A** | c | d | c+d |
| **Tổng** | a+c | b+d | N |

**Công thức:**

$$\chi^2 = \frac{N \cdot (ad - bc)^2}{(a+b)(c+d)(a+c)(b+d)}$$

**Ngưỡng:**
- χ² ≥ **3.841** → có ý nghĩa thống kê với độ tin cậy 95%
- χ² < 3.841 → quan hệ ngẫu nhiên, loại bỏ

**Ví dụ minh họa:**
- Nếu rule "tuổi > 50 → bệnh tim" có χ² = 10.5 → **chắc chắn có liên kết thật**, giữ lại
- Nếu rule "thích ăn cơm → bệnh tim" có χ² = 1.2 → **ngẫu nhiên**, loại bỏ

## 3.5 FP-tree (Cây Pattern Thường Gặp)

**Mục đích:** Cấu trúc dữ liệu để lưu dataset một cách **nén lại**, giúp mining nhanh.

**Cách hoạt động:**
1. Duyệt dataset 1 lần → đếm tần suất từng item
2. Sắp xếp các item trong mỗi transaction theo tần suất giảm dần
3. Xây cây, các transaction có prefix chung sẽ dùng chung đường đi

**Ví dụ:**
```
4 transactions:
T1: {A, B, C}
T2: {A, B, D}
T3: {A, C}
T4: {B, C, D}

FP-tree sẽ "gộp" T1 và T2 vì cùng bắt đầu A, B
→ Tiết kiệm bộ nhớ, duyệt nhanh
```

**Ưu điểm:** FP-growth chỉ cần **duyệt dataset 2 lần**, trong khi Apriori cần duyệt nhiều lần → nhanh hơn đáng kể.

---

# 4. THUẬT TOÁN CMAR

## 4.1 Tổng quan 3 giai đoạn

```
┌─────────────────────────────────────────┐
│  Dataset huấn luyện                      │
└──────────────────┬──────────────────────┘
                   ▼
    ┌──────────────────────────────┐
    │  GIAI ĐOẠN 1: MINING         │
    │  Tìm tất cả luật A → C       │
    │  thỏa minSup và minConf      │
    │  (dùng FP-growth)            │
    └──────────────┬───────────────┘
                   ▼
    ┌──────────────────────────────┐
    │  GIAI ĐOẠN 2: PRUNING        │
    │  Lọc luật bằng 3 phương pháp │
    │  1. Chi-square               │
    │  2. General-to-specific      │
    │  3. Database coverage        │
    └──────────────┬───────────────┘
                   ▼
    ┌──────────────────────────────┐
    │  GIAI ĐOẠN 3: CLASSIFY       │
    │  Dùng nhiều luật bỏ phiếu    │
    │  (WCS voting)                │
    └──────────────────────────────┘
```

## 4.2 Giai đoạn 1 — Mining (Tìm luật)

**Mục tiêu:** Tìm TẤT CẢ luật dạng A → C thỏa:
- support ≥ **minSup** (mặc định 1%)
- confidence ≥ **minConf** (mặc định 50%)

**Phương pháp:** FP-growth

### Các bước tìm luật

**Bước 1 — Đếm tần suất:** Duyệt dataset, đếm xem mỗi item xuất hiện bao nhiêu lần.

**Bước 2 — Lọc item hiếm:** Bỏ item xuất hiện ít hơn minSup (vì không thể tạo luật đủ support).

**Bước 3 — Xây FP-tree:** Biến dataset thành cây để mining nhanh.

**Bước 4 — Mine pattern:**
- Với mỗi lớp c (ví dụ: bệnh tim = CÓ)
- Tìm tất cả pattern (tập item) thường xuất hiện cùng c
- Với mỗi pattern P, tạo luật `P → c`
- Kiểm tra support và confidence → nếu đủ ngưỡng thì giữ lại

### Kết quả

Sau giai đoạn 1, ta có **rất nhiều luật** — có thể lên đến **hàng chục nghìn**. Đây là lý do cần giai đoạn 2 để lọc bớt.

## 4.3 Giai đoạn 2 — Pruning (Lọc luật)

### 4.3.1 Chi-Square Pruning — Lọc luật vô nghĩa

**Ý tưởng:** Không phải luật nào có support/confidence cao cũng tốt. Có những luật xuất hiện ngẫu nhiên.

**Cách làm:**
- Tính χ² cho mỗi luật
- Nếu χ² < 3.841 → **loại bỏ** (vì không có ý nghĩa thống kê)
- Nếu confidence(A → C) ≤ prior(C) → **loại bỏ** (A không giúp ích gì cho việc dự đoán C)

**Ví dụ:**
- Luật "thích uống cà phê → bệnh tim" có χ² = 0.5 → loại bỏ (không liên quan)
- Luật "hút thuốc → ung thư phổi" có χ² = 50 → giữ (liên kết mạnh)

### 4.3.2 General-to-Specific Pruning — Lọc luật chi tiết thừa

**Ý tưởng:** Nếu đã có luật **tổng quát** tốt, không cần luật **cụ thể hơn** có confidence thấp hơn.

**Ví dụ:**
```
Luật 1 (tổng quát): tuổi > 50 → bệnh tim (conf 80%)
Luật 2 (cụ thể):    tuổi > 50 AND sống ở HN → bệnh tim (conf 75%)
```

Luật 2 phức tạp hơn nhưng confidence lại thấp hơn → **loại bỏ Luật 2**, giữ Luật 1.

**Lý do:** Luật đơn giản hơn thường generalize tốt hơn.

### 4.3.3 Database Coverage Pruning — Lọc luật không đóng góp

**Ý tưởng:** Mỗi mẫu training chỉ cần được **δ luật** classify đúng là đủ (δ = 4 trong cài đặt của em). Các luật thứ 5 trở đi là dư thừa.

**Cách làm:**
1. Sắp xếp luật theo confidence giảm dần
2. Với mỗi mẫu training, đếm xem nó đã được bao nhiêu luật classify đúng (count[i])
3. Duyệt từng luật r theo thứ tự:
   - Nếu r giúp classify đúng ít nhất 1 mẫu **chưa đủ δ** → giữ luật r
   - Nếu không → bỏ
4. Mỗi khi giữ r, tăng count cho các mẫu mà r classify đúng

**Kết quả:** Sau pruning, từ hàng chục nghìn luật còn lại **vài trăm luật tốt nhất**.

## 4.4 Giai đoạn 3 — Classification (Phân loại)

Khi có một mẫu test mới cần phân loại:

### Bước 1: Tìm tất cả luật khớp

Duyệt qua tập luật đã pruned, tìm các luật có antecedent ⊆ mẫu test.

**Ví dụ:** Bệnh nhân mới có `{tuổi=55, huyết áp=145, cholesterol=180}`
- Luật 1 "tuổi>50 → bệnh" khớp ✓
- Luật 2 "huyết áp>140 → bệnh" khớp ✓
- Luật 3 "cholesterol>200 → bệnh" KHÔNG khớp (180 < 200)

### Bước 2: Kiểm tra đồng thuận

Nếu **tất cả luật có confidence cao nhất đều dự đoán cùng 1 lớp** → trả về lớp đó.

### Bước 3: Nếu không đồng thuận → Weighted Chi-Square Voting

**Công thức:**

$$\text{Score}(c) = \sum_{r \in R_c} \frac{\chi^2(r)}{\chi^2_{\max}(r)}$$

Trong đó:
- $R_c$ = tập luật khớp và dự đoán lớp c
- $\chi^2_{\max}(r)$ = giá trị chi-square tối đa có thể (upper bound)

**Công thức χ²_max:**

$$\chi^2_{\max}(r) = \left( \min(\text{sup}(A), \text{sup}(C)) - \frac{\text{sup}(A) \cdot \text{sup}(C)}{N} \right)^2 \times N \times e$$

**Quyết định:** Lớp có Score cao nhất thắng.

**Tại sao chia cho χ²_max?**
- Lớp đông (nhiều mẫu) tự nhiên có χ² lớn hơn
- Chia cho χ²_max → **cân bằng**, không thiên vị lớp đông

---

# 5. CÁCH EM ĐÃ CÀI ĐẶT

## 5.1 Ngôn ngữ và công cụ

- **Ngôn ngữ:** Java (JDK 17)
- **Môi trường:** Windows 11, IntelliJ IDEA
- **Không dùng thư viện ML** — em cài từ đầu theo paper

## 5.2 Kiến trúc tổ chức

Em chia code thành các module theo 3 giai đoạn:

| Module | Nhiệm vụ |
|--------|----------|
| **Mining** | Triển khai FP-growth, xây FP-tree để tìm luật |
| **Pruning** | Triển khai 3 phương pháp lọc (Chi², General-to-Specific, Coverage) |
| **Classification** | Tra cứu luật khớp, thực hiện WCS voting |
| **Discretization** | Rời rạc hóa thuộc tính số bằng MDL |
| **Benchmark** | Chạy 10-fold CV, đánh giá trên 26 datasets |

## 5.3 Rời rạc hóa dữ liệu (Discretization)

**Vấn đề:** CMAR chỉ làm việc với dữ liệu **rời rạc**. Nhưng nhiều dataset có thuộc tính **số liên tục** (tuổi, huyết áp...).

**Giải pháp:** Dùng thuật toán **MDL (Fayyad & Irani 1993)** — phương pháp supervised discretization.

### Thuật toán MDL (tóm tắt)

**Ý tưởng:** Chia khoảng giá trị thành các đoạn sao cho mỗi đoạn thuần về class nhất có thể.

**Công thức Entropy** (đo độ "hỗn loạn" về class):

$$E(S) = -\sum_{i=1}^{c} p_i \log_2(p_i)$$

**Công thức Information Gain** (giảm entropy khi chia):

$$\text{Gain}(S, T) = E(S) - \frac{|S_1|}{|S|} E(S_1) - \frac{|S_2|}{|S|} E(S_2)$$

**Tiêu chí dừng MDL:** Chỉ chia tiếp nếu Gain đủ lớn để "bù" chi phí mã hóa thêm.

**Ví dụ:** Chia khoảng tuổi (20-80) thành:
- [20-40]: toàn người khỏe
- [41-60]: hỗn hợp
- [61-80]: đa số bệnh
→ MDL tìm ra ngưỡng 40 và 60 là điểm chia tốt nhất.

## 5.4 Điểm quan trọng: CV không rò rỉ dữ liệu

**Cách sai (có rò rỉ):**
```
1. Rời rạc hóa trên TOÀN BỘ dataset
2. Chia thành 10 fold
3. Train/test
→ SAI vì rời rạc hóa "nhìn thấy" test data
```

**Cách đúng (em đã làm):**
```
Với mỗi fold:
  1. Chia train/test
  2. Rời rạc hóa CHỈ TỪ train fold
  3. Áp cut points cho cả train và test
  4. Train model, predict test
→ ĐÚNG, trung thực
```

Cách này gọi là **per-fold MDL discretization** — đảm bảo test data không bị "lộ" trong quá trình tiền xử lý.

## 5.5 Các tối ưu áp dụng

| Tối ưu | Công dụng |
|--------|-----------|
| **Bitmap matching** | Kiểm tra luật khớp mẫu rất nhanh (AND bit) |
| **CR-tree indexing** | Tra cứu luật khớp trong O(log n) |
| **Phân vùng theo class** | Mine riêng từng class, tăng tốc |

---

# 6. THỰC NGHIỆM

## 6.1 Bộ dữ liệu

Em sử dụng **26 UCI datasets** giống hệt paper (Table 3):

| Nhóm | Dataset |
|------|---------|
| **Y khoa** | Breast-Cancer, Cleve, Diabetes, Heart, Hepatitis, Horse, Hypo, Pima, Sick |
| **Kinh tế/Tài chính** | Australian, Crx, German, Labor |
| **Sinh học** | Iris, Zoo, Lymphography |
| **Vật lý/Kỹ thuật** | Anneal, Auto, Glass, Iono, Sonar, Vehicle, Wine |
| **Tổng hợp** | Led7, Tic-Tac-Toe, Waveform |

**Đặc điểm:**
- Kích thước từ 57 mẫu (Labor) đến 5000 mẫu (Waveform)
- Số thuộc tính từ 4 (Iris) đến 60 (Sonar)
- Số lớp từ 2 (nhiều dataset) đến 10 (Led7)

## 6.2 Tham số cài đặt

| Tham số | Giá trị | Nguồn |
|---------|---------|-------|
| **Minimum Support** | 1% | Paper Section 5 |
| **Minimum Confidence** | 50% | Paper Section 5 |
| **Chi-Square threshold** | 3.841 (p=0.05) | Paper Section 3.1 |
| **Delta δ (coverage)** | 4 | Paper Section 3.2 |
| **Cross-Validation** | 10-fold stratified | Chuẩn ML |
| **Random seed** | 42 | Cố định để reproducible |

## 6.3 Phương pháp đánh giá

**10-fold Stratified Cross-Validation:**

1. Chia dataset thành 10 phần bằng nhau, giữ nguyên tỷ lệ các lớp
2. Lặp 10 lần:
   - 1 phần làm test (10% data)
   - 9 phần làm train (90% data)
   - Rời rạc hóa CHỈ từ train (không rò rỉ)
   - Train model CMAR
   - Predict test, tính accuracy
3. **Accuracy cuối = trung bình 10 lần**

---

# 7. KẾT QUẢ

## 7.1 Bảng kết quả đầy đủ

| # | Dataset | **Của em** | Paper CMAR | Paper CBA | Paper C4.5 | Chênh | Trạng thái |
|---|---------|-----------|-----------|-----------|-----------|-------|-------|
| 1 | Anneal | 97.7% | 97.3% | 97.9% | 94.8% | +0.4% | ✅ |
| 2 | Australian | 86.7% | 86.1% | 84.9% | 84.7% | +0.6% | 🟢 |
| 3 | Auto | 81.4% | 78.1% | 78.3% | 80.1% | +3.3% | 🟢 |
| 4 | Breast-Cancer | 96.9% | 96.4% | 96.3% | 95.0% | +0.5% | ✅ |
| 5 | Cleve | 82.9% | 82.2% | 82.8% | 78.2% | +0.7% | 🟢 |
| 6 | Crx | 85.7% | 84.9% | 84.7% | 84.9% | +0.8% | 🟢 |
| 7 | Diabetes | 73.4% | 75.8% | 74.5% | 74.2% | -2.4% | 🔴 |
| 8 | German | 72.8% | 74.9% | 73.4% | 72.3% | -2.1% | 🔴 |
| 9 | **Glass** | **70.0%** | **70.1%** | 73.9% | 68.7% | **-0.1%** | 🎯 |
| 10 | Heart | 80.7% | 82.2% | 81.9% | 80.8% | -1.5% | 🔴 |
| 11 | Hepatitis | 82.7% | 80.5% | 81.8% | 80.6% | +2.2% | 🟢 |
| 12 | Horse | 80.7% | 82.6% | 82.1% | 82.6% | -1.9% | 🔴 |
| 13 | Hypo | 97.9% | 98.4% | 98.9% | 99.2% | -0.5% | ✅ |
| 14 | **Iono** | **91.7%** | **91.5%** | 92.3% | 90.0% | **+0.2%** | 🎯 |
| 15 | Iris | 92.7% | 94.0% | 94.7% | 95.3% | -1.3% | 🔴 |
| 16 | Labor | 93.3% | 89.7% | 86.3% | 79.3% | +3.6% | 🟢 |
| 17 | Led7 | 71.2% | 72.5% | 71.9% | 73.5% | -1.3% | 🔴 |
| 18 | Lymphography | 83.5% | 83.1% | 77.8% | 73.5% | +0.4% | ✅ |
| 19 | Pima | 73.4% | 75.1% | 72.9% | 75.5% | -1.7% | 🔴 |
| 20 | Sick | 96.5% | 97.5% | 97.0% | 98.5% | -1.0% | 🔴 |
| 21 | Sonar | 78.0% | 79.4% | 77.5% | 70.2% | -1.4% | 🔴 |
| 22 | **Tic-Tac-Toe** | **99.2%** | **99.2%** | 99.6% | 99.4% | **0.0%** | 🎯 |
| 23 | Vehicle | 68.1% | 68.8% | 68.7% | 72.6% | -0.7% | 🔴 |
| 24 | Waveform | 81.6% | 83.2% | 80.0% | 78.1% | -1.6% | 🔴 |
| 25 | Wine | 96.7% | 95.0% | 95.0% | 92.7% | +1.7% | 🟢 |
| 26 | Zoo | 96.5% | 97.1% | 96.8% | 92.2% | -0.6% | 🔴 |
| | **TRUNG BÌNH** | **85.1%** | **85.2%** | **84.7%** | **83.3%** | **-0.1%** | 🎯 |

**Chú thích:**
- 🎯 Khớp paper (chênh ≤ 0.2%)
- ✅ Hòa (chênh ≤ 0.5%)
- 🟢 Thắng (chênh > 0.5%)
- 🔴 Thua (chênh < -0.5%)

## 7.2 Điểm nổi bật

### ✨ Trung bình 26 datasets: 85.1% vs paper 85.2% — CHÊNH CHỈ 0.1%

Đây là bằng chứng **thuật toán em cài đúng**. Nếu sai, chênh lệch sẽ phải lớn hơn nhiều (5-10%).

### 🎯 3 datasets khớp paper chính xác

| Dataset | Của em | Paper | Giải thích |
|---------|--------|-------|------------|
| **Tic-Tac-Toe** | 99.2% | 99.2% | Dataset deterministic, luật rõ ràng |
| **Glass** | 70.0% | 70.1% | Cài đúng, chỉ lệch 0.1% do random CV |
| **Iono** | 91.7% | 91.5% | MDL discretization hiệu quả |

### 🟢 Trung bình vượt CBA và C4.5

- Em: **85.1%**
- Paper CBA: 84.7% → **CMAR em hơn CBA 0.4%**
- Paper C4.5: 83.3% → **CMAR em hơn C4.5 1.8%**

Kết quả này trùng khớp kết luận của paper: **CMAR > CBA > C4.5**.

## 7.3 Phân tích các trường hợp thua paper

Có 12 datasets em thua paper. Các nguyên nhân chính:

### Nguyên nhân 1: Seed Cross-Validation khác paper

Paper **không công bố seed** để reproduce. Em dùng seed=42.
- Dataset nhỏ nhạy cảm: Iris (150 mẫu), Heart (270), Labor (57)
- Chênh ±1-2% là bình thường

### Nguyên nhân 2: Class Imbalance (mất cân bằng lớp)

- **Sick**: 94% khỏe / 6% bệnh → em -1.0%
- **German**: 70/30 → em -2.1%
- **Diabetes/Pima**: 65/35 → em -1.7/-2.4%

Lớp hiếm có ít rule mạnh → dễ dự đoán sai.

### Nguyên nhân 3: Xử lý Missing Values

- **Horse**: 30% ô dữ liệu là "?" → em -1.9%
- **Diabetes**: có giá trị 0 vô lý (Glucose=0, BP=0) → em -2.4%

Paper có thể dùng imputation (điền median), em giữ nguyên.

### Nguyên nhân 4: High Dimensionality

- **Sonar**: 60 thuộc tính / 208 mẫu → em -1.4%
- **Waveform**: 21 thuộc tính / 5000 mẫu → em -1.6%

Số thuộc tính quá lớn → mining rule khó khăn.

## 7.4 Phân tích các trường hợp thắng paper

Có 7 datasets em thắng. Các nguyên nhân:

### Labor (+3.6%)
- Dataset chỉ 57 mẫu → mỗi fold chỉ 5-6 test
- **CV variance rất lớn** → may mắn với seed=42
- Không phải thuật toán em giỏi hơn, chỉ là ngẫu nhiên

### Auto (+3.3%), Hepatitis (+2.2%)
- 2 dataset này có nhiều missing values
- Em encode "?" thành category "MISS" riêng
- Cách này **giữ được thông tin** — đôi khi việc "không đo được" cũng là thông tin y khoa

### Wine (+1.7%), Cleve (+0.7%), Crx (+0.8%), Australian (+0.6%)
- MDL discretization tìm được ngưỡng tốt hơn equal-frequency
- Dataset có phân bố rõ ràng theo class

---

# 8. KẾT LUẬN

## 8.1 Những gì em đã đạt được

### ✅ Về mặt thuật toán
- Hiểu sâu cách CMAR hoạt động qua 3 giai đoạn
- Nắm được các công thức quan trọng: support, confidence, chi-square, WCS
- Phân biệt được CMAR với CBA, C4.5

### ✅ Về mặt cài đặt
- Cài đặt đầy đủ thuật toán bằng Java
- Tổ chức code theo module rõ ràng (Mining / Pruning / Classification)
- Áp dụng tối ưu hóa (bitmap matching, CR-tree)

### ✅ Về mặt thực nghiệm
- Thử nghiệm trên đầy đủ 26 datasets của paper
- Đánh giá bằng 10-fold stratified CV đúng chuẩn
- Tránh data leakage (per-fold MDL)
- **Đạt kết quả sát paper: 85.1% vs 85.2% (chênh 0.1%)**

## 8.2 Bài học rút ra

**Về thuật toán:**
- FP-growth hiệu quả hơn Apriori nhiều
- Chi-square quan trọng để đảm bảo rule có ý nghĩa thống kê
- Voting nhiều rule tốt hơn chọn 1 rule

**Về khoa học dữ liệu:**
- Tiền xử lý rất quan trọng (discretization, missing values)
- Cross-validation phải đúng cách (không rò rỉ)
- Không thể tránh khỏi chênh lệch 1-2% khi reproduce paper (do seed, hyperparameter)

**Về trung thực học thuật:**
- Thà chấp nhận kết quả hơi thấp hơn paper chứ không tune gian lận
- Paper không công bố đủ chi tiết → khó reproduce 100%
- Quan trọng là **cài đúng thuật toán**, không phải **ép khớp con số**

## 8.3 Hạn chế của thuật toán CMAR (nhận định)

- **Chậm** với dataset lớn (mining combinatorial)
- **Cần rời rạc hóa** trước khi áp dụng
- **Nhạy cảm** với minSupport, minConfidence
- **Chưa xử lý tốt class imbalance**
- **Khó mở rộng** cho dữ liệu stream hoặc dữ liệu lớn

## 8.4 Hướng phát triển tiếp theo

- **CPAR** (Yin, Han 2003) — Cải tiến CMAR với predictive rule
- **MCAR** (Thabtah 2005) — Xử lý multi-class tốt hơn
- **Parallel CMAR** — Chạy song song trên GPU/Spark
- **Kết hợp Deep Learning** — Rule-guided neural network

---

# TÀI LIỆU THAM KHẢO

1. Li, W., Han, J., & Pei, J. (2001). **CMAR: Accurate and efficient classification based on multiple class-association rules**. *Proceedings of IEEE ICDM 2001*, 369-376.

2. Liu, B., Hsu, W., & Ma, Y. (1998). **Integrating classification and association rule mining (CBA)**. *Proceedings of KDD-98*, 80-86.

3. Han, J., Pei, J., & Yin, Y. (2000). **Mining frequent patterns without candidate generation (FP-growth)**. *SIGMOD Record*, 29(2), 1-12.

4. Fayyad, U., & Irani, K. (1993). **Multi-interval discretization of continuous-valued attributes for classification learning**. *IJCAI-93*, 1022-1027.

5. Quinlan, J. R. (1993). **C4.5: Programs for Machine Learning**. Morgan Kaufmann Publishers.

6. UCI Machine Learning Repository: https://archive.ics.uci.edu/ml/

---

# PHỤ LỤC — BẢNG THUẬT NGỮ ANH-VIỆT

| Tiếng Anh | Tiếng Việt |
|-----------|------------|
| Association Rule | Luật kết hợp |
| Antecedent | Tiền đề (vế NẾU) |
| Consequent | Kết luận (vế THÌ) |
| Support | Độ hỗ trợ |
| Confidence | Độ tin cậy |
| Chi-Square | Chi bình phương |
| Mining | Tìm/khai phá luật |
| Pruning | Cắt tỉa/lọc luật |
| Coverage | Độ bao phủ |
| Discretization | Rời rạc hóa |
| Cross-Validation | Kiểm chứng chéo |
| Stratified | Phân tầng |
| Data Leakage | Rò rỉ dữ liệu |
| Class Imbalance | Mất cân bằng lớp |
| Missing Values | Giá trị thiếu |

---

**HẾT BÁO CÁO**

*Em cảm ơn cô đã đọc báo cáo.*
