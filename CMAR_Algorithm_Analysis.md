# 📊 Phân Tích Thuật Toán CMAR - Classification Based on Multiple Association Rules

> **Tài liệu tham khảo gốc:** Li, Han, Pei - *"CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules"* (IEEE ICDM 2001)  
> **Ngôn ngữ cài đặt:** Java  
> **Ngày phân tích:** 16/03/2026

---

## 1. Tổng Quan

**CMAR** (Classification based on Multiple Association Rules) là thuật toán phân loại dựa trên khai phá luật kết hợp lớp (Class Association Rules - CARs). Thuật toán kết hợp:

- **FP-Growth** để khai phá tập phổ biến (frequent itemsets)
- **Kiểm định Chi-Square** để cắt tỉa luật không có ý nghĩa thống kê
- **Database Coverage** để loại bỏ luật dư thừa
- **Bỏ phiếu có trọng số** (Weighted Chi-Square Voting) để phân loại

### Ưu điểm so với CBA (Classification Based on Associations):
| Đặc điểm | CBA | CMAR |
|-----------|-----|------|
| Phân loại | Dùng 1 luật tốt nhất | Dùng **nhiều luật** kết hợp bỏ phiếu |
| Tỉa luật | Pessimistic pruning | **Chi-Square + Coverage** |
| Lưu trữ luật | Danh sách tuyến tính | **CR-Tree** (cây băm) |
| Tốc độ | Chậm hơn | **Nhanh hơn** |

---

## 2. Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────┐
│                    CMARClassifier                        │
│  (Bộ phân loại chính - điều phối toàn bộ pipeline)      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐   ┌────────────┐   ┌──────────┐          │
│  │ FPGrowth │──>│ RulePruner │──>│  CRTree  │          │
│  │ (Mining) │   │ (Pruning)  │   │ (Index)  │          │
│  └────┬─────┘   └────────────┘   └──────────┘          │
│       │                                                 │
│  ┌────┴─────┐                                           │
│  │  FPTree  │                                           │
│  └────┬─────┘                                           │
│       │                                                 │
│  ┌────┴─────┐   ┌──────┐                               │
│  │  FPNode  │   │ Rule │  (dùng xuyên suốt)            │
│  └──────────┘   └──────┘                                │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  benchmark/                                             │
│  ┌────────────────┐ ┌────────────┐ ┌──────────────────┐│
│  │BenchmarkRunner │ │ DataLoader │ │  UCIDatasets      ││
│  └────────────────┘ └────────────┘ └──────────────────┘│
└─────────────────────────────────────────────────────────┘
```

---

## 3. Phân Tích Chi Tiết Từng Lớp (Class)

### 3.1. `Rule.java` — Luật Kết Hợp Lớp (Class Association Rule)

**File:** `src/cmar/Rule.java` (90 dòng)

**Mô tả:** Biểu diễn một luật kết hợp dạng: `{antecedent} => classLabel`

#### Cấu trúc dữ liệu:
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `antecedent` | `int[]` | Mảng ID các item trong vế trái (đã sắp xếp tăng dần) |
| `classLabel` | `int` | Nhãn lớp dự đoán |
| `support` | `int` | Hỗ trợ tuyệt đối (số giao dịch chứa cả antecedent VÀ class) |
| `antecedentSupport` | `int` | Hỗ trợ của riêng antecedent |
| `confidence` | `double` | Độ tin cậy = support / antecedentSupport |
| `chiSquare` | `double` | Giá trị chi-square (đo mức ý nghĩa thống kê) |
| `weight` | `double` | Trọng số dùng khi phân loại (chi² chuẩn hóa) |

#### Thứ tự sắp xếp luật (CMAR ordering):
```
1. Confidence giảm dần (luật tin cậy hơn lên trước)
2. Support giảm dần (luật phổ biến hơn ưu tiên)
3. Độ dài antecedent tăng dần (luật ngắn hơn ưu tiên)
```

#### Matching bằng Bitmap:
```java
// Kiểm tra antecedent có là tập con của instance không
// Dùng phép toán bitwise O(1) thay vì duyệt tuần tự O(n)
public boolean matchesBitmap(long[] bitmap) {
    for (int item : antecedent) {
        int idx = item >> 6;    // item / 64 → vị trí word
        int bit = item & 63;    // item % 64 → vị trí bit
        if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0)
            return false;
    }
    return true;
}
```
> **Giải thích:** Mỗi instance được mã hóa thành mảng `long[]` (mỗi `long` = 64 bit). Item thứ `i` tương ứng bit thứ `i`. Kiểm tra subset chỉ cần phép AND bitwise → **cực nhanh**.

---

### 3.2. `FPNode.java` — Nút của FP-Tree

**File:** `src/cmar/FPNode.java` (41 dòng)

**Mô tả:** Nút trong cây FP-Tree, lưu trữ item và tần suất.

#### Cấu trúc:
```
FPNode:
├── item: int          → ID của item
├── count: int         → Số lần xuất hiện
├── parent: FPNode     → Nút cha (để truy ngược đường đi)
├── children: HashMap  → Các nút con (key = item ID)
└── link: FPNode       → Liên kết ngang (header table)
```

- Dùng `HashMap<Integer, FPNode>` cho children → tra cứu O(1)
- `link` tạo danh sách liên kết ngang cho các nút cùng item (phục vụ FP-Growth)
- Nút gốc (root) có `item = -1`

---

### 3.3. `FPTree.java` — Cây FP-Tree

**File:** `src/cmar/FPTree.java` (172 dòng)

**Mô tả:** Cây FP-Tree nén toàn bộ dataset, dùng để khai phá tập phổ biến mà **không cần sinh ứng viên** (candidate-free).

#### Cấu trúc chính:
| Thành phần | Kiểu | Ý nghĩa |
|------------|------|---------|
| `root` | `FPNode` | Nút gốc (item = -1) |
| `headerTable` | `Map<Integer, FPNode>` | Bảng header: item → nút đầu tiên trong linked list |
| `itemCounts` | `Map<Integer, Integer>` | Tần suất từng item |

#### Quy trình xây dựng FP-Tree (`build()`):

```
Bước 1: Quét dataset lần 1 → Đếm tần suất từng item
Bước 2: Loại bỏ item có tần suất < minSupport
Bước 3: Quét dataset lần 2:
    - Với mỗi giao dịch: lọc item không phổ biến
    - Sắp xếp item theo tần suất GIẢM DẦN
    - Chèn vào cây (chia sẻ tiền tố → nén dữ liệu)
