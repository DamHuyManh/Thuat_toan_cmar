# Plan: Fix CMAR Discretization to Match Paper

## Problem

CBA (which CMAR copies preprocessing from) uses Fayyad & Irani 1993 supervised MDL
discretization. In the codebase this is `DataLoader.parseMDL()`. Fourteen of 26 datasets
incorrectly call `DataLoader.parseCSV(csv, N, null)` (equal-frequency binning), which
produces different discretization boundaries and therefore different accuracy numbers.

## Scope

**File:** `Thuat_toan_cmar/src/cmar/benchmark/UCIDatasets.java`

**14 methods to change** (one call each):

| Method            | Local CSV var  | Old call                        |
|-------------------|----------------|---------------------------------|
| `loadAustralian`  | `australianCsv`| `parseCSV(australianCsv, 9, null)` |
| `loadDiabetes`    | `csv`          | `parseCSV(csv, 9, null)`          |
| `loadGerman`      | `germanCsv`    | `parseCSV(germanCsv, 8, null)`    |
| `loadHeart`       | `heartCsv`     | `parseCSV(heartCsv, 2, null)`     |
| `loadIono`        | `csv`          | `parseCSV(csv, 5, null)`          |
| `loadIris`        | `csv`          | `parseCSV(csv, 3, null)`          |
| `loadLed7`        | `csv`          | `parseCSV(csv, 2, null)`          |
| `loadLymphography`| `lymphCsv`     | `parseCSV(lymphCsv, 6, null)`     |
| `loadPima`        | `csv`          | `parseCSV(csv, 9, null)`          |
| `loadSonar`       | `csv`          | `parseCSV(csv, 3, null)`          |
| `loadTicTacToe`   | `csv`          | `parseCSV(csv, 2, null)`          |
| `loadVehicle`     | `vehicleCsv`   | `parseCSV(vehicleCsv, 7, null)`   |
| `loadWaveform`    | `csv`          | `parseCSV(csv, 4, null)`          |
| `loadWine`        | `wineCsv`      | `parseCSV(wineCsv, 4, null)`      |
| `loadZoo`         | `zooCsv`       | `parseCSV(zooCsv, 2, null)`       |

**11 methods already correct** (parseMDL): Anneal, Auto, Breast-Cancer, Cleve, Crx,
Glass, Hepatitis, Horse, Hypo, Labor, Sick — no change needed.

## Approach

Single-phase mechanical substitution: replace each `parseCSV(var, N, null)` call with
`parseMDL(var)`. No structural changes to any method. `parseMDL` handles both numeric
(supervised MDL cuts) and categorical (pass-through) attributes identically to CBA.

## Phase

- **phase-01-switch-discretization.md** — exact old/new lines per dataset

## Risk

Low. `parseMDL` signature is `parseMDL(String csv) → int[][][]`; return type is identical
to `parseCSV`. The only behavioral change is discretization boundary selection, which is
the intended fix.
