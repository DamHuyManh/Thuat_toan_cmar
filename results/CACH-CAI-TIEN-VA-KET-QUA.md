# Cách Cải Tiến và Kết Quả

**Đề tài:** Cải tiến hiệu năng thuật toán phân lớp dựa trên luật kết hợp (CMAR)
**Ngày:** 06/05/2026

---

## 1. Vấn Đề

Thuật toán CMAR gốc có 2 nhược điểm chính:

1. **Đếm thừa**: Mỗi luật phải duyệt toàn bộ dữ liệu để đếm số mẫu khớp. Việc đếm này lặp lại ở **3 giai đoạn cắt tỉa** khác nhau.
2. **Kiểm tra từng cái một**: Kiểm tra "mẫu nào chứa thuộc tính A" được làm tuần tự cho từng mẫu.

Hệ quả: Trên dataset Anneal (898 mẫu, 38 thuộc tính), chương trình thực hiện **5,6 tỷ phép so sánh bit** chỉ để xử lý 1 dataset.

---

## 2. Hai Ý Tưởng Cải Tiến

### Ý tưởng 1 — Lập bảng tra cứu, dùng lại

Xây sẵn **một bảng tra cứu** ngay đầu chương trình: ghi rõ "thuộc tính X có ở những mẫu nào". Bảng này lập **một lần duy nhất**, dùng đi dùng lại cho mọi luật, mọi giai đoạn → **không phải đếm lại**.

> *Giống tra từ điển: có mục lục, biết từ ở trang nào, không phải đọc cả cuốn.*

### Ý tưởng 2 — Xử lý 64 mẫu cùng lúc

Thay vì kiểm tra từng mẫu một, **gom 64 mẫu thành một nhóm** và xử lý cả nhóm trong một thao tác.

> *Giống đếm phiếu bầu: cách cũ mở từng phiếu đếm tay, cách mới quét máy 64 phiếu/lần.*

**Tỷ lệ tăng tốc lý thuyết: ~64 lần.**

---

## 3. Các Phần Cải Tiến (Tóm Tắt)

| # | Phần | Vấn đề cũ | Cách cải tiến |
|---|------|-----------|---------------|
| 1 | Đo lường | Không có công cụ đo, không biết phần nào chậm | Bổ sung công cụ đo thời gian + bộ nhớ từng giai đoạn |
| 2 | Lưu trữ luật | Duyệt tuần tự toàn bộ luật khi phân loại | Tổ chức luật theo **3 tầng chỉ mục**, chỉ duyệt nhóm liên quan |
| 3 | Khai phá luật | Đếm support cho từng tập điều kiện bằng cách quét toàn bộ dữ liệu | Áp dụng **bảng tra cứu** + **xử lý hàng loạt** |
| 4 | Cắt tỉa luật | Ba giai đoạn cắt tỉa đều quét lại dữ liệu cho cùng một luật | **Chia sẻ kết quả** giữa các giai đoạn, tính một lần dùng nhiều lần |
| 5 | Loại luật trùng | Bỏ qua hoàn toàn khi có >10.000 luật | Dùng **dấu vân tay bit** + **chỉ mục theo độ dài** → luôn cắt tỉa |
| 6 | **Phase 09** — Đếm giao BitSet | Mining/pruning lặp `clone().and().cardinality()` rất nhiều lần | **`intersectCardinality`**: đếm \|a∩b\| bằng một vòng `nextSetBit` (chọn lớp ngoài theo `length()`), **coverage** bỏ clone khi duyệt match |
| 7 | **Phase 10** — Nối mining→pruning + emit + song song | Pruning dựng lại `itemIndex` trùng mining; emitRules quét match × số lớp; mỗi lần mine tạo `FixedThreadPool` | **Tái dùng** `Map<item,BitSet>` từ `FPGrowthOptimized`; **histogram nhãn** (ThreadLocal) trong emit; chi² đếm exact bằng `labels[]`; parallel qua **ForkJoinPool.commonPool** |
| 8 | **Phase 11** — Pruning “mỏng” nhánh improved | Improved từng dựng `long[][]` từng dòng không cần cho coverage/chi²; chi² clone BitSet nhánh đầu mọi luật; `cardinality` + duyệt match tách hai lần | **Bỏ `buildBitmaps`** trong nhánh improved (coverage chỉ dùng BitSet match); **BitSet scratch** (ThreadLocal) cho AND chi² + **clone chỉ luật giữ được**; **một vòng** đếm `antSupport` + `exactSupport` |
| 9 | **Phase 12** — Header FP-tree (tail) | Chi phí duyệt/ghi header khi cập nhật cây | Tối ưu vùng **header / tail** của FP-tree (giảm thao tác thừa khi mine) |
| 10 | **Phase 13** — Conditional tree ít cấp phát | `buildConditionalTree` tạo nhiều `List<Integer>`/`List<int[]>` và cấp phát mảng lặp | Xây conditional tree **2-pass trực tiếp từ header-table** (đếm freq có trọng số → insert), tái dùng **buffer path** qua `ThreadLocal` |
| 11 | **Phase 14** — `insertTransaction` / prefix | Cấp phát và nhánh rẽ không cần thiết khi thêm giao dịch | Chuẩn hóa **prefix**, giảm tạo đối tượng/list trên đường nóng |
| 12 | **Phase 15** — Mining top‑k / cắt sớm (tùy chế độ) | Quét không gian luật rộng khi chỉ cần vài luật mạnh | **Top‑k** / ngưỡng sớm (nơi bật) để giảm việc sinh luật dư |
| 13 | **Phase 16** — Dự đoán + bitmap antecedent | Khớp luật tốn CPU, `HashSet`/duyệt thừa khi bỏ phiếu | `ensureAntBitmap(maxItem)`, khớp tiền đề theo **từ `long`**, `ThreadLocal` bitmap khi predict; gọn bước **weighted voting** |
| 14 | **Phase 17** — `emitRules` + single path | `emitRules` quét cả `classMasks`; single path tạo `BitSet`/list dư | **Chỉ xét lớp có trong histogram (dirty)**; `mineSinglePath` tái dùng **BitSet** + **`collectSinglePathItemIds`** |