```

**Ví dụ minh họa:**
```
Giao dịch gốc:        Sau lọc + sắp xếp:
{A, B, C, E}     →    {E, B, A, C}  (E phổ biến nhất)
{A, C, D}         →    {A, C, D}
{B, C, E}         →    {E, B, C}

FP-Tree:
        [root]
       /      \
     E:2      A:1
     |         |
     B:2      C:1
    / \        |
   A:1 C:1    D:1
   |
   C:1
```

#### Tối ưu Single-Path:
- Khi cây chỉ có 1 đường đi duy nhất → liệt kê tất cả tập con trực tiếp bằng bitmask
- Tránh xây dựng cây điều kiện (conditional FP-tree) không cần thiết

#### Xây dựng cây điều kiện (`buildConditional()`):
- Dùng cho FP-Growth đệ quy
- Nhận danh sách pattern + trọng số (count)
- Áp dụng cùng minSupport → **đảm bảo tính đúng đắn của FP-Growth**

---

### 3.4. `FPGrowth.java` — Khai Phá Luật Kết Hợp Lớp

**File:** `src/cmar/FPGrowth.java` (190 dòng)

**Mô tả:** Thuật toán FP-Growth mở rộng cho khai phá luật kết hợp lớp (CARs).

#### Tham số:
| Tham số | Mặc định | Ý nghĩa |
|---------|----------|---------|
| `minSupport` | 2 | Ngưỡng hỗ trợ tối thiểu (tuyệt đối) |
| `minConfidence` | 0.5 | Ngưỡng confidence tối thiểu |
| `maxRulesPerClass` | 80000 | Giới hạn số luật mỗi lớp |
| `maxAntecedentLength` | 6 | Độ dài tối đa vế trái luật |

#### Quy trình khai phá (`mineRules()`):

```
PHASE 1: Khai phá tập phổ biến
─────────────────────────────
1. Xây dựng bitmap cho mỗi giao dịch (phục vụ đếm nhanh)
2. Xây dựng FP-Tree với minSupport
3. Đào sâu FP-Tree bằng FP-Growth đệ quy:
   a. Nếu cây rỗng → dừng
   b. Nếu vượt maxAntecedentLength → dừng
   c. Nếu cây single-path → liệt kê tập con bằng bitmask
   d. Ngược lại:
      - Với mỗi item trong header table:
        i.  Tạo itemset mới = prefix + item
        ii. Xây dựng conditional pattern base
        iii.Xây conditional FP-tree (cùng minSupport)
        iv. Đệ quy khai phá

