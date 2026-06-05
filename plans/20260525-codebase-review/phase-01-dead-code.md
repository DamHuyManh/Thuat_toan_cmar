# Phase 1 — Dead Code Removal

**Priority**: 🔴 HIGH
**Effort**: 2h
**Risk**: Low (failed approaches không dùng nữa)

## Context

Sau 22 experiments, có nhiều file/flag từ failed approaches vẫn ở codebase. Cluttering, làm khó maintain.

## Key insight

Phân biệt:
- **Failed nhưng valuable for ablation paper** → KEEP (BoostedCMAR, BayesianCMAR, ChiMerge — em DOC trong paper as negative results)
- **Failed AND không value** → REMOVE
- **Marginal AND no plan to use** → REMOVE

## Items to REMOVE

### Trong `Rule.java` (15 static fields → giảm xuống ~5)

Tất cả flag dưới đây test rồi marginal, không trong FINAL, không dùng cho ablation paper:

```java
// REMOVE — marginal sort alternatives
public static boolean useLongerRules = false;
public static boolean useLiftTieBreak = false;
public static boolean useLiftSecond = false;
public static boolean useChiFirst = false;
public static boolean useSortCompose = false;
public static boolean useSortChiLift = false;
public static boolean useConfLinear = false;
public static double confLinearAlpha = 0.1;
public static boolean useCondLift = false;
public static boolean useDominantClass = false;
public static boolean useClassWeightedSort = false;
```

→ Cả compareTo() logic dùng các flag này cũng remove (50+ dòng).

KEEP:
- `useHMLift`, `useLiftSort` — dùng trong ablation report
- `TOTAL_N`, `CLASS_FREQS` — dùng bởi CostSensitive

### Trong `RulePruner.java` (17 → 8 fields)

REMOVE:
```java
public static boolean useDualFilter = false;
public static double dualMinLift = 1.5;
public static double dualMinConf = 0.7;
public static double strictLift = 0.0;
public static boolean useAdaptMinConf = false;  // NO EFFECT proven
public static double adaptMinConfFloor = 0.3;
public static Map<Integer, Double> perClassMinConf = null;
```

KEEP:
- `useHMLift`, `minLift`, `minHM` — ablation
- `stratifiedTopN` — WIN cải tiến

### Trong `CMARClassifier.java` (11 → 4 fields)

REMOVE:
```java
public static boolean useHMWeightOnly = false;
public static boolean useAvgVote = false;
public static int perClassTopK = 0;
public static boolean useRelaxedUnanimity = false;  // FAIL
public static int unanimityMinCount = 3;
public static boolean useLaplaceWeight = false;     // NO EFFECT
```

KEEP:
- `useLiftWeight`, `useChiLiftWeight`, `useConfLiftWeight` — ablation reproducibility
- `useCostSensitive`, `imbalanceThreshold` — WIN cải tiến

### Trong `UCIDatasets.java` (1110 lines)

REMOVE: All `createXxxSynthetic()` methods (~300 lines dead code).
Tất cả 26 CSV có sẵn → synthetic không bao giờ chạy.

### Trong `BoostedBenchmarkRunner.java` CLI flags

REMOVE flags sau (failed/no-use):
- `--bootstrapRatio` (BR=0.7 fail)
- `--adaptMinConf` (no effect)
- `--adaptMinConfFloor`
- `--chiMerge` (worse than MDL)
- `--chiThreshold` (OOM với 2.706, không gain với 5.024)

## Files to DELETE (or move to `archive/`)

- `src/cmar/ChiMergeDiscretizer.java` (proven worse — keep MDL only)
  - Hoặc move sang `src/cmar/experimental/` để giữ cho reproducibility

## Verification

After cleanup:
1. Compile clean
2. Re-run FINAL config: `--method=bagging --T=10 --featureSubset=1.0 --stratified=10 --costSensitive --adaptMinSup --adaptFormula=sqrt --minSupScale=0.3`
3. Verify Acc 85.47%, F1 82.84%, Recall 83.48%
4. Re-run baseline `BenchmarkRunner --mode=improved` should also unchanged

## LOC reduction estimate

| File | Before | After | Saved |
|---|---:|---:|---:|
| Rule.java | 318 | ~180 | -138 |
| RulePruner.java | 659 | ~500 | -159 |
| CMARClassifier.java | 368 | ~280 | -88 |
| UCIDatasets.java | 1110 | ~800 | -310 |
| BoostedBenchmarkRunner.java | 313 | ~240 | -73 |
| ChiMergeDiscretizer.java | 141 | 0 (delete) | -141 |
| **Total** | | | **~-900 LOC** |

## Todo

- [ ] Snapshot current results before refactor
- [ ] Remove dead Rule.java flags + compareTo branches
- [ ] Remove dead RulePruner.java flags
- [ ] Remove dead CMARClassifier.java flags
- [ ] Remove synthetic dataset code
- [ ] Remove dead CLI flags
- [ ] Delete or move ChiMergeDiscretizer
- [ ] Compile clean
- [ ] Re-run FINAL config, verify identical
- [ ] Update CLAUDE.md / docs if any reference removed flags
