# Báo Cáo So Sánh Accuracy (Hiện Tại)

Tệp dùng để so sánh: `results/summary-report.md` (đã chạy lại sau khi tắt auto-tune để chạy nhanh).

## Tổng quan

- Dataset kiểm tra trong lần chạy này: **24** (do `Anneal` và `Horse` bị skip vì local chưa đủ số dòng theo `paper Instances`)
- Trung bình chênh lệch `Our - Paper`: **-0.3%**
- Thắng/Hòa/Thua (ngưỡng 0.5%):
  - Thắng: 9
  - Hòa: 5
  - Thua: 10

## Bảng chênh lệch accuracy (Our vs Paper CMAR)

| Dataset | Our CMAR | Paper CMAR | Chênh lệch |
|---|---:|---:|---:|
| Australian | 88.0% | 86.1% | +1.9% |
| Auto | 77.7% | 78.1% | -0.4% |
| Breast-Cancer | 96.0% | 96.4% | -0.4% |
| Cleve | 84.2% | 82.2% | +2.0% |
| Crx | 87.7% | 84.9% | +2.8% |
| Diabetes | 73.8% | 75.8% | -2.0% |
| German | 73.1% | 74.9% | -1.8% |
| Glass | 69.6% | 70.1% | -0.5% |
| Heart | 81.9% | 82.2% | -0.3% |
| Hepatitis | 82.3% | 80.5% | +1.8% |
| Hypo | 96.6% | 98.4% | -1.8% |
| Iono | 89.4% | 91.5% | -2.1% |
| Iris | 96.0% | 94.0% | +2.0% |
| Labor | 93.0% | 89.7% | +3.3% |
| Led7 | 72.2% | 72.5% | -0.3% |
| Lymphography | 84.8% | 83.1% | +1.7% |
| Pima | 73.8% | 75.1% | -1.3% |
| Sick | 95.1% | 97.5% | -2.4% |
| Sonar | 80.8% | 79.4% | +1.4% |
| Tic-Tac-Toe | 99.3% | 99.2% | +0.1% |
| Vehicle | 67.9% | 69.0% | -1.1% |
| Waveform | 79.2% | 83.2% | -4.0% |
| Wine | 95.6% | 95.0% | +0.6% |
| Zoo | 90.4% | 97.1% | -6.7% |

## Hai dataset bị skip (do thiếu Instances)

- `Anneal`: local 798 < paper 898
- `Horse`: local 300 < paper 368

Nếu bạn bổ sung `anneal.test` và `horse-colic.test` (hoặc bản tương đương .csv), mình có thể chạy lại full 26/26 để so sánh công bằng hơn.

