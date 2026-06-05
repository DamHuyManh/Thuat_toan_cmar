# Heart - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 270 instances, 13 attrs, 2 classes |
| Instances | 270 |
| Attributes | 13 |
| Classes | 2 |

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
| **Our CMAR (Java)** | **79.3%** |
| Paper CMAR | 82.2% |
| Paper CBA | 81.9% |
| Paper C4.5 | 80.8% |

**Difference vs Paper CMAR:** -2.9%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 66.7% |
| Fold 2 | 88.9% |
| Fold 3 | 77.8% |
| Fold 4 | 77.8% |
| Fold 5 | 77.8% |
| Fold 6 | 70.4% |
| Fold 7 | 77.8% |
| Fold 8 | 81.5% |
| Fold 9 | 92.6% |
| Fold 10 | 81.5% |
| **Average** | **79.3%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 14 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 15134 |
| Avg Rules After Pruning | 253 |
| Pruning Ratio | 98.3% |
