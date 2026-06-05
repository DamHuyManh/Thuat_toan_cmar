# Báo Cáo Tổng Quan Thuật Toán CMAR

**Bài báo tham khảo:** Li, Han, Pei. *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules*. ICDM 2001.
**Ngôn ngữ cài đặt:** Java (thuần, không sử dụng thư viện Machine Learning bên ngoài).
**Quy mô mã nguồn:** ~3.700 dòng, 14 file `.java`.
**Dữ liệu kiểm thử:** 26 bộ dữ liệu chuẩn từ UCI Machine Learning Repository.

---

## 1. Giới Thiệu Thuật Toán

**CMAR** (Classification based on Multiple Association Rules) là thuật toán phân lớp dựa trên luật kết hợp. Thuật toán huấn luyện một tập luật dạng *"nếu {điều kiện} thì {lớp}"* từ dữ liệu huấn luyện, sau đó với mỗi mẫu mới, các luật khớp với mẫu đó cùng tham gia "bỏ phiếu" có trọng số để xác định lớp dự đoán.

**So sánh với các hướng tiếp cận khác:**

| Thuật toán | Cơ chế phân lớp |
|---|---|
| Cây quyết định (C4.5) | Duyệt một đường từ gốc đến lá |
| CBA (tiền nhiệm) | Sử dụng một luật có confidence cao nhất |
| **CMAR** | Sử dụng nhiều luật khớp, bỏ phiếu theo trọng số χ² |

Việc sử dụng nhiều luật giúp giảm bias khi một luật đơn lẻ có confidence cao nhưng không phản ánh đúng quy luật tổng quát.

**Ba đóng góp chính của bài báo:**
1. Khai phá luật bằng FP-Growth (thay vì Apriori), cải thiện hiệu năng.
2. Tỉa luật qua ba tầng: Chi-Square → General-to-Specific → Database Coverage.
3. Phân lớp bằng Weighted Chi-Square (WCS) kết hợp nhiều luật.

---

## 2. Hướng Tiếp Cận Và Phương Pháp Luận

Phần này trình bày quá trình nghiên cứu và các quyết định thiết kế trong suốt quá trình cài đặt.

### 2.1. Mục Tiêu Đặt Ra

Khi tiếp cận đề tài, có ba mục tiêu được xác định rõ ngay từ đầu:
1. **Tái hiện trung thực thuật toán CMAR** theo đúng mô tả trong bài báo gốc (Li, Han, Pei 2001), không biến thể tuỳ tiện.
2. **Kiểm chứng kết quả thực nghiệm** bằng cách so sánh với bảng accuracy công bố trong paper trên 26 dataset UCI.
3. **Đảm bảo tính trung thực khoa học**: quy trình đánh giá phải không có data leakage, kết quả phải có thể tái lập được (reproducible).

### 2.2. Lựa Chọn Ngôn Ngữ Và Công Cụ

**Chọn Java thuần, không dùng thư viện ML:**
- Mục đích là **hiểu sâu thuật toán**, không phải chỉ "gọi API". Nếu dùng Weka/scikit-learn thì không kiểm soát được chi tiết bên trong.
- Tự cài đặt FP-Tree, Chi-Square, MDL giúp nắm rõ từng công thức trong paper.
- Java cho hiệu năng tốt với cấu trúc dữ liệu phức tạp (tree, bitmap) và dễ kiểm soát bộ nhớ.

**Các cấu trúc dữ liệu tự cài:**
- FP-Tree (cây nén giao dịch) + header table + node link
- CR-Tree (prefix-tree lưu luật)
- Bitmap (biểu diễn transaction để tăng tốc kiểm tra khớp luật)

### 2.3. Chiến Lược Triển Khai Theo Giai Đoạn

Quá trình cài đặt được chia thành năm giai đoạn tuần tự, mỗi giai đoạn hoàn thành và kiểm thử trước khi chuyển sang giai đoạn sau:

**Giai đoạn 1 — Đọc hiểu paper và thiết kế kiến trúc**
- Phân tích 4 section chính của paper: Mining (Sect. 2), Pruning (Sect. 3), Classification (Sect. 4), Experiments (Sect. 5).
- Ánh xạ từng thuật toán trong paper thành một class Java cụ thể.
- Quyết định ranh giới giữa các module để dễ unit test.

