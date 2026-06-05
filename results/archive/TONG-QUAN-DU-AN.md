# CMAR — Tổng quan dự án

> Tài liệu định hướng để viết báo cáo / luận văn. Tóm tắt **toàn bộ những gì đã làm**: kiến trúc, cải tiến, thí nghiệm, kết quả.

---

## A. Bối cảnh

### A.1. CMAR là gì?
- **CMAR** = Classification based on Multiple Association Rules
- Bài báo gốc: **Li, Han, Pei — IEEE ICDM 2001**
- Thuộc nhóm thuật toán **Associative Classification (AC)**: kết hợp **luật kết hợp** (association rules) và **phân lớp** (classification)
- Khác CBA (1998): CMAR dùng **nhiều luật** để bỏ phiếu thay vì 1 luật, dùng **chi-square** để pruning thay vì pessimistic error

### A.2. Pipeline 5 bước
```
1. Discretize numeric attrs (MDL — Fayyad & Irani 1993)
2. Mining: FP-Growth → sinh tập luật ứng viên
3. Pruning:
   3a. Chi-square pruning (CSP)
   3b. General-to-Specific pruning (G2S)
   3c. Database Coverage pruning (DCP)
4. Indexing: lưu luật vào CR-Tree (hash by class + first item)
5. Predict: vote bằng weighted chi-square trên các luật khớp
```

---

## B. Kiến trúc code

### B.1. Cấu trúc thư mục
```
src/cmar/
├── Main.java                    Entry point
├── CMARClassifier.java          Train (fit) + predict
├── FPGrowth.java                Mining (baseline)
├── FPGrowthOptimized.java       Mining (improved, parallel + inverted index)
├── FPNode.java                  Node của FP-Tree
├── FPTree.java                  FP-Tree container
├── Rule.java                    Luật kết hợp (CAR)
├── CRTree.java                  Index luật để classify
├── RulePruner.java              3 chiến lược pruning (chi², G2S, coverage)
├── MDLDiscretizer.java          Rời rạc hóa numeric attributes
└── benchmark/
    ├── BenchmarkRunner.java     Chạy 10-fold CV trên 26 dataset UCI
    ├── DataLoader.java          Đọc CSV + encode + discretize per-fold
    └── UCIDatasets.java         Định nghĩa 26 dataset

datasets/                         CSV chuẩn hóa (input)
datasets_uci_raw/                 File gốc UCI (chỉ để tham chiếu)
results/                          Kết quả output
results/rules/                    Chi tiết luật mỗi dataset (CSV)
```

### B.2. Cấu trúc dữ liệu chính

#### FPNode (FP-Tree node)
```java
class FPNode {
    int item;                          // ID item
    int count;                         // Tần suất prefix qua node
    FPNode parent;                     // Truy ngược root
    Map<Integer, FPNode> children;     // Tra cứu con O(1)
    FPNode link;                       // Chain header table
}
```

#### Rule (Class Association Rule)
```java
class Rule {
    int[] antecedent;          // X — tiền đề (items đã sort)
    int classLabel;            // c — lớp đích
    int support;               // Sup(X→c)
    int antecedentSupport;     // Sup(X)
    double confidence;         // conf = sup/antSup
    double chiSquare;          // χ² statistic
    double weight;             // Trọng số voting (chuẩn-hóa χ²)
    double lift;               // Sup(X→c)·N / (Sup(X)·Sup(c))
    double hm;                 // Harmonic mean của conf & wSupp
    long[] antBitmap;          // Bitmap để match instance nhanh
}
```

#### CRTree (index luật)
```java
class CRTree {
    Map<Integer, Map<Integer, List<Rule>>> index;
    //   classLabel → firstItem → rules
    List<Rule> allRules;
}
```

---

## C. Cải tiến hiệu năng đã làm (Phase 01–17)

