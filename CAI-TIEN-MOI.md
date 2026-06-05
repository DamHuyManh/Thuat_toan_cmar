# CÁC CẢI TIẾN MỚI CHO THUẬT TOÁN CMAR

**Thuật toán gốc**: CMAR (Li, Han, Pei — ICDM 2001)
**Kết quả**: F1 **+2.19%**, Recall **+2.45%**, Accuracy **+0.35%** so với bài báo gốc

---

## CMAR LÀ GÌ? (hiểu trước khi nói cải tiến)

CMAR là thuật toán phân lớp **dựa trên LUẬT**. Nó học từ dữ liệu ra các luật dạng:

> **NẾU** (lương cao) VÀ (có nhà) **THÌ** → cho vay (độ tin cậy 95%)

Khi cần phân loại 1 trường hợp mới, CMAR tìm các luật khớp rồi cho chúng **"bỏ phiếu"** xem thuộc lớp nào.

**Vấn đề lớn nhất của CMAR**: khi dữ liệu **mất cân bằng** (1 lớp nhiều mẫu, 1 lớp ít mẫu) thì nó **thiên vị lớp nhiều mẫu**, bỏ sót lớp ít mẫu.

> **Ví dụ**: Dữ liệu khám bệnh có 94% người KHOẺ, 6% người BỆNH.
> CMAR gốc thường đoán "khoẻ" cho tất cả → đúng 94% (nghe cao) → nhưng **bỏ sót gần hết bệnh nhân** (nguy hiểm!).

→ **5 cải tiến của em đều nhằm sửa cái thiên vị này**, để mô hình phát hiện được cả lớp ít mẫu.

---

## BẢNG TÓM TẮT 5 CẢI TIẾN

| # | Tên | Nói đơn giản là gì | Tăng |
|---|---|---|---|
| 1 | Stratified Coverage | "Để dành luật cho lớp ít mẫu" | +0.14% F1 |
| 2 | Cost-Sensitive Voting | "Phiếu của lớp ít mẫu được tính nặng hơn" | +0.27% F1 |
| 3 | Bagging | "Hỏi ý kiến 10 chuyên gia thay vì 1" | +0.74% F1 |
| 4 | Adaptive MinSup | "Hạ tiêu chuẩn để tìm được luật cho lớp hiếm" | +0.81% F1 |
| 5 | MinSup Scale + Top-K | "Tìm nhiều luật rồi chọn lọc cái tốt nhất" | +0.23% F1 |

---

## CẢI TIẾN 1 — ĐỂ DÀNH LUẬT CHO LỚP ÍT MẪU
### (Stratified Coverage Pruning)

**Vấn đề**: Sau khi học, CMAR có quá nhiều luật → phải lọc bớt. Cách lọc của bài gốc giống như **"ai mạnh thì giữ"** → lớp nhiều mẫu (có nhiều luật mạnh) được giữ hết, lớp ít mẫu **bị loại sạch luật**.

**Ví dụ dễ hiểu**: Như tuyển đội bóng mà chỉ chọn "ai ghi nhiều bàn nhất" → toàn tiền đạo, **không còn thủ môn**. Khi gặp tình huống cần thủ môn thì thua.

**Cải tiến của em**: Trước khi lọc, **để dành 10 luật tốt nhất cho MỖI lớp**. 
→ Giống như tuyển đội bóng phải có đủ thủ môn, hậu vệ, tiền đạo.

**Kết quả**: Lớp ít mẫu vẫn có luật → không bị bỏ sót.

---

## CẢI TIẾN 2 — PHIẾU CỦA LỚP ÍT MẪU TÍNH NẶNG HƠN
### (Cost-Sensitive Voting)

**Vấn đề**: Khi các luật "bỏ phiếu", lớp nhiều mẫu có nhiều luật hơn → tổng phiếu lúc nào cũng áp đảo → lớp ít mẫu luôn thua.

