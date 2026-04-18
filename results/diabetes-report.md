# Diabetes - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 768 instances, 8 numeric attrs, 2 classes |
| Instances | 768 |
| Attributes | 8 |
| Classes | 2 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 5 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **75.8%** |
| Paper CMAR | 75.8% |
| Paper CBA | 74.5% |
| Paper C4.5 | 74.2% |

**Difference vs Paper CMAR:** -0.0%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 75.3% |
| Fold 2 | 79.2% |
| Fold 3 | 72.7% |
| Fold 4 | 76.6% |
| Fold 5 | 74.0% |
| Fold 6 | 77.9% |
| Fold 7 | 70.1% |
| Fold 8 | 79.2% |
| Fold 9 | 76.3% |
| Fold 10 | 76.3% |
| **Average** | **75.8%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 36 ms |
| Avg Prediction Time | 0 ms |
| Avg Rules Mined | 1218 |
| Avg Rules After Pruning | 198 |
| Pruning Ratio | 83.7% |