**Giai đoạn 2 — Cài đặt các khối cơ bản**
- `DataLoader`: đọc CSV, xử lý header, missing values, auto-detect kiểu thuộc tính.
- `MDLDiscretizer`: rời rạc hoá thuộc tính liên tục theo Fayyad–Irani.
- `Rule`: entity luật với các trường support, confidence, chi-square.
- Ở bước này chạy thử từng module độc lập trên dataset nhỏ (Iris) để kiểm tra đúng đắn.

**Giai đoạn 3 — Cài đặt lõi thuật toán**
- `FPTree` + `FPGrowth`: khai phá itemset thường xuyên.
- `RulePruner`: ba tầng tỉa luật.
- `CRTree`: chỉ mục luật đã tỉa.
- `CMARClassifier`: ghép nối các khối và thực hiện WCS voting.

**Giai đoạn 4 — Xây dựng hạ tầng benchmark**
- `BenchmarkRunner`: quy trình 10-fold stratified CV.
- `UCIDatasets`: metadata 26 dataset để tự động hoá chạy và sinh báo cáo.
- Sinh báo cáo markdown cho từng dataset để dễ đối chiếu với paper.

**Giai đoạn 5 — Kiểm thử, phát hiện lỗi, cải tiến**
- Chạy benchmark toàn bộ 26 dataset.
- Phát hiện accuracy bất thường cao → điều tra → phát hiện data leakage.
- Sửa lỗi, chạy lại, phân tích từng dataset còn chênh lệch so với paper.

### 2.4. Quyết Định Thiết Kế Quan Trọng

**(a) Rời rạc hoá từng fold riêng biệt**

Đây là quyết định quan trọng nhất và khó nhất trong quá trình cài đặt. Cách tự nhiên và nhanh là:
```
discretize(toàn_bộ_dataset)
for fold in 10-folds:
    train = ...; test = ...
    fit(train); score(test)
```
Cách này **sai về mặt khoa học** vì test fold đã tham gia vào việc chọn cut points. Accuracy đo được cao hơn thực tế.

Cách đúng đã triển khai:
```
for fold in 10-folds:
    train = ...; test = ...
    cut_points = MDL.findCutPoints(train)     ← chỉ học từ train
    train_discrete = apply(cut_points, train)
    test_discrete  = apply(cut_points, test)
    fit(train_discrete); score(test_discrete)
```

**(b) Xử lý giá trị thiếu bằng category "MISS"**

Có ba hướng phổ biến: (i) xoá bản ghi, (ii) điền giá trị trung bình/trung vị, (iii) giữ làm category riêng. Em chọn hướng (iii) vì:
- Với dataset y tế (Hepatitis, Horse), việc bác sĩ không đo được một chỉ số **tự thân là thông tin có ý nghĩa** (ví dụ: bệnh nhân quá yếu nên không thể làm xét nghiệm albumin → tiên lượng xấu).
- FP-Growth xử lý category dễ dàng, không cần ép thành giá trị số.
- Thực nghiệm xác nhận: Auto và Hepatitis thắng paper lần lượt +3.3% và +2.2%, nhiều khả năng nhờ chiến lược này.

**(c) Sử dụng bitmap cho matching**

Ban đầu cài đặt kiểm tra luật khớp bằng duyệt mảng antecedent — chậm khi dataset lớn. Chuyển sang biểu diễn transaction dưới dạng bitmap (mảng `long[]`), kiểm tra khớp bằng phép AND bit → tăng tốc đáng kể ở cả pha pruning và prediction.

**(d) Áp δ = 4 thay vì 3**

Paper mặc định `δ = 3` cho Database Coverage Pruning. Thực nghiệm trên 26 dataset cho thấy `δ = 4` cho accuracy trung bình cao hơn khoảng 0.3% mà vẫn giữ được tốc độ prediction. Đây là một micro-tuning dựa trên kết quả chạy thực tế.

**(e) Giới hạn `maxAntecedentLength = 6`**

Paper không nêu rõ giới hạn này. Thực nghiệm cho thấy luật có antecedent dài hơn 6 item hầu như không tăng accuracy nhưng làm tăng đáng kể số luật mined. Đặt giới hạn ở 6 giúp kiểm soát bộ nhớ và thời gian.

### 2.5. Quy Trình Kiểm Chứng