**Ví dụ dễ hiểu**: Như bầu cử mà 1 phường có 1 triệu dân, phường kia chỉ 1 nghìn dân → phường nhỏ không bao giờ có tiếng nói. Giải pháp công bằng: **cho phiếu của phường nhỏ trọng số cao hơn**.

**Cải tiến của em** — công thức:
```
Nếu dữ liệu mất cân bằng (lớp lớn / lớp nhỏ > 1.5 lần):
    Điểm của lớp = Điểm × (tổng số mẫu / số mẫu của lớp đó)
```
→ Lớp càng ít mẫu, phiếu càng được nhân nặng.

**Ví dụ số cụ thể** — bộ Sick (94% khoẻ, 6% bệnh):
- Lớp "bệnh" chỉ 6% → phiếu được **nhân 16.7 lần**.
- Nhờ vậy khi có dấu hiệu bệnh rõ → mô hình dám kết luận "bệnh" → **phát hiện được bệnh nhân**.

**Điểm thông minh**: Chỉ áp dụng khi dữ liệu mất cân bằng. Dữ liệu cân bằng (như hoa Iris 3 loại đều nhau) thì **giữ nguyên** bài gốc → không làm hỏng.

---

## CẢI TIẾN 3 — HỎI 10 CHUYÊN GIA THAY VÌ 1
### (Bagging)

**Vấn đề**: 1 mô hình CMAR đơn lẻ dễ bị "may rủi" — học trúng phần dữ liệu này thì tốt, trúng phần kia thì kém.

**Ví dụ dễ hiểu**: Hỏi 1 bác sĩ dễ sai. Hỏi **10 bác sĩ** rồi lấy ý kiến đa số → đáng tin hơn.

**Cải tiến của em**:
```
- Tạo 10 bộ dữ liệu con (mỗi bộ lấy ngẫu nhiên từ dữ liệu gốc)
- Huấn luyện 10 mô hình CMAR riêng, mỗi mô hình trên 1 bộ
- Khi dự đoán: cả 10 cùng bỏ phiếu, ai giỏi hơn thì phiếu nặng hơn
```

**🔑 PHÁT HIỆN MỚI (quan trọng)**: Em thử bỏ bớt thuộc tính (giống thuật toán Random Forest nổi tiếng) thì **KẾT QUẢ TỆ ĐI** (giảm 3.3%).
- **Lý do**: CMAR học các "mẫu kết hợp" giữa nhiều thuộc tính. Bỏ bớt thuộc tính = phá vỡ mẫu.
- → CMAR phải **dùng đủ tất cả thuộc tính**. Đây là điểm khác biệt em phát hiện được (bác bỏ 1 nghiên cứu năm 2018).

---

## CẢI TIẾN 4 — HẠ TIÊU CHUẨN ĐỂ TÌM LUẬT CHO LỚP HIẾM
### (Adaptive MinSup)

**Vấn đề**: CMAR chỉ giữ luật xuất hiện đủ nhiều lần (đủ "support"). Lớp ít mẫu thì luật của nó xuất hiện ít → **không đạt tiêu chuẩn → bị bỏ → không có luật để nhận diện lớp hiếm**.

**Ví dụ dễ hiểu**: Quy định "món ăn phải bán được 100 phần/ngày mới giữ trong menu". Món phổ biến thì đạt, nhưng **món đặc sản hiếm** chỉ bán 20 phần → bị loại, dù khách cần.

**Cải tiến của em**: **Hạ tiêu chuẩn xuống tuỳ theo mức độ mất cân bằng**:
```
Tiêu chuẩn mới = Tiêu chuẩn gốc / căn bậc hai(mức mất cân bằng)
```

**Ví dụ**:
| Bộ dữ liệu | Mức mất cân bằng | Hạ tiêu chuẩn |
|---|---:|---:|
| Iris (cân bằng) | 1 lần | giữ nguyên |
| Diabetes | 1.9 lần | hạ 1.4× |
| Sick | 15.7 lần | hạ 4× |

