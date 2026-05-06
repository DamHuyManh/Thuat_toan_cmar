# Cleve - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 303 instances, 13 attrs, 2 classes |
| Instances | 303 |
| Attributes | 13 |
| Classes | 2 |

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
| **Our CMAR (Java)** | **82.6%** |
| Paper CMAR | 82.2% |
| Paper CBA | 82.8% |
| Paper C4.5 | 78.2% |

**Difference vs Paper CMAR:** +0.4%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 80.6% |
| Fold 2 | 80.6% |
| Fold 3 | 67.7% |
| Fold 4 | 87.1% |
| Fold 5 | 73.3% |
| Fold 6 | 86.7% |
| Fold 7 | 76.7% |
| Fold 8 | 96.7% |
| Fold 9 | 90.0% |
| Fold 10 | 86.2% |
| **Average** | **82.6%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 13 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 16274 |
| Avg Rules After Pruning | 276 |
| Pruning Ratio | 98.3% |
