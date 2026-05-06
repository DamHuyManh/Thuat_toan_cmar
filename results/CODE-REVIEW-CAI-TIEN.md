# Code Review — CMAR Java Implementation (Phase 02–08)

**Ngày:** 2026-04-27  
**Người review:** Code Review Agent  
**Phạm vi:** Phase 02–08 optimization work (mining, pruning, bitmap, parallel)

---

## Tổng quan

| File | Dòng | Vai trò |
|---|---|---|
| `CMARClassifier.java` | 218 | Pipeline chính |
| `FPGrowth.java` | 233 | Baseline mining |
| `FPGrowthOptimized.java` | 284 | Class-aware mining (Phase 06+08) |
| `RulePruner.java` | 538 | Chi²/G2S/Coverage pruning |
| `CRTree.java` | 103 | Hash-indexed rule storage |
| `Rule.java` | 104 | Data class |
| `PhaseTimer.java` | 47 | Đo thời gian |
| `OptimizationProfile.java` | 16 | Switch baseline/improved |
| `MemorySampler.java` | 50 | Đo memory |
| `BenchmarkRunner.java` | 572 | Harness benchmark |

---

## Đánh giá tổng thể

### Điểm mạnh

1. **Thuật toán đúng với paper.** CMAR ordering (conf desc → sup desc → len asc) khớp Section 2; chi² 2×2 contingency table đúng công thức; weighted voting đúng Section 4.
2. **Bitmap hot-path nhất quán.** `Rule.matchesBitmap()`, `isSubsetBitmap()`, inverted index AND — tất cả dùng cùng encoding `item>>6 / item&63`.
3. **Determinism sau parallel mining.** Phase 08 đúng khi bỏ in-loop cap, sort toàn bộ rồi cap post-sort — đây là quyết định thiết kế tốt, có comment rõ ràng.
4. **ThreadLocal cho PhaseTimer.** Tránh race condition trong parallel benchmark, không cần sync.
5. **`Rule.compareTo` fully deterministic.** Tie-breaker lexicographic + classLabel — không để `equals` vi phạm Comparable contract.
6. **`coveragePrune` (optimized)** dùng `BitSet notFullyCovered` thay `boolean[]` — cho phép `isEmpty()` early-exit tốt.

### Điểm yếu chính

1. **Dead code đáng kể:** `chiSquarePruneLazy` (`RulePruner.java:483`), `allClassesFull` luôn trả `false` (`FPGrowthOptimized.java:245`), `ruleCount` param trong `emitRules` không dùng.
2. **Baseline path trong `FPGrowth.java` bị lẫn với optimized logic** (line 44 check `isBaseline()` bên trong FPGrowth — không nhất quán với việc CMARClassifier đã dispatch đúng class).
3. **ThreadLocal leak tiềm tàng** trong benchmark multi-fold.
4. **`OptimizationProfile` static mutable** — không an toàn trong parallel test nếu có.
5. **Nhiều magic number** không được đặt tên constant.

---

## Vấn đề Critical

**Không có vấn đề critical** (security, data loss, crash) trong phạm vi academic thesis.

---

## Vấn đề Major

### M1 — `allClassesFull()` là no-op, nhưng vẫn được gọi nhiều lần trong hot loop

**File:** `FPGrowthOptimized.java:245`, gọi tại dòng 190, 258  
```java
private boolean allClassesFull(Map<Integer, AtomicInteger> ruleCount) {
    return false;  // No-op now (cap moved to post-sort)
}
```
Hàm này gọi bên trong `mineRecursive` (line 190) — tức là mọi node đệ quy đều gọi hàm trả `false`. Không ảnh hưởng đúng/sai nhưng lãng phí call overhead và gây nhầm lẫn. Nên xóa cả hàm lẫn mọi lời gọi.

---

### M2 — `chiSquarePruneLazy` là dead code nhưng vẫn tồn tại

**File:** `RulePruner.java:483–524`  
Có annotation `@SuppressWarnings("unused")` nhưng vẫn để lại ~42 dòng code không được dùng trong bất kỳ path nào. Với thesis phải trình giáo viên, code chết làm loãng và gây câu hỏi không cần thiết.