PHASE 2: Sinh luật kết hợp lớp
─────────────────────────────
Với mỗi tập phổ biến (itemset):
  1. Đếm support theo từng lớp (dùng bitmap scan)
  2. Với mỗi lớp:
     - Tính confidence = classSup / totalMatches
     - Nếu conf >= minConfidence VÀ classSup >= classMinSup:
       → Tạo Rule(itemset, class, support, confidence)
```

#### Xử lý lớp hiếm (Rare Class):
```java
// Lớp có ≤ 10 instances: dùng classMinSup = 1 (thay vì minSupport toàn cục)
// → Cho phép lớp thiểu số cũng tạo được luật
int classMinSup = (classTotal <= 10) ? 1 : minSupport;
```

#### Thuật toán FP-Growth đệ quy (`mineItemsets()`):

```
mineItemsets(tree, prefix, itemsets):
  IF tree.isEmpty() → return
  IF prefix.length >= maxAntecedentLength → return
  IF tree.isSinglePath():
    → Liệt kê tất cả tập con bằng bitmask (2^n - 1 tập)
    → return
  
  FOR mỗi item trong tree.headerTable (tần suất tăng dần):
    newItemset = prefix + [item]
    itemsets.add(newItemset)
    
    // Xây conditional pattern base
    patterns = []
    FOR mỗi nút N có item này (theo header link):
      path = truy ngược N → root
      patterns.add(path, count=N.count)
    
    // Xây conditional FP-tree
    condTree = FPTree.buildConditional(patterns, counts, minSupport)
    IF !condTree.isEmpty():
      mineItemsets(condTree, newItemset, itemsets)  // ĐỆ QUY
```

---

### 3.5. `RulePruner.java` — Tỉa Luật

**File:** `src/cmar/RulePruner.java` (210 dòng)

**Mô tả:** Hai giai đoạn tỉa luật theo paper CMAR: **Chi-Square Pruning (CSP)** và **Database Coverage Pruning (DCP)**.

#### Tham số:
| Tham số | Mặc định | Ý nghĩa |
|---------|----------|---------|
| `chiSquareThreshold` | 3.841 | Ngưỡng chi² (p = 0.05, df = 1) |
| `maxCoverageCount` | 3 | δ trong paper (số luật tối thiểu cover mỗi instance) |
| `minConfidence` | 0.50 | Ngưỡng confidence sau khi tính lại chính xác |

#### Phase 1: Chi-Square Pruning (CSP)

**Mục đích:** Loại bỏ luật không có ý nghĩa thống kê hoặc tương quan âm.

**Bảng contingency 2×2:**
```
                | Class = c  | Class ≠ c  | Tổng
