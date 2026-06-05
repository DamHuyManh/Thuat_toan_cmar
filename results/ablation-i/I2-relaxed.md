# CMAR — Báo cáo benchmark (tóm tắt)

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-20 |
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
| Anneal | 898 | 38 | 6 | **98.3%** | 97.3% | 97.9% | 94.8% | +1.0% |
| Australian | 690 | 14 | 2 | **86.1%** | 86.1% | 84.9% | 84.7% | -0.0% |
| Auto | 205 | 25 | 6 | **81.4%** | 78.1% | 78.3% | 80.1% | +3.3% |
| Breast-Cancer | 683 | 9 | 2 | **96.3%** | 96.4% | 96.3% | 95.0% | -0.1% |
| Cleve | 303 | 13 | 2 | **81.9%** | 82.2% | 82.8% | 78.2% | -0.3% |
| Crx | 690 | 15 | 2 | **86.3%** | 84.9% | 84.7% | 84.9% | +1.4% |
| Diabetes | 768 | 8 | 2 | **73.7%** | 75.8% | 74.5% | 74.2% | -2.1% |
| German | 1000 | 20 | 2 | **73.4%** | 74.9% | 73.4% | 72.3% | -1.5% |
| Glass | 214 | 9 | 6 | **69.9%** | 70.1% | 73.9% | 68.7% | -0.2% |
| Heart | 270 | 13 | 2 | **79.3%** | 82.2% | 81.9% | 80.8% | -2.9% |
| Hepatitis | 155 | 19 | 2 | **80.1%** | 80.5% | 81.8% | 80.6% | -0.4% |
| Horse | 368 | 22 | 2 | **82.3%** | 82.6% | 82.1% | 82.6% | -0.3% |
| Hypo | 3163 | 25 | 2 | **98.2%** | 98.4% | 98.9% | 99.2% | -0.2% |
| Iono | 351 | 34 | 2 | **92.9%** | 91.5% | 92.3% | 90.0% | +1.4% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **91.7%** | 89.7% | 86.3% | 79.3% | +2.0% |
| Led7 | 3200 | 7 | 10 | **73.5%** | 72.5% | 71.9% | 73.5% | +1.0% |
| Lymphography | 148 | 18 | 4 | **84.6%** | 83.1% | 77.8% | 73.5% | +1.5% |
| Pima | 768 | 8 | 2 | **73.7%** | 75.1% | 72.9% | 75.5% | -1.4% |
| Sick | 2800 | 29 | 2 | **97.4%** | 97.5% | 97.0% | 98.5% | -0.1% |
| Sonar | 208 | 60 | 2 | **80.3%** | 79.4% | 77.5% | 70.2% | +0.9% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.2%** | 99.2% | 99.6% | 99.4% | -0.0% |
| Vehicle | 846 | 18 | 4 | **68.7%** | 68.8% | 68.7% | 72.6% | -0.1% |
| Waveform | 5000 | 21 | 3 | **82.1%** | 83.2% | 80.0% | 78.1% | -1.1% |
| Wine | 178 | 13 | 3 | **95.1%** | 95.0% | 95.0% | 92.7% | +0.1% |
| Zoo | 101 | 16 | 7 | **96.5%** | 97.1% | 96.8% | 92.2% | -0.6% |
| **Average** | | | | **85.2%** | 85.2% | 84.7% | 83.3% | -0.0% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 574 ms | 0 ms | 156588 | 183 | 99.9% |
| Australian | 42 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 439 ms | 0 ms | 209009 | 237 | 99.9% |
| Breast-Cancer | 8 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 12 ms | 0 ms | 16274 | 279 | 98.3% |
| Crx | 40 ms | 0 ms | 30762 | 559 | 98.2% |
| Diabetes | 3 ms | 0 ms | 1585 | 219 | 86.2% |
| German | 159 ms | 0 ms | 89483 | 951 | 98.9% |
| Glass | 6 ms | 0 ms | 2021 | 143 | 92.9% |
| Heart | 15 ms | 0 ms | 15134 | 253 | 98.3% |
| Hepatitis | 43 ms | 0 ms | 38172 | 131 | 99.7% |
| Horse | 167 ms | 0 ms | 129386 | 404 | 99.7% |
| Hypo | 231 ms | 1 ms | 86450 | 186 | 99.8% |
| Iono | 333 ms | 0 ms | 129736 | 201 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 38 | 57.8% |
| Labor | 20 ms | 0 ms | 24003 | 51 | 99.8% |
| Led7 | 5 ms | 0 ms | 243 | 100 | 58.8% |
| Lymphography | 74 ms | 0 ms | 65800 | 170 | 99.7% |
| Pima | 5 ms | 0 ms | 1585 | 219 | 86.2% |
| Sick | 223 ms | 1 ms | 85874 | 291 | 99.7% |
| Sonar | 1076 ms | 0 ms | 160000 | 178 | 99.9% |
| Tic-Tac-Toe | 9 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 86 ms | 0 ms | 36922 | 492 | 98.7% |
| Waveform | 515 ms | 5 ms | 75473 | 2650 | 96.5% |
| Wine | 24 ms | 0 ms | 16933 | 62 | 99.6% |
| Zoo | 20 ms | 0 ms | 13758 | 76 | 99.4% |

