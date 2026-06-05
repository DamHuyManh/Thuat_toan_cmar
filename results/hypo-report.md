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
| **Our CMAR (Java)** | **98.5%** |
| Paper CMAR | 98.4% |
| Paper CBA | 98.9% |
| Paper C4.5 | 99.2% |

**Difference vs Paper CMAR:** +0.1%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 98.4% |
| Fold 2 | 98.4% |
| Fold 3 | 98.1% |
| Fold 4 | 98.1% |
| Fold 5 | 98.4% |
| Fold 6 | 99.1% |
| Fold 7 | 98.4% |
| Fold 8 | 97.8% |
| Fold 9 | 99.4% |
| Fold 10 | 99.1% |
| **Average** | **98.5%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 236 ms |
| Avg Prediction Time | 1 ms |
| Avg Rules Mined | 86450 |
| Avg Rules After Pruning | 186 |
| Pruning Ratio | 99.8% |
