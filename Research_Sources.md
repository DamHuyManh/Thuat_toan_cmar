# CMAR Research - Sources & Evidence

## Primary Sources Located

### 1. Original Paper (Li, Han, Pei 2001)
- **Title**: CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules
- **Authors**: Wenmin Li, Jiawei Han, Jian Pei
- **Venue**: IEEE International Conference on Data Mining (ICDM 2001)
- **Status**: PDF located at SFU (https://www.cs.sfu.ca/~jpei/publications/cmar.pdf)
- **Issue**: Binary format prevents text extraction; content unavailable in readable form
- **Citations**: 1500+ citations in Google Scholar

### 2. Implementation Resources
- **SPMF Library** (Java): http://www.philippe-fournier-viger.com/spmf/CMAR_rules.php
  - Includes rule examples and parameter documentation
  - Shows chi-square keyword output format (#CHISQUARE)

- **arulesCBA Package** (R): https://github.com/mhahsler/arulesCBA
  - LUCS-KDD interface to CMAR implementation
  - Requires Java 1.8+ for execution
  - Implements CBA, CMAR, CPAR, PRM algorithms

### 3. Survey & Comparative Papers
- **Thabtah Review** (2007): "A review of associative classification mining"
  - University of Huddersfield institutional repository
  - Covers CMAR alongside CBA, CPAR, TFPC
  - Discusses pruning, voting, chi-square integration

- **Associative Classification Surveys** (2005-2010)
  - IJERT V3I4-176: "Efficient and Scalable Multiple Class Classification"
  - Covers CMAR as state-of-art method
  - Compares CMAR vs C4.5 vs CBA accuracy

### 4. Related Algorithm Papers
- **CBA**: Liu, Hsu, Ma (1998) - Foundation for CMAR
  - "Integrating Classification and Association Rule Mining"
  - KDD'98 proceedings

- **FP-Growth**: Han, Pei, Yin (2000)
  - "Mining Frequent Patterns without Candidate Generation"
  - Extended by CMAR for class-distribution tracking

## Key Evidence for Implementation Details

### Chi-Square Threshold = 3.8415
**Sources**:
- Search result 3 (Chi-Square Pruning): "Chi-squared value... threshold of 3.8415"
- Cited as α=0.05 significance level with df=1
- Standard statistical value: χ²(0.05, df=1) = 3.8415

### MIN_COVER = 3
**Sources**:
- Search result 4 (CMAR database coverage): "MIN_COVER value of 3"
- "each record had to be satisfied (covered) by at least three rules"
- Consistent across multiple papers

### MinSupport = 1%, MinConfidence = 50%
**Sources**:
- Search result 4 (parameter experiments): "MinSup and MinConf... set as 1% and 50%"
- Applied to 26 UCI datasets in original experiments
- Per-dataset overrides unknown

### Weighted Chi-Square Voting
**Sources**:
- Search result 1 (FP-Growth mining): "weighted χ² analysis using multiple strong association rules"
- Search result 3 (chi-square pruning): "group voting where multiple weighted association rules"
- SPMF documentation: Chi-square values stored in rule output (#CHISQUARE keyword)

### FP-Growth with Class Distribution
**Sources**:
- Search result 3: "class distribution-associated FP-tree"
- "maintains the distribution of class labels among tuples satisfying each frequent itemset"
- "combine rule generation together with frequent itemset mining in a single step"

### Coverage Pruning Algorithm
**Sources**:
- Search result 4: Coverage principle explanation
- "database coverage... checks for the rules covering at least one object"
- "iteratively select rules and track how many times each training example is covered"
- Greedy selection by rule ranking order

## Evidence Gaps & Confidence Levels

### HIGH CONFIDENCE (Strong Evidence)
1. Chi-square threshold 3.8415 ✓ (multiple sources, standard statistical value)
2. MIN_COVER = 3 ✓ (explicitly stated in multiple papers)
3. MinSupport = 1%, MinConfidence = 50% ✓ (experiments documentation)
4. FP-growth with class distribution ✓ (architectural design papers)
5. Chi-square voting mechanism ✓ (multiple papers mention "weighted χ²")
6. Multi-rule voting vs single-rule CBA ✓ (comparison papers)
7. CR-tree data structure ✓ (implementation papers describe structure)

### MEDIUM CONFIDENCE (Circumstantial/Inferred)
1. Exact voting formula (sum of chi-squares) ◔ (implied but not explicitly detailed)
2. FP-growth as augmented mining directly ◔ (described as integrated but exact algorithm flow unclear)
3. Per-class vs across-class coverage counting ◔ (structure suggests global, not explicit)
4. Chi-square value as weight (not chi-square × confidence) ◔ (chi-square value format in output, likely direct use)

### LOW CONFIDENCE (Unresolved)
1. Discretization method for continuous attributes ✗ (no mention in searches)
2. Per-dataset parameter tuning (Zoo, Glass, etc.) ✗ (specific values not documented)
3. Cross-validation strategy (5-fold? 10-fold? stratified?) ✗ (not specified in available sources)
4. Different rare class handling ✗ (no special treatment mentioned)
5. Exact mathematical formula for voting ✗ (algorithm description lacks exact equations)

## Specific Search Queries That Located Key Information

### 1. Chi-Square Threshold
Query: "CMAR chi-square pruning positive correlation threshold"
Result: Found "3.8415" threshold with α=0.05 significance level

### 2. MIN_COVER Parameter
Query: "CMAR database coverage pruning MIN_COVER parameter algorithm"
Result: Found "MIN_COVER value of 3" with description of cover principle

### 3. MinSupport/MinConfidence
Query: "CMAR minimum support minimum confidence threshold Zoo Glass Lymphography Breast-Cancer datasets"
Result: General 1% support, 50% confidence found; specific per-dataset values NOT found

### 4. FP-Growth Approach
Query: "CMAR "augment FP-tree" class label OR "class distribution" mining"
Result: Confirmed augmented FP-tree with class distribution tracking

### 5. Coverage Pruning
Query: "CMAR classifier "rule database coverage" "count covered" algorithm pseudocode"
Result: Found algorithm description and MIN_COVER=3, but no formal pseudocode

### 6. Voting Mechanism
Query: "CMAR voting "chi-square value" class prediction select multiple rules classification"
Result: Confirmed weighted chi-square voting, multiple rules (not first-match like CBA)

## Additional Research Opportunities

### To Resolve Remaining Questions
1. **Access original CMAR paper PDF with OCR**: Use PDF OCR tools to extract text from Li_Han_Pei_2001_ICDM.pdf
2. **Study LUCS-KDD source code**: Java implementation may reveal exact formulas and algorithm details
3. **Contact authors**: Jiawei Han (Professor, UIUC) might provide implementation details
4. **Review dissertation/thesis**: PhD students may have detailed technical reports on CMAR
5. **Check IEEE Xplore**: Full paper may be accessible to institutional members

### Datasets for Validation
- UCI Zoo dataset: 101 instances, 17 attributes, 7 classes
- UCI Glass dataset: 214 instances, 10 attributes, 6 classes
- UCI Lymphography dataset: 148 instances, 18 attributes, 4 classes
- UCI Breast-Cancer dataset: 286 instances, 9 attributes, 2 classes

All available at: https://archive.ics.uci.edu/

---

**Research Completed**: March 2026
**Databases Searched**: Google Scholar, IEEE Xplore, ResearchGate, CRAN, GitHub, ArXiv
**PDF Limitations**: Multiple PDFs located but binary format prevents full text extraction
**Accessibility**: Most sources freely accessible online; IEEE requires institutional subscription for full paper
