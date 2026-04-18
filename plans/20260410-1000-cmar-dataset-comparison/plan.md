# Plan: Sửa Dataset CMAR cho khớp Paper gốc

## Mục tiêu
Sửa 12/26 dataset lệch so với paper CMAR (Li, Han, Pei 2001) để đạt độ chính xác gần nhất với kết quả paper (85.2% avg).

## Hiện trạng
- 24/26 dataset đã chạy (Anneal, Horse bị skip do thiếu dòng)
- Trung bình chênh lệch: -0.3% so với paper
- 3 dataset lệch nhiều: Zoo (-6.7%), Waveform (-4.0%), Sick (-2.4%)

## 3 Phase

### Phase 1: Download & Fix Dataset (phase-01)
- Merge anneal.data + anneal.test → 898 dòng
- Merge horse-colic.data + horse-colic.test → 368 dòng
- Fix Breast-Cancer: loại 16 dòng missing → 683
- Fix Zoo: bỏ cột animal name (18→17 cột)
- Fix Diabetes/Pima off-by-one (767→768)
- Fix format: space-delimited files

### Phase 2: Data Quality & Preprocessing (phase-02)
- Cải thiện xử lý missing values (Anneal, Horse, Sick, Hypo)
- Kiểm tra MDL discretization khớp với CBA
- Fix Waveform: đảm bảo đúng version 5000 records, 21 attrs

### Phase 3: Benchmark & So sánh (phase-03)
- Chạy lại 26/26 dataset
- So sánh accuracy mới vs paper
- Báo cáo kết quả cuối cùng

## Ưu tiên
1. **CRITICAL**: Anneal, Horse (bị skip hoàn toàn)
2. **HIGH**: Zoo (-6.7%), Waveform (-4.0%), Sick (-2.4%)
3. **MEDIUM**: Hypo (-1.8%), Diabetes (-2.0%), German (-1.8%), Iono (-2.1%)

## Files liên quan
- `src/cmar/benchmark/DataLoader.java` — parsing & missing value handling
- `src/cmar/benchmark/UCIDatasets.java` — dataset configs
- `datasets/originals/` — raw UCI files
- `datasets/*.csv` — processed files
