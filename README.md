# Cách Cải Tiến và Kết Quả

**Đề tài:** Cải tiến hiệu năng thuật toán phân lớp dựa trên luật kết hợp (CMAR)
**Ngày:** 28/04/2026

---

## 1. Vấn Đề

Thuật toán CMAR gốc có 2 nhược điểm chính:

1. **Đếm thừa**: Mỗi luật phải duyệt toàn bộ dữ liệu để đếm số mẫu khớp. Việc đếm này lặp lại ở **3 giai đoạn cắt tỉa** khác nhau.
2. **Kiểm tra từng cái một**: Kiểm tra "mẫu nào chứa thuộc tính A" được làm tuần tự cho từng mẫu.

Hệ quả: Trên dataset Anneal (898 mẫu, 38 thuộc tính), chương trình thực hiện **5,6 tỷ phép so sánh bit** chỉ để xử lý 1 dataset.

---

## 2. Hai Ý Tưởng Cải Tiến

### Ý tưởng 1 — Lập bảng tra cứu, dùng lại

Xây sẵn **một bảng tra cứu** ngay đầu chương trình: ghi rõ "thuộc tính X có ở những mẫu nào". Bảng này lập **một lần duy nhất**, dùng đi dùng lại cho mọi luật, mọi giai đoạn → **không phải đếm lại**.

> *Giống tra từ điển: có mục lục, biết từ ở trang nào, không phải đọc cả cuốn.*

### Ý tưởng 2 — Xử lý 64 mẫu cùng lúc

Thay vì kiểm tra từng mẫu một, **gom 64 mẫu thành một nhóm** và xử lý cả nhóm trong một thao tác.

> *Giống đếm phiếu bầu: cách cũ mở từng phiếu đếm tay, cách mới quét máy 64 phiếu/lần.*

**Tỷ lệ tăng tốc lý thuyết: ~64 lần.**

---

## 3. Năm Phần Cải Tiến

| # | Phần | Vấn đề cũ | Cách cải tiến |
|---|------|-----------|---------------|
| 1 | Đo lường | Không có công cụ đo, không biết phần nào chậm | Bổ sung công cụ đo thời gian + bộ nhớ từng giai đoạn |
| 2 | Lưu trữ luật | Duyệt tuần tự toàn bộ luật khi phân loại | Tổ chức luật theo **3 tầng chỉ mục**, chỉ duyệt nhóm liên quan |
| 3 | Khai phá luật | Đếm support cho từng tập điều kiện bằng cách quét toàn bộ dữ liệu | Áp dụng **bảng tra cứu** + **xử lý hàng loạt** |
| 4 | Cắt tỉa luật | Ba giai đoạn cắt tỉa đều quét lại dữ liệu cho cùng một luật | **Chia sẻ kết quả** giữa các giai đoạn, tính một lần dùng nhiều lần |
| 5 | Loại luật trùng | Bỏ qua hoàn toàn khi có >10.000 luật | Dùng **dấu vân tay bit** + **chỉ mục theo độ dài** → luôn cắt tỉa |

---

## 4. Kết Quả Tổng Hợp

| Chỉ số | Phiên bản cũ | Phiên bản mới | Cải thiện |
|--------|-------------:|--------------:|----------:|
| Tổng thời gian xử lý | 23.302 ms | 5.131 ms | **4,54× nhanh hơn** |
| Bộ nhớ đỉnh trung bình | 92 MB | 74 MB | **giảm 20%** |
| Độ chính xác trung bình | 85,1% | 85,4% | **+0,3%** |

---

## 5. Bảng Chi Tiết 26 Bộ Dữ Liệu

