# Auto - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 205 instances, 25 mixed attrs, 6 classes |
| Instances | 205 |
| Attributes | 25 |
| Classes | 6 |

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
| **Our CMAR (Java)** | **83.2%** |
| Paper CMAR | 78.1% |
| Paper CBA | 78.3% |
| Paper C4.5 | 80.1% |

**Difference vs Paper CMAR:** +5.1%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 87.5% |
| Fold 2 | 91.7% |
| Fold 3 | 77.3% |
| Fold 4 | 85.7% |
| Fold 5 | 90.0% |
| Fold 6 | 75.0% |
| Fold 7 | 80.0% |
| Fold 8 | 83.3% |
| Fold 9 | 83.3% |
| Fold 10 | 77.8% |
| **Average** | **83.2%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 1971 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 219492 |
| Avg Rules After Pruning | 212 |
| Pruning Ratio | 99.9% |