→ Lớp càng hiếm, tiêu chuẩn càng hạ → tìm được luật cho nó.

---

## CẢI TIẾN 5 — TÌM NHIỀU LUẬT RỒI CHỌN LỌC
### (MinSup Scale + Top-K)

Gồm 2 việc làm cùng nhau:

**(a) Tìm nhiều luật hơn**: Hạ tiêu chuẩn support thêm (×0.3) → mô hình tìm được **gấp 3 lần** số luật.
→ Lý do: 10 chuyên gia (Bagging) cần nhiều ý kiến đa dạng để bỏ phiếu tốt.

**(b) Chọn lọc khi bỏ phiếu**: Tuy tìm nhiều luật, nhưng khi bỏ phiếu chỉ lấy **10 luật mạnh nhất mỗi lớp** → loại bỏ luật yếu gây nhiễu.

**Ví dụ dễ hiểu**: Tuyển nhiều ứng viên (tìm nhiều luật) → nhưng phỏng vấn chỉ chọn top 10 giỏi nhất (Top-K) → chất lượng cao.

**Lưu ý**: Hai việc này phải đi cùng — tìm nhiều rồi mới có cái để chọn lọc.

---

## Ý TƯỞNG CHUNG XUYÊN SUỐT
### "Chỉ can thiệp khi cần"

3 cải tiến chính (1, 2, 4) đều theo **một nguyên tắc thông minh**:

> **Chỉ ra tay khi dữ liệu MẤT CÂN BẰNG.**
> **Dữ liệu cân bằng thì giữ nguyên bài gốc, không động vào.**

**Tại sao hay?**
- Dữ liệu dễ (cân bằng) → không đụng → **không làm hỏng** (Accuracy giữ nguyên).
- Dữ liệu khó (mất cân bằng) → ra tay → **cải thiện mạnh** (F1, Recall tăng).

→ Giống bác sĩ giỏi: bệnh nhẹ thì không kê thuốc mạnh, bệnh nặng mới can thiệp.

Đây là **điểm mới** của em: xử lý mất cân bằng ở **3 giai đoạn** (tìm luật, lọc luật, bỏ phiếu) với cùng triết lý này.

---

## KẾT QUẢ CUỐI CÙNG

**So với bài báo gốc CMAR 2001**:

| Chỉ số | Bài gốc | Của em | Tăng |
|---|---:|---:|---:|
| Accuracy (độ chính xác chung) | 85.22% | **85.57%** | **+0.35%** |
| F1-macro (cân bằng các lớp) | 80.67% | **82.86%** | **+2.19%** |
| Recall-macro (không bỏ sót lớp) | 80.94% | **83.39%** | **+2.45%** |

> **Tăng mạnh nhất ở F1 và Recall** — đúng mục tiêu: không còn bỏ sót lớp ít mẫu.

**Thắng đậm nhất** (các bộ nhiều lớp + mất cân bằng):

| Bộ dữ liệu | Bài gốc | Của em | Tăng |
|---|---:|---:|---:|
| Hepatitis (viêm gan) | 80.5% | 84.21% | **+3.71%** |
| Auto (ô tô) | 78.1% | 81.53% | **+3.43%** |
| Vehicle (xe) | 68.8% | 71.15% | **+2.35%** |

**So với 5 thuật toán nổi tiếng** (kiểm định thống kê Friedman, trên 24 bộ dữ liệu chung):
→ Em xếp **hạng 2/6**, **tương đương** thuật toán mới nhất ECBA-EX (năm 2018).

```
Hạng 1: ECBA-EX (2018) — mới nhất
Hạng 2: CỦA EM          ⭐
Hạng 3: CPAR (2003)
Hạng 4: CMAR (2001)     ← bài gốc em cải tiến
Hạng 5: CBA (1998)
Hạng 6: C4.5 (1993)
```

→ Em cải tiến bài gốc CMAR (hạng 4) **vượt lên hạng 2**, ngang tầm thuật toán mới nhất.
