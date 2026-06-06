# Sick - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 2800 instances, 29 mixed attrs, 2 classes |
| Instances | 2800 |
| Attributes | 29 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 12 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **96.8%** |
| Paper CMAR | 97.5% |
| Paper CBA | 97.0% |
| Paper C4.5 | 98.5% |

**Difference vs Paper CMAR:** -0.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 95.0% |
| Fold 2 | 96.4% |
| Fold 3 | 97.1% |
| Fold 4 | 97.9% |
| Fold 5 | 97.1% |
| Fold 6 | 97.1% |
| Fold 7 | 97.9% |
| Fold 8 | 95.7% |
| Fold 9 | 96.4% |
| Fold 10 | 97.1% |
| **Average** | **96.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 631 ms |
| Avg Prediction Time | 2 ms |
| Avg Rules Mined | 85874 |
| Avg Rules After Pruning | 279 |
| Pruning Ratio | 99.7% |
