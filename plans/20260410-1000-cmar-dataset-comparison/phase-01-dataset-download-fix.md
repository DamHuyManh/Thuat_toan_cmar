# Phase 1: Download & Fix Dataset

## Mục tiêu
Fix tất cả dataset bị thiếu dòng, sai format, hoặc dư cột để khớp với paper CMAR.

---

## Task 1.1: Merge Anneal (CRITICAL)
**Vấn đề**: Local 798 dòng, paper 898 dòng. Thiếu 100 dòng từ file test.
**Giải pháp**:
1. Đọc `datasets/originals/anneal.data` (798 dòng) và `datasets/originals/anneal.test` (100 dòng)
2. Kiểm tra cả 2 file có cùng format (số cột, delimiter)
3. Nối 2 file thành 1, tổng 898 dòng
4. Xác nhận: 38 attrs + 1 class = 39 cột
5. Lưu ra `datasets/anneal.csv`
6. Cập nhật `UCIDatasets.java` nếu cần (đã có PAPER_INSTANCE_COUNTS = 898)

**Kiểm tra**:
- `wc -l datasets/anneal.csv` = 898 (không tính header)
- Số cột nhất quán giữa train và test

## Task 1.2: Merge Horse-Colic (CRITICAL)
**Vấn đề**: Local 300 dòng, paper 368 dòng. File test có 68 dòng.
**Giải pháp**:
1. Đọc `datasets/originals/horse-colic.data` (300) và `datasets/originals/horse-colic.test` (68)
2. Horse-colic là space-delimited → chuyển sang CSV
3. Nối 2 file, tổng 368 dòng
4. Xác nhận: 22 attrs + 1 class (cột 24 là class "surgical lesion")
5. Lưu ra `datasets/horse-colic.csv`

**Lưu ý**: Horse-colic có nhiều missing values (ký hiệu "?"). Giữ nguyên, xử lý ở Phase 2.

## Task 1.3: Fix Breast-Cancer
**Vấn đề**: Raw 699 dòng, paper dùng 683 (bỏ 16 dòng có missing values).
**Giải pháp**:
1. Đọc `datasets/breast-cancer-wisconsin.csv`
2. Xác nhận cột nào có missing ("?" hoặc "MISS")
3. Loại bỏ dòng có missing value → 683 dòng
4. Bỏ cột ID (cột 1) → còn 10 cols (9 attrs + 1 class)
5. Lưu lại

**Kiểm tra**: `wc -l` = 683

## Task 1.4: Fix Zoo
**Vấn đề**: Local 18 cột, paper 16 attrs + 1 class = 17 cột.
**Giải pháp**:
1. Đọc `datasets/originals/zoo.data`
2. Cột 1 là "animal name" (string) → bỏ cột này
3. Kết quả: 16 attrs + 1 class = 17 cột, 101 dòng
4. Lưu ra `datasets/zoo.csv`

**Kiểm tra**: Xác nhận zoo.csv có 17 cột

## Task 1.5: Fix Diabetes/Pima off-by-one
**Vấn đề**: Local 767 dòng, paper 768.
**Giải pháp**:
1. Đọc `datasets/originals/pima-indians-diabetes.data`
2. Đếm dòng → nếu 768, copy trực tiếp sang CSV
3. Nếu 767, kiểm tra có header row bị bỏ không
4. Xác nhận `datasets/diabetes.csv` và `datasets/pima-indians-diabetes.csv` đều 768 dòng

**Lưu ý**: Diabetes và Pima là CÙNG 1 dataset. Paper list riêng nhưng dùng chung nguồn UCI. Giữ cả 2 file.

## Task 1.6: Fix Format Issues
**Vấn đề**: Nhiều file space-delimited nhưng lưu .csv
**Các file cần fix**:
- `australian.csv` (space-delimited)
- `german.csv` (space-delimited)
- `heart.csv` (space-delimited)
- `vehicle-xaa.csv` (space-delimited, cần merge nhiều file vehicle-xa*)

**Giải pháp**:
1. Đọc từ `datasets/originals/` (file .dat hoặc .data)
2. Thay space bằng dấu phẩy
3. Loại bỏ cột ID nếu có
4. Lưu lại đúng format CSV

**Vehicle**: Merge tất cả `originals/vehicle-xa*.dat` thành 1 file 846 dòng

## Task 1.7: Verify Iris
**Vấn đề**: Local 151 dòng (có thể do header), paper 150.
**Giải pháp**: Kiểm tra và bỏ header nếu có. Xác nhận 150 dòng data.

---

## Output của Phase 1
- 26 file CSV trong `datasets/` với đúng số dòng theo paper
- Tất cả file đúng CSV format (comma-delimited)
- Không có cột ID/name thừa
- Cột cuối là class label
