# CMAR — Tóm tắt ngắn gọn

## 🎯 Kết quả chính

| Cấu hình | Avg Acc | Δ vs Paper |
|---|---:|---:|
| Paper CMAR 2001 | 85.2% | — |
| Baseline (gốc) | 84.5% | −0.7% |
| **Improved** ⭐ | **85.3%** | **+0.1%** |
| Lift voting | 85.2% | 0.0% |

→ **Cải tiến vượt paper +0.1%**, thắng paper 14/26 dataset.

---

## ⚙️ Pipeline hiện tại

```
1. MINING  : FP-Growth optimized
2. PRUNING : Chi² → G2S → Coverage (3 tầng)
3. SORT    : conf DESC → sup DESC → length ASC
4. VOTING  : Sum χ² của TẤT CẢ luật khớp
```

---

## 📊 Top 5 cải thiện (Improved vs Baseline)

| Dataset | Baseline | Improved | Δ |
|---|---:|---:|---:|
| **Labor** | 83.0 | **91.7** | **+8.7** 🟢🟢🟢 |
| **Sonar** | 76.5 | **80.8** | **+4.3** 🟢🟢🟢 |
| **Hepatitis** | 81.4 | **83.3** | +1.9 |
| **Auto** | 79.7 | **81.4** | +1.7 |
| **Horse** | 80.9 | **82.3** | +1.4 |

---

## 🟡 Lift voting tốt cho dataset rule-noisy

| Dataset | Chi² (default) | **Lift voting** | Gain |
|---|---:|---:|---:|
| Hepatitis | 83.3 | **84.8** | +1.5 ⭐ |
| Auto | 81.4 | **82.5** | +1.1 ⭐ |
| Iono | 92.6 | **93.2** | +0.6 |
| Sonar | 80.8 | **81.3** | +0.5 |
| Breast-Cancer | 97.1 | **97.4** | +0.3 |

---

## ❌ Các hướng đã thử nhưng KHÔNG dùng

| Hướng | Lý do |
|---|---|
| Sort theo Lift (`--liftSort`) | Phá coverage prune → −3% |
| Sort theo HM (`--hmLift`) | Phá coverage prune → −9% |
| Filter Lift ≥ 1 | Redundant (chi² đã làm) |
| Top-k = 3/5 voting | Thua topK=0 0.5–0.6% |
| avgVote (chia thay sum) | Mất thông tin → −3% |

---

## ✅ Khuyến nghị

| Mục tiêu | Cấu hình |
|---|---|
| 🎯 Mặc định (accuracy max) | `--mode=improved --topK=0` |
| 🟡 Rule-noisy dataset | `--mode=improved --liftWeight --topK=0` |
| ⚡ Predict nhanh | `--mode=improved --topK=7` (kém k=0: 0.2%) |

---

## 📁 File quan trọng

- [BAO-CAO-SO-SANH-PAPER-BASE-IMPROVED.md](BAO-CAO-SO-SANH-PAPER-BASE-IMPROVED.md) — báo cáo chi tiết 9 section
- [summary-report.md](summary-report.md) — bảng acc + perf 26 dataset
- [rules/](rules/) — chi tiết từng luật + Lift (CSV)

---

## 💬 1 câu cho luận văn

> *"Triển khai CMAR theo paper Li-Han-Pei 2001 với 17 phase tối ưu hiệu năng, đạt accuracy trung bình **85.3%** trên 26 dataset UCI (10-fold CV), vượt paper +0.1% và cao hơn baseline +0.8%. Lift voting là phương án thay thế hữu ích cho dataset rule-noisy (Hepatitis +1.5%, Auto +1.1%)."*
