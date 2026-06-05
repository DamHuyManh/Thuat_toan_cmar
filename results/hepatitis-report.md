# Hepatitis - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 155 instances, 19 mixed attrs, 2 classes |
| Instances | 155 |
| Attributes | 19 |
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
| **Our CMAR (Java)** | **81.5%** |
| Paper CMAR | 80.5% |
| Paper CBA | 81.8% |
| Paper C4.5 | 80.6% |

**Difference vs Paper CMAR:** +1.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 70.6% |
| Fold 2 | 82.4% |
| Fold 3 | 62.5% |
| Fold 4 | 86.7% |
| Fold 5 | 93.3% |
| Fold 6 | 80.0% |
| Fold 7 | 93.3% |
| Fold 8 | 93.3% |
| Fold 9 | 73.3% |
| Fold 10 | 80.0% |
| **Average** | **81.5%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 45 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 38172 |
| Avg Rules After Pruning | 131 |
| Pruning Ratio | 99.7% |
