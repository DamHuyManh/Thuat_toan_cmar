# Waveform - CMAR Benchmark Report

## Dataset Info
| Property | Value |
|---|---|
| Description | 5000 instances, 21 numeric attrs, 3 classes |
| Instances | 5000 |
| Attributes | 21 |
| Classes | 3 |

## Parameters
| Parameter | Value |
|---|---|
| Min Support (absolute) | 45 |
| Min Support (ratio) | 0.01 |
| Min Confidence | 0.50 |
| Chi-Square Threshold | 3.841 (p=0.05) |
| Max Coverage Count | 3 |
| Cross-Validation | 10-fold |

## Results

### Our CMAR vs Paper Results
| Classifier | Accuracy |
|---|---|
| **Our CMAR (Java)** | **81.6%** |
| Paper CMAR | 83.2% |
| Paper CBA | 80.0% |
| Paper C4.5 | 78.1% |

**Difference vs Paper CMAR:** -1.6%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 79.6% |
| Fold 2 | 78.6% |
| Fold 3 | 79.0% |
| Fold 4 | 83.8% |
| Fold 5 | 80.6% |
| Fold 6 | 81.6% |
| Fold 7 | 82.8% |
| Fold 8 | 82.5% |
| Fold 9 | 83.1% |
| Fold 10 | 84.3% |
| **Average** | **81.6%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 575 ms |
| Avg Prediction Time | 4 ms |
| Avg Rules Mined | 75473 |
| Avg Rules After Pruning | 2650 |
| Pruning Ratio | 96.5% |
