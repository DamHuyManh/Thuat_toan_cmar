# Boosted CMAR — T=10 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4 stratified=10 costSensitive

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Cleve | 82.2% | **82.23%** | +0.03% | 0.8187 | 0.8193 | 10.0 | 2033 |
| Diabetes | 75.8% | **73.70%** | -2.10% | 0.6839 | 0.6766 | 10.0 | 2122 |
| Heart | 82.2% | **81.11%** | -1.09% | 0.8066 | 0.8083 | 10.0 | 1862 |
| Pima | 75.1% | **73.70%** | -1.40% | 0.6839 | 0.6766 | 10.0 | 2122 |
| **Average** | **78.82%** | **77.68%** | **-1.14%** | **0.7483** | **0.7452** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=10) |
|---|---:|
| Accuracy | 0.7768 |
| Precision macro | 0.7743 |
| **Recall macro** | **0.7452** |
| **F1 macro** | **0.7483** |
| F1 weighted | 0.7687 |
