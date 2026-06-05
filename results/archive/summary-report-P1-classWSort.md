# CMAR — Báo cáo benchmark (tóm tắt)

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-15 |
| **Bài báo tham chiếu** | Li, Han, Pei — *CMAR* (IEEE ICDM 2001) |
| **Code** | Java — bitmap matching, CR-tree có hash, chi-square + coverage pruning |
| **Đánh giá** | 10-fold cross-validation |
| **Dự đoán** | Top-k toàn cục: chỉ lấy **10** luật khớp tốt nhất khi bỏ phiếu (khác bản paper đầy đủ) |

## Cách đọc báo cáo

### Bảng độ chính xác (Accuracy Comparison)

- **Our CMAR:** độ chính xác (%) do chương trình của bạn đo được.
- **Paper CMAR / Paper CBA / Paper C4.5:** số **ghi trong bài báo** để so sánh — *không* phải chạy lại CBA/C4.5 trên máy bạn.
- **Diff:** chênh lệch **Our CMAR − Paper CMAR** (%). Dương (+) = bạn cao hơn paper; âm (−) = thấp hơn.
- **Instances / Attrs / Classes:** số mẫu, số thuộc tính, số lớp của bộ dữ liệu.

### Bảng hiệu năng (Performance Metrics)

- **Train / Predict:** thời gian huấn luyện (mine + prune) và dự đoán, **trung bình theo fold** (ms). Giá trị **0 ms** thường là làm tròn (< 1 ms).
- **Rules mined:** số luật sinh ra **trước** bước cắt tỉa.
- **Rules after prune:** số luật **còn lại sau** prune (dùng để phân lớp). *(Tên cũ "Rules Pruned" dễ gây nhầm — đây là luật **giữ lại**, không phải số luật bị xóa.)*
- **% Removed:** phần trăm luật thô bị loại: `100 * (1 - after/mined)` (trong bảng, *after* là cột *Rules after prune*).

---

## Accuracy Comparison

| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |
|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|
| Anneal | 898 | 38 | 6 | **95.5%** | 97.3% | 97.9% | 94.8% | -1.8% |
| Australian | 690 | 14 | 2 | **83.2%** | 86.1% | 84.9% | 84.7% | -2.9% |
| Auto | 205 | 25 | 6 | **82.5%** | 78.1% | 78.3% | 80.1% | +4.4% |
| Breast-Cancer | 683 | 9 | 2 | **96.9%** | 96.4% | 96.3% | 95.0% | +0.5% |
| Cleve | 303 | 13 | 2 | **81.6%** | 82.2% | 82.8% | 78.2% | -0.6% |
| Crx | 690 | 15 | 2 | **83.9%** | 84.9% | 84.7% | 84.9% | -1.0% |
| Diabetes | 768 | 8 | 2 | **73.6%** | 75.8% | 74.5% | 74.2% | -2.2% |
| German | 1000 | 20 | 2 | **57.9%** | 74.9% | 73.4% | 72.3% | -17.0% |
| Glass | 214 | 9 | 6 | **68.4%** | 70.1% | 73.9% | 68.7% | -1.7% |
| Heart | 270 | 13 | 2 | **78.5%** | 82.2% | 81.9% | 80.8% | -3.7% |
| Hepatitis | 155 | 19 | 2 | **75.7%** | 80.5% | 81.8% | 80.6% | -4.8% |
| Horse | 368 | 22 | 2 | **76.6%** | 82.6% | 82.1% | 82.6% | -6.0% |
| Hypo | 3163 | 25 | 2 | **97.3%** | 98.4% | 98.9% | 99.2% | -1.1% |
| Iono | 351 | 34 | 2 | **90.6%** | 91.5% | 92.3% | 90.0% | -0.9% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **91.7%** | 89.7% | 86.3% | 79.3% | +2.0% |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| Lymphography | 148 | 18 | 4 | **78.9%** | 83.1% | 77.8% | 73.5% | -4.2% |
| Pima | 768 | 8 | 2 | **73.6%** | 75.1% | 72.9% | 75.5% | -1.5% |
| Sick | 2800 | 29 | 2 | **96.5%** | 97.5% | 97.0% | 98.5% | -1.0% |
| Sonar | 208 | 60 | 2 | **78.8%** | 79.4% | 77.5% | 70.2% | -0.6% |
| Tic-Tac-Toe | 958 | 9 | 2 | **92.9%** | 99.2% | 99.6% | 99.4% | -6.3% |
| Vehicle | 846 | 18 | 4 | **68.5%** | 68.8% | 68.7% | 72.6% | -0.3% |
| Waveform | 5000 | 21 | 3 | **81.5%** | 83.2% | 80.0% | 78.1% | -1.7% |
| Wine | 178 | 13 | 3 | **96.2%** | 95.0% | 95.0% | 92.7% | +1.2% |
| Zoo | 101 | 16 | 7 | **95.6%** | 97.1% | 96.8% | 92.2% | -1.5% |
| **Average** | | | | **83.1%** | 85.2% | 84.7% | 83.3% | -2.1% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 1262 ms | 0 ms | 156588 | 156 | 99.9% |
| Australian | 90 ms | 0 ms | 18745 | 439 | 97.7% |
| Auto | 956 ms | 0 ms | 209009 | 210 | 99.9% |
| Breast-Cancer | 10 ms | 0 ms | 2836 | 259 | 90.9% |
| Cleve | 34 ms | 0 ms | 16274 | 268 | 98.4% |
| Crx | 130 ms | 0 ms | 30762 | 533 | 98.3% |
| Diabetes | 7 ms | 0 ms | 1585 | 213 | 86.6% |
| German | 453 ms | 0 ms | 89483 | 817 | 99.1% |
| Glass | 10 ms | 0 ms | 2021 | 120 | 94.1% |
| Heart | 45 ms | 0 ms | 15134 | 241 | 98.4% |
| Hepatitis | 111 ms | 0 ms | 38172 | 118 | 99.7% |
| Horse | 690 ms | 0 ms | 129386 | 389 | 99.7% |
| Hypo | 968 ms | 7 ms | 86450 | 183 | 99.8% |
| Iono | 1599 ms | 1 ms | 129736 | 190 | 99.9% |
| Iris | 1 ms | 0 ms | 90 | 30 | 66.7% |
| Labor | 78 ms | 0 ms | 24003 | 49 | 99.8% |
| Led7 | 32 ms | 0 ms | 243 | 111 | 54.3% |
| Lymphography | 354 ms | 0 ms | 65800 | 149 | 99.8% |
| Pima | 10 ms | 0 ms | 1585 | 213 | 86.6% |
| Sick | 1122 ms | 4 ms | 85874 | 235 | 99.7% |
| Sonar | 4919 ms | 0 ms | 160000 | 167 | 99.9% |
| Tic-Tac-Toe | 54 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 367 ms | 0 ms | 36922 | 479 | 98.7% |
| Waveform | 1832 ms | 22 ms | 75473 | 2650 | 96.5% |
| Wine | 88 ms | 0 ms | 16933 | 54 | 99.7% |
| Zoo | 71 ms | 0 ms | 13758 | 35 | 99.7% |

