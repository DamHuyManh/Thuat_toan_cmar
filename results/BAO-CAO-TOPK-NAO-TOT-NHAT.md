# So sánh các top-k: top-k nào tốt nhất?

> Phân tích trên 26 dataset UCI, 10-fold CV. Chỉ so sánh giữa **4 cấu hình top-k của Improved**: k=0 (all), k=3, k=5, k=7.

## TL;DR — Kết luận nhanh

| Hạng | Cấu hình | Avg Accuracy | Ghi chú |
|:---:|---|---:|---|
| 🥇 1 | **topK = 0** (all rules) | **85.3%** | Voting toàn bộ → bằng chứng nhiều nhất |
| 🥈 2 | **topK = 7** | **85.1%** | Chỉ kém k=0 0.2%, predict nhanh hơn |
| 🥉 3 | topK = 5 | 84.8% | |
| 4 | topK = 3 | 84.7% | Cắt quá sâu → mất thông tin |

**→ Tốt nhất về accuracy: `topK = 0`. Tốt nhất về cân bằng accuracy/tốc độ: `topK = 7`.**

---

## 1. Xếp hạng theo độ chính xác trung bình

```
topK=3   84.7% ████████████████████▏
topK=5   84.8% ████████████████████▏
topK=7   85.1% ████████████████████▌
topK=0   85.3% ████████████████████▋   ← best
```

**Xu hướng**: k càng lớn → accuracy càng cao (đơn điệu).

| k → | 3 | 5 | 7 | 0 (all) |
|---|---:|---:|---:|---:|
| Avg | 84.7 | 84.8 | 85.1 | **85.3** |
| Δ vs k=0 | −0.6 | −0.5 | −0.2 | 0 |

---

## 2. Đếm dataset thắng (per-dataset winner)

Số dataset mà mỗi cấu hình **đạt accuracy cao nhất** (kể cả hòa):

| Cấu hình | Thắng / hòa hạng nhất | Thắng độc nhất | % |
|---|---:|---:|---:|
| **topK = 0** | **20 / 26** | 11 | 77% |
| topK = 7 | 11 / 26 | 2 | 42% |
| topK = 3 | 8 / 26 | 2 | 31% |
| topK = 5 | 6 / 26 | 1 | 23% |

→ **topK=0 thắng trên 77% dataset**, gấp đôi tỷ lệ topK=7.

---

## 3. Các dataset mà top-k > 0 **thắng** top-k = 0

Đây là những trường hợp cắt bớt luật giúp **giảm nhiễu** và tăng accuracy:

| Dataset | k=0 | Best k>0 | Cải thiện |
|---|---:|---:|---:|
| Auto | 81.4 | **81.6** (k=5) | +0.2 |
| Glass | 70.0 | **70.4** (k=3) | +0.4 |
| Iono | 92.6 | **92.9** (k=7) | +0.3 |
| Iris | 92.7 | **93.3** (k=3) | +0.6 |
| Lymphography | 83.4 | **85.3** (k=7) | **+1.9** ⭐ |
| Tic-Tac-Toe | 99.2 | **99.4** (k=5/7) | +0.2 |

**Pattern**: dataset có ít class hoặc dataset có nhiều luật yếu (rule-noisy) thì top-k vừa phải giúp ích.

---

## 4. Các dataset mà top-k = 0 **vượt trội** rõ rệt

Những dataset cần *nhiều* luật để vote chính xác — cắt top-k làm giảm accuracy mạnh:

| Dataset | k=0 | k=3 | Mất khi cắt |
|---|---:|---:|---:|
| Labor | **91.7** | 83.0 | **−8.7** 🔴 |
| Hepatitis | **83.3** | 81.4 | −1.9 |
| Wine | **96.7** | 95.6 | −1.1 |
| Heart | **80.7** | 79.6 | −1.1 |

**Labor**: dataset nhỏ (57 mẫu) → mỗi luật vote đều quan trọng, cắt sâu = mất tín hiệu mạnh.

---

## 5. Bảng so sánh đầy đủ (Accuracy %, chỉ giữa các top-k)

| Dataset | k=0 | k=3 | k=5 | k=7 | Best |
|---|---:|---:|---:|---:|:---:|
| Anneal | **98.2** | 97.9 | 97.7 | 97.7 | k=0 |
| Australian | **86.8** | 86.2 | 86.4 | 86.2 | k=0 |
| Auto | 81.4 | 79.7 | **81.6** | 81.0 | k=5 |
| Breast-Cancer | **97.1** | 96.9 | 96.9 | **97.1** | k=0=k=7 |
| Cleve | **82.6** | 81.9 | 81.6 | 82.3 | k=0 |
| Crx | **86.1** | 85.5 | 86.0 | **86.1** | k=0=k=7 |
| Diabetes | **73.4** | 73.3 | 73.3 | 73.3 | k=0 |
| German | **72.9** | 72.3 | 72.2 | 72.3 | k=0 |
| Glass | 70.0 | **70.4** | 70.0 | 70.0 | k=3 |
| Heart | **80.7** | 79.6 | 79.3 | 80.4 | k=0 |
| Hepatitis | **83.3** | 81.4 | 80.8 | 82.0 | k=0 |
| Horse | **82.3** | **82.3** | 81.5 | **82.3** | k=0=k=3=k=7 |
| Hypo | 97.9 | 97.9 | 97.9 | 97.9 | tie |
| Iono | 92.6 | 92.3 | 91.7 | **92.9** | k=7 |
| Iris | 92.7 | **93.3** | 92.7 | 92.7 | k=3 |
| Labor | **91.7** | 83.0 | 86.3 | 88.3 | k=0 |
| Led7 | 72.2 | 72.2 | 72.2 | 72.2 | tie |
| Lymphography | 83.4 | 84.0 | 84.7 | **85.3** | k=7 |
| Pima | **73.4** | 73.3 | 73.3 | 73.3 | k=0 |
| Sick | 96.8 | 96.8 | 96.8 | 96.8 | tie |
| Sonar | **80.8** | **80.8** | 80.7 | 80.3 | k=0=k=3 |
| Tic-Tac-Toe | 99.2 | 99.3 | **99.4** | **99.4** | k=5=k=7 |
| Vehicle | **68.2** | 68.1 | 68.1 | 68.1 | k=0 |
| Waveform | **81.6** | 81.5 | 81.5 | **81.6** | k=0=k=7 |
| Wine | **96.7** | 95.6 | 96.2 | 96.2 | k=0 |
| Zoo | 96.5 | 96.5 | 96.5 | 96.5 | tie |
| **Avg** | **85.3** | 84.7 | 84.8 | 85.1 | k=0 |

---

## 6. Khuyến nghị cuối

| Mục tiêu | Chọn |
|---|---|
| 🎯 Accuracy tối đa | **`topK = 0`** (paper-faithful) |
| ⚡ Cân bằng accuracy & tốc độ predict | **`topK = 7`** |
| 🚀 Predict nhanh, chấp nhận giảm ~0.5% | `topK = 3` hoặc `topK = 5` |
| ❌ Tránh | `topK ≤ 2` — voting dễ bị 1 luật outlier chi phối |

### Quy luật thực nghiệm

- **k=0 thắng 77% dataset** → đây là lựa chọn an toàn nhất nếu chưa biết dataset.
- Mỗi lần tăng k thêm 2 (3→5→7), accuracy tăng ~0.1–0.3%.
- Từ k=7 đến k=∞ (all), chỉ còn cải thiện ~0.2%.

→ **Nếu phải chọn 1 giá trị k cố định: chọn `topK = 7`** (chỉ kém best 0.2% nhưng vẫn predict nhanh).
