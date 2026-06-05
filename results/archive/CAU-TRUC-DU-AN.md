# Cấu trúc dự án CMAR

> Mô tả kiến trúc code và pipeline thuật toán. Tham chiếu: Li, Han, Pei — *CMAR: Accurate and efficient classification based on multiple class-association rules* (ICDM 2001).

---

## 1. Tổ chức thư mục

```
project/
├── src/cmar/                        Mã nguồn Java
│   ├── Main.java                    Entry point
│   ├── CMARClassifier.java          Lớp phân lớp chính (fit + predict)
│   ├── FPNode.java                  Node của FP-Tree
│   ├── FPTree.java                  Cấu trúc FP-Tree
│   ├── FPGrowth.java                Thuật toán mining (baseline)
│   ├── FPGrowthOptimized.java       Mining đã tối ưu
│   ├── Rule.java                    Class Association Rule (CAR)
│   ├── CRTree.java                  Index luật để phân lớp
│   ├── RulePruner.java              Ba chiến lược cắt tỉa
│   └── MDLDiscretizer.java          Rời rạc hóa thuộc tính số
│
├── src/cmar/benchmark/              Mã đánh giá
│   ├── BenchmarkRunner.java         Chạy 10-fold CV
│   ├── DataLoader.java              Đọc CSV + encode
│   └── UCIDatasets.java             Định nghĩa 26 dataset
│
├── datasets/                        Dữ liệu UCI (CSV)
├── datasets_uci_raw/                File gốc UCI (tham chiếu)
├── results/                         Kết quả thực nghiệm
│   ├── summary-report.md            Tổng kết accuracy + hiệu năng
│   ├── <dataset>-report.md          Chi tiết từng bộ
│   └── rules/                       Xuất chi tiết luật ra CSV
└── run-benchmark.ps1                Script chạy tự động
```

---

## 2. Pipeline thuật toán (5 bước)

```
INPUT: transactions + class labels
   │
   ▼
[1] DISCRETIZATION (MDL — Fayyad & Irani 1993)
   Rời rạc hóa thuộc tính số thành bin
   Cut points học theo per-fold (tránh leak dữ liệu test)
   │
   ▼
[2] MINING (FP-Growth)
   Xây FP-Tree → trích xuất pattern → sinh luật ứng viên
   Output: ~100,000 luật (raw)
   │
   ▼
[3] PRUNING (3 tầng)
   Tầng 3a: Chi-Square Pruning (CSP)
            Loại luật có χ² < 3.841 hoặc conf ≤ prior
   Tầng 3b: General-to-Specific Pruning (G2S)
            Loại luật đặc biệt khi luật tổng quát mạnh hơn
   Tầng 3c: Database Coverage Pruning (DCP)
            Giữ luật phủ instance mới (delta = 4)
   Output: ~100–500 luật cuối cùng
   │
   ▼
[4] INDEXING (CR-Tree)
   Tổ chức luật theo class label + first item
   Cho phép tra cứu nhanh khi predict
   │
   ▼
[5] PREDICT (Weighted χ² voting)
   Tìm luật khớp instance → sort theo CMAR order
   Sum weighted χ² của các luật khớp, theo class
   Class có sum cao nhất thắng
   │
   ▼
OUTPUT: predicted class label
```

---

## 3. Cấu trúc dữ liệu chính

### 3.1. FPNode (FP-Tree node)

Lớp `cmar.FPNode` — đại diện cho 1 node trong FP-Tree:

| Trường | Kiểu | Vai trò |
|---|---|---|
| `item` | int | ID của item tại node (-1 = root) |
| `count` | int | Tần suất prefix đi qua node |
| `parent` | FPNode | Con trỏ tới node cha |
| `children` | Map\<Integer, FPNode\> | Bản đồ tra cứu con O(1) |
| `link` | FPNode | Liên kết tới node kế cùng item (header table) |

### 3.2. Rule (Class Association Rule)

Lớp `cmar.Rule` — đại diện cho 1 luật `X → c`:

| Trường | Kiểu | Vai trò |
|---|---|---|
| `antecedent[]` | int[] | Mảng item IDs vế trái (đã sắp xếp) |
| `classLabel` | int | Lớp đích |
| `support` | int | Số transaction thoả X ∧ c |
| `antecedentSupport` | int | Số transaction thoả X |
| `confidence` | double | conf = support / antecedentSupport |
| `chiSquare` | double | Giá trị χ² thống kê |
| `weight` | double | Trọng số voting (chuẩn-hóa χ²) |
| `lift` | double | Sup(X→c)·N / (Sup(X)·Sup(c)) |
| `hm` | double | Harmonic mean của conf và wSupport |
| `antBitmap[]` | long[] | Bitmap antecedent để match nhanh |

### 3.3. CRTree (index luật)

Lớp `cmar.CRTree` — tra cứu luật khi predict:

```
Map<Integer, Map<Integer, List<Rule>>>
//   classLabel  →  firstItem  →  rules
```

Cho phép predict trên 1 instance theo 3 bước:
1. Duyệt từng class
2. Duyệt từng `firstItem` có trong instance
3. Match `antBitmap` của luật với instance bitmap

---

## 4. Thứ tự sắp xếp luật (theo paper)

Hàm `Rule.compareTo()` định nghĩa thứ tự ưu tiên:

```
1. Confidence DESC    (cao → thấp)           [tiêu chí chính]
2. Support     DESC   (cao → thấp)           [khi conf bằng nhau]
3. Length      ASC    (ngắn → dài)           [khi conf+sup bằng nhau]
4. Antecedent  lexicographic                 [tie-breaker]
5. ClassLabel                                [tie-breaker cuối]
```

