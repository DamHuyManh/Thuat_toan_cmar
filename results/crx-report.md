# Crx - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 690 instances, 15 mixed attrs, 2 classes |
| Instances | 690 |
| Attributes | 15 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 6 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **85.8%** |
| Paper CMAR | 84.9% |
| Paper CBA | 84.7% |
| Paper C4.5 | 84.9% |

**Difference vs Paper CMAR:** +0.9%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 81.4% |
| Fold 2 | 80.0% |
| Fold 3 | 77.1% |
| Fold 4 | 81.2% |
| Fold 5 | 91.3% |
| Fold 6 | 87.0% |
| Fold 7 | 88.4% |
| Fold 8 | 86.8% |
| Fold 9 | 91.2% |
| Fold 10 | 94.1% |
| **Average** | **85.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 906 ms |
| Avg Prediction Time | 1 ms |
| Avg Rules Mined | 32146 |
| Avg Rules After Pruning | 661 |
| Pruning Ratio | 97.9% |
