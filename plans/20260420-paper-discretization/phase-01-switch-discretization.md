# Phase 01: Switch Discretization — Exact Per-Dataset Changes

**File:** `d:/Jun Tech/Cmar/Thuat_toan_cmar/src/cmar/benchmark/UCIDatasets.java`

Each entry gives the method name, the exact old line to match, and the replacement line.
All changes are single-line substitutions; no other lines in the method change.

---

## 1. loadAustralian (line ~808)

**Method:** `loadAustralian()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(australianCsv, 9, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(australianCsv);
```

---

## 2. loadDiabetes (line ~274)

**Method:** `loadDiabetes()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 9, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw diabetes/pima CSV string (no renaming done in this method).

---

## 3. loadGerman (line ~300)

**Method:** `loadGerman()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(germanCsv, 8, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(germanCsv);
```

---

## 4. loadHeart (line ~759)

**Method:** `loadHeart()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(heartCsv, 2, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(heartCsv);
```

---

## 5. loadIono (line ~399)

**Method:** `loadIono()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 5, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw ionosphere CSV string (no renaming done in this method).

---

## 6. loadIris (line ~526)

**Method:** `loadIris()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 3, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw iris CSV string (local file or URL fetch).

---

## 7. loadLed7 (line ~443)

**Method:** `loadLed7()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 2, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: Led7 attributes are binary (0/1); `parseMDL` treats them as categorical — correct.

---

## 8. loadLymphography (line ~725)

**Method:** `loadLymphography()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(lymphCsv, 6, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(lymphCsv);
```

---

## 9. loadPima (line ~781)

**Method:** `loadPima()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 9, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw pima CSV string (local file or URL fetch).

---

## 10. loadSonar (line ~505)

**Method:** `loadSonar()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 3, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw sonar CSV string (no renaming done in this method).

---

## 11. loadTicTacToe (line ~690)

**Method:** `loadTicTacToe()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 2, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: Tic-Tac-Toe attributes are categorical (x/o/b); `parseMDL` handles same as `parseCSV`
for non-numeric columns — no behavioral difference, but keeps preprocessing uniform.

---

## 12. loadVehicle (line ~868)

**Method:** `loadVehicle()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(vehicleCsv, 7, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(vehicleCsv);
```

---

## 13. loadWaveform (line ~887)

**Method:** `loadWaveform()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(csv, 4, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(csv);
```

Note: `csv` here is the raw waveform CSV string (no renaming done in this method).

---

## 14. loadWine (line ~561)

**Method:** `loadWine()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(wineCsv, 4, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(wineCsv);
```

---

## 15. loadZoo (line ~630)

**Method:** `loadZoo()`

```
OLD:  int[][][] parsed = DataLoader.parseCSV(zooCsv, 2, null);
NEW:  int[][][] parsed = DataLoader.parseMDL(zooCsv);
```

Note: Zoo has mostly binary attrs plus one numeric (legs count); `parseMDL` applies MDL
only to numeric columns and passes categorical through — correct behavior.

---

## Disambiguation: methods with `csv` as the variable name

Five methods use the bare name `csv` rather than a renamed local variable. Each has exactly
one `parseCSV` call, so there is no ambiguity within the method. The surrounding context
lines (shown below) uniquely identify each call for the Edit tool:

| Method         | Unique surrounding context to anchor the edit          |
|----------------|--------------------------------------------------------|
| `loadDiabetes` | Line above: `if (csv != null && csv.length() > 100) {` — first if-block, dataset name "Diabetes" in comment above |
| `loadIono`     | Method comment `// ===== IONO` above; only CSV call in method |
| `loadIris`     | Fallback to `createIrisSynthetic()` after the if-block |
| `loadPima`     | Method comment `// ===== PIMA DIABETES`; URL fetch from jbrownlee |
| `loadSonar`    | Method comment `// ===== SONAR`; no pre-processing of lines before the call |
| `loadTicTacToe`| Fallback to `createTicTacToeSynthetic()` after the if-block |
| `loadWaveform` | Method comment `// ===== WAVEFORM`; no pre-processing of lines before the call |

When issuing Edit calls for these, include one surrounding line (e.g. the `if (parsed != null)` line that follows) to ensure uniqueness.