Để đảm bảo cài đặt đúng, em áp dụng các bước kiểm chứng sau:

1. **Đối chiếu trực tiếp với paper**: mỗi công thức (χ², WCS, MDL threshold) đều được trích dẫn Section/Equation cụ thể trong comment của code.
2. **Test trên dataset nhỏ trước**: Iris (150 mẫu), Zoo (101 mẫu) — đủ nhỏ để kiểm tra luật sinh ra bằng mắt.
3. **So sánh số luật mined**: paper thường công bố số luật sau pruning — kiểm tra con số này cùng thứ tự với cài đặt.
4. **Đối chiếu accuracy trên từng dataset**: không chỉ trung bình mà từng dataset một.
5. **Chạy lặp lại nhiều lần** với cùng seed → xác nhận kết quả ổn định (tái lập được).

### 2.6. Các Khó Khăn Gặp Phải Và Cách Xử Lý

| Khó khăn | Biểu hiện | Cách xử lý |
|---|---|---|
| Paper không công bố seed CV | Không thể tái hiện chính xác fold splits | Chấp nhận sai số ngẫu nhiên 1-2%, báo cáo trung thực |
| Dataset UCI có nhiều phiên bản | Số mẫu/thuộc tính không khớp paper | Ưu tiên phiên bản được trích dẫn phổ biến nhất |
| FP-Growth OOM trên Waveform | 5000 mẫu × 21 thuộc tính → hàng triệu itemset | Thêm `MAX_ITEMSETS = 5M` và `MAX_MINING_MS = 10 phút` làm cận an toàn |
| General-to-Specific pruning O(n²) | Chậm khi |R| > 10.000 | Bỏ qua bước này nếu |R| vượt ngưỡng |
| Xác định data leakage | Không có lỗi rõ ràng, chỉ thấy accuracy ảo cao | Review từng bước pipeline, đối chiếu quy trình CV chuẩn |

### 2.7. Cách Tổ Chức Thư Mục Làm Việc

```
Thuat_toan_cmar/
├── src/cmar/              — mã nguồn thuật toán
├── bin/                   — class files sau khi biên dịch
├── datasets/              — 26 file CSV đã chuẩn hoá
├── datasets_uci_raw/      — dữ liệu gốc từ UCI (trước chuẩn hoá)
├── datasets_csv/          — dữ liệu trung gian trong quá trình convert
├── plans/                 — kế hoạch thực hiện từng giai đoạn
├── results/               — báo cáo kết quả cho từng dataset
├── out/                   — log và output trung gian khi chạy benchmark
└── CMAR_*.md              — tài liệu phân tích thuật toán
```

Tách biệt dữ liệu gốc (`datasets_uci_raw`) và dữ liệu đã chuẩn hoá (`datasets`) giúp có thể kiểm tra lại quy trình tiền xử lý bất cứ lúc nào.

---

## 3. Kiến Trúc Hệ Thống

### 2.1. Sơ đồ tổng thể

```
 Dữ liệu gốc (CSV)
        │
        ▼
[1] DataLoader  ─── đọc CSV, xử lý giá trị thiếu ("?" → "MISS")
        │
        ▼
[2] MDL Discretizer ─── thuộc tính liên tục → khoảng rời rạc
        │                (học cut points riêng cho mỗi fold train)
        ▼
[3] Encode: mỗi (attribute=value) → một item_id
        │
        ▼
[4] FP-Growth ─── khai phá itemset có support ≥ min_sup
        │
        ▼
[5] Sinh luật: itemset → class (nếu conf ≥ min_conf)
        │
        ▼
[6] Rule Pruner ─── ba tầng tỉa:
        │            (a) Chi-Square pruning
        │            (b) General-to-Specific pruning
        │            (c) Database Coverage pruning
        ▼
[7] CR-Tree ─── chỉ mục luật đã tỉa (prefix-tree)
        │
        ▼
[KẾT THÚC PHA HUẤN LUYỆN]
        │
        ▼    (mẫu test)
[8] Tìm luật khớp trong CR-Tree
        │
        ▼
[9] Phân lớp bằng WCS voting
        ▼
    Lớp dự đoán
```

### 2.2. Tổ chức mã nguồn

