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
| **Our CMAR (Java)** | **81.0%** |
| Paper CMAR | 82.6% |
| Paper CBA | 82.1% |
| Paper C4.5 | 82.6% |

**Difference vs Paper CMAR:** -1.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 76.3% |
| Fold 2 | 92.1% |
| Fold 3 | 78.4% |
| Fold 4 | 78.4% |
| Fold 5 | 81.1% |
| Fold 6 | 73.0% |
| Fold 7 | 77.8% |
| Fold 8 | 86.1% |
| Fold 9 | 86.1% |
| Fold 10 | 80.6% |
| **Average** | **81.0%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 2046 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 129267 |
| Avg Rules After Pruning | 397 |
| Pruning Ratio | 99.7% |
