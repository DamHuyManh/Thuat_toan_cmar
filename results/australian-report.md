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
| **Our CMAR (Java)** | **85.9%** |
| Paper CMAR | 86.1% |
| Paper CBA | 84.9% |
| Paper C4.5 | 84.7% |

**Difference vs Paper CMAR:** -0.2%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 84.3% |
| Fold 2 | 87.1% |
| Fold 3 | 82.9% |
| Fold 4 | 85.5% |
| Fold 5 | 89.9% |
| Fold 6 | 91.3% |
| Fold 7 | 81.2% |
| Fold 8 | 86.8% |
| Fold 9 | 85.3% |
| Fold 10 | 85.3% |
| **Average** | **85.9%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 39 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 18745 |
| Avg Rules After Pruning | 456 |
| Pruning Ratio | 97.6% |
