# Anneal - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 898 instances, 38 mixed attrs, 6 classes |
| Instances | 898 |
| Attributes | 38 |
| Classes | 6 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 8 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **98.0%** |
| Paper CMAR | 97.3% |
| Paper CBA | 97.9% |
| Paper C4.5 | 94.8% |

**Difference vs Paper CMAR:** +0.7%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 98.9% |
| Fold 2 | 97.8% |
| Fold 3 | 95.6% |
| Fold 4 | 98.9% |
| Fold 5 | 98.9% |
| Fold 6 | 97.8% |
| Fold 7 | 94.4% |
| Fold 8 | 98.9% |
| Fold 9 | 100.0% |
| Fold 10 | 98.9% |
| **Average** | **98.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 558 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 156588 |
| Avg Rules After Pruning | 183 |
| Pruning Ratio | 99.9% |
