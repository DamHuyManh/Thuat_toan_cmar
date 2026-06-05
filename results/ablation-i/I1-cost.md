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
| Anneal | 898 | 38 | 6 | **98.0%** | 97.3% | 97.9% | 94.8% | +0.7% |
| Australian | 690 | 14 | 2 | **86.8%** | 86.1% | 84.9% | 84.7% | +0.7% |
| Auto | 205 | 25 | 6 | **82.4%** | 78.1% | 78.3% | 80.1% | +4.3% |
| Breast-Cancer | 683 | 9 | 2 | **97.4%** | 96.4% | 96.3% | 95.0% | +1.0% |
| Cleve | 303 | 13 | 2 | **82.6%** | 82.2% | 82.8% | 78.2% | +0.4% |
| Crx | 690 | 15 | 2 | **86.1%** | 84.9% | 84.7% | 84.9% | +1.2% |
| Diabetes | 768 | 8 | 2 | **73.3%** | 75.8% | 74.5% | 74.2% | -2.5% |
| German | 1000 | 20 | 2 | **72.7%** | 74.9% | 73.4% | 72.3% | -2.2% |
| Glass | 214 | 9 | 6 | **71.8%** | 70.1% | 73.9% | 68.7% | +1.7% |
| Heart | 270 | 13 | 2 | **80.0%** | 82.2% | 81.9% | 80.8% | -2.2% |
| Hepatitis | 155 | 19 | 2 | **84.8%** | 80.5% | 81.8% | 80.6% | +4.3% |
| Horse | 368 | 22 | 2 | **81.3%** | 82.6% | 82.1% | 82.6% | -1.3% |
| Hypo | 3163 | 25 | 2 | **98.0%** | 98.4% | 98.9% | 99.2% | -0.4% |
| Iono | 351 | 34 | 2 | **93.2%** | 91.5% | 92.3% | 90.0% | +1.7% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **93.3%** | 89.7% | 86.3% | 79.3% | +3.6% |
| Led7 | 3200 | 7 | 10 | **72.8%** | 72.5% | 71.9% | 73.5% | +0.3% |
| Lymphography | 148 | 18 | 4 | **82.5%** | 83.1% | 77.8% | 73.5% | -0.6% |
| Pima | 768 | 8 | 2 | **73.3%** | 75.1% | 72.9% | 75.5% | -1.8% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| Sonar | 208 | 60 | 2 | **81.3%** | 79.4% | 77.5% | 70.2% | +1.9% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.1%** | 99.2% | 99.6% | 99.4% | -0.1% |
| Vehicle | 846 | 18 | 4 | **68.2%** | 68.8% | 68.7% | 72.6% | -0.6% |
| Waveform | 5000 | 21 | 3 | **81.6%** | 83.2% | 80.0% | 78.1% | -1.6% |
| Wine | 178 | 13 | 3 | **95.1%** | 95.0% | 95.0% | 92.7% | +0.1% |
| Zoo | 101 | 16 | 7 | **94.8%** | 97.1% | 96.8% | 92.2% | -2.3% |
| **Average** | | | | **85.4%** | 85.2% | 84.7% | 83.3% | +0.2% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 550 ms | 0 ms | 156588 | 183 | 99.9% |
| Australian | 38 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 490 ms | 0 ms | 209009 | 237 | 99.9% |
| Breast-Cancer | 4 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 15 ms | 0 ms | 16274 | 279 | 98.3% |
| Crx | 48 ms | 0 ms | 30762 | 559 | 98.2% |
| Diabetes | 3 ms | 0 ms | 1585 | 219 | 86.2% |
| German | 149 ms | 0 ms | 89483 | 951 | 98.9% |
| Glass | 7 ms | 0 ms | 2021 | 143 | 92.9% |
| Heart | 14 ms | 0 ms | 15134 | 253 | 98.3% |
| Hepatitis | 56 ms | 0 ms | 38172 | 131 | 99.7% |
| Horse | 168 ms | 0 ms | 129386 | 404 | 99.7% |
| Hypo | 232 ms | 1 ms | 86450 | 186 | 99.8% |
| Iono | 340 ms | 0 ms | 129736 | 201 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 38 | 57.8% |
| Labor | 20 ms | 0 ms | 24003 | 51 | 99.8% |
| Led7 | 6 ms | 0 ms | 243 | 100 | 58.8% |
| Lymphography | 77 ms | 0 ms | 65800 | 170 | 99.7% |
| Pima | 4 ms | 0 ms | 1585 | 219 | 86.2% |
| Sick | 227 ms | 1 ms | 85874 | 291 | 99.7% |
| Sonar | 1091 ms | 0 ms | 160000 | 178 | 99.9% |
| Tic-Tac-Toe | 11 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 76 ms | 0 ms | 36922 | 492 | 98.7% |
| Waveform | 574 ms | 5 ms | 75473 | 2650 | 96.5% |
| Wine | 27 ms | 0 ms | 16933 | 62 | 99.6% |
| Zoo | 26 ms | 0 ms | 13758 | 76 | 99.4% |

