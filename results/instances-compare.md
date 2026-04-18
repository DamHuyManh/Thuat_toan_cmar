# So Sánh Instances: Paper vs Dataset Hiện Có

Nguồn đối chiếu:
- Paper CMAR (Li, Han, Pei 2001): số `Instances` chuẩn.
- File local trong thư mục `datasets/*.csv`: số dòng dữ liệu hiện có.

## Kết quả tổng quan

- Tổng dataset kiểm tra: **26**
- Khớp (`MATCH`): **23**
- Lệch (`MISMATCH`): **3**

## Bảng chi tiết

| Dataset | Paper Instances | Local Instances | Delta (Local - Paper) | Trạng thái |
|---|---:|---:|---:|---|
| Anneal | 898 | 798 | -100 | MISMATCH |
| Australian | 690 | 690 | 0 | MATCH |
| Auto (Imports-85) | 205 | 205 | 0 | MATCH |
| Breast-Cancer-Wisconsin | 683 | 699 | 16 | MISMATCH |
| Cleve (Cleveland Heart) | 303 | 303 | 0 | MATCH |
| Crx (Credit Approval) | 690 | 690 | 0 | MATCH |
| Diabetes (Pima) | 768 | 768 | 0 | MATCH |
| German | 1000 | 1000 | 0 | MATCH |
| Glass | 214 | 214 | 0 | MATCH |
| Heart (Statlog) | 270 | 270 | 0 | MATCH |
| Hepatitis | 155 | 155 | 0 | MATCH |
| Horse Colic | 368 | 300 | -68 | MISMATCH |
| Hypothyroid | 3163 | 3163 | 0 | MATCH |
| Ionosphere | 351 | 351 | 0 | MATCH |
| Iris | 150 | 150 | 0 | MATCH |
| Labor | 57 | 57 | 0 | MATCH |
| Led7 | 3200 | 3200 | 0 | MATCH |
| Lymphography | 148 | 148 | 0 | MATCH |
| Pima | 768 | 768 | 0 | MATCH |
| Sick (Thyroid) | 2800 | 2800 | 0 | MATCH |
| Sonar | 208 | 208 | 0 | MATCH |
| Tic-Tac-Toe | 958 | 958 | 0 | MATCH |
| Vehicle (Statlog) | 846 | 846 | 0 | MATCH |
| Waveform | 5000 | 5000 | 0 | MATCH |
| Wine | 178 | 178 | 0 | MATCH |
| Zoo | 101 | 101 | 0 | MATCH |

## Ghi chú cho 3 dataset lệch

- **Anneal**: file hiện có là 798 dòng; để khớp paper 898 thường cần ghép thêm phần test (`anneal.test`).
- **Horse Colic**: file hiện có 300 dòng; để khớp paper 368 thường cần bổ sung phần test (`horse-colic.test`).
- **Breast-Cancer-Wisconsin**: file local 699 dòng raw; paper dùng 683 (thường sau bước lọc dữ liệu thiếu/không hợp lệ).
