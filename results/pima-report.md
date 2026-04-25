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
| **Our CMAR (Java)** | **73.4%** |
| Paper CMAR | 75.1% |
| Paper CBA | 72.9% |
| Paper C4.5 | 75.5% |

**Difference vs Paper CMAR:** -1.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 76.6% |
| Fold 2 | 64.9% |
| Fold 3 | 72.7% |
| Fold 4 | 72.7% |
| Fold 5 | 76.6% |
| Fold 6 | 74.0% |
| Fold 7 | 76.6% |
| Fold 8 | 72.7% |
| Fold 9 | 77.6% |
| Fold 10 | 69.7% |
| **Average** | **73.4%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 2 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 1585 |
| Avg Rules After Pruning | 213 |
| Pruning Ratio | 86.6% |
