# Vehicle - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 846 instances, 18 numeric attrs, 4 classes |
| Instances | 846 |
| Attributes | 18 |
| Classes | 4 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 11 |
| Min Support (ratio) | 0.02 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **68.2%** |
| Paper CMAR | 68.8% |
| Paper CBA | 68.7% |
| Paper C4.5 | 72.6% |

**Difference vs Paper CMAR:** -0.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 68.6% |
| Fold 2 | 66.3% |
| Fold 3 | 64.7% |
| Fold 4 | 76.5% |
| Fold 5 | 67.1% |
| Fold 6 | 64.7% |
| Fold 7 | 63.5% |
| Fold 8 | 69.0% |
| Fold 9 | 72.3% |
| Fold 10 | 69.5% |
| **Average** | **68.2%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 68 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 36922 |
| Avg Rules After Pruning | 477 |
| Pruning Ratio | 98.7% |
