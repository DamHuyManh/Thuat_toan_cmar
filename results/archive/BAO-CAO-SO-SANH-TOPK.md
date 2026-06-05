# CMAR — Thí nghiệm: Đổi cách BỎ PHIẾU (voting)

| Mục | Nội dung |
|---|---|
| **Ngày chạy** | 2026-05-12 |
| **Đánh giá** | 10-fold CV trên 26 bộ UCI |
| **Mục tiêu** | Thử các cách voting MỚI (không đổi sort) |

## Các strategy đã thử

| Strategy | Mô tả | CLI |
|---|---|---|
| **Default CMAR** ⭐ | Sum weighted χ² (top-k global) | (mặc định) |
| **avgVote** | **Trung bình** weights per class (thay vì sum) | `--avgVote` |
| **perClassTopK** | Lấy top-k của **MỖI class riêng** (thay vì global) | `--perClassTopK=N` |
| **perClass + avg** | Combo cả 2 | `--perClassTopK=N --avgVote` |

> Lý do thử:
> - **avgVote**: Tránh bias về class có nhiều luật (class lớn dễ thắng do nhiều luật cộng dồn).
> - **perClassTopK**: CPAR-style — đảm bảo mỗi class đóng góp k luật tốt nhất, fair hơn.

---

## 1. Tổng kết Avg Accuracy

| Cấu hình | Avg | vs Default tương đương |
|---|---:|---:|
| 🥇 **Default topK=0** | **85.3%** | (baseline) |
| Default topK=7 | 85.1% | — |
| **perClass k=5** ⭐ | **85.1%** | **+0.3** vs default k=5 |
| Default topK=5 | 84.8% | — |
| Default topK=3 | 84.7% | — |
| perClass k=3 | 84.3% | **−0.4** vs default k=3 |
| avgVote topK=7 | 83.9% | **−1.2** ❌ |
| perClass k=5 + avgVote | 82.8% | **−2.0** ❌ |
| avgVote topK=0 | 82.1% | **−3.2** 🔴 |

### Kết luận tổng thể

| Strategy | Có cải thiện không? |
|---|---|
| **perClassTopK alone** | 🟢 **CÓ** (+0.3% trên k=5) — thay được top-k global |
| avgVote alone | ❌ Tệ hơn (−1 đến −3%) |
| Combo (perClass+avg) | ❌ Tệ |

---

## 2. Bảng accuracy chi tiết (k=0 vs perClass k=5)

| Dataset | Default k=0 | perClass k=5 | Δ |
|---|---:|---:|---:|
| Anneal | 98.2 | **98.2** | 0.0 |
| Australian | **86.8** | 86.4 | −0.4 |
| Auto | 81.4 | 81.4 | 0.0 |
| **Breast-Cancer** | 97.1 | **97.4** | **+0.3** ⭐ |
| Cleve | **82.6** | 82.3 | −0.3 |
| **Crx** | 86.1 | **86.3** | **+0.2** ⭐ |
| Diabetes | **73.4** | 73.3 | −0.1 |
| German | **72.9** | 72.7 | −0.2 |
| Glass | 70.0 | 70.0 | 0.0 |
| Heart | 80.7 | 80.7 | 0.0 |
| **Hepatitis** | 83.3 | **83.4** | **+0.1** |
| Horse | **82.3** | 82.1 | −0.2 |
| Hypo | 97.9 | 97.9 | 0.0 |
| Iono | **92.6** | 91.7 | −0.9 |
| Iris | 92.7 | 92.7 | 0.0 |
| Labor | **91.7** | 91.7 | 0.0 |
| Led7 | 72.2 | 72.2 | 0.0 |
| Lymphography | **83.4** | 81.6 | **−1.8** ⚠️ |
| Pima | **73.4** | 73.3 | −0.1 |
| Sick | 96.8 | 96.8 | 0.0 |
| Sonar | **80.8** | 79.8 | −1.0 |
| **Tic-Tac-Toe** | 99.2 | **99.4** | **+0.2** ⭐ |
| **Vehicle** | 68.2 | **68.3** | **+0.1** |
| Waveform | 81.6 | 81.6 | 0.0 |
| Wine | **96.7** | 96.2 | −0.5 |
| Zoo | 96.5 | 96.5 | 0.0 |
| **Avg** | **85.3** | 85.1 | **−0.2** |

