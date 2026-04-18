# CMAR Paper Dataset Comparison Report

## Overview
CMAR (Classification based on Multiple Association Rules) paper (Cheng et al., 2001) tested on 26 UCI datasets. Local implementation has all 26 files but with discrepancies in row/column counts.

## Key Findings

### CRITICAL MISMATCHES

**1. Anneal Dataset**
- **Local:** 798 rows, 39 cols
- **Paper:** 898 records, 38 attrs
- **Issue:** 100 rows missing. UCI source shows anneal contains 898 instances.
- **Root Cause:** Local dataset missing ~100 records. Likely incomplete download or preprocessing removed records.

**2. Horse-Colic Dataset**
- **Local:** 300 rows
- **Paper:** 368 records, 22 attrs  
- **Issue:** 68 rows missing
- **Root Cause:** UCI horse-colic splits into train (300) + test (68) files. Total 368. Local only contains training set.

**3. Diabetes / Pima Indians - SAME DATASET**
- **Local:** diabetes.csv (767 rows), pima-indians-diabetes.csv (767 rows)
- **Paper:** Lists both "Diabetes" and "Pima" separately (768 records each)
- **Finding:** Both files are identical - same UCI dataset with different names. Paper likely references same dataset twice as one consolidated version. Off-by-one discrepancy (767 vs 768) suggests one row removed during preprocessing.

**4. Zoo Dataset**
- **Local:** 18 cols
- **Paper:** 16 attrs + 1 class = 17 cols expected
- **Discrepancy:** Extra column(s) - likely includes animal name/ID as additional feature.

### MINOR DISCREPANCIES

**5. Iris Dataset**
- **Local:** 151 rows (includes header as row?)
- **Paper:** 150 records
- **Note:** Standard iris has exactly 150 instances. Header row may be miscounted.

**6. Column Count Patterns**
- Many local files have +1 column from paper specs
- Explanation: Local files include ID/index column not counted in paper's "attributes"
- Example: breast-cancer-wisconsin.csv (11 cols) = 10 attrs + 1 ID
- Example: crx.csv (16 cols) = 15 attrs + 1 class
- Example: glass.csv (11 cols) = 9 attrs + ID + class

### CONFIRMED DATASET IDENTITIES

**Cleve vs Heart (DIFFERENT DATASETS)**
- Cleve (processed.cleveland.csv): 303 rows, 14 cols - Cleveland heart disease subset
- Heart (heart.csv): 270 rows - separate heart disease dataset
- Both valid in paper; different medical studies

**Breast = Breast-Cancer-Wisconsin**
- Confirmed UCI dataset
- 699 instances with 11 columns (includes ID)

## Format Issues
- Several datasets are space-delimited but saved as .csv (australian, german, horse-colic, vehicle)
- No impact on data integrity if parsers handle whitespace

## Data Completeness Assessment
| Status | Count | Details |
|--------|-------|---------|
| Exact Match | 16 | australian, imports-85, heart, hypothyroid, ionosphere, labor-neg, led7, lymphography, sick, sonar, tic-tac-toe, vehicle, waveform, wine, german, hepatitis |
| Minor Variance | 4 | Column metadata (ID/class included in local) |
| Missing Data | 2 | anneal (100 rows), horse-colic (68 test rows) |
| Off-by-One | 3 | iris, diabetes, pima |
| Duplicate Name | 1 | diabetes = pima (same dataset) |

## Recommendations
1. **Anneal:** Download complete 898-record version from UCI
2. **Horse-Colic:** Merge horse-colic.data (300) + horse-colic.test (68) = 368 total
3. **Diabetes/Pima:** Keep single copy; remove duplicate
4. **Zoo:** Verify extra column purpose; remove ID if present
5. **Iris:** Confirm 150-record count (ignore headers)

## Sources
- [UCI Anneal Dataset](https://archive.ics.uci.edu/dataset/3/annealing)
- [UCI Horse-Colic Dataset](http://archive.ics.uci.edu/dataset/47/horse+colic)
- [CMAR Paper (ResearchGate)](https://www.researchgate.net/publication/3940198_CMAR_Accurate_and_Efficient_Classification_Based_on_Multiple_Class-Association_Rules)
- [CMAR Paper (IEEE Xplore)](https://ieeexplore.ieee.org/document/989541/)
- [Pima Indians Diabetes (Kaggle)](https://www.kaggle.com/datasets/uciml/pima-indians-diabetes-database)
