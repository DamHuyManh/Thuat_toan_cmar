# Led7 - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 3200 instances, 7 binary attrs, 10 classes |
| Instances | 3200 |
| Attributes | 7 |
| Classes | 10 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 28 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **71.2%** |
| Paper CMAR | 72.5% |
| Paper CBA | 71.9% |
| Paper C4.5 | 73.5% |

**Difference vs Paper CMAR:** -1.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 71.4% |
| Fold 2 | 68.6% |
| Fold 3 | 70.7% |
| Fold 4 | 70.3% |
| Fold 5 | 71.3% |
| Fold 6 | 67.4% |
| Fold 7 | 73.2% |
| Fold 8 | 71.2% |
| Fold 9 | 74.3% |
| Fold 10 | 73.7% |
| **Average** | **71.2%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 3 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 242 |
| Avg Rules After Pruning | 111 |
| Pruning Ratio | 54.1% |