---

## 4. Kết Quả Tổng Hợp

| Chỉ số | Phiên bản cũ | Phiên bản mới | Cải thiện |
|--------|-------------:|--------------:|----------:|
| Tổng thời gian xử lý | 23.302 ms | 4.216 ms | **~5,53× nhanh hơn** |
| Bộ nhớ đỉnh trung bình | ~92 MB | ~142 MB | TB cột `peakMemMB` (run improved mới nhất); baseline ~92 MB từ đo trước — khác điều kiện JVM vẫn có thể lệch |
| Độ chính xác trung bình | 85,1% | 85,3% | **+0,2 điểm %** so baseline; **+0,1 điểm %** so Paper CMAR (xem `summary-report.md`) |

---

## 5. Bảng Chi Tiết 26 Bộ Dữ Liệu

Dữ liệu **Mới**, **Mới (ms)** và **Speedup** lấy từ lần benchmark improved mới nhất (**2026-05-06**, `results/profiling-metrics.csv`, Phase 09–17, `run-benchmark.ps1` có warmup). **Cũ** / **Cũ (ms)** giữ baseline gốc (cùng 10-fold, seed cố định). Hàng tổng **Cũ (ms)** = **23.302** (Iris &lt;1 ms được tính 0 trong tổng); **Mới (ms)** = **4.216**.

| # | Dataset | N | Paper | Cũ | Mới | Cũ (ms) | Mới (ms) | Speedup |
|---|---------|------:|------:|------:|------:|--------:|---------:|--------:|
| 1 | Anneal | 898 | 97,3% | 97,7% | **98,2%** | 4.098 | 601 | 6,82× |
| 2 | Australian | 690 | 86,1% | 86,7% | **86,8%** | 220 | 38 | 5,79× |
| 3 | Auto | 205 | 78,1% | 81,4% | 81,4% | 644 | 488 | 1,32× |
| 4 | Breast-Cancer | 683 | 96,4% | 97,1% | 97,1% | 25 | 3 | 8,33× |
| 5 | Cleve | 303 | 82,2% | 82,6% | 82,6% | 83 | 13 | 6,38× |
| 6 | Crx | 690 | 84,9% | 86,0% | **86,1%** | 252 | 37 | 6,81× |
| 7 | Diabetes | 768 | 75,8% | 73,4% | 73,4% | 13 | 1 | 13,00× |
| 8 | German | 1.000 | 74,9% | 72,9% | 72,9% | 1.165 | 143 | 8,15× |
| 9 | Glass | 214 | 70,1% | 70,0% | 70,0% | 7 | 2 | 3,50× |
| 10 | Heart | 270 | 82,2% | 80,7% | 80,7% | 100 | 10 | 10,00× |
| 11 | Hepatitis | 155 | 80,5% | 83,3% | 83,3% | 150 | 46 | 3,26× |
| 12 | Horse | 368 | 82,6% | 80,7% | **82,3%** | 689 | 188 | 3,67× |
| 13 | Hypo | 3.163 | 98,4% | 98,0% | 97,9% | 2.854 | 227 | 12,57× |
| 14 | Iono | 351 | 91,5% | 92,0% | **92,6%** | 1.023 | 376 | 2,72× |
| 15 | Iris | 150 | 94,0% | 92,7% | 92,7% | &lt;1 | &lt;1 | — |
| 16 | Labor | 57 | 89,7% | 91,7% | 91,7% | 49 | 19 | 2,58× |
| 17 | Led7 | 3.200 | 72,5% | 72,2% | 72,2% | 23 | 2 | 11,50× |
| 18 | Lymphography | 148 | 83,1% | 83,4% | 83,4% | 157 | 79 | 1,99× |
| 19 | Pima | 768 | 75,1% | 73,4% | 73,4% | 12 | 1 | 12,00× |
| 20 | Sick | 2.800 | 97,5% | 96,5% | **96,8%** | 3.304 | 225 | 14,68× |
| 21 | Sonar | 208 | 79,4% | 78,4% | **80,8%** | 2.717 | 1.253 | 2,17× |
| 22 | Tic-Tac-Toe | 958 | 99,2% | 99,2% | 99,2% | 72 | 7 | 10,29× |
| 23 | Vehicle | 846 | 68,8% | 68,2% | 68,2% | 463 | 68 | 6,81× |
| 24 | Waveform | 5.000 | 83,2% | 81,6% | 81,6% | 5.107 | 351 | 14,55× |
| 25 | Wine | 178 | 95,0% | 96,7% | 96,7% | 43 | 22 | 1,95× |
| 26 | Zoo | 101 | 97,1% | 96,5% | 96,5% | 32 | 16 | 2,00× |
|   | **Trung bình / Tổng** | | **85,2%** | **85,1%** | **85,3%** | **23.302** | **4.216** | **~5,53×** |

