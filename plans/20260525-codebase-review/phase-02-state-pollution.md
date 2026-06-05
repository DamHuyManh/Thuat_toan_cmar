# Phase 2 — Static State Pollution Fix

**Priority**: 🔴 HIGH
**Effort**: 4h
**Risk**: Medium (touches core APIs)

## Context

Hiện tại **43 public static mutable fields** rải khắp:
- `Rule.java`: 15 static (sort flags)
- `RulePruner.java`: 17 static (filter flags)
- `CMARClassifier.java`: 11 static (vote flags)

→ **Global state hell**. Vấn đề:
1. Hard to test (state leak giữa tests)
2. Hard to parallelize (race conditions)
3. Hard to reason about (where set? where read?)
4. Reproducibility risk (forget to reset → wrong results)

## Solution: Config object pattern

Thay flags global bằng **immutable config object** passed vào constructor.

### Before:
```java
cmar.RulePruner.stratifiedTopN = 10;
cmar.CMARClassifier.useCostSensitive = true;
CMARClassifier c = new CMARClassifier(...);
```

### After:
```java
CMARConfig config = CMARConfig.builder()
    .stratifiedTopN(10)
    .costSensitive(true)
    .adaptMinSup(true, "sqrt")
    .build();
CMARClassifier c = new CMARClassifier(config);
```

## Implementation steps

### Step 1: Create `CMARConfig` class
```java
public final class CMARConfig {
    // Pruning config
    public final int stratifiedTopN;        // 0 = disabled
    public final boolean hmLift;
    public final double minLift, minHM;
    
    // Voting config
    public final boolean costSensitive;
    public final double imbalanceThreshold;
    public final boolean liftWeight, chiLiftWeight, confLiftWeight;
    public final int topKGlobal;
    
    // Mining config
    public final int minSupport;
    public final double minConfidence;
    public final double chiSquareThreshold;
    public final int maxCoverageCount, maxRulesPerClass, maxAntecedentLen;
    
    public static Builder builder() { return new Builder(); }
    public static class Builder { ... }
}
```

### Step 2: Refactor CMARClassifier
- Constructor takes `CMARConfig` only
- Reads from config (not static)
- Remove static fields

### Step 3: Refactor RulePruner
- Constructor takes `CMARConfig`
- Remove static fields

### Step 4: Update BenchmarkRunner + BoostedBenchmarkRunner
- Build CMARConfig from CLI args
- Pass to constructors

## Backward compat

Em giữ 1 deprecated static accessor để baseline code không break:
```java
@Deprecated
public static CMARConfig fromLegacyStatics() { ... }
```

Run once, then remove later.

## Risks

- Đụng core class → có thể subtle bugs
- → Mitigation: golden test sau mỗi step (Iris dataset, verify Acc/F1 identical)

## Todo

- [ ] Snapshot golden test (Iris dataset Acc/F1/Recall expected)
- [ ] Create CMARConfig + Builder
- [ ] Refactor CMARClassifier (constructor + static removal)
- [ ] Refactor RulePruner
- [ ] Refactor Rule.java sort logic (config-based)
- [ ] Update BenchmarkRunner
- [ ] Update BoostedBenchmarkRunner
- [ ] Update Bagging/Boosted/Bayesian wrappers
- [ ] Run golden test — verify identical
- [ ] Run FINAL config 26 datasets — verify Acc 85.47%
- [ ] Remove deprecated static accessor
