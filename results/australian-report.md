# Australian - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 690 instances, 14 mixed attrs, 2 classes |
| Instances | 690 |
| Attributes | 14 |
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
| **Our CMAR (Java)** | **86.4%** |
| Paper CMAR | 86.1% |
| Paper CBA | 84.9% |
| Paper C4.5 | 84.7% |

**Difference vs Paper CMAR:** +0.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 87.1% |
| Fold 2 | 85.7% |
| Fold 3 | 84.3% |
| Fold 4 | 87.0% |
| Fold 5 | 87.0% |
| Fold 6 | 89.9% |
| Fold 7 | 82.6% |
| Fold 8 | 85.3% |
| Fold 9 | 83.8% |
| Fold 10 | 91.2% |
| **Average** | **86.4%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 690 ms |
| Avg Prediction Time | 1 ms |
| Avg Rules Mined | 26719 |
| Avg Rules After Pruning | 660 |
| Pruning Ratio | 97.5% |
