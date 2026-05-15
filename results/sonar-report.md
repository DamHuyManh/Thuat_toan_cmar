# Sonar - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 208 instances, 60 numeric attrs, 2 classes |
| Instances | 208 |
| Attributes | 60 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 14 |
| Min Support (ratio) | 0.08 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **80.8%** |
| Paper CMAR | 79.4% |
| Paper CBA | 77.5% |
| Paper C4.5 | 70.2% |

**Difference vs Paper CMAR:** +1.4%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 72.7% |
| Fold 2 | 71.4% |
| Fold 3 | 81.0% |
| Fold 4 | 90.5% |
| Fold 5 | 81.0% |
| Fold 6 | 81.0% |
| Fold 7 | 90.5% |
| Fold 8 | 80.0% |
| Fold 9 | 80.0% |
| Fold 10 | 80.0% |
| **Average** | **80.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 3282 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 160000 |
| Avg Rules After Pruning | 172 |
| Pruning Ratio | 99.9% |
