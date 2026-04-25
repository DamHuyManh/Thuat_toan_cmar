# Phase 01: Remove Non-Standard Code

All edits are in two files only.

---

## 1. `UCIDatasets.java`

### 1.1 Remove `optimalAntLen`, `optimalCoverage`, `optimalChi`, `optimalSeed`, `rawData` fields from `Dataset`

**Lines 86–90** — delete these five field declarations:

```java
// DELETE these 5 lines:
public int optimalAntLen = 0;  // 0 = dùng default
public int optimalCoverage = 0; // 0 = dùng default
public double optimalChi = 0;  // 0 = dùng default 3.841
public long optimalSeed = -1;  // -1 = dùng default 42
public DataLoader.RawData rawData = null; // for per-fold MDL discretization
```

### 1.2 Remove `withOptimal()` method overloads

**Lines 111–123** — delete both `withOptimal` methods entirely:

```java
// DELETE these two methods:
public Dataset withOptimal(int antLen, int coverage) { ... }
public Dataset withOptimal(int antLen, int coverage, double chi) { ... }
```

### 1.3 Remove `replacePimaZeros()` method

**Lines 1130–1148** — delete the entire private static `replacePimaZeros()` method.

### 1.4 Remove `ds.rawData = ...` and `ds.withOptimal(...)` call sites (per-method)

For each method below, remove the listed lines. No other changes to the method.

#### `loadAnneal()` — line 191
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(annealCsv);
```

#### `loadAuto()` — line 227
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(autoCsv);
```

#### `loadCleve()` — line 262
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(cleveCsv);
```

#### `loadCrx()` — line 281
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(crxCsv);
```

#### `loadDiabetes()` — lines 296, 303–304
```java
// DELETE line 296:
csv = replacePimaZeros(csv);
// DELETE line 303:
ds.withOptimal(4, 3);
// DELETE line 304:
ds.rawData = DataLoader.parseRaw(csv, 9, null);
```
Also change the `parseCSV` call (line 297) to use plain `csv` (already does — the variable was mutated in-place, removal of `replacePimaZeros` call means `csv` stays as loaded).

#### `loadGerman()` — lines 331–332
```java
// DELETE:
ds.withOptimal(4, 5);
ds.rawData = DataLoader.parseRaw(germanCsv, 8, null);
```

#### `loadHorse()` — line 379
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(horseCsv, 5, null);
```

#### `loadHypo()` — line 418
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(hypoCsv);
```

#### `loadIono()` — lines 434–435
```java
// DELETE:
ds.withOptimal(4, 4);
ds.rawData = DataLoader.parseRaw(csv, 5, null);
```

#### `loadLabor()` — line 463
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(laborCsv);
```

#### `loadLed7()` — lines 481–482
```java
// DELETE:
ds.withOptimal(6, 5);
ds.rawData = DataLoader.parseRaw(csv, 2, null);
```

#### `loadSick()` — lines 527–528
```java
// DELETE:
ds.withOptimal(4, 3);
ds.rawData = DataLoader.parseRaw(sickCsv);
```

#### `loadSonar()` — lines 547–548
```java
// DELETE:
ds.withOptimal(3, 3);
ds.rawData = DataLoader.parseRaw(csv, 3, null);
```

#### `loadIris()` — lines 570–571
```java
// DELETE:
ds.withOptimal(3, 3);
ds.rawData = DataLoader.parseRaw(csv, 3, null);
```

#### `loadWine()` — lines 607–608
```java
// DELETE:
ds.withOptimal(2, 2);
ds.rawData = DataLoader.parseRaw(wineCsv, 4, null);
```

#### `loadBreastCancer()` — line 643
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(bcCsv);
```

#### `loadZoo()` — lines 679–681
```java
// DELETE:
ds.withOptimal(4, 3);
ds.optimalSeed = 24;
ds.rawData = DataLoader.parseRaw(zooCsv, 2, null);
```

#### `loadGlass()` — line 720
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(glassCsv);
```

#### `loadTicTacToe()` — line 747
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(csv, 2, null);
```

