# Boosted CMAR — T=10 — Benchmark Report

**Date**: 2026-06-06
**Algorithm**: SAMME multiclass AdaBoost wrapper around CMARClassifier
**Base classifier params**: chi²=3.841 δ=4 maxAntLen=4 stratified=10 costSensitive

## Accuracy / F1 / Recall per dataset

| Dataset | Paper CMAR | Boosted CMAR | ΔAcc vs paper | F1 macro | Recall macro | Rounds | Total rules |
|---|---:|---:|---:|---:|---:|---:|---:|
| Cleve | 82.2% | **82.81%** | +0.61% | 0.8253 | 0.8261 | 10.0 | 1923 |
| Diabetes | 75.8% | **65.11%** | -10.69% | 0.3943 | 0.5000 | 10.0 | 0 |
| Heart | 82.2% | **81.11%** | -1.09% | 0.8081 | 0.8092 | 10.0 | 1769 |
| Pima | 75.1% | **65.11%** | -9.99% | 0.3943 | 0.5000 | 10.0 | 0 |
| **Average** | **78.82%** | **73.53%** | **-5.29%** | **0.6055** | **0.6588** | | |

## Aggregate metrics

| Metric | Boosted CMAR (T=10) |
|---|---:|
| Accuracy | 0.7353 |
| Precision macro | 0.5748 |
| **Recall macro** | **0.6588** |
| **F1 macro** | **0.6055** |
| F1 weighted | 0.6660 |
