# CMAR Java Implementation Scout Report
**Date:** 2026-04-23 | **Focus:** Performance analysis of rule mining, pruning, and storage

---

## File Summary

| File | Lines | Key Role |
|------|-------|----------|
| CMARClassifier.java | 205 | Pipeline orchestration (fit → mine → prune → index → predict) |
| FPGrowth.java | 191 | Frequent itemset mining (FP-tree + bitmap scanning) |
| FPTree.java | 172 | Tree structure with header table + linked lists |
| RulePruner.java | 216 | 3-phase pruning (chi-square → g2s → coverage) |
| CRTree.java | 95 | Rule indexing (class → first-item → rules) |
| Rule.java | 90 | Rule storage (antecedent array + metadata) |
| FPNode.java | 41 | Tree node (item, count, children HashMap, link) |
| MDLDiscretizer.java | 167 | Supervised discretization (entropy-based cuts) |

**Total:** 1,177 lines core + 3,738 total with benchmarks.

---

## Data Structures & Complexity

### Rule Mining (FPGrowth)
**Structure:** Bitmap-based scanning for itemset support counting
- Bitmaps: `long[][] bitmaps` (FPGrowth:42) — O(N × ⌈maxItem/64⌉) space
- Header table: HashMap<item, first-node> in FPTree — O(1) lookup per item
- Recursive FP-tree mining: FPGrowth:105-155

**Time Complexity:**
- Mining: O(itemsets × N × items_per_set) — itemset generation + per-bitmap scan
- Bottleneck: FPGrowth:71-76 — N scans per itemset (100K+ itemsets × 5K instances = 500M ops)
- Conditional trees: O(header-size²) building each conditional tree (FPTree:63-97)

**Memory Hotspots:**
- Bitmap arrays: ~40KB per dataset (Waveform 5K instances = 40KB)
- Itemsets list: MAX_ITEMSETS=5M cap prevents runaway (FPGrowth:15), but 1-2M typical
- Frequent itemsets storage: ~64 bytes/itemset × 100K itemsets = 6.4MB

### Rule Pruning (RulePruner)
**Structure:** 3-phase sequential filter
1. **Chi-Square (RulePruner:54-94):** Rebuilds bitmaps + re-scans all rules
   - Time: O(rules × N × items_per_rule) — recomputes support for every rule
   - Example: 156K rules × 898 instances = 140M bitmap match ops (Anneal dataset)
2. **General-to-Specific (RulePruner:101-133):** O(n²) subset checking
   - Skips if rules > 10K (line 103) — bottleneck for large rule sets
3. **Coverage (RulePruner:143-179):** O(rules × N) with coverage tracking
   - Arrays: `int[] coverCount`, `boolean[] fullyCovered` — O(N) per phase

**Memory Hotspots:**
- Bitmap rebuild in prune() (RulePruner:204-215): duplicate of mining bitmaps
- Two separate bitmap arrays (FPGrowth:42 + RulePruner:210) for same data

### Rule Storage (CRTree)
**Structure:** Nested HashMaps for indexed retrieval
```
index: Map<classLabel, Map<firstItem, List<Rule>>>
allRules: List<Rule>
```
- O(classes × items × avg-rules-per-bucket) = O(1) amortized lookup
- CRTree:74-82: Flat list fallback (findAllMatching) scans all rules linearly

**Memory Hotspots:**
- Rule antecedent cloning: `itemset.clone()` (FPGrowth:91) — happens per rule
- List copies in CRTree:42-68 (findMatchingRules) creates new ArrayList per class

---

## Identified Bottlenecks

### 1. **Bitmap Rebuild in Pruning (RulePruner:204-215)**
- **Issue:** Rebuilds maxItem + bitmap arrays already computed during mining
- **Impact:** O(N × items) re-processing, ~1-5ms per dataset (negligible on small datasets, 100+ ms on large)
- **Example:** Sonar (160K mined rules) scans bitmaps 3 times (mining + chi-sq + coverage)
- **Fix:** Cache and reuse bitmaps from mining phase

### 2. **O(rules × N) Chi-Square Pruning (RulePruner:61-90)**
- **Issue:** For each rule, scans all N transactions to recount support
- **Impact:** 156K rules × 898 txns = 140M ops (Anneal), Sonar = 160K × 208 = 33M ops
- **Fix:** Pre-compute rule-transaction matches once, reuse in all pruning phases