────────────────|────────────|────────────|─────
Antecedent ∈    |     a      |     b      | a+b = supP
Antecedent ∉    |     c      |     d      | c+d = N-supP
────────────────|────────────|────────────|─────
Tổng            |   a+c=supC | b+d=N-supC |  N
```

**Công thức Chi-Square:**
```
χ² = N × (ad - bc)² / ((a+b)(c+d)(a+c)(b+d))
```

**Điều kiện giữ luật:**
1. `χ² >= 3.841` (có ý nghĩa thống kê ở mức p = 0.05)
2. `confidence > priorProb` (tương quan DƯƠNG - luật tốt hơn đoán ngẫu nhiên)
3. `confidence >= minConfidence` (sau khi tính lại chính xác)

```
Quy trình CSP:
──────────────
Với mỗi luật:
  1. Quét bitmap → Tính lại chính xác:
     - antecedentSupport (số instance match antecedent)
     - exactSupport (match antecedent VÀ đúng class)
     - confidence = exactSupport / antecedentSupport
  2. Tính chi² từ bảng contingency
  3. Tính priorProb = classSupport / N
  4. GIỮ nếu: chi² >= 3.841 VÀ conf > priorProb VÀ conf >= minConf
```

#### Phase 2: Database Coverage Pruning (DCP)

**Mục đích:** Loại bỏ luật dư thừa — chỉ giữ luật thực sự cần thiết để phân loại.

**Nguyên tắc (trích paper):** *"Một luật hữu ích nếu nó phân loại ĐÚNG ít nhất 1 instance huấn luyện chưa được cover bởi δ luật xếp hạng cao hơn."*

```
Quy trình DCP:
──────────────
Khởi tạo: coverCount[i] = 0 cho mỗi instance i
           fullyCovered[i] = false

FOR mỗi luật R (theo thứ tự CMAR: conf↓, sup↓, len↑):
  useful = false
  FOR mỗi instance i chưa fully covered:
    IF R match antecedent VÀ R đúng class → useful = true; break
  
  IF useful:
    selected.add(R)
    FOR mỗi instance i chưa fully covered:
      IF R match VÀ đúng class:
        coverCount[i]++
        IF coverCount[i] >= δ (=3):
          fullyCovered[i] = true
```

**Ý nghĩa:** Mỗi instance chỉ cần được cover bởi δ = 3 luật đúng là đủ → dừng giữ luật cho instance đó.

#### Phase 0 (tuỳ chọn): General-to-Specific Pruning

**Mục đích:** Nếu luật tổng quát (ít item hơn) có confidence ≥ luật cụ thể (nhiều item hơn, cùng class), thì loại luật cụ thể.

```
VD: {A} => class1 [conf=0.9]  ← GIỮ
    {A, B} => class1 [conf=0.85]  ← LOẠI (vì {A} tổng quát hơn & conf cao hơn)
```

---

### 3.6. `CRTree.java` — Cây Luật Phân Loại

**File:** `src/cmar/CRTree.java` (95 dòng)

**Mô tả:** Cấu trúc dữ liệu lập chỉ mục (index) cho luật, giúp truy xuất nhanh khi phân loại.

#### Cấu trúc lập chỉ mục:
```
index: classLabel → firstItem → List<Rule>

Ví dụ:
  class 0:
    item 3 → [Rule1, Rule2]
    item 7 → [Rule3]
  class 1:
    item 1 → [Rule4, Rule5, Rule6]
    item 5 → [Rule7]
```

#### Tối ưu khi tìm luật matching:
1. **Phân vùng theo class** → giảm không gian tìm kiếm
2. **Index theo item đầu tiên** của antecedent
3. **Quick check:** Nếu item đầu tiên không có trong bitmap → bỏ qua toàn bộ nhóm luật

```java
// Kiểm tra nhanh: item đầu tiên có trong instance không?
int idx = firstItem >> 6;
int bit = firstItem & 63;
if (idx >= bitmap.length || (bitmap[idx] & (1L << bit)) == 0)
    continue;  // Bỏ qua → tiết kiệm thời gian kiểm tra chi tiết