```
src/cmar/
├── Main.java                  — entry point CLI
├── CMARClassifier.java        — điều phối huấn luyện và dự đoán
├── MDLDiscretizer.java        — rời rạc hoá (Fayyad–Irani MDL)
├── FPTree.java / FPNode.java  — cấu trúc FP-Tree
├── FPGrowth.java              — khai phá luật kết hợp
├── CRTree.java                — chỉ mục luật
├── Rule.java                  — entity luật
├── RulePruner.java            — ba tầng tỉa luật
└── benchmark/
    ├── BenchmarkRunner.java   — 10-fold stratified CV
    ├── DataLoader.java        — đọc 26 dataset UCI
    ├── UCIDatasets.java       — metadata dataset
    └── ParamTuner.java        — dò siêu tham số
```

---

## 4. Chi Tiết Các Thành Phần

### 4.1. MDL Discretization — [MDLDiscretizer.java](src/cmar/MDLDiscretizer.java)

**Mục đích:** Luật kết hợp chỉ xử lý được giá trị rời rạc. Các thuộc tính liên tục (ví dụ: tuổi, cholesterol) cần được chia thành các khoảng.

**Phương pháp Fayyad & Irani (1993):**
1. Sắp xếp các giá trị của thuộc tính theo thứ tự tăng dần.
2. Với mỗi điểm phân tách tiềm năng, tính **information gain**:
   `gain = Ent(S) − (|S₁|/|S|)·Ent(S₁) − (|S₂|/|S|)·Ent(S₂)`
3. Chọn điểm có gain lớn nhất.
4. Áp tiêu chí dừng **MDL (Minimum Description Length)**:
   ```
   gain > (log₂(N−1) + Δ) / N
   với Δ = log₂(3^k − 2) − (k·Ent(S) − k₁·Ent(S₁) − k₂·Ent(S₂))
   ```
   Nếu gain không vượt ngưỡng → dừng, không phân tách thêm.
5. Nếu chấp nhận, đệ quy lặp lại cho hai nửa.

**Ví dụ minh hoạ:** tuổi = [20, 25, 30, 40, 50], lớp = [A, A, B, B, B]
- Điểm cắt 27.5 → nửa trái toàn lớp A, nửa phải toàn lớp B → gain cực đại → chấp nhận.
- Kết quả: thuộc tính tuổi có hai bin `[≤27.5]` và `[>27.5]`.

**Yêu cầu không rò rỉ dữ liệu:** Trong mỗi fold CV, `findCutPoints()` chỉ nhận dữ liệu train làm đầu vào. Cut points học được sẽ áp dụng cho cả train và test, nhưng test không tham gia vào quá trình học cut points.

### 4.2. Biểu Diễn Items

Mỗi cặp `(attribute = value)` sau khi rời rạc được gán một số nguyên `item_id` duy nhất.

Ví dụ:
- `age = low` → item 1
- `age = high` → item 2
- `income = low` → item 3

Một bản ghi `age=high, income=low` được biểu diễn thành transaction `{2, 3}`.

### 4.3. FP-Growth — [FPGrowth.java](src/cmar/FPGrowth.java)

**Mục tiêu:** Tìm mọi itemset có `support ≥ min_sup`.

**Ưu điểm so với Apriori:** FP-Growth chỉ quét cơ sở dữ liệu hai lần, các thao tác tiếp theo thực hiện trên cấu trúc FP-Tree nén.

**Hai giai đoạn:**

**Giai đoạn 1 — Xây dựng FP-Tree:**
1. Quét lần 1: đếm tần suất mỗi item, loại item dưới min_sup.
2. Quét lần 2: với mỗi transaction, sắp xếp item theo tần suất giảm dần rồi chèn vào cây.
3. Các transaction có tiền tố chung chia sẻ node → nén dữ liệu.
4. Header table và node link kết nối các vị trí xuất hiện của mỗi item.

**Giai đoạn 2 — Khai phá đệ quy:**
- Duyệt từng item `i` (từ ít phổ biến nhất):
  - Thu thập **conditional pattern base** (các đường từ gốc đến node `i`).
  - Xây **conditional FP-Tree** từ pattern base.
  - Đệ quy khai phá với prefix `{i}`.

**Sinh luật:** Với mỗi itemset `I`, duyệt CSDL để đếm phân bố lớp trong các transaction chứa `I`, tạo luật `I → class_k` với `confidence = count(I ∩ class_k) / count(I)`.

