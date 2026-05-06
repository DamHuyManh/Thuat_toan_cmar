# Báo cáo chi tiết các cải tiến CMAR (Java)

**Dự án:** `Thuat_toan_cmar`  
**Mục tiêu chính:** tối ưu **hiệu năng** nhưng vẫn giữ **độ chính xác** ổn định trên 26 bộ UCI (theo paper CMAR 2001).  
**Cập nhật theo benchmark gần nhất:** **2026-05-06** (xem `results/summary-report.md`, `results/profiling-metrics.csv`).  

---

## 1. Tóm tắt kết quả (benchmark 26 bộ UCI)

- **Độ chính xác trung bình (Our CMAR):** **85,4%**  
- **So với Paper CMAR:** **+0,2 điểm %** (Paper 85,2%)  
- **Tổng thời gian huấn luyện (sum `trainMs` 26 bộ):** **4.468 ms**  
- **So baseline đo trước (23.302 ms):** **~5,21× nhanh hơn**  

Ghi chú:
- `trainMs` là trung bình theo fold (ms); tổng là tổng của 26 bộ trong `profiling-metrics.csv`.
- Một số bộ có `Train=0 ms` là do làm tròn (< 1 ms).

### 1.1. Kết quả đã chạy (chi tiết 26 bộ) — 2026-05-06

Nguồn dữ liệu:
- **Độ chính xác**: `results/summary-report.md` (mục *Accuracy Comparison*)
- **Thời gian / số luật**: `results/summary-report.md` (mục *Performance Metrics*) và `results/profiling-metrics.csv`

#### 1.1.1. Accuracy Comparison (Our CMAR vs Paper)

| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |
|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|
| **Anneal** | 898 | 38 | 6 | **98.3%** | 97.3% | 97.9% | 94.8% | **+1.0%** |
| **Australian** | 690 | 14 | 2 | **86.8%** | 86.1% | 84.9% | 84.7% | **+0.7%** |
| **Auto** | 205 | 25 | 6 | **81.4%** | 78.1% | 78.3% | 80.1% | **+3.3%** |
| **Breast-Cancer** | 683 | 9 | 2 | **97.1%** | 96.4% | 96.3% | 95.0% | **+0.7%** |
| **Cleve** | 303 | 13 | 2 | **82.6%** | 82.2% | 82.8% | 78.2% | **+0.4%** |
| **Crx** | 690 | 15 | 2 | **86.1%** | 84.9% | 84.7% | 84.9% | **+1.2%** |
| Diabetes | 768 | 8 | 2 | **73.4%** | 75.8% | 74.5% | 74.2% | -2.4% |
| German | 1000 | 20 | 2 | **72.9%** | 74.9% | 73.4% | 72.3% | -2.0% |
| Glass | 214 | 9 | 6 | **70.0%** | 70.1% | 73.9% | 68.7% | -0.1% |
| Heart | 270 | 13 | 2 | **80.7%** | 82.2% | 81.9% | 80.8% | -1.5% |
| **Hepatitis** | 155 | 19 | 2 | **83.3%** | 80.5% | 81.8% | 80.6% | **+2.8%** |
| Horse | 368 | 22 | 2 | **82.6%** | 82.6% | 82.1% | 82.6% | +0.0% |
| Hypo | 3163 | 25 | 2 | **98.1%** | 98.4% | 98.9% | 99.2% | -0.3% |
| **Iono** | 351 | 34 | 2 | **92.6%** | 91.5% | 92.3% | 90.0% | **+1.1%** |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| **Labor** | 57 | 16 | 2 | **91.7%** | 89.7% | 86.3% | 79.3% | **+2.0%** |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| **Lymphography** | 148 | 18 | 4 | **83.4%** | 83.1% | 77.8% | 73.5% | **+0.3%** |
| Pima | 768 | 8 | 2 | **73.4%** | 75.1% | 72.9% | 75.5% | -1.7% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| **Sonar** | 208 | 60 | 2 | **80.8%** | 79.4% | 77.5% | 70.2% | **+1.4%** |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.2%** | 99.2% | 99.6% | 99.4% | -0.0% |
| Vehicle | 846 | 18 | 4 | **68.2%** | 68.8% | 68.7% | 72.6% | -0.6% |
| Waveform | 5000 | 21 | 3 | **81.6%** | 83.2% | 80.0% | 78.1% | -1.6% |
| **Wine** | 178 | 13 | 3 | **96.7%** | 95.0% | 95.0% | 92.7% | **+1.7%** |
| Zoo | 101 | 16 | 7 | **96.5%** | 97.1% | 96.8% | 92.2% | -0.6% |
| **Average** | | | | **85.4%** | 85.2% | 84.7% | 83.3% | **+0.2%** |

#### 1.1.2. Performance Metrics (Train/Predict + số luật)

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 518 ms | 0 ms | 168583 | 159 | 99.9% |
| Australian | 26 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 508 ms | 0 ms | 209009 | 208 | 99.9% |
| Breast-Cancer | 3 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 12 ms | 0 ms | 16274 | 276 | 98.3% |
| Crx | 33 ms | 0 ms | 29313 | 557 | 98.1% |
| Diabetes | 1 ms | 0 ms | 1585 | 213 | 86.6% |
| German | 164 ms | 0 ms | 86583 | 950 | 98.9% |
| Glass | 2 ms | 0 ms | 2021 | 121 | 94.0% |
| Heart | 9 ms | 0 ms | 13748 | 249 | 98.2% |
| Hepatitis | 75 ms | 0 ms | 34973 | 122 | 99.7% |
| Horse | 212 ms | 0 ms | 123610 | 396 | 99.7% |
| Hypo | 301 ms | 0 ms | 85669 | 176 | 99.8% |
| Iono | 423 ms | 0 ms | 128964 | 196 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 30 | 66.7% |
| Labor | 18 ms | 0 ms | 22992 | 49 | 99.8% |
| Led7 | 2 ms | 0 ms | 249 | 111 | 55.4% |
| Lymphography | 81 ms | 0 ms | 59900 | 149 | 99.8% |
| Pima | 1 ms | 0 ms | 1585 | 213 | 86.6% |
| Sick | 239 ms | 0 ms | 85787 | 289 | 99.7% |
| Sonar | 1345 ms | 0 ms | 160000 | 171 | 99.9% |
| Tic-Tac-Toe | 6 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 77 ms | 0 ms | 35744 | 483 | 98.6% |
| Waveform | 373 ms | 6 ms | 75473 | 2650 | 96.5% |
| Wine | 21 ms | 0 ms | 16933 | 54 | 99.7% |
| Zoo | 18 ms | 0 ms | 13758 | 35 | 99.7% |

