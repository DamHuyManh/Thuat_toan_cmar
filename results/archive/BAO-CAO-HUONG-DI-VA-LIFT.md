# Báo cáo: Hướng đi hiện tại & Thí nghiệm với Lift

> Trả lời 4 câu hỏi: (1) Hiện tại chạy thế nào? (2) So với cách cũ? (3) Đi hướng nào? (4) Đã thử Lift chưa?

---

## 🟢 1. CÁCH HIỆN TẠI ĐANG CHẠY

### Pipeline mặc định

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT: dataset UCI (vd. Anneal)                            │
│         ↓                                                    │
│  Discretize (MDL) — chỉ học cut points từ train fold        │
│         ↓                                                    │
│  Mining: FP-Growth (optimized — parallel + inverted index)  │
│         ↓ sinh ~100k–200k luật ứng viên                     │
│  Pruning 3 tầng:                                            │
│    1. Chi-square (χ² ≥ 3.841)                               │
│    2. General-to-Specific (G2S)                             │
│    3. Database Coverage (DCP)                               │
│         ↓ còn ~100–500 luật                                 │
│  Indexing: CR-Tree (hash by class + first item)             │
│         ↓                                                    │
│  PREDICT: sort theo confidence DESC → support DESC          │
│           sum chi² của TẤT CẢ luật khớp                     │
│           → chọn class có sum cao nhất                      │
└─────────────────────────────────────────────────────────────┘
```

### Cấu hình mặc định

| Thành phần | Cách dùng | Theo paper? |
|---|---|:---:|
| **Sort luật** | `confidence DESC → support DESC → length ASC` | ✅ Chuẩn paper |
| **Voting weight** | χ² chuẩn-hóa (`χ² / max_χ²`) | ✅ Chuẩn paper |
| **Số luật vote** | TẤT CẢ luật khớp (topK=0) | ✅ Chuẩn paper |
| **Filter** | conf ≥ 0.5, chi² ≥ 3.841, conf > prior | ✅ Chuẩn paper |

### Command chạy

```powershell
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=0
```

### Kết quả

| Metric | Giá trị |
|---|---:|
| **Avg Accuracy** | **85.3%** ⭐ |
| Δ vs Paper CMAR (85.2%) | +0.1% (vượt nhẹ paper) |
| Δ vs Baseline (84.5%) | +0.8% |
| Số dataset thắng paper | 14/26 |

---

## 🔄 2. SO SÁNH VỚI CÁCH CŨ

### Bảng tổng hợp các cấu hình đã thử

| # | Cấu hình | Avg | vs Default | Đánh giá |
|---|---|---:|---:|:---:|
| 0 | **🟢 HIỆN TẠI (default + topK=0)** | **85.3%** | 0 | ⭐ Best |
| 1 | Paper CMAR 2001 (số gốc) | 85.2% | −0.1% | (mục tiêu) |
| 2 | Baseline (chưa optimize) | 84.5% | −0.8% | (trước cải tiến) |
| 3 | topK=7 | 85.1% | −0.2% | 🥈 Cân bằng acc/speed |
| 4 | topK=5 | 84.8% | −0.5% | 🟡 OK |
| 5 | topK=3 | 84.7% | −0.6% | 🟡 OK |
| 6 | **Lift voting weight** | 85.2% | −0.1% | 🟢 Đáng dùng cho rule-noisy |
| 7 | HM voting weight | 85.2% | −0.1% | 🟢 Tương tự Lift |
| 8 | Lift filter (≥1.0) | 85.3% | 0 | ⚪ No-op (redundant) |
| 9 | ❌ Full hybrid (HM sort + Lift filter + HM weight) | **76.2%** | −9.1% | 🔴 Thảm họa |
| 10 | ❌ avgVote | 82.1% | −3.2% | 🔴 Tệ |
| 11 | perClassTopK=5 | 85.1% | −0.2% | 🟢 Tốt cho predict nhanh |

### Đọc bảng

- **Hàng 0**: Cấu hình hiện tại — **85.3%, tốt nhất tổng thể**
- **Hàng 3-5**: Giảm số luật vote (top-k) → accuracy giảm dần (k=0 > k=7 > k=5 > k=3)
- **Hàng 6-7**: Đổi voting weight (Lift/HM thay χ²) → gần bằng nhưng kém nhẹ
- **Hàng 9-10**: Cách "cải tiến" thất bại — đừng dùng

---

## 🧭 3. ĐI THEO HƯỚNG NÀO?

### **Đi theo hướng CMAR GỐC** ⭐

Đây là hướng **paper Li-Han-Pei 2001**. Không phải hướng "larger lift value".

### Tại sao?

| Đã thử | Kết quả | Quyết định |
|---|---|---|
| Đổi sort theo HM/Lift | Phá coverage prune → −9% | ❌ Bỏ |
| Đổi voting weight (Lift) | Gần bằng χ² (±0.1%) | 🟡 Optional |
| Thêm Lift filter | Redundant — chi² đã làm | ⚪ Bỏ |
| Chia avg thay vì sum | Mất thông tin → −3% | ❌ Bỏ |

→ **Mọi thí nghiệm rời xa CMAR gốc đều KHÔNG tốt hơn**. Hướng paper gốc là tối ưu.

### Vị trí của Lift trong hệ thống hiện tại

| Vai trò | Có dùng không? |
|---|:---:|
| Tính Lift cho mọi luật | ✅ Có (mỗi run đều tính, lưu trong `Rule.lift`) |
| Dùng Lift làm filter | ❌ Không (vô tác dụng vì chi² đã loại) |
| Dùng Lift làm sort | ❌ Không (phá coverage prune) |
| Dùng Lift làm voting weight | 🟡 **OPTION** (`--liftWeight`) — không bật mặc định |
| Xuất Lift ra CSV để inspect | ✅ Có (`--dumpRules`) |

→ Lift **không phải hướng chính**, chỉ là **option phụ** cho người dùng muốn test.

---

## 🔬 4. ĐÃ THỬ LIFT CHƯA?

### **RỒI**, thử 3 cách dùng Lift

### Cách A — Lift làm FILTER (loại luật)

Logic:
```
chỉ giữ luật có Lift ≥ 1.0  (tương quan thuận)
```

| Command | Avg | Kết luận |
|---|---:|---|
| `--liftOnly --topK=0` | **85.3%** | ⚪ **REDUNDANT** |

**Tại sao redundant?** Pipeline CMAR đã có `if (chi² ≥ 3.841 && conf > prior) ...` — điều kiện này đã **loại sạch luật có tương quan độc lập/nghịch**, tương đương Lift ≥ 1. Thêm Lift filter không cắt thêm luật nào.

### Cách B — Lift làm VOTING WEIGHT

Logic:
```
classScores[c] += rule.lift   // thay vì += rule.weight (χ²)
```

| Command | Avg | Kết luận |
|---|---:|---|
| `--liftWeight --topK=0` | **85.2%** | 🟢 **OK** — gần bằng χ², phân bố khác |
| `--liftWeight --topK=7` | 85.2% | Tương tự |

**Lift voting THẮNG χ² trên 5 dataset rule-noisy**:

| Dataset | χ² weight | Lift weight | Gain |
|---|---:|---:|---:|
| **Hepatitis** | 83.3 | **84.8** | **+1.5** ⭐⭐ |
| **Auto** | 81.4 | **82.5** | **+1.1** ⭐ |
| **Iono** | 92.6 | **93.2** | +0.6 |
| **Sonar** | 80.8 | **81.3** | +0.5 |
| **Breast-Cancer** | 97.1 | **97.4** | +0.3 |

**Lift voting THUA χ² trên dataset balanced**:

| Dataset | χ² | Lift | Loss |
|---|---:|---:|---:|
| Lymphography | 83.4 | 82.0 | −1.4 |
| Horse | 82.3 | 81.0 | −1.3 |
| Wine | 96.7 | 95.6 | −1.1 |
| Zoo | 96.5 | 95.6 | −0.9 |

### Cách C — Lift kết hợp HM làm SORT

Logic:
```
sort luật theo: HM DESC → Lift DESC → length ASC
```

| Command | Avg | Kết luận |
|---|---:|---|
| `--hmLift --topK=0` | **76.2%** | ❌ **THẢM HỌA** |

**Tại sao thảm họa?**
- HM ưu tiên rule recall cao (cover nhiều instances)
- Coverage prune iterate theo thứ tự sort → mark instance covered sớm
- Các luật conf cao quan trọng bị loại vì không cover thêm instance
- Số rule còn lại sau prune giảm 90% (Anneal: 159 → 14)
- Model collapse về majority-class prediction

---

## 📊 5. TÓM TẮT 1 BẢNG

| Câu hỏi | Trả lời ngắn |
|---|---|
| Hiện tại chạy như nào? | **CMAR gốc + topK=0 + chi² voting** (paper-faithful) |
| So với cách cũ thế nào? | **85.3% > Baseline 84.5% (+0.8%) > Paper 85.2% (+0.1%)** |
| Đi theo hướng nào? | **CMAR GỐC** — không phải Lift |
| Đã thử Lift chưa? | **Rồi, 3 cách**: filter (redundant), weight (mixed), sort (broken) |
| Lift có thay χ² không? | **Không** — tương đương trung bình, chỉ hơn cho rule-noisy DS |
| Khi nào dùng Lift? | **Dataset rule-noisy** (Auto, Hepatitis, Iono, Sonar) → gain 0.5–1.5% |

---

## 💡 6. KẾT LUẬN ĐỀ XUẤT

### Quan điểm

> Implementation của tôi **đi theo CMAR gốc** và đã **chứng minh empirically** trên 26 dataset UCI rằng:
> 
> 1. **CMAR gốc + tối ưu hiệu năng** là cấu hình tốt nhất (85.3%, vượt paper +0.1%)
> 2. **Lift đã được thử** ở cả 3 vai trò (filter, weight, sort). Chỉ Lift voting **có giá trị conditional** cho dataset rule-noisy
> 3. **Đổi sort sang HM/Lift** là **side effect nguy hiểm** — phá coverage pruning, giảm 9%
> 4. **Đổi voting weight** là cách **an toàn nhất** để thử nghiệm khác

### Đóng góp chính

| # | Đóng góp |
|---|---|
| 1 | Tối ưu hiệu năng 5.28× so với baseline (giữ accuracy paper-level) |
| 2 | Empirical verification: CMAR gốc vẫn là tốt nhất sau khi thử 5+ biến thể |
| 3 | Phát hiện **Lift voting** có ích cho **rule-noisy dataset** (gain +1.5% trên Hepatitis) |
| 4 | Phát hiện **Lift filter** REDUNDANT trên CMAR pipeline (do chi² đã làm) |
| 5 | Phân tích sensitivity top-k per dataset (42% DS không nhạy, 8% cực nhạy) |

### Nếu phải chọn 1 cấu hình duy nhất

→ **`--mode=improved --topK=0`** (default).
- 85.3% trung bình
- Thắng paper 14/26 dataset
- Không cần tham số kỳ quặc, chuẩn paper

### Nếu được chọn 2 cấu hình

→ **Mặc định = `topK=0 + chi²`** (general purpose)  
→ **Khi dataset rule-noisy = `topK=0 + --liftWeight`** (gain 0.5–1.5%)

---

## 📁 File liên quan đã tạo

| File | Nội dung |
|---|---|
| ⭐ **[BAO-CAO-HUONG-DI-VA-LIFT.md](BAO-CAO-HUONG-DI-VA-LIFT.md)** | **File này** — đọc đầu tiên |
| [CHI-TIET-TOPK-VA-LIFT.md](CHI-TIET-TOPK-VA-LIFT.md) | Chi tiết từng cấu hình top-k và Lift |
| [BAO-CAO-HM-LIFT-VA-TOPK.md](BAO-CAO-HM-LIFT-VA-TOPK.md) | Phân tích Lift/HM/top-k đầy đủ + thống kê per dataset |
| [BAO-CAO-TOPK-NAO-TOT-NHAT.md](BAO-CAO-TOPK-NAO-TOT-NHAT.md) | So sánh giữa các giá trị top-k |
| [TONG-QUAN-DU-AN.md](TONG-QUAN-DU-AN.md) | Tổng quan toàn dự án |

---

## 🎬 Mẫu đoạn cho báo cáo / luận văn

> *"Chúng tôi triển khai thuật toán CMAR theo đúng paper gốc (Li, Han, Pei 2001), kết hợp với các tối ưu hiệu năng nâng tốc độ 5.28×. Đồng thời, chúng tôi tiến hành 3 thí nghiệm thay thế với độ đo Lift: (1) **Lift filter** — loại luật có lift < 1, kết quả là redundant do chi-square đã thực hiện vai trò này; (2) **Lift voting weight** — thay thế chi² bằng Lift trong công thức bỏ phiếu, kết quả là gần như tương đương trung bình (85.2% vs 85.3%) nhưng cải thiện rõ rệt trên các dataset có nhiều luật yếu (Hepatitis +1.5%, Auto +1.1%); (3) **Lift sort** — kết hợp HM và Lift làm tiêu chí sort, kết quả là phá vỡ database coverage pruning và làm giảm accuracy 9%. Kết luận: **CMAR gốc vẫn là phương án tối ưu**, Lift voting có giá trị conditional cho dataset rule-noisy."*
