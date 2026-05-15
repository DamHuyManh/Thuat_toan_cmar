# Horse - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 368 instances, 22 mixed attrs, 2 classes |
| Instances | 368 |
| Attributes | 22 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 3 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **82.3%** |
| Paper CMAR | 82.6% |
| Paper CBA | 82.1% |
| Paper C4.5 | 82.6% |

**Difference vs Paper CMAR:** -0.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 76.3% |
| Fold 2 | 92.1% |
| Fold 3 | 81.1% |
| Fold 4 | 83.8% |
| Fold 5 | 75.7% |
| Fold 6 | 81.1% |
| Fold 7 | 83.3% |
| Fold 8 | 83.3% |
| Fold 9 | 86.1% |
| Fold 10 | 80.6% |
| **Average** | **82.3%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 467 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 129386 |
| Avg Rules After Pruning | 397 |
| Pruning Ratio | 99.7% |