```

---

### 3.7. `CMARClassifier.java` — Bộ Phân Loại Chính

**File:** `src/cmar/CMARClassifier.java` (225 dòng)

**Mô tả:** Lớp chính điều phối toàn bộ pipeline: training → pruning → indexing → prediction.

#### Tham số:
| Tham số | Mặc định | Ý nghĩa |
|---------|----------|---------|
| `minSupport` | 2 | Ngưỡng support tối thiểu |
| `minConfidence` | 0.5 | Ngưỡng confidence tối thiểu |
| `chiSquareThreshold` | 3.841 | Ngưỡng chi² (p=0.05) |
| `maxCoverageCount` | 4 | δ - coverage threshold |
| `maxRulesPerClass` | 80000 | Giới hạn số luật mỗi lớp |
| `maxAntecedentLength` | 6 | Độ dài tối đa antecedent |

#### Quy trình Huấn Luyện (`fit()`):

```
INPUT: transactions[][] (giao dịch), labels[] (nhãn)

PHASE 1 - KHAI PHÁ:
  FPGrowth.mineRules(transactions, labels)
  → Kết quả: danh sách luật thô (chưa tỉa)

PHASE 2 - TỈA LUẬT:
  RulePruner.prune(rules, transactions, labels)
  = chiSquarePrune() → coveragePrune()
  → Kết quả: danh sách luật đã tỉa

PHASE 3 - TÍNH TRỌNG SỐ:
  Với mỗi luật: weight = computeNormalizedChiSquare()
  → weight = χ² / max_χ² (chuẩn hóa để loại bias lớp đa số)

PHASE 4 - LẬP CHỈ MỤC:
  CRTree.build(prunedRules)
  → Kết quả: cây chỉ mục sẵn sàng cho prediction
```

#### Công thức Normalized Chi-Square:

```
weight = χ² / max_χ²

Trong đó:
  max_χ² = maxDev² × e
  maxDev = min(supP, supC) - supP × supC / N
  e = N × (1/(rowA×colA) + 1/(rowA×colB) + 1/(rowB×colA) + 1/(rowB×colB))
  
  rowA = supP          (support antecedent)
  rowB = N - supP
  colA = supC          (support class)
  colB = N - supC
```

> **Mục đích chuẩn hóa:** Loại bỏ bias theo kích thước lớp. Lớp lớn tự nhiên có χ² cao hơn → chuẩn hóa giúp công bằng.

#### Quy trình Phân Loại (`predict()`):

```
INPUT: instance[] (một mẫu cần phân loại)

Bước 1: Chuyển instance → bitmap (long[])
Bước 2: Tìm tất cả luật matching từ CRTree
Bước 3: Sắp xếp theo CMAR ordering (conf↓, sup↓, len↑)

Bước 4: KIỂM TRA ĐỒNG THUẬN
  - Lấy tất cả luật có confidence cao nhất (= bestConf)
  - Nếu TẤT CẢ đều dự đoán cùng 1 class → Trả về class đó

Bước 5: BỎ PHIẾU CÓ TRỌNG SỐ (nếu không đồng thuận)
  - Nhóm luật theo class
  - Với mỗi class: lấy top-5 luật mạnh nhất (theo weight)
  - Score(class) = tổng weight của top-5 luật
  - Trả về class có score CAO NHẤT

Bước 6: NẾU KHÔNG CÓ LUẬT NÀO MATCH
  → Trả về defaultClass (lớp đa số trong training data)
```

**Sơ đồ quyết định phân loại:**
```
             Luật matching?
            /              \
          Có              Không
          |                 |
    Đồng thuận?        → defaultClass
    /          \
  Có          Không
  |             |
→ class đó   → Weighted voting
              (top-5 per class)
