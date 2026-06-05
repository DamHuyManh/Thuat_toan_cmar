# Boosted CMAR — T=10 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4 stratified=10 costSensitive

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Cleve | 82.2% | **82.83%** | +0.63% | 0.8252 | 0.8254 | 10.0 | 1929 |
| Diabetes | 75.8% | **74.87%** | -0.93% | 0.6885 | 0.6794 | 10.0 | 2417 |
| Heart | 82.2% | **81.85%** | -0.35% | 0.8139 | 0.8158 | 10.0 | 1773 |
| Pima | 75.1% | **74.87%** | -0.23% | 0.6885 | 0.6794 | 10.0 | 2417 |
| **Average** | **78.82%** | **78.60%** | **-0.22%** | **0.7540** | **0.7500** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=10) |
|---|---:|
| Accuracy | 0.7860 |
| Precision macro | 0.7894 |
| **Recall macro** | **0.7500** |
| **F1 macro** | **0.7540** |
| F1 weighted | 0.7756 |
