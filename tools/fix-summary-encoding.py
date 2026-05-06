# -*- coding: utf-8 -*-
"""Rewrite results/summary-report.md as UTF-8 (no BOM)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
out = ROOT / "results" / "summary-report.md"

# Tables preserved from last benchmark (ASCII-safe)
TABLES = r'''
---

## Accuracy Comparison

| Dataset | Instances | Attrs | Classes | **Our CMAR** | Paper CMAR | Paper CBA | Paper C4.5 | Diff |
|---------|-----------|-------|---------|-------------|------------|-----------|------------|------|
| Anneal | 898 | 38 | 6 | **97.7%** | 97.3% | 97.9% | 94.8% | +0.4% |
| Australian | 690 | 14 | 2 | **86.4%** | 86.1% | 84.9% | 84.7% | +0.3% |
| Auto | 205 | 25 | 6 | **81.6%** | 78.1% | 78.3% | 80.1% | +3.5% |
| Breast-Cancer | 683 | 9 | 2 | **96.9%** | 96.4% | 96.3% | 95.0% | +0.5% |
| Cleve | 303 | 13 | 2 | **81.6%** | 82.2% | 82.8% | 78.2% | -0.6% |
| Crx | 690 | 15 | 2 | **86.0%** | 84.9% | 84.7% | 84.9% | +1.1% |
| Diabetes | 768 | 8 | 2 | **73.3%** | 75.8% | 74.5% | 74.2% | -2.5% |
| German | 1000 | 20 | 2 | **72.2%** | 74.9% | 73.4% | 72.3% | -2.7% |
| Glass | 214 | 9 | 6 | **70.0%** | 70.1% | 73.9% | 68.7% | -0.1% |
| Heart | 270 | 13 | 2 | **79.3%** | 82.2% | 81.9% | 80.8% | -2.9% |
| Hepatitis | 155 | 19 | 2 | **80.8%** | 80.5% | 81.8% | 80.6% | +0.3% |
| Horse | 368 | 22 | 2 | **81.5%** | 82.6% | 82.1% | 82.6% | -1.1% |
| Hypo | 3163 | 25 | 2 | **97.9%** | 98.4% | 98.9% | 99.2% | -0.5% |
| Iono | 351 | 34 | 2 | **91.7%** | 91.5% | 92.3% | 90.0% | +0.2% |
| Iris | 150 | 4 | 3 | **92.7%** | 94.0% | 94.7% | 95.3% | -1.3% |
| Labor | 57 | 16 | 2 | **86.3%** | 89.7% | 86.3% | 79.3% | -3.4% |
| Led7 | 3200 | 7 | 10 | **72.2%** | 72.5% | 71.9% | 73.5% | -0.3% |
| Lymphography | 148 | 18 | 4 | **84.7%** | 83.1% | 77.8% | 73.5% | +1.6% |
| Pima | 768 | 8 | 2 | **73.3%** | 75.1% | 72.9% | 75.5% | -1.8% |
| Sick | 2800 | 29 | 2 | **96.8%** | 97.5% | 97.0% | 98.5% | -0.7% |
| Sonar | 208 | 60 | 2 | **80.7%** | 79.4% | 77.5% | 70.2% | +1.3% |
| Tic-Tac-Toe | 958 | 9 | 2 | **99.4%** | 99.2% | 99.6% | 99.4% | +0.2% |
| Vehicle | 846 | 18 | 4 | **68.1%** | 68.8% | 68.7% | 72.6% | -0.7% |
| Waveform | 5000 | 21 | 3 | **81.5%** | 83.2% | 80.0% | 78.1% | -1.7% |
| Wine | 178 | 13 | 3 | **96.2%** | 95.0% | 95.0% | 92.7% | +1.2% |
| Zoo | 101 | 16 | 7 | **96.5%** | 97.1% | 96.8% | 92.2% | -0.6% |
| **Average** | | | | **84.8%** | 85.2% | 84.7% | 83.3% | -0.4% |

## Performance Metrics

| Dataset | Train (ms) | Predict (ms) | Rules mined | Rules after prune | % Removed |
|---------|------------|--------------|-------------|-------------------|----------|
| Anneal | 584 ms | 0 ms | 156588 | 159 | 99.9% |
| Australian | 32 ms | 0 ms | 18745 | 456 | 97.6% |
| Auto | 464 ms | 0 ms | 209009 | 208 | 99.9% |
| Breast-Cancer | 3 ms | 0 ms | 2836 | 265 | 90.7% |
| Cleve | 11 ms | 0 ms | 16274 | 276 | 98.3% |
| Crx | 35 ms | 0 ms | 30762 | 557 | 98.2% |
| Diabetes | 1 ms | 0 ms | 1585 | 213 | 86.6% |
| German | 132 ms | 0 ms | 89483 | 951 | 98.9% |
| Glass | 2 ms | 0 ms | 2021 | 121 | 94.0% |
| Heart | 9 ms | 0 ms | 15134 | 249 | 98.4% |
| Hepatitis | 45 ms | 0 ms | 38172 | 122 | 99.7% |
| Horse | 149 ms | 0 ms | 129386 | 397 | 99.7% |
| Hypo | 219 ms | 1 ms | 86450 | 176 | 99.8% |
| Iono | 385 ms | 0 ms | 129736 | 196 | 99.8% |
| Iris | 0 ms | 0 ms | 90 | 30 | 66.7% |
| Labor | 21 ms | 0 ms | 24003 | 49 | 99.8% |
| Led7 | 2 ms | 0 ms | 243 | 112 | 53.9% |
| Lymphography | 96 ms | 0 ms | 65800 | 149 | 99.8% |
| Pima | 2 ms | 0 ms | 1585 | 213 | 86.6% |
| Sick | 257 ms | 1 ms | 85874 | 279 | 99.7% |
| Sonar | 1291 ms | 0 ms | 160000 | 172 | 99.9% |
| Tic-Tac-Toe | 6 ms | 0 ms | 7047 | 182 | 97.4% |
| Vehicle | 67 ms | 0 ms | 36922 | 477 | 98.7% |
| Waveform | 367 ms | 6 ms | 75473 | 2650 | 96.5% |
| Wine | 26 ms | 0 ms | 16933 | 54 | 99.7% |
| Zoo | 18 ms | 0 ms | 13758 | 35 | 99.7% |

'''

PARAM_ROWS = r'''| Dataset | Min Support (ratio) | Min Support (abs) | Min Confidence |
|---------|--------------------|--------------------|----------------|
| Anneal | 0.01 | 8 | 0.50 |
| Australian | 0.01 | 6 | 0.50 |
| Auto | 0.01 | 2 | 0.50 |
| Breast-Cancer | 0.01 | 6 | 0.50 |
| Cleve | 0.01 | 2 | 0.50 |
| Crx | 0.01 | 6 | 0.50 |
| Diabetes | 0.01 | 5 | 0.50 |
| German | 0.01 | 9 | 0.50 |
| Glass | 0.01 | 2 | 0.50 |
| Heart | 0.01 | 2 | 0.50 |
| Hepatitis | 0.01 | 2 | 0.50 |
| Horse | 0.01 | 3 | 0.50 |
| Hypo | 0.01 | 28 | 0.50 |
| Iono | 0.03 | 10 | 0.50 |
| Iris | 0.01 | 2 | 0.50 |
| Labor | 0.01 | 2 | 0.50 |
| Led7 | 0.01 | 28 | 0.50 |
| Lymphography | 0.01 | 2 | 0.50 |
| Pima | 0.01 | 5 | 0.50 |
| Sick | 0.01 | 12 | 0.50 |
| Sonar | 0.08 | 14 | 0.50 |
| Tic-Tac-Toe | 0.01 | 8 | 0.50 |
| Vehicle | 0.02 | 11 | 0.50 |
| Waveform | 0.01 | 45 | 0.50 |
| Wine | 0.01 | 2 | 0.50 |
| Zoo | 0.03 | 3 | 0.50 |

'''

# Vietnamese prose as Python unicode literals (file saved UTF-8)
HEADER = """# CMAR \u2014 B\u00e1o c\u00e1o benchmark (t\u00f3m t\u1eaft)