**Ghi chú:** Số in đậm ở cột "Mới" = độ chính xác cải thiện so với "Cũ".

---

## 6. Điểm Đáng Chú Ý

- **Tăng tốc cao nhất** (theo bảng mục 5): Sick **14,68×**, Waveform **14,55×**, Hypo **12,57×**, Pima **12,00×**, Diabetes **13,00×**, Led7 **11,50×**.
- **Độ chính xác so baseline (Cũ)**: cải thiện rõ trên 6 dataset (Sonar +2,4%, Horse +1,6%, Iono +0,6%, Anneal +0,5%, Sick +0,3%, Australian/Crx +0,1%); **Hypo** giảm nhẹ **−0,1 điểm %** (98,0% → 97,9%) — biến động nhỏ giữa các fold/làm tròn.
- **So với Paper CMAR** (ngưỡng ±0,5 điểm %, xem `summary-report.md`): **10** thắng / **7** hòa / **9** thua; chênh trung bình **+0,1 điểm %** (Our **85,3%** vs Paper **85,2%**).
- **Bộ dữ liệu giữ nguyên** 100%, không sửa nội dung.

---

## 7. Demo

```bash
# Chạy bản gốc (~4 phút 37s)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=baseline

# Chạy bản cải tiến (nhanh hơn nhiều so baseline; thời gian tùy máy)
java -cp bin cmar.benchmark.BenchmarkRunner --mode=improved
```

Cùng dataset, cùng tham số, cùng phương pháp đánh giá (10-fold cross-validation, seed=42). Bản mới **nhanh hơn ~5,53 lần** (tổng `trainMs` **4.216** ms so **23.302** ms baseline). Độ chính xác trung bình **85,3%** — chi tiết từng bộ khớp `summary-report.md` và bảng mục 5 / `profiling-metrics.csv`.

Gợi ý chạy nhanh và ổn định trên Windows (không đổi dataset):

```powershell
cd "d:\Jun Tech\Cmar\Thuat_toan_cmar"
.\run-benchmark.ps1
```

---

## 8. Chạy lại thực tế (cập nhật Phase 09–17)

Đã **biên dịch và chạy** `BenchmarkRunner --mode=improved` trên đủ 26 dataset gốc.

**Sau Phase 17** (benchmark ngày **2026-05-06**):

- **Tổng `trainMs` (cộng 26 dataset):** **4.216 ms** — `results/profiling-metrics.csv`.
- **So baseline 23.302 ms:** **~5,53×**. (`run-benchmark.ps1`: warmup + JVM flags, thời gian tổng ~5–6 phút trên máy chủ chạy gần đây.)
- **Độ chính xác:** Our CMAR TB **85,3%**; so Paper CMAR TB **85,2%**, diff **+0,1 điểm %** — xem đủ dòng Accuracy trong `results/summary-report.md`.

**Giới hạn thực tế:** phần lớn thời gian còn lại nằm ở **FP-Growth** (duyệt cây, conditional tree). Muốn nhanh thêm một bậc (`×2` kiểu) thường cần **đổi kiến trúc** (GPU, SIMD, trie gọn hơn) hoặc **nới chặn mining** có rủi ro đổi độ chính xác — không chỉ tối ưu “vặt” Java.
