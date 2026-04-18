# CMAR Algorithm: Datasets, Parameters & Benchmarks

## Summary

The original CMAR paper by Li, Han, Pei (2001, IEEE ICDM) conducted extensive experiments on **26 UCI datasets**. The paper reports that CMAR achieved better average classification accuracy compared to CBA and C4.5, with consistent and effective performance across various database types.

## Original Paper Details

- **Title**: CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules
- **Authors**: Wenmin Li, Jiawei Han, Jian Pei
- **Published**: 2001 IEEE International Conference on Data Mining (ICDM)
- **Pages**: 369-376
- **Key Result**: CMAR outperforms CBA and C4.5 on accuracy across 26 UCI datasets

## Known Datasets Used

The paper evaluated CMAR on 26 UCI datasets. Common benchmarks mentioned in literature citations include:
- Iris (150 instances, 4 attributes, 3 classes)
- Wine (178 instances, 13 attributes, 3 classes)
- Breast Cancer Wisconsin (699 instances, 10 attributes, 2 classes)
- Mushroom (8,124 instances, 22 attributes, 2 classes)
- Heart (303 instances, 13 attributes, 2 classes)
- Zoo (101 instances, 17 attributes, 7 classes)
- Glass (214 instances, 9 attributes, 6 classes)
- Tic-Tac-Toe (958 instances, 9 attributes, 2 classes)
- Lymphography (148 instances, 18 attributes, 4 classes)
- Vehicle (846 instances, 18 attributes, 4 classes)

## Algorithm Parameters

Default parameters commonly used in CMAR implementations:
- **min_support**: 0.05 - 0.2 (varies by dataset)
- **min_confidence**: 0.5 - 0.9 (typically 0.5+)
- **Default values**: min_sup=0.1, min_conf=0.5

## Critical Note: Exact Benchmark Data Unavailable

The specific numeric accuracy results from the original 2001 paper (Table 3) were **not accessible** in freely available sources during this research. While the paper is widely cited and accessible via:
- SFU publications: https://www.cs.sfu.ca/~jpei/publications/cmar.pdf
- IEEE Xplore: https://ieeexplore.ieee.org/document/989541/
- ACM DL: https://dl.acm.org/doi/10.5555/645496.657866

The PDF content extraction failed to render readable tables due to compression.

## Verified Performance Claims

All sources consistently report (without exact percentages):
1. CMAR > C4.5 on average accuracy
2. CMAR > CBA on average accuracy (though some studies report CBA slightly better on specific datasets)
3. Method is more efficient than CBA in runtime
4. Results are less sensitive to threshold selection than CBA/CPAR
5. Tested on 26 UCI datasets with consistent effectiveness

## Related Papers & Follow-up Work

- **CPAR (2003)**: Yin & Han - Similar approach with predictive rules
- **QCBA (2022)**: Improved quantitative CBA with postprocessing
- **GARC**: Genetic Algorithm-based Associative Classification
- Surveys: IJERT (2013), IJST (2015), Big Data Analytics (2018-2019)

## Unresolved Questions

1. Exact accuracy percentages for each dataset in original paper
2. Specific min_support/min_confidence per dataset in 2001 experiments
3. Whether all 10 listed datasets were included in the 26
4. Cross-validation method used (k-fold, hold-out, percentage split)
5. Statistical significance testing details

## Recommendation

To obtain exact benchmark data, access full paper via:
- University library with IEEE/ACM subscriptions
- Direct request to authors (J. Han at UIUC)
- KEEL dataset repository (may have archived results)

---

**Research Date**: March 12, 2026
**Sources**: 40+ academic searches, multiple PDF fetch attempts
