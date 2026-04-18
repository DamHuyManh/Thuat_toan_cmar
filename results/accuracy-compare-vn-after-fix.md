# Bao Cao Sau Khi Fix Code (Do Lech Accuracy)

## Cac thay doi da fix

- Sua bug fallback trong `DataLoader`: khong con map nham `*.test` sang `*.csv` (nguyen nhan lam nhan doi du lieu `Anneal` va `Horse`).
- Canh lai `BenchmarkRunner` theo huong paper:
  - `maxCoverageCount = 3` (thay vi 4)
  - `maxAntecedentLength = 4` co dinh
- Canh lai `FPGrowth`: bo nguong ho tro theo lop hiem, dung `minSupport` nhat quan.

## Ket qua chay lai

- `Anneal` va `Horse` hien **bi skip** vi local instances chua du paper:
  - Anneal: 798/898
  - Horse: 300/368
- Cac dataset con lai da duoc so sanh voi paper.

| Dataset | Our CMAR (%) | Paper CMAR (%) | Chenh lech (%) |
|---|---:|---:|---:|
| Australian | 88.0 | 86.1 | +1.9 |
| Auto | 77.7 | 78.1 | -0.4 |
| Breast-Cancer | 96.0 | 96.4 | -0.4 |
| Cleve | 84.2 | 82.2 | +2.0 |
| Crx | 87.7 | 84.9 | +2.8 |
| Diabetes | 74.1 | 75.8 | -1.7 |
| German | 73.1 | 74.9 | -1.8 |
| Glass | 69.6 | 70.1 | -0.5 |
| Heart | 81.9 | 82.2 | -0.3 |
| Hepatitis | 82.3 | 80.5 | +1.8 |
| Hypo | 96.6 | 98.4 | -1.8 |
| Iono | 89.4 | 91.5 | -2.1 |
| Iris | 96.0 | 94.0 | +2.0 |
| Labor | 93.0 | 89.7 | +3.3 |
| Led7 | 72.2 | 72.5 | -0.3 |
| Lymphography | 84.1 | 83.1 | +1.0 |
| Pima | 74.1 | 75.1 | -1.0 |
| Sick | 95.1 | 97.5 | -2.4 |
| Sonar | 80.8 | 79.4 | +1.4 |
| Tic-Tac-Toe | 99.3 | 99.2 | +0.1 |
| Vehicle | 68.7 | 69.0 | -0.3 |
| Waveform | 79.2 | 83.2 | -4.0 |
| Wine | 95.6 | 95.0 | +0.6 |
| Zoo | 93.2 | 97.1 | -3.9 |

## Ket luan ngan

- Da fix xong bug du lieu bi nhan doi (nghiem trong) cho `Anneal`/`Horse`.
- Do lech lon con lai chu yeu o `Waveform`, `Zoo`, `Sick`, `Iono` va mot so bo co phan bo dac thu.
- Muon so sanh day du 26/26 theo paper, can bo sung:
  - `datasets/anneal.test` (hoac `anneal.test.csv`)
  - `datasets/horse-colic.test` (hoac `horse-colic.test.csv`)
