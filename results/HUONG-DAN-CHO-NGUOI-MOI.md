# 📘 CMAR — Hiểu thuật toán & Cách em đang làm

> File này giúp người mới: (1) **hiểu được CMAR đang làm gì**, (2) **biết em đã thử những hướng nào**, hướng nào **thành công**, hướng nào **thất bại**.

---

## 📑 Mục lục

1. [Bài toán phân lớp — em đang giải gì?](#1-bài-toán-phân-lớp--em-đang-giải-gì)
2. [Luật phân lớp là gì?](#2-luật-phân-lớp-là-gì)
3. [Bốn thước đo quan trọng của một luật](#3-bốn-thước-đo-quan-trọng-của-một-luật)
4. [CMAR — pipeline 4 bước](#4-cmar--pipeline-4-bước)
5. [Hành trình thử nghiệm](#5-hành-trình-thử-nghiệm)
6. [Cấu hình hiện tại em chốt](#6-cấu-hình-hiện-tại-em-chốt)

---

## 1. Bài toán phân lớp — em đang giải gì?

**Phân lớp** = cho máy học từ dữ liệu đã biết nhãn, để **đoán nhãn** của dữ liệu mới.

Em dùng **26 bộ dữ liệu UCI** chuẩn (giống y bài báo Li-Han-Pei 2001): Iris, Wine, Sonar, Hepatitis, Auto, German, Pima, ...

Đánh giá bằng **10-fold cross-validation** — chia dữ liệu thành 10 phần, dùng 9 để học, 1 để test, lặp 10 lần, lấy trung bình.

---

## 2. Luật phân lớp là gì?

CMAR sinh ra các luật **IF...THEN...** rồi dùng chúng để đoán nhãn.

**Ví dụ**: với dữ liệu hoa Iris
```
IF (cánh hẹp) AND (đài hẹp)  →  loài = Setosa
IF (cánh rộng) AND (đài dài)  →  loài = Virginica
```

Mỗi luật có 2 phần:
- **Tiền đề (antecedent)**: tập điều kiện bên trái
- **Lớp (class)**: nhãn bên phải

Đặt tên gọi tắt: **CAR** = Class Association Rule.

---

## 3. Bốn thước đo quan trọng của một luật

Cho 1 luật `X → c` (X = tiền đề, c = lớp):

### 📏 Support — luật phổ biến cỡ nào?
> Có bao nhiêu % mẫu thỏa cả X và c?

Cao → luật áp dụng được cho nhiều mẫu (đáng tin về thống kê).

### 📏 Confidence — luật chính xác cỡ nào?
> Trong những mẫu có X, bao nhiêu % thực sự thuộc lớp c?

Cao → khi thấy X thì đoán c rất khó sai.

### 📏 Chi-square (χ²) — luật có ý nghĩa thống kê không?
> X và c có liên quan **thật sự** không, hay chỉ trùng hợp?

- χ² ≥ 3.841 → có ý nghĩa (p < 0.05) → giữ
- χ² < 3.841 → có thể do may rủi → **vứt**

### 📏 Lift — X có "kéo" c lên không?
> Khả năng c xuất hiện khi có X **so với** khi không có X

- Lift > 1 → X *tăng* khả năng c → luật tốt
- Lift = 1 → X và c **độc lập** → luật vô nghĩa
- Lift < 1 → X *giảm* khả năng c → luật xấu

**So sánh nhanh**:
| Thước đo | Trả lời câu hỏi | Dùng để |
|---|---|---|
| Support | "Bao nhiêu mẫu?" | Bỏ luật hiếm |
| Confidence | "Đúng bao nhiêu %?" | Sắp xếp ưu tiên luật |
| χ² | "Có thật sự liên quan?" | Lọc nhiễu thống kê |
| Lift | "X kéo c lên hay đè c xuống?" | Lọc luật yếu / vote nâng cao |

---

## 4. CMAR — pipeline 4 bước

```
   📊 Dữ liệu huấn luyện (có nhãn)
          │
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 1 — KHAI PHÁ LUẬT                ║
   ║   Sinh tất cả luật có support đủ lớn  ║
   ╚═══════════════════════════════════════╝
          │ ~100,000 luật ứng viên
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 2 — TỈA LUẬT (3 tầng)            ║
   ║   ① Tỉa theo χ²    (lọc nhiễu)        ║
   ║   ② Tỉa tổng→đặc   (lọc dư thừa)      ║
   ║   ③ Tỉa theo phủ   (lọc trùng phủ)    ║
   ╚═══════════════════════════════════════╝
          │ còn ~100–500 luật
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 3 — TỔ CHỨC LUẬT THÀNH CÂY       ║
   ║   để tra cứu cực nhanh khi predict    ║
   ╚═══════════════════════════════════════╝
          │
          ▼
   📥 Mẫu test ──▶ ╔═════════════════════════════╗
                    ║ BƯỚC 4 — BỎ PHIẾU CÓ TRỌNG SỐ║
                    ║   Tìm luật khớp + vote class ║
                    ╚═════════════════════════════╝
                                 │
                                 ▼
                          🏷️ Nhãn dự đoán
```

### 📌 Bước 1 — Khai phá luật

Ý tưởng: tìm những **tập điều kiện thường xuất hiện cùng nhau** + nhãn. Mỗi tập như vậy = 1 luật ứng viên.

Vấn đề: với 20 thuộc tính có hơn **1 triệu tập con** khả dĩ. Không thể thử hết.

Giải pháp: **FP-Growth** — nén dữ liệu thành cây, rồi sinh luật trực tiếp từ cây, không sinh thừa.

→ Sau bước này có **~100,000 luật ứng viên** (đa số là rác).

### 📌 Bước 2 — Tỉa luật (3 tầng)

#### Tầng ① — Tỉa theo χ²
Loại 3 loại rác:
- **Confidence quá thấp** (< 50%) → không đáng tin
- **χ² quá nhỏ** (< 3.841) → liên quan có thể do may rủi
- **Confidence ≤ tỉ lệ ngẫu nhiên của lớp đó** → đoán không hơn random

→ Còn ~10% số luật.

#### Tầng ② — Tỉa "tổng quát → đặc biệt" (General-to-Specific)
Loại luật đặc biệt **dư thừa** khi đã có luật tổng quát mạnh hơn:

```
Luật A: {X1}      → c    [χ² = 50]   ← tổng quát + mạnh ✅
Luật B: {X1, X2}  → c    [χ² = 40]   ← đặc biệt + yếu hơn → VỨT
```

B chứa nhiều điều kiện hơn A nhưng lại yếu hơn → chỉ là "phình to vô ích" của A.

→ Còn ~1% số luật.

#### Tầng ③ — Tỉa theo độ phủ (Database Coverage) ⭐
**Quan trọng nhất** — chỉ giữ luật **đóng góp mẫu mới**.

Cách làm:
1. Sắp luật theo thứ tự: `confidence DESC → support DESC → ngắn trước`
2. Quét từ luật mạnh nhất xuống:
   - Mỗi luật phủ những mẫu nào?
   - Nếu có ≥ 1 mẫu **chưa từng được phủ** → **giữ**
   - Đánh dấu các mẫu đó là "đã được phủ"
   - Mỗi mẫu chỉ cho phép được phủ tối đa **4 lần** (tham số δ=4 của paper)
3. Dừng khi mọi mẫu được phủ đủ, hoặc hết luật

→ Còn **~100–500 luật cuối**. Đây là model.

### 📌 Bước 3 — Tổ chức luật thành cây

Khi có 1 mẫu test, phải tìm "luật nào khớp với mẫu này". Nếu duyệt tuyến tính cả 500 luật → chậm.

Giải pháp: tổ chức luật thành **CR-Tree** (cây tiền tố theo `lớp + item đầu`). Tra cứu chỉ duyệt nhánh đúng → **nhanh ~20 lần**.

### 📌 Bước 4 — Bỏ phiếu có trọng số

Khi có mẫu test mới:
```
1. Tìm tất cả luật khớp (tiền đề ⊆ điều kiện của mẫu)
2. Nếu không có luật nào khớp → trả về lớp đông nhất (default class)
3. Nếu các luật mạnh nhất đều đoán cùng 1 lớp → trả lớp đó ngay
4. Ngược lại:
   - Nhóm luật theo lớp
   - Mỗi luật góp 1 phiếu nặng = χ² chuẩn-hóa của nó
   - Lớp nào TỔNG phiếu cao nhất → THẮNG
```

**Tại sao dùng χ² làm trọng số?** Vì χ² đo "mức độ liên quan thống kê" của luật → luật liên quan chặt được vote nặng hơn luật yếu.

---

## 5. Hành trình thử nghiệm

> Em đã thử **NHIỀU hướng** khác nhau. Có cái thành công, có cái thất bại. Phần này quan trọng nhất.

### 🎯 Hai trục thử nghiệm

Em chia thử nghiệm thành 2 trục:
- **Trục A — Tối ưu hiệu năng**: làm thuật toán *chạy nhanh hơn*, **không đổi công thức**
- **Trục B — Đổi công thức**: thay đổi cách sort, cách lọc, cách vote

### 🟢 TRỤC A — Tối ưu hiệu năng (THÀNH CÔNG hoàn toàn)

Đây là phần em gọi là **"Improved"**. Bản chất: làm như compiler bật `-O3`.

| # | Hướng | Ý tưởng | Kết quả |
|:---:|---|---|---|
| 1 | **Đếm support bằng phép AND bit** | Thay vì quét từng giao dịch để đếm, em mã hóa mỗi "item xuất hiện ở giao dịch nào" thành 1 dải bit. Giao 2 dải bit (AND) ra ngay support. | ✅ Mining nhanh **~10×** |
| 2 | **Chia sẻ kết quả "khớp" giữa các tầng tỉa** | Tầng χ² đã tính "luật này phủ mẫu nào" rồi → tầng coverage dùng lại, không tính lại. | ✅ Tỉa nhanh **~2×** |
| 3 | **Subset bằng dấu vân tay bitmap** | Tầng tỉa "tổng→đặc" cần check "X ⊆ Y". Em mã hóa X, Y thành dải bit → check chỉ bằng `(X AND NOT Y) = 0`. | ✅ Gỡ được hack "bỏ qua tầng này khi >10K luật" của baseline |
| 4 | **Index luật theo (lớp, item đầu, độ dài)** | Khi check subset, chỉ so cặp **có thể** subset (cùng lớp, cùng item đầu, đủ ngắn). | ✅ Giảm số phép so từ O(n²) → gần O(n) |
| 5 | **Hash CR-Tree** | Khi predict, tra luật khớp theo `(lớp, item đầu)` thay vì duyệt cả cây. | ✅ Predict nhanh **~20×** |
| 6 | **Song song hóa mining theo item top-level** | Mỗi CPU core khai phá một nhánh. | ✅ Tăng ~2× trên máy 4 core |
| 7 | **Tie-breaker đầy đủ khi sort** | Vì song song nên thứ tự luật không ổn định → em thêm tiêu chí phụ (item-theo-item, classLabel) để sort **luôn cho kết quả y hệt**. | ✅ 2 lần chạy = 2 kết quả giống nhau từng số |
| 8 | **Tái sử dụng buffer (ThreadLocal)** | Predict cấp phát buffer mới mỗi mẫu → GC nhiều. Em giữ buffer ThreadLocal, dùng lại. | ✅ Giảm 30% thời gian predict |
| 9 | **Học rời rạc hóa CHỈ từ train fold** | Trước đây học cut-points từ cả dataset → bị "leak" thông tin từ test. Em sửa lại để chỉ học từ train. | ✅ Đúng paper, accuracy ổn định hơn |

**Kết quả tổng**: Train **nhanh 5.28×**, accuracy **tăng nhẹ +0.8%** so với baseline.

> 💡 **Quan trọng**: Trục A KHÔNG đổi công thức. CMAR vẫn là CMAR. Em chỉ làm nó chạy nhanh hơn và ổn định hơn.

### 🟡 TRỤC B — Đổi công thức (HỖN HỢP, nhiều cái thất bại)

Đây là phần em **thử nghiệm thêm** xem có cải thiện accuracy không. Đa số **thất bại** hoặc **không hơn baseline**.

#### B1. Đổi trọng số vote: χ² → Lift
**Ý tưởng**: Lift đo "X có thật sự kéo c lên không" — có thể chính xác hơn χ² trên dữ liệu nhiễu.

**Kết quả**: 85.2% (= paper).
- Trung bình **bằng** chi² (không hơn không kém)
- **Thắng rõ trên dataset chiều cao**: Hepatitis +1.5%, Auto +1.1%, Iono +0.6%
- **Thua nhẹ trên dataset cân bằng**: Lymphography −1.4%, Horse −1.3%

→ 🟢 **Đáng giữ làm tùy chọn** cho dataset nhiễu, không thay thế χ² làm mặc định.

#### B2. Đổi trọng số vote: χ² → HM (harmonic mean)
**Ý tưởng**: HM là trung bình điều hòa của support và confidence (giống F1-score).

**Kết quả**: 85.2% — tương tự Lift, không hơn không kém.

→ 🟢 Tùy chọn được.

#### B3. Lọc thêm bằng Lift ≥ 1
**Ý tưởng**: Vứt mọi luật có Lift < 1 (luật xấu) ngay từ đầu.

**Kết quả**: 85.3% — **giống y hệt mặc định**.

→ ⚪ Vô tác dụng. Vì χ² đã lọc chặt hơn rồi — luật có Lift < 1 sẽ tự rớt ở tầng χ².

#### B4. Sắp luật theo Lift trước, rồi mới confidence ❌
**Ý tưởng**: "Lift đo tương quan tốt hơn confidence → sort theo Lift trước".

**Kết quả**: 82.3% — **giảm ~3%** so với mặc định.

→ 🔴 **Thất bại**. Lý do: tầng tỉa coverage (tầng ③) **phụ thuộc vào thứ tự sort**. Đổi sort → tầng coverage giữ lại tập luật khác → kết quả phân lớp tệ đi. **Bài học**: không nên đụng vào sort của paper.

#### B5. Sắp luật theo HM trước ❌❌
**Tương tự B4 nhưng dùng HM**.

**Kết quả**: 76.2% — **thảm họa, giảm 9%**.

→ 🔴 **Thất bại nặng**. Cùng lý do với B4 nhưng mạnh hơn vì HM khác xa với CMAR ordering.

#### B6. Sắp luật theo "dài → ngắn" thay vì "ngắn → dài" ❌
**Ý tưởng**: Luật dài "đặc biệt" hơn → chính xác hơn → nên ưu tiên.

**Kết quả**: giảm 2–3%.

→ 🔴 **Thất bại**. Paper sort "ngắn trước" có lý do: luật ngắn tổng quát hơn, đỡ overfit.

#### B7. Bỏ phiếu bằng TRUNG BÌNH thay vì TỔNG ❌❌
**Ý tưởng**: Lớp có nhiều luật khớp đang chiếm lợi thế "số đông" → chia trung bình cho công bằng.

**Kết quả**: 82.1% — **giảm 3.2%**.

→ 🔴 **Thất bại**. Lý do: "số đông luật đồng thuận" CHÍNH LÀ thông tin quan trọng. Chia trung bình **mất** thông tin này. Paper biết điều này — họ chọn SUM có chủ ý.

#### B8. Top-K voting (k=3, 5, 7) 🟡
**Ý tưởng**: Chỉ K luật mạnh nhất bỏ phiếu, không cho luật yếu góp phần.

**Kết quả trung bình**:
| K | Accuracy | vs default |
|---|---:|---:|
| k=0 (tất cả) ← mặc định | 85.3% | 0 |
| k=7 | 85.1% | −0.2% |
| k=5 | 84.8% | −0.5% |
| k=3 | 84.7% | −0.6% |

→ 🟡 **Trung bình kém hơn**. Nhưng có dataset cụ thể cải thiện (xem mục sau).

#### B9. Lift voting + Top-K — ý tưởng cô đề xuất ⭐
**Ý tưởng**: Kết hợp 2 ý tưởng — chỉ top-K luật mạnh, vote bằng Lift.

**Kết quả trên 11 "data khó" (Auto, Hepatitis, Sonar, Iono, Wine, ...)**:

| Cấu hình | Avg 11 data khó | vs Paper |
|---|---:|---:|
| Paper CMAR 2001 | 87.4% | — |
| Lift + topK=3 | 87.5% | +0.1% |
| **Lift + topK=5** ⭐ | **88.3%** | **+0.9%** |

→ 🟢🟢 **THÀNH CÔNG trên nhóm data khó**.
- Auto **+4.0%**, Hepatitis **+2.1%**, Iono **+1.6%**, Wine **+1.2%**, Crx **+1.1%**
- Riêng Labor (chỉ 57 mẫu) bị giảm vì dữ liệu quá nhỏ
- **k=5 ổn hơn k=3** — k=3 quá ít, thiếu "đa số"

---

### 🗺️ Bản đồ tổng quát các hướng

```
                ┌───────────────────────┐
                │     Paper CMAR 2001   │
                │       Avg: 85.2%      │
                └───────────┬───────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
  ─────────────       ─────────────       ─────────────
  🟢 TRỤC A           🟡 TRỤC B-vote       🔴 TRỤC B-sort
  Tối ưu hiệu năng    Đổi weight vote      Đổi cách sort
  ─────────────       ─────────────       ─────────────
                                          
  + BitSet AND        + Lift weight        ❌ Sort by Lift
  + Hash CR-tree        85.2% (=)            82.3% (−3%)
  + Subset bitmap     + HM weight          ❌ Sort by HM
  + Parallel mining     85.2% (=)            76.2% (−9%)
  + Determinism                            ❌ Sort dài→ngắn
                                              −2% đến −3%
  Avg: 85.3% ⭐       Đặc biệt:
  (vượt paper +0.1)   + Lift + topK=5     🔴 Vote trung bình
                        thắng paper +0.9%    82.1% (−3.2%)
                        trên data khó ⭐
```

### 🧠 Bài học rút ra

1. ✅ **Tối ưu hiệu năng luôn an toàn** — không đụng công thức thì không giảm accuracy
2. ⚠️ **Sort là "linh hồn" của coverage pruning** — đụng vào sort là phải đo cẩn thận, dễ giảm mạnh
3. ⚠️ **Đổi weight vote ít rủi ro hơn đổi sort** — vì không ảnh hưởng tỉa luật
4. ⚠️ **Tổng (SUM) khi vote > Trung bình (AVG)** — số đông đồng thuận là thông tin, không phải nhiễu
5. ⚠️ **Top-K nhỏ chỉ tốt cho data khó** — data thường thì cần tất cả luật để vote
6. 🎯 **Lift hợp với data chiều cao + nhiễu**, χ² hợp với data thường

---

## 6. Cấu hình hiện tại em chốt

### Khuyến nghị

| Loại dataset | Cấu hình | Vì sao |
|---|---|---|
| 📊 **Mặc định (đa số bộ)** | Improved + tất cả luật + vote χ² | 85.3% — vượt paper, mạnh nhất tổng thể |
| 🧬 **Data khó** (chiều cao, nhiễu: Auto, Hepatitis, Iono, Sonar, Wine, Crx) | Improved + Lift weight + **top-K=5** | 88.3% — thắng paper +0.9% trên 11 bộ này |
| 🔬 **Data cực nhỏ** (< 100 mẫu: Labor, Iris) | Improved + tất cả luật | Top-K cắt quá nhiều thông tin trên dataset nhỏ |

### Số liệu tổng kết

| Cấu hình | Avg 26 bộ | Speed |
|---|---:|---:|
| Baseline (CMAR gốc) | 84.5% | 1.00× |
| Paper CMAR 2001 | 85.2% | — |
| **Improved (em chốt)** ⭐ | **85.3%** (vượt paper +0.1%) | **5.28×** |
| Improved + Lift + topK=5 | 85.0% TB nhưng +0.9% trên data khó | 5.28× |

---

## 🎯 Tóm tắt 1 đoạn cho cô

> *Em giữ nguyên 4 bước CMAR của paper (khai phá → tỉa 3 tầng → xếp cây → bỏ phiếu χ²). Cải tiến của em chia 2 trục: **(A) Tối ưu hiệu năng** — chạy nhanh 5.28× mà accuracy vẫn vượt paper +0.1%. **(B) Mở rộng công thức** — em thử 9 hướng (đổi weight, đổi sort, top-K, vote trung bình...): **5 hướng thất bại** vì đụng vào sort của paper hoặc mất thông tin "số đông", **2 hướng hòa**, **1 hướng thắng rõ trên data khó** là Lift + top-K=5 (+0.9% so với paper trên 11 bộ chiều cao như Auto, Hepatitis, Iono, Sonar).*
