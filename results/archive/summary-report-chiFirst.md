# CMAR — Báo cáo benchmark (tóm tắt)

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-15 |
| **Bài báo tham chiếu** | Li, Han, Pei — *CMAR* (IEEE ICDM 2001) |
| **Code** | Java — bitmap matching, CR-tree có hash, chi-square + coverage pruning |
| **Đánh giá** | 10-fold cross-validation |

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
| Anneal | 898 | 38 | 6 | **93.2%** | 97.3% | 97.9% | 94.8% | -4.1% |
| Australian | 690 | 14 | 2 | **85.5%** | 86.1% | 84.9% | 84.7% | -0.6% |
| Auto | 205 | 25 | 6 | **65.5%** | 78.1% | 78.3% | 80.1% | -12.6% |
| Breast-Cancer | 683 | 9 | 2 | **90.2%** | 96.4% | 96.3% | 95.0% | -6.2% |
| Cleve | 303 | 13 | 2 | **77.9%** | 82.2% | 82.8% | 78.2% | -4.3% |
| Crx | 690 | 15 | 2 | **85.5%** | 84.9% | 84.7% | 84.9% | +0.6% |
| Diabetes | 768 | 8 | 2 | **71.4%** | 75.8% | 74.5% | 74.2% | -4.4% |
| German | 1000 | 20 | 2 | **70.0%** | 74.9% | 73.4% | 72.3% | -4.9% |
| Glass | 214 | 9 | 6 | **68.2%** | 70.1% | 73.9% | 68.7% | -1.9% |
| Heart | 270 | 13 | 2 | **80.0%** | 82.2% | 81.9% | 80.8% | -2.2% |
| Hepatitis | 155 | 19 | 2 | **75.0%** | 80.5% | 81.8% | 80.6% | -5.5% |
| Horse | 368 | 22 | 2 | **80.9%** | 82.6% | 82.1% | 82.6% | -1.7% |
| Hypo | 3163 | 25 | 2 | **98.7%** | 98.4% | 98.9% | 99.2% | +0.3% |
| Iono | 351 | 34 | 2 | **91.2%** | 91.5% | 92.3% | 90.0% | -0.3% |
| Iris | 150 | 4 | 3 | **94.0%** | 94.0% | 94.7% | 95.3% | +0.0% |
| Labor | 57 | 16 | 2 | **86.0%** | 89.7% | 86.3% | 79.3% | -3.7% |
| Led7 | 3200 | 7 | 10 | **69.9%** | 72.5% | 71.9% | 73.5% | -2.6% |
| Lymphography | 148 | 18 | 4 | **74.4%** | 83.1% | 77.8% | 73.5% | -8.7% |
| Pima | 768 | 8 | 2 | **71.4%** | 75.1% | 72.9% | 75.5% | -3.7% |
| Sick | 2800 | 29 | 2 | **96.7%** | 97.5% | 97.0% | 98.5% | -0.8% |
| Sonar | 208 | 60 | 2 | **74.0%** | 79.4% | 77.5% | 70.2% | -5.4% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.2%** | 99.2% | 99.6% | 99.4% | -0.0% |
| Vehicle | 846 | 18 | 4 | **58.6%** | 68.8% | 68.7% | 72.6% | -10.2% |
| Waveform | 5000 | 21 | 3 | **72.8%** | 83.2% | 80.0% | 78.1% | -10.4% |
| Wine | 178 | 13 | 3 | **92.4%** | 95.0% | 95.0% | 92.7% | -2.6% |
| Zoo | 101 | 16 | 7 | **97.3%** | 97.1% | 96.8% | 92.2% | +0.2% |
| **Average** | | | | **81.5%** | 85.2% | 84.7% | 83.3% | -3.7% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 1010 ms | 0 ms | 156588 | 143 | 99.9% |
| Australian | 62 ms | 0 ms | 18745 | 20 | 99.9% |
| Auto | 680 ms | 0 ms | 209009 | 80 | 100.0% |
| Breast-Cancer | 7 ms | 0 ms | 2836 | 75 | 97.4% |
| Cleve | 25 ms | 0 ms | 16274 | 44 | 99.7% |
| Crx | 79 ms | 0 ms | 30762 | 23 | 99.9% |
| Diabetes | 5 ms | 0 ms | 1585 | 29 | 98.2% |
| German | 266 ms | 0 ms | 89483 | 101 | 99.9% |
| Glass | 5 ms | 0 ms | 2021 | 55 | 97.3% |
| Heart | 24 ms | 0 ms | 15134 | 41 | 99.7% |
| Hepatitis | 90 ms | 0 ms | 38172 | 29 | 99.9% |
| Horse | 314 ms | 0 ms | 129386 | 14 | 100.0% |
| Hypo | 403 ms | 0 ms | 86450 | 32 | 100.0% |
| Iono | 626 ms | 0 ms | 129736 | 77 | 99.9% |
| Iris | 0 ms | 0 ms | 90 | 18 | 80.0% |
| Labor | 35 ms | 0 ms | 24003 | 21 | 99.9% |
| Led7 | 6 ms | 0 ms | 243 | 99 | 59.3% |
| Lymphography | 129 ms | 0 ms | 65800 | 47 | 99.9% |
| Pima | 4 ms | 0 ms | 1585 | 29 | 98.2% |
| Sick | 431 ms | 0 ms | 85874 | 12 | 100.0% |
| Sonar | 2128 ms | 0 ms | 160000 | 68 | 100.0% |
| Tic-Tac-Toe | 16 ms | 0 ms | 7047 | 125 | 98.2% |
| Vehicle | 156 ms | 0 ms | 36922 | 116 | 99.7% |
| Waveform | 667 ms | 1 ms | 75473 | 128 | 99.8% |
| Wine | 45 ms | 0 ms | 16933 | 39 | 99.8% |
| Zoo | 41 ms | 0 ms | 13758 | 31 | 99.8% |

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

- **Thắng / Wins** (Our > Paper hơn 0,5%): 1/26
- **Hòa / Ties** (chênh lệch trong ±0,5%): 5/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 20/26
- **Chênh TB vs Paper CMAR / Average diff:** -3.7%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
