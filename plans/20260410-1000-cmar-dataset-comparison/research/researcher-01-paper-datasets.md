# CMAR Paper Dataset Research Findings

## Paper Details
- Title: "CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules"
- Authors: Wenmin Li, Jiawei Han, Jian Pei
- Published: IEEE ICDM 2001, pages 369-376
- Evaluated: 26 UCI datasets

## 26 UCI Datasets - STATUS: INCOMPLETE

**Issue**: Exact dataset list with specifications (records, attributes, classes) not found in web-accessible sources. ResearchGate full PDF access denied (403 error).

**Known dataset**: Auto/Imports-85 with 205 records, 26 variables, insurance risk classification task.

**Action needed**: Access full paper PDF to extract complete Table 3 (Experimental datasets).

## Discretization Method

**Paper statement**: "the same method as CBA"

**CBA discretization**: Uses **Minimum Description Length (MDL)** principle for continuous attributes. CBA systems limited to nominal attributes; numeric attributes discretized using MDL before rule mining.

**Status**: Confirmed but need to verify EXACT CBA implementation details used by CMAR.

## Missing Value Handling

**Finding**: Not explicitly found in web searches. Paper likely addresses but specific method not documented in available summaries.

**Status**: UNRESOLVED - need full paper access

## Train/Test Split & Evaluation

**Key detail found**: "data shuffled using C4.5's shuffle utility"

**Status**: Implies cross-validation or stratified split methodology consistent with C4.5, but exact validation approach (k-fold, train/test ratio) not confirmed.

## Auto Dataset Question

**Answer**: YES - "Auto" in CMAR paper is **Imports-85 from UCI**
- 205 records, 26 attributes
- Automobile insurance risk classification
- UCI repository: https://archive.ics.uci.edu/dataset/10/automobile
- Legacy path: https://archive.ics.uci.edu/ml/machine-learning-databases/autos/imports-85.data

## UCI Repository Access Today

**Current URLs (2026)**:
- Primary: https://archive.ics.uci.edu/
- Datasets: https://archive.ics.uci.edu/datasets
- GitHub mirror: https://github.com/uci-ml-repo
- Legacy path still accessible: https://archive.ics.uci.edu/ml/machine-learning-databases/

**Status**: Old links return different pages but data still downloadable via archive.ics.uci.edu domain.

## Critical Unresolved Questions

1. Complete list of 26 dataset names and exact specifications
2. Exact missing value handling strategy
3. Exact cross-validation/split methodology (k-fold? ratio?)
4. Specific CBA discretization parameters used by CMAR

**Next steps**: Obtain full CMAR paper PDF (IEEE Xplore or institutional access) to extract Table 3 and methodology section.
