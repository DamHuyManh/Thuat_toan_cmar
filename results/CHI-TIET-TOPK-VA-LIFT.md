# Chi tiết từng cấu hình Top-k và Lift đã chạy

> Liệt kê **chi tiết từng thí nghiệm**: cấu hình, command, kết quả accuracy, file output, phân tích.

---

## 🎯 Top-k voting (Chi² weight — chuẩn paper)

Đổi **bao nhiêu luật được tính vào voting** khi predict.

---

### 1. `topK = 0` — Voting TẤT CẢ luật khớp (paper-faithful) ⭐

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=0` |
| **Mô tả** | Sum trọng số χ² của **tất cả** luật khớp instance |
| **Avg Accuracy** | **85.3%** ⭐ Cao nhất |
| **Δ vs Paper** | **+0.1%** (vượt paper) |
| **Δ vs Baseline** | **+0.8%** |
| **DS thắng paper** | 14/26 |
| **File kết quả** | [summary-report-topk0.md](summary-report-topk0.md) |

**Đặc điểm**:
- Mặc định theo bài báo CMAR 2001 — vote tất cả luật, mỗi luật đóng góp χ²
- Predict chậm hơn (sum hàng trăm luật) nhưng accuracy ổn định
- **Thắng/hòa trên 20/26 dataset** so với các giá trị k khác

**Top dataset thắng paper rõ**:
| Dataset | Our | Paper | Δ |
|---|---:|---:|---:|
| Auto | 81.4 | 78.1 | **+3.3** ⭐ |
| Hepatitis | 83.3 | 80.5 | **+2.8** ⭐ |
| Labor | 91.7 | 89.7 | **+2.0** |
| Wine | 96.7 | 95.0 | +1.7 |
| Sonar | 80.8 | 79.4 | +1.4 |

---

### 2. `topK = 3` — Chỉ vote 3 luật top

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=3` |
| **Mô tả** | Sort luật khớp theo CMAR order, chỉ lấy 3 đầu vote |
| **Avg Accuracy** | **84.7%** |
| **Δ vs Paper** | −0.5% |
| **Δ vs k=0** | **−0.6%** |
| **DS thắng paper** | 11/26 |
| **File kết quả** | [summary-report-topk3.md](summary-report-topk3.md) |

**Đặc điểm**:
- Predict nhanh (chỉ cộng 3 weight)
- Mất thông tin nhiều khi dataset cần nhiều luật để vote đúng
- **Thua mạnh trên dataset nhỏ**: Labor 91.7 → **83.0** (mất 8.7%!), Hepatitis 83.3 → 81.4

**Khi nào dùng**:
- Predict cực nhanh, chấp nhận giảm 0.5% accuracy
- Tránh trên dataset < 200 mẫu

---

### 3. `topK = 5` — Vote 5 luật top

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=5` |
| **Avg Accuracy** | **84.8%** |
| **Δ vs Paper** | −0.4% |
| **Δ vs k=0** | −0.5% |
| **DS thắng paper** | 12/26 |
| **File kết quả** | [summary-report-topk5.md](summary-report-topk5.md) |

**Đặc điểm**:
- Tốt hơn k=3 một chút (+0.1% trung bình)
- **Auto thắng** k=0 ở đây: 81.4 → **81.6** (k=5 best)
- Tic-Tac-Toe đạt 99.4% (cao hơn k=0: 99.2)

---

### 4. `topK = 7` — Vote 7 luật top 🥈

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --topK=7` |
| **Avg Accuracy** | **85.1%** 🥈 |
| **Δ vs Paper** | −0.1% |
| **Δ vs k=0** | **−0.2%** (chỉ kém chút) |
| **DS thắng paper** | 13/26 |
| **File kết quả** | [summary-report-topk7.md](summary-report-topk7.md) |

**Đặc điểm**:
- **Cân bằng tốt nhất giữa accuracy và speed**
- Chỉ kém k=0: 0.2% nhưng predict nhanh hơn nhiều
- **Iono** thắng k=0: 92.6 → **92.9** 
- **Lymphography** thắng cực mạnh k=0: 83.4 → **85.3** (+1.9%) ⭐

---

### 📊 So sánh 4 giá trị top-k

```
k=3  84.7%  ████████████████████▏
k=5  84.8%  ████████████████████▏
k=7  85.1%  ████████████████████▌  🥈
k=0  85.3%  ████████████████████▋  🥇
```

→ **Xu hướng**: k tăng → accuracy tăng đều đặn. Best là k=0 (all rules).

---

## 🔄 Lift voting weight — Đổi trọng số voting

Thay vì dùng **χ²** (chuẩn CMAR), dùng **Lift** làm trọng số khi sum vote.

**Lift** = `Supp(X→c) · N / (Supp(X) · Supp(c))`
- Lift > 1 = tương quan thuận → đóng góp nhiều
- Lift ≈ 1 = ngẫu nhiên → đóng góp ít

---