```

---

## 4. Hệ Thống Benchmark

### 4.1. `DataLoader.java` — Đọc và Tiền Xử Lý Dữ Liệu

**Chức năng chính:**
- Đọc file CSV local hoặc tải từ UCI Repository
- Tự động phát hiện thuộc tính **numeric** vs **categorical**
- **Numeric:** Rời rạc hóa bằng **equal-frequency binning** (quantile-based)
- **Categorical:** Mã hóa trực tiếp thành integer
- Numeric có ít giá trị unique (≤ numBins + 1) → xử lý như categorical

**Sơ đồ mã hóa item:**
```
Attribute 0: offset=0,  values 0..k₀-1
Attribute 1: offset=k₀, values k₀..k₀+k₁-1
...
→ Mỗi (attribute, value) pair → 1 item ID duy nhất
```

### 4.2. `UCIDatasets.java` — 26 Dataset từ Paper Gốc

Tải và chuẩn bị **26 datasets** UCI được dùng trong paper CMAR gốc:

| # | Dataset | Instances | Attrs | Classes | Paper CMAR Acc |
|---|---------|-----------|-------|---------|----------------|
| 1 | Anneal | 898 | 38 | 6 | 97.3% |
| 2 | Australian | 690 | 14 | 2 | 86.1% |
| 3 | Auto | 205 | 25 | 7 | 78.1% |
| 4 | Breast-Cancer | 683 | 9 | 2 | 96.4% |
| 5 | Cleve | 303 | 13 | 2 | 82.2% |
| 6 | Crx | 690 | 15 | 2 | 84.9% |
| 7 | Diabetes | 768 | 8 | 2 | 75.8% |
| 8 | German | 1000 | 20 | 2 | 74.9% |
| 9 | Glass | 214 | 9 | 6 | 70.1% |
| 10 | Heart | 270 | 13 | 2 | 82.2% |
| 11 | Hepatitis | 155 | 19 | 2 | 80.5% |
| 12 | Horse | 368 | 22 | 2 | 82.6% |
| 13 | Hypo | 3163 | 25 | 2 | 98.4% |
| 14 | Iono | 351 | 34 | 2 | 91.5% |
| 15 | Iris | 150 | 4 | 3 | 94.0% |
| 16 | Labor | 57 | 16 | 2 | 89.7% |
| 17 | Led7 | 3200 | 7 | 10 | 72.5% |
| 18 | Lymphography | 148 | 18 | 4 | 83.1% |
| 19 | Pima | 768 | 8 | 2 | 75.1% |
| 20 | Sick | 2800 | 29 | 2 | 97.5% |
| 21 | Sonar | 208 | 60 | 2 | 79.4% |
| 22 | Tic-Tac-Toe | 958 | 9 | 2 | 99.2% |
| 23 | Vehicle | 846 | 18 | 4 | 69.0% |
| 24 | Waveform | 5000 | 21 | 3 | 83.2% |
| 25 | Wine | 178 | 13 | 3 | 95.0% |
| 26 | Zoo | 101 | 16 | 7 | 97.1% |

### 4.3. `BenchmarkRunner.java` — Đánh Giá Hiệu Năng

- **Stratified 10-fold cross-validation** (phân tầng)
- Tự động tính minSupport tuyệt đối từ tỉ lệ trong paper
- Dataset-specific `maxAntecedentLength` (5 nếu ≤ 10 attrs, 4 nếu > 10)
- Xuất report Markdown cho từng dataset + tổng hợp

### 4.4. `DiagnosticRunner.java` — Chẩn Đoán

- Phân tích phân bố luật theo lớp
- Hiển thị confidence trung bình mỗi lớp
- Kiểm tra training accuracy

---

## 5. Luồng Xử Lý Tổng Thể (End-to-End Pipeline)

```
┌─────────────────────────────────────────────────────────────────┐
│                    TRAINING PIPELINE                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Raw Data]                                                     │
│      │                                                          │
│      ▼                                                          │
│  ┌──────────────┐                                               │
│  │  DataLoader   │ → Discretize + Encode → int[][] transactions │
│  └──────┬───────┘                                               │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │  FP-Growth    │                                              │
│  │  ┌──────────┐│   1. Xây FP-Tree (2 lần quét)                │
│  │  │ FP-Tree  ││   2. Khai phá đệ quy tập phổ biến            │
│  │  │ FP-Node  ││   3. Sinh CARs (bitmap scan per class)        │
│  │  └──────────┘│                                               │
│  └──────┬───────┘                                               │
│         │ List<Rule> (luật thô)                                 │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │ RulePruner   │                                               │
│  │  Phase 1: CSP│   Chi-Square Pruning → loại luật vô nghĩa    │
│  │  Phase 2: DCP│   Coverage Pruning → loại luật dư thừa        │
│  └──────┬───────┘                                               │
│         │ List<Rule> (luật đã tỉa)                              │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │ Weight Calc  │   weight = χ²_normalized (chuẩn hóa)          │
│  └──────┬───────┘                                               │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │   CR-Tree    │   Index: class → firstItem → rules            │
│  └──────────────┘                                               │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                   PREDICTION PIPELINE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [New Instance]                                                 │
│      │                                                          │
│      ▼                                                          │
│  toBitmap() → long[]                                            │
│      │                                                          │
│      ▼                                                          │
│  CR-Tree.findAllMatching(bitmap) → List<Rule>                   │
│      │                                                          │
│      ▼                                                          │
│  ┌─────────────────────────┐                                    │
│  │ Luật cao nhất đồng thuận│ ──YES──→ Trả về class đó           │
│  └────────┬────────────────┘                                    │
│           │NO                                                   │
│           ▼                                                     │
│  ┌─────────────────────────┐                                    │
│  │ Weighted Chi² Voting    │                                    │
│  │ (top-5 per class)       │ → Trả về class có score cao nhất   │
│  └─────────────────────────┘                                    │
│                                                                 │
│  [Nếu không có luật nào match → defaultClass (lớp đa số)]      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Các Tối Ưu Đã Áp Dụng

