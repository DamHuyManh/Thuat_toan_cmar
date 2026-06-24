# CHI TIẾT: CÁCH CẢI TIẾN SO VỚI CÁCH CŨ

> **Đề tài**: Cải tiến hiệu năng thuật toán phân lớp luật kết hợp CMAR (Li, Han, Pei — ICDM 2001).
> File này phân tích **TỪNG cải tiến**: cách CŨ (nhược điểm) → cách MỚI (ưu điểm) + ví dụ + công thức.
> *Ví dụ xuyên suốt*: 100 bệnh nhân — sốt cao = 30, ho = nhiều người, cúm = 40, vừa sốt vừa cúm = 24.

---

## BỐI CẢNH: CMAR hoạt động 2 giai đoạn

1. **HỌC (train)**: từ dữ liệu → khai phá hàng nghìn **luật** (vd *sốt+ho → cúm*) → cắt tỉa bỏ luật dở → lưu lại.
2. **DỰ ĐOÁN (test)**: mẫu mới đến → tìm luật khớp → các luật **bỏ phiếu** chọn lớp.

CMAR gốc làm **chậm** (đếm/lọc tốn kém) và **lọc luật ẩu** (bỏ qua bước khi quá nhiều luật). Khóa luận cải tiến **8 điểm**, chia 2 nhóm: **TỐC ĐỘ** (1–3) và **ĐỘ CHÍNH XÁC** (4–8).

---

# NHÓM A — CẢI TIẾN TỐC ĐỘ

## ① ĐẾM SUPPORT — cải tiến quan trọng nhất

**Việc cần làm**: liên tục đếm "bao nhiêu mẫu thỏa 1 tổ hợp" (vd "sốt VÀ ho") — làm HÀNG TRIỆU lần.

**❌ Cách CŨ — quét list O(N·L)**
Duyệt từng bệnh nhân: BN1 có sốt? có ho? → BN2 → ... → BN_N.
- N mẫu × L thuộc tính = rất nhiều thao tác → **chậm nhất trong thuật toán**.

**✅ Cách MỚI — Bitmap AND (64 bit/lệnh)**
Ghi mỗi triệu chứng thành dãy bit (1=có, 0=không), mỗi bit là 1 mẫu:
```
sốt = 1 0 1 1 0 1 ...      ho = 1 1 1 0 0 1 ...
AND = 1 0 1 0 0 1 ...   → đếm bit 1 = support
```
Máy lưu **64 bit trong 1 ô (long)** và làm phép AND **cả 64 bit trong 1 LỆNH CPU** → xử lý **64 mẫu cùng lúc**.
- 1000 mẫu: cách cũ ~1000 thao tác → bitmap ~16 lệnh → **nhanh ~64×**.

**Công thức**: `support(A∩B) = đếm_bit_1( bitmap[A] AND bitmap[B] )`

---

## ② KHAI PHÁ LUẬT — chia việc nhiều lõi

**Việc cần làm**: đào ra hàng nghìn tổ hợp triệu chứng (thuật toán FP-Growth xây cây rồi đào).

**❌ Cách CŨ — tuần tự**: 1 lõi CPU làm hết, đào từng nhánh một → chậm.

**✅ Cách MỚI — song song (N≥200)**: mỗi nhánh gốc của cây độc lập → **chia cho nhiều lõi CPU làm cùng lúc** (ForkJoinPool).
- Như 4 người dọn 4 phòng thay vì 1 người dọn cả 4.
- Chỉ bật khi bộ **≥200 mẫu** (bộ nhỏ làm 1 lõi còn nhanh hơn vì khỏi tốn chi phí tạo luồng).
- Có **tie-break tất định** → dù song song, kết quả vẫn giống hệt mọi lần chạy.

---

## ③ LƯU & TÌM LUẬT — CR-tree

**Việc cần làm**: khi 1 mẫu đến, tìm nhanh "luật nào khớp".