### 5. `Lift voting + topK = 0`

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftWeight --topK=0` |
| **Mô tả** | Sort theo CMAR (giữ), nhưng voting weight = Lift |
| **Avg Accuracy** | **85.2%** |
| **Δ vs Paper** | +0.0% (bằng paper) |
| **Δ vs Default k=0** | −0.1% |
| **File kết quả** | [summary-report-liftweight-topk0.md](summary-report-liftweight-topk0.md) |

**Dataset HƯỞNG LỢI** khi đổi từ χ² → Lift:

| Dataset | χ² | Lift | Gain |
|---|---:|---:|---:|
| **Hepatitis** | 83.3 | **84.8** | **+1.5** ⭐⭐ |
| **Auto** | 81.4 | **82.5** | **+1.1** ⭐ |
| **Iono** | 92.6 | **93.2** | +0.6 |
| **Sonar** | 80.8 | **81.3** | +0.5 |
| **Breast-Cancer** | 97.1 | **97.4** | +0.3 |

**Dataset BỊ TỔN** khi đổi sang Lift:

| Dataset | χ² | Lift | Loss |
|---|---:|---:|---:|
| Lymphography | 83.4 | 82.0 | −1.4 |
| Horse | 82.3 | 81.0 | −1.3 |
| Wine | 96.7 | 95.6 | −1.1 |
| Zoo | 96.5 | 95.6 | −0.9 |

**Phát hiện**:
- **Lift voting tốt cho dataset rule-noisy** (Auto, Hepatitis, Iono, Sonar) — Lift trừng phạt mạnh các luật tương quan yếu
- **Tệ trên dataset balanced** (Wine, Zoo) — chi² weight tự nhiên hợp hơn
- **Tệ với dataset có outlier Lift** (Lymphography có max lift = 67) — 1 luật rare lift cực cao chi phối vote

---

### 6. `Lift voting + topK = 7`

| | |
|---|---|
| **Command** | `java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved --liftWeight --topK=7` |
| **Avg Accuracy** | **85.2%** |
| **Δ vs Paper** | +0.0% |
| **File kết quả** | [summary-report-liftweight-topk7.md](summary-report-liftweight-topk7.md) |

**Đặc điểm**:
- Bằng Lift k=0 trên trung bình
- **Iono** đạt cao nhất trong tất cả cấu hình: **93.4%**
- Predict nhanh hơn Lift k=0

---

## 📈 So sánh chéo Top-k × Voting Weight

| Cấu hình | Avg Acc | Đặc điểm |
|---|---:|---|
| 🥇 **k=0 + χ² (default)** | **85.3%** | Best tổng thể — paper-faithful |
| 🥈 **k=7 + χ²** | 85.1% | Cân bằng acc/speed |
| **Lift k=0** | 85.2% | Tốt cho rule-noisy DS |
| **Lift k=7** | 85.2% | Combo speed + Lift |
| k=5 + χ² | 84.8% | |
| k=3 + χ² | 84.7% | |

---

## 📊 Per-dataset best configuration

Dataset nào hợp với cấu hình nào?

| Dataset | Best cấu hình | Acc |
|---|---|---:|
| **Auto** | Lift k=0 | **82.5** |
| **Hepatitis** | Lift k=0 | **84.8** |
| **Iono** | Lift k=7 | **93.4** |
| **Sonar** | Default k=0 | **80.8** |
| **Breast-Cancer** | Lift k=0 | **97.4** |
| **Anneal** | Default k=0 | **98.2** |
| **Lymphography** | Default k=7 | **85.3** |
| **Tic-Tac-Toe** | Default k=5/k=7 | **99.4** |
| **Wine** | Default k=0 | **96.7** |
| **Labor** | Default k=0 | **91.7** |

→ Không có cấu hình nào **toàn năng**. **Default k=0** phù hợp 75% dataset, **Lift** phù hợp các dataset rule-noisy.

---

## 🎯 Khuyến nghị áp dụng

| Tình huống thực tế | Cấu hình | Command |
|---|---|---|
| Default tổng quát | **k=0 + χ²** | `--topK=0` |
| Predict nhanh, gần như giữ acc | **k=7 + χ²** | `--topK=7` |
| Dataset có nhiều luật yếu (rule-noisy) | **Lift k=0** | `--liftWeight --topK=0` |
| Dataset nhỏ < 200 mẫu | **BẮT BUỘC k=0** | `--topK=0` |
| Tránh hoàn toàn | k ≤ 2 | — |

---

## 📁 File output đã tạo

| File | Cấu hình | Avg |
|---|---|---:|
| [summary-report-baseline.md](summary-report-baseline.md) | Baseline (CMAR gốc) | 84.5% |
| [summary-report-topk0.md](summary-report-topk0.md) | k=0 + χ² | **85.3%** |
| [summary-report-topk3.md](summary-report-topk3.md) | k=3 + χ² | 84.7% |
| [summary-report-topk5.md](summary-report-topk5.md) | k=5 + χ² | 84.8% |
| [summary-report-topk7.md](summary-report-topk7.md) | k=7 + χ² | 85.1% |
| [summary-report-liftweight-topk0.md](summary-report-liftweight-topk0.md) | k=0 + Lift | 85.2% |
| [summary-report-liftweight-topk7.md](summary-report-liftweight-topk7.md) | k=7 + Lift | 85.2% |

**Mỗi file** chứa:
- Bảng accuracy 26 dataset (so với Paper/CBA/C4.5)
- Bảng performance metrics (train ms, predict ms, rules count)
- Bảng tham số dùng

---

## Tổng kết — 1 câu trả lời cho mỗi câu hỏi

| Câu hỏi | Trả lời |
|---|---|
| Top-k nào tốt nhất tổng thể? | **k=0** (85.3%) |
| Top-k nào cân bằng acc/speed? | **k=7** (85.1%) |
| Lift voting có hữu ích không? | **Có**, cho dataset rule-noisy (+0.5–1.5%) |
| k tăng có luôn tốt hơn không? | **Có**, monotonic trên trung bình |
| Dataset nào hưởng lợi nhất từ Lift voting? | **Hepatitis (+1.5%), Auto (+1.1%)** |
| Dataset nào CỰC NHẠY với top-k? | **Labor** (range 8.7%), **Hepatitis** (2.5%) |
| Dataset nào KHÔNG quan tâm top-k? | Hypo, Sick, Zoo, Tic-Tac-Toe, Vehicle, Waveform... (11/26) |
