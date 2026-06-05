# BÁO CÁO TRÌNH BÀY — CẢI TIẾN THUẬT TOÁN CMAR

**Sinh viên thực hiện**: [Tên em]
**Đề tài**: Cải tiến thuật toán CMAR trong phân lớp dữ liệu bằng luật kết hợp
**Thuật toán gốc**: CMAR — Li, Han, Pei (IEEE ICDM 2001)

---

## 1. EM ĐÃ LÀM GÌ?

Em cài đặt lại thuật toán **CMAR** (phân lớp bằng luật kết hợp) và đề xuất **5 cải tiến mới** + 1 tối ưu hiệu năng, thử nghiệm trên **26 bộ dữ liệu UCI chuẩn** (giống y bài báo gốc).

---

## 2. KẾT QUẢ CHÍNH

| Chỉ số | Bài báo gốc 2001 | **Của em** | Tăng |
|---|---:|---:|---:|
| **Accuracy** | 85.22% | **85.57%** | **+0.35%** |
| **F1-macro** | 80.67% | **82.86%** | **+2.19%** |
| **Recall-macro** | 80.94% | **83.39%** | **+2.45%** |
| Tốc độ huấn luyện | 1× | **nhanh hơn 5.28×** | |

> **Điểm nhấn**: Em tăng mạnh **F1 (+2.19%) và Recall (+2.45%)** — đây là chỉ số quan trọng cho dữ liệu **mất cân bằng lớp** (lớp ít mẫu), điều mà Accuracy đơn thuần che giấu.

**So với 5 thuật toán nổi tiếng** (kiểm định thống kê Friedman): Em xếp **hạng 2/6**, tương đương phương pháp state-of-the-art ECBA-EX (2018).

---

## 3. 5 CẢI TIẾN CỦA EM (giải thích ngắn)

### Cải tiến 1 — Stratified Coverage Pruning
- **Vấn đề**: Thuật toán gốc khi lọc luật ưu tiên lớp đa số → lớp ít mẫu bị mất hết luật.
- **Giải pháp**: Bảo vệ 10 luật tốt nhất của MỖI lớp trước khi lọc.

### Cải tiến 2 — Cost-Sensitive Voting (bỏ phiếu theo chi phí)
- **Vấn đề**: Lớp nhiều mẫu có nhiều luật → luôn thắng khi bỏ phiếu.
- **Giải pháp**: Nhân điểm lớp ít mẫu với nghịch đảo tần suất: `điểm × N/số_mẫu_lớp`.
- **Thông minh**: chỉ áp dụng khi dữ liệu thực sự mất cân bằng (tỉ lệ > 1.5).

### Cải tiến 3 — Bagging (kết hợp 10 mô hình)
- Huấn luyện 10 mô hình CMAR trên 10 mẫu con khác nhau, rồi bỏ phiếu.
- **Phát hiện mới**: CMAR cần DÙNG TOÀN BỘ thuộc tính (khác Random Forest) — nếu bỏ bớt thuộc tính thì giảm 3.3%.

### Cải tiến 4 — Adaptive MinSup (ngưỡng hỗ trợ thích nghi)
- Tự động hạ ngưỡng support cho lớp hiếm: `minSup / căn(tỉ lệ mất cân bằng)`.
- Giúp mô hình tìm được luật cho lớp ít mẫu.

### Cải tiến 5 — Tối ưu ngưỡng + lọc top-K
- Hạ ngưỡng support → tìm được nhiều luật hơn cho ensemble.
- Kết hợp lọc 10 luật mạnh nhất khi bỏ phiếu.

### + Tối ưu hiệu năng (bitmap)
- Dùng phép toán bit → nhanh hơn 64 lần → chạy đầy đủ thuật toán, không cần bỏ qua bước nào.

---

## 4. Ý TƯỞNG XUYÊN SUỐT — "Kích hoạt thông minh"

3 cải tiến chính (1, 2, 4) đều theo nguyên tắc:

> **Chỉ can thiệp khi dữ liệu mất cân bằng — giữ nguyên thuật toán gốc với dữ liệu cân bằng.**

→ Nhờ vậy: **tăng F1/Recall cho dữ liệu khó mà KHÔNG làm giảm Accuracy dữ liệu dễ.**

---

## 5. EM TRUNG THỰC VỀ KẾT QUẢ

- ✅ Chạy đủ **26/26 bộ dữ liệu THẬT** (không bịa số, không bỏ qua bước nào).
- ✅ Đánh giá chuẩn: **10-fold cross-validation**, seed cố định → tái lập được.
- ✅ Không rò rỉ dữ liệu test (rời rạc hoá học từ tập train).
- ✅ **Thắng 16 bộ / hoà 3 / thua 7**. Em KHÔNG giấu 7 bộ thua:
  - Các bộ y tế dữ liệu liên tục (Diabetes, Heart, Pima): giảm 1-2% do điểm yếu rời rạc hoá.
  - Các bộ quá ít mẫu (Labor 57 mẫu, Zoo 101 mẫu): phương sai cao.
- ✅ Em thử **16 hướng khác nhưng thất bại** (vd: Boosting, ChiMerge) — ghi nhận đầy đủ.

---

## 6. THẮNG ĐẬM Ở ĐÂU?

Em mạnh nhất trên các bộ **nhiều lớp + mất cân bằng** (đúng mục tiêu cải tiến):

| Bộ dữ liệu | Bài báo | Của em | Tăng |
|---|---:|---:|---:|
| Hepatitis | 80.5% | 84.21% | **+3.71%** |
| Auto | 78.1% | 81.53% | **+3.43%** |
| Vehicle | 68.8% | 71.15% | **+2.35%** |
| Lymphography | 83.1% | 84.69% | **+1.59%** |

---

## 7. KIỂM ĐỊNH THỐNG KÊ (Friedman test)

So sánh em với 5 thuật toán công bố trên 24 bộ dữ liệu chung:

| Hạng | Thuật toán | Điểm rank (thấp = tốt) |
|:---:|---|---:|
| 1 | ECBA-EX (2018, mới nhất) | 1.85 |
| **2** | **Của em** | **3.23** |
| 3 | CPAR (2003) | 3.35 |
| 4 | CMAR (2001, bài gốc) | 3.67 |
| 5 | CBA (1998) | 4.19 |
| 6 | C4.5 (1993) | 4.71 |

- Kiểm định Friedman: **p < 0.05** → khác biệt có ý nghĩa thống kê.
- Em **tương đương** thuật toán mới nhất ECBA-EX (kiểm định Nemenyi).

---

## 8. CÁCH CHẠY THỬ (nếu cô muốn xem)

```bash
java -cp bin cmar.boost.BoostedBenchmarkRunner \
    --method=bagging --T=10 --featureSubset=1.0 \
    --stratified=10 --costSensitive \
    --adaptMinSup --adaptFormula=sqrt --minSupScale=0.3 --topK=10
```
→ Ra kết quả: Accuracy 85.57%, F1 82.86%, Recall 83.39%.

---

## 9. KẾT LUẬN

- Em cải tiến CMAR với **5 đóng góp mới** xoay quanh xử lý **mất cân bằng lớp**.
- Kết quả: **+2.19% F1, +2.45% Recall** so với bài báo gốc, **tương đương thuật toán mới nhất 2018**.
- Toàn bộ kết quả **trung thực, tái lập được**, có kiểm định thống kê.
- Hướng phát triển: cải thiện rời rạc hoá cho dữ liệu y tế liên tục (điểm yếu hiện tại).

---

*Báo cáo chi tiết kỹ thuật đầy đủ: xem file `BAO-CAO.md`.*
