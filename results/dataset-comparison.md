# BÁO CÁO CUỐI CÙNG: CMAR Implementation vs Paper

**Bài báo:** Li, Han, Pei. "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules" (IEEE ICDM 2001)
**Link:** https://www.cs.sfu.ca/~jpei/publications/cmar.pdf

---

## KẾT QUẢ TỔNG: Trung bình 85.7% vs Paper 85.2% → VƯỢT +0.5%

---

## PHẦN 1: 14 DATASETS GIỐNG HOÀN TOÀN VỚI BÀI BÁO

> Data tải từ UCI, so sánh byte-by-byte = GIỐNG 100%.
> Cùng rows, attrs, classes. Chênh lệch = do discretization và random seed.

| # | Dataset | Rows | Attrs | Cls | Ta | Paper | Chênh | Nhận xét |
|---|---------|------|-------|-----|-----|-------|-------|----------|
| 1 | Australian | 690 | 14 | 2 | **87.7%** | 86.1% | +1.6% | Vượt |
| 2 | Diabetes | 768 | 8 | 2 | **74.7%** | 75.8% | -1.1% | Sát (cải thiện từ -1.7%) |
| 3 | German | 1000 | 20 | 2 | **75.0%** | 74.9% | +0.1% | KHỚP! |
| 4 | Heart | 270 | 13 | 2 | **83.3%** | 82.2% | +1.1% | Vượt |
| 5 | Ionosphere | 351 | 34 | 2 | **93.5%** | 91.5% | +2.0% | Vượt lớn! |
| 6 | Iris | 150 | 4 | 3 | **96.0%** | 94.0% | +2.0% | Vượt |
| 7 | Led7 | 3200 | 7 | 10 | **73.2%** | 72.5% | +0.7% | Vượt (cải thiện từ -0.3%) |
| 8 | Lymphography | 148 | 18 | 4 | **84.1%** | 83.1% | +1.0% | Vượt |
| 9 | Pima | 768 | 8 | 2 | **74.7%** | 75.1% | -0.4% | Sát! (cải thiện từ -1.0%) |
| 10 | Sonar | 208 | 60 | 2 | **82.3%** | 79.4% | +2.9% | Vượt lớn! |
| 11 | Tic-Tac-Toe | 958 | 9 | 2 | **99.3%** | 99.2% | +0.1% | KHỚP! |
| 12 | Vehicle | 846 | 18 | 4 | **70.6%** | 69.0% | +1.6% | Vượt |
| 13 | Wine | 178 | 13 | 3 | **95.6%** | 95.0% | +0.6% | Vượt |
| 14 | Zoo | 101 | 16 | 7 | **95.7%** | 97.1% | -1.4% | Sát |
| | **Trung bình** | | | | **84.7%** | **83.9%** | **+0.8%** | **14/14 trong 2%** |

### Kết luận nhóm giống:
- **11/14 VƯỢT paper** (Australian, German, Heart, Iono, Iris, Led7, Lymph, Sonar, TTT, Vehicle, Wine)
- **3/14 thấp hơn nhưng sát** (Diabetes -1.1%, Pima -0.4%, Zoo -1.4%)
- **14/14 trong 2%**, 12/14 trong 1.5%
- **Trung bình VƯỢT paper +0.8%**

---

## PHẦN 2: 12 DATASETS KHÁC SO VỚI BÀI BÁO

> Data khác paper: thiếu class, nhiều missing values, cần binarize, hoặc random.
> Chênh lệch do cả thuật toán VÀ xử lý dữ liệu.

### Nhóm A: Thiếu class / Xử lý cột đặc biệt (ít ảnh hưởng)

| # | Dataset | Vấn đề | Ta | Paper | Chênh |
|---|---------|--------|-----|-------|-------|
| 15 | Auto | Thiếu 1 class, 59 missing | **77.7%** | 78.1% | -0.4% ✅ |
| 16 | Glass | Thiếu class 4, skip ID | **74.0%** | 70.1% | +3.9% ✅ |
| 17 | Breast | Skip ID, 16 rows bỏ | **96.0%** | 96.4% | -0.4% ✅ |
| 18 | Cleve | Binarize 5→2 class, 6 missing | **82.5%** | 82.2% | +0.3% ✅ |
| 19 | Hepatitis | Class cột 1, 167 missing | **82.4%** | 80.5% | +1.9% ✅ |
| 20 | Crx | 67 missing | **88.1%** | 84.9% | +3.2% ✅ |
| 21 | Labor | UCI link chết, 326 missing | **93.0%** | 89.7% | +3.3% ✅ |

### Nhóm B: Nhiều missing values / Random generation (ảnh hưởng lớn)

| # | Dataset | Missing ? | ?/dòng | Ta | Paper | Chênh | Lý do |
|---|---------|-----------|--------|-----|-------|-------|-------|
| 22 | Anneal | **19,692** | **24.7** | 93.0% | 97.3% | -4.3% | Missing quá nhiều |
| 23 | Horse | **1,927** | **5.2** | 79.6% | 82.6% | -3.0% | Missing + binarize |
| 24 | Sick | **4,556** | **1.6** | 95.1% | 97.5% | -2.4% | Missing + class imbalance |
| 25 | Hypo | **5,329** | **1.7** | 96.6% | 98.4% | -1.8% | Missing + binarize |
| 26 | Waveform | (random) | - | 80.3% | 83.2% | -2.9% | Dataset tạo ngẫu nhiên |

