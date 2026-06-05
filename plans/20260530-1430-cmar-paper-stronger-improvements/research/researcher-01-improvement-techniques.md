# CMAR Stronger Improvements — Research Report

**Goal:** Techniques that boost BOTH F1-macro AND Recall-macro on imbalanced UCI datasets, on top of existing Java CMAR (Stratified Coverage Pruning, Cost-Sensitive Voting, Bagging T=10, Adaptive MinSup; current +2.17% F1 / +2.54% Recall vs paper). No Python deps. Java-implementable.

**Current weak spot:** continuous medical datasets (Diabetes, Heart, Pima, Cleveland) — these are discretization-bound, which points the strongest gains at the discretization/fuzzy directions.

---

## Ranked recommendations (expected gain × feasibility)

### TIER 1 — Implement these (high gain, high feasibility)

#### 1. Fuzzy Association Rules for continuous attributes (FACA-style fuzzy CMAR) ★ TOP PICK
- **Papers:** Hadi, Aburub, Alhawari, "A new fast associative classification algorithm for detecting phishing websites," *Applied Soft Computing* 48 (2016) 729-734 (FACA, uses Diffset). Earlier fuzzy-AC: Sowan, Dahal, Hossain et al., "A novel associative classification model based on a fuzzy frequent pattern mining algorithm," *Expert Systems with Applications* 41(13), 2014.
- **Reported:** FACA reported as the **best F1 of all tested** vs CBA, CMAR, MCAR, ECAR (F1 is the headline metric, not just accuracy). Fuzzy-AC papers report consistent accuracy gains over crisp CMAR on continuous/medical data.
- **Why it fits us:** Hard MDL bins lose info at boundaries (a glucose of 126 is "high", 125 is "normal"). Fuzzy membership lets one instance partially satisfy 2-3 fuzzy items → more minority-covering rules fire → higher recall. Directly targets our weak datasets.
- **Java feasibility: HIGH.** Replace crisp bin membership with triangular/trapezoidal fuzzy sets (3 per continuous attr). Item "support" → sum of membership degrees (fuzzy support); confidence uses fuzzy counts. FP-Growth still works with weighted (fuzzy) counts. ~2-3 days. Reuse existing rule/voting code; only the item-encoding + count aggregation change.
- **Risk:** choosing membership function breakpoints (use percentiles or MDL cut-points as fuzzy centers — cheap, principled).

#### 2. Better supervised discretization: CAIM / CACC (replace/augment MDL)
- **Papers:** Kurgan & Cios, "CAIM discretization algorithm," *IEEE TKDE* 16(2), 2004. Tsai, Lee, Yang, "A discretization algorithm based on Class-Attribute Contingency Coefficient (CACC)," *Information Sciences* 178, 2008. Survey: García et al., "A Survey of Discretization Techniques," *IEEE TKDE* 25(4), 2013 (ranks ChiMerge, CAIM, FUSINTER, MDLP top).
- **Reported:** CAIM/CACC maximize class-attribute interdependence; competitive-to-better than MDLP on medical sets, fewer/cleaner intervals. Gains are dataset-specific (MDL wins some, CAIM others).
- **Java feasibility: HIGH.** CAIM is a simple greedy top-down algorithm (quanta matrix + CAIM criterion). ~1 day. Drop-in replacement for the discretizer; everything downstream unchanged.
- **Note:** Lower expected gain than fuzzy (fuzzy strictly generalizes crisp bins), and partially overlapping. **Recommendation: do fuzzy first; CAIM as a cheap ablation/fallback or to seed fuzzy breakpoints.**

### TIER 2 — Strong but more work

#### 3. Cost-sensitive RULE GENERATION/RANKING (not just voting) — SSCR
- **Papers:** Hsu, Lin, et al. — "A Cost-Sensitive Based Approach for Improving Associative Classification on Imbalanced Datasets," *PAKDD 2014* (LNCS 8643). SSCR = Statistically Significant Cost-sensitive Rules: ranks rules by estimated misclassification **risk/cost** from training data, not raw confidence. General cost framing: Elkan, "The Foundations of Cost-Sensitive Learning," *IJCAI 2001*; MetaCost (Domingos, *KDD 1999*).
- **Reported:** Targeted at "class of interest = minority"; improves minority recall/AUC on imbalanced AC vs plain CBA/CMAR ranking.
- **Why it adds over our cost-sensitive voting:** we currently re-weight at the *voting* stage; SSCR pushes cost into *rule selection/pruning* so minority rules survive coverage pruning. Complements (does not duplicate) what we have.
- **Java feasibility: MEDIUM.** Add a cost-weighted rule score `score = conf × lift × cost(class)` used in ranking AND in stratified coverage pruning. ~2 days. We already compute lift/conf, so mostly plumbing the class-cost into the existing ranking comparator.

#### 4. Boosting that keeps rules (AdaBoost-style weighted rule mining, à la WARM / weighted FP-Growth)
- **Papers:** Soni & Pillai, "An associative classifier using weighted association rule," *IEEE NaBIC 2009*. Tao, Murtagh, Farid, "Weighted Association Rule Mining using Weighted Support," 2003 (weighted support framework). SMOTEBoost (Chawla et al., *PKDD 2003*) and RUSBoost (Seiffert et al., *IEEE SMC-A 2010*) as the imbalance-aware boosting templates.
- **Reported:** Weighted/boosted ensembles consistently lift minority F1/G-mean; SMOTEBoost/RUSBoost are standard strong baselines on imbalanced data.
- **How:** Run T rounds; mine CARs with per-instance weights (weighted support in FP-Growth = sum of instance weights, not counts); up-weight misclassified (esp. minority) each round; final = weighted vote. This is "boosting but we keep the rules" exactly as asked.
- **Java feasibility: MEDIUM.** We already have Bagging T=10 scaffolding — swap bootstrap sampling for AdaBoost weight updates + weighted-support FP-Growth. ~3 days. Higher variance/tuning risk than Tier 1.

