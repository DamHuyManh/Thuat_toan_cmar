# Bao Cao So Sanh Do Chinh Xac CMAR (Dataset Local vs Bai Bao)

## Pham vi

- Thuat toan: CMAR (ban hien tai trong du an)
- Du lieu: bo `datasets/*.csv` local cua ban
- Cach danh gia: cross-validation theo `BenchmarkRunner`
- Moc doi chieu: do chinh xac CMAR trong paper (Li, Han, Pei 2001)

## Ket qua tong quan

- Tong so dataset: **26**
- Tot hon paper (> +0.5%): **9**
- Xap xi paper (trong khoang +/-0.5%): **5**
- Kem hon paper (< -0.5%): **12**
- Do lech trung binh (Our - Paper): **-0.03%**

## Bang chi tiet

| Dataset | Our CMAR (%) | Paper CMAR (%) | Chenh lech (%) |
|---|---:|---:|---:|
| Anneal | 91.5 | 97.3 | -5.8 |
| Australian | 87.7 | 86.1 | +1.6 |
| Auto | 77.1 | 78.1 | -1.0 |
| Breast-Cancer | 96.8 | 96.4 | +0.4 |
| Cleve | 84.8 | 82.2 | +2.6 |
| Crx | 87.9 | 84.9 | +3.0 |
| Diabetes | 74.0 | 75.8 | -1.8 |
| German | 72.1 | 74.9 | -2.8 |
| Glass | 69.6 | 70.1 | -0.5 |
| Heart | 81.9 | 82.2 | -0.3 |
| Hepatitis | 83.5 | 80.5 | +3.0 |
| Horse | 86.6 | 82.6 | +4.0 |
| Hypo | 96.6 | 98.4 | -1.8 |
| Iono | 90.0 | 91.5 | -1.5 |
| Iris | 95.3 | 94.0 | +1.3 |
| Labor | 95.0 | 89.7 | +5.3 |
| Led7 | 73.0 | 72.5 | +0.5 |
| Lymphography | 82.1 | 83.1 | -1.0 |
| Pima | 74.0 | 75.1 | -1.1 |
| Sick | 95.1 | 97.5 | -2.4 |
| Sonar | 80.8 | 79.4 | +1.4 |
| Tic-Tac-Toe | 99.2 | 99.2 | +0.0 |
| Vehicle | 67.9 | 69.0 | -1.1 |
| Waveform | 79.2 | 83.2 | -4.0 |
| Wine | 96.7 | 95.0 | +1.7 |
| Zoo | 96.5 | 97.1 | -0.6 |

## Nhan xet nhanh

- Ket qua trung binh gan nhu bang paper (chenh lech trung binh -0.03%).
- Co nhieu dataset vuot paper ro (vd: Crx, Horse, Labor, Cleve).
- Mot so dataset thap hon paper dang ke (vd: Anneal, Waveform, Sick, German).

## Luu y ky thuat

- Trong lan chay nay, loader ghi nhan:
  - Anneal local doc duoc 1596 dong va da trim ve 898 theo paper.
  - Horse local doc duoc 600 dong va da trim ve 368 theo paper.
- Vi vay ket qua da so sanh tren cung quy mo `Instances` theo paper, nhung chat luong/phan bo mau co the khac ban goc paper.