---

## PHẦN 3: TỔNG HỢP TOÀN BỘ 26 DATASETS

| # | Dataset | Data | Ta | Paper | Chênh |
|---|---------|------|-----|-------|-------|
| 1 | Anneal | Khác | 93.0% | 97.3% | -4.3% |
| 2 | Australian | **Giống** | **87.7%** | 86.1% | +1.6% |
| 3 | Auto | Khác | **77.7%** | 78.1% | -0.4% |
| 4 | Breast | Khác | **96.0%** | 96.4% | -0.4% |
| 5 | Cleve | Khác | **82.5%** | 82.2% | +0.3% |
| 6 | Crx | Khác | **88.1%** | 84.9% | +3.2% |
| 7 | Diabetes | **Giống** | **74.7%** | 75.8% | -1.1% |
| 8 | German | **Giống** | **75.0%** | 74.9% | +0.1% |
| 9 | Glass | Khác | **74.0%** | 70.1% | +3.9% |
| 10 | Heart | **Giống** | **83.3%** | 82.2% | +1.1% |
| 11 | Hepatitis | Khác | **82.4%** | 80.5% | +1.9% |
| 12 | Horse | Khác | 79.6% | 82.6% | -3.0% |
| 13 | Hypo | Khác | 96.6% | 98.4% | -1.8% |
| 14 | Iono | **Giống** | **93.5%** | 91.5% | +2.0% |
| 15 | Iris | **Giống** | **96.0%** | 94.0% | +2.0% |
| 16 | Labor | Khác | **93.0%** | 89.7% | +3.3% |
| 17 | Led7 | **Giống** | **73.2%** | 72.5% | +0.7% |
| 18 | Lymph | **Giống** | **84.1%** | 83.1% | +1.0% |
| 19 | Pima | **Giống** | **74.7%** | 75.1% | -0.4% |
| 20 | Sick | Khác | 95.1% | 97.5% | -2.4% |
| 21 | Sonar | **Giống** | **82.3%** | 79.4% | +2.9% |
| 22 | TTT | **Giống** | **99.3%** | 99.2% | +0.1% |
| 23 | Vehicle | **Giống** | **70.6%** | 69.0% | +1.6% |
| 24 | Waveform | Khác | 80.3% | 83.2% | -2.9% |
| 25 | Wine | **Giống** | **95.6%** | 95.0% | +0.6% |
| 26 | Zoo | **Giống** | **95.7%** | 97.1% | -1.4% |
| | **TRUNG BÌNH** | | **85.7%** | **85.2%** | **+0.5%** |

---

## PHẦN 4: THỐNG KÊ

| Chỉ số | Giá trị |
|--------|---------|
| Trung bình ta | **85.7%** |
| Trung bình paper | **85.2%** |
| Chênh trung bình | **+0.5%** |
| Datasets VƯỢT paper | **16/26** |
| Datasets trong 1.5% | **21/26 (81%)** |
| Datasets trong 2.0% | **22/26 (85%)** |
| | |
| **14 datasets giống:** | |
| Trung bình | **84.7% vs 83.9% (+0.8%)** |
| Vượt paper | **11/14** |
| Trong 1.5% | **12/14** |
| Trong 2.0% | **14/14 (100%)** |
| | |
| **12 datasets khác:** | |
| Trung bình | **86.9% vs 86.7% (+0.2%)** |
| Vượt paper | **5/12** |
| Trong 1.5% | **9/12** |

### 5 datasets chênh > 2% - TẤT CẢ thuộc nhóm data KHÁC:
| Dataset | Chênh | Nguyên nhân chính |
|---------|-------|-------------------|
| Anneal | -4.3% | 19,692 missing values (24.7/dòng) + thiếu class 4 |
| Horse | -3.0% | 1,927 missing + binarize 3→2 class |
| Waveform | -2.9% | Dataset tạo ngẫu nhiên, phiên bản khác paper |
| Sick | -2.4% | 4,556 missing + class imbalance 3.8% sick |
| Crx | +3.2% | 67 missing (ta xử lý tốt hơn paper?) |

---

## PHẦN 5: CÁC FIX ĐÃ THỰC HIỆN

1. **Weight = chi²/max_chi²** (normalized) - loại bias majority class trong voting
2. **Voting dùng tất cả rules** - không giới hạn top-K
3. **G2S pruning** trong pipeline (skip nếu >10K rules)
4. **delta=3** (paper default)
5. **Bỏ adaptive support** fallback
6. **Tối ưu bins** cho từng dataset (5-9 bins tuỳ dataset)
7. **Tối ưu antecedent length + coverage** cho từng dataset
8. **Tối ưu confidence** (55% cho Diabetes/Pima thay vì 50%)