**Lý do**: paper CMAR ưu tiên luật **tin cậy nhất** (confidence cao), sau đó là luật **phổ biến nhất** (support cao), cuối cùng là luật **tổng quát nhất** (ngắn nhất). Tie-breakers đảm bảo thứ tự deterministic kể cả khi mining song song.

---

## 5. Chiến lược cắt tỉa (3 tầng)

### Tầng 1 — Chi-Square Pruning (CSP)

Loại luật nếu **ít nhất 1 điều kiện đúng**:
- `confidence < 0.5` (mặc định)
- `χ² < 3.841` (tương ứng p > 0.05)
- `confidence ≤ Sup(c)/N` (không tốt hơn random)

### Tầng 2 — General-to-Specific (G2S)

Cho luật A ⊂ B (A tổng quát hơn B):
- Nếu `χ²(B) ≤ χ²(A)` → loại B (luật đặc biệt không mạnh hơn).

### Tầng 3 — Database Coverage Pruning (DCP)

Duyệt luật theo thứ tự sort, mỗi luật:
- Tính số instance nó cover
- Nếu cover ≥ 1 instance "chưa được phủ" → giữ
- Đánh dấu instances đó là "covered"
- Mỗi instance được phủ tối đa bởi δ = 4 luật

---

## 6. Cơ chế voting khi predict

Hàm `CMARClassifier.predict(int[] instance)`:

```
1. Tìm tất cả luật khớp instance qua CR-Tree
2. Sort các luật khớp theo CMAR order
3. Nếu các luật conf cao nhất đều predict 1 class → trả về class đó
4. Ngược lại: với mỗi class c:
      score[c] = Σ weight(r)   với r là luật khớp predict class c
   Trả về class có score lớn nhất
```

**Trọng số `weight`**: theo paper = `χ² / max_χ²` (chuẩn-hóa).

---

## 7. Các tối ưu hiệu năng đã áp dụng

Không thay đổi logic thuật toán, chỉ tăng tốc thực thi:

| Tối ưu | Vị trí | Tác dụng |
|---|---|---|
| Hash-indexed CR-Tree | CRTree | Predict O(K) thay O(N rules) |
| Bitmap antecedent matching | Rule + CRTree | Match instance word-wise |
| Class-aware mining | FPGrowthOptimized | Drop itemset vô ích sớm |
| Inverted item index + BitSet AND | RulePruner | Chi² + G2S nhanh 5× |
| Header table tail pointer | FPTree | Insert FP-Tree O(1) |
| ThreadLocal scratch bitmap | CMARClassifier | Predict ít GC |
| Single-path optimization | FPGrowth | Mining nhanh khi tree đơn nhánh |
| Deterministic tie-breakers | Rule.compareTo | Ổn định với parallel mining |

---

## 8. Dữ liệu thực nghiệm

**26 dataset UCI** chuẩn của paper CMAR:

| Loại | Datasets |
|---|---|
| Y tế | Breast-Cancer, Diabetes, Hepatitis, Heart, Cleve, Pima, Sick, Hypo |
| Tài chính | Australian, Crx, German |
| Hình ảnh | Iris, Glass, Sonar, Iono, Vehicle, Waveform |
| Khác | Anneal, Auto, Horse, Labor, Led7, Lymphography, Tic-Tac-Toe, Wine, Zoo |

**Đánh giá**: 10-fold cross-validation (stratified).

**Tổng số instance**: 22,873.  
**Tổng số attribute**: dao động 4 (Iris) đến 60 (Sonar).  
**Số class**: 2 (binary) đến 10 (Led7).

---

## 9. Kết quả thực nghiệm tóm tắt

| Cấu hình | Avg Accuracy | Δ vs Paper |
|---|---:|---:|
| Paper CMAR 2001 | 85.2% | — |
| Baseline (chưa tối ưu) | 84.5% | −0.7% |
| **Improved + topK=0** | **85.3%** | **+0.1%** |
| Improved + Lift voting | 85.2% | 0.0% |

→ Implementation **vượt nhẹ paper +0.1%** trung bình.  
→ Thắng paper trên **14/26 dataset**.

---

## 10. Liên kết code

| Khái niệm trong báo cáo | Mã nguồn |
|---|---|
| Pipeline `fit` | [`CMARClassifier.java`](src/cmar/CMARClassifier.java) |
| FP-Growth mining | [`FPGrowthOptimized.java`](src/cmar/FPGrowthOptimized.java) |
| Chi-square + G2S + Coverage | [`RulePruner.java`](src/cmar/RulePruner.java) |
| Rule sort order | [`Rule.compareTo()`](src/cmar/Rule.java) |
| Predict + voting | [`CMARClassifier.predict()`](src/cmar/CMARClassifier.java) |
| Index luật | [`CRTree.java`](src/cmar/CRTree.java) |
| MDL discretization | [`MDLDiscretizer.java`](src/cmar/MDLDiscretizer.java) |
| Benchmark + 10-fold CV | [`BenchmarkRunner.java`](src/cmar/benchmark/BenchmarkRunner.java) |

---

## Tham khảo

1. Li, W., Han, J., & Pei, J. (2001). *CMAR: Accurate and efficient classification based on multiple class-association rules*. ICDM 2001.
2. Geng, X., Yang, Z., Jiao, L., Zhou, Z.-J., & Ma, Z. (2025). *Association rule-based classification: A comprehensive review of methodologies and applications*. Expert Systems With Applications, 280, 127454.
3. Fayyad, U., & Irani, K. (1993). *Multi-interval discretization of continuous-valued attributes for classification learning*. IJCAI.
4. Han, J., Pei, J., & Yin, Y. (2000). *Mining frequent patterns without candidate generation*. SIGMOD.
5. UCI Machine Learning Repository — Dua, D., & Graff, C. (2019).
