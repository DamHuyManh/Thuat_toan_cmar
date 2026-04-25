# Auto - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 205 instances, 25 mixed attrs, 6 classes |
| Instances | 205 |
| Attributes | 25 |
| Classes | 6 |

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
| **Our CMAR (Java)** | **81.4%** |
| Paper CMAR | 78.1% |
| Paper CBA | 78.3% |
| Paper C4.5 | 80.1% |

**Difference vs Paper CMAR:** +3.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 87.5% |
| Fold 2 | 87.5% |
| Fold 3 | 90.9% |
| Fold 4 | 81.0% |
| Fold 5 | 90.0% |
| Fold 6 | 85.0% |
| Fold 7 | 70.0% |
| Fold 8 | 77.8% |
| Fold 9 | 83.3% |
| Fold 10 | 61.1% |
| **Average** | **81.4%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 750 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 209009 |
| Avg Rules After Pruning | 208 |
| Pruning Ratio | 99.9% |
