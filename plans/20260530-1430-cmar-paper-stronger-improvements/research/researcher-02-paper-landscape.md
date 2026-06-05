# AC / CMAR Paper Landscape (for international student science competition)

Scope: associative classification (AC) lineage around CMAR (Li, Han, Pei, IEEE ICDM 2001).
Honesty note: numbers below are taken from original papers where the PDF table was directly
extractable (CBA/CMAR/CPAR), or reported as the authors stated them. Where a value could not
be verified from a primary table, it is marked "not verified" rather than invented.

---

## 1. Related Work table (drop-in for the paper)

| Method | Year | Venue | Key idea | Reported avg acc (UCI) |
|--------|------|-------|----------|------------------------|
| **CBA** | 1998 | KDD (AAAI) | First AC: mine Class Association Rules (CARs) via Apriori, single best-rule classification, database-coverage pruning | ~84.7% (26 sets, 10-fold); beats C4.5 |
| **CMAR** | 2001 | IEEE ICDM | FP-growth CR-tree; classify by **multiple rules** per class via **weighted chi-square**; chi-square + coverage pruning | **85.22%** (26 sets, 10-fold) vs CBA 84.69, C4.5 83.34 |
| **CPAR** | 2003 | SIAM SDM | FOIL-style greedy rule generation (not exhaustive); **Laplace expected accuracy**; best-k rules per class | +0.48% vs CBA, +1.83% vs C4.5, +2.24% vs RIPPER (authors' reported gains) |
| **MCAR** | 2005 | ACS/IEEE AICCSA | Vertical (tid-list) intersection mining; multi-class rule ranking adding confidence/support tie-breaks | competitive w/ C4.5/RIPPER on 15 sets (no single avg published) |
| **ACAC** | 2007/11 | IEEE GrC | Adds **all-confidence** to Apriori to keep only strongly correlated itemsets before CARs | reported > CBA (not verified numerically) |
| **ACCF** | 2008 | IEEE FSKD | Mine **closed frequent itemsets** (CHARM) -> far fewer rules, same coverage | "better avg acc than CBA" (authors, not verified) |
| **CACA** | ~2007 | (DASFAA-era) | Collapses 3 AC phases to 2 (rule formation + model build merged); class-based structure for speed | speed-focused; acc ~ CMAR (not verified) |
| **WCBA** | 2018 | Applied Soft Computing | **Weighted** CARs (feature/factor weights) for breast-cancer AC | up to 97.4% on Wisconsin BC (domain-specific, not a 26-set avg) |
| **ECBA / statistical-rank CBA** | 2016-18 | IBIMA / ASOC (Alwidian et al.) | Replace confidence ranking with a **statistical ranking measure**; Apriori optimization | reported > CBA/MCAR/FACA/FCBA (not verified numerically) |
| **ACPRISM** | 2017 | Information Sciences | PRISM + Information-Gain attribute selection to prune CARs | "outperforms CBA, MCAR, PRISM, RIPPER" (16 sets) |
| **ACO self-training AC** | 2021 | PeerJ CS | Semi-supervised AC; Ant Colony builds rules from labeled+pseudo-labeled data | beats classical self-training (domain-specific) |
| **ACMKC** | 2023 | Mathematics (MDPI) | Compact AC via **K-modes clustering** + coverage rule representation | compact model, competitive acc (not verified) |

CBA average of ~84.7% is the widely cited figure; the CMAR paper's own Table 3 reports CBA
84.69 / C4.5 83.34 / CMAR 85.22 on the **same 26 shuffled UCI sets, 10-fold CV** — use these
three as the anchor triplet because they are apples-to-apples in one paper.

---

## 2. Key improvement idea + reported gain (per method)

- **CBA (Liu, Hsu, Ma 1998, KDD):** introduced CARs + the "single rule" classifier with
  database coverage pruning. Gain: modest but consistent edge over C4.5 because it exploits
  multi-attribute associations vs one-attribute-at-a-time entropy splits.
- **CMAR (Li, Han, Pei 2001, ICDM):** two ideas — (a) **weighted chi-square** combination of
  *multiple* rules per class instead of picking one, (b) **CR-tree / FP-growth** for efficiency.
  Reported gain: +0.53 over CBA, +1.88 over C4.5 in average accuracy (85.22 vs 84.69 vs 83.34),
  and "wins in more datasets" while being faster/more scalable.
- **CPAR (Yin, Han 2003, SDM):** avoid the exponential candidate-rule blow-up via **greedy FOIL
  rule generation** + **Laplace** accuracy estimate; predicts with best-k rules. Reported gain:
  +0.48% over CBA, +1.83% over C4.5, +2.24% over RIPPER, with far fewer rules and much faster.
- **MCAR (Thabtah et al. 2005):** **vertical tid-list** mining (intersection-based support) and a
  richer rule-ranking tie-break. Gain: efficiency + accuracy competitive with C4.5/RIPPER.
- **ACAC / ACCF / CACA:** correlation-aware and closed-itemset rule reduction. The shared theme is
  "fewer but higher-quality rules at equal/better accuracy" — efficiency and rule-set size are the
  headline, accuracy gains over CMAR are small/not robustly established.
- **WCBA / weighted AC (Alwidian 2018):** attach **weights** to attributes/rules (domain importance)
  so discriminative features dominate. Strong on medical/imbalanced data; reported domain accuracy
  high (97.4% BC) but not a general-purpose 26-set benchmark.
- **2019-2025 directions:** semi-supervised AC (ACO), compact/clustered AC (ACMKC, K-modes),
  cost-sensitive & ensemble AC for imbalance, and "AC + deep features." These are scattered across
  journals; none has displaced CMAR/CPAR as the standard accuracy baseline.

---

## 3. What AC papers use for EVALUATION (standard protocol)

- **Datasets:** UCI repository; classic count is **26 datasets** (CBA/CMAR set). Modern papers use
  10-30 UCI sets; medical/imbalance papers add Wisconsin BC, Pima, etc.
- **CV protocol:** **10-fold cross-validation** is the de-facto standard (CBA, CMAR, CPAR all use it).
  CMAR/CBA shuffle data with C4.5's shuffle utility and discretize numeric attributes the same way.
- **Primary metric:** classification **accuracy** (or error rate). This is the historical norm.
- **Statistical tests:** older AC papers (CBA/CMAR/CPAR) used **no formal significance test** — just
  per-dataset tables + average + win counts. Modern/competitive AC papers add the
  **Friedman test + Nemenyi/Holm post-hoc** with a **critical-difference (CD) diagram** — this is now
  expected at top venues and is an easy differentiator for a student paper.
- **Baselines:** the canonical comparison set is **C4.5, RIPPER, CBA, CMAR, CPAR** (+ MCAR). Add
  SVM / RandomForest / a gradient-boosting baseline for a modern, defensible comparison.
- **Secondary reported quantities:** number of rules, training time/scalability, memory.

---

## 4. Where AC improvements get published

- **Journals (most common):** Knowledge-Based Systems; Expert Systems with Applications (also hosts
  the 2025 comprehensive AC review, S0957417425010760); Applied Intelligence; Information Sciences
  (ACPRISM); Knowledge and Information Systems (KAIS); Applied Soft Computing (WCBA);
  Journal of Information & Knowledge Management; PeerJ CS; Mathematics (MDPI, open access).
- **Conferences:** IEEE ICDM (CMAR home), SIAM SDM (CPAR), PAKDD, DEXA, FSKD (ACCF), IEEE GrC (ACAC),
  ACS/IEEE AICCSA (MCAR).
- For a **student competition**, target an open-access mid-tier journal style (Mathematics/MDPI,
  PeerJ CS, ESWA-style) — they value clear benchmarking + reproducibility over heavy theory.

---

## 5. What makes an AC paper COMPETITIVE for an international student competition

**Novelty bar (pick at least one real lever, don't just re-tune thresholds):**
1. A **new rule-ranking / vote-combination** rule (your repo already does composite vote = conf x lift
   + Top-K voting + stratified coverage — frame this as the contribution).
2. **Imbalance handling inside AC**: class-aware support, cost-sensitive rule weighting, or
   minority-class rule boosting. This is the strongest angle today and underserved in classic AC.
3. **Interpretability + accuracy** together — AC's natural edge over deep models.

**Required experiments to be taken seriously:**
- >= 15-20 UCI datasets (include several **imbalanced** ones: e.g. sick, hypothyroid, hepatitis,
  Pima, German credit, breast-cancer), **10-fold CV**, fixed seeds, reported per-dataset table.
- Baselines: **C4.5, RIPPER, CBA, CMAR, CPAR** + 1-2 modern (RandomForest / XGBoost).
- **Statistical rigor:** Friedman + Nemenyi/Holm + **CD diagram**. This alone separates you from
  90% of AC student work.
- Ablation showing each component's contribution (e.g. vote rule on/off, Top-K, stratified coverage).
- Report **rule count + runtime** so "we're not just slower for +0.3%".

**Framing "we improve F1/Recall on imbalanced data" (recommended thesis):**
- Argue accuracy is misleading under imbalance; switch the headline metric to **macro-F1 and Recall
  of the minority class** (also G-mean / balanced accuracy). This is honest and modern.
- Story: "Classic AC (CBA/CMAR) optimizes confidence -> majority-class bias. We re-weight the
  multi-rule vote toward minority-discriminative rules (composite conf x lift, stratified coverage),
  recovering minority Recall/F1 with negligible accuracy loss and full rule interpretability."
- Show a 2-axis win: maintain overall accuracy vs CMAR while **gaining macro-F1/minority-Recall**,
  and back it with the Friedman/CD diagram on the F1 ranking. A small honest gain (e.g. +2-4 macro-F1
  points) that is **statistically significant** beats a fragile +0.5% accuracy claim.

**Realistic accuracy expectation:** CMAR -> modern AC improvements typically report **sub-1% to ~2%**
average accuracy gains across 20+ UCI sets. Do not promise large accuracy jumps; promise a
**significant imbalance-metric gain at preserved accuracy + interpretability**.

---

## Anchor citations
- CBA: Liu, Hsu, Ma. "Integrating Classification and Association Rule Mining." KDD 1998. (AAAI PDF)
- CMAR: Li, Han, Pei. ICDM 2001, pp.369-376. (Table 3: C4.5 83.34 / CBA 84.69 / CMAR 85.22)
- CPAR: Yin, Han. SDM 2003, pp.331-335.
- MCAR: Thabtah, Cowling, Peng. ACS/IEEE AICCSA 2005.
- ACAC: Huang et al., IEEE GrC. ACCF: Li, Qian, Yang, FSKD 2008.
- WCBA: Alwidian, Hammo, Obeid. Applied Soft Computing 62 (2018) 536-549.
- 2025 review: "Association rule-based classification: a comprehensive review." Expert Systems with
  Applications (Elsevier), S0957417425010760.
