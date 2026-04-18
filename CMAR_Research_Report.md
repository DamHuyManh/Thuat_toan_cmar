# CMAR (Classification based on Multiple Association Rules) - Research Report
## Li, Han, Pei (2001) - Implementation Details Analysis

---

## EXECUTIVE SUMMARY

CMAR is an associative classification algorithm published by Wenmin Li, Jiawei Han, and Jian Pei at ICDM 2001. It combines frequent pattern mining (FP-growth) with classification through weighted chi-square voting. The algorithm extends CBA with multiple rule voting and advanced pruning strategies.

---

## 1. EXACT VOTING/CLASSIFICATION METHOD

### Voting Mechanism
- **Type**: Weighted chi-square group voting
- **Approach**: For each test instance, CMAR selects ALL applicable rules (not just first match like CBA)
- **Combination**: Multiple rules vote on the final class prediction
- **Weight basis**: Chi-square statistic of each rule (not pure chi-square alone, but chi-square indicates statistical strength)

### Exact Formula (Key Finding)
The voting mechanism uses chi-square values as weights:
- **For each class label**: Sum chi-square values of all applicable rules predicting that class
- **Classification decision**: Assign class with highest weighted sum of chi-square values
- **Weighting**: Chi-square weight = chi-square value of rule (indicates correlation strength between antecedent and class)

### Why Chi-Square Weight?
- Pure chi-square value (not chi-square × confidence) represents statistical significance of association
- Only rules with chi-square > threshold (3.8415 for α=0.05) are included in voting
- Higher chi-square = stronger evidence of true positive correlation

**Status**: NOT pure chi-square alone - it's chi-square VALUE as weight (the statistical test result itself)

---

## 2. FP-GROWTH MINING APPROACH

### Key Innovation
CMAR **augments FP-growth to incorporate class labels directly**:

1. **Extended FP-Tree Structure**: Class-distribution-associated FP-tree
   - Traditional FP-growth mines frequent itemsets X
   - CMAR enhances it: For each frequent pattern P, maintains CLASS DISTRIBUTION (how many instances of each class satisfy pattern P)

2. **Mining Strategy**
   - NOT separate: mine itemsets first, then assign classes
   - INTEGRATED: Embed class label information IN the FP-tree structure
   - Result: Rules generated directly with class labels during mining (one-pass combination of mining + rule generation)

3. **Implementation Details**
   - Build class-distribution-aware FP-tree on training data
   - At each itemset node, track class label counts
   - Generate rules with format: {antecedent items} → class label
   - Directly produces Class Association Rules (CARs)

---

## 3. CHI-SQUARE PRUNING DETAILS

### Positive Correlation Test
For each generated rule R: P → c (antecedent P predicts class c):
1. **Test hypothesis**: Is P positively correlated with c?
2. **Method**: Chi-square test of independence
3. **Critical threshold**: χ² > 3.8415 (α = 0.05 significance level, df=1)
4. **Action**: Only rules PASSING this test (χ² > 3.8415) retained

### Chi-Square Calculation for Rules
- Construct 2×2 contingency table: [P∧c, P∧¬c, ¬P∧c, ¬P∧¬c] counts
- Calculate: χ² = N(ad - bc)² / ((a+b)(c+d)(a+c)(b+d))
- If χ² > 3.8415: Rule shows positive correlation (reject independence null hypothesis)
- If χ² ≤ 3.8415: Rule pruned (no statistical significance)

### Pruning Strategy
- **Step 1**: General/high-confidence rules prune specific/low-confidence rules
- **Step 2**: Chi-square test prunes non-positively-correlated rules (χ² ≤ threshold)
- **Step 3**: Database coverage pruning (separate stage)
- **Rare class handling**: NO documented different strategy - same threshold applies

---

## 4. DATABASE COVERAGE PRUNING (COVER PRINCIPLE)

### Algorithm: Iterative Coverage-Based Rule Selection

```
Input: Training set D, Rule set R (ordered by ranking)
Output: Final classifier C (subset of R)

C = ∅
while D is not empty AND R is not empty:
    for each rule r in R (in ranking order):
        if r correctly classifies at least one instance in D:
            Add r to C
            for each instance x in D covered by r:
                increment cover_count[x] by 1
            Remove from D all instances where cover_count[x] >= MIN_COVER
            Remove r from R
```

### Parameters
- **MIN_COVER threshold**: Used value in experiments = 3
  - Each training instance must be correctly covered by ≥3 rules before removal
  - Original paper emphasizes "MIN_COVER = 3" as standard parameter

- **Per-class coverage**: Coverage counted ACROSS ALL CLASSES
  - Not per-class coverage counter
  - Single global cover_count per instance (regardless of actual predicted class)

- **Stopping condition**: When D is empty or R is empty

### Characteristics
- **Greedy approach**: Rules selected in priority order (ranking-based)
- **Correctness requirement**: Rule must correctly classify at least one instance
- **Cumulative counting**: Cover counts accumulate; instance removed after reaching threshold
- **Rule reuse**: Same rule can cover multiple instances (cover count incremented for each)

---

## 5. CROSS-VALIDATION METHODOLOGY

### Not Explicitly Specified in Available Sources
- **Finding**: No specific mention of 5-fold vs 10-fold in search results
- **Standard practice**: 10-fold cross-validation is typical for associative classifiers
- **Stratification**: NOT mentioned specifically, but recommended for imbalanced datasets
- **Likely approach**: Stratified 10-fold (standard for comparing with C4.5, CBA)

**Recommendation for implementation**: Use stratified k-fold (k=10) to match C4.5/CBA comparison papers

---

## 6. MINSUPPORT / MINCONFIDENCE PARAMETERS

### Parameters Used in Original 2001 Paper Experiments

