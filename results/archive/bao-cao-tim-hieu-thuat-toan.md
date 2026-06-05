# Báo Cáo Tìm Hiểu Thuật Toán CMAR

**Tên bài báo:** "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules"
**Tác giả:** Wenmin Li, Jiawei Han, Jian Pei
**Hội nghị:** IEEE International Conference on Data Mining (ICDM) 2001
**Sinh viên:** [Họ tên của anh]
**Môn học:** [Tên môn]

---

## MỤC LỤC
1. [Giới thiệu](#1-giới-thiệu)
2. [Bối cảnh và động lực](#2-bối-cảnh-và-động-lực)
3. [Các khái niệm cơ bản](#3-các-khái-niệm-cơ-bản)
4. [Thuật toán CMAR chi tiết](#4-thuật-toán-cmar-chi-tiết)
5. [So sánh với các thuật toán khác](#5-so-sánh-với-các-thuật-toán-khác)
6. [Ưu điểm và hạn chế](#6-ưu-điểm-và-hạn-chế)
7. [Kết luận](#7-kết-luận)

---

## 1. GIỚI THIỆU

### 1.1 CMAR là gì?

**CMAR** (Classification based on Multiple Association Rules) là thuật toán **phân loại** (classification) dựa trên **nhiều luật kết hợp** (association rules).

Ý tưởng cốt lõi: **"Dùng nhiều luật để quyết định, thay vì chỉ 1 luật duy nhất."**

### 1.2 Bài toán phân loại

**Input:** Dataset gồm N mẫu, mỗi mẫu có:
- Các thuộc tính (attributes): `A1, A2, ..., Ak`
- Nhãn lớp (class label): `C`

**Output:** Model có thể dự đoán lớp C cho mẫu mới.

**Ví dụ:**
```
Input:  [tuổi=45, huyết áp=150, cholesterol=cao]
Output: bệnh_tim = CÓ
```

### 1.3 Luật kết hợp (Association Rule)

Một luật có dạng: **IF (antecedent) THEN (consequent)**

**Ví dụ:**
```
IF tuổi > 50 AND huyết áp > 140 THEN bệnh_tim = CÓ
```

Trong Class Association Rule (CAR):
- **Antecedent**: tập các điều kiện (tuổi > 50, huyết áp > 140)
- **Consequent**: phải là một class label (bệnh_tim = CÓ)

---

## 2. BỐI CẢNH VÀ ĐỘNG LỰC

### 2.1 Trước CMAR: Thuật toán CBA (Liu, Hsu, Ma — 1998)

CBA là thuật toán **đầu tiên** kết hợp association rule với classification. Hoạt động:
1. Mine tất cả rule từ training data
2. Sắp xếp rule theo confidence giảm dần
3. Khi predict: chọn **rule đầu tiên** khớp với mẫu test

### 2.2 Vấn đề của CBA

**Vấn đề 1: Chỉ dùng 1 rule duy nhất**
- Nếu rule đó sai → dự đoán sai toàn bộ
- Bỏ qua thông tin từ các rule khác cũng khớp

**Vấn đề 2: Hiệu quả về mặt bộ nhớ**
- Lưu tất cả rule theo danh sách → tốn nhiều RAM
- Predict chậm với dataset lớn

**Vấn đề 3: Không xử lý rule thừa**
- Nhiều rule cùng kết luận 1 lớp
- Không có cơ chế lọc rule tương tự

### 2.3 CMAR giải quyết như thế nào?

| Vấn đề CBA | Giải pháp CMAR |
|------------|-----------------|
| Dùng 1 rule | **Weighted Chi-Square voting** — dùng tất cả rule khớp |
| Tốn RAM | **CR-tree** — cấu trúc compact lưu rule |
| Rule thừa | **3 phương pháp pruning** — Chi², General-to-specific, Coverage |
| Mining chậm | **FP-growth** thay vì Apriori |

---

## 3. CÁC KHÁI NIỆM CƠ BẢN

### 3.1 Support (Độ hỗ trợ)

**Định nghĩa:** Tỷ lệ mẫu trong dataset thỏa mãn luật.

```
support(A → C) = (số mẫu có cả A và C) / (tổng số mẫu)
```

**Ví dụ:** Trong 1000 bệnh nhân:
- 200 người có "huyết áp cao" VÀ "bị bệnh tim"
- → support = 200/1000 = 20%

**Ý nghĩa:** Luật có tần suất xuất hiện bao nhiêu trong data.

### 3.2 Confidence (Độ tin cậy)

**Định nghĩa:** Xác suất C đúng khi A đúng.

```
confidence(A → C) = support(A ∧ C) / support(A)
```

**Ví dụ:**
- 300 người có huyết áp cao
- Trong đó 200 người bị bệnh tim
- → confidence = 200/300 = 67%

**Ý nghĩa:** Luật đúng 67% thời gian — độ chính xác của luật.

### 3.3 Chi-Square (χ²)

**Mục đích:** Đo mức độ **phụ thuộc thống kê** giữa antecedent và class.

**Bảng contingency 2x2:**

| | Class = c | Class ≠ c | Tổng |
|---|-----------|-----------|------|
| Có A | a | b | a+b |
| Không A | c | d | c+d |
| Tổng | a+c | b+d | N |

**Công thức:**
```
χ² = N × (ad - bc)² / [(a+b)(c+d)(a+c)(b+d)]
```

**Ngưỡng:** χ² ≥ 3.841 → rule có ý nghĩa thống kê (p-value < 0.05).

**Ý nghĩa:** Nếu χ² thấp → mối liên hệ giữa A và C là **ngẫu nhiên**, không đáng tin.

### 3.4 FP-tree (Frequent Pattern Tree)

**Mục đích:** Cấu trúc dữ liệu compact lưu tất cả transaction để mine rule hiệu quả.

**Cách xây:**
1. Duyệt dataset lần 1: đếm tần suất từng item
2. Sắp xếp item trong mỗi transaction theo tần suất giảm dần
3. Duyệt lần 2: insert từng transaction vào cây, chia sẻ prefix chung

**Ưu điểm:**
- Chỉ duyệt dataset **2 lần** (thay vì N lần như Apriori)
- Prefix được chia sẻ → tiết kiệm bộ nhớ

**Minh họa:**
```
Transactions: {ABC}, {ABD}, {AC}, {BCD}

FP-tree:
        root
       /    \
      A      B
     / \      \
    B   C      C
   / \          \
  C   D          D
```

### 3.5 CR-tree (Class-Rule Tree)

**Mục đích:** Cấu trúc lưu rule sau khi mining để **tra cứu nhanh**.

**Đặc điểm:**
- Phân cấp theo item đầu tiên của antecedent
- Hỗ trợ tra cứu rule khớp với mẫu test trong O(log n)
- Tiết kiệm RAM so với lưu list rule thông thường

---

## 4. THUẬT TOÁN CMAR CHI TIẾT

### 4.1 Tổng quan 3 giai đoạn

```
┌──────────────────────────────────────────────────┐
│  Training Dataset                                │
└───────────────────┬──────────────────────────────┘
                    ▼
     ┌──────────────────────────┐
     │  GIAI ĐOẠN 1: MINING     │
     │  FP-growth → Rule set R  │
     └───────────────┬──────────┘
                     ▼
     ┌──────────────────────────┐
     │  GIAI ĐOẠN 2: PRUNING    │
     │  1. Chi-square           │
     │  2. General-to-specific  │
     │  3. Database coverage    │
     └───────────────┬──────────┘
                     ▼
     ┌──────────────────────────┐
     │  GIAI ĐOẠN 3: CLASSIFY   │
     │  WCS voting              │
     └──────────────────────────┘
```

### 4.2 Giai đoạn 1 — Mining Class Association Rules

**Mục tiêu:** Tìm **tất cả** rule `A → C` thỏa:
- support(A → C) ≥ minSup (mặc định 1%)
- confidence(A → C) ≥ minConf (mặc định 50%)

**Phương pháp:** FP-growth (Han, Pei, Yin — 2000)

**Các bước:**

**Bước 1.1: Đếm tần suất**
```
Duyệt dataset 1 lần → đếm số lần xuất hiện của mỗi item
Loại bỏ item có support < minSup
```

**Bước 1.2: Xây FP-tree**
```
Với mỗi transaction:
  1. Giữ lại chỉ item frequent (đã qua bước 1.1)
  2. Sắp xếp theo tần suất giảm dần
  3. Insert vào FP-tree, chia sẻ prefix
```

**Bước 1.3: Mine rule từ FP-tree**
```
Với mỗi class c:
  Với mỗi item đầu I (từ dưới lên):
    Tìm tất cả pattern P kết thúc bằng I
    Tính support, confidence của P → c
    Nếu đủ ngưỡng: thêm P → c vào rule set
```

**Kết quả:** Rule set R gồm hàng nghìn đến hàng trăm nghìn rule.

### 4.3 Giai đoạn 2 — Pruning (Lọc rule)

Vì rule mining ra rất nhiều, cần lọc bớt. CMAR dùng **3 phương pháp**:

#### 4.3.1 Chi-square Pruning (CSP)

**Mục đích:** Loại rule không có ý nghĩa thống kê.

**Cách làm:**
```
Với mỗi rule r = (A → C):
  Tính χ²(r) theo công thức
  Nếu χ²(r) < 3.841 → LOẠI
  Nếu confidence(r) < prior_probability(C) → LOẠI (correlation âm)
```

**Ý nghĩa:** Giữ rule mà antecedent **thực sự** dự đoán class (không phải ngẫu nhiên).

#### 4.3.2 General-to-Specific Pruning

**Mục đích:** Loại rule cụ thể khi có rule tổng quát tốt hơn.

**Quy tắc:** Nếu rule R1 tổng quát hơn R2 (R1 ⊂ R2) và conf(R1) ≥ conf(R2) → loại R2.

**Ví dụ:**
```
R1: tuổi>50 → bệnh_tim (conf 80%)
R2: tuổi>50 AND cholesterol>200 → bệnh_tim (conf 75%)
→ Loại R2 vì R1 đã đủ tốt, R2 phức tạp hơn nhưng confidence thấp hơn
```

**Ý nghĩa:** Giữ rule đơn giản, dễ generalize.

#### 4.3.3 Database Coverage Pruning (DCP)

**Mục đích:** Mỗi mẫu train chỉ cần **δ rule** bao phủ là đủ (δ=3 trong paper, em dùng δ=4).

**Cách làm:**
```
Sắp xếp rule theo (confidence ↓, support ↓, length ↑)
count[i] = 0 cho mọi mẫu i

Với mỗi rule r (theo thứ tự):
  useful = false
  Với mỗi mẫu i chưa đủ coverage:
    Nếu r khớp i AND r dự đoán đúng class:
      useful = true
      break
  Nếu useful:
    Thêm r vào selected rules
    Tăng count[i] cho mọi mẫu i mà r khớp
    Nếu count[i] ≥ δ: đánh dấu i đã đủ coverage
```

**Ý nghĩa:** Loại rule thừa (đã có rule khác tốt hơn cover mẫu đó).

### 4.4 Giai đoạn 3 — Classification (Phân loại)

Khi có mẫu test mới, CMAR dự đoán class như sau:

**Bước 3.1: Tìm rule khớp**
```
Với mẫu test t:
  Tìm tất cả rule r mà antecedent(r) ⊆ t
  Gọi tập này là S
  Nếu S rỗng: trả về default class (lớp đông nhất)
```

**Bước 3.2: Kiểm tra đồng thuận**
```
Lấy rule có confidence cao nhất
Nếu tất cả rule có confidence cao (gần bằng max) đều dự đoán 1 class:
  → Trả về class đó
```

**Bước 3.3: Weighted Chi-Square Voting (nếu không đồng thuận)**

**Công thức:** Chia rule theo class, tính tổng trọng số.

```
Với mỗi class c:
  score(c) = Σ (χ²(r) / max_χ²(r))  với mọi r ∈ S và r.class = c

Dự đoán class có score cao nhất.
```

**max_χ²** là giá trị chi-square tối đa có thể của rule đó (upper bound), tính bằng:
```
max_χ² = (min(sup(A), sup(C)) - sup(A)·sup(C)/N)² × N × e
```

**Ý nghĩa của việc chia max_χ²:**
- Class đông (nhiều mẫu) có max_χ² lớn → chia để **không thiên vị lớp đông**
- Đảm bảo công bằng giữa các class

---

## 5. SO SÁNH VỚI CÁC THUẬT TOÁN KHÁC

### 5.1 CMAR vs CBA

| Khía cạnh | CBA | CMAR |
|-----------|-----|------|
| Mining | Apriori (chậm) | FP-growth (nhanh hơn) |
| Pruning | Coverage đơn giản | 3 phương pháp (Chi², G2S, Coverage) |
| Classification | 1 rule đầu tiên | WCS voting nhiều rule |
| Lưu trữ rule | List | CR-tree (compact) |
| Accuracy trung bình | 84.7% | 85.2% (+0.5%) |

### 5.2 CMAR vs C4.5 (Decision Tree)

| Khía cạnh | C4.5 | CMAR |
|-----------|------|------|
| Mô hình | Cây quyết định | Tập luật |
| Interpretability | Cao (xem cây) | Cao (xem rule) |
| Overfitting | Dễ overfit | Ít hơn (có pruning) |
| Multi-class | Native | Native |
| Accuracy trung bình | 83.3% | 85.2% (+1.9%) |

### 5.3 Tóm tắt thứ tự:

**CMAR > CBA > C4.5** về độ chính xác (chênh 0.5-2%).

CMAR là **sự cải tiến** của CBA:
- Vẫn dùng association rule
- Nhưng pruning tốt hơn + voting nhiều rule

---

## 6. ƯU ĐIỂM VÀ HẠN CHẾ

### 6.1 Ưu điểm

✅ **Accuracy cao** — Top 1 trong các thuật toán rule-based thời đó
✅ **Interpretable** — Rule dễ đọc, dễ giải thích
✅ **Robust** — 3 phương pháp pruning chống overfitting
✅ **Multi-class** — Xử lý nhiều lớp native
✅ **Statistical significance** — Chi-square đảm bảo rule có ý nghĩa

### 6.2 Hạn chế

❌ **Chậm trên dataset lớn** — Mining rule bùng nổ combinatorial
❌ **Cần rời rạc hóa** — Chỉ làm việc với discrete data
❌ **Nhạy cảm tham số** — minSup, minConf ảnh hưởng lớn đến kết quả
❌ **Class imbalance** — Chưa xử lý tốt khi lớp hiếm quá ít
❌ **High dimensionality** — Với >100 thuộc tính, rule mining khó khăn

---

## 7. KẾT LUẬN

### 7.1 Tóm tắt đóng góp của CMAR

Paper Li/Han/Pei 2001 đã đề xuất **3 đóng góp chính**:

1. **FP-growth** cho mining rule (nhanh hơn Apriori)
2. **3 phương pháp pruning** (Chi², G2S, Coverage) — giảm rule dư thừa
3. **Weighted Chi-Square voting** — dùng nhiều rule thay vì 1 rule

### 7.2 Ý nghĩa

- Đặt nền móng cho **associative classification**
- Được citation **hàng nghìn lần** trong 20+ năm qua
- Nhiều thuật toán sau phát triển từ CMAR: CPAR (2003), MCAR (2005), ACME (2011)...

### 7.3 Bài học cá nhân

Qua việc cài đặt CMAR, em hiểu được:

**Về mặt thuật toán:**
- Cách FP-growth mine rule hiệu quả
- Ý nghĩa của chi-square trong data mining
- Tại sao cần pruning (rule mining ra quá nhiều)
- Cách kết hợp nhiều rule để ra decision tốt hơn

**Về mặt thực hành:**
- Cross-validation đúng cách (per-fold MDL, không leak)
- Ý nghĩa của class imbalance, missing values
- Sự khác biệt giữa paper và thực tế (paper không công bố đủ chi tiết)
- Cách đánh giá thuật toán trên benchmark datasets

**Về mặt khoa học:**
- Tầm quan trọng của **reproducibility** — paper nên công bố seed, hyperparameters
- Sự khác biệt giữa **accuracy cao nhất** và **accuracy trung thực**
- Trade-off giữa **thuật toán đúng** và **kết quả đẹp**

### 7.4 Hướng phát triển

Nếu có thời gian, em muốn nghiên cứu thêm:
- **CPAR** — Cải tiến CMAR với predictive rule
- **Parallel CMAR** — Tận dụng GPU/multi-core
- **Deep Learning** so sánh — CMAR vs Neural Network trên cùng dataset

---

## TÀI LIỆU THAM KHẢO

1. Li, W., Han, J., & Pei, J. (2001). **CMAR: Accurate and efficient classification based on multiple class-association rules**. *Proceedings of IEEE ICDM 2001*, 369-376.

2. Liu, B., Hsu, W., & Ma, Y. (1998). **Integrating classification and association rule mining (CBA)**. *Proceedings of KDD-98*, 80-86.

3. Han, J., Pei, J., & Yin, Y. (2000). **Mining frequent patterns without candidate generation (FP-growth)**. *SIGMOD Record*, 29(2), 1-12.

4. Fayyad, U., & Irani, K. (1993). **Multi-interval discretization of continuous-valued attributes for classification learning (MDL)**. *IJCAI-93*, 1022-1027.

5. Quinlan, J. R. (1993). **C4.5: Programs for Machine Learning**. Morgan Kaufmann Publishers.

6. UCI Machine Learning Repository: https://archive.ics.uci.edu/ml/

---

## PHỤ LỤC A — Thuật Ngữ

| Thuật ngữ | Tiếng Việt | Ý nghĩa |
|-----------|------------|---------|
| Association Rule | Luật kết hợp | IF A THEN B |
| Antecedent | Tiền đề | Vế trái của rule |
| Consequent | Kết luận | Vế phải của rule |
| Support | Độ hỗ trợ | Tần suất rule trong data |
| Confidence | Độ tin cậy | Độ chính xác của rule |
| Chi-square (χ²) | Chi bình phương | Đo độ phụ thuộc thống kê |
| FP-tree | Cây pattern thường gặp | Cấu trúc lưu transaction |
| CR-tree | Cây class-rule | Cấu trúc lưu rule sau mining |
| Pruning | Cắt tỉa | Loại rule xấu/thừa |
| Coverage | Độ bao phủ | Rule bao nhiêu mẫu train |
| Discretization | Rời rạc hóa | Chia số liên tục thành khoảng |
| Cross-validation | Kiểm chứng chéo | Kỹ thuật đánh giá model |

---

**Hết báo cáo.**