| # | Tối ưu | Mô tả | Độ phức tạp |
|---|--------|-------|-------------|
| 1 | **Bitmap Rule Matching** | Mã hóa instance thành `long[]`, kiểm tra subset bằng AND bitwise | O(k/64) thay vì O(k×n) |
| 2 | **Hash-indexed CR-Tree** | Phân vùng theo class + index theo first item | O(1) amortized lookup |
| 3 | **Single-path FP-Tree** | Khi cây chỉ 1 đường đi → liệt kê bằng bitmask 2^n | Tránh đệ quy không cần thiết |
| 4 | **Insertion Sort** | Sắp xếp item trong giao dịch (mảng nhỏ) | O(n²) nhưng nhanh hơn O(n log n) cho n nhỏ |
| 5 | **Quantile Binning** | Rời rạc hóa numeric bằng equal-frequency | Phân bố đều item → FP-tree cân bằng hơn |
| 6 | **Per-class Adaptive MinSup** | Lớp hiếm (≤10 instances) dùng minSup = 1 | Tránh mất luật cho minority class |
| 7 | **Normalized Chi-Square Weight** | weight = χ²/max_χ² | Loại bias lớp đa số trong voting |
| 8 | **Top-K Voting** | Chỉ lấy top-5 luật mạnh nhất mỗi class | Tránh "majority flood" |
| 9 | **Max Antecedent Length Cap** | Giới hạn 4-6 items/luật | Giảm overfitting + giảm không gian tìm kiếm |
| 10 | **Early Stopping in Coverage** | Dừng khi tất cả instance đã fully covered | Tiết kiệm thời gian tỉa |

---

## 7. Độ Phức Tạp Thuật Toán

