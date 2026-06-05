# Lymphography - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 148 instances, 18 attrs, 4 classes |
| Instances | 148 |
| Attributes | 18 |
| Classes | 4 |

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
| **Our CMAR (Java)** | **82.5%** |
| Paper CMAR | 83.1% |
| Paper CBA | 77.8% |
| Paper C4.5 | 73.5% |

**Difference vs Paper CMAR:** -0.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 77.8% |
| Fold 2 | 81.3% |
| Fold 3 | 86.7% |
| Fold 4 | 86.7% |
| Fold 5 | 78.6% |
| Fold 6 | 78.6% |
| Fold 7 | 78.6% |
| Fold 8 | 78.6% |
| Fold 9 | 85.7% |
| Fold 10 | 92.9% |
| **Average** | **82.5%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 74 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 65800 |
| Avg Rules After Pruning | 170 |
| Pruning Ratio | 99.7% |