**❌ Cách CŨ — List**: luật để trong danh sách dài → tìm phải **quét hết** từ đầu đến cuối.

**✅ Cách MỚI — CR-tree + bitmap antecedent**:
- **CR-tree**: lưu luật trong **cây tiền tố có băm** (như sắp sách theo kệ có nhãn) → đi theo cây lấy ngay luật khớp.
- **Bitmap antecedent**: phần điều kiện của luật lưu dạng bit → kiểm tra khớp = **1 phép AND** (thay vì so từng triệu chứng).

---

# NHÓM B — CẢI TIẾN ĐỘ CHÍNH XÁC

## ④ CẮT TỈA G2S — chạy ĐẦY ĐỦ ⭐ (điểm hay nhất)

**Việc cần làm**: bỏ luật **thừa**. Luật yếu "sốt → cúm" bị luật mạnh hơn "sốt+ho → cúm" **che** → nên bỏ luật yếu.

**❌ Cách CŨ — bỏ qua khi >10K luật**
So từng cặp luật rất chậm (O(L²)) → khi sinh **>10.000 luật thì BỎ QUA** bước này → **giữ luôn luật rác** → nhiễu → **đoán sai nhiều hơn**.

**✅ Cách MỚI — bitmap AND, chạy đầy đủ**
Kiểm tra "luật A có nằm trong luật B" = **1 phép AND bitmap** → đủ nhanh để **chạy HẾT** dù nhiều luật → luật **sạch** → đoán đúng hơn.

**Công thức**: `A ⊆ B  ⟺  ( bitmap[A] AND bitmap[B] ) == bitmap[A]`

→ **Đây là cầu nối tốc độ ↔ chính xác**: nhờ ① đếm nhanh, mới có thời gian làm kỹ bước này. Accuracy tăng mạnh ở bộ nhiều luật: **Horse +9%, German +9%, Sonar +7%**.

---

## ⑤ TRỌNG SỐ BỎ PHIẾU — Lift thay χ²

**Việc cần làm**: khi 5 luật bỏ phiếu, mỗi luật có "sức nặng" khác nhau.

**❌ Cách CŨ — χ² chuẩn hóa**: sức nặng = chỉ số χ². Nhược: χ² bị **méo bởi độ phổ biến** (luật phổ biến thường χ² cao dù tương quan chỉ trung bình).

**✅ Cách MỚI — Lift**: sức nặng = **lift** (luật đoán đúng gấp mấy lần ngẫu nhiên) → đo tương quan **trực tiếp**, không bị méo.
```
Lift = Confidence / tỉ lệ lớp = 0.80 / 0.40 = 2.0
Bỏ phiếu: score(lớp c) = Σ lift(các luật khớp dự đoán c) → chọn c lớn nhất
```
→ Em đã test: lift > χ² (+0.21% accuracy).

---

## ⑥ CẮT TỈA COVERAGE — Stratified

**Việc cần làm**: bỏ bớt luật **trùng độ phủ** (nhiều luật cùng phủ 1 nhóm mẫu).

**❌ Cách CŨ — DCP cơ bản**: cắt chung → lớp **hiếm** (ít mẫu) dễ bị **mất sạch luật** → không đoán được lớp đó.

**✅ Cách MỚI — Stratified**: **giữ riêng top 10 luật mạnh nhất MỖI lớp** trước khi cắt → lớp hiếm chắc chắn còn luật.
→ Như chia học bổng đảm bảo mỗi lớp đều có suất.

---

## ⑦ NGƯỠNG χ² — chặt hơn (p=0.01)

**Việc cần làm**: lọc luật có liên hệ **thật** hay chỉ **ngẫu nhiên** (dùng kiểm định χ²).

**❌ Cách CŨ — p=0.05** (χ²≥3.84): dễ, lọt nhiều luật yếu/may rủi.

**✅ Cách MỚI — p=0.01** (χ²≥6.63): **chặt hơn** → chỉ giữ luật liên hệ rõ ràng → bớt luật rác.

