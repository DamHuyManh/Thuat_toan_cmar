# Boosted CMAR — T=10 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4 stratified=10 costSensitive

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Cleve | 82.2% | **82.23%** | +0.03% | 0.8193 | 0.8200 | 10.0 | 2033 |
| Diabetes | 75.8% | **75.26%** | -0.54% | 0.7046 | 0.6947 | 10.0 | 2122 |
| Heart | 82.2% | **81.85%** | -0.35% | 0.8146 | 0.8167 | 10.0 | 1862 |
| Pima | 75.1% | **75.26%** | +0.16% | 0.7046 | 0.6947 | 10.0 | 2122 |
| **Average** | **78.82%** | **78.65%** | **-0.17%** | **0.7608** | **0.7565** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=10) |
|---|---:|
| Accuracy | 0.7865 |
| Precision macro | 0.7848 |
| **Recall macro** | **0.7565** |
| **F1 macro** | **0.7608** |
| F1 weighted | 0.7796 |
