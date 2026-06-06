# Boosted CMAR — T=1 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Diabetes | 75.8% | **73.96%** | -1.84% | 0.6764 | 0.6693 | 1.0 | 199 |
| Iris | 94.0% | **93.33%** | -0.67% | 0.9325 | 0.9333 | 1.0 | 28 |
| **Average** | **84.90%** | **83.65%** | **-1.25%** | **0.8045** | **0.8013** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=1) |
|---|---:|
| Accuracy | 0.8365 |
| Precision macro | 0.8385 |
| **Recall macro** | **0.8013** |
| **F1 macro** | **0.8045** |
| F1 weighted | 0.8259 |