| M\u1ee5c | N\u1ed9i dung |
|---|---|
| **Ng\u00e0y ch\u1ea1y** | 2026-05-06 |
| **B\u00e0i b\u00e1o tham chi\u1ebfu** | Li, Han, Pei \u2014 *CMAR* (IEEE ICDM 2001) |
| **Code** | Java \u2014 bitmap matching, CR-tree c\u00f3 hash, chi-square + coverage pruning |
| **\u0110\u00e1nh gi\u00e1** | 10-fold cross-validation |

## C\u00e1ch \u0111\u1ecdc b\u00e1o c\u00e1o

### B\u1ea3ng \u0111\u1ed9 ch\u00ednh x\u00e1c (Accuracy Comparison)

- **Our CMAR:** \u0111\u1ed9 ch\u00ednh x\u00e1c (%) do ch\u01b0\u01a1ng tr\u00ecnh c\u1ee7a b\u1ea1n \u0111o \u0111\u01b0\u1ee3c.
- **Paper CMAR / Paper CBA / Paper C4.5:** s\u1ed1 **ghi trong b\u00e0i b\u00e1o** \u0111\u1ec3 so s\u00e1nh \u2014 *kh\u00f4ng* ph\u1ea3i ch\u1ea1y l\u1ea1i CBA/C4.5 tr\u00ean m\u00e1y b\u1ea1n.
- **Diff:** ch\u00eanh l\u1ec7ch **Our CMAR \u2212 Paper CMAR** (%). D\u01b0\u01a1ng (+) = b\u1ea1n cao h\u01a1n paper; \u00e2m (\u2212) = th\u1ea5p h\u01a1n.
- **Instances / Attrs / Classes:** s\u1ed1 m\u1eabu, s\u1ed1 thu\u1ed9c t\u00ednh, s\u1ed1 l\u1edbp c\u1ee7a b\u1ed9 d\u1eef li\u1ec7u.

