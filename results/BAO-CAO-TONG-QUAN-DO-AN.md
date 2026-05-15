# 📑 BÁO CÁO TỔNG QUAN ĐỒ ÁN — CMAR

> File tổng hợp toàn bộ những gì đã làm trong đồ án: cài đặt, cải tiến hiệu năng, và mở rộng thuật toán.

---

## 📋 Mục lục

1. [Phần I — Đặt vấn đề](#phần-i--đặt-vấn-đề)
2. [Phần II — Cài đặt CMAR (Baseline)](#phần-ii--cài-đặt-cmar-baseline)
3. [Phần III — Cải tiến HIỆU NĂNG (Improved)](#phần-iii--cải-tiến-hiệu-năng-improved)
4. [Phần IV — Cải tiến THUẬT TOÁN (mở rộng công thức)](#phần-iv--cải-tiến-thuật-toán-mở-rộng-công-thức)
5. [Phần V — Tổng kết thí nghiệm](#phần-v--tổng-kết-thí-nghiệm-15-biến-thể)
6. [Phần VI — Cấu hình đề xuất cuối cùng](#phần-vi--cấu-hình-đề-xuất-cuối-cùng)
7. [Phần VII — Bài học rút ra](#phần-vii--bài-học-rút-ra)
8. [Phần VIII — File tham chiếu](#phần-viii--file-tham-chiếu)

---

# Phần I — Đặt vấn đề

## 1.1. Bài toán

**Phân lớp dữ liệu** dựa trên luật kết hợp (Associative Classification): cho máy học các luật `IF (X) THEN (lớp = c)` từ dữ liệu cũ → dùng các luật đó để đoán nhãn dữ liệu mới.

## 1.2. Thuật toán mục tiêu

**CMAR** — Classification based on Multiple Association Rules
Tác giả: Wenmin Li, Jiawei Han, Jian Pei
Công bố: IEEE ICDM 2001

## 1.3. Dữ liệu thực nghiệm

**26 bộ dữ liệu UCI** chuẩn (giống y trong paper):
- Phân loại: Iris, Wine, Glass, Zoo, Lymphography, ...
- Y tế: Diabetes, Heart, Hepatitis, Breast-Cancer, Sick, Hypo
- Tài chính: Crx, Australian, German
- Khác: Auto, Sonar, Iono, Labor, Anneal, Tic-Tac-Toe, ...

Đánh giá: **10-fold cross-validation** (như paper).

## 1.4. Mục tiêu đồ án

1. Cài đặt CMAR đúng paper (Baseline)
2. Tối ưu hiệu năng (Improved) — chạy nhanh hơn, accuracy bằng/hơn baseline
3. Thử mở rộng công thức để vượt qua paper trên một số nhóm dataset

---

# Phần II — Cài đặt CMAR (Baseline)

## 2.1. Pipeline 4 bước của CMAR

```
   📊 Dữ liệu train (N mẫu, có nhãn)
          │
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 1 — KHAI PHÁ LUẬT (FP-Growth)    ║  → 100,000 luật ứng viên
   ╚═══════════════════════════════════════╝
          │
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 2 — TỈA LUẬT (3 tầng)            ║
   ║   ① χ² pruning   (lọc thống kê)       ║
   ║   ② G2S pruning  (lọc dư thừa)        ║  → còn ~100–500 luật
   ║   ③ DCP pruning  (lọc phủ trùng)      ║
   ╚═══════════════════════════════════════╝
          │
          ▼
   ╔═══════════════════════════════════════╗
   ║ BƯỚC 3 — XẾP VÀO CR-TREE              ║  → cấu trúc tra cứu nhanh
   ╚═══════════════════════════════════════╝
          │
          ▼
   📥 Mẫu test → ┌────────────────────────────┐
                  │ BƯỚC 4 — BỎ PHIẾU CÓ TRỌNG │  → Nhãn dự đoán
                  │   SỐ (Weighted χ² Voting)  │
                  └────────────────────────────┘
```

## 2.2. Chi tiết từng bước

### Bước 1 — Khai phá luật (FP-Growth)
- Nén dữ liệu vào **FP-Tree** (cây tiền tố tần suất)
- Sinh tất cả tập mục thường xuyên (support ≥ minSup)
- Gắn nhãn lớp cho mỗi tập → luật `X → c`
- Tính `support`, `confidence` ngay khi sinh

### Bước 2 — Tỉa luật (3 tầng pruning)

**Tầng ① — Chi-Square Pruning (CSP)**: loại 3 loại rác
- Confidence < 0.5 → không đủ tin cậy
- χ² < 3.841 → không có ý nghĩa thống kê (p > 0.05)
- Confidence ≤ tỉ lệ ngẫu nhiên của lớp → đoán không hơn random

**Tầng ② — General-to-Specific Pruning (G2S)**: loại luật đặc biệt dư thừa
- Nếu luật B `⊇` luật A và chi²(B) ≤ chi²(A) → loại B

**Tầng ③ — Database Coverage Pruning (DCP)** ⭐ quan trọng nhất
- Sort theo `confidence DESC → support DESC → length ASC`
- Mỗi luật giữ lại nếu phủ ≥ 1 mẫu chưa được phủ
- Mỗi mẫu chỉ phủ tối đa δ=4 lần
- Còn ~100–500 luật cuối

### Bước 3 — Xếp luật vào CR-Tree
- Tree tiền tố theo `(lớp, item đầu)`
- Khi predict, chỉ duyệt nhánh đúng → O(K) thay O(N)

### Bước 4 — Bỏ phiếu có trọng số
1. Tìm tất cả luật khớp mẫu test
2. Nếu top luật conf cao nhất cùng đoán 1 lớp → trả lớp đó
3. Nếu không, vote bằng `weight = χ² chuẩn-hóa`:
   ```
   weight(r) = χ²(r) / max_χ²(r)
   ```
4. Lớp có TỔNG weight cao nhất → THẮNG

## 2.3. Tham số mặc định (theo paper)

| Tham số | Giá trị | Ý nghĩa |
|---|---:|---|
| minSupport | 1% | Tỉ lệ tối thiểu để giữ tập mục |
| minConfidence | 50% | Độ tin cậy tối thiểu của luật |
| chi² threshold | 3.841 | Ngưỡng p=0.05 |
| δ (coverage) | 4 | Mỗi mẫu phủ tối đa 4 luật |
| maxAntLen | 4 | Độ dài tiền đề tối đa |

## 2.4. Kết quả Baseline

| Metric | Giá trị |
|---|---:|
| Avg Accuracy | **84.5%** |
| vs Paper (85.2%) | −0.7% |
| Train time (sum 26 DS) | 70,461 ms |
| Số dataset thắng paper | 4/26 (15%) |

→ Baseline thua paper 0.7% vì cài đặt cũ có một **lỗi quan trọng** (giải thích ở Phần III).

---

# Phần III — Cải tiến HIỆU NĂNG (Improved)

## 3.1. Phát hiện vấn đề

Sau khi đo thời gian từng phase, em thấy:
- **Mining** chiếm 70% thời gian
- **Pruning** chiếm 25%
- **Predict** chiếm 5%

Và đặc biệt, em **phát hiện ra một dòng code "gian lận"** trong tầng G2S:

```java
if (rules.size() > 10000) return rules;
```

**Nghĩa**: Nếu có >10,000 luật thì **bỏ qua tầng G2S hoàn toàn** vì G2S là O(n²) — quá chậm trên dataset to.

**Hậu quả**:
- Luật dư thừa lọt qua → vote bị lệch trên dataset to (Waveform, Sick, Hypo, Led7)
- Baseline thua paper 0.7% chính vì lý do này

## 3.2. Các phase cải tiến (17 phase)

| Phase | Tối ưu | Cách làm | Tác động |
|:---:|---|---|---|
| 01 | Phase timer + memory sampler | Đo thời gian từng phase | Có bằng chứng định lượng |
| 02 | Hash-indexed CR-Tree | Tra luật theo `(class, first_item)` | Predict nhanh ~20× |
| 03 | Bitmap rule matching | AND bit thay quét tuần tự | Match nhanh ~10× |
| 04 | Shared BitSet giữa CSP/DCP | Tính match 1 lần, dùng 2 lần | Pruning −40% |
| 05 | Switch baseline/improved | CLI flag `--mode=` | So sánh A/B công bằng |
| 06 | Class-aware FP mining | Drop itemset vô ích sớm | Mining −30% itemset |
| 07 | Inverted item index | Support = `BitSet.and().cardinality()` | Mining ~5× |
| 08 | Deterministic compareTo | Tie-breaker đầy đủ | 2 lần chạy = số y hệt |
| 09 | Parallel top-level items | Mỗi item một thread | Mining ~2× |
| 10 | Post-sort cap luật/lớp | Cap sau sort, không trong loop | Determinism + accuracy ổn |
| 11 | ThreadLocal scratch BitSet | Tái sử dụng buffer cho chi² | −50% GC pressure |
| 12 | Header table tail pointer | Insert FP-tree O(n) → O(1) | Build tree nhanh |
| **13** | **Subset bitmap cho G2S** ⭐ | `(A & ~B) == 0` per word | **Gỡ skip G2S** |
| 14 | Index G2S theo `(cls, item, len)` | Chỉ so cặp có thể subset | G2S nhanh ~8× |
| 15 | Optional top-K voting | Tham số tùy chọn | Mở rộng tương lai |
| 16 | ThreadLocal instance bitmap | Tái sử dụng khi predict | Predict −30% |
| 17 | Single-path FP-tree | Sinh trực tiếp khi tree đơn nhánh | Mining nhanh thêm |

## 3.3. Phase QUAN TRỌNG NHẤT — Phase 13 (gỡ skip G2S)

### Vấn đề
G2S check `A ⊆ B` cho mọi cặp (A, B) — O(n²). Với 30,000 luật → 900 triệu phép check → quá chậm.

### Cách em làm

**Bước 1**: Mỗi luật được gắn 1 **dấu vân tay bitmap** (mảng bit):
```
Antecedent {3, 7, 15} → bit thứ 3, 7, 15 = 1
                      = long[] {0b1000000010001000...}
```

**Bước 2**: Subset check trên bitmap, **1 phép tính cho 64 item cùng lúc**:
```
A ⊆ B  ⟺  (A AND NOT B) == 0  (cho từng word 64-bit)
```

**Bước 3**: Index luật theo 3 tiêu chí:
- `class`: A và B phải cùng class
- `first item`: nếu A ⊆ B thì A phải chứa ít nhất 1 item của B
- `length bucket`: A phải ngắn hơn B

→ Chỉ duyệt cặp **có thể là subset**, bỏ qua cặp chắc chắn không phải.

### Kết quả
| Số luật | Baseline cũ | Improved mới |
|---:|---|---|
| 1,000 | < 1s | < 0.1s |
| 10,000 | ~5s | ~0.5s |
| 30,000 | ❌ **SKIP** (giữ luật rác) | ~1.5s, **lọc xuống ~3,000** |
| 50,000 | ❌ **SKIP** | ~3s, lọc xuống ~5,000 |

→ Em **bỏ luôn dòng skip** vì G2S đủ nhanh.

## 3.4. Kết quả Improved tổng thể

| Metric | Baseline | **Improved** | Tăng |
|---|---:|---:|---:|
| Train time (sum 26 DS) | 70,461 ms | **13,339 ms** | **5.28× nhanh** ⭐ |
| Mining time | 48,871 ms | 9,153 ms | 5.34× |
| Pruning time | 21,601 ms | 4,197 ms | 5.15× |
| **Avg Accuracy** | 84.5% | **85.3%** | **+0.8%** ⭐ |
| vs Paper | −0.7% | **+0.1%** | vượt paper |
| DS thắng paper | 4/26 | **14/26** | +10 DS |

→ Improved **vượt paper trên 14/26 dataset**, mạnh nhất ở dataset chiều cao (Auto +3.3, Hepatitis +2.8, Labor +2.0).

---

# Phần IV — Cải tiến THUẬT TOÁN (mở rộng công thức)

> Phần này KHÔNG đụng vào pipeline 4 bước của paper. Em chỉ thử **thay đổi công thức ở một số chỗ** để xem có vượt paper được nữa không.

## 4.1. 3 nhóm thí nghiệm

### Nhóm A — Đổi trọng số vote (an toàn nhất)
Vote là **bước cuối**, không ảnh hưởng tỉa luật → ít rủi ro.

| Biến thể | Vote weight |
|---|---|
| Default | χ² chuẩn-hóa |
| Lift weight | Lift |
| HM weight | Harmonic mean của (support, confidence) |
| **V7 χ²×Lift** ⭐ | χ² × Lift |
| **V9 conf×Lift** ⭐⭐ | confidence × Lift |

### Nhóm B — Đổi cách sắp xếp luật (rủi ro cao)
Sort ảnh hưởng **DCP** (tầng tỉa phụ thuộc thứ tự).

| Biến thể | Sort |
|---|---|
| Default (paper) | conf DESC → sup DESC → len ASC |
| Lift sort | Lift DESC → conf DESC → len ASC |
| HM sort | HM DESC → Lift DESC → len ASC |
| Longer-rules first | conf DESC → sup DESC → len **DESC** |
| Lift tiebreaker | conf DESC → sup DESC → Lift DESC → len ASC |

### Nhóm C — Đổi cách lọc luật
| Biến thể | Filter |
|---|---|
| Default | χ² ≥ 3.841 + conf ≥ 0.5 |
| Strict χ² | χ² ≥ 6.635 (p=0.01) |
| Relaxed χ² | χ² ≥ 2.706 (p=0.10) |
| Strict Lift | Lift ≥ 1.5 hoặc 2.0 |
| Lift-only | Bỏ điều kiện χ², thêm Lift ≥ 1.0 |

### Nhóm D — Đổi số luật vote (Top-K)
| Biến thể | Top-K |
|---|---|
| Default | 0 (tất cả) |
| Top-K=3, 5, 7 | giới hạn |

## 4.2. Kết quả 15 biến thể

### Bảng tổng hợp — toàn bộ 26 dataset

| # | Biến thể | Sort | Filter | Weight | TopK | Avg | vs Paper |
|:---:|---|---|---|---|:---:|---:|---:|
| 0 | **Default Improved** ⭐ | paper | paper | χ² | 0 | **85.3%** | **+0.1%** |
| 1 | Paper CMAR 2001 | paper | paper | χ² | 0 | 85.2% | — |
| 2 | Lift weight | paper | paper | Lift | 0 | 85.2% | ±0.0% |
| 3 | HM weight | paper | paper | HM | 0 | 85.2% | ±0.0% |
| 4 | **V7 χ²×Lift** ⭐ | paper | paper | **χ²×Lift** | 0 | 85.2% | ±0.0% |
| 5 | **V9 conf×Lift** ⭐⭐ | paper | paper | **conf×Lift** | 0 | 85.2% | ±0.0% |
| 6 | Lift filter (≥1) | paper | +Lift≥1 | χ² | 0 | 85.3% | +0.1% |
| 7 | Strict χ² | paper | χ²≥6.635 | χ² | 0 | 85.3% | +0.1% |
| 8 | Strict Lift=1.5 | paper | +Lift≥1.5 | χ² | 0 | 85.3% | +0.1% (no-op) |
| 9 | Lift tiebreaker | paper+Lift | paper | χ² | 0 | 85.3% | +0.1% (no-op) |
| 10 | Top-K=5 | paper | paper | χ² | 5 | 84.8% | −0.4% |
| 11 | Lift weight + topK=5 | paper | paper | Lift | 5 | 85.0% | −0.2% |
| 12 | Lift weight + topK=3 | paper | paper | Lift | 3 | 84.7% | −0.5% |
| 13 | 🔴 Lift sort | Lift DESC | paper | χ² | 0 | 82.3% | **−3%** |
| 14 | 🔴 HM sort | HM DESC | paper | χ² | 0 | 76.2% | **−9%** |
| 15 | 🔴 avgVote (chia TB) | paper | paper | χ² | 0 | 82.1% | **−3%** |
| 16 | Relaxed χ² | paper | χ²≥2.706 | χ² | 0 | 84.6% | −0.6% |
| 17 | Strict Lift=2.0 | paper | +Lift≥2 | χ² | 0 | 84.7% | −0.5% |

### Bảng tổng hợp — 11 data khó (Auto, Hepatitis, Iono, Sonar, Wine, Crx, Anneal, Breast-Cancer, Australian, Cleve, Labor)

| Cấu hình | Avg 11 hard | vs Paper (87.4%) |
|---|---:|---:|
| Paper CMAR | 87.4% | — |
| Default Improved | 88.6% | +1.2% |
| Lift weight + topK=0 | 88.6% | +1.2% |
| Lift weight + topK=5 | 88.3% | +0.9% |
| **V7 χ²×Lift, topK=0** ⭐ | **89.0%** | **+1.6%** |
| **V9 conf×Lift, topK=0** ⭐⭐ | **89.0%** | **+1.7%** |

→ **V9 là cấu hình mới TỐT NHẤT trên data khó.**

## 4.3. Phân tích các thất bại quan trọng

### ❌ Sort by Lift (−3%) / Sort by HM (−9%)
**Lý do**: DCP (tầng tỉa coverage) **phụ thuộc thứ tự sort**. Đổi sort = giữ tập luật cuối khác hẳn = vote khác = accuracy tụt mạnh.

→ **Bài học**: Sort là "linh hồn" của CMAR. Đừng đụng.

### ❌ Vote trung bình thay vì tổng (−3.2%)
**Lý do**: "Số đông luật đồng thuận" CHÍNH LÀ thông tin. Chia trung bình **xóa** thông tin này.

→ **Bài học**: Tổng > Trung bình khi vote.

### ⚪ Lift tiebreaker, Strict Lift=1.5 = no-op
**Lý do**:
- Tiebreaker chỉ kích hoạt khi conf+sup hòa chính xác → quá hiếm
- χ² ≥ 3.841 đã ngầm yêu cầu Lift > 1 → lọc thêm Lift≥1.5 dư thừa

→ **Bài học**: χ² và Lift đo cùng hướng, không cộng dồn.

## 4.4. Cải tiến THÀNH CÔNG mới (V9)

### Công thức cũ (paper)
```
weight(luật) = χ² chuẩn-hóa
```

### Công thức mới (V9 em đề xuất)
```
weight(luật) = confidence × Lift
```

### Tại sao V9 thắng trên data khó?
1. **Confidence** đo "độ chính xác dự đoán" → bù cho yếu điểm của Lift (Lift có thể cao trên luật ít mẫu nhưng kém chính xác)
2. **Lift** đo "độ tương quan dương" → bù cho yếu điểm của confidence (confidence cao có thể trùng hợp)
3. Tích 2 thứ → ép luật phải **vừa chính xác VÀ vừa thực sự tương quan** → chỉ luật "vàng" được nặng cân

### Kết quả V9 trên 11 data khó

| Dataset | Paper | **V9** | Δ |
|---|---:|---:|---:|
| Auto | 78.1 | **82.5** | **+4.4** 🟢🟢🟢 |
| Hepatitis | 80.5 | **84.8** | **+4.3** 🟢🟢🟢 |
| Sonar | 79.4 | **81.3** | **+1.9** 🟢🟢 |
| Iono | 91.5 | **93.2** | **+1.7** 🟢🟢 |
| Labor | 89.7 | **91.7** | **+2.0** 🟢🟢 |
| Breast-Cancer | 96.4 | **97.4** | **+1.0** 🟢 |
| Crx | 84.9 | **85.7** | **+0.8** 🟢 |
| Australian | 86.1 | **86.7** | **+0.6** 🟢 |
| Wine | 95.0 | **95.6** | **+0.6** 🟢 |
| Anneal | 97.3 | **97.9** | **+0.6** 🟢 |
| Cleve | 82.2 | **82.6** | **+0.4** 🟢 |
| **Avg** | **87.4** | **89.0** | **+1.7%** ⭐ |

→ **Thắng 11/11 data khó.**

---

# Phần V — Tổng kết thí nghiệm (15+ biến thể)

## 5.1. Sơ đồ tổng các hướng đã thử

```
                  ┌─────────────────────────┐
                  │     CMAR Paper 2001     │
                  │       Avg: 85.2%        │
                  └────────────┬────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
  ─────────────         ─────────────         ─────────────
   🟢 TRỤC A             🟡 TRỤC B-vote        🔴 TRỤC B-sort
   Tối ưu hiệu năng      Đổi weight vote       Đổi cách sort
  ─────────────         ─────────────         ─────────────
                                              
  17 phase tối ưu:      • Lift weight         ❌ Sort by Lift
  + BitSet AND            85.2% (=)              82.3% (−3%)
  + Hash CR-tree        • HM weight           ❌ Sort by HM
  + Subset bitmap         85.2% (=)              76.2% (−9%)
  + Parallel mining     • χ²×Lift (V7)        ❌ Longer-rules first
  + Determinism           85.2% (=) but ⭐       −2 đến −3%
  + Gỡ skip G2S           +1.6% trên hard     ❌ avgVote
                        • conf×Lift (V9)         82.1% (−3.2%)
  Avg: 85.3% ⭐⭐          85.2% (=) but ⭐⭐
  (vượt paper +0.1)       +1.7% trên hard
                                              ⚪ Lift tiebreaker
                        • Top-K=3, 5, 7          (no-op)
                          85.1%, 84.8%, 84.7%
                          (giảm dần)
                                              
                        ─────────────
                         🟡 TRỤC B-filter
                        ─────────────
                        ⚪ Lift filter (=1)
                        ⚪ Strict Lift=1.5
                          (no-op)
                        🔴 Strict Lift=2.0
                          84.7% (−0.5%)
                        🔴 Relaxed χ²
                          84.6% (−0.6%)
                        ⚪ Strict χ²=6.635
                          85.3% (= default)
```

## 5.2. Phân loại 18 biến thể

| Phân loại | Số | Tên |
|---|:---:|---|
| 🟢🟢 Thành công lớn | 1 | **V9 conf×Lift** (+1.7% trên data khó) |
| 🟢 Thành công nhỏ | 2 | V7 χ²×Lift (+1.6% hard), Default Improved (+0.1% all) |
| 🟡 Hòa | 4 | Lift weight, HM weight, V7, V9 trên all-26 |
| ⚪ No-op | 4 | Lift tiebreaker, Lift filter (≥1), Strict Lift=1.5, Strict χ² |
| 🔴 Thất bại | 7 | Sort by Lift, Sort by HM, Longer-rules, avgVote, Top-K=3/5/7, Strict Lift=2, Relaxed χ², V8/V10 |

---

# Phần VI — Cấu hình đề xuất cuối cùng

## 6.1. Chọn cấu hình theo loại dataset

| Loại dataset | Cấu hình | Avg | Lý do |
|---|---|---:|---|
| **Mặc định (đa số 26 bộ)** | Default Improved | **85.3%** | Vượt paper +0.1%, ổn định toàn cục |
| **Data khó (chiều cao, nhiễu)** | **V9: --weightConfLift --topK=0** ⭐ | **89.0%** trên 11 bộ | **Vượt paper +1.7%** |
| **Data cực nhỏ (<100 mẫu)** | Default Improved | varies | Top-K cắt thông tin quan trọng |

## 6.2. Bảng cài đặt chi tiết

### Cấu hình mặc định (data thường)
```
Sắp xếp luật:   confidence DESC → support DESC → length ASC  (paper)
Lọc luật:       chi² ≥ 3.841 + confidence ≥ 0.5             (paper)
Tỉa coverage:   δ = 4                                        (paper)
Vote weight:    chi² chuẩn-hóa                               (paper)
Top-K:          0 (tất cả luật khớp)                         (paper)
```
→ Accuracy: **85.3%** (toàn bộ 26 dataset)

### Cấu hình V9 (data khó) ⭐
```
Sắp xếp luật:   confidence DESC → support DESC → length ASC  (giữ paper)
Lọc luật:       chi² ≥ 3.841 + confidence ≥ 0.5             (giữ paper)
Tỉa coverage:   δ = 4                                        (giữ paper)
Vote weight:    confidence × Lift                            ← MỚI
Top-K:          0 (tất cả luật khớp)                         (giữ paper)
```
→ Accuracy: **89.0%** trên 11 data khó (vượt paper +1.7%)

## 6.3. Command line

```powershell
# Compile
javac -encoding UTF-8 -cp src -d bin src/cmar/util/*.java src/cmar/*.java src/cmar/benchmark/*.java

# Baseline (CMAR gốc) — đối chứng
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Default Improved — cấu hình chính 26 dataset
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved

# V9 conf×Lift — cấu hình data khó ⭐
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --weightConfLift --topK=0
```

---

# Phần VII — Bài học rút ra

## 7.1. Bài học kỹ thuật

| # | Bài học | Bằng chứng |
|:---:|---|---|
| 1 | **Tối ưu hiệu năng luôn an toàn** | 17 phase tối ưu, accuracy +0.8% (không đánh đổi) |
| 2 | **Đọc CODE cũ kỹ** | Phát hiện dòng `if (rules.size() > 10000) return rules` bị giấu trong baseline |
| 3 | **Sort là linh hồn của coverage prune** | Đổi sort = giảm 3–9% accuracy ngay |
| 4 | **Tổng (SUM) > Trung bình (AVG) khi vote** | avgVote thất bại −3.2% |
| 5 | **χ² và Lift đo cùng hướng** | Lọc thêm Lift sau χ² là no-op |
| 6 | **Lift hợp với data khó, χ² hợp với data thường** | V9 conf×Lift +1.7% trên hard, =0% trên all |
| 7 | **Composite weight > Single weight** trên data khó | V9 (conf×Lift) > Lift thuần (+0.5% trên hard) |
| 8 | **Top-K nhỏ chỉ tốt nếu thiếu thông tin** | Top-K=5 thua TopK=0 trên data đủ lớn (Labor: 91.7% vs 86.7%) |

## 7.2. Bài học nghiên cứu

| # | Bài học |
|:---:|---|
| 1 | Phải có **Baseline** để đo "cải tiến của em hơn bao nhiêu" |
| 2 | Phải so với **PAPER** (chuẩn mực), không chỉ Baseline (em tự viết) |
| 3 | "Thất bại" cũng là **kết quả nghiên cứu có giá trị** — chứng minh paper đã tối ưu kỹ |
| 4 | **Đo phase timing** là chìa khóa tìm bottleneck thực sự |
| 5 | **Determinism** (chạy lại ra số y hệt) là yêu cầu tối thiểu của khoa học |

---

# Phần VIII — File tham chiếu

## 8.1. Báo cáo chi tiết

| File | Nội dung |
|---|---|
| [BAO-CAO-TONG-QUAN-DO-AN.md](BAO-CAO-TONG-QUAN-DO-AN.md) | **File này** — tổng quan |
| [HUONG-DAN-CHO-NGUOI-MOI.md](HUONG-DAN-CHO-NGUOI-MOI.md) | Hướng dẫn dành cho người chưa biết gì |
| [BAO-CAO-CHI-TIET-DA-CAI-TIEN-DUOC-GI.md](BAO-CAO-CHI-TIET-DA-CAI-TIEN-DUOC-GI.md) | Chi tiết 17 phase tối ưu hiệu năng |
| [BAO-CAO-SO-SANH-PAPER-BASE-IMPROVED.md](BAO-CAO-SO-SANH-PAPER-BASE-IMPROVED.md) | Bảng so sánh 4 cấu hình chính |
| [BAO-CAO-COMPOSITE-WEIGHT.md](BAO-CAO-COMPOSITE-WEIGHT.md) | **V7/V8/V9/V10 (composite weight)** ⭐ |
| [BAO-CAO-THU-NGHIEM-LOC-VA-SAP-XEP.md](BAO-CAO-THU-NGHIEM-LOC-VA-SAP-XEP.md) | V1–V6 (thử lọc/sắp xếp) |
| [BAO-CAO-TOPK35-LIFT-VS-PAPER.md](BAO-CAO-TOPK35-LIFT-VS-PAPER.md) | Lift+topK=3,5 so với Paper |
| [BAO-CAO-HUONG-DI-VA-LIFT.md](BAO-CAO-HUONG-DI-VA-LIFT.md) | Pipeline hiện tại + Lift |

## 8.2. File số liệu chính

| File | Cấu hình | Avg 26 DS | Avg 11 hard |
|---|---|---:|---:|
| [summary-report-baseline.md](summary-report-baseline.md) | Baseline | 84.5% | — |
| [summary-report-topk0.md](summary-report-topk0.md) | Default Improved | 85.3% | 88.6% |
| [summary-report-liftweight-topk0.md](summary-report-liftweight-topk0.md) | Lift weight | 85.2% | 88.6% |
| **[summary-report-confLift-topk0.md](summary-report-confLift-topk0.md)** ⭐ | **V9 conf×Lift** | **85.2%** | **89.0%** |
| [summary-report-chiLift-topk0.md](summary-report-chiLift-topk0.md) | V7 χ²×Lift | 85.2% | 89.0% |
| [summary-report-liftweight-topk5.md](summary-report-liftweight-topk5.md) | Lift + topK=5 | 85.0% | 88.3% |
| [summary-report-liftweight-topk3.md](summary-report-liftweight-topk3.md) | Lift + topK=3 | 84.7% | 87.5% |

## 8.3. Cấu trúc thư mục code

```
src/cmar/
├── benchmark/
│   ├── BenchmarkRunner.java    ← Chạy 10-fold CV trên 26 dataset (CLI flag)
│   ├── DataLoader.java         ← Đọc UCI .data, .names
│   ├── UCIDatasets.java        ← Tham số paper từng bộ
│   ├── MDLDiscretizer.java     ← Rời rạc hóa Entropy/MDL
│   ├── DiagnosticRunner.java
│   └── ParamTuner.java
├── util/
│   ├── PhaseTimer.java         ← Đo thời gian từng phase
│   ├── MemorySampler.java      ← Đo peak memory
│   └── OptimizationProfile.java ← Switch baseline/improved
├── FPGrowth.java               ← Baseline FP-Growth
├── FPGrowthOptimized.java      ← Improved FP-Growth (BitSet)
├── FPTree.java, FPNode.java    ← FP-Tree
├── Rule.java                   ← Class Rule + compareTo + bitmap
├── RulePruner.java             ← 3 tầng pruning (CSP/G2S/DCP)
├── CRTree.java                 ← Hash-indexed rule tree
└── CMARClassifier.java         ← Lớp chính (fit/predict)
```

---

# 🎯 TÓM TẮT 1 ĐOẠN CHO BÁO CÁO ĐỒ ÁN

> *Em cài đặt thuật toán **CMAR** (Li-Han-Pei, ICDM 2001) trên 26 bộ dataset UCI chuẩn với 10-fold cross-validation. Đầu tiên cài đặt Baseline đúng paper, đạt **84.5%** (thua paper 0.7% do baseline có dòng `if (rules.size() > 10000) return rules` ẩn — bỏ qua tầng tỉa G2S trên dataset to). Sau đó em làm **17 phase tối ưu hiệu năng** (BitSet AND, hash CR-tree, subset bitmap, parallel mining, deterministic ordering, ...), trong đó cải tiến quan trọng nhất là **gỡ skip G2S** bằng dấu vân tay bitmap + index theo `(class, item đầu, độ dài)`. Phiên bản Improved chạy **nhanh 5.28×** và đạt accuracy **85.3%** (vượt paper +0.1% trên toàn bộ 26 dataset, thắng paper trên 14/26 bộ). Em cũng thử **15 biến thể mở rộng công thức**: 7 hướng thất bại (sort by Lift/HM, avgVote, top-K nhỏ...) chứng minh paper đã tối ưu kỹ; 4 hướng no-op (Lift tiebreaker, Strict χ², Strict Lift=1.5...); và 1 hướng **THÀNH CÔNG**: cấu hình `--weightConfLift --topK=0` (vote weight = confidence × Lift) **thắng paper +1.7% trên 11 data khó** (Auto +4.4, Hepatitis +4.3, Sonar +1.9, Iono +1.7, Labor +2.0, ...) — thắng 11/11 bộ trong nhóm này.*
