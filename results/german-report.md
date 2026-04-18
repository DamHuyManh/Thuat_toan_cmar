# German - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 1000 instances, 20 mixed attrs, 2 classes |
| Instances | 1000 |
| Attributes | 20 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 9 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **75.0%** |
| Paper CMAR | 74.9% |
| Paper CBA | 73.4% |
| Paper C4.5 | 72.3% |

**Difference vs Paper CMAR:** +0.1%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 71.0% |
| Fold 2 | 79.0% |
| Fold 3 | 78.0% |
| Fold 4 | 69.0% |
| Fold 5 | 75.0% |
| Fold 6 | 72.0% |
| Fold 7 | 76.0% |
| Fold 8 | 78.0% |
| Fold 9 | 72.0% |
| Fold 10 | 80.0% |
| **Average** | **75.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 3591 ms |
| Avg Prediction Time | 3 ms |
| Avg Rules Mined | 88299 |
| Avg Rules After Pruning | 1344 |
| Pruning Ratio | 98.5% |
