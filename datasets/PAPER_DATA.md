# CMAR Paper - Full Dataset & Results

## Paper Info
- **Title:** CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules
- **Authors:** Wenmin Li, Jiawei Han, Jian Pei
- **Venue:** IEEE ICDM 2001, pp. 369-376
- **PDF:** https://www.cs.sfu.ca/~jpei/publications/cmar.pdf

## Parameters (Same for ALL 26 datasets)
- minSupport: **1%**
- minConfidence: **50%**
- Database coverage threshold (delta): **4**
- Chi-square pruning: significance level test
- Classification: weighted chi-square (chi²/max_chi²)

## Full Accuracy Table (26 datasets)

| # | Dataset | C4.5 | CBA | CMAR |
|---|---------|------|-----|------|
| 1 | Anneal | 94.8 | 97.9 | 97.3 |
| 2 | Austral | 84.7 | 84.9 | 86.1 |
| 3 | Auto | 80.1 | 78.3 | 78.1 |
| 4 | Breast | 95.0 | 96.3 | 96.4 |
| 5 | Cleve | 78.2 | 82.8 | 82.2 |
| 6 | Crx | 84.9 | 84.7 | 84.9 |
| 7 | Diabetes | 74.2 | 74.5 | 75.8 |
| 8 | German | 72.3 | 73.4 | 74.9 |
| 9 | Glass | 68.7 | 73.9 | 70.1 |
| 10 | Heart | 80.8 | 81.9 | 82.2 |
| 11 | Hepatic | 80.6 | 81.8 | 80.5 |
| 12 | Horse | 82.6 | 82.1 | 82.6 |
| 13 | Hypo | 99.2 | 98.9 | 98.4 |
| 14 | Iono | 90.0 | 92.3 | 91.5 |
| 15 | Iris | 95.3 | 94.7 | 94.0 |
| 16 | Labor | 79.3 | 86.3 | 89.7 |
| 17 | Led7 | 73.5 | 71.9 | 72.5 |
| 18 | Lymph | 73.5 | 77.8 | 83.1 |
| 19 | Pima | 75.5 | 72.9 | 75.1 |
| 20 | Sick | 98.5 | 97.0 | 97.5 |
| 21 | Sonar | 70.2 | 77.5 | 79.4 |
| 22 | Tic-tac | 99.4 | 99.6 | 99.2 |
| 23 | Vehicle | 72.6 | 68.7 | 68.8 |
| 24 | Waveform | 78.1 | 80.0 | 83.2 |
| 25 | Wine | 92.7 | 95.0 | 95.0 |
| 26 | Zoo | 92.2 | 96.8 | 97.1 |
| | **Average** | **83.34** | **84.69** | **85.22** |

## Dataset Specifications

| # | Dataset | Attrs | Classes | Records |
|---|---------|-------|---------|---------|
| 1 | Anneal | 38 | 6 | 898 |
| 2 | Austral | 14 | 2 | 690 |
| 3 | Auto | 25 | 7 | 205 |
| 4 | Breast | 10 | 2 | 699 |
| 5 | Cleve | 13 | 2 | 303 |
| 6 | Crx | 15 | 2 | 690 |
| 7 | Diabetes | 8 | 2 | 768 |
| 8 | German | 20 | 2 | 1000 |
| 9 | Glass | 9 | 7 | 214 |
| 10 | Heart | 13 | 2 | 270 |
| 11 | Hepatic | 19 | 2 | 155 |
| 12 | Horse | 22 | 2 | 368 |
| 13 | Hypo | 25 | 2 | 3163 |
| 14 | Iono | 34 | 2 | 351 |
| 15 | Iris | 4 | 3 | 150 |
| 16 | Labor | 16 | 2 | 57 |
| 17 | Led7 | 7 | 10 | 3200 |
| 18 | Lymph | 18 | 4 | 148 |
| 19 | Pima | 8 | 2 | 768 |
| 20 | Sick | 29 | 2 | 2800 |
| 21 | Sonar | 60 | 2 | 208 |
| 22 | Tic-tac | 9 | 2 | 958 |
| 23 | Vehicle | 18 | 4 | 846 |
| 24 | Waveform | 21 | 3 | 5000 |
| 25 | Wine | 13 | 3 | 178 |
| 26 | Zoo | 16 | 7 | 101 |

## Key Algorithm Details

### Rule Ranking (R1 > R2 if):
1. conf(R1) > conf(R2), OR
2. conf equal but sup(R1) > sup(R2), OR
3. conf and sup equal but R1 has fewer antecedent items

### Three Pruning Methods:
1. General high-confidence rule prunes more specific lower-confidence rules
2. Chi-square testing: only positively correlated rules kept
3. Database coverage pruning (delta=4)

### Classification:
- Weighted chi-square: weight = chi²/max_chi² (normalized)
- max_chi² removes bias toward majority classes
- Group measure = SUM(chi²/max_chi²) for all rules in group

### Sensitivity Analysis (Sonar dataset):
- Coverage threshold 1-5: peak at ~4
- Confidence difference 0-0.12: peak at ~0.02-0.04
