# Breast-Cancer - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 683 instances, 9 integer attrs, 2 classes |
| Instances | 683 |
| Attributes | 9 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 6 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **97.1%** |
| Paper CMAR | 96.4% |
| Paper CBA | 96.3% |
| Paper C4.5 | 95.0% |

**Difference vs Paper CMAR:** +0.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 98.6% |
| Fold 2 | 95.7% |
| Fold 3 | 97.1% |
| Fold 4 | 97.1% |
| Fold 5 | 98.5% |
| Fold 6 | 100.0% |
| Fold 7 | 97.1% |
| Fold 8 | 95.6% |
| Fold 9 | 98.5% |
| Fold 10 | 92.5% |
| **Average** | **97.1%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 10 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 2836 |
| Avg Rules After Pruning | 265 |
| Pruning Ratio | 90.7% |