**Công thức**: `χ² = Σ (Quan_sát − Kỳ_vọng)² / Kỳ_vọng`

---

## ⑧ POOL LUẬT — giàu hơn (minSup × 0.5)

**minSup** = ngưỡng support tối thiểu (luật phải phổ biến tới mức này mới giữ).

**❌ Cách CŨ — minSup theo paper**: ngưỡng cao → **ít luật ứng viên**.

**✅ Cách MỚI — minSup × 0.5**: hạ ngưỡng còn một nửa → **nhiều luật ứng viên hơn** → khi chọn top-5 có nhiều luật chất để chọn.
→ Như tuyển 5 người: 100 ứng viên chọn được người giỏi hơn so với chỉ 10 ứng viên.
→ Làm được nhờ **bitmap đủ nhanh** để xử lý lượng luật lớn.

---

## ⑨ GIỮ NGUYÊN của bản gốc: SẮP XẾP LUẬT (CSA)

Khi chọn top-5, sắp xếp theo **confidence → support → độ dài** (CSA gốc). Em đã **thử 7 cách sắp xếp khác** (lift, χ², WRA...) — **tất cả đều kém hơn** → giữ cách gốc.
→ *Trung thực: không phải mọi thứ đều đổi; cái gì gốc đã tốt nhất thì giữ.*

---

# BẢNG TỔNG HỢP: CŨ vs MỚI

| # | Khâu | CÁCH CŨ | CÁCH MỚI | Loại |
|---|---|---|---|---|
| 1 | Đếm support | quét list O(N·L) | **bitmap AND** (64 bit/lệnh) | ⚡ tốc độ |
| 2 | Khai phá | tuần tự 1 lõi | **song song** (N≥200) | ⚡ tốc độ |
| 3 | Lưu/tìm luật | List, quét hết | **CR-tree + bitmap** | ⚡ tốc độ |
| 4 | Cắt tỉa G2S | **bỏ qua** khi >10K luật | **chạy đầy đủ** (bitmap) | ⚡+🎯 |
| 5 | Bỏ phiếu | χ² chuẩn hóa | **Lift** | 🎯 chính xác |
| 6 | Coverage | DCP cơ bản | **Stratified** | 🎯 chính xác |
| 7 | Ngưỡng χ² | p=0.05 | **p=0.01** | 🎯 chính xác |
| 8 | Pool luật | minSup paper | **minSup × 0.5** | 🎯 chính xác |
| 9 | Sắp xếp luật | CSA | **CSA (giữ nguyên)** | đã tối ưu |

---

# KẾT QUẢ (chạy thật, 26 bộ UCI, 10-fold CV, topK=5)

| Chỉ số | Cách CŨ (gốc) | Cách MỚI (cải tiến) | Bài báo CMAR |
|---|---:|---:|---:|
| Accuracy | 83–84% | **85.23%** | 85.22% |
| Tốc độ | 1× | **~4× nhanh hơn** | — |
| F1 / Recall | thấp hơn | cao hơn | — |

Kiểm chứng thêm trên **Mushroom** (8124 mẫu, ngoài paper): **nhanh 7.14×**, accuracy 99.93% (giữ nguyên).

---

# CÂU CHỐT (nhớ cái này là đủ)

> **Nhờ ĐẾM NHANH hơn bằng bitmap (①), bản cải tiến có thời gian LỌC LUẬT KỸ hơn — chạy đầy đủ G2S (④) mà bản gốc phải bỏ qua → vừa NHANH ~4× VỪA CHÍNH XÁC hơn (vượt cả bài báo). Cộng thêm bỏ phiếu lift (⑤), bảo vệ lớp ít (⑥), lọc χ² chặt (⑦), pool luật giàu (⑧).**

Tóm gọn 2 nhóm:
- **TỐC ĐỘ** ⚡: bitmap AND + song song + CR-tree.
- **CHÍNH XÁC** 🎯: full G2S + lift + stratified + χ² chặt + pool giàu.
