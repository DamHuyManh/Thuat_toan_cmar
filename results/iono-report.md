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
| **Our CMAR (Java)** | **90.6%** |
| Paper CMAR | 91.5% |
| Paper CBA | 92.3% |
| Paper C4.5 | 90.0% |

**Difference vs Paper CMAR:** -0.9%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 88.9% |
| Fold 2 | 91.7% |
| Fold 3 | 86.1% |
| Fold 4 | 94.4% |
| Fold 5 | 91.7% |
| Fold 6 | 91.4% |
| Fold 7 | 91.2% |
| Fold 8 | 91.2% |
| Fold 9 | 88.2% |
| Fold 10 | 91.2% |
| **Average** | **90.6%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 905 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 129736 |
| Avg Rules After Pruning | 194 |
| Pruning Ratio | 99.9% |