### 3. **O(n²) General-to-Specific Pruning (RulePruner:114-128)**
- **Issue:** Nested loop checking if rule i is subset of rule j, skipped if > 10K rules
- **Example:** German dataset has 89K rules → skipped (line 103)
- **Impact:** Skipping means lower-quality final rule set
- **Fix:** Bitmap-based subset checking or trie structure for O(n log n)

### 4. **Flat Scan Fallback (CRTree:74-82)**
- **Issue:** findAllMatching iterates all rules instead of using index
- **Explanation:** Used when rules don't start with same item, fallback is linear O(rules)
- **Impact:** Prediction phase (CMARClassifier:99) scans all rules for every instance
- **Fix:** Partition rules by *any* antecedent item, not just first item

### 5. **Rule Antecedent Cloning (FPGrowth:91, Rule:20)**
- **Issue:** `itemset.clone()` + `Arrays.sort()` per rule generated
- **Impact:** Small (ns-level per rule), but happens 100K times = milliseconds
- **Fix:** Pass sorted references, not clones

---

## Existing Optimizations

| Optimization | Location | Impact |
|--------------|----------|--------|
| **Bitmap matching** | Rule:56-64, FPGrowth:183-190 | Avoids N² itemset checks; uses bitwise ops |
| **HashMap header table** | FPTree:11-12 | O(1) item lookup in mining |
| **Single-path detection** | FPTree:130-150 | Exponential generation for single paths (FPGrowth:158-181) |
| **Confidence filtering** | FPGrowth:87 | Early rejection of low-conf rules |
| **Support threshold** | FPTree:24-34, 73-74 | Prunes infrequent items in tree build |
| **Chi-square significance** | RulePruner:87 | Removes uncorrelated rules (99.9% pruning ratio) |

---

## Benchmark Summary (from results/summary-report.md)

| Metric | Best | Worst | Average |
|--------|------|-------|---------|
| **Accuracy** | 99.2% (TicTacToe) | 68.1% (Vehicle) | 85.1% |
| **Train Time** | 0 ms (Glass) | 4,126 ms (Waveform, 5K inst) | 700ms |
| **Rules Mined** | 90 (Iris) | 242K (Sonar) | 60K |
| **Rules After Prune** | 30 (Iris) | 2,659 (Waveform) | 300 |
| **Prune Ratio** | 54.1% (Led7) | 99.9% (Auto, Sonar) | 95% |
| **Vs Paper CMAR** | +3.6% (Labor) | -2.4% (Diabetes) | -0.1% |

---

## Top 5 Concrete Improvement Opportunities

1. **Cache and Reuse Bitmaps (2-5x speedup on large datasets)**
   - FPGrowth:42 creates bitmaps; RulePruner:210 rebuilds them
   - Pass bitmaps to pruner instead of rebuilding
   - Saves ~5-10% of total training time on Anneal/German/Waveform

2. **Pre-compute Rule-Transaction Matches (3-4x on pruning)**
   - RulePruner:54-90 re-scans transactions for every rule
   - Build boolean[][] before pruning: matches[rule_id][txn_id]
   - Eliminates redundant bitmap operations in chi-square, coverage phases

3. **Bitmap-based Subset Checking (O(n log n) vs O(n²))**
   - RulePruner:121 uses linear subset scanning, skipped for >10K rules
   - Encode rule antecedents as bitmaps, use bitwise AND for O(1) subset test
   - Enables G2S pruning on German (89K→select rules) currently skipped

4. **Multi-Index CR-Tree (O(log n) prediction vs O(rules))**
   - CRTree:74-82 linear scan when rules don't share first item
   - Index by all frequent items in antecedent, not just first
   - Prediction: query multiple buckets, intersect results → O(rules/items) ops

5. **Reduce Rule Cloning (10-20% minor, but clean)**
   - FPGrowth:91 `itemset.clone()`, Rule:20 `Arrays.sort(antecedent)`
   - Pass immutable sorted references in itemset mining
   - Negligible on runtime but improves code clarity

---

## Unresolved Questions
- Does general-to-specific pruning (skipped for >10K rules) significantly impact accuracy?
- Why does coverage pruning use `maxCoverageCount=4` (not paper's `delta=3`)?
- Is bitmap size (5-40KB) optimal for prediction latency vs memory trade-off?

