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
| **Our CMAR (Java)** | **73.2%** |
| Paper CMAR | 72.5% |
| Paper CBA | 71.9% |
| Paper C4.5 | 73.5% |

**Difference vs Paper CMAR:** +0.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 72.9% |
| Fold 2 | 71.1% |
| Fold 3 | 74.1% |
| Fold 4 | 74.3% |
| Fold 5 | 74.1% |
| Fold 6 | 75.2% |
| Fold 7 | 73.5% |
| Fold 8 | 71.8% |
| Fold 9 | 74.3% |
| Fold 10 | 71.1% |
| **Average** | **73.2%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 8 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 243 |
| Avg Rules After Pruning | 100 |
| Pruning Ratio | 58.8% |