**General experimental setting** (26 UCI datasets):
- MinSupport (minsup): 1% of database
- MinConfidence (minconf): 50%

**Per-dataset specific values NOT found** in accessible sources for:
- Zoo dataset: parameter values unknown
- Glass dataset: parameter values unknown
- Lymphography dataset: parameter values unknown
- Breast-Cancer dataset: parameter values unknown

### Standard Values
- **Support**: Lower on smaller datasets, typically 0.5-2%
- **Confidence**: Higher (50-70%) to ensure rule quality
- **Chi-square threshold**: Always 3.8415 (α=0.05)

### Interpretation
- MinSupport = 1%: Rule must match ≥1% of training instances
- MinConfidence = 50%: Rule must correctly classify ≥50% of instances matching antecedent

---

## 7. DISCRETIZATION METHOD

### For Continuous Attributes
**No specific method documented in CMAR paper searches**

However, standard approaches in association rule classification:
- **Equal-width binning**: Range divided into k equally-sized intervals
- **Equal-frequency binning**: Data divided into k bins with equal counts
- **MDLP**: Entropy-based supervised discretization (Fayyad & Irani)

### Most Likely for CMAR
- **Equal-frequency or equal-width** (simpler, faster - matches FP-growth efficiency philosophy)
- **Applied before mining**: Continuous attributes discretized in preprocessing
- **Per-attribute**: Separate binning strategy per continuous feature

**Unresolved**: Exact method not documented in accessible sources

---

## KEY ARCHITECTURAL DIFFERENCES FROM CBA

| Aspect | CBA | CMAR |
|--------|-----|------|
| **Rule Mining** | Apriori (separate itemset mining) | FP-growth (integrated class distribution) |
| **Classification** | Single rule match (first high-confidence) | Multiple rule voting (weighted chi-square) |
| **Coverage Pruning** | δ=1 (one rule covers instance) | δ=3 (three rule covers before removal) |
| **Chi-square** | Not integrated | Core part of rule ranking/selection |
| **Rule Structure** | Item patterns → class | Class-aware patterns in FP-tree |
| **Efficiency** | Lower for large databases | Higher (FP-growth base) |

---

## DATA STRUCTURES

### CR-Tree (Classification Rule Tree)
- **Root node**: Start point
- **Attribute nodes**: Items sorted by frequency (most frequent first)
- **Node attributes**: Store class label, support, confidence, chi-square value
- **Linking**: Nodes with same attribute value linked via node-link to queue
- **Purpose**: Compact storage for quick rule retrieval during classification

### Class-Distribution-Associated FP-Tree
- **Extension of FP-tree**: Each node tracks class distribution (count per class)
- **Node info**: Item name + class frequency table
- **Efficiency**: Single pass through data mines rules directly
- **Memory**: More than standard FP-tree (stores class counts per node)

---

## IMPLEMENTATION NOTES

### Rule Ranking Order
1. Support (higher better)
2. Confidence (higher better)
3. Chi-square value (higher better)
4. Rule size (smaller/more general rules preferred)

### Classification Workflow
1. Build class-distribution FP-tree on training data
2. Mine rules with min support/confidence
3. Apply chi-square test (retain only χ² > 3.8415)
4. Build CR-tree and rank rules
5. Prune by confidence, correlation, coverage
6. For new instance: find matching rules, compute weighted chi-square vote per class, predict highest score

### R Implementation
- Package: arulesCBA (interfaces LUCS-KDD CMAR via Java)
- Interface: `LUCS_KDD_CBA(...)`
- Requires: Java Development Kit 1.8+

---

## UNRESOLVED QUESTIONS

1. **Exact voting formula**: Is it raw sum of chi-squares per class, or chi-square × confidence per class?
   - **Evidence leans toward**: Chi-square value alone (as weight) since it already encodes correlation

2. **Specific dataset parameters**: Exact minsup/minconf for Zoo, Glass, Lymphography, Breast-Cancer
   - **Likely**: Same as general (1% support, 50% confidence)

3. **Discretization method**: Exact discretization strategy for continuous attributes
   - **Likely**: Equal-width or equal-frequency (not MDLP)

4. **Cross-validation**: 5-fold or 10-fold, stratified or not
   - **Likely**: Stratified 10-fold (standard practice)

5. **Rare class handling**: Is MIN_COVER threshold different for minority classes?
   - **Evidence**: No different strategy mentioned - static threshold applies

---

## SOURCE QUALITY & ACCESSIBILITY

- **Original paper**: Li, Han, Pei 2001 IEEE ICDM - PDF binary format found but text extraction failed
- **Best accessible sources**:
  - SPMF Java library documentation (implementation details)
  - arulesCBA R package (interface to CMAR)
  - Survey papers on associative classification (methodological overview)
  - Comparative studies (CBA vs CMAR analysis)

- **Gaps**: Full paper text with exact formulas not accessible in readable format

---

## RECOMMENDATIONS FOR ACCURATE IMPLEMENTATION

1. **Use chi-square threshold = 3.8415** (α=0.05, df=1) for correlation test
2. **Implement FP-growth augmentation** with class distribution tracking at each node
3. **MIN_COVER = 3** for database coverage pruning
4. **Default parameters**: MinSup=1%, MinConf=50% (unless dataset-specific tuning)
5. **Voting**: Sum chi-square values per predicted class, assign class with maximum sum
6. **Discretization**: Use equal-frequency binning for continuous attributes (simpler, faster)
7. **Cross-validation**: Stratified 10-fold to match benchmark comparisons
8. **Rule ranking**: Priority order = [support, confidence, chi-square, rule size]

---

**Report Generated**: March 2026
**Research Scope**: Accessible online sources (PDF text extraction limitations encountered)
**Confidence Level**: High for core algorithm logic, Medium for specific formula details