| Phase | Tối ưu | Tác dụng |
|---|---|---|
| 01 | Profiling infrastructure | Đo thời gian từng phase, peak memory |
| 02 | Hash-indexed CR-Tree | Predict O(K) thay O(N rules) |
| 05 | Baseline vs Improved switch | So sánh trước/sau cải tiến |
| 06 | Class-aware FP mining | Drop itemset vô ích sớm |
| 07 | Inverted item index + BitSet AND | Chi² + G2S nhanh hơn 5× |
| 08 | Deterministic compareTo | Ổn định với parallel mining |
| 12 | Header table tail pointer | Insert FP-tree O(1) |
| 13–14 | Conditional tree không dùng list | Bớt allocation |
| 15 | Top-k global voting flag | Voting nhanh, tunable |
| 16 | ThreadLocal scratch bitmap | Predict ít GC hơn |
| 17 | Single-path collection optimization | Mining nhanh khi tree đơn nhánh |

→ **Tổng hợp**: tăng tốc **~5.28× so với baseline gốc**, giữ nguyên accuracy.

---

## D. Thí nghiệm đã chạy (đối tượng nghiên cứu)

### D.1. Top-k voting (TRỤC 1)

| Cấu hình | Mô tả | Avg Acc |
|---|---|---:|
| **topK=0** | Vote ALL luật khớp (paper-faithful) | **85.3%** ⭐ |
| topK=3 | Top 3 luật mỗi predict | 84.7% |
| topK=5 | Top 5 luật | 84.8% |
| topK=7 | Top 7 luật | 85.1% |

→ Càng tăng k, accuracy càng tiệm cận topK=0. **k=0 thắng/hòa 20/26 dataset (77%)**.

### D.2. Voting weight (TRỤC 2) — đổi cách tính trọng số

| Strategy | Công thức | Avg | Δ |
|---|---|---:|---:|
| **Chi² (default)** | `w = χ² / max_χ²` | **85.3%** | — |
| **Lift** | `w = Lift` | 85.2% | −0.1 |
| **HM** | `w = HM (F1-like)` | 85.2% | −0.1 |
| ❌ avgVote | trung bình (thay vì sum) | 82.1% | **−3.2** |

→ 3 trọng số chính (χ², Lift, HM) gần như **tương đương trung bình**, nhưng **phân bố theo dataset khác**:
- **Lift voting**: tốt hơn trên **Auto +1.1, Hepatitis +1.5, Iono +0.6, Sonar +0.5**
- **HM voting**: tốt hơn trên **Auto +0.9, Sonar +1.0, Iono +0.3**

### D.3. Sort order (TRỤC 3) — đổi cách xếp luật

| Strategy | Sort | Trạng thái |
|---|---|---|
| **CMAR (default)** | conf → sup → length | ✅ Tốt |
| WCBA-style (HM sort) | HM → Lift → length | ❌ **Phá coverage prune** → accuracy giảm 9% |

→ **KHÔNG nên đổi sort** mà không refactor coverage pruning. Sort thay đổi gây hiệu ứng dây chuyền: coverage prune phủ instance theo thứ tự sort → đổi sort = giữ luật khác = model collapse.

### D.4. Per-class top-k (TRỤC 4)

| Strategy | Mô tả | Avg |
|---|---|---:|
| Global top-k | k luật tốt nhất từ tất cả class | (như D.1) |
| **perClassTopK=5** | k luật tốt nhất **mỗi class** | **85.1%** |

→ **Cải thiện +0.3%** so với global top-k=5. Đặc biệt tốt cho dataset có **binary class với rule conflict**: Breast-Cancer, Crx, Tic-Tac-Toe.

### D.5. Lift filter (TRỤC 5)

| Strategy | Mô tả | Kết quả |
|---|---|---|
| `--liftOnly` | Thêm điều kiện Lift ≥ 1 | **No-op** (chi² + prior đã loại rồi) |

→ Phát hiện: trên codebase này, filter Lift là **redundant**.

---

## E. Kết quả tổng hợp

### E.1. So với paper gốc

| | Avg Acc | Δ vs Paper |
|---|---:|---:|
| Paper CMAR 2001 | 85.2% | — |
| **Our CMAR (improved + topK=0)** | **85.3%** ⭐ | **+0.1%** |
| Baseline (chưa cải tiến) | 84.5% | −0.7% |
| Paper CBA | 84.7% | — |
| Paper C4.5 | 83.3% | — |

