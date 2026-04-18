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
| **Our CMAR (Java)** | **79.4%** |
| Paper CMAR | 79.4% |
| Paper CBA | 77.5% |
| Paper C4.5 | 70.2% |

**Difference vs Paper CMAR:** -0.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 77.3% |
| Fold 2 | 81.0% |
| Fold 3 | 85.7% |
| Fold 4 | 85.7% |
| Fold 5 | 61.9% |
| Fold 6 | 85.7% |
| Fold 7 | 66.7% |
| Fold 8 | 85.0% |
| Fold 9 | 85.0% |
| Fold 10 | 80.0% |
| **Average** | **79.4%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 1637 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 19489 |
| Avg Rules After Pruning | 166 |
| Pruning Ratio | 99.1% |