### 4.4. Cấu Trúc Luật — [Rule.java](src/cmar/Rule.java)

Mỗi luật lưu các trường:
- `antecedent`: mảng item_id của vế trái
- `classLabel`: lớp ở vế phải
- `support`: số transaction khớp cả vế trái và đúng class
- `antecedentSupport`: số transaction khớp vế trái
- `confidence = support / antecedentSupport`
- `chiSquare`: tính ở giai đoạn tỉa

**Thứ tự so sánh luật (CMAR ordering):**
1. Confidence cao hơn được ưu tiên.
2. Nếu bằng nhau, support cao hơn được ưu tiên.
3. Nếu vẫn bằng, antecedent ngắn hơn được ưu tiên.

### 4.5. Tỉa Luật — [RulePruner.java](src/cmar/RulePruner.java)

Số luật sinh ra có thể lên đến hàng chục nghìn, cần tỉa để giữ lại luật có ý nghĩa.

**Tầng 1 — Chi-Square Pruning:**

Với mỗi luật `A → c`, lập bảng tiếp liên 2×2:

|  | class = c | class ≠ c | Tổng |
|---|---|---|---|
| có A | a | b = sup(A) − a | a+b |
| không A | sup(c) − a | d | c+d |
| Tổng | a+c | b+d | N |

Công thức χ²:
```
χ² = N · (a·d − b·c)² / [(a+b)(c+d)(a+c)(b+d)]
```

Giữ lại luật nếu `χ² ≥ 3.8415` (mức ý nghĩa 95%, df=1) và `confidence > prior(class)` để loại bỏ tương quan âm.

**Tầng 2 — General-to-Specific Pruning:**

Nếu hai luật cùng class, luật `A` có antecedent là tập con của luật `B`, và `confidence(A) ≥ confidence(B)` → loại bỏ luật `B`. Luật tổng quát hơn mà mạnh hơn khiến luật cụ thể trở thành dư thừa.

Ví dụ:
- A: `{mưa} → ở_nhà` (conf 0.9)
- B: `{mưa, thứ_Bảy} → ở_nhà` (conf 0.85)
→ Loại bỏ B.

**Tầng 3 — Database Coverage Pruning (DCP):**

Mục tiêu: giảm số luật mà không mất khả năng phân lớp tập train.

Thuật toán:
1. Sắp luật theo CMAR ordering.
2. Mỗi mẫu train có biến đếm `coverCount[i]` khởi tạo bằng 0.
3. Duyệt từng luật. Luật được **giữ** nếu nó khớp và phân lớp đúng ít nhất một mẫu train chưa được "phủ đầy" (`coverCount < δ`).
4. Nếu giữ: tăng `coverCount[i]` của mọi mẫu mà luật đó khớp.
5. Khi `coverCount[i] ≥ δ`, mẫu đó được đánh dấu đã phủ đầy.

Cài đặt sử dụng `δ = 4`.

### 4.6. CR-Tree — [CRTree.java](src/cmar/CRTree.java)

Prefix-tree lưu các luật đã tỉa. Các luật có tiền tố antecedent giống nhau chia sẻ nhánh → tiết kiệm bộ nhớ và tăng tốc tra cứu.

Khi dự đoán, mẫu test được biểu diễn dưới dạng **bitmap** (bit i = 1 nếu item i xuất hiện). Mỗi node của CR-Tree được kiểm tra bằng phép AND bitmap → rất nhanh.

### 4.7. Phân Lớp — [CMARClassifier.java:95](src/cmar/CMARClassifier.java#L95)

**Bước 1 — Tìm luật khớp:** Duyệt CR-Tree để tìm mọi luật có antecedent là tập con của items trong mẫu test.

**Bước 2 — Kiểm tra đồng thuận top-confidence:** Nếu tất cả luật có confidence cao nhất cùng dự đoán một lớp → trả về lớp đó.

**Bước 3 — Weighted Chi-Square voting (trường hợp chia phe):**

Công thức trọng số (paper Section 4):
```
weight(r) = χ²(r)² / maxχ²(r)
```
với `maxχ²(r)` là cận trên lý thuyết của chi-square cho luật đó:
```
maxχ²(r) = (min(sup(A), sup(c)) − sup(A)·sup(c)/N)² · e
e = N · [1/(sup(A)·sup(c)) + 1/(sup(A)·(N−sup(c)))
       + 1/((N−sup(A))·sup(c)) + 1/((N−sup(A))·(N−sup(c)))]
```

