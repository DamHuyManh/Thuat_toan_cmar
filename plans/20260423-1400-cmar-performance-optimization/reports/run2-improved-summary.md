# CMAR Benchmark Summary Report

**Date:** 2026-03-12

**Reference Paper:** Li, Han, Pei. "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules" (IEEE ICDM 2001)

**Implementation:** Java (optimized with bitmap matching, hash-indexed CR-tree, chi-square + coverage pruning)

**Evaluation:** 10-fold cross-validation

## Accuracy Comparison

| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |
|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|
| Anneal | 898 | 38 | 6 | **98.2%** | 97.3% | 97.9% | 94.8% | +0.9% |
| Australian | 690 | 14 | 2 | **86.8%** | 86.1% | 84.9% | 84.7% | +0.7% |
| Auto | 205 | 25 | 6 | **81.4%** | 78.1% | 78.3% | 80.1% | +3.3% |
| Breast-Cancer | 683 | 9 | 2 | **97.1%** | 96.4% | 96.3% | 95.0% | +0.7% |
| Cleve | 303 | 13 | 2 | **82.6%** | 82.2% | 82.8% | 78.2% | +0.4% |
| Crx | 690 | 15 | 2 | **86.1%** | 84.9% | 84.7% | 84.9% | +1.2% |
| Diabetes | 768 | 8 | 2 | **73.4%** | 75.8% | 74.5% | 74.2% | -2.4% |
| German | 1000 | 20 | 2 | **72.9%** | 74.9% | 73.4% | 72.3% | -2.0% |
| Glass | 214 | 9 | 6 | **70.0%** | 70.1% | 73.9% | 68.7% | -0.1% |
| Heart | 270 | 13 | 2 | **80.7%** | 82.2% | 81.9% | 80.8% | -1.5% |
| Hepatitis | 155 | 19 | 2 | **83.3%** | 80.5% | 81.8% | 80.6% | +2.8% |
| Horse | 368 | 22 | 2 | **82.3%** | 82.6% | 82.1% | 82.6% | -0.3% |
| Hypo | 3163 | 25 | 2 | **97.9%** | 98.4% | 98.9% | 99.2% | -0.5% |
| Iono | 351 | 34 | 2 | **92.6%** | 91.5% | 92.3% | 90.0% | +1.1% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **91.7%** | 89.7% | 86.3% | 79.3% | +2.0% |
| Led7 | 3200 | 7 | 10 | **71.2%** | 72.5% | 71.9% | 73.5% | -1.3% |
| Lymphography | 148 | 18 | 4 | **83.4%** | 83.1% | 77.8% | 73.5% | +0.3% |
| Pima | 768 | 8 | 2 | **73.4%** | 75.1% | 72.9% | 75.5% | -1.7% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| Sonar | 208 | 60 | 2 | **80.8%** | 79.4% | 77.5% | 70.2% | +1.4% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.2%** | 99.2% | 99.6% | 99.4% | -0.0% |
| Vehicle | 846 | 18 | 4 | **68.2%** | 68.8% | 68.7% | 72.6% | -0.6% |
| Waveform | 5000 | 21 | 3 | **81.6%** | 83.2% | 80.0% | 78.1% | -1.6% |
| Wine | 178 | 13 | 3 | **96.7%** | 95.0% | 95.0% | 92.7% | +1.7% |
| Zoo | 101 | 16 | 7 | **96.5%** | 97.1% | 96.8% | 92.2% | -0.6% |
| **Average** | | | | **85.3%** | 85.2% | 84.7% | 83.3% | +0.1% |

## Performance Metrics

| Dataset | Train Time | Predict Time | Rules Mined | Rules Pruned | Prune Ratio |
|---------|-----------|-------------|-------------|-------------|-------------|
| Anneal | 564 ms | 0 ms | 156588 | 159 | 99.9% |
| Australian | 29 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 494 ms | 0 ms | 209009 | 208 | 99.9% |
| Breast-Cancer | 4 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 11 ms | 0 ms | 16274 | 276 | 98.3% |
| Crx | 30 ms | 0 ms | 30762 | 557 | 98.2% |
| Diabetes | 2 ms | 0 ms | 1585 | 213 | 86.6% |
| German | 120 ms | 0 ms | 89483 | 951 | 98.9% |
| Glass | 2 ms | 0 ms | 2021 | 121 | 94.0% |
| Heart | 11 ms | 0 ms | 15134 | 249 | 98.4% |
| Hepatitis | 40 ms | 0 ms | 38172 | 122 | 99.7% |
| Horse | 150 ms | 0 ms | 129386 | 397 | 99.7% |
| Hypo | 90 ms | 0 ms | 86450 | 176 | 99.8% |
| Iono | 316 ms | 0 ms | 129736 | 196 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 30 | 66.7% |
| Labor | 19 ms | 0 ms | 24003 | 49 | 99.8% |
| Led7 | 3 ms | 0 ms | 242 | 111 | 54.1% |
| Lymphography | 71 ms | 0 ms | 65800 | 149 | 99.8% |
| Pima | 2 ms | 0 ms | 1585 | 213 | 86.6% |
| Sick | 132 ms | 0 ms | 85874 | 279 | 99.7% |
| Sonar | 972 ms | 0 ms | 160000 | 172 | 99.9% |
| Tic-Tac-Toe | 9 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 54 ms | 0 ms | 36922 | 477 | 98.7% |
| Waveform | 904 ms | 6 ms | 75473 | 2650 | 96.5% |
| Wine | 20 ms | 0 ms | 16933 | 54 | 99.7% |
| Zoo | 18 ms | 0 ms | 13758 | 35 | 99.7% |

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
| Sick | 0.01 | 12 | 0.50 |
| Sonar | 0.08 | 14 | 0.50 |
| Tic-Tac-Toe | 0.01 | 8 | 0.50 |
| Vehicle | 0.02 | 11 | 0.50 |
| Waveform | 0.01 | 45 | 0.50 |
| Wine | 0.01 | 2 | 0.50 |
| Zoo | 0.03 | 3 | 0.50 |

## Key Observations

- **Wins** (our > paper by >0.5%): 10/26
- **Ties** (within 0.5%): 6/26
- **Losses** (our < paper by >0.5%): 10/26
- **Average accuracy difference:** +0.1%

## Optimizations Applied

1. **Bitmap rule matching** - bitwise AND for O(1) antecedent subset testing
2. **Hash-indexed CR-tree** - class-partitioned with first-item pruning
3. **Chi-square pruning (CSP)** - removes statistically insignificant rules (p<0.05)
4. **Database coverage pruning (DCP)** - eliminates redundant rules
5. **Single-path FP-tree optimization** - direct subset enumeration
6. **Weighted voting** - weight = chi² × confidence, top-5 per class
7. **Per-class adaptive minSupport** - rare classes (?10 instances) use support floor of 1
8. **Max antecedent length** - capped at 4 items to reduce noise