---

### M3 — `FPGrowth.java` kiểm tra `OptimizationProfile.isBaseline()` nội bộ (line 44)

**File:** `FPGrowth.java:44`  
```java
if (cmar.util.OptimizationProfile.isBaseline()) {
    return generateRulesBaseline(frequentItemsets, transactions, labels, N);
}
```
`FPGrowth` là baseline class — khi `isImproved()`, `CMARClassifier` đã dispatch tới `FPGrowthOptimized`. Vậy code path `!isBaseline()` bên trong `FPGrowth` không bao giờ được chạy khi benchmark ở mode IMPROVED. Đây là logic trùng lặp và gây nhầm: nếu ai đó gọi `FPGrowth` trực tiếp trong mode IMPROVED, họ sẽ nhận code từ `FPGrowth.java:48–103` chứ không phải `FPGrowthOptimized`.

---

### M4 — ThreadLocal trong PhaseTimer không được reset giữa các fold

**File:** `BenchmarkRunner.java:234`, `PhaseTimer.java`  
`PhaseTimer.reset()` được gọi đúng trước mỗi fold (line 234). Tuy nhiên `PhaseTimer` dùng `ThreadLocal` — nếu fold chạy trên thread khác (ví dụ future work dùng parallel fold), timer sẽ đọc giá trị của thread đó, không phải main thread. Hiện tại không gây bug vì benchmark chạy single-threaded folds, nhưng là bẫy tiềm tàng.

---

### M5 — `coveragePruneOld` (baseline) logic khác `coveragePrune` (optimized) — ảnh hưởng tới so sánh công bằng

**File:** `RulePruner.java:389–416` vs `196–231`  
`coveragePruneOld` kiểm tra `labels[i] == rule.classLabel` trong vòng "useful" check (line 399), nhưng `coveragePrune` (optimized) kiểm tra điều kiện tương tự qua `usefulSet.and(cmask)` (line 215). Về logic là tương đương, nhưng:
- `coveragePruneOld` scan bitmap lần 2 để tăng `coverCount` cho **mọi** transaction match rule (bất kể class, line 406), không chỉ class-matching.
- `coveragePrune` (optimized) cũng tăng `coverCount` cho mọi match (line 222), vậy là tương đương.

Hai path thực chất tương đương về logic, nhưng khó verify nếu không đọc kỹ. Nên thêm comment xác nhận sự tương đương.

---

## Vấn đề Minor / Code smell

### m1 — Magic numbers không có tên

**File:** `FPGrowthOptimized.java:24,31`, `FPGrowth.java:15,17`, `BenchmarkRunner.java:17`

```java
private static final long MAX_MINING_MS = 600000;  // OK, có tên
private static final int PARALLEL_MIN_TX = 200;    // OK
private int n = Math.min(pathItems.size(), Math.min(15, ...));  // 15 = ?
```
Số `15` tại `FPGrowthOptimized.java:253` và `FPGrowth.java:211` không có tên constant — đây là giới hạn subset enumeration cho single-path (2^15 = 32768 subsets). Nên đặt `MAX_SINGLE_PATH_ITEMS = 15`.

---

### m2 — `generalToSpecificPruneOld` có hardcode `> 10000` skip

**File:** `RulePruner.java:365`  
```java
if (rules.size() > 10000) return rules;
```
Baseline G2S bỏ qua pruning khi có hơn 10000 rules — làm cho accuracy baseline có thể cao hơn thực tế nếu nhiều rules. Nên document rõ đây là trade-off performance của baseline, không phải feature.

---

### m3 — `Rule` fields là package-private (không có `private`)

**File:** `Rule.java:11–19`  
```java
int[] antecedent;
int classLabel;
double chiSquare;
long[] antBitmap;
```
Tất cả fields là package-private. Trong cùng package `cmar` thì truy cập thoải mái — OK cho academic code, nhưng `antBitmap` là implementation detail của RulePruner và nên được encapsulate hơn.

---

### m4 — `CRTree.build()` không check `antecedent.length == 0`

