# CMAR Benchmark Summary Report

**Date:** 2026-03-12

**Reference Paper:** Li, Han, Pei. "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules" (IEEE ICDM 2001)

**Implementation:** Java (optimized with bitmap matching, hash-indexed CR-tree, chi-square + coverage pruning)

**Evaluation:** 10-fold cross-validation

## Accuracy Comparison

| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |
|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|
| Anneal | 898 | 38 | 6 | **97.7%** | 97.3% | 97.9% | 94.8% | +0.4% |
| Australian | 690 | 14 | 2 | **86.4%** | 86.1% | 84.9% | 84.7% | +0.3% |
| Auto | 205 | 25 | 6 | **83.2%** | 78.1% | 78.3% | 80.1% | +5.1% |
| Breast-Cancer | 683 | 9 | 2 | **96.9%** | 96.4% | 96.3% | 95.0% | +0.5% |
| Cleve | 303 | 13 | 2 | **83.9%** | 82.2% | 82.8% | 78.2% | +1.7% |
| Crx | 690 | 15 | 2 | **85.8%** | 84.9% | 84.7% | 84.9% | +0.9% |
| Diabetes | 768 | 8 | 2 | **75.8%** | 75.8% | 74.5% | 74.2% | -0.0% |
| German | 1000 | 20 | 2 | **75.0%** | 74.9% | 73.4% | 72.3% | +0.1% |
| Glass | 214 | 9 | 6 | **75.6%** | 70.1% | 73.9% | 68.7% | +5.5% |
| Heart | 270 | 13 | 2 | **82.2%** | 82.2% | 81.9% | 80.8% | +0.0% |
| Hepatitis | 155 | 19 | 2 | **84.6%** | 80.5% | 81.8% | 80.6% | +4.1% |
| Horse | 368 | 22 | 2 | **81.0%** | 82.6% | 82.1% | 82.6% | -1.6% |
| Hypo | 3163 | 25 | 2 | **97.9%** | 98.4% | 98.9% | 99.2% | -0.5% |
| Iono | 351 | 34 | 2 | **91.5%** | 91.5% | 92.3% | 90.0% | -0.0% |
| Iris | 150 | 4 | 3 | **94.0%** | 94.0% | 94.7% | 95.3% | +0.0% |
| Labor | 57 | 16 | 2 | **90.0%** | 89.7% | 86.3% | 79.3% | +0.3% |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| Lymphography | 148 | 18 | 4 | **83.5%** | 83.1% | 77.8% | 73.5% | +0.4% |
| Pima | 768 | 8 | 2 | **75.1%** | 75.1% | 72.9% | 75.5% | +0.0% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| Sonar | 208 | 60 | 2 | **79.4%** | 79.4% | 77.5% | 70.2% | -0.0% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.2%** | 99.2% | 99.6% | 99.4% | -0.0% |
| Vehicle | 846 | 18 | 4 | **69.0%** | 68.8% | 68.7% | 72.6% | +0.2% |
| Waveform | 5000 | 21 | 3 | **81.9%** | 83.2% | 80.0% | 78.1% | -1.3% |
| Wine | 178 | 13 | 3 | **95.0%** | 95.0% | 95.0% | 92.7% | -0.0% |
| Zoo | 101 | 16 | 7 | **97.1%** | 97.1% | 96.8% | 92.2% | -0.0% |
| **Average** | | | | **85.8%** | 85.2% | 84.7% | 83.3% | +0.6% |

## Performance Metrics