### TIER 3 — Lower priority / research-heavy / honest cautions

#### 5. SMOTE before rule mining — CAUTION, likely weak for CMAR
- **Papers:** Chawla et al., "SMOTE," *JAIR* 16 (2002). Recent: *Scientific Reports* 2025 reports +13% F1, +16.5% G-mean, +7.9% AUC — **but for feature-space classifiers (SVM/trees), not AC.**
- **Honest problem:** SMOTE interpolates in **continuous feature space**; CMAR mines **discretized/categorical items**. Synthetic points (glucose=123.7) don't map to new rule semantics — after re-discretization they mostly duplicate existing minority bins ≈ random oversampling, which AC literature finds weak/over-fitting. No strong, clean "SMOTE+CMAR" result found.
- **Verdict:** Lower ROI than fuzzy. If tried, apply SMOTE *before* discretization and report honestly; expect marginal gain. Better imbalance lever = cost-sensitive rule gen (#3) or boosting (#4).

#### 6. Lazy / instance-specific selection (Lazy AC, CPAR, HARMONY)
- **Papers:** Veloso, Meira, Zaki, "Lazy Associative Classification," *ICDM 2006*. Yin & Han, "CPAR," *SDM 2003*. Wang & Karypis, "HARMONY," *SDM 2005* (instance-centric highest-confidence rule).
- **Reported:** Lazy AC improves on hard/overlapping/imbalanced instances; combines well with sampling. CPAR is faster but usually ~CMAR accuracy.
- **Feasibility: MEDIUM-LOW.** Lazy AC re-projects training data per test instance → rebuild rules at query time = big architectural change + runtime cost. **Defer** unless time permits; weaker F1/feasibility ratio than Tier 1-2.

#### 7. Multi-objective / Pareto rule ranking (support × confidence × coverage)
- **Papers:** NSGA-II-based associative classifiers, e.g. Ishibuchi et al. multi-objective fuzzy rule selection (*IEEE TFS* 2007); various MOEA-AC works.
- **Reported:** Pareto fronts trade accuracy vs interpretability; gains modest and tuning-heavy.
- **Feasibility: LOW.** Full MOEA is heavy. **Cheap lite version recommended instead:** our composite score already multiplies conf×lift; add a coverage/minority term → effectively a scalarized multi-objective rank. Fold this into #3 rather than building an evolutionary optimizer.

---

## Bottom line for the paper

1. **Fuzzy CMAR (#1)** — biggest expected F1/Recall gain on your weak continuous medical datasets, HIGH feasibility, strong cite story (FACA beats CMAR on F1). **Do this first.**
2. **CAIM discretization (#2)** — cheap, clean ablation; can also seed fuzzy breakpoints.
3. **Cost-sensitive rule generation/ranking SSCR (#3)** — pushes cost into selection/pruning, complements your existing cost voting; strong imbalance story.
4. **AdaBoost weighted-rule ensemble (#4)** — upgrade existing Bagging; "keeps rules + boosting" as asked.

Skip/deprioritize for a competition deadline: SMOTE+CMAR (semantic mismatch — be honest), full Lazy AC, full MOEA. Headline novelty = **Fuzzy CMAR + cost-sensitive rule generation**, both with proven F1 (not just accuracy) gains and clean Java implementations.

## Sources
- FACA: Applied Soft Computing 48 (2016) — https://www.sciencedirect.com/science/article/abs/pii/S1568494616303970
- Fuzzy AC: ESWA 41(13) 2014 — https://www.sciencedirect.com/science/article/abs/pii/S0957417414005600
- Cost-sensitive AC (SSCR), PAKDD 2014 — https://link.springer.com/chapter/10.1007/978-3-319-08979-9_3
- CAIM — https://www.researchgate.net/publication/3297255_CAIM_discretization_algorithm
- CACC (Information Sciences 2008) — https://sci2s.ugr.es/keel/pdf/algorithm/articulo/2008-Tsai-IS.pdf
- Discretization survey (IEEE TKDE 2013) — https://www.academia.edu/78051687/A_Survey_of_Discretization_Techniques
- Weighted AC (NaBIC 2009) — https://ieeexplore.ieee.org/document/5393687
- Weighted_FPGrowth — https://www.researchgate.net/publication/325466518
- WARM weighted support — https://eprints.soton.ac.uk/257986/1/331.tao.pdf
- Lazy Associative Classification (ICDM 2006) — http://www.cs.rpi.edu/~zaki/PaperDir/ICDM06.pdf
- SMOTE (JAIR 2002) — https://arxiv.org/abs/1106.1813
- SMOTE gains (Sci Reports 2025) — https://www.nature.com/articles/s41598-025-09506-w
- Cost-sensitive learning survey — https://sci2s.ugr.es/keel/pdf/keel/articulo/lop12_eswa.pdf
