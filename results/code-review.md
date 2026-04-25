# Đánh Giá Mã Nguồn CMAR Java

**Ngày đánh giá:** 2026-04-21
**Người đánh giá:** Hệ thống kiểm tra tự động
**Phiên bản tham chiếu:** Li, Han, Pei 2001 — "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules"

---

## Tổng Quan

Codebase gồm 7 lớp chính (~1.400 dòng logic thuần) triển khai đầy đủ pipeline CMAR: khai phá luật bằng FP-Growth → cắt tỉa chi-square + G2S + DCP → phân loại bằng bầu chọn weighted chi-square. Kết quả thực nghiệm đạt 86,2% trung bình so với 85,2% trong bài báo (+1,0%), cho thấy độ chính xác tốt hơn kỳ vọng. Mã nguồn rõ ràng, có Javadoc hợp lý và cấu trúc phân lớp đúng.

---

## Đánh Giá Độ Trung Thực Với Bài Báo

### Phase 1 — Khai phá luật bằng FP-Growth (`FPGrowth.java`, `FPTree.java`)

**Đúng:** FP-tree được xây theo chuẩn Han 2000; dùng `minSupport` nhất quán cho cả cây gốc lẫn cây điều kiện (`FPTree.java:63`). Hàm `mineRules` tính đúng support và confidence theo từng class.

**Sai lệch nhỏ:** Bài báo không giới hạn độ dài antecedent; tham số `maxAntecedentLength=4` (`CMARClassifier.java:31`) là kỹ thuật heuristic không có trong paper. Điều này tốt cho thực tế nhưng khác paper.

**Vấn đề tiềm ẩn:** `FPGrowth.java:89` kiểm tra `clsSup >= minSupport` lần thứ hai sau khi đã lọc ở bước itemset — dư thừa nhưng không sai.

### Phase 2 — Cắt tỉa (`RulePruner.java`)

**Đúng:** Ba bước đúng thứ tự: CSP → G2S → DCP (dòng 199-201).

**Chi-Square Pruning (CSP):** Công thức 2×2 contingency table (`RulePruner.java:31-48`) đúng với paper Section 3.1. Điều kiện giữ luật (`chi2 >= threshold AND conf > priorProb`) khớp paper.

**G2S Pruning:** Logic đúng — loại luật specific khi tồn tại general rule cùng class có `confidence > specific.confidence`. Tuy nhiên điều kiện là `general.confidence > specific.confidence` (`RulePruner.java:120`), trong khi paper nói "không kém hơn" (≥). Điều này làm G2S ít tích cực hơn paper một chút.

**DCP Pruning:** Ngưỡng `maxCoverageCount=4` (`CMARClassifier.java:31`) khớp comment trong code. Bài báo gốc dùng delta=3 nhưng comment trong `RulePruner.java:11` ghi `delta=3` trong khi constructor mặc định dùng `4`. Có mâu thuẫn nội bộ.

### Phase 3 — Phân loại (`CMARClassifier.java`)

**Đúng:** Chiến lược hai bước đúng: (1) nếu tất cả luật confidence cao nhất đồng thuận → chọn ngay; (2) bầu chọn weighted chi-square.

**Sai lệch quan trọng:** Bài báo Section 4 mô tả weight = chi² thô (raw chi-square). Code triển khai weight = chi²/max_chi² (normalized, `CMARClassifier.java:161-179`). Chuẩn hóa này không có trong paper gốc. Mục đích tốt (giảm bias class đa số) nhưng đây là bổ sung ngoài paper.

**Sai lệch phân loại:** `predict()` dùng `r.weight` (normalized) để vote nhưng `computeNormalizedChiSquare()` tính max_chi² bằng công thức phức tạp không được cite trong paper. Hiệu quả trong thực tế nhưng không phải "faithful implementation".

### Discretization (`MDLDiscretizer.java`, `DataLoader.java`)

**Đúng:** MDL criterion Fayyad & Irani 1993 được triển khai đúng (`MDLDiscretizer.java:97-117`): `Gain > (log2(N-1) + delta) / N`. Đệ quy chia đôi tìm cut points đúng chuẩn.

**Tốt:** `DataLoader.encodeFold()` tính cut points từ **tập train** mỗi fold — tránh data leakage đúng cách.

**Vấn đề:** `DataLoader.parseMDL()` (dùng khi không có per-fold encoding) tính MDL trên **toàn bộ dữ liệu** (`DataLoader.java:243`) — đây là data leakage khi dùng trong BenchmarkRunner hiện tại vì `BenchmarkRunner` dùng `ds.transactions` đã được encode toàn cục, không gọi `encodeFold`.

---

## Điểm Mạnh

1. **Bitmap matching** cho cả matching luật và tính support — hiệu quả O(w) thay vì O(len).
2. **CRTree** phân vùng theo class + first-item pruning giảm thời gian lookup khi predict.
3. **Stratified k-fold** đúng chuẩn (`BenchmarkRunner.java:56-67`), seed cố định đảm bảo tái lập.
4. **Error handling** hợp lý: `antSupport == 0` kiểm tra trước chia (`RulePruner.java:73`), `maxDev <= 0` kiểm tra trước tính max_chi² (`CMARClassifier.java:169`).
5. **FP-tree single-path optimization** (`FPTree.java:111`, `FPGrowth.java:158`) — đúng chuẩn paper Han 2000.
6. **Thực nghiệm 26 dataset** từ paper gốc, tải dữ liệu UCI thực tế với fallback synthetic.

---

