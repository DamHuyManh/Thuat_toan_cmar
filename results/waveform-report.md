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
| **Our CMAR (Java)** | **81.9%** |
| Paper CMAR | 83.2% |
| Paper CBA | 80.0% |
| Paper C4.5 | 78.1% |

**Difference vs Paper CMAR:** -1.3%

### Per-Fold Accuracy
| Fold | Accuracy |
|---|---|
| Fold 1 | 83.4% |
| Fold 2 | 79.4% |
| Fold 3 | 81.4% |
| Fold 4 | 85.0% |
| Fold 5 | 80.8% |
| Fold 6 | 79.8% |
| Fold 7 | 80.4% |
| Fold 8 | 81.3% |
| Fold 9 | 83.9% |
| Fold 10 | 83.7% |
| **Average** | **81.9%** |

### Performance
| Metric | Value |
|---|---|
| Avg Training Time | 25511 ms |
| Avg Prediction Time | 60 ms |
| Avg Rules Mined | 72235 |
| Avg Rules After Pruning | 3586 |
| Pruning Ratio | 95.0% |
