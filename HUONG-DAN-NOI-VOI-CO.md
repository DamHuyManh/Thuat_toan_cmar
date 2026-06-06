# HƯỚNG DẪN TRÌNH BÀY VỚI CÔ — Đề tài cải tiến CMAR

> File này hướng dẫn em **nói gì với cô** + **trả lời câu hỏi** thế nào. Đọc thuộc 3 con số chính + 5 cải tiến là đủ tự tin.

---

## 1. NÓI MỞ ĐẦU (30 giây — học thuộc)

> "Thưa cô, em cài đặt lại thuật toán **CMAR** (phân lớp dữ liệu bằng luật kết hợp, của Li-Han-Pei 2001) và **cải tiến** nó tập trung vào xử lý **dữ liệu mất cân bằng lớp**. Em thử nghiệm trên **26 bộ dữ liệu UCI chuẩn**, đánh giá bằng **10-fold cross-validation**. Kết quả: **F1 tăng 2.17%, Recall tăng 2.82%** so với CMAR gốc, và xếp **hạng 2/5** khi so với các thuật toán nổi tiếng (có kiểm định thống kê)."

---

## 2. BA CON SỐ PHẢI THUỘC

| Chỉ số | CMAR gốc | Của em | Tăng |
|---|---:|---:|---:|
| **Accuracy** | 85.22% | 85.47% | **+0.25%** |
| **F1 macro** | 80.67% | 82.84% | **+2.17%** |
| **Recall macro** | 80.66% | 83.48% | **+2.82%** |

→ **Câu chốt**: "Gain chính của em ở **F1 và Recall** — vì đây là chỉ số quan trọng cho dữ liệu mất cân bằng, mà Accuracy đơn thuần che giấu."

---

## 3. GIẢI THÍCH 5 CẢI TIẾN (mỗi cái 1 câu dễ hiểu)

1. **Stratified Coverage Pruning** — "Để dành luật cho lớp ít mẫu, không để bị loại hết."
2. **Cost-Sensitive Voting** — "Phiếu của lớp ít mẫu được tính nặng hơn cho công bằng."
3. **Bagging (10 mô hình)** — "Hỏi ý kiến 10 chuyên gia thay vì 1, rồi bỏ phiếu."
4. **Adaptive MinSup** — "Hạ tiêu chuẩn để tìm được luật cho lớp hiếm."
5. **MinSup Scale** — "Tìm nhiều luật hơn để ensemble đa dạng."

→ **Ý tưởng xuyên suốt**: "Cả 3 cải tiến chính chỉ **kích hoạt khi dữ liệu mất cân bằng** — dữ liệu cân bằng thì giữ nguyên thuật toán gốc. Nhờ vậy tăng F1/Recall cho data khó mà **không làm giảm** Accuracy data dễ."

---

## 4. NẾU CÔ HỎI... (câu hỏi thường gặp + cách trả lời)

**Hỏi: "Em có chắc kết quả thật không, không phải bịa số?"**
> "Dạ chắc ạ. Em chạy trên **26 bộ dữ liệu thật** từ kho UCI, mỗi lần chạy console in rõ 'real data (N dòng)'. Em đã **xóa toàn bộ code sinh dữ liệu giả** khỏi chương trình. Cố định seed=42 và chế độ deterministic nên **chạy lại ra y hệt**. Em cũng **không giấu 6 bộ thua** CMAR gốc."

**Hỏi: "Tại sao có 6 bộ thua?"**
> "Dạ chủ yếu là các bộ **y tế dữ liệu liên tục** (Diabetes, Heart, Pima) — biên giới giữa các lớp mờ nên rời rạc hóa kém; và vài bộ **quá ít mẫu** (Labor 57 mẫu, Zoo 101 mẫu) nên phương sai cao. Em báo cáo trung thực cả phần thua."

**Hỏi: "So với thuật toán mới hơn thì sao?"**
> "Em so với 5 thuật toán công bố (C4.5, CBA, CMAR, CPAR) bằng **kiểm định Friedman** — em xếp **hạng 2/5**, tương đương CPAR, có ý nghĩa thống kê (p < 0.05)."

**Hỏi: "Em đã thử cải tiến gì thêm chưa?"**
> "Dạ em thử thêm **Fuzzy CMAR** và **CAIM discretization** cho các bộ y tế. Chúng giúp riêng vài bộ nhưng **không cải thiện trung bình** — em ghi nhận đây là **negative result** (kết quả âm) để chứng minh đã khảo sát kỹ. Cấu hình tốt nhất vẫn là 5 cải tiến chính."

**Hỏi: "Accuracy chỉ tăng 0.25% thôi à?"**
> "Dạ đúng, Accuracy tăng ít vì nó bị **lớp đa số che**. Điểm mạnh thật sự là **F1 +2.17% và Recall +2.82%** — nghĩa là mô hình **không bỏ sót lớp ít mẫu** nữa, rất quan trọng cho bài toán thực tế như chẩn đoán bệnh (lớp 'bệnh' thường ít)."

---

## 5. CÂU KẾT (khi cô hỏi "đóng góp chính là gì")

> "Đóng góp chính của em là **mẫu thiết kế 'kích hoạt thông minh'** — xử lý mất cân bằng lớp ở **3 tầng** (khai phá luật, lọc luật, bỏ phiếu), chỉ can thiệp khi cần. Kết quả tăng F1 +2.17%, Recall +2.82% mà không hi sinh Accuracy, tương đương thuật toán hiện đại, và toàn bộ **trung thực, tái lập được**."

---

## 6. NHỮNG ĐIỂM CÔ SẼ ĐÁNH GIÁ CAO (nhấn mạnh khi nói)

- ✅ **Có kiểm định thống kê** (Friedman + Nemenyi) — ít đồ án sinh viên có.
- ✅ **Trung thực**: báo cáo cả phần thua, cả negative results (Fuzzy/CAIM).
- ✅ **Tái lập được**: seed cố định, deterministic, dữ liệu thật.
- ✅ **Có công thức rõ ràng** cho từng cải tiến.
- ✅ **26 bộ UCI chuẩn** — đúng như bài báo gốc dùng.

---

## 7. TÀI LIỆU MANG THEO

| File | Khi nào dùng |
|---|---|
| `BAO-CAO.md` | Báo cáo kỹ thuật đầy đủ (khi cô hỏi sâu) |
| `CAI-TIEN-MOI.md` | Giải thích 5 cải tiến dễ hiểu |
| File này | Kịch bản nói + trả lời câu hỏi |

---

## ⚠️ LƯU Ý KHI NÓI

- **Đừng** nói "Accuracy tăng mạnh" — nó chỉ tăng 0.25%. Hãy nói **"F1 và Recall tăng mạnh"**.
- **Đừng** giấu 6 bộ thua — cô sẽ đánh giá cao sự trung thực.
- **Nhấn**: "kích hoạt thông minh" (adaptive triggering) là điểm mới của em.
- Nếu cô hỏi cái gì không chắc → "Dạ em xin phép kiểm tra lại trong báo cáo ạ" (đừng bịa).
