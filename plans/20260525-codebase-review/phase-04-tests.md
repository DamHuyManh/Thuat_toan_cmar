# Phase 4 — Unit Tests for Ensemble Classes

**Priority**: 🟡 MEDIUM
**Effort**: 4h
**Risk**: Low (chỉ thêm tests, không touch core)

## Context

Hiện chỉ có 1 unit test file: `MetricsVerify.java` (4 tests).

Không có tests cho:
- BaggingCMARClassifier
- BoostedCMARClassifier
- BayesianCMARClassifier
- ChiMergeDiscretizer (nếu giữ)
- Per-fold MDL discretization (verify no leak)

## Solution: Add lightweight tests

Em không setup JUnit (overkill cho project nhỏ). Dùng pattern existing `MetricsVerify.java` — main() method với assertions.

### Test files cần thêm

1. **`BaggingCMARTest.java`**
```java
public class BaggingCMARTest {
    public static void main(String[] args) {
        // Test 1: Bagging on Iris achieves >= 90% acc
        int[][] X = loadIrisX();
        int[] y = loadIrisY();
        BaggingCMARClassifier bag = new BaggingCMARClassifier(10, 2, 0.5, 3.841, 4, 80000, 4, 0, 1.0);
        bag.fit(X, y);
        double acc = bag.scoreFull(X, y).accuracy;
        assert acc >= 0.90 : "Bagging on Iris should >= 90% (got " + acc + ")";
        
        // Test 2: Same seed → identical results
        // Test 3: T=0 returns defaultClass
        // Test 4: Empty data doesn't crash
    }
}
```

2. **`PerFoldDiscretizationTest.java`** — quan trọng cho honesty
```java
public class PerFoldDiscretizationTest {
    public static void main(String[] args) {
        // Verify: cut points learned from train fold differ from cut points using full data
        // Verify: encoding test fold doesn't use test labels
        // Verify: same train fold → same cut points (deterministic)
    }
}
```

3. **`FinalConfigSmokeTest.java`** — full integration smoke
```java
public class FinalConfigSmokeTest {
    public static void main(String[] args) {
        // Run FINAL config on Iris + Diabetes + 1 multi-class
        // Verify: doesn't crash
        // Verify: reasonable metrics (acc > paper baseline)
        // Verify: same seed → identical output
    }
}
```

## Why no JUnit

KISS — pattern existing (`MetricsVerify`) đủ cho:
- 1 file = 1 test suite
- main() runs all assertions
- Fail fast với clear message
- No build system overhead

## Todo

- [ ] Add `BaggingCMARTest.java`
- [ ] Add `BoostedCMARTest.java`
- [ ] Add `PerFoldDiscretizationTest.java`
- [ ] Add `FinalConfigSmokeTest.java`
- [ ] Add test runner script: `bash test-all.sh`
- [ ] Document trong CLAUDE.md "How to run tests"
