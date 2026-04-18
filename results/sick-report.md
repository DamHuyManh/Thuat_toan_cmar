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
| Min Support (absolute) | 25 |
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
| Fold 1 | 97.5% |
| Fold 2 | 96.4% |
| Fold 3 | 96.4% |
| Fold 4 | 96.4% |
| Fold 5 | 98.6% |
| Fold 6 | 96.4% |
| Fold 7 | 96.8% |
| Fold 8 | 95.4% |
| Fold 9 | 96.8% |
| Fold 10 | 97.5% |
| **Average** | **96.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 10947 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 85172 |
| Avg Rules After Pruning | 178 |
| Pruning Ratio | 99.8% |