→ Implementation **vượt paper +0.1%**, thắng 14/26 dataset.

### E.2. Top dataset thắng paper

| Dataset | Our | Paper | Δ |
|---|---:|---:|---:|
| Auto | 81.4 | 78.1 | **+3.3** ⭐ |
| Hepatitis | 83.3 | 80.5 | **+2.8** ⭐ |
| Labor | 91.7 | 89.7 | **+2.0** |
| Wine | 96.7 | 95.0 | +1.7 |
| Sonar | 80.8 | 79.4 | +1.4 |
| Crx | 86.1 | 84.9 | +1.2 |
| Iono | 92.6 | 91.5 | +1.1 |

### E.3. Phân loại dataset theo sensitivity top-k

| Loại | Range | Số dataset | Ví dụ |
|---|---:|:---:|---|
| 🔵 Không nhạy | ≤ 0.3% | **11/26 (42%)** | Hypo, Sick, Zoo, Tic-Tac-Toe |
| 🟢 Nhạy nhẹ | 0.3–1% | 9/26 (35%) | Anneal, Australian, Cleve |
| 🟡 Nhạy vừa | 1–2% | 4/26 (15%) | Auto, Iono, Lymphography |
| 🔴 Cực nhạy | > 2% | **2/26 (8%)** | **Labor (8.7%!), Hepatitis (2.5%)** |

---

## F. Khuyến nghị cuối

### F.1. Cấu hình tốt nhất theo mục tiêu

| Mục tiêu | Cấu hình | Avg Acc |
|---|---|---:|
| 🎯 **Accuracy tối đa** | Default + **topK=0** | **85.3%** |
| ⚡ **Cân bằng acc/speed** | Default + **topK=7** | 85.1% |
| ⚡ Khác | **perClassTopK=5** | 85.1% |
| 🟡 Rule-noisy dataset | **Lift weight** + topK=0 | 85.2% |
| ❌ TRÁNH | avgVote, HM sort, full hybrid | < 84% |

### F.2. 3 đóng góp đáng đưa vào báo cáo

1. **Tối ưu hiệu năng 5.28× so với baseline** — vẫn giữ accuracy paper-level
2. **Phân tích sensitivity top-k per dataset** — 42% dataset không nhạy với top-k → có thể skip tuning
3. **So sánh 4 chiến lược voting weight** (χ², HM, Lift, avgVote) — đã verify empirically với 26 dataset

### F.3. Bài học kỹ thuật

| Bài học | Chi tiết |
|---|---|
| **Sort order side effect** | Đổi sort = đổi coverage prune behavior. Không thể "chỉ đổi sort". |
| **Lift filter trùng chi²** | χ² + prior check đã loại rule âm tương quan → Lift filter vô tác dụng |
| **avgVote tệ hơn sum** | Mất thông tin "số luật đồng thuận" |
| **HM định nghĩa quan trọng** | `wSupp = sup/N` (WCBA-style) khác `sup/classSupp` — cho kết quả khác xa |

---

## G. File báo cáo đã tạo

### G.1. Báo cáo phân tích

| File | Nội dung |
|---|---|
| ⭐ [TONG-QUAN-DU-AN.md](TONG-QUAN-DU-AN.md) | **File này** — overview toàn dự án |
| [BAO-CAO-HM-LIFT-VA-TOPK.md](BAO-CAO-HM-LIFT-VA-TOPK.md) | Phân tích top-k + Lift voting + HM (per dataset) |
| [BAO-CAO-TOPK-NAO-TOT-NHAT.md](BAO-CAO-TOPK-NAO-TOT-NHAT.md) | So sánh chi tiết k=0/3/5/7 |
| [BAO-CAO-SO-SANH-TOPK.md](BAO-CAO-SO-SANH-TOPK.md) | avgVote + perClassTopK experiments |
| [BAO-CAO-CHI-TIET-CAI-TIEN-CMAR.md](BAO-CAO-CHI-TIET-CAI-TIEN-CMAR.md) | 17 phase tối ưu hiệu năng |
| [BAO-CAO-HIEU-NANG-RUN2.md](BAO-CAO-HIEU-NANG-RUN2.md) | Profiling chi tiết thời gian/memory |

