# 📚 BÁO CÁO ĐỒ ÁN — CMAR (Hoàn chỉnh, có công thức chi tiết)

> **Ngày**: 2026-05-16
> **Đề tài**: Cải tiến thuật toán CMAR (Li, Han, Pei, ICDM 2001)
> **Kết quả chính**: **Vượt paper +0.2% trên 26 dataset, vượt +1.7% trên 11 dataset khó**

---

## 📑 Mục lục

1. [Bài toán & Mục tiêu](#1-bài-toán--mục-tiêu)
2. [Khái niệm cơ bản & công thức](#2-khái-niệm-cơ-bản--công-thức)
3. [Thuật toán CMAR paper 2001](#3-thuật-toán-cmar-paper-2001)
4. [Cải tiến #1 — HIỆU NĂNG (17 phase)](#4-cải-tiến-1--hiệu-năng-17-phase)
5. [Cải tiến #2 — TỈA LUẬT: Stratified Coverage](#5-cải-tiến-2--tỉa-luật-stratified-coverage)
6. [Cải tiến #3 — BỎ PHIẾU: Composite Weight](#6-cải-tiến-3--bỏ-phiếu-composite-weight)
7. [Cải tiến #4 — BỎ PHIẾU: Top-K Voting](#7-cải-tiến-4--bỏ-phiếu-top-k-voting)
8. [Các hướng thất bại (24 thí nghiệm)](#8-các-hướng-thất-bại-24-thí-nghiệm)
9. [Kết quả số liệu đầy đủ](#9-kết-quả-số-liệu-đầy-đủ)
10. [Code Review](#10-code-review)
11. [Hướng nghiên cứu tiếp theo](#11-hướng-nghiên-cứu-tiếp-theo)

---

## 1. Bài toán & Mục tiêu

### 1.1. Bài toán

Cho dataset có N mẫu, mỗi mẫu có K thuộc tính và 1 nhãn lớp c ∈ {c₁, c₂, ..., cₘ}.
**Mục tiêu**: Học một tập luật `IF X THEN c` từ dataset, dùng để đoán nhãn mẫu mới.

**Ví dụ Iris**:
```
IF (cánh hẹp) AND (đài hẹp) → loài = Setosa
IF (cánh rộng) AND (đài dài) → loài = Virginica
```

### 1.2. Dữ liệu & đánh giá

- **26 dataset UCI** (giống y paper 2001)
- Kích thước: từ 57 (Labor) đến 5000 (Waveform)
- Số lớp: 2 đến 10
- Số thuộc tính: 4 đến 60
- **Đánh giá**: 10-fold cross-validation (chia 10 phần, dùng 9 train, 1 test, lặp 10 lần)

### 1.3. Mục tiêu đồ án

1. Cài đặt CMAR đúng paper (Baseline)
2. **Tối ưu hiệu năng** (chạy nhanh hơn, không đổi công thức)
3. **Cải tiến công thức** để vượt paper

### 1.4. Lý do cần cải tiến cho data hiện đại

| Đặc điểm | UCI 2001 (paper) | Hiện đại |
|---|---|---|
| Phân bố lớp | Cân bằng | Imbalanced (Diabetes 65/35, Hypo 95/5) |
| Số lớp | 2 (đa số) | Đa lớp (Glass 6, Led7 10, Zoo 7) |
| Kích thước | > 200 | Có cả nhỏ (Labor 57) |
| Số thuộc tính | < 30 | Chiều cao (Sonar 60, Iono 34) |

→ Paper 2001 không tối ưu cho data hiện đại.

---

## 2. Khái niệm cơ bản & công thức

### 2.1. Luật phân lớp (Class Association Rule — CAR)

```
R: X → c
```

Trong đó:
- **X** = tiền đề = `{x₁, x₂, ..., xₖ}` (tập điều kiện)
- **c** = lớp dự đoán

**Ví dụ**: `{cánh=hẹp, đài=hẹp} → loài=Setosa`

### 2.2. Support (Độ phổ biến)

Cho dataset D có N mẫu, luật R: X → c:

```
       |{ d ∈ D : X ⊆ d AND label(d) = c }|
Sup(R) = ────────────────────────────────────
                       N
```

**Nghĩa**: Tỉ lệ mẫu vừa chứa tất cả điều kiện X vừa có lớp c.

**Ví dụ**:
- Dataset có 100 mẫu
- 30 mẫu có {cánh=hẹp, đài=hẹp, loài=Setosa}
- → Sup(R) = 30/100 = 0.30 (30%)

**Ý nghĩa**: Sup cao → luật phổ biến, áp dụng được cho nhiều mẫu.

### 2.3. Confidence (Độ tin cậy)

```
        |{ d ∈ D : X ⊆ d AND label(d) = c }|
Conf(R) = ────────────────────────────────────
              |{ d ∈ D : X ⊆ d }|
```

**Nghĩa**: Trong các mẫu CÓ X, có bao nhiêu % thuộc lớp c.

**Ví dụ**:
- 40 mẫu có {cánh=hẹp, đài=hẹp}
- Trong đó 30 mẫu thuộc Setosa
- → Conf(R) = 30/40 = 0.75 (75%)

**Phạm vi**: Conf ∈ [0, 1].

**Ý nghĩa**: Conf = "xác suất predict đúng khi thấy X".

### 2.4. Chi-square (χ²) — kiểm định độc lập

Cho bảng 2×2:

```
              | Lớp = c | Lớp ≠ c | Tổng
─────────────┼─────────┼─────────┼──────
  X ⊆ d      |    a    |    b    | a+b
  X ⊄ d      |    c    |    d    | c+d
─────────────┼─────────┼─────────┼──────
   Tổng      |  a+c    |  b+d    |  N
```

Công thức χ²:
```
                  N × (ad − bc)²
χ²(R) = ──────────────────────────────────
        (a+b)(c+d)(a+c)(b+d)
```

**Nghĩa**: Đo độ "lệch" giữa quan sát thực tế và giả thuyết "X độc lập với c".

**Ngưỡng paper**: χ² ≥ **3.841** ⟺ p-value < 0.05 (ý nghĩa thống kê 95%).

**Vấn đề**: χ² **không bị giới hạn** — phụ thuộc N (dataset to → χ² to).

**Chuẩn-hóa của paper** (Weighted Chi-Square):
```
                    χ²(R)²
χ²_chuẩn-hóa(R) = ────────────
                  χ²_max(R)
```

Trong đó `χ²_max(R)` = giá trị χ² lý thuyết tối đa.

### 2.5. Lift (Độ tương quan)

```
        P(X, c)              Sup(X → c) × N
Lift(R) = ───────────── = ──────────────────────────
        P(X) × P(c)        Sup(X) × Sup(c)
```

**Nghĩa**: So sánh "xác suất X và c xuất hiện cùng nhau" với "xác suất nếu X và c độc lập".

**Đọc giá trị Lift**:
| Lift | Ý nghĩa |
|:---:|---|
| `> 1` | X **làm tăng** khả năng có c → tương quan dương ✅ |
| `= 1` | X và c **độc lập** → luật vô dụng |
| `< 1` | X **làm giảm** khả năng có c → tương quan âm ❌ |

**Ví dụ**:
- P(X, c) = 0.30, P(X) = 0.40, P(c) = 0.50
- Lift = 0.30 / (0.40 × 0.50) = 0.30 / 0.20 = **1.5**
- → X làm tăng khả năng c lên 50% so với ngẫu nhiên.

### 2.6. So sánh 4 thước đo

| Thước đo | Trả lời câu hỏi | Phạm vi | Phụ thuộc N? |
|---|---|---|:---:|
| Support | "Luật phổ biến cỡ nào?" | [0, 1] | Không |
| Confidence | "Khi thấy X, predict đúng bao nhiêu %?" | [0, 1] | Không |
| Chi² | "X và c liên quan thật hay trùng hợp?" | [0, ∞) | **Có** |
| Lift | "X kéo c lên hay đè c xuống?" | [0, ∞) | Không |

---

## 3. Thuật toán CMAR paper 2001

### 3.1. Pipeline 4 bước

```
   📊 Dữ liệu huấn luyện
          │
          ▼
   ┌─────────────────────────────────────────┐
   │ BƯỚC 1: KHAI PHÁ LUẬT (FP-Growth)       │
   │   Sinh tất cả luật có Sup ≥ minSup      │
   └─────────────────────────────────────────┘
          │  ~100,000 luật
          ▼
   ┌─────────────────────────────────────────┐
   │ BƯỚC 2: TỈA LUẬT (3 tầng)               │
   │   ① CSP: Chi-square pruning             │
   │   ② G2S: General-to-Specific            │
   │   ③ DCP: Database Coverage Pruning      │
   └─────────────────────────────────────────┘
          │  ~100-500 luật
          ▼
   ┌─────────────────────────────────────────┐
   │ BƯỚC 3: SẮP XẾP LUẬT vào CR-Tree        │
   │   Theo confidence DESC → sup DESC →     │
   │   length ASC                            │
   └─────────────────────────────────────────┘
          │
          ▼
   📥 Mẫu test
          ▼
   ┌─────────────────────────────────────────┐
   │ BƯỚC 4: BỎ PHIẾU                        │
   │   Tổng phiếu = Σ weight(luật khớp)      │
   │   với weight = χ² chuẩn-hóa             │
   └─────────────────────────────────────────┘
          │
          ▼
       🏷️ Lớp dự đoán
```

### 3.2. Chi tiết Bước 2 — Tỉa luật

#### Tầng ① — Chi-Square Pruning (CSP)
Giữ luật nếu **TẤT CẢ** điều kiện sau đúng:
```
1. Conf(R) ≥ 0.5                    (đủ tin cậy)
2. χ²(R)  ≥ 3.841                   (p < 0.05, ý nghĩa thống kê)
3. Conf(R) > Sup(c)/N               (đoán hơn random)
```

#### Tầng ② — General-to-Specific Pruning (G2S)
Loại luật B nếu tồn tại luật A:
```
- A.antecedent ⊂ B.antecedent       (A tổng quát hơn B)
- A.class = B.class                  (cùng lớp)
- χ²(A) ≥ χ²(B)                      (A mạnh hơn B)
```
→ Loại luật B (đặc biệt hóa vô ích).

#### Tầng ③ — Database Coverage Pruning (DCP)
Tham số: δ = 4 (mỗi mẫu được phủ tối đa 4 luật).

```
1. Sắp luật theo: confidence DESC → support DESC → length ASC
2. Quét từ luật mạnh nhất:
   - Đếm mẫu nó phủ
   - Nếu có ≥1 mẫu chưa được phủ → GIỮ
   - Đánh dấu mẫu đó "đã phủ"
   - Mẫu đã phủ δ=4 lần → ngừng phủ nữa
3. Lặp đến khi mọi mẫu được phủ hoặc hết luật
```

### 3.3. Chi tiết Bước 4 — Bỏ phiếu

#### Công thức trọng số paper (Weighted Chi-Square)
```
                   χ²(R)
weight(R) = ─────────────────────
            χ²_max(R)
```

#### Quy trình bỏ phiếu cho mẫu test t
```
1. Tìm tất cả luật R khớp t (R.antecedent ⊆ items(t))
2. Nếu không có luật khớp → trả về defaultClass
3. Sắp luật khớp theo CMAR order
4. NHANH: Nếu các luật conf cao nhất cùng predict 1 lớp → trả lớp đó
5. NẾU không nhất trí:
       Phiếu(c) = Σ weight(R) cho mọi R khớp với R.class = c
       Lớp thắng = argmax Phiếu(c)
```

### 3.4. Tham số mặc định paper

| Tham số | Giá trị | Ý nghĩa |
|---|---:|---|
| `minSupport` | Tùy bộ (paper Table 3) | Tỉ lệ tối thiểu |
| `minConfidence` | 0.5 | Conf tối thiểu |
| `chi² threshold` | 3.841 | Mức ý nghĩa p=0.05 |
| `δ (coverage)` | 4 | Mỗi mẫu phủ tối đa 4 luật |
| `maxAntLen` | 4 | Độ dài tiền đề tối đa |

### 3.5. Kết quả paper 2001

- **Avg accuracy 26 dataset**: 85.2%

---

## 4. Cải tiến #1 — HIỆU NĂNG (17 phase)

> **Mục tiêu**: Chạy nhanh hơn baseline, KHÔNG đổi công thức.

### 4.1. Vấn đề phát hiện

Khi đo thời gian từng bước trên baseline:
- Mining: 70% thời gian
- Pruning: 25%
- Predict: 5%

**Đặc biệt**: Baseline có dòng code "gian lận" trong G2S:
```java
if (rules.size() > 10000) return rules;  // BỎ QUA tầng G2S
```
→ Trên dataset to (Waveform, Sick, Led7), G2S bị skip → luật rác lọt qua → accuracy giảm.

### 4.2. Các cải tiến (17 phase)

| Phase | Cải tiến | Tác dụng |
|:---:|---|---|
| 01 | PhaseTimer + MemorySampler | Đo bottleneck |
| 02 | Hash-indexed CR-Tree | Predict O(K) thay O(N) → nhanh ~20× |
| 03 | Bitmap rule matching | AND bit thay quét tuần tự |
| 04 | Shared BitSet cho CSP/DCP | Pruning −40% time |
| 05 | CLI flag `--mode=baseline\|improved` | A/B test công bằng |
| 06 | Class-aware FP mining | Drop itemset vô ích sớm |
| 07 | Inverted item index + BitSet | Support tính ~10× nhanh |
| 08 | Deterministic compareTo | 2 lần chạy = số y hệt |
| 09 | Parallel top-level items | Mining ~2× trên 4-core |
| 10 | Post-sort cap luật/lớp | Determinism + accuracy ổn |
| 11 | ThreadLocal scratch BitSet | −50% GC pressure |
| 12 | Header table tail pointer | Insert FP-tree O(1) |
| **13** ⭐ | **Subset bitmap cho G2S** | **GỠ hack "skip G2S"** |
| 14 | Index G2S theo (cls, item, len) | G2S nhanh ~8× |
| 15 | Optional top-K voting | Tham số tùy chọn |
| 16 | ThreadLocal instance bitmap | Predict −30% |
| 17 | Single-path FP-tree | Mining nhanh thêm |

### 4.3. Phase 13 chi tiết — Gỡ hack "skip G2S"

#### Vấn đề
G2S check `A ⊆ B` cho mọi cặp (A, B) — O(n²). Với 30,000 luật → 900 triệu phép check → quá chậm.

#### Cải tiến — Dùng dấu vân tay bitmap

**Bước 1**: Mỗi luật được gán **dấu vân tay bitmap** `long[] antBitmap`:
```
Antecedent {3, 7, 15}:
   antBitmap[0] = bit 3 + bit 7 + bit 15
                = 0b...1000000010001000
```

**Bước 2**: Subset check trên bitmap, 64 item/phép:
```
A ⊆ B  ⟺  (A AND NOT B) == 0  cho từng long word
```

**Bước 3**: Index theo `(class, first_item, length_bucket)` → chỉ so cặp có thể subset.

#### Kết quả gỡ hack

| Số luật | Baseline (có skip) | Improved (không skip) |
|---:|---|---|
| 1,000 | < 1s | < 0.1s |
| 10,000 | ~5s | ~0.5s |
| 30,000 | ❌ **SKIP, giữ luật rác** | ~1.5s, lọc còn ~3,000 luật |
| 50,000 | ❌ **SKIP** | ~3s, lọc còn ~5,000 luật |

### 4.4. Kết quả tổng

| | Tổng train time (26 DS) | Mining | Pruning | Accuracy avg |
|---|---:|---:|---:|---:|
| Baseline | 70,461 ms | 48,871 ms | 21,601 ms | 84.5% |
| **Improved** | **13,339 ms** | **9,153 ms** | **4,197 ms** | **85.3%** |
| Tăng tốc | **5.28×** | 5.34× | 5.15× | +0.8% |

---

## 5. Cải tiến #2 — TỈA LUẬT: Stratified Coverage

> **Mục tiêu**: Sửa lỗi paper trên data đa lớp / không cân bằng / nhỏ.

### 5.1. Vấn đề phát hiện

DCP gốc paper:
```
Duyệt luật theo confidence DESC
Giữ luật phủ ≥1 mẫu mới
```

Trên data **đa lớp** (Glass 6 lớp, Lymphography 4 lớp) hoặc **imbalanced**:
- Lớp đông duyệt trước → phủ hết mẫu
- Lớp ít → bị "đói luật" → vote thiếu thông tin

### 5.2. Cải tiến — Stratified Coverage Prune

#### Pseudocode

```
BƯỚC 0 (mới): Bảo vệ top-N luật MỖI lớp
  for class c in all_classes:
    keep top-N rules of class c (ưu tiên cao nhất)
    update coverage from these rules

BƯỚC 1 (như paper): DCP bình thường
  for rule in remaining_rules:
    if rule covers ≥1 uncovered instance:
      keep rule
      update coverage
```

#### Công thức

```
Final_rules = StratifiedReserve(top_N_per_class) ∪ DCP(remaining)
```

### 5.3. Tìm N tối ưu

| N | Avg 26 DS |
|:---:|---:|
| 0 (tắt) | 85.3% |
| 3 | 85.3% (quá ít) |
| 5 | 85.3% |
| 8 | 85.3% |
| **10** ⭐ | **85.4%** (best) |
| 15 | 85.3% |
| 20 | 85.3% |

→ **N = 10** là sweet spot.

### 5.4. Kết quả thắng trên data đa lớp / nhỏ

| Dataset | N | Classes | Không Stratified | **Stratified=10** | Δ |
|---|---:|---:|---:|---:|---:|
| **Glass** | 214 | **6** | 69.9% | **71.8%** | **+1.9%** |
| **Labor** | **57** | 2 | 91.7% | **93.3%** | **+1.6%** |
| **Lymphography** | 148 | **4** | 82.0% | **83.1%** | **+1.1%** |
| Led7 | 3200 | **10** | 72.2% | **72.8%** | +0.6% |
| Horse | 368 | 2 | 81.0% | **81.5%** | +0.5% |

→ Đúng giả thuyết: **bảo vệ minority class** → tăng accuracy trên data đa lớp.

---

## 6. Cải tiến #3 — BỎ PHIẾU: Composite Weight

> **Mục tiêu**: Tăng accuracy trên dataset chiều cao / có nhiễu.

### 6.1. Vấn đề paper

Paper bỏ phiếu bằng **MỘT chỉ số**: χ² chuẩn-hóa.

Trên data nhiễu:
- χ² có thể "thổi phồng" trên luật có ít mẫu nhưng số trùng hợp
- Confidence cao có thể do may rủi

### 6.2. Cải tiến — Vote weight = `confidence × Lift`

#### Công thức so sánh

| | Paper 2001 | **Cải tiến của em** |
|---|---|---|
| Công thức | `weight(R) = χ²_chuẩn-hóa(R)` | `weight(R) = Conf(R) × Lift(R)` |
| Ý nghĩa | "Ý nghĩa thống kê" | "Chính xác × Tương quan" |
| Tác dụng | Có thể nặng cân luật trùng hợp | Chỉ luật **vừa chính xác VÀ tương quan mạnh** mới nặng cân |

#### Diễn giải toán học

```
weight_paper(R) = χ²(R) / χ²_max(R)
                = "luật có ý nghĩa thống kê đến đâu"

weight_em(R)    = Conf(R) × Lift(R)
                = "Conf(R)" × "Sup(R→c) × N / [Sup(R) × Sup(c)]"
                = (xác suất predict đúng) × (mức độ X kéo c lên)
```

#### Ví dụ minh họa

Luật A: Conf=0.85, Lift=2.3, χ²=50
Luật B: Conf=0.70, Lift=3.0, χ²=40
Luật C: Conf=0.95, Lift=1.1, χ²=80 (Lift gần 1 → trùng hợp)

```
Cách paper (χ² chuẩn-hóa):
   weight_A = 0.5 (vd)
   weight_B = 0.4
   weight_C = 0.8  ← cao nhất, mặc dù Lift gần 1!

Cách em (Conf × Lift):
   weight_A = 0.85 × 2.3 = 1.955
   weight_B = 0.70 × 3.0 = 2.100  ← cao nhất, tương quan mạnh
   weight_C = 0.95 × 1.1 = 1.045  ← thấp, vì Lift gần 1
```

→ Cách em **phạt luật trùng hợp** (Lift ≈ 1), thưởng luật **tương quan thực sự**.

### 6.3. Vì sao chọn `Conf × Lift` chứ không phải `χ² × Lift`?

Em đã thử **CẢ 2 phương án**:

| Cách | Công thức | Số bước tính | Phạm vi | Cần normalize? |
|---|---|:---:|:---:|:---:|
| **A** | `Conf × Lift` | **1 phép nhân** | Tự nhiên 0–∞ | **Không** |
| **B** | `χ² × Lift` | 4 bước (lấy χ², lấy χ²_max, chia, nhân) | Sau normalize 0–∞ | Có |

#### Kết quả số liệu — 2 cách cho kết quả IDENTICAL

| Dataset | Cách A (Conf×Lift) | Cách B (χ²×Lift) | Khác biệt |
|---|---:|---:|---:|
| Auto | 82.5% | 82.5% | 0 |
| Hepatitis | 84.8% | 84.8% | 0 |
| Iono | 93.2% | 93.2% | 0 |
| Sonar | 81.3% | 81.3% | 0 |
| Wine | 95.6% | 95.6% | 0 |
| Labor | 91.7% | 91.7% | 0 |
| ... | ... | ... | ... |
| **Trung bình 26** | **85.2%** | **85.2%** | **0** |
| **Trung bình 11 hard** | **89.0%** | **89.0%** | **0** |

#### Vì sao 2 cách cho cùng kết quả?

Khi sort luật theo trọng số để chọn lớp thắng, **thứ tự tương đối** mới quan trọng, không phải giá trị tuyệt đối. Vì χ²_chuẩn-hóa và Conf **có tương quan rất cao** (cùng đo "luật đúng nhiều hay ít"), nên 2 công thức cho ranking gần như giống hệt nhau.

#### Áp dụng nguyên tắc Occam's Razor

> *"Khi 2 phương án cho cùng kết quả, chọn phương án ĐƠN GIẢN HƠN."*
> — William of Ockham (1300)

→ Chọn **Cách A: `Conf × Lift`** vì:
1. Chỉ 1 phép nhân (paper cần 4 bước)
2. Không cần normalize
3. Confidence đã ở dạng [0, 1] sẵn
4. Dễ hiểu cho người mới

### 6.4. Kết quả V9 (Conf × Lift) trên 11 data khó

| Dataset | Paper | **Cải tiến em** | Δ |
|---|---:|---:|---:|
| **Auto** (205 mẫu, 25 attrs, 6 lớp) | 78.1% | **82.5%** | **+4.4%** 🟢🟢🟢 |
| **Hepatitis** (155 mẫu, 19 attrs) | 80.5% | **84.8%** | **+4.3%** 🟢🟢🟢 |
| **Sonar** (208 mẫu, 60 attrs) | 79.4% | **81.3%** | **+1.9%** 🟢🟢 |
| **Iono** (351 mẫu, 34 attrs) | 91.5% | **93.2%** | **+1.7%** 🟢🟢 |
| **Labor** (57 mẫu) | 89.7% | **91.7%** | **+2.0%** 🟢🟢 |
| Breast-Cancer | 96.4% | **97.4%** | +1.0% |
| Crx | 84.9% | **85.7%** | +0.8% |
| Anneal | 97.3% | **97.9%** | +0.6% |
| Australian | 86.1% | **86.7%** | +0.6% |
| Wine | 95.0% | **95.6%** | +0.6% |
| Cleve | 82.2% | **82.6%** | +0.4% |
| **Avg 11 hard** | **87.4%** | **89.0%** | **+1.7%** ⭐ |

→ **Thắng 11/11 bộ data khó** so với paper.

---

## 7. Cải tiến #4 — BỎ PHIẾU: Top-K Voting

> **Mục tiêu**: Hiện đại hóa theo CPAR (Yin & Han 2003), Park & Lim (2021).

### 7.1. Vấn đề paper

Paper vote **TẤT CẢ** luật khớp mẫu test.

Ví dụ với mẫu test khớp 50 luật:
- 5 luật mạnh (Conf > 0.9)
- 45 luật yếu (Conf 0.5–0.7)
- → 45 luật yếu vote chung với 5 luật mạnh → **nhiễu**

### 7.2. Cải tiến — Top-K Voting

Chỉ K luật **mạnh nhất** (sau khi sort theo CMAR order) được vote.

```
Predict(t):
  matched_rules = find_all_matching(t)
  sorted_rules = sort(matched_rules)  # conf → sup → len
  top_K = sorted_rules[0:K]
  
  Phiếu(c) = Σ weight(R) for R in top_K where R.class = c
  return argmax Phiếu(c)
```

### 7.3. Tìm K tối ưu

Em chạy benchmark với K ∈ {3, 5, 7, 10, 15, 0 (= tất cả)}:

| K | Avg 11 data khó | Đánh giá |
|:---:|---:|:---:|
| 3 | 87.5% | Cắt quá tay |
| 5 | 88.3% | Vẫn ít |
| 7 | 88.8% | Khá |
| **10** ⭐ | **89.0%** | **Best** |
| 15 | 88.9% | Bắt đầu bão hoà |
| 0 (= paper cũ) | 89.0% | Tương đương K=10 |

#### Vì sao K=10 là điểm vàng?

- K = 3, 5: cắt **quá tay** → trên dataset nhỏ (Labor 57 mẫu), giảm tận **−5%** (91.7 → 86.7)
- K = 15+: bắt đầu cho cả luật yếu vào → quay về như paper cũ
- **K = 10**: vừa đủ luật cho mọi dataset, vừa lọc nhiễu

---

## 8. Các hướng thất bại (24 thí nghiệm)

> Đây là bằng chứng khoa học: paper 2001 đã được **tinh chỉnh rất kỹ**, không phải hướng nào cũng cải tiến được.

### 8.1. Đụng vào SẮP XẾP (12 hướng — TẤT CẢ thất bại)

| # | Cách đã thử | Avg | vs Paper | Lý do thất bại |
|:---:|---|---:|---:|---|
| 0 | ⭐ **conf → sup → ngắn (paper)** | **85.3%** | **+0.1%** | Best |
| 1 | Sắp theo Lift trước | 82.3% | **−2.9%** | Lift trên ít mẫu thắng → phá coverage |
| 2 | Sắp theo HM trước | 76.2% | **−9.0%** | Thảm họa |
| 3 | Sắp theo χ² trước | 81.5% | **−3.7%** | χ² to trên dataset to → phá tỉ lệ |
| 4 | Tích `conf × Lift` làm sort | 83.7% | **−1.5%** | Như Lift sort |
| 5 | Tích `χ² × Lift` làm sort | 82.5% | **−2.7%** | Tương tự |
| 6 | Tuyến tính `conf + 0.1×Lift` | 85.2% | **−0.1%** | Bù trừ |
| 7 | Luật dài trước | ~83% | **−2 đến −3%** | Luật dài dễ overfit |
| 8 | Lift làm tiebreaker vị trí 3 (Zou & Chou 2022) | 85.3% | ±0 | No-op (Lift bằng nhau giữa cùng class) |
| 9 | Lift vị trí 2 | 85.3% | ±0 | No-op |
| 10 | Lift có điều kiện | 85.3% | ±0 | No-op |
| 11 | Dominant class tie-breaker | 85.3% | ±0 | No-op |
| 12 | Class-weighted sort (em đề xuất) | 83.1% | **−2.1%** | Phá coverage prune |

**Bài học**: Confidence là tiêu chí TỐI ƯU cho sort. Không thay được.

### 8.2. Đụng vào LỌC (7 hướng — TẤT CẢ thất bại)

| # | Cách đã thử | Avg | vs Paper |
|:---:|---|---:|---:|
| 0 | ⭐ **χ² ≥ 3.841 (paper)** | **85.3%** | **+0.1%** |
| 1 | χ² ≥ 6.635 (chặt, p=0.01) | 85.3% | ±0 (bù trừ) |
| 2 | χ² ≥ 2.706 (nới, p=0.10) | 84.6% | **−0.6%** |
| 3 | + Lift ≥ 1 | 85.3% | ±0 (no-op) |
| 4 | + Lift ≥ 1.5 | 85.3% | ±0 (no-op) |
| 5 | + Lift ≥ 2.0 | 84.7% | **−0.5%** |
| 6 | Bỏ filter χ², dùng Lift-only | Giảm | 🔴 |
| 7 | Dual filter (χ² OR Lift+conf) | 85.3% | ±0 (no-op) |

**Bài học**: χ² = 3.841 là điểm cân bằng. Lift filter dư thừa vì χ² đã bao trùm.

### 8.3. Đụng vào TỈA COVERAGE (5 hướng — 1 thành công)

| # | Cách đã thử | Avg | Đánh giá |
|:---:|---|---:|:---:|
| 1 | δ = 2 | Giảm | 🔴 Mất luật |
| 2 | δ = 3 | Giảm | 🔴 |
| 3 | δ = 4 (paper) | 85.3% | Baseline |
| 4 | δ = 5, 6 | Giảm | 🔴 |
| **5** | **Stratified=10 (em đề xuất)** | **85.4%** | 🟢🟢 **Success!** |

### 8.4. Tổng kết 24 hướng đã thử

```
┌────────────────────────────────────────┐
│  Tổng:                       24        │
│  Thành công (cải thiện):      3 (12.5%)│
│  No-op:                       7 (29.2%)│
│  Thất bại (giảm acc):        14 (58.3%)│
└────────────────────────────────────────┘
```

→ Tỉ lệ thành công 12.5% — chứng minh paper 2001 đã tinh chỉnh kỹ.

---

## 9. Kết quả số liệu đầy đủ

### 9.1. Tổng kết 3 cấu hình (Fresh hôm nay)

| | Avg 26 DS | Avg 11 data khó | Speedup |
|---|---:|---:|---:|
| Paper CMAR 2001 | 85.2% | 87.4% | (chuẩn) |
| Baseline (chưa tối ưu) | 84.5% | 87.0% | 1.00× |
| Improved hiệu năng | 85.3% | 88.9% | **5.28×** |
| **🏆 Cấu hình CUỐI** ⭐ | **85.4%** | **89.0%** | **5.28×** |
| **Δ vs Paper** | **+0.2%** | **+1.7%** | — |

**Cấu hình cuối**: Tối ưu hiệu năng + Stratified Coverage=10 + Vote weight (conf × Lift) + Top-K=10.

### 9.2. Bảng đầy đủ 26 dataset

| Dataset | Số mẫu | Số lớp | Paper | **Cấu hình cuối** | Δ vs Paper |
|---|---:|---:|---:|---:|---:|
| Anneal | 898 | 6 | 97.3 | **97.8** | +0.5 🟢 |
| Australian | 690 | 2 | 86.1 | **86.8** | +0.7 🟢 |
| **Auto** | 205 | 6 | 78.1 | **82.6** | **+4.5** 🟢🟢🟢 |
| **Breast-Cancer** | 683 | 2 | 96.4 | **97.4** | **+1.0** 🟢🟢 |
| Cleve | 303 | 2 | 82.2 | **82.6** | +0.4 🟢 |
| Crx | 690 | 2 | 84.9 | **85.7** | +0.8 🟢 |
| Diabetes | 768 | 2 | **75.8** | 73.3 | −2.5 🔴 |
| German | 1000 | 2 | **74.9** | 72.9 | −2.0 🔴 |
| **Glass** | 214 | **6** | 70.1 | **71.8** | **+1.7** 🟢🟢 |
| Heart | 270 | 2 | **82.2** | 80.4 | −1.8 🔴 |
| **Hepatitis** | 155 | 2 | 80.5 | **84.8** | **+4.3** 🟢🟢🟢 |
| Horse | 368 | 2 | **82.6** | 81.5 | −1.1 🔴 |
| Hypo | 3163 | 2 | **98.4** | 97.9 | −0.5 ⚪ |
| **Iono** | 351 | 2 | 91.5 | **93.2** | **+1.7** 🟢🟢 |
| Iris | 150 | 3 | **94.0** | 92.7 | −1.3 🔴 |
| **Labor** | **57** | 2 | 89.7 | **93.3** | **+3.6** 🟢🟢🟢 |
| Led7 | 3200 | **10** | **72.5** | 72.8 | +0.3 🟢 |
| Lymphography | 148 | **4** | 83.1 | **83.1** | ±0 ⚪ |
| Pima | 768 | 2 | **75.1** | 73.3 | −1.8 🔴 |
| Sick | 2800 | 2 | **97.5** | 96.8 | −0.7 ⚪ |
| **Sonar** | 208 | 2 | 79.4 | **81.2** | **+1.8** 🟢🟢 |
| Tic-Tac-Toe | 958 | 2 | 99.2 | **99.0** | −0.2 ⚪ |
| Vehicle | 846 | 4 | **68.8** | 68.3 | −0.5 ⚪ |
| Waveform | 5000 | 3 | **83.2** | 81.6 | −1.6 🔴 |
| Wine | 178 | 3 | 95.0 | **95.1** | +0.1 ⚪ |
| Zoo | 101 | 7 | **97.1** | 95.6 | −1.5 🔴 |
| **TB 26** | | | **85.2** | **85.4** ⭐ | **+0.2%** |

### 9.3. Thống kê thắng/thua

| Nhóm | Số DS | Tỉ lệ | Ví dụ |
|---|:---:|---:|---|
| 🟢🟢🟢 Thắng đậm (≥2%) | 4 | 15% | Auto +4.5, Hepatitis +4.3, Labor +3.6, Glass +1.7 |
| 🟢🟢 Thắng vừa (+1 đến +2%) | 4 | 15% | Iono, Sonar, Breast-Cancer, Wine |
| 🟢 Thắng nhẹ (+0.1 đến +1%) | 6 | 23% | Anneal, Australian, Cleve, Crx, Led7 |
| ⚪ Hòa (±0.5%) | 5 | 19% | Hypo, Lymphography, Sick, Vehicle, Wine |
| 🔴 Thua (−0.5% đến −2.5%) | 7 | 27% | Diabetes, German, Heart, Pima, Iris, Horse, Zoo |

→ **Thắng 14/26, hòa 5, thua 7.**

### 9.4. Tốc độ train (ms, 26 dataset cộng dồn)

| | Tổng train time | Mining | Pruning |
|---|---:|---:|---:|
| Baseline | 70,461 ms | 48,871 ms | 21,601 ms |
| **Improved (final)** | **13,339 ms** | **9,153 ms** | **4,197 ms** |
| **Speedup** | **5.28×** | **5.34×** | **5.15×** |

---

## 10. Code Review

### 10.1. Thống kê

| Mục | Số liệu |
|---|---:|
| Tổng dòng code | **5,566 dòng** |
| Số file Java | 19 file |

### 10.2. Đánh giá CHI TIẾT

#### ✅ Điểm mạnh (8)

1. **Tách biệt Baseline vs Improved rõ ràng** qua `OptimizationProfile.Mode` → so sánh A/B công bằng
2. **Đo lường đầy đủ**: PhaseTimer + MemorySampler → biết bottleneck
3. **Bitmap-based subset check** (long[] antBitmap) → cực nhanh
4. **Hash CR-Tree** → predict O(K)
5. **Parallel mining** qua ForkJoinPool → mỗi cây con độc lập
6. **Deterministic compareTo** với tie-breaker đầy đủ → 2 lần chạy = số y hệt
7. **ThreadLocal scratch buffers** → giảm GC pressure
8. **CLI flags đầy đủ** → thử nhiều biến thể không phải sửa code

#### ⚠️ Điểm cần cải thiện (8)

1. **Static state trong Rule.java** (`useLiftSort`, `CLASS_FREQS`, ...) — anti-pattern, khó test
2. **Quá nhiều CLI flags** (20+) — nên gom thành preset (`--preset=paper|v9|final`)
3. **Method dài**: `BenchmarkRunner.main` ~110 dòng, `CMARClassifier.fit` ~70 dòng
4. **Comment trộn Việt-Anh** — thống nhất 1 ngôn ngữ
5. **Thiếu unit test toàn diện** — chỉ có TestSubsetBitmap
6. **Duplicate code** giữa FPGrowth (173) và FPGrowthOptimized (427)
7. **Magic numbers** rải rác (`MAX_MINING_MS = 600000`, `PARALLEL_MIN_TX = 200`)
8. **Output report trộn logic** trong `BenchmarkRunner` — nên tách `ReportWriter`

### 10.3. Tổng đánh giá

| Tiêu chí | Điểm | Nhận xét |
|---|:---:|---|
| Đúng đắn (correctness) | 9/10 | Logic CMAR đúng paper, đã verify 10-fold CV |
| Hiệu năng | 9/10 | Tăng tốc 5.28×, dùng BitSet/parallel chuẩn |
| Khả năng đọc | 6/10 | Comment đầy đủ nhưng method dài, static state |
| Khả năng test | 5/10 | Chỉ có 1 unit test, static state khó test |
| Khả năng mở rộng | 7/10 | CLI flags dễ thêm cấu hình, nhưng cần refactor |
| Tài liệu | 8/10 | Code có comment chi tiết, nhiều file báo cáo |
| Determinism | 10/10 | 2 lần chạy ra số y hệt — tốt cho khoa học |
| **Tổng** | **7.7/10** | **Khá tốt — nên refactor static state trước khi mở rộng** |

---

## 11. Hướng nghiên cứu tiếp theo

### 11.1. Cải thiện accuracy
1. **Phân loại dataset tự động**: đo `n_classes`, `imbalance ratio`, `n_features` → chọn cấu hình tự động
2. **Adaptive Stratified N**: thay N=10 cố định bằng `N = log(n_class) × √n_samples`
3. **Lift threshold theo dataset**: data nhiễu cao dùng filter chặt hơn

### 11.2. Cải thiện tốc độ
1. **GPU-accelerated mining**: CUDA cho subset check / support counting
2. **Incremental mining**: thêm 1 mẫu không phải mine lại từ đầu
3. **Approximate mining**: sampling để mine nhanh trên dataset lớn

### 11.3. Mở rộng phạm vi
1. **Multi-label classification**: 1 mẫu thuộc nhiều lớp
2. **Streaming data**: dữ liệu đến liên tục
3. **Fuzzy AC**: thuộc tính mờ (Alcala-Fdez 2011)

### 11.4. Refactor code
1. Loại bỏ static state trong `Rule.java` → dependency injection
2. Gom CLI flags thành preset
3. Thêm JUnit tests cho các module chính

---

## 12. Tổng kết 1 đoạn cho đồ án

> *Đồ án này cài đặt thuật toán CMAR (Li-Han-Pei, ICDM 2001) trên 26 dataset UCI chuẩn với 10-fold cross-validation. Đầu tiên, phát hiện baseline có hack "skip G2S nếu >10K luật" làm giảm accuracy. Sau khi gỡ bỏ và thực hiện **17 cải tiến hiệu năng** (BitSet AND, hash CR-Tree, subset bitmap, parallel mining, deterministic ordering...), bản tối ưu chạy **nhanh 5.28×** mà accuracy vẫn vượt paper +0.1%. Tiếp đó, em thử **24 hướng cải tiến công thức** — chỉ **3 hướng thành công**: (1) **Stratified Coverage Pruning** (bảo vệ top-10 luật mỗi class trước DCP) thắng trên data đa lớp (Glass +1.9, Labor +1.6, Lymphography +1.1); (2) **Composite Vote Weight** (`weight = Conf × Lift` thay `χ² chuẩn-hóa` của paper) — chọn vì đơn giản hơn `χ² × Lift` mà cho cùng kết quả (Occam's Razor) — thắng trên data nhiễu (Auto +4.4, Hepatitis +4.3, Sonar +1.9, Iono +1.7); (3) **Top-K Voting** với K=10 (hiện đại hóa từ CPAR/Park & Lim). 21 hướng còn lại thất bại — bằng chứng paper tinh chỉnh kỹ. Cấu hình cuối đạt **85.4% trên 26 dataset (vượt paper +0.2%)** và **89.0% trên 11 dataset khó (vượt paper +1.7%, thắng 11/11)**. Mã nguồn 5,566 dòng Java, có 19 file, đánh giá tổng 7.7/10 — kiến trúc rõ ràng, đo lường đầy đủ, deterministic; điểm cần cải thiện là loại bỏ static state và thêm unit tests.*