## Parameters Used

*Tham số FP-Growth / CMAR cho từng bộ (min support dạng tỷ lệ và số giao dịch tối thiểu).*

| Dataset | Min Support (ratio) | Min Support (abs) | Min Confidence |
|---------|--------------------|--------------------|----------------|
| Anneal | 0.01 | 8 | 0.50 |
| Australian | 0.01 | 6 | 0.50 |
| Auto | 0.01 | 2 | 0.50 |
| Breast-Cancer | 0.01 | 6 | 0.50 |
| Cleve | 0.01 | 2 | 0.50 |
| Crx | 0.01 | 6 | 0.50 |
| Diabetes | 0.01 | 5 | 0.50 |
| German | 0.01 | 9 | 0.50 |
| Glass | 0.01 | 2 | 0.50 |
| Heart | 0.01 | 2 | 0.50 |
| Hepatitis | 0.01 | 2 | 0.50 |
| Horse | 0.01 | 3 | 0.50 |
| Hypo | 0.01 | 28 | 0.50 |
| Iono | 0.03 | 10 | 0.50 |
| Iris | 0.01 | 2 | 0.50 |
| Labor | 0.01 | 2 | 0.50 |
| Led7 | 0.01 | 28 | 0.50 |
| Lymphography | 0.01 | 2 | 0.50 |
| Pima | 0.01 | 5 | 0.50 |
| Sick | 0.01 | 12 | 0.50 |
| Sonar | 0.08 | 14 | 0.50 |
| Tic-Tac-Toe | 0.01 | 8 | 0.50 |
| Vehicle | 0.02 | 11 | 0.50 |
| Waveform | 0.01 | 45 | 0.50 |
| Wine | 0.01 | 2 | 0.50 |
| Zoo | 0.03 | 3 | 0.50 |

## Key Observations

*So sánh **Our CMAR** với **Paper CMAR**, ngưỡng chênh lệch 0,5 điểm phần trăm.*

- **Thắng / Wins** (Our > Paper hơn 0,5%): 4/26
- **Hòa / Ties** (chênh lệch trong ±0,5%): 2/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 20/26
- **Chênh TB vs Paper CMAR / Average diff:** -2.1%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
