# Glass - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 214 instances, 9 numeric attrs, 6 classes |
| Instances | 214 |
| Attributes | 9 |
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
| **Our CMAR (Java)** | **71.7%** |
| Paper CMAR | 70.1% |
| Paper CBA | 73.9% |
| Paper C4.5 | 68.7% |

**Difference vs Paper CMAR:** +1.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 56.5% |
| Fold 2 | 82.6% |
| Fold 3 | 82.6% |
| Fold 4 | 68.2% |
| Fold 5 | 68.2% |
| Fold 6 | 77.3% |
| Fold 7 | 81.0% |
| Fold 8 | 70.0% |
| Fold 9 | 70.0% |
| Fold 10 | 61.1% |
| **Average** | **71.7%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 2 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 2021 |
| Avg Rules After Pruning | 143 |
| Pruning Ratio | 92.9% |
