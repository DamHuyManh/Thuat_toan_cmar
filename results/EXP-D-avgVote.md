# CMAR — Báo cáo benchmark (tóm tắt)

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-19 |
| **Bài báo tham chiếu** | Li, Han, Pei — *CMAR* (IEEE ICDM 2001) |
| **Code** | Java — bitmap matching, CR-tree có hash, chi-square + coverage pruning |
| **Đánh giá** | 10-fold cross-validation |
| **Dự đoán** | Top-k toàn cục: chỉ lấy **3** luật khớp tốt nhất khi bỏ phiếu (khác bản paper đầy đủ) |

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
| Anneal | 898 | 38 | 6 | **97.7%** | 97.3% | 97.9% | 94.8% | +0.4% |
| Australian | 690 | 14 | 2 | **85.6%** | 86.1% | 84.9% | 84.7% | -0.5% |
| Auto | 205 | 25 | 6 | **80.8%** | 78.1% | 78.3% | 80.1% | +2.7% |
| Breast-Cancer | 683 | 9 | 2 | **96.6%** | 96.4% | 96.3% | 95.0% | +0.2% |
| Cleve | 303 | 13 | 2 | **80.9%** | 82.2% | 82.8% | 78.2% | -1.3% |
| Crx | 690 | 15 | 2 | **85.7%** | 84.9% | 84.7% | 84.9% | +0.8% |
| Diabetes | 768 | 8 | 2 | **73.3%** | 75.8% | 74.5% | 74.2% | -2.5% |
| German | 1000 | 20 | 2 | **72.1%** | 74.9% | 73.4% | 72.3% | -2.8% |
| Glass | 214 | 9 | 6 | **70.0%** | 70.1% | 73.9% | 68.7% | -0.1% |
| Heart | 270 | 13 | 2 | **79.3%** | 82.2% | 81.9% | 80.8% | -2.9% |
| Hepatitis | 155 | 19 | 2 | **82.0%** | 80.5% | 81.8% | 80.6% | +1.5% |
| Horse | 368 | 22 | 2 | **81.2%** | 82.6% | 82.1% | 82.6% | -1.4% |
| Hypo | 3163 | 25 | 2 | **97.9%** | 98.4% | 98.9% | 99.2% | -0.5% |
| Iono | 351 | 34 | 2 | **92.0%** | 91.5% | 92.3% | 90.0% | +0.5% |
| Iris | 150 | 4 | 3 | **93.3%** | 94.0% | 94.7% | 95.3% | -0.7% |
| Labor | 57 | 16 | 2 | **83.0%** | 89.7% | 86.3% | 79.3% | -6.7% |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| Lymphography | 148 | 18 | 4 | **84.6%** | 83.1% | 77.8% | 73.5% | +1.5% |
| Pima | 768 | 8 | 2 | **73.3%** | 75.1% | 72.9% | 75.5% | -1.8% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| Sonar | 208 | 60 | 2 | **79.3%** | 79.4% | 77.5% | 70.2% | -0.1% |
| Tic-Tac-Toe | 958 | 9 | 2 | **98.3%** | 99.2% | 99.6% | 99.4% | -0.9% |
| Vehicle | 846 | 18 | 4 | **67.9%** | 68.8% | 68.7% | 72.6% | -0.9% |
| Waveform | 5000 | 21 | 3 | **81.5%** | 83.2% | 80.0% | 78.1% | -1.7% |
| Wine | 178 | 13 | 3 | **94.0%** | 95.0% | 95.0% | 92.7% | -1.0% |
| Zoo | 101 | 16 | 7 | **95.6%** | 97.1% | 96.8% | 92.2% | -1.5% |
| **Average** | | | | **84.4%** | 85.2% | 84.7% | 83.3% | -0.8% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 1440 ms | 1 ms | 156588 | 159 | 99.9% |
| Australian | 109 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 1304 ms | 0 ms | 209009 | 208 | 99.9% |
| Breast-Cancer | 10 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 35 ms | 0 ms | 16274 | 276 | 98.3% |
| Crx | 113 ms | 0 ms | 30762 | 557 | 98.2% |
| Diabetes | 5 ms | 0 ms | 1585 | 213 | 86.6% |
| German | 422 ms | 0 ms | 89483 | 951 | 98.9% |
| Glass | 7 ms | 0 ms | 2021 | 121 | 94.0% |
| Heart | 31 ms | 0 ms | 15134 | 249 | 98.4% |
| Hepatitis | 121 ms | 0 ms | 38172 | 122 | 99.7% |
| Horse | 483 ms | 0 ms | 129386 | 397 | 99.7% |
| Hypo | 561 ms | 2 ms | 86450 | 176 | 99.8% |
| Iono | 982 ms | 0 ms | 129736 | 196 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 30 | 66.7% |
| Labor | 50 ms | 0 ms | 24003 | 49 | 99.8% |
| Led7 | 17 ms | 0 ms | 243 | 112 | 53.9% |
| Lymphography | 212 ms | 0 ms | 65800 | 149 | 99.8% |
| Pima | 5 ms | 0 ms | 1585 | 213 | 86.6% |
| Sick | 628 ms | 2 ms | 85874 | 279 | 99.7% |
| Sonar | 2652 ms | 0 ms | 160000 | 172 | 99.9% |
| Tic-Tac-Toe | 19 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 220 ms | 0 ms | 36922 | 477 | 98.7% |
| Waveform | 1467 ms | 16 ms | 75473 | 2650 | 96.5% |
| Wine | 56 ms | 0 ms | 16933 | 54 | 99.7% |
| Zoo | 46 ms | 0 ms | 13758 | 35 | 99.7% |

