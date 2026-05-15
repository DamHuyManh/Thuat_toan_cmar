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
| **Our CMAR (Java)** | **70.0%** |
| Paper CMAR | 70.1% |
| Paper CBA | 73.9% |
| Paper C4.5 | 68.7% |

**Difference vs Paper CMAR:** -0.1%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 60.9% |
| Fold 2 | 73.9% |
| Fold 3 | 78.3% |
| Fold 4 | 63.6% |
| Fold 5 | 72.7% |
| Fold 6 | 77.3% |
| Fold 7 | 76.2% |
| Fold 8 | 65.0% |
| Fold 9 | 65.0% |
| Fold 10 | 66.7% |
| **Average** | **70.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 6 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 2021 |
| Avg Rules After Pruning | 121 |
| Pruning Ratio | 94.0% |
