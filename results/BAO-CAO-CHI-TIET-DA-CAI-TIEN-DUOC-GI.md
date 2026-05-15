# BÁO CÁO CHI TIẾT — ĐÃ CẢI TIẾN ĐƯỢC GÌ (CMAR)

**Mục tiêu:** Tổng hợp rõ ràng *những cải tiến đã làm*, *làm ở đâu*, *tác động đo được*, và *cách kiểm chứng lại* trên repo này.

---

## 1. Tóm tắt nhanh (điểm ăn tiền)

### 1.1. Cải tiến cốt lõi (đúng bản chất bottleneck)
- **Chuyển từ “quét từng mẫu cho từng luật/tập điều kiện” sang “chỉ mục ngược + BitSet AND”** để tính support/match hàng loạt.
- **Chia sẻ kết quả match** giữa các bước pruning (Chi² / Coverage) để tránh “đếm lại cùng một thứ”.
- **Gỡ nút thắt General-to-Specific (G2S)** bằng subset check trên bitmap + lập chỉ mục theo độ dài → không còn phải “skip khi >10K luật”.

### 1.2. Tác động số liệu (đã đo trên 26 UCI datasets)
Repo hiện có **hai mốc benchmark** (khác thời điểm/điều kiện chạy):

- **Phase 08 (Final, cold-start, 1 run)** trong `results/BAO-CAO-CAI-TIEN-PHASE08.md`:
  - Tổng train: **70,461 ms → 13,339 ms (5.28×)**
  - Mining: **5.34×**, Pruning: **5.15×**
  - **Deterministic** (2 run identical accuracy)

- **Benchmark mới hơn (Phase 09–17, có warmup/ps1)** trong `results/CACH-CAI-TIEN-VA-KET-QUA.md`:
  - Tổng train: **23,302 ms → 4,216 ms (~5.53×)**
  - Accuracy TB: **85.1% → 85.3%** (không đánh đổi accuracy để lấy speed)

> Lưu ý: Hai bảng số liệu không mâu thuẫn; chúng đo ở **khác điều kiện chạy** (cold-start vs warmup, phiên bản code/flags khác). Điểm nhất quán là: **tăng tốc mạnh, accuracy giữ ổn định**.

---

## 2. Đã cải tiến gì theo từng “khối” kỹ thuật

## 2.1. Hạ tầng đo lường (để biết tối ưu đúng chỗ)

**Vấn đề trước:** không có số liệu tách phase → tối ưu dễ “mò mẫm”.

**Cải tiến:**
- Thêm đo thời gian theo phase qua `src/cmar/util/PhaseTimer.java`
- Thêm đo peak memory qua `src/cmar/util/MemorySampler.java`
- Harness benchmark 10-fold trên 26 datasets qua `src/cmar/benchmark/BenchmarkRunner.java`

**Tác động:** có bằng chứng định lượng để:
- xác định mining vs pruning chiếm bao nhiêu,
- so sánh baseline/improved công bằng,
- phát hiện hồi quy (regression) sau mỗi phase tối ưu.

---

## 2.2. Mining (FP-Growth) — chuyển “đếm thủ công” sang “BitSet AND”

