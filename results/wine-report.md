# Wine - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 178 instances, 13 numeric attrs, 3 classes |
| Instances | 178 |
| Attributes | 13 |
| Classes | 3 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 2 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **95.0%** |
| Paper CMAR | 95.0% |
| Paper CBA | 95.0% |
| Paper C4.5 | 92.7% |

**Difference vs Paper CMAR:** -0.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 89.5% |
| Fold 2 | 88.9% |
| Fold 3 | 94.4% |
| Fold 4 | 100.0% |
| Fold 5 | 94.4% |
| Fold 6 | 94.4% |
| Fold 7 | 100.0% |
| Fold 8 | 94.4% |
| Fold 9 | 100.0% |
| Fold 10 | 93.8% |
| **Average** | **95.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 31 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 1226 |
| Avg Rules After Pruning | 56 |
| Pruning Ratio | 95.4% |
