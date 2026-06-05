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
| Anneal | 898 | 38 | 6 | **97.0%** | 97.3% | 97.9% | 94.8% | -0.3% |
| Australian | 690 | 14 | 2 | **83.0%** | 86.1% | 84.9% | 84.7% | -3.1% |
| Auto | 205 | 25 | 6 | **82.7%** | 78.1% | 78.3% | 80.1% | +4.6% |
| Breast-Cancer | 683 | 9 | 2 | **96.6%** | 96.4% | 96.3% | 95.0% | +0.2% |
| Cleve | 303 | 13 | 2 | **79.6%** | 82.2% | 82.8% | 78.2% | -2.6% |
| Crx | 690 | 15 | 2 | **83.8%** | 84.9% | 84.7% | 84.9% | -1.1% |
| Diabetes | 768 | 8 | 2 | **74.0%** | 75.8% | 74.5% | 74.2% | -1.8% |
| German | 1000 | 20 | 2 | **58.3%** | 74.9% | 73.4% | 72.3% | -16.6% |
| Glass | 214 | 9 | 6 | **72.0%** | 70.1% | 73.9% | 68.7% | +1.9% |
| Heart | 270 | 13 | 2 | **80.4%** | 82.2% | 81.9% | 80.8% | -1.8% |
| Hepatitis | 155 | 19 | 2 | **79.5%** | 80.5% | 81.8% | 80.6% | -1.0% |
| Horse | 368 | 22 | 2 | **79.1%** | 82.6% | 82.1% | 82.6% | -3.5% |
| Hypo | 3163 | 25 | 2 | **97.4%** | 98.4% | 98.9% | 99.2% | -1.0% |
| Iono | 351 | 34 | 2 | **92.3%** | 91.5% | 92.3% | 90.0% | +0.8% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **89.3%** | 89.7% | 86.3% | 79.3% | -0.4% |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| Lymphography | 148 | 18 | 4 | **83.6%** | 83.1% | 77.8% | 73.5% | +0.5% |
| Pima | 768 | 8 | 2 | **74.0%** | 75.1% | 72.9% | 75.5% | -1.1% |
| Sick | 2800 | 29 | 2 | **96.5%** | 97.5% | 97.0% | 98.5% | -1.0% |
| Sonar | 208 | 60 | 2 | **76.3%** | 79.4% | 77.5% | 70.2% | -3.1% |
| Tic-Tac-Toe | 958 | 9 | 2 | **91.8%** | 99.2% | 99.6% | 99.4% | -7.4% |
| Vehicle | 846 | 18 | 4 | **67.8%** | 68.8% | 68.7% | 72.6% | -1.0% |
| Waveform | 5000 | 21 | 3 | **81.7%** | 83.2% | 80.0% | 78.1% | -1.5% |
| Wine | 178 | 13 | 3 | **97.8%** | 95.0% | 95.0% | 92.7% | +2.8% |
| Zoo | 101 | 16 | 7 | **96.5%** | 97.1% | 96.8% | 92.2% | -0.6% |
| **Average** | | | | **83.7%** | 85.2% | 84.7% | 83.3% | -1.5% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 1100 ms | 0 ms | 156588 | 213 | 99.9% |
| Australian | 54 ms | 0 ms | 18745 | 525 | 97.2% |
| Auto | 678 ms | 0 ms | 209009 | 296 | 99.9% |
| Breast-Cancer | 8 ms | 0 ms | 2836 | 258 | 90.9% |
| Cleve | 23 ms | 0 ms | 16274 | 291 | 98.2% |
| Crx | 70 ms | 0 ms | 30762 | 627 | 98.0% |
| Diabetes | 4 ms | 0 ms | 1585 | 216 | 86.4% |
| German | 240 ms | 0 ms | 89483 | 867 | 99.0% |
| Glass | 4 ms | 0 ms | 2021 | 135 | 93.3% |
| Heart | 22 ms | 0 ms | 15134 | 267 | 98.2% |
| Hepatitis | 71 ms | 0 ms | 38172 | 140 | 99.6% |
| Horse | 288 ms | 0 ms | 129386 | 434 | 99.7% |
| Hypo | 430 ms | 1 ms | 86450 | 194 | 99.8% |
| Iono | 699 ms | 0 ms | 129736 | 202 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 33 | 63.3% |
| Labor | 35 ms | 0 ms | 24003 | 53 | 99.8% |
| Led7 | 12 ms | 0 ms | 243 | 111 | 54.3% |
| Lymphography | 124 ms | 0 ms | 65800 | 190 | 99.7% |
| Pima | 3 ms | 0 ms | 1585 | 216 | 86.4% |
| Sick | 462 ms | 1 ms | 85874 | 288 | 99.7% |
| Sonar | 2386 ms | 0 ms | 160000 | 184 | 99.9% |
| Tic-Tac-Toe | 14 ms | 0 ms | 7047 | 356 | 94.9% |
| Vehicle | 134 ms | 0 ms | 36922 | 510 | 98.6% |
| Waveform | 696 ms | 10 ms | 75473 | 2650 | 96.5% |
| Wine | 38 ms | 0 ms | 16933 | 75 | 99.6% |
| Zoo | 35 ms | 0 ms | 13758 | 58 | 99.6% |

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
- **Hòa / Ties** (chênh lệch trong ±0,5%): 5/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 17/26
- **Chênh TB vs Paper CMAR / Average diff:** -1.5%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