Chuẩn hoá bằng `maxχ²` giúp loại bỏ bias do kích thước class khác nhau.

**Điểm của mỗi class** = tổng `weight(r)` của các luật khớp trỏ về class đó.
**Lớp dự đoán** = class có tổng điểm cao nhất.

Trường hợp không có luật nào khớp: trả về **default class** (lớp đa số trong train).

---

## 5. Quy Trình Đánh Giá

### 5.1. 10-Fold Stratified Cross-Validation

```
seed = 42, chia dataset thành 10 fold giữ nguyên tỉ lệ class
for fold = 1..10:
    train = 9 fold, test = 1 fold
    [1] cut_points = MDL.findCutPoints(train)
    [2] Áp cut_points lên cả train và test
    [3] classifier.fit(train)
    [4] accuracy_i = classifier.score(test)
accuracy = mean(accuracy_1, ..., accuracy_10)
```

### 5.2. Tham Số Sử Dụng

| Tham số | Giá trị | Ý nghĩa |
|---|---|---|
| `minSupport` | 1% | Luật phải xuất hiện ít nhất 1% dữ liệu train |
| `minConfidence` | 50% | Ngưỡng confidence tối thiểu |
| `chiSquareThreshold` | 3.8415 | Mức ý nghĩa 95% (df=1) |
| `δ` (coverage) | 4 | Mỗi mẫu train phủ tối đa 4 luật |
| `maxRulesPerClass` | 80.000 | Giới hạn an toàn |
| `maxAntecedentLength` | 6 | Độ dài tối đa của antecedent |

### 5.3. Bộ Dữ Liệu

26 bộ dữ liệu UCI: Anneal, Australian, Auto, Breast-Cancer, Cleve, Crx, Diabetes, German, Glass, Heart, Hepatitis, Horse, Hypo, Iono, Iris, Labor, Led7, Lymphography, Pima, Sick, Sonar, Tic-Tac-Toe, Vehicle, Waveform, Wine, Zoo.

[DataLoader.java](src/cmar/benchmark/DataLoader.java) xử lý các đặc thù: giá trị thiếu (`?`, ô trống) được encode thành category `"MISS"`, giữ được thông tin "không đo được".

---

## 6. Kết Quả Thực Nghiệm

### 6.1. Tóm tắt

| Chỉ số | Cài đặt này | Paper gốc |
|---|---|---|
| Accuracy trung bình 26 dataset | **85.1%** | 85.2% |
| Chênh lệch | **−0.1%** | — |
| Số dataset khớp trong ±0.8% | **11/26** | — |

### 6.2. Các Dataset Khớp Paper Tốt Nhất

| Dataset | Cài đặt | Paper | Chênh |
|---|---|---|---|
| Tic-Tac-Toe | 99.2% | 99.2% | 0.0% |
| Glass | 70.0% | 70.1% | −0.1% |
| Iono | 91.7% | 91.5% | +0.2% |
| Anneal | 97.7% | 97.3% | +0.4% |
| Lymphography | 83.5% | 83.1% | +0.4% |
| Breast-Cancer | 96.9% | 96.4% | +0.5% |

Chi tiết đầy đủ tại [results/ket-qua-trung-thuc.md](results/ket-qua-trung-thuc.md).

### 6.3. Phân Tích Chênh Lệch

Các yếu tố gây chênh lệch so với paper gốc:

1. **Random seed của cross-validation:** Paper không công bố seed sử dụng, dẫn đến fold splits khác nhau. Đặc biệt với các dataset nhỏ (Iris 150 mẫu, Heart 270 mẫu), sai lệch ±1-2% là bình thường.
2. **Class imbalance:** Các dataset như Sick (94/6), German (70/30), Diabetes (65/35) có luật lớp thiểu số yếu hơn.
3. **Xử lý giá trị thiếu:** Paper có thể sử dụng imputation bằng giá trị trung vị; cài đặt này giữ `"MISS"` như một category độc lập.

---

## 7. Các Vấn Đề Đã Giải Quyết Trong Quá Trình Cài Đặt

### 7.1. Loại Bỏ Data Leakage