## Vấn Đề Phát Hiện

### Critical

**C1. Data Leakage trong CV — `DataLoader.java:243`, `BenchmarkRunner.java:145`**
`BenchmarkRunner.runBenchmark()` dùng `ds.transactions` đã encode bằng `parseMDL()` trên toàn bộ dữ liệu. MDL cut points được tính từ cả train+test → test set ảnh hưởng đến discretization → accuracy inflate. Infrastructure `encodeFold()` đã có nhưng không được dùng trong benchmark chính.

**C2. Mâu thuẫn delta trong comment và constructor — `RulePruner.java:11` vs `RulePruner.java:25`**
Comment file ghi `delta=3` (paper gốc) nhưng constructor mặc định dùng `4`. `CMARClassifier.java:31` cũng dùng `4`. Nếu paper dùng 3, kết quả so sánh không hoàn toàn fair.

### Major

**M1. G2S dùng `>` thay `>=` — `RulePruner.java:120`**
```java
// Hiện tại:
&& general.confidence > specific.confidence
// Paper nói: "no less than" → nên là:
&& general.confidence >= specific.confidence
```
Làm giảm hiệu quả G2S pruning, giữ lại nhiều luật dư thừa hơn paper.

**M2. Weight không theo paper — `CMARClassifier.java:74-79`**
Paper Section 4 dùng chi² thô làm weight. Code dùng chi²/max_chi² chuẩn hóa. Ảnh hưởng đến tính so sánh kết quả.

**M3. `maxRulesPerClass=80000` không tôn trọng thứ tự — `FPGrowth.java:89`**
Khi đạt giới hạn `maxRulesPerClass`, các luật bị bỏ qua theo thứ tự itemset (không phải theo confidence/support). Nên sort itemsets trước hoặc dùng priority queue.

**M4. Header table traversal O(n) mỗi lần insert — `FPTree.java:116-119`**
```java
FPNode last = headerTable.get(item);
while (last.link != null) last = last.link; // O(n) traversal
```
Nên lưu con trỏ đến node cuối để insert O(1).

### Minor

**m1. `readCsvFirst()` gọi `readLocalFile()` hai lần giống nhau — `UCIDatasets.java:67-69`**
```java
String csv = DataLoader.readLocalFile(csvPath);
if (csv != null) return csv;
return DataLoader.readLocalFile(csvPath); // dư thừa
```

**m2. `Rule.compareTo()` dùng `!=` với double — `Rule.java:33`**
```java
if (this.confidence != other.confidence) // floating-point unsafe
```
Nên dùng `Double.compare()` hoặc kiểm tra `Math.abs(diff) > epsilon`.

**m3. `BenchmarkRunner.java:309` hardcode ngày "2026-03-12"** — nên dùng `LocalDate.now()`.

**m4. `MDLDiscretizer.entropy()` tạo `HashMap` mới mỗi lần gọi — `MDLDiscretizer.java:122`**
Hàm được gọi O(N log N) lần, mỗi lần allocate HashMap — gây GC pressure. Có thể dùng mảng đếm nếu class labels là small integers.

**m5. `CRTree.findAllMatching()` quét tuyến tính qua `allRules` — `CRTree.java:75-82`**
Không dùng index đã build. Trong khi đó `findMatchingRules()` dùng first-item index đúng. Hàm `predict()` gọi `findAllMatching()` → bỏ qua tối ưu hóa.

---

## Đề Xuất Cải Tiến

1. **Sửa data leakage:** Thay `ds.transactions` trong `BenchmarkRunner.evaluateConfig()` bằng `encodeFold()` — cơ sở hạ tầng đã có sẵn tại `DataLoader.encodeFold()`.

2. **Sửa G2S condition** tại `RulePruner.java:120`: đổi `>` thành `>=` để khớp paper.

3. **Thêm tùy chọn raw chi² weight** song song với normalized để có thể chạy paper-faithful và optimized cùng lúc.

4. **Tối ưu header table** tại `FPTree.java:115-119`: lưu `Map<Integer, FPNode> headerTableTail` để insert O(1).

5. **Sửa `findAllMatching`** tại `CRTree.java:75`: dùng index đã build thay vì quét tuyến tính.

6. **Sửa delta comment** tại `RulePruner.java:11`: đổi `delta=3` thành `delta=4` hoặc xác nhận giá trị đúng với paper.

7. **Sửa `readCsvFirst`** tại `UCIDatasets.java:67-69`: bỏ lần gọi thứ hai dư thừa.

---

## Kết Luận

Triển khai CMAR đạt chất lượng tốt, logic thuật toán cơ bản đúng với paper, và kết quả thực nghiệm vượt nhẹ so với paper (+1,0%). Hai vấn đề cần ưu tiên sửa trước khi dùng làm kết quả học thuật: **(C1) data leakage trong cross-validation** có thể làm inflate accuracy, và **(M1) điều kiện G2S sai** làm kết quả không hoàn toàn tái lập paper. Các vấn đề còn lại là minor và không ảnh hưởng đến tính đúng đắn cơ bản. Codebase sẵn sàng cho thực nghiệm sau khi vá hai điểm trên.

| Hạng mục | Đánh giá |
|---|---|
| Độ trung thực với paper | 85% |
| Độ chính xác kết quả | Tốt (86,2% vs 85,2%) |
| Chất lượng mã | Tốt |
| An toàn kiểu dữ liệu | Tốt (một điểm minor) |
| Hiệu năng | Tốt (một điểm Major) |
| Data integrity (CV) | Cần sửa (C1) |
