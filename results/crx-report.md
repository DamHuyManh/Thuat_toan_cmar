# Crx - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 690 instances, 15 mixed attrs, 2 classes |
| Instances | 690 |
| Attributes | 15 |
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
| **Our CMAR (Java)** | **86.1%** |
| Paper CMAR | 84.9% |
| Paper CBA | 84.7% |
| Paper C4.5 | 84.9% |

**Difference vs Paper CMAR:** +1.2%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 84.3% |
| Fold 2 | 82.9% |
| Fold 3 | 77.1% |
| Fold 4 | 84.1% |
| Fold 5 | 89.9% |
| Fold 6 | 85.5% |
| Fold 7 | 88.4% |
| Fold 8 | 88.2% |
| Fold 9 | 88.2% |
| Fold 10 | 92.6% |
| **Average** | **86.1%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 37 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 30762 |
| Avg Rules After Pruning | 557 |
| Pruning Ratio | 98.2% |
