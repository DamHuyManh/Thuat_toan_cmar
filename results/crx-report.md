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
| **Our CMAR (Java)** | **86.6%** |
| Paper CMAR | 84.9% |
| Paper CBA | 84.7% |
| Paper C4.5 | 84.9% |

**Difference vs Paper CMAR:** +1.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 82.9% |
| Fold 2 | 80.0% |
| Fold 3 | 75.7% |
| Fold 4 | 85.5% |
| Fold 5 | 92.8% |
| Fold 6 | 87.0% |
| Fold 7 | 91.3% |
| Fold 8 | 88.2% |
| Fold 9 | 88.2% |
| Fold 10 | 94.1% |
| **Average** | **86.6%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 46 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 30762 |
| Avg Rules After Pruning | 559 |
| Pruning Ratio | 98.2% |
