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
| **Our CMAR (Java)** | **96.5%** |
| Paper CMAR | 97.5% |
| Paper CBA | 97.0% |
| Paper C4.5 | 98.5% |

**Difference vs Paper CMAR:** -1.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 97.2% |
| Fold 2 | 96.4% |
| Fold 3 | 96.4% |
| Fold 4 | 96.4% |
| Fold 5 | 96.8% |
| Fold 6 | 96.4% |
| Fold 7 | 96.1% |
| Fold 8 | 94.3% |
| Fold 9 | 96.8% |
| Fold 10 | 98.6% |
| **Average** | **96.5%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 415 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 85874 |
| Avg Rules After Pruning | 146 |
| Pruning Ratio | 99.8% |
