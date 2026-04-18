# UCI Machine Learning Datasets

This directory contains datasets downloaded from the [UCI Machine Learning Repository](https://archive.ics.uci.edu/ml/).

## Dataset Summary

| # | Dataset | File | Instances | Attributes | Classes | Task |
|---|---------|------|-----------|------------|---------|------|
| 1 | Iris | `iris.data` | 150 | 4 | 3 | Classification |
| 2 | Wine | `wine.data` | 178 | 13 | 3 | Classification |
| 3 | Breast Cancer Wisconsin | `breast-cancer-wisconsin.data` | 699 | 9 | 2 (benign/malignant) | Classification |
| 4 | Zoo | `zoo.data` | 101 | 16 | 7 | Classification |
| 5 | Glass Identification | `glass.data` | 214 | 9 | 7 | Classification |
| 6 | Tic-Tac-Toe Endgame | `tic-tac-toe.data` | 958 | 9 | 2 (win/loss) | Classification |
| 7 | Lymphography | `lymphography.data` | 148 | 18 | 4 | Classification |
| 8 | Heart Disease (Cleveland) | `heart-cleveland.data` | 303 | 13 | 5 (0-4) | Classification |
| 9 | Pima Indians Diabetes | `pima-indians-diabetes.data` | 768 | 8 | 2 (positive/negative) | Classification |
| 10 | Hepatitis | `hepatitis.data` | 155 | 19 | 2 (die/live) | Classification |
| 11 | Horse Colic | `horse-colic.data` | 300 | 27 | 2 (lived/died) | Classification |
| 12 | Vehicle Silhouettes | `vehicle.dat` | 846 | 18 | 4 (bus/van/saab/opel) | Classification |
| 13 | Australian Credit Approval | `australian.dat` | 690 | 14 | 2 (approved/denied) | Classification |
| 14 | Waveform | `waveform.data` | 5000 | 21 | 3 | Classification |

## Dataset Details

### 1. Iris
- **Source:** R.A. Fisher (1936)
- **Attributes:** sepal length, sepal width, petal length, petal width (all numeric)
- **Classes:** Iris-setosa, Iris-versicolour, Iris-virginica

### 2. Wine
- **Source:** Chemical analysis of wines from three different cultivars in Italy
- **Attributes:** 13 continuous attributes (alcohol, malic acid, ash, etc.)
- **Classes:** 3 wine cultivar types

### 3. Breast Cancer Wisconsin
- **Source:** Dr. William H. Wolberg, University of Wisconsin Hospitals
- **Attributes:** 9 integer-valued attributes (clump thickness, cell size, etc.)
- **Classes:** 2 (benign, malignant)
- **Note:** Contains 16 missing values denoted by "?"

### 4. Zoo
- **Source:** Richard Forsyth
- **Attributes:** 16 boolean-valued attributes + animal name
- **Classes:** 7 animal types

### 5. Glass Identification
- **Source:** USA Forensic Science Service
- **Attributes:** 9 continuous attributes (RI, Na, Mg, Al, Si, K, Ca, Ba, Fe)
- **Classes:** 7 glass types (though class 4 is not present in the data)

### 6. Tic-Tac-Toe Endgame
- **Source:** Aha, D.W.
- **Attributes:** 9 categorical attributes (x, o, b for each board position)
- **Classes:** 2 (positive = win for x, negative = loss for x)

### 7. Lymphography
- **Source:** University Medical Centre, Institute of Oncology, Ljubljana
- **Attributes:** 18 attributes (mix of nominal and numeric)
- **Classes:** 4 (normal find, metastases, malign lymph, fibrosis)

### 8. Heart Disease (Cleveland)
- **Source:** Cleveland Clinic Foundation (Robert Detrano, M.D., Ph.D.)
- **Attributes:** 13 attributes (age, sex, chest pain type, blood pressure, etc.)
- **Classes:** 5 levels (0 = no disease, 1-4 = increasing severity)
- **Note:** Contains some missing values denoted by "?"

### 9. Pima Indians Diabetes
- **Source:** National Institute of Diabetes and Digestive and Kidney Diseases
- **Attributes:** 8 numeric attributes (pregnancies, glucose, blood pressure, etc.)
- **Classes:** 2 (tested positive/negative for diabetes)
- **Note:** Downloaded from alternative mirror (original UCI link retired)

### 10. Hepatitis
- **Source:** G. Gong (Carnegie-Mellon University)
- **Attributes:** 19 attributes (mix of numeric and categorical)
- **Classes:** 2 (die, live)
- **Note:** Contains missing values denoted by "?"

### 11. Horse Colic
- **Source:** Veterinary records
- **Attributes:** 27 attributes (mix of nominal and continuous)
- **Classes:** 2 (lived, died; also has "euthanized")
- **Note:** Contains many missing values denoted by "?"

### 12. Vehicle Silhouettes
- **Source:** Turing Institute, Glasgow (originally from Siebert)
- **Attributes:** 18 numeric attributes extracted from vehicle silhouettes
- **Classes:** 4 (bus, van, saab, opel)
- **Note:** Combined from 9 separate segment files (xaa-xai)

### 13. Australian Credit Approval
- **Source:** Statlog project
- **Attributes:** 14 attributes (6 numeric, 8 categorical)
- **Classes:** 2 (approved, denied)
- **Note:** Attribute names and values changed to protect confidentiality

### 14. Waveform
- **Source:** Breiman et al. (1984) CART book
- **Attributes:** 21 continuous attributes
- **Classes:** 3 waveform types
- **Note:** Originally distributed as compressed .Z file, decompressed for use

## File Naming Convention

- `.data` / `.dat` - the actual dataset (CSV or space-delimited)
- `.names` / `.doc` - dataset description and attribute information

## Usage Notes

- Some datasets contain missing values (typically marked with `?`)
- The Pima Indians Diabetes dataset was obtained from an alternative source as the original UCI link has been retired
- The Vehicle dataset was reconstructed by concatenating the original 9 segment files
- The Waveform dataset was decompressed from the original `.Z` format