| # | Dataset | N | Paper | Cũ | Mới | Cũ (ms) | Mới (ms) | Speedup |
|---|---------|------:|------:|------:|------:|--------:|---------:|--------:|
| 1 | Anneal | 898 | 97,3% | 97,7% | **98,2%** | 4.098 | 647 | 6,33× |
| 2 | Australian | 690 | 86,1% | 86,7% | **86,8%** | 220 | 46 | 4,78× |
| 3 | Auto | 205 | 78,1% | 81,4% | 81,4% | 644 | 582 | 1,11× |
| 4 | Breast-Cancer | 683 | 96,4% | 97,1% | 97,1% | 25 | 5 | 5,00× |
| 5 | Cleve | 303 | 82,2% | 82,6% | 82,6% | 83 | 14 | 5,93× |
| 6 | Crx | 690 | 84,9% | 86,0% | **86,1%** | 252 | 40 | 6,30× |
| 7 | Diabetes | 768 | 75,8% | 73,4% | 73,4% | 13 | 2 | 6,50× |
| 8 | German | 1.000 | 74,9% | 72,9% | 72,9% | 1.165 | 139 | **8,38×** |
| 9 | Glass | 214 | 70,1% | 70,0% | 70,0% | 7 | 2 | 3,50× |
| 10 | Heart | 270 | 82,2% | 80,7% | 80,7% | 100 | 14 | 7,14× |
| 11 | Hepatitis | 155 | 80,5% | 83,3% | 83,3% | 150 | 48 | 3,12× |
| 12 | Horse | 368 | 82,6% | 80,7% | **82,3%** | 689 | 165 | 4,18× |
| 13 | Hypo | 3.163 | 98,4% | 98,0% | 98,0% | 2.854 | 115 | **24,82×** |
| 14 | Iono | 351 | 91,5% | 92,0% | **92,6%** | 1.023 | 399 | 2,56× |
| 15 | Iris | 150 | 94,0% | 92,7% | 92,7% | <1 | <1 | — |
| 16 | Labor | 57 | 89,7% | 91,7% | 91,7% | 49 | 20 | 2,45× |
| 17 | Led7 | 3.200 | 72,5% | 72,2% | 72,2% | 23 | 3 | 7,67× |
| 18 | Lymphography | 148 | 83,1% | 83,4% | 83,4% | 157 | 99 | 1,59× |
| 19 | Pima | 768 | 75,1% | 73,4% | 73,4% | 12 | 2 | 6,00× |
| 20 | Sick | 2.800 | 97,5% | 96,5% | **96,8%** | 3.304 | 159 | **20,78×** |
| 21 | Sonar | 208 | 79,4% | 78,4% | **80,8%** | 2.717 | 1.140 | 2,38× |
| 22 | Tic-Tac-Toe | 958 | 99,2% | 99,2% | 99,2% | 72 | 9 | 8,00× |
| 23 | Vehicle | 846 | 68,8% | 68,2% | 68,2% | 463 | 61 | 7,59× |
| 24 | Waveform | 5.000 | 83,2% | 81,6% | 81,6% | 5.107 | 1.374 | 3,72× |
| 25 | Wine | 178 | 95,0% | 96,7% | 96,7% | 43 | 25 | 1,72× |
| 26 | Zoo | 101 | 97,1% | 96,5% | 96,5% | 32 | 21 | 1,52× |
|   | **Trung bình / Tổng** | | **85,2%** | **85,1%** | **85,4%** | **23.302** | **5.131** | **4,54×** |

**Ghi chú:** Số in đậm ở cột "Mới" = độ chính xác cải thiện so với "Cũ".

---

## 6. Điểm Đáng Chú Ý

- **Tăng tốc cao nhất:** Hypo **24,82×**, Sick **20,78×**, German **8,38×**.
- **Độ chính xác cải thiện trên 7 dataset**: Sonar +2,4%, Horse +1,6%, Iono +0,6%, Anneal +0,5%, Sick +0,3%, Australian/Crx +0,1%.
- **Không có dataset nào bị suy giảm** độ chính xác.
- **Vượt bài báo gốc** trên 11/26 dataset; trung bình vượt 0,2 điểm phần trăm.
- **Bộ dữ liệu giữ nguyên** 100%, không sửa nội dung.

---

## 7. Demo

```bash
# Chạy bản gốc (~4 phút 37s)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Chạy bản cải tiến (~1 phút 38s)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved
```

Cùng dataset, cùng tham số, cùng phương pháp đánh giá (10-fold cross-validation, seed=42). Bản mới **nhanh hơn 4,54 lần** mà **độ chính xác không suy giảm**.
