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
| **Our CMAR (Java)** | **72.2%** |
| Paper CMAR | 72.5% |
| Paper CBA | 71.9% |
| Paper C4.5 | 73.5% |

**Difference vs Paper CMAR:** -0.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 72.0% |
| Fold 2 | 68.0% |
| Fold 3 | 73.1% |
| Fold 4 | 72.8% |
| Fold 5 | 71.7% |
| Fold 6 | 71.2% |
| Fold 7 | 74.8% |
| Fold 8 | 72.8% |
| Fold 9 | 73.7% |
| Fold 10 | 71.7% |
| **Average** | **72.2%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 3 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 243 |
| Avg Rules After Pruning | 112 |
| Pruning Ratio | 53.9% |
