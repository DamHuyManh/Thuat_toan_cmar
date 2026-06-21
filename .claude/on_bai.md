# KIẾN THỨC CƠ BẢN — Ôn tập cho đề tài CMAR

> File này giải thích lại TỪ ĐẦU mọi khái niệm dùng trong đề tài, đơn giản, dùng chung 1 ví dụ.
> **Ví dụ xuyên suốt**: bệnh viện có **100 bệnh nhân**, mỗi người có triệu chứng + nhãn bệnh.
> - Sốt cao: **30** người · Bị cúm: **40** người · Vừa sốt cao vừa cúm: **24** người.

---

## PHẦN 1 — KHÁI NIỆM NỀN

### 1.1. Phân lớp (Classification) là gì?
Cho dữ liệu đã biết nhãn (bệnh nhân + chẩn đoán) → **học** ra mô hình → dự đoán nhãn cho bệnh nhân MỚI. CMAR là một thuật toán phân lớp.

### 1.2. Luật kết hợp (Association Rule) là gì?
Dạng **ĐIỀU KIỆN → LỚP**:
> **sốt cao + ho → cúm**
- Vế trái ("sốt cao + ho") = **điều kiện** (antecedent).
- Vế phải ("cúm") = **lớp** (consequent).
CMAR = phân lớp **dựa trên nhiều luật kết hợp** (Classification based on Multiple Association Rules).

---

## PHẦN 2 — 3 CHỈ SỐ ĐO LUẬT (quan trọng nhất)

### 2.1. SUPPORT — luật phổ biến cỡ nào
```
            số mẫu khớp CẢ điều kiện VÀ lớp        24
 Support = ─────────────────────────────────  =  ────  =  0.24  (24%)
                   tổng số mẫu                     100
```
→ 24% bệnh nhân vừa sốt cao vừa cúm. **Dùng để**: loại luật quá hiếm (ngưỡng `minSup`).

### 2.2. CONFIDENCE — luật đáng tin cỡ nào
```
               số mẫu khớp CẢ điều kiện VÀ lớp        24
 Confidence = ────────────────────────────────  =  ────  =  0.80  (80%)
                 số mẫu khớp điều kiện               30
```
→ Trong 30 người sốt cao, 24 người bị cúm = 80%. **Dùng để**: SẮP XẾP luật, chọn top-5 (cách CSA).

**Nhớ phân biệt**: Support chia cho **tổng 100**; Confidence chia cho **30 người thỏa điều kiện**.

### 2.3. LIFT — luật mạnh hơn đoán mò mấy lần
```
        confidence       0.80
 Lift = ───────────  =  ──────  =  2.0
        tỉ lệ lớp chung   0.40        (tỉ lệ cúm chung = 40/100 = 0.40)
```
- Lift **> 1**: luật tốt (2.0 = mạnh gấp đôi đoán bừa).
- Lift **= 1**: vô dụng. Lift **< 1**: ngược.
**Dùng để**: làm SỨC NẶNG khi bỏ phiếu (luật lift cao → phiếu nặng hơn).

### 2.4. CHI-SQUARE (χ²) — mối liên hệ có thật hay ngẫu nhiên
χ² là **kiểm định thống kê** xem "điều kiện" và "lớp" có **liên hệ thật** không, hay chỉ trùng hợp ngẫu nhiên.
- χ² **lớn** → liên hệ mạnh, có ý nghĩa → giữ luật.
- χ² **nhỏ** → có thể chỉ ngẫu nhiên → bỏ luật.
- Ngưỡng: **p=0.05 → χ²≥3.84**; **p=0.01 → χ²≥6.63** (chặt hơn, đề tài dùng cái này).
**Dùng để**: lọc bỏ luật "may rủi". (Bài báo gốc còn dùng χ² làm sức nặng bỏ phiếu; đề tài đổi sang lift.)

---

## PHẦN 3 — KHAI PHÁ & LƯU TRỮ LUẬT

### 3.1. FP-Growth — khai phá luật
Thuật toán tìm mọi **tổ hợp triệu chứng hay đi cùng nhau** (frequent itemsets) một cách hiệu quả bằng cây FP-tree, rồi sinh ra luật. (Đề tài: chạy **song song** nhiều lõi.)

### 3.2. Bitmap inverted index — đếm support nhanh
Mỗi triệu chứng lưu thành **dãy bit** (1=có, 0=không) dài N:
```
sốt = 1 0 1 1 0 1 ...     ho = 1 1 1 0 0 1 ...
AND = 1 0 1 0 0 1 ...  → đếm bit 1 = support
```
CPU làm phép **AND 64 bit/lệnh** → nhanh ~64× so với quét từng người. **Đây là nguồn tốc độ chính.**

### 3.3. CR-tree — lưu luật để tìm nhanh
Lưu luật trong **cây tiền tố có băm** thay vì danh sách dài → khi dự đoán tìm luật khớp nhanh, không phải quét hết.

---

## PHẦN 4 — CẮT TỈA LUẬT (bỏ luật dở/thừa)

Khai phá ra hàng nghìn luật → phải lọc. 3 lớp:

### 4.1. Lọc χ²
Bỏ luật mà liên hệ chỉ là ngẫu nhiên (χ² < ngưỡng). Xem mục 2.4.

