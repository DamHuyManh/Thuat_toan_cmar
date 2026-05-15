# Hypo - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 3163 instances, 25 mixed attrs, 2 classes |
| Instances | 3163 |
| Attributes | 25 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 28 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **97.9%** |
| Paper CMAR | 98.4% |
| Paper CBA | 98.9% |
| Paper C4.5 | 99.2% |

**Difference vs Paper CMAR:** -0.5%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 97.8% |
| Fold 2 | 97.8% |
| Fold 3 | 97.5% |
| Fold 4 | 97.8% |
| Fold 5 | 98.1% |
| Fold 6 | 98.1% |
| Fold 7 | 97.8% |
| Fold 8 | 97.2% |
| Fold 9 | 98.1% |
| Fold 10 | 99.4% |
| **Average** | **97.9%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 598 ms |
| Avg Prediction Time | 3 ms |
| Avg Rules Mined | 86450 |
| Avg Rules After Pruning | 176 |
| Pruning Ratio | 99.8% |