### B\u1ea3ng hi\u1ec7u n\u0103ng (Performance Metrics)

- **Train / Predict:** th\u1eddi gian hu\u1ea5n luy\u1ec7n (mine + prune) v\u00e0 d\u1ef1 \u0111o\u00e1n, **trung b\u00ecnh theo fold** (ms). Gi\u00e1 tr\u1ecb **0 ms** th\u01b0\u1eddng l\u00e0 l\u00e0m tr\u00f2n (< 1 ms).
- **Rules mined:** s\u1ed1 lu\u1eadt sinh ra **tr\u01b0\u1edbc** b\u01b0\u1edbc c\u1eaft t\u1ec9a.
- **Rules after prune:** s\u1ed1 lu\u1eadt **c\u00f2n l\u1ea1i sau** prune (d\u00f9ng \u0111\u1ec3 ph\u00e2n l\u1edbp). *(T\u00ean c\u0169 \"Rules Pruned\" d\u1ec5 g\u00e2y nh\u1ea7m \u2014 \u0111\u00e2y l\u00e0 lu\u1eadt **gi\u1eef l\u1ea1i**, kh\u00f4ng ph\u1ea3i s\u1ed1 lu\u1eadt b\u1ecb x\u00f3a.)*
- **% Removed:** ph\u1ea7n tr\u0103m lu\u1eadt th\u00f4 b\u1ecb lo\u1ea1i: `100 * (1 - after/mined)` (trong b\u1ea3ng, *after* l\u00e0 c\u1ed9t *Rules after prune*).

"""

PARAM_NOTE = "*Tham s\u1ed1 FP-Growth / CMAR cho t\u1eebng b\u1ed9 (min support d\u1ea1ng t\u1ef7 l\u1ec7 v\u00e0 s\u1ed1 giao d\u1ecbch t\u1ed1i thi\u1ec3u).*\n\n"

FOOTER = """## Key Observations

*So s\u00e1nh **Our CMAR** v\u1edbi **Paper CMAR**, ng\u01b0\u1ee1ng ch\u00eanh l\u1ec7ch 0,5 \u0111i\u1ec3m ph\u1ea7n tr\u0103m.*

- **Th\u1eafng / Wins** (Our > Paper h\u01a1n 0,5%): 6/26
- **H\u00f2a / Ties** (ch\u00eanh l\u1ec7ch trong \u00b10,5%): 8/26
- **Thua / Losses** (Our th\u1ea5p h\u01a1n Paper h\u01a1n 0,5%): 12/26
- **Ch\u00eanh TB vs Paper CMAR / Average diff:** -0.4%

## Optimizations Applied

1. **Bitmap rule matching** \u2014 ki\u1ec3m tra ti\u1ec1n \u0111\u1ec1 b\u1eb1ng AND bit, t\u1ed1i \u01b0u kh\u1edbp lu\u1eadt.
2. **Hash-indexed CR-tree** \u2014 l\u01b0u lu\u1eadt theo l\u1edbp, c\u1eaft nh\u00e1nh nh\u1edd m\u1ee5c \u0111\u1ea7u ti\u00ean.
3. **Chi-square pruning (CSP)** \u2014 b\u1ecf lu\u1eadt kh\u00f4ng c\u00f3 \u00fd ngh\u0129a th\u1ed1ng k\u00ea (p < 0,05).
4. **Database coverage pruning (DCP)** \u2014 b\u1ecf lu\u1eadt d\u01b0 th\u1eeba theo \u0111\u1ed9 ph\u1ee7.
5. **Single-path FP-tree** \u2014 t\u1ed1i \u01b0u khi ch\u1ec9 c\u00f2n m\u1ed9t nh\u00e1nh.
6. **Weighted voting** \u2014 tr\u1ecdng s\u1ed1 \u2248 chi-square \u00d7 confidence; top-5 m\u1ed7i l\u1edbp khi b\u1ecf phi\u1ebfu.
7. **Per-class adaptive minSupport** \u2014 l\u1edbp hi\u1ebfm (\u226410 m\u1eabu trong fold) d\u00f9ng support t\u1ed1i thi\u1ec3u 1.
8. **Max antecedent length** \u2014 gi\u1edbi h\u1ea1n \u0111\u1ed9 d\u00e0i ti\u1ec1n \u0111\u1ec1 t\u1ed1i \u0111a 4 m\u1ee5c.
"""

def main():
    text = (
        HEADER
        + TABLES
        + "## Parameters Used\n\n"
        + PARAM_NOTE
        + PARAM_ROWS
        + FOOTER
    )
    out.write_text(text, encoding="utf-8", newline="\n")
    print("Wrote", out, "chars", len(text))


if __name__ == "__main__":
    main()
