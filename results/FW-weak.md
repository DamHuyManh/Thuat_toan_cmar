# Boosted CMAR — T=10 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4 stratified=10 costSensitive

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Cleve | 82.2% | **83.86%** | +1.66% | 0.8356 | 0.8359 | 10.0 | 2033 |
| Diabetes | 75.8% | **75.27%** | -0.53% | 0.7121 | 0.7035 | 10.0 | 2122 |
| Heart | 82.2% | **80.37%** | -1.83% | 0.7986 | 0.7992 | 10.0 | 1862 |
| Pima | 75.1% | **75.27%** | +0.17% | 0.7121 | 0.7035 | 10.0 | 2122 |
| **Average** | **78.82%** | **78.69%** | **-0.13%** | **0.7646** | **0.7605** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=10) |
|---|---:|
| Accuracy | 0.7869 |
| Precision macro | 0.7848 |
| **Recall macro** | **0.7605** |
| **F1 macro** | **0.7646** |
| F1 weighted | 0.7818 |
