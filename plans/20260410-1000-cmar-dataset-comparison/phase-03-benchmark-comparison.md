# Phase 3: Benchmark & So sánh

## Mục tiêu
Chạy lại CMAR trên 26/26 dataset (đã fix) và tạo báo cáo so sánh cuối cùng với paper.

---

## Task 3.1: Chạy Benchmark 26/26 datasets

**Bước thực hiện**:
1. Compile lại project: `javac -d bin src/cmar/*.java src/cmar/benchmark/*.java`
2. Chạy `BenchmarkRunner` với params giống paper:
   - minSupport = 1% (0.01)
   - minConfidence = 50% (0.5)
   - delta (coverage threshold) = 4
3. Dùng 10-fold cross-validation (hoặc method giống paper)
4. Ghi kết quả từng dataset vào `results/`

**Kiểm tra trước khi chạy**:
- [ ] 26 file CSV đều tồn tại trong `datasets/`
- [ ] Số dòng khớp PAPER_INSTANCE_COUNTS (UCIDatasets.java)
- [ ] Không còn dataset nào bị skip

## Task 3.2: Tạo bảng so sánh mới

Tạo file `results/accuracy-compare-vn-final.md` với format:

```
| # | Dataset | Paper CMAR | Our CMAR (trước fix) | Our CMAR (sau fix) | Chênh lệch |
```

Tính:
- Trung bình accuracy toàn bộ 26 dataset
- Số dataset thắng/hòa/thua (ngưỡng 0.5%)
- Dataset cải thiện nhiều nhất sau fix

## Task 3.3: Phân tích gap còn lại

Với mỗi dataset chênh lệch > 1.5%:
1. Xác định nguyên nhân (data? discretization? classification?)
2. Ghi nhận vào báo cáo
3. Đề xuất cách fix tiếp (nếu có)

**Dự kiến các dataset vẫn có thể lệch**:
- Waveform: random generation khác nhau → chấp nhận
- Zoo: dataset nhỏ (101 dòng) → variance cao trong cross-validation
- German/Diabetes: discretization sensitivity

## Task 3.4: Báo cáo cuối cùng

Tạo `results/final-report.md`:
1. **Tổng quan**: avg accuracy trước/sau fix
2. **Chi tiết**: bảng 26 dataset
3. **Dataset đã fix**: liệt kê thay đổi cụ thể
4. **Gap còn lại**: giải thích lý do
5. **Kết luận**: implementation có đúng với paper không

---

## Tiêu chí hoàn thành (Definition of Done)

- [ ] 26/26 dataset chạy thành công (không bị skip)
- [ ] Trung bình accuracy chênh ≤ 1.0% so với paper (85.2%)
- [ ] ≥ 20/26 dataset chênh ≤ 2.0%
- [ ] Không có dataset nào chênh > 5% (hiện tại Zoo -6.7%)
- [ ] Báo cáo cuối cùng có đầy đủ so sánh

## Timeline ước tính
- Phase 1: 2-3 giờ (fix data thủ công)
- Phase 2: 3-4 giờ (code changes + testing)
- Phase 3: 1-2 giờ (chạy benchmark + viết báo cáo)
- **Tổng**: ~1 ngày làm việc