## F1 / Precision / Recall (macro average over folds)

| Dataset | Accuracy | Precision (macro) | Recall (macro) | F1 (macro) | F1 (weighted) |
|---------|---------:|------------------:|---------------:|-----------:|--------------:|
| Anneal | 0.9765 | 0.8268 | 0.7958 | 0.8039 | 0.9710 |
| Australian | 0.8565 | 0.8560 | 0.8554 | 0.8547 | 0.8564 |
| Auto | 0.8084 | 0.8356 | 0.8082 | 0.7978 | 0.8047 |
| Breast-Cancer | 0.9663 | 0.9634 | 0.9633 | 0.9627 | 0.9662 |
| Cleve | 0.8091 | 0.8143 | 0.8051 | 0.8048 | 0.8070 |
| Crx | 0.8569 | 0.8555 | 0.8557 | 0.8552 | 0.8569 |
| Diabetes | 0.7331 | 0.7261 | 0.6617 | 0.6685 | 0.7124 |
| German | 0.7210 | 0.7005 | 0.5598 | 0.5387 | 0.6545 |
| Glass | 0.6997 | 0.5798 | 0.6169 | 0.5869 | 0.6694 |
| Heart | 0.7926 | 0.7988 | 0.7892 | 0.7877 | 0.7905 |
| Hepatitis | 0.8201 | 0.7026 | 0.6551 | 0.6615 | 0.7983 |
| Horse | 0.8123 | 0.8084 | 0.7870 | 0.7920 | 0.8087 |
| Hypo | 0.9795 | 0.9639 | 0.8006 | 0.8611 | 0.9771 |
| Iono | 0.9202 | 0.9351 | 0.8956 | 0.9084 | 0.9177 |
| Iris | 0.9333 | 0.9416 | 0.9333 | 0.9325 | 0.9325 |
| Labor | 0.8300 | 0.7933 | 0.7750 | 0.7676 | 0.8055 |
| Led7 | 0.7217 | 0.7413 | 0.7189 | 0.7089 | 0.7102 |
| Lymphography | 0.8458 | 0.7170 | 0.7241 | 0.7129 | 0.8302 |
| Pima | 0.7331 | 0.7261 | 0.6617 | 0.6685 | 0.7124 |
| Sick | 0.9679 | 0.9003 | 0.7973 | 0.8379 | 0.9653 |
| Sonar | 0.7932 | 0.8055 | 0.7958 | 0.7911 | 0.7914 |
| Tic-Tac-Toe | 0.9833 | 0.9878 | 0.9758 | 0.9811 | 0.9831 |
| Vehicle | 0.6786 | 0.6581 | 0.6820 | 0.6558 | 0.6537 |
| Waveform | 0.8148 | 0.8162 | 0.8144 | 0.8132 | 0.8134 |
| Wine | 0.9398 | 0.9522 | 0.9358 | 0.9376 | 0.9384 |
| Zoo | 0.9561 | 0.9010 | 0.9336 | 0.9113 | 0.9448 |
| **Average** | **0.8442** | **0.8195** | **0.7922** | **0.7924** | **0.8335** |

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

- **Thắng / Wins** (Our > Paper hơn 0,5%): 5/26
- **Hòa / Ties** (chênh lệch trong ±0,5%): 7/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 14/26
- **Chênh TB vs Paper CMAR / Average diff:** -0.8%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
