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
| **Our CMAR (Java)** | **86.8%** |
| Paper CMAR | 86.1% |
| Paper CBA | 84.9% |
| Paper C4.5 | 84.7% |

**Difference vs Paper CMAR:** +0.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 87.1% |
| Fold 2 | 88.6% |
| Fold 3 | 85.7% |
| Fold 4 | 88.4% |
| Fold 5 | 89.9% |
| Fold 6 | 91.3% |
| Fold 7 | 78.3% |
| Fold 8 | 88.2% |
| Fold 9 | 83.8% |
| Fold 10 | 86.8% |
| **Average** | **86.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 114 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 18745 |
| Avg Rules After Pruning | 456 |
| Pruning Ratio | 97.6% |