### G.2. Kết quả raw

| File | Nội dung |
|---|---|
| [summary-report.md](summary-report.md) | Tóm tắt accuracy + perf 26 dataset (lần chạy gần nhất) |
| `summary-report-<config>.md` | Mỗi cấu hình voting có 1 file riêng (topK=0/3/5/7, lift, HM, perclass, avgvote) |
| `<dataset>-report.md` | 26 file detail per dataset (fold-by-fold accuracy) |
| [rules/<dataset>-rules.csv](rules/) | **Chi tiết từng luật** + Lift/HM/χ² (8865 luật tổng cộng) |
| [profiling-metrics.csv](profiling-metrics.csv) | CSV thời gian từng phase mỗi dataset |

---

## H. Cách tái tạo kết quả

### H.1. Build
```powershell
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java
```

### H.2. Chạy benchmark
```powershell
# Best config (accuracy tối đa)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=0

# So sánh các top-k
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=3
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=5
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=7

# Đổi voting weight
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftWeight --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --hmWeightOnly --topK=0

# Baseline (CMAR gốc, chưa cải tiến)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Xuất chi tiết từng luật ra CSV
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=0 --dumpRules
```

### H.3. Hoặc chạy script tổng hợp
```powershell
.\run-benchmark.ps1
```
→ Compile + warmup JIT + chạy baseline + chạy improved + ghi báo cáo.

---

## I. Hướng phát triển có thể đề xuất trong báo cáo

| Hướng | Khả thi | Ý tưởng |
|---|---|---|
| **Adaptive top-k** | Cao | Tự chọn k theo độ nhạy của dataset (đã phân loại) |
| **Hybrid voting (Lift cho rule-noisy, χ² cho lại)** | Trung bình | Phát hiện dataset Lift-friendly tự động |
| **Refactor coverage pruning** để hỗ trợ sort thay thế | Khó | Mở đường cho HM sort khả thi |
| **Multi-label rule mining** | Mở | Bài review 2025 nhắc tới — chưa có ai làm trên CMAR framework |
| **Online learning** | Khó | Update CR-Tree khi có data mới |

---

## Mẫu cách trích dẫn trong báo cáo của bạn

> *"Trong nghiên cứu này, chúng tôi triển khai thuật toán CMAR (Li, Han, Pei 2001) trên 26 bộ dữ liệu UCI chuẩn với đánh giá 10-fold cross-validation. Sau khi tối ưu hiệu năng qua 17 phase, implementation đạt **độ chính xác trung bình 85.3%**, vượt nhẹ paper gốc (+0.1%) và hơn baseline cải tiến **+0.8%**. Chúng tôi đồng thời tiến hành 5 trục thí nghiệm: (1) top-k voting với 4 giá trị k, (2) 4 strategy trọng số voting (χ², Lift, HM, avgVote), (3) sort order alternative, (4) per-class top-k, (5) Lift filter. Kết quả cho thấy cấu hình paper-faithful (topK=0, χ² weight) vẫn là tốt nhất tổng thể, nhưng **Lift voting cải thiện rõ rệt trên dataset rule-noisy** (Auto +1.1%, Hepatitis +1.5%) và **perClassTopK=5 cho ưu thế khi cần predict nhanh**."*

---

> **Liên hệ trong code**: tất cả khái niệm trong báo cáo có thể truy ngược về code:
> - Pipeline → [`CMARClassifier.fit()`](src/cmar/CMARClassifier.java)
> - Pruning → [`RulePruner.java`](src/cmar/RulePruner.java)
> - Voting → [`CMARClassifier.predict()`](src/cmar/CMARClassifier.java)
> - Sort → [`Rule.compareTo()`](src/cmar/Rule.java)
