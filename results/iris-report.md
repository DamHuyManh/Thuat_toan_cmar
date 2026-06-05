# Iris - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 150 instances, 4 numeric attrs, 3 classes |
| Instances | 150 |
| Attributes | 4 |
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
| **Our CMAR (Java)** | **92.7%** |
| Paper CMAR | 94.0% |
| Paper CBA | 94.7% |
| Paper C4.5 | 95.3% |

**Difference vs Paper CMAR:** -1.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 86.7% |
| Fold 2 | 100.0% |
| Fold 3 | 100.0% |
| Fold 4 | 93.3% |
| Fold 5 | 86.7% |
| Fold 6 | 86.7% |
| Fold 7 | 93.3% |
| Fold 8 | 93.3% |
| Fold 9 | 93.3% |
| Fold 10 | 93.3% |
| **Average** | **92.7%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 0 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 90 |
| Avg Rules After Pruning | 38 |
| Pruning Ratio | 57.8% |