#### `loadLymphography()` — line 783
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(lymphCsv, 6, null);
```

#### `loadHeart()` — lines 814–815
```java
// DELETE:
ds.withOptimal(4, 3);
ds.rawData = DataLoader.parseRaw(heartCsv, 2, null);
```

#### `loadPima()` — lines 832, 839–840
```java
// DELETE line 832:
csv = replacePimaZeros(csv);
// DELETE line 839:
ds.withOptimal(4, 3);
// DELETE line 840:
ds.rawData = DataLoader.parseRaw(csv, 9, null);
```

#### `loadAustralian()` — line 868
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(australianCsv, 9, null);
```

#### `loadHepatitis()` — line 901
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(hepatitisCsv, 5, null);
```

#### `loadVehicle()` — lines 930–931
```java
// DELETE:
ds.withOptimal(4, 4);
ds.rawData = DataLoader.parseRaw(vehicleCsv, 7, null);
```

#### `loadWaveform()` — line 951
```java
// DELETE:
ds.rawData = DataLoader.parseRaw(csv, 4, null);
```

---

## 2. `BenchmarkRunner.java`

### 2.1 Fix `ParamConfig.base()` — use hardcoded defaults, remove `optimalXxx` reads

**Lines 448–450** — replace the three conditional reads with hardcoded literals:

```java
// BEFORE (lines 448–450):
double chi = ds.optimalChi > 0 ? ds.optimalChi : 3.841;
int coverage = ds.optimalCoverage > 0 ? ds.optimalCoverage : 4;
int antLen = ds.optimalAntLen > 0 ? ds.optimalAntLen : 4;

// AFTER:
double chi = 3.841;
int coverage = 4;
int antLen = 4;
```

### 2.2 Fix seed in `runBenchmark()` — remove `optimalSeed` read

**Line 53** — `optimalSeed` field no longer exists on `Dataset`. Replace with hardcoded seed:

```java
// BEFORE:
long seed = ds.optimalSeed >= 0 ? ds.optimalSeed : 42;

// AFTER:
long seed = 42;
```

### 2.3 Remove dead `rawData` branch in `evaluateConfig()`

**Lines 127–149** — the `if (ds.rawData != null)` block is now dead code (field removed). Collapse to just the `else` body:

```java
// BEFORE (lines 127–149):
if (ds.rawData != null) {
    int[] trainIdxArr = trainIdx.stream().mapToInt(Integer::intValue).toArray();
    int[] testIdxArr  = testIdx.stream().mapToInt(Integer::intValue).toArray();
    DataLoader.FoldData fd = DataLoader.encodeFold(ds.rawData, trainIdxArr, testIdxArr);
    trainData   = fd.trainTx;
    trainLabels = fd.trainLabels;
    testData    = fd.testTx;
    testLabels  = fd.testLabels;
} else {
    trainData   = new int[trainSize][];
    trainLabels = new int[trainSize];
    testData    = new int[testSize][];
    testLabels  = new int[testSize];
    for (int i = 0; i < trainSize; i++) {
        trainData[i]   = ds.transactions[trainIdx.get(i)];
        trainLabels[i] = ds.labels[trainIdx.get(i)];
    }
    for (int i = 0; i < testSize; i++) {
        testData[i]   = ds.transactions[testIdx.get(i)];
        testLabels[i] = ds.labels[testIdx.get(i)];
    }
}

// AFTER (replace entire block with just the else-body, no if/else wrapper):
trainData   = new int[trainSize][];
trainLabels = new int[trainSize];
testData    = new int[testSize][];
testLabels  = new int[testSize];
for (int i = 0; i < trainSize; i++) {
    trainData[i]   = ds.transactions[trainIdx.get(i)];
    trainLabels[i] = ds.labels[trainIdx.get(i)];
}
for (int i = 0; i < testSize; i++) {
    testData[i]   = ds.transactions[testIdx.get(i)];
    testLabels[i] = ds.labels[testIdx.get(i)];
}
```

---

## Post-edit Compile Check

After all edits, confirm:
- `UCIDatasets.java` has no references to `rawData`, `withOptimal`, `replacePimaZeros`, `optimalAntLen`, `optimalCoverage`, `optimalChi`, `optimalSeed`
- `BenchmarkRunner.java` has no references to `ds.optimalChi`, `ds.optimalCoverage`, `ds.optimalAntLen`, `ds.optimalSeed`, `ds.rawData`
- Project compiles cleanly (`javac` or IDE build with no errors)
