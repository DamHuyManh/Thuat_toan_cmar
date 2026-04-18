# Vehicle - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 846 instances, 18 numeric attrs, 4 classes |
| Instances | 846 |
| Attributes | 18 |
| Classes | 4 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 11 |
| Min Support (ratio) | 0.02 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **69.0%** |
| Paper CMAR | 68.8% |
| Paper CBA | 68.7% |
| Paper C4.5 | 72.6% |

**Difference vs Paper CMAR:** +0.2%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 75.6% |
| Fold 2 | 72.1% |
| Fold 3 | 71.8% |
| Fold 4 | 70.6% |
| Fold 5 | 67.1% |
| Fold 6 | 67.1% |
| Fold 7 | 63.5% |
| Fold 8 | 66.7% |
| Fold 9 | 69.9% |
| Fold 10 | 65.9% |
| **Average** | **69.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 523 ms |
| Avg Prediction Time | 1 ms |
| Avg Rules Mined | 11093 |
| Avg Rules After Pruning | 748 |
| Pruning Ratio | 93.3% |