---

## 2. Triết lý cải tiến

Các cải tiến được thiết kế theo 2 nguyên tắc:

1. **Giảm số lần “đếm lại” và “duyệt lại”**  
   - Nếu một thông tin có thể tính 1 lần và tái dùng (support, match set, histogram lớp…), tránh quét dữ liệu nhiều lần.

2. **Dùng biểu diễn bit để xử lý theo lô**  
   - Thay vì kiểm tra từng transaction/instance, chuyển sang BitSet/bitmap để giao & đếm nhanh hơn.

---

## 3. Danh sách cải tiến theo Phase (từ đo lường → mining/prune → dự đoán)

> Phần dưới mô tả “đã làm gì” theo từng nhóm thay đổi. Tên Phase là tên bạn dùng trong các báo cáo/ghi chú nội bộ.

### 3.1. Nền tảng đo lường và báo cáo

- **Bổ sung hạ tầng đo thời gian & bộ nhớ theo phase**
  - Ghi ra `results/profiling-metrics.csv` và `results/profiling-metrics.md`.
  - Mục tiêu: biết chính xác thời gian nằm ở mining/pruning/index/predict để tối ưu đúng chỗ.

- **Chuẩn hóa báo cáo tổng hợp**
  - `results/summary-report.md` có: bảng accuracy (Our vs Paper), hiệu năng, tham số, thống kê thắng/hòa/thua so paper.
  - Sửa encoding tiếng Việt và đảm bảo ghi **UTF-8** (để không còn ký tự `�`).

---

### 3.2. Tối ưu khai phá luật (Mining / FP-Growth)

- **Giảm cấp phát & clone trong FP-tree/conditional tree**
  - Tập trung vào đoạn nóng: tạo conditional tree, duyệt single-path.
  - Tối ưu “ít tạo List/mảng tạm” và tái sử dụng buffer/BitSet khi có thể.

- **Tối ưu `emitRules` theo lớp có dữ liệu**
  - Thay vì quét hết mask của mọi lớp mỗi lần emit, chỉ xét những lớp xuất hiện trong histogram (dirty set).
  - Mục tiêu: giảm chi phí khi số lớp lớn hoặc khi conditional pattern base thưa.

- **Single-path FP-tree**
  - Khi FP-tree chỉ còn 1 nhánh, đi theo đường nhanh (giảm overhead khai phá).

Tác động thực tế:
- Trên bộ “nặng” (Waveform, Sonar, Anneal…), phần mining thường chiếm tỷ trọng lớn của `trainMs`. Các cải tiến ở đây giúp giảm tổng train rõ nhất.

---

### 3.3. Tối ưu cắt tỉa luật (Pruning: chi-square / G2S / coverage)

- **Giảm thao tác `clone().and().cardinality()` lặp lại**
  - Một số nhánh tối ưu chuyển sang đếm giao nhanh (ít clone), tận dụng BitSet/bitmap scratch theo thread.

- **Tách rõ các pha prune và tái dùng dữ liệu trung gian**
  - Mục tiêu: cùng một luật không nên quét lại dữ liệu nhiều lần cho các test khác nhau (chi-square, coverage…).

Tác động thực tế:
- Pruning là phần còn lại lớn thứ hai sau mining trong `trainMs` (tùy dataset).

---

### 3.4. Tối ưu lưu trữ & khớp luật (CR-Tree, bitmap antecedent)

- **Tiền tính bitmap tiền đề (antecedent) cho mỗi luật**
  - Mỗi luật sau prune được gọi `ensureAntBitmap(maxItem)` để có bitmap cố định theo vũ trụ item của training.
  - Mục tiêu: khi dự đoán chỉ cần AND/so sánh bitmap, không phải thao tác trên cấu trúc danh sách/Set.

- **Khớp luật theo “từ `long`”**
  - Bitmap dạng `long[]` giúp so sánh tiền đề nhanh hơn so với BitSet chung.

- **ThreadLocal scratch bitmap khi predict**
  - Giảm allocation churn trong dự đoán (đặc biệt khi chạy theo fold).

Tác động thực tế:
- `predictMs` trong báo cáo thường rất nhỏ (vì test set không quá lớn), nhưng tối ưu này giúp ổn định và tránh GC.

---

### 3.5. Tối ưu bỏ phiếu phân lớp (Weighted voting)

- **Giữ logic paper-faithful mặc định**
  - Mặc định dùng toàn bộ luật khớp (`topKGlobal=0`) để giống paper.

- **Hỗ trợ giới hạn top-K toàn cục (tuỳ chọn)**
  - Có cờ `--topK=<K>` để chỉ dùng K luật tốt nhất (theo order CMAR) khi cộng điểm theo lớp.
  - Mục tiêu: thử trade-off “tốc độ / nhiễu luật” trong một số dataset.

---