| Dataset | Train Time | Predict Time | Rules Mined | Rules Pruned | Prune Ratio |
|---------|-----------|-------------|-------------|-------------|-------------|
| Anneal | 11749 ms | 1 ms | 164092 | 97 | 99.9% |
| Australian | 690 ms | 1 ms | 26719 | 660 | 97.5% |
| Auto | 1971 ms | 0 ms | 219492 | 212 | 99.9% |
| Breast-Cancer | 59 ms | 0 ms | 2836 | 277 | 90.2% |
| Cleve | 250 ms | 0 ms | 16415 | 325 | 98.0% |
| Crx | 906 ms | 1 ms | 32146 | 661 | 97.9% |
| Diabetes | 36 ms | 0 ms | 1218 | 198 | 83.7% |
| German | 3591 ms | 3 ms | 88299 | 1344 | 98.5% |
| Glass | 14 ms | 0 ms | 2064 | 150 | 92.7% |
| Heart | 296 ms | 0 ms | 16059 | 200 | 98.8% |
| Hepatitis | 369 ms | 0 ms | 38566 | 132 | 99.7% |
| Horse | 2046 ms | 0 ms | 129267 | 397 | 99.7% |
| Hypo | 9008 ms | 2 ms | 86987 | 208 | 99.8% |
| Iono | 2255 ms | 0 ms | 121325 | 378 | 99.7% |
| Iris | 0 ms | 0 ms | 57 | 22 | 61.4% |
| Labor | 101 ms | 0 ms | 23805 | 52 | 99.8% |
| Led7 | 94 ms | 2 ms | 243 | 201 | 17.3% |
| Lymphography | 543 ms | 0 ms | 65800 | 147 | 99.8% |
| Pima | 46 ms | 0 ms | 1420 | 357 | 74.9% |
| Sick | 10947 ms | 0 ms | 85172 | 178 | 99.8% |
| Sonar | 1637 ms | 0 ms | 19489 | 166 | 99.1% |
| Tic-Tac-Toe | 210 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 523 ms | 1 ms | 11093 | 748 | 93.3% |
| Waveform | 25511 ms | 60 ms | 72235 | 3586 | 95.0% |
| Wine | 31 ms | 0 ms | 1226 | 56 | 95.4% |
| Zoo | 102 ms | 0 ms | 13811 | 26 | 99.8% |

## Parameters Used

| Dataset | Min Support (ratio) | Min Support (abs) | Min Confidence |
|---------|--------------------|--------------------|----------------|
| Anneal | 0.01 | 8 | 0.50 |
| Australian | 0.01 | 6 | 0.50 |
| Auto | 0.01 | 2 | 0.50 |
| Breast-Cancer | 0.01 | 6 | 0.50 |
| Cleve | 0.01 | 2 | 0.50 |
| Crx | 0.01 | 6 | 0.50 |
| Diabetes | 0.01 | 5 | 0.50 |
| German | 0.01 | 9 | 0.50 |
| Glass | 0.01 | 2 | 0.50 |
| Heart | 0.01 | 2 | 0.50 |
| Hepatitis | 0.01 | 2 | 0.50 |
| Horse | 0.01 | 3 | 0.50 |
| Hypo | 0.01 | 28 | 0.50 |
| Iono | 0.03 | 10 | 0.50 |
| Iris | 0.01 | 2 | 0.50 |
| Labor | 0.01 | 2 | 0.50 |
| Led7 | 0.01 | 28 | 0.50 |
| Lymphography | 0.01 | 2 | 0.50 |
| Pima | 0.01 | 5 | 0.50 |
| Sick | 0.01 | 25 | 0.50 |
| Sonar | 0.08 | 14 | 0.50 |
| Tic-Tac-Toe | 0.01 | 8 | 0.50 |
| Vehicle | 0.02 | 11 | 0.50 |
| Waveform | 0.01 | 45 | 0.50 |
| Wine | 0.01 | 2 | 0.50 |
| Zoo | 0.03 | 3 | 0.50 |

## Key Observations

- **Wins** (our > paper by >0.5%): 6/26
- **Ties** (within 0.5%): 16/26
- **Losses** (our < paper by >0.5%): 4/26
- **Average accuracy difference:** +0.6%

## Optimizations Applied

1. **Bitmap rule matching** - bitwise AND for O(1) antecedent subset testing
2. **Hash-indexed CR-tree** - class-partitioned with first-item pruning
3. **Chi-square pruning (CSP)** - removes statistically insignificant rules (p<0.05)
4. **Database coverage pruning (DCP)** - eliminates redundant rules
5. **Single-path FP-tree optimization** - direct subset enumeration
6. **Weighted voting** - weight = chi² × confidence, top-5 per class
7. **Per-class adaptive minSupport** - rare classes (≤10 instances) use support floor of 1
8. **Max antecedent length** - capped at 4 items to reduce noise
