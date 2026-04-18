# Phase 2: Data Quality & Preprocessing

## Mục tiêu
Cải thiện xử lý missing values và discretization để khớp với paper CMAR.

---

## Task 2.1: Phân tích cách paper xử lý Missing Values

**Hiện trạng**: `DataLoader.java` dòng 24 bỏ toàn bộ dòng có missing value:
```java
if (missingMarker != null && line.contains(missingMarker)) continue; // skip missing
```

**Vấn đề**: Cách này quá mạnh với dataset nhiều missing:
- Anneal: 24.7 missing/dòng → mất gần hết data
- Horse: 5.2 missing/dòng → mất nhiều dòng
- Sick: 4,556 missing values
- Hypo: 5,329 missing values

**Nghiên cứu**: Paper CMAR dùng "same method as CBA". CBA paper (Liu et al. 1998) xử lý missing values bằng cách:
- Categorical: thay bằng mode (giá trị xuất hiện nhiều nhất)
- Numeric: thay bằng mean hoặc median

**Giải pháp**: Sửa `DataLoader.java` thêm option imputation thay vì skip:
1. Thêm method `imputeMissing(rows, isNumeric)`:
   - Numeric: thay "?" bằng median của cột đó
   - Categorical: thay "?" bằng mode của cột đó
2. Giữ option `skipMissing` cho Breast-Cancer (đã loại dòng missing ở Phase 1)
3. Dùng imputation cho: Anneal, Horse, Sick, Hypo, Hepatitis, Crx

## Task 2.2: Kiểm tra MDL Discretization

**Hiện trạng**: `MDLDiscretizer.java` đã implement MDL method.
**Cần kiểm tra**:
1. So sánh output discretization với CBA reference implementation
2. Đảm bảo các bước:
   - Tìm best split point (minimize entropy)
   - Kiểm tra MDL criterion: Gain > log2(N-1)/N + delta(S,A)/N
   - Recursive split cho đến khi không thỏa MDL
3. Kiểm tra số bin tạo ra có hợp lý (thường 3-10 bins/attr)

**Cách kiểm tra**:
- Chạy MDLDiscretizer trên Iris (4 numeric attrs, kết quả dễ verify)
- In số bin cho mỗi attr và so sánh với CBA papers

## Task 2.3: Fix Waveform Dataset

**Vấn đề**: Accuracy thấp -4.0% so với paper. Waveform là randomly generated dataset.
**Giải pháp**:
1. Kiểm tra `datasets/originals/waveform.data`:
   - Phải có 5000 dòng, 21 attrs, 3 classes
   - Kiểm tra distribution của 3 classes (mỗi class ~1/3)
2. Nếu file hiện tại không đủ 5000 dòng:
   - Download từ UCI: https://archive.ics.uci.edu/dataset/107/waveform+database+generator+version+1
3. Gap -4.0% có thể do:
   - Discretization không tốt (21 continuous attrs)
   - Paper có thể dùng version khác của waveform generator
   - Chấp nhận sai lệch này nếu data đúng

## Task 2.4: Fix Sick & Hypo (Thyroid datasets)

**Vấn đề**: Sick -2.4%, Hypo -1.8%. Cả 2 có nhiều missing values.
**Giải pháp**:
1. Áp dụng imputation từ Task 2.1
2. Kiểm tra số dòng sau imputation:
   - Sick: 2800 (không bỏ dòng nào)
   - Hypo: 3163 (không bỏ dòng nào)
3. Kiểm tra các cột: nhiều cột là binary (T/F) với missing → impute bằng mode

## Task 2.5: Xử lý các Dataset Accuracy Gap trung bình

**Diabetes (-2.0%), German (-1.8%), Iono (-2.1%)**:
1. Kiểm tra data đúng (đã fix format ở Phase 1)
2. Kiểm tra discretization output
3. Gap nhỏ có thể do cross-validation randomness → chấp nhận

---

## Thay đổi code cần thiết

### `DataLoader.java`:
```
+ public static int[][][] parseCSV(csv, numBins, missingMarker, boolean impute)
+ private static void imputeMissing(List<String[]> rows, boolean[] isNumeric)
  - Numeric: median
  - Categorical: mode
```

### `UCIDatasets.java`:
- Cập nhật các hàm loadXxx() để truyền `impute=true` cho dataset có nhiều missing

## Output của Phase 2
- DataLoader hỗ trợ cả 2 mode: skip missing và impute missing
- 6 dataset có nhiều missing dùng imputation
- Discretization đã kiểm tra khớp MDL/CBA
- Waveform data đã verify