### 4.2. G2S Pruning (General-to-Specific) — bỏ luật thừa ⭐
Nếu luật A "sốt→cúm" (yếu) và luật B "sốt+ho→cúm" (cụ thể, mạnh hơn) cùng tồn tại → A bị B "che" → **bỏ A**.
- Kiểm tra "A có nằm trong B" = phép AND bitmap (nhanh).
- **Bài báo gốc bỏ qua bước này khi >10.000 luật** (vì chậm) → giữ luật thừa → kém. **Đề tài chạy ĐẦY ĐỦ** → luật sạch → chính xác hơn.

### 4.3. Coverage Pruning + Stratified
Bỏ luật trùng về "độ phủ" (database coverage). **Stratified** = giữ riêng top luật **MỖI lớp** trước khi cắt → lớp hiếm (ít bệnh nhân) không bị mất sạch luật.

---

## PHẦN 5 — TIỀN XỬ LÝ

### 5.1. Rời rạc hóa MDL (Fayyad-Irani)
Triệu chứng dạng SỐ ("nhiệt độ 38.7°C") → chia thành KHOẢNG ("cao/vừa/thấp") để làm luật. MDL tự tìm ranh giới cắt tối ưu.
**Quan trọng**: chỉ học ranh giới từ **dữ liệu train**, KHÔNG nhìn test → tránh "rò rỉ" (gian lận làm accuracy ảo cao).

---

## PHẦN 6 — DỰ ĐOÁN

### 6.1. Top-K voting
Mẫu mới khớp nhiều luật → chỉ giữ **K=5 luật mạnh nhất** (theo confidence) cho bỏ phiếu → nhanh + tránh nhiễu luật yếu.

### 6.2. Bỏ phiếu (voting)
5 luật bầu cho lớp của mình, **mỗi phiếu nặng = lift của luật**; cộng lift theo lớp; **lớp tổng cao nhất thắng**.
Ví dụ: cúm (3.0+1.8+1.2=6.0) vs cảm (2.5+2.0=4.5) → **chọn cúm**.

---

## PHẦN 7 — ĐÁNH GIÁ

### 7.1. 10-fold Cross-Validation
Chia dữ liệu thành 10 phần. Mỗi vòng: **9 phần train (90%) + 1 phần test (10%)**. Xoay 10 vòng để mỗi mẫu được test đúng 1 lần, lấy **trung bình**. Ổn định + công bằng (giống bài báo).

### 7.2. Ma trận nhầm lẫn → các chỉ số
Với 1 lớp: **TP** (đúng dương), **FP** (báo nhầm dương), **FN** (bỏ sót), **TN** (đúng âm).

| Chỉ số | Công thức | Nghĩa |
|---|---|---|
| **Accuracy** | (đoán đúng) / (tổng) | tỉ lệ đúng chung |
| **Precision** | TP / (TP+FP) | báo dương thì đúng bao nhiêu % |
| **Recall** | TP / (TP+FN) | các ca dương thật bắt được bao nhiêu % |
| **F1** | 2·P·R / (P+R) | trung bình hài hòa Precision & Recall |

- **Macro** = tính cho từng lớp rồi lấy trung bình (công bằng với lớp ít mẫu).
- Vì sao cần F1/Recall ngoài Accuracy? Bộ **mất cân bằng** (95% lớp A): đoán bừa "A" được accuracy 95% nhưng Recall lớp B = 0. F1/Recall phát hiện điều đó.

---

## PHẦN 8 — GHÉP LẠI: 1 CÂU CHUYỆN HOÀN CHỈNH

**Học (train):**
1. Rời rạc hóa số bằng **MDL** (chỉ từ train).
2. **FP-Growth song song** khai phá luật; đếm support bằng **bitmap AND**.
3. Cắt tỉa: **χ²** → **G2S đầy đủ** → **Stratified coverage**.
4. Sắp xếp luật theo **confidence** (CSA); lưu vào **CR-tree**.

**Dự đoán (test):**
5. Mẫu đến → CR-tree tìm luật khớp → lấy **top-5** (confidence) → bỏ phiếu **nặng theo lift** → lớp tổng lift cao nhất = kết quả.

**Đánh giá:**
6. **10-fold CV**, đo **Accuracy / F1 / Recall**.

**Kết quả đề tài:** nhanh ~3.6–4.2×, accuracy 85.23% (vượt bài báo 85.22%, thắng 17/26 bộ).

---

## PHẦN 9 — BẢNG TRA NHANH (cheat sheet)

| Thuật ngữ | 1 câu |
|---|---|
| Luật | điều kiện → lớp |
| Support | luật phổ biến cỡ nào (chia tổng) |
| Confidence | luật đáng tin cỡ nào (chia nhóm thỏa điều kiện) |
| Lift | mạnh hơn đoán mò mấy lần (=confidence/tỉ lệ lớp) |
| χ² | liên hệ thật hay ngẫu nhiên |
| FP-Growth | thuật toán khai phá luật |
| Bitmap | dãy bit, đếm support bằng AND (nhanh 64×) |
| CR-tree | cây lưu luật, tìm nhanh |
| G2S | bỏ luật tổng quát bị luật cụ thể che |
| Coverage/Stratified | bỏ luật trùng, giữ luật mỗi lớp |
| MDL | chia số liên tục thành khoảng (từ train) |
| Top-K | chỉ dùng 5 luật mạnh nhất khi dự đoán |
| Voting | bỏ phiếu nặng theo lift, lớp cao nhất thắng |
| 10-fold CV | 90% train/10% test, xoay 10 vòng |
| Accuracy/Precision/Recall/F1 | các cách đo độ chính xác |