**File:** `CRTree.java:33`  
```java
.computeIfAbsent(rule.antecedent[0], k -> new ArrayList<>())
```
Nếu `rule.antecedent` là mảng rỗng (edge case: empty itemset lọt qua), sẽ ném `ArrayIndexOutOfBoundsException`. Trong flow hiện tại không xảy ra vì FPGrowth skip `itemset.length == 0`, nhưng không có guard tại `CRTree.build()`.

---

### m5 — `OptimizationProfile` là static mutable global state

**File:** `OptimizationProfile.java:10`  
```java
private static volatile Mode mode = Mode.IMPROVED;
```
Dùng `volatile` đúng cho visibility, nhưng khi chạy nhiều benchmark trong cùng JVM (ví dụ unit test), việc set global state có thể gây hiện tượng test interference. Không ảnh hưởng khi chạy từng JVM riêng qua `--mode` flag.

---

### m6 — `MemorySampler.start()` gọi `System.gc()` + `Thread.sleep(50)`

**File:** `MemorySampler.java:23–24`  
`System.gc()` không đảm bảo GC chạy ngay, và sleep 50ms làm chậm benchmark mỗi fold. Trong 10-fold × N datasets = 10×N lần sleep. Có thể thay bằng `Runtime.getRuntime().gc()` + `MemoryMXBean` snapshot không sleep.

---

### m7 — `BenchmarkRunner.writeProfilingReport` hardcode date "2026-04-23"

**File:** `BenchmarkRunner.java:82`  
```java
sb.append("**Date:** 2026-04-23\n");
```
Nên dùng `LocalDate.now()` để date luôn đúng.

---

### m8 — VN/EN comment không nhất quán

Một số file comment tiếng Việt (`RulePruner.java`: "Tránh cloning", "Backward-compatible"), một số tiếng Anh. Không phải lỗi nghiêm trọng nhưng nên thống nhất — với thesis tiếng Việt, nên comment bằng tiếng Việt.

---

## Đề xuất cải tiến

### Ưu tiên cao (trước khi nộp)

1. **Xóa `chiSquarePruneLazy`** (`RulePruner.java:483–524`) — dead code hoàn toàn.
2. **Xóa `allClassesFull()` + mọi lời gọi** trong `FPGrowthOptimized` — hàm no-op gây nhầm.
3. **Thêm guard tại `CRTree.build()`:**
   ```java
   if (rule.antecedent == null || rule.antecedent.length == 0) continue;
   ```
4. **Thêm constant `MAX_SINGLE_PATH_ITEMS = 15`** tại cả `FPGrowth` và `FPGrowthOptimized`.
5. **Sửa hardcode date** trong `writeProfilingReport`:
   ```java
   sb.append("**Date:** ").append(java.time.LocalDate.now()).append("\n");
   ```

### Ưu tiên trung bình

6. **Document rõ tại `generalToSpecificPruneOld`** rằng `> 10000` skip là intentional performance trade-off.
7. **Thêm comment tại `coveragePruneOld` và `coveragePrune`** xác nhận logic tương đương để người đọc thesis dễ so sánh.
8. **Làm rõ logic dispatch trong `FPGrowth.java:44`:** Hoặc xóa branch `isBaseline()` (vì CMARClassifier đã dispatch đúng), hoặc comment rõ "Fallback nếu gọi trực tiếp".

### Ưu tiên thấp

9. Thống nhất VN/EN trong comment (ưu tiên VN cho thesis).
10. Xem xét tạo interface `RuleMiner` cho `FPGrowth` và `FPGrowthOptimized` — giúp giáo viên thấy OOP design pattern rõ ràng hơn.

---

## Kết luận

Code đạt chất lượng tốt cho thesis đại học. Thuật toán CMAR được implement đúng với paper gốc. Các optimization từ Phase 02–08 được phân tách rõ ràng, có thể benchmark độc lập, và determinism được đảm bảo qua post-sort cap (Phase 08).

**Vấn đề cần xử lý trước nộp:** M2 (dead code `chiSquarePruneLazy`), M1 (no-op `allClassesFull`), và m4 (guard tại `CRTree.build`). Các vấn đề còn lại là code smell, không ảnh hưởng kết quả.

**Không có bug nào ảnh hưởng độ chính xác** được phát hiện trong review này.
