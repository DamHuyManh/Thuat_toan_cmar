# Labor - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 57 instances, 16 mixed attrs, 2 classes |
| Instances | 57 |
| Attributes | 16 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 2 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **91.7%** |
| Paper CMAR | 89.7% |
| Paper CBA | 86.3% |
| Paper C4.5 | 79.3% |

**Difference vs Paper CMAR:** +2.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 83.3% |
| Fold 2 | 100.0% |
| Fold 3 | 100.0% |
| Fold 4 | 83.3% |
| Fold 5 | 100.0% |
| Fold 6 | 83.3% |
| Fold 7 | 66.7% |
| Fold 8 | 100.0% |
| Fold 9 | 100.0% |
| Fold 10 | 100.0% |
| **Average** | **91.7%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 19 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 24003 |
| Avg Rules After Pruning | 49 |
| Pruning Ratio | 99.8% |