**Bottleneck gốc:** mỗi itemset/rule phải quét N transactions để đếm support theo lớp → tốn \(O(\#itemsets \times N \times k)\).

**Cải tiến chính:**
- **Inverted index:** `item -> BitSet(transactions chứa item)` build 1 lần.
- **Support/match:** antecedent match = AND các BitSet → `cardinality()` để lấy support.
- **Class-aware mining:** kết hợp kiểm tra theo lớp sớm để bớt sinh luật “vô ích”.

**File liên quan (đầu mối đọc code):**
- `src/cmar/FPGrowthOptimized.java` (phiên bản improved)
- `src/cmar/FPGrowth.java` (baseline đối chứng)
- `src/cmar/FPTree.java`, `src/cmar/FPNode.java` (cấu trúc FP-tree)

**Tác động đo được:**
- Mining trong Phase 08: **48,871 ms → 9,153 ms (5.34×)** (xem `results/BAO-CAO-CAI-TIEN-PHASE08.md`).

---

## 2.3. Pruning — “tính một lần, dùng nhiều lần”

CMAR có 3 lớp pruning quan trọng:
- **Chi-square pruning**
- **General-to-Specific pruning (G2S)**
- **Database coverage pruning (δ=4)**

### 2.3.1. Chi-square pruning (nhanh hơn nhờ match BitSet)
**Trước:** quét transactions để đếm \(a,b,c,d\) trong bảng 2×2 → chậm.

**Sau:** dùng BitSet match + mask theo lớp để lấy counts nhanh hơn (AND + cardinality).

**Đầu mối code:** `src/cmar/RulePruner.java`

### 2.3.2. G2S pruning — bỏ “skip khi >10K luật”
**Vấn đề trước:** G2S \(O(n^2)\) nên baseline có đoạn **bỏ qua hoàn toàn** khi số luật lớn → ảnh hưởng chất lượng pruning/accuracy ở vài dataset.

**Cải tiến:**
- Mỗi rule có `long[] antBitmap` (dấu vân tay antecedent).
- Subset check cực nhanh:
  - \(A \subseteq B \iff (A \& \sim B) = 0\) (trên từng word 64-bit)
- Lập chỉ mục theo:
  - `class`,
  - `firstItem`,
  - `length bucket`
  để giảm số cặp cần so.

**Đầu mối code:** `src/cmar/RulePruner.java`, `src/cmar/Rule.java`

**Kiểm chứng:** trong Phase 08 có unit test subset bitmap được nhắc tới ở báo cáo (xem `results/BAO-CAO-CAI-TIEN-PHASE08.md`).

### 2.3.3. Coverage pruning — giảm work lặp
**Cải tiến:** tái sử dụng match/mask (BitSet) để:
- biết rule có “useful” không,
- cập nhật coverCount hiệu quả hơn,
- early-exit khi đã phủ đủ.

**Tác động đo được:**
- Pruning trong Phase 08: **21,601 ms → 4,197 ms (5.15×)**.

---

## 2.4. Tổ chức luật & phân lớp (lookup nhanh hơn, gọn hơn)

**Vấn đề trước:** nếu duyệt tuyến tính nhiều luật khi predict thì chậm và thừa.

**Cải tiến:**
- Dùng cấu trúc lưu/tra cứu luật theo class + item (CR-tree / index) để giảm số luật cần xét khi match.

**Đầu mối code:** `src/cmar/CRTree.java`, `src/cmar/CMARClassifier.java`

---

## 2.5. Song song hóa + đảm bảo determinism (Phase 08)

**Bài toán:** song song hóa mining dễ gây “race” làm thứ tự sinh luật khác nhau → kết quả không deterministic.

**Cải tiến đã làm (đúng hướng và đã verify):**
- Parallel ở mức top-level items (không phá cấu trúc FP-growth sâu).
- Mỗi thread giữ output riêng, **merge cuối**.
- **Cap số luật / lớp sau khi sort** (post-sort cap), không cap trong loop mining.
- Comparator / tie-breaker đầy đủ để sort **deterministic**.

**Bằng chứng:** `results/BAO-CAO-CAI-TIEN-PHASE08.md` ghi rõ “2 runs identical”.

---

## 3. Những thứ “đã thử nhưng không giữ lại”

Theo Phase 08:
- Có thử **confidence-based pruning trong mining** nhưng overhead clone+AND > lợi ích → **revert** để giữ performance ổn định.

Điểm này quan trọng khi trình bày: *không phải tối ưu nào cũng có ích; có thực nghiệm và quyết định kỹ thuật có lý do*.

---

## 4. Cách tự chạy để kiểm chứng (không cần sửa dataset)

### 4.1. Compile & chạy (cách chung)

```bash
# Compile
javac -d out -sourcepath src src/cmar/*.java src/cmar/benchmark/*.java src/cmar/util/*.java

# Baseline
java -cp out cmar.benchmark.BenchmarkRunner --mode=baseline

# Improved
java -cp out cmar.benchmark.BenchmarkRunner --mode=improved
```

### 4.2. Chạy script benchmark (Windows)
- Script: `run-benchmark.ps1`
- Kết quả số liệu tổng hợp thường nằm ở:
  - `results/profiling-metrics.csv`
  - `results/profiling-metrics.md`
  - `results/summary-report.md`

---

## 5. File tham chiếu (để trích vào báo cáo/nộp)

- Báo cáo Phase 08 (có bảng speedup + determinism): `results/BAO-CAO-CAI-TIEN-PHASE08.md`
- Tổng hợp “cách cải tiến + kết quả” (Phase 09–17): `results/CACH-CAI-TIEN-VA-KET-QUA.md`
- Báo cáo đồ án (dài, có lý thuyết + số liệu): `results/BAO-CAO-CHI-TIET-CAI-TIEN-CMAR.md`
- Code review (điểm mạnh/yếu, việc cần dọn trước nộp): `results/CODE-REVIEW-CAI-TIEN.md`

---

## 6. Kết luận (trả lời đúng câu hỏi “đã cải tiến được gì”)

Bạn đã cải tiến được:
- **Tốc độ train tăng ~5×** trên 26 datasets chuẩn (và có dataset tăng mạnh hơn nhiều).
- **Không đánh đổi accuracy** (thậm chí tăng nhẹ ở nhiều dataset).
- **Gỡ được nút thắt pruning bị skip** khi số luật lớn, giúp kết quả ổn định hơn.
- **Có hạ tầng đo + báo cáo + chạy lại được**, giúp chứng minh kết quả trung thực và tái lập.