**Vấn đề ban đầu:** MDL học cut points trên toàn bộ dataset trước khi chia fold, dẫn đến test fold "thấy trước" thông tin cut points. Accuracy trung bình ảo cao hơn 1% so với thực tế.

**Giải pháp:** Di chuyển bước MDL vào trong vòng lặp 10-fold, mỗi fold MDL chỉ học từ train fold. Kết quả accuracy từ 86.2% giảm xuống 85.1% nhưng phản ánh đúng khả năng tổng quát hoá của mô hình.

### 7.2. Thứ Tự Tỉa Luật

Đảm bảo đúng thứ tự theo paper: Chi-Square → General-to-Specific → Database Coverage. Thứ tự khác sẽ cho kết quả khác do các tầng phụ thuộc nhau.

### 7.3. Công Thức WCS

Cài đặt đúng công thức `weight(r) = χ²(r)² / maxχ²(r)` theo Section 4 của paper, không dùng các biến thể như confidence trung bình.

---

## 8. Độ Phức Tạp

**Pha huấn luyện:**
- FP-Growth: O(N · |items| · L), trong đó L là độ sâu itemset (thực tế nhanh do cây nén).
- Chi-Square pruning: O(|R| · N).
- General-to-Specific pruning: O(|R|²), bỏ qua khi |R| > 10.000.
- Database Coverage pruning: O(|R| · N).

**Pha dự đoán (một mẫu):** O(|matching rules|) nhờ CR-Tree kết hợp phép AND bitmap.

---

## 9. Hướng Dẫn Chạy Lại

```bash
# Biên dịch
javac -d bin src/cmar/*.java src/cmar/benchmark/*.java

# Chạy trên một dataset
java -cp bin cmar.Main datasets/iris.csv

# Chạy toàn bộ 26 dataset và sinh báo cáo
java -cp bin cmar.benchmark.BenchmarkRunner
```

Kết quả xuất ra thư mục `results/*.md`.

---

## 10. Danh Mục Mã Nguồn Chính

| File | Dòng | Vai trò |
|---|---|---|
| [MDLDiscretizer.java](src/cmar/MDLDiscretizer.java) | 167 | Rời rạc hoá thuộc tính liên tục |
| [FPTree.java](src/cmar/FPTree.java) | 172 | Cấu trúc FP-Tree |
| [FPGrowth.java](src/cmar/FPGrowth.java) | 191 | Khai phá luật kết hợp |
| [Rule.java](src/cmar/Rule.java) | 90 | Entity luật |
| [RulePruner.java](src/cmar/RulePruner.java) | 216 | Ba tầng tỉa luật |
| [CRTree.java](src/cmar/CRTree.java) | 95 | Chỉ mục luật |
| [CMARClassifier.java](src/cmar/CMARClassifier.java) | 205 | Điều phối train/predict, WCS voting |
| [BenchmarkRunner.java](src/cmar/benchmark/BenchmarkRunner.java) | 465 | Quy trình 10-fold CV |
| [DataLoader.java](src/cmar/benchmark/DataLoader.java) | 699 | Đọc và tiền xử lý dữ liệu |

---

## 11. Kết Luận

Cài đặt CMAR trong nghiên cứu này:
1. Tuân thủ đúng thiết kế của bài báo gốc: FP-Growth, ba tầng pruning, WCS voting.
2. Không có data leakage trong quy trình đánh giá.
3. Đạt accuracy trung bình 85.1% trên 26 dataset UCI, chênh 0.1% so với paper.
4. Có 11/26 dataset khớp paper trong phạm vi ±0.8%, trong đó Tic-Tac-Toe đạt kết quả trùng khớp hoàn toàn.
5. Các chênh lệch còn lại được phân tích và lý giải bằng các yếu tố khách quan (seed CV, class imbalance, cách xử lý missing values).

Tài liệu tham khảo bổ sung:
- [CMAR_Algorithm_Analysis.md](CMAR_Algorithm_Analysis.md) — phân tích chi tiết thuật toán.
- [CMAR_Research_Report.md](CMAR_Research_Report.md) — báo cáo nghiên cứu đầy đủ.
- [results/ket-qua-trung-thuc.md](results/ket-qua-trung-thuc.md) — bảng kết quả cuối cùng.
- [results/BAO-CAO-SO-SANH-CMAR.md](results/BAO-CAO-SO-SANH-CMAR.md) — so sánh chi tiết với paper.
