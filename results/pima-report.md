# Pima - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 768 instances, 8 numeric attrs, 2 classes |
| Instances | 768 |
| Attributes | 8 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 5 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **75.1%** |
| Paper CMAR | 75.1% |
| Paper CBA | 72.9% |
| Paper C4.5 | 75.5% |

**Difference vs Paper CMAR:** +0.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 76.6% |
| Fold 2 | 76.6% |
| Fold 3 | 75.3% |
| Fold 4 | 74.0% |
| Fold 5 | 71.4% |
| Fold 6 | 81.8% |
| Fold 7 | 74.0% |
| Fold 8 | 79.2% |
| Fold 9 | 69.7% |
| Fold 10 | 72.4% |
| **Average** | **75.1%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 46 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 1420 |
| Avg Rules After Pruning | 357 |
| Pruning Ratio | 74.9% |
