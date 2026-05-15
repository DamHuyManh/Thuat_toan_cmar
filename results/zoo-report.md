# Zoo - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 101 instances, 16 boolean attrs, 7 classes |
| Instances | 101 |
| Attributes | 16 |
| Classes | 7 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 3 |
| Min Support (ratio) | 0.03 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **96.5%** |
| Paper CMAR | 97.1% |
| Paper CBA | 96.8% |
| Paper C4.5 | 92.2% |

**Difference vs Paper CMAR:** -0.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 84.6% |
| Fold 2 | 100.0% |
| Fold 3 | 91.7% |
| Fold 4 | 100.0% |
| Fold 5 | 100.0% |
| Fold 6 | 88.9% |
| Fold 7 | 100.0% |
| Fold 8 | 100.0% |
| Fold 9 | 100.0% |
| Fold 10 | 100.0% |
| **Average** | **96.5%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 49 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 13758 |
| Avg Rules After Pruning | 35 |
| Pruning Ratio | 99.7% |
