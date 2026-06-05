# Phase 3 — DRY Ensemble Classes

**Priority**: 🔴 HIGH
**Effort**: 3h
**Risk**: Medium

## Context

3 ensemble classes lặp logic:
- `BoostedCMARClassifier` (205 lines)
- `BaggingCMARClassifier` (181 lines)
- `HyperRandomBaggingCMAR` (135 lines)

Logic chung:
1. **Bootstrap sample** (chung ~10 LOC) — DRY violation
2. **Train T base classifiers** (chung ~5 LOC)
3. **Weighted majority vote** (chung ~10 LOC) — 3 lần copy-paste:

```java
Map<Integer, Double> votes = new HashMap<>();
for (int t = 0; t < classifiers.size(); t++) {
    int pred = classifiers.get(t).predict(x);
    votes.merge(pred, weights.get(t), Double::sum);
}
int best = defaultClass;
double bestScore = Double.NEGATIVE_INFINITY;
for (Map.Entry<Integer, Double> e : votes.entrySet()) {
    if (e.getValue() > bestScore) { bestScore = e.getValue(); best = e.getKey(); }
}
```

## Solution: `EnsembleCMAR` base class

```java
public abstract class EnsembleCMAR {
    protected final int T;
    protected final List<CMARClassifier> classifiers = new ArrayList<>();
    protected final List<Double> weights = new ArrayList<>();
    protected int defaultClass;
    protected long seed = 42;
    
    /** Train all T base classifiers — subclasses override how to sample/configure */
    public abstract void fit(int[][] X, int[] y);
    
    /** Common weighted majority vote */
    public int predict(int[] x) {
        if (classifiers.isEmpty()) return defaultClass;
        Map<Integer, Double> votes = new HashMap<>();
        for (int t = 0; t < classifiers.size(); t++) {
            votes.merge(classifiers.get(t).predict(x), weights.get(t), Double::sum);
        }
        return argmaxVotes(votes);
    }
    
    /** Common bootstrap sample helper */
    protected static int[][] bootstrap(int[][] X, int[] y, int[] yOut, Random rng, double ratio) {
        int N = X.length;
        int size = Math.max(2, (int) Math.round(N * ratio));
        int[][] xt = new int[size][];
        for (int i = 0; i < size; i++) {
            int idx = rng.nextInt(N);
            xt[i] = X[idx];
            yOut[i] = y[idx];
        }
        return xt;
    }
    
    /** Common predict for all instances */
    public int[] predict(int[][] X) { ... }
    public Metrics scoreFull(int[][] X, int[] y) { ... }
    
    protected static int argmaxVotes(Map<Integer, Double> votes) { ... }
}
```

## Subclasses become thin

### `BaggingCMAR extends EnsembleCMAR`:
```java
public void fit(int[][] X, int[] y) {
    setupDefaults(X, y);
    for (int t = 0; t < T; t++) {
        Random rng = new Random(seed + 100L * t);
        int[] yt = new int[X.length];
        int[][] xt = bootstrap(X, y, yt, rng, bootstrapRatio);
        CMARClassifier h = new CMARClassifier(config);
        h.fit(xt, yt);
        classifiers.add(h);
        weights.add(computeOOBWeight(h, X, y));
    }
}
```

### `BoostedCMAR extends EnsembleCMAR`:
```java
public void fit(int[][] X, int[] y) {
    setupDefaults(X, y);
    double[] w = uniformWeights(X.length);
    for (int t = 0; t < T; t++) {
        int[] yt = new int[X.length];
        int[][] xt = weightedBootstrap(X, y, yt, w, rng);
        CMARClassifier h = new CMARClassifier(config);
        h.fit(xt, yt);
        double err = computeError(h, X, y, w);
        if (err >= maxErr) break;
        double alpha = computeAlpha(err);
        updateWeights(w, h, X, y, alpha);
        classifiers.add(h);
        weights.add(alpha);
    }
}
```

→ Mỗi subclass còn ~50 LOC thay vì 150-200.

## LOC reduction

| Before | After | Saved |
|---|---|---|
| BoostedCMAR 205 + BaggingCMAR 181 + HyperRandom 135 = 521 | EnsembleCMAR 120 + 3 thin subclasses (50 each) = 270 | **-251 LOC** |

## Todo

- [ ] Create `EnsembleCMAR` base class
- [ ] Refactor BaggingCMAR to extend
- [ ] Refactor BoostedCMAR to extend
- [ ] Refactor HyperRandomBaggingCMAR to extend
- [ ] Run all ensemble configs — verify identical results
- [ ] Update BoostedBenchmarkRunner if needed