## F1 / Precision / Recall (macro average over folds)

| Dataset | Accuracy | Precision (macro) | Recall (macro) | F1 (macro) | F1 (weighted) |
|---------|---------:|------------------:|---------------:|-----------:|--------------:|
| Anneal | 0.9800 | 0.8192 | 0.8264 | 0.8211 | 0.9759 |
| Australian | 0.8681 | 0.8678 | 0.8664 | 0.8662 | 0.8678 |
| Auto | 0.8236 | 0.8335 | 0.8134 | 0.8010 | 0.8235 |
| Breast-Cancer | 0.9735 | 0.9696 | 0.9727 | 0.9709 | 0.9736 |
| Cleve | 0.8257 | 0.8363 | 0.8219 | 0.8211 | 0.8231 |
| Crx | 0.8612 | 0.8615 | 0.8586 | 0.8590 | 0.8609 |
| Diabetes | 0.7331 | 0.7255 | 0.6625 | 0.6692 | 0.7128 |
| German | 0.7270 | 0.6932 | 0.5850 | 0.5808 | 0.6797 |
| Glass | 0.7182 | 0.6148 | 0.6661 | 0.6273 | 0.6891 |
| Heart | 0.8000 | 0.8082 | 0.7942 | 0.7943 | 0.7975 |
| Hepatitis | 0.8480 | 0.7874 | 0.8029 | 0.7888 | 0.8535 |
| Horse | 0.8126 | 0.8049 | 0.8071 | 0.8002 | 0.8126 |
| Hypo | 0.9798 | 0.9646 | 0.8040 | 0.8638 | 0.9775 |
| Iono | 0.9318 | 0.9315 | 0.9207 | 0.9246 | 0.9311 |
| Iris | 0.9267 | 0.9360 | 0.9267 | 0.9258 | 0.9258 |
| Labor | 0.9333 | 0.9358 | 0.9250 | 0.9231 | 0.9320 |
| Led7 | 0.7276 | 0.7478 | 0.7251 | 0.7166 | 0.7177 |
| Lymphography | 0.8252 | 0.7005 | 0.6915 | 0.6903 | 0.8268 |
| Pima | 0.7331 | 0.7255 | 0.6625 | 0.6692 | 0.7128 |
| Sick | 0.9679 | 0.8963 | 0.8028 | 0.8396 | 0.9655 |
| Sonar | 0.8125 | 0.8207 | 0.8132 | 0.8105 | 0.8112 |
| Tic-Tac-Toe | 0.9906 | 0.9908 | 0.9885 | 0.9895 | 0.9906 |
| Vehicle | 0.6822 | 0.6609 | 0.6856 | 0.6588 | 0.6567 |
| Waveform | 0.8162 | 0.8177 | 0.8157 | 0.8146 | 0.8147 |
| Wine | 0.9506 | 0.9591 | 0.9521 | 0.9516 | 0.9500 |
| Zoo | 0.9477 | 0.8914 | 0.9300 | 0.9026 | 0.9378 |
| **Average** | **0.8537** | **0.8308** | **0.8123** | **0.8108** | **0.8469** |

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

- **Thắng / Wins** (Our > Paper hơn 0,5%): 10/26
- **Hòa / Ties** (chênh lệch trong ±0,5%): 5/26
- **Thua / Losses** (Our thấp hơn Paper hơn 0,5%): 11/26
- **Chênh TB vs Paper CMAR / Average diff:** +0.2%

## Optimizations Applied

1. **Bitmap rule matching** — kiểm tra tiền đề bằng AND bit, tối ưu khớp luật.
2. **Hash-indexed CR-tree** — lưu luật theo lớp, cắt nhánh nhờ mục đầu tiên.
3. **Chi-square pruning (CSP)** — bỏ luật không có ý nghĩa thống kê (p < 0,05).
4. **Database coverage pruning (DCP)** — bỏ luật dư thừa theo độ phủ.
5. **Single-path FP-tree** — tối ưu khi chỉ còn một nhánh.
6. **Weighted voting** — trọng số ≈ chi-square × confidence; top-5 mỗi lớp khi bỏ phiếu.
7. **Per-class adaptive minSupport** — lớp hiếm (≤10 mẫu trong fold) dùng support tối thiểu 1.
8. **Max antecedent length** — giới hạn độ dài tiền đề tối đa 4 mục.
