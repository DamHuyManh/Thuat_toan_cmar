# Iono - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 351 instances, 34 numeric attrs, 2 classes |
| Instances | 351 |
| Attributes | 34 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 10 |
| Min Support (ratio) | 0.03 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **92.6%** |
| Paper CMAR | 91.5% |
| Paper CBA | 92.3% |
| Paper C4.5 | 90.0% |

**Difference vs Paper CMAR:** +1.1%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 94.4% |
| Fold 2 | 91.7% |
| Fold 3 | 88.9% |
| Fold 4 | 94.4% |
| Fold 5 | 94.4% |
| Fold 6 | 97.1% |
| Fold 7 | 91.2% |
| Fold 8 | 94.1% |
| Fold 9 | 88.2% |
| Fold 10 | 91.2% |
| **Average** | **92.6%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 355 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 129736 |
| Avg Rules After Pruning | 196 |
| Pruning Ratio | 99.8% |