### perClass k=5 thắng: 4 dataset

- **Breast-Cancer** (+0.3), **Crx** (+0.2), **Tic-Tac-Toe** (+0.2), **Vehicle** (+0.1)
- 13 dataset hòa, 9 dataset thua

→ Per-class fairness giúp **dataset binary class với rule conflict** (Breast-Cancer, Crx, Tic-Tac-Toe).

→ Mất nhiều trên dataset multi-class với class imbalance (Lymphography 4 class, Iono).

---

## 3. Phân tích — Tại sao avgVote thất bại?

avgVote = chia tổng vote cho **số luật** mỗi class:

```
Class A: 100 rules, sum=500  →  avg = 5.0
Class B: 2 rules,   sum=8    →  avg = 4.0
→ avgVote chọn A (5.0 > 4.0)
```

Vấn đề: avgVote **bỏ thông tin "có bao nhiêu luật đồng ý"**. 100 luật cùng nói A đáng tin hơn 2 luật nói B.

→ Sum vote (chuẩn CMAR) đúng về mặt **statistical evidence accumulation**.

---

## 4. So sánh CHÉO tất cả các thí nghiệm voting đã thử

| Strategy | Avg k=0 | Δ Default |
|---|---:|---:|
| 🥇 **Default CMAR** (sum χ², global k) | **85.3%** | — |
| **Lift weight** | 85.2% | −0.1 |
| **HM weight** | 85.2% | −0.1 |
| perClass k=5 (sum χ²) | 85.1%* | +0.3 vs k=5 default |
| Default top-k=5 | 84.8% | −0.5 |
| Default top-k=3 | 84.7% | −0.6 |
| **avgVote** | 82.1% | **−3.2** |
| perClass + avgVote combo | 82.8% | −2.5 |

> *perClass k=5 chạy với topK=5, so sánh fair: 85.1 > 84.8 = +0.3.

→ Tất cả thí nghiệm voting **không vượt được Default topK=0** (85.3%). 

→ Nhưng **perClass k=5 cải thiện rõ rệt** trên cùng giá trị k = một option đáng cân nhắc khi cần predict nhanh (top-k thấp).

---

## 5. Khuyến nghị

| Mục tiêu | Cấu hình |
|---|---|
| 🎯 Tối đa accuracy | **Default topK=0** (85.3%) |
| ⚡ Cân bằng acc/speed | **perClass k=5** (85.1%, predict nhanh) |
| 🟡 Lift voting tốt cho rule-noisy | `--liftWeight --topK=0` |
| ❌ TRÁNH | avgVote (mọi cấu hình) |

### Combo có thể thử thêm

- `--perClassTopK=7 --liftWeight` — kết hợp 2 ý best
- `--perClassTopK=10` — k cao hơn xem có gần được default k=0 không

---

## File dữ liệu

| File | Cấu hình |
|---|---|
| [summary-report-topk0.md](summary-report-topk0.md) | Default (baseline) |
| [summary-report-avgvote-topk0.md](summary-report-avgvote-topk0.md) | avgVote k=0 |
| [summary-report-avgvote-topk7.md](summary-report-avgvote-topk7.md) | avgVote k=7 |
| [summary-report-perclass-k3.md](summary-report-perclass-k3.md) | perClass k=3 |
| [summary-report-perclass-k5.md](summary-report-perclass-k5.md) | **perClass k=5** ⭐ |
| [summary-report-perclass-k5-avg.md](summary-report-perclass-k5-avg.md) | perClass k=5 + avgVote |

## Tái tạo

```powershell
# perClass best
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --perClassTopK=5

# avgVote tests
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --avgVote --topK=0
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --avgVote --topK=7
```