### Thời gian:
| Giai đoạn | Worst Case | Thực tế |
|-----------|-----------|---------|
| Xây FP-Tree | O(N × M) | N = #transactions, M = avg items/txn |
| FP-Growth Mining | O(2^M) | Giới hạn bởi maxAntecedentLength |
| Chi-Square Pruning | O(R × N) | R = #rules, quét bitmap mỗi rule |
| Coverage Pruning | O(R × N) | Dừng sớm khi đủ cover |
| Prediction (1 instance) | O(R') | R' = #rules sau tỉa (thường << R) |

### Bộ nhớ:
| Cấu trúc | Kích thước |
|-----------|-----------|
| FP-Tree | O(N × M) nút worst case, thường << nhờ nén prefix |
| Bitmap/instance | O(maxItem / 64) words |
| CR-Tree | O(R' × avg_antecedent_length) |

---

## 8. Cấu Trúc File Dự Án

```
E:\Thuật toán Cmar\
├── src/cmar/
│   ├── Main.java              ← Demo + benchmark nhỏ
│   ├── CMARClassifier.java    ← Bộ phân loại chính (225 dòng)
│   ├── FPGrowth.java          ← Khai phá FP-Growth (190 dòng)
│   ├── FPTree.java            ← Cây FP-Tree (172 dòng)
│   ├── FPNode.java            ← Nút FP-Tree (41 dòng)
│   ├── Rule.java              ← Luật kết hợp lớp (90 dòng)
│   ├── RulePruner.java        ← Tỉa luật CSP + DCP (210 dòng)
│   ├── CRTree.java            ← Cây lập chỉ mục luật (95 dòng)
│   └── benchmark/
│       ├── BenchmarkRunner.java    ← Chạy 10-fold CV
│       ├── DataLoader.java         ← Đọc + tiền xử lý
│       ├── DiagnosticRunner.java   ← Chẩn đoán
│       └── UCIDatasets.java        ← 26 datasets UCI
├── datasets/                  ← Dữ liệu UCI gốc
├── results/                   ← Báo cáo kết quả
└── out/cmar/                  ← Bytecode đã biên dịch
```

**Tổng cộng:** ~2,000 dòng code Java (core) + ~1,000 dòng benchmark

---

## 9. Tóm Tắt Thuật Toán Bằng Pseudocode

```
ALGORITHM CMAR_Train(D, minSup, minConf, χ²_threshold, δ)
────────────────────────────────────────────────────────
Input:  D = {(transaction, label)} - tập huấn luyện
Output: CR-Tree chứa luật đã tỉa

1.  tree ← BuildFPTree(D, minSup)
2.  itemsets ← FPGrowth(tree, maxLen=6)
3.  rules ← ∅
4.  FOR EACH itemset I IN itemsets:
5.      FOR EACH class c:
6.          sup_Ic ← |{t ∈ D : I ⊆ t ∧ label(t) = c}|
7.          conf ← sup_Ic / |{t ∈ D : I ⊆ t}|
8.          IF sup_Ic ≥ minSup AND conf ≥ minConf:
9.              rules.add(I → c, sup_Ic, conf)
10. // Chi-Square Pruning
11. FOR EACH rule r IN rules:
12.     Compute χ²(r) from 2×2 contingency table
13.     IF χ²(r) < χ²_threshold OR conf(r) ≤ prior(c):
14.         Remove r
15. // Database Coverage Pruning
16. Sort rules by (conf↓, sup↓, len↑)
17. coverCount[1..N] ← 0
18. selected ← ∅
19. FOR EACH rule r (in order):
20.     IF ∃ uncovered instance correctly classified by r:
21.         selected.add(r)
22.         Update coverCount, mark fully covered if count ≥ δ
23. // Build index
24. crTree ← BuildCRTree(selected)
25. RETURN crTree

ALGORITHM CMAR_Predict(x, crTree, defaultClass)
────────────────────────────────────────────────
Input:  x = instance to classify
Output: predicted class label

1.  matched ← crTree.findAllMatching(toBitmap(x))
2.  IF matched = ∅: RETURN defaultClass
3.  Sort matched by (conf↓, sup↓, len↑)
4.  topConf ← matched[0].confidence
5.  topClasses ← {r.class : r ∈ matched, r.conf = topConf}
6.  IF |topClasses| = 1: RETURN topClasses[0]
7.  // Weighted voting
8.  FOR EACH class c IN matched:
9.      score(c) ← sum of top-5 weights of rules predicting c
10. RETURN argmax_c score(c)
```

---

> **Kết luận:** Đây là implementation khá hoàn chỉnh và được tối ưu tốt của thuật toán CMAR, bám sát paper gốc (Li, Han, Pei 2001). Các điểm nổi bật gồm: bitmap matching, normalized chi-square weighting, adaptive per-class support, và hệ thống benchmark đầy đủ với 26 datasets UCI.