## F1 / Precision / Recall (macro average over folds)

| Dataset | Accuracy | Precision (macro) | Recall (macro) | F1 (macro) | F1 (weighted) |
|---------|---------:|------------------:|---------------:|-----------:|--------------:|
| Anneal | 0.9833 | 0.8287 | 0.8223 | 0.8240 | 0.9787 |
| Australian | 0.8609 | 0.8615 | 0.8587 | 0.8587 | 0.8606 |
| Auto | 0.8143 | 0.8380 | 0.8066 | 0.7988 | 0.8099 |
| Breast-Cancer | 0.9634 | 0.9622 | 0.9572 | 0.9593 | 0.9632 |
| Cleve | 0.8190 | 0.8328 | 0.8151 | 0.8141 | 0.8162 |
| Crx | 0.8628 | 0.8638 | 0.8594 | 0.8603 | 0.8623 |
| Diabetes | 0.7370 | 0.7327 | 0.6647 | 0.6720 | 0.7158 |
| German | 0.7340 | 0.6867 | 0.6224 | 0.6298 | 0.7081 |
| Glass | 0.6988 | 0.5923 | 0.6421 | 0.6028 | 0.6711 |
| Heart | 0.7926 | 0.8017 | 0.7858 | 0.7864 | 0.7899 |
| Hepatitis | 0.8013 | 0.6408 | 0.6436 | 0.6334 | 0.7785 |
| Horse | 0.8234 | 0.8138 | 0.8111 | 0.8098 | 0.8227 |
| Hypo | 0.9817 | 0.9646 | 0.8301 | 0.8812 | 0.9800 |
| Iono | 0.9287 | 0.9360 | 0.9108 | 0.9200 | 0.9275 |
| Iris | 0.9267 | 0.9360 | 0.9267 | 0.9258 | 0.9258 |
| Labor | 0.9167 | 0.9258 | 0.9000 | 0.9009 | 0.9134 |
| Led7 | 0.7350 | 0.7461 | 0.7335 | 0.7264 | 0.7274 |
| Lymphography | 0.8462 | 0.7423 | 0.7491 | 0.7389 | 0.8334 |
| Pima | 0.7370 | 0.7327 | 0.6647 | 0.6720 | 0.7158 |
| Sick | 0.9739 | 0.9045 | 0.8626 | 0.8799 | 0.9732 |
| Sonar | 0.8030 | 0.8119 | 0.8036 | 0.8009 | 0.8016 |
| Tic-Tac-Toe | 0.9916 | 0.9923 | 0.9893 | 0.9906 | 0.9916 |
| Vehicle | 0.6870 | 0.6655 | 0.6903 | 0.6667 | 0.6647 |
| Waveform | 0.8212 | 0.8221 | 0.8209 | 0.8202 | 0.8204 |
| Wine | 0.9506 | 0.9591 | 0.9521 | 0.9516 | 0.9500 |
| Zoo | 0.9652 | 0.9081 | 0.9371 | 0.9181 | 0.9530 |
| **Average** | **0.8521** | **0.8270** | **0.8100** | **0.8093** | **0.8444** |

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

- **Thắng / Wins** (Our > Paper hơn 0,5%): 8/26
- **Hòa / Ties** (chênh lệch trong ±0,5%): 11/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 7/26
- **Chênh TB vs Paper CMAR / Average diff:** -0.0%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
