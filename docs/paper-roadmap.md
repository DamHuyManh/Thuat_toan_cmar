# Lộ trình đưa CMAR cải tiến lên bài báo quốc tế

> **Trạng thái hiện tại**: 3 cải tiến nhỏ (+0.27% F1 vs paper 2001) — chưa đủ A-tier
> **Mục tiêu**: B-tier conference (DEXA/FSKD) hoặc Q2 journal (Applied Intelligence/KAIS)
> **Hướng đang theo đuổi**: A — Boosted CMAR với χ²-weighted hard mining

---

## Phase 1 — STRENGTHENED ALGORITHM (đang làm, ~1-2 tuần)

### 1.1. Boosted CMAR (SAMME multiclass AdaBoost) — IMPLEMENTED ✅

- File: [src/cmar/boost/BoostedCMARClassifier.java](../src/cmar/boost/BoostedCMARClassifier.java)
- Algorithm: T rounds, mỗi round resample (X, y) theo weight, train CMAR, update weight theo lỗi
- Stop condition: err ≥ (K-1)/K (SAMME bound, K = num classes)
- Output: weighted vote of T classifiers với α = log((1-err)/err) + log(K-1)

### 1.2. Novel twists (cần implement nếu T=5 baseline work)

**Twist 1 — χ²-Weighted Reweighting**:
- Thay vì uniform reweight cho mọi misclassified, dùng χ² của winning rule làm extra multiplier
- `w_i *= exp(α_t) * (χ²_winning_rule(x_i) / max_χ²)`
- Rationale: instances bị các rule MẠNH (χ² lớn) misclassify → khó hơn → cần weight cao hơn

**Twist 2 — Diversity constraint**:
- Round t+1 mining phải có ≥30% antecedent items khác round t
- Force rule diversity → reduce ensemble correlation
- Reference: Kuncheva 2003 "Measures of Diversity in Classifier Ensembles"

**Twist 3 — Per-class boosting**:
- Standard SAMME treat all errors equally
- Em đề xuất: weight error theo class imbalance (kết hợp với cost-sensitive đã có)
- Novel cho AC space

---

## Phase 2 — STRONG BASELINE COMPARISON (~1 tuần)

Cần so sánh BoostedCMAR vs:

### AC methods
| Method | Năm | Lấy số từ đâu? |
|---|---|---|
| CBA | Liu 1998 | Paper Table |
| CMAR | Li 2001 | Paper Table (đã có) |
| CPAR | Yin 2003 | Paper Table |
| MCAR | Thabtah 2005 | Paper Table |
| L3 | Baralis 2008 | Paper Table |
| ACAC | Huang 2011 | Paper Table |
| ECBA-EX | Alwidian 2018 | Paper Table |
| **BoostedCMAR (ours)** | 2026 | Em chạy |

### Modern ML
| Method | Tool |
|---|---|
| C4.5 | Weka J48 |
| Random Forest | sklearn / Weka |
| XGBoost | xgboost4j |
| LightGBM | LightGBM Java |
| SVM (RBF) | LibSVM |

---

## Phase 3 — STATISTICAL SIGNIFICANCE TESTS (~2-3 ngày)

### Friedman test
- H₀: tất cả methods rank tương đương trên 26 datasets
- Reject nếu p < 0.05
- Đã có lib: Apache Commons Math (`FriedmanTest`)

### Nemenyi post-hoc
- Pairwise comparison sau Friedman
- Tính Critical Difference (CD)
- p_threshold = q_α / √(k(k+1)/(6N)) với k methods, N datasets

### CD-Diagram
- Visualize: methods trên 1 axis theo average rank
- Nối bằng bar nếu không khác có ý nghĩa
- Reference: Demšar 2006 "Statistical Comparisons of Classifiers over Multiple Data Sets"

---

## Phase 4 — PAPER WRITE-UP (~2-3 tuần)

### Outline đề xuất

**Title**: "Boosted CMAR with Adaptive Cost-Sensitive Voting for Imbalanced Multi-Class Classification"

**Sections**:
1. Introduction
   - AC vs modern DL: interpretability advantage
   - CMAR limitations: class imbalance, accuracy plateau
   - Our contributions (3-4 bullets)

2. Related Work
   - Associative Classification: CBA → CMAR → CPAR → MCAR → ECBA
   - Boosting: AdaBoost → SAMME
   - Boosted AC: Liu 2003 (gap em fill)
   - Cost-sensitive learning: Elkan 2001, Fawcett 2006

3. Method
   - 3.1 CMAR background (1 trang)
   - 3.2 Stratified Coverage Pruning (em đề xuất, 1 trang)
   - 3.3 Cost-Sensitive Voting (em đề xuất, 1 trang)
   - 3.4 Boosted CMAR algorithm (em đề xuất, 2 trang)
   - 3.5 Complexity analysis

4. Experiments
   - 26 UCI datasets
   - 10-fold stratified CV
   - Comparison vs 8+ baselines
   - Ablation study (mỗi component)

5. Statistical Analysis
   - Friedman + Nemenyi
   - CD diagram
   - Per-dataset Wilcoxon

6. Discussion
   - When does boosting help? (imbalanced, hard datasets)
   - Trade-offs (training time × T)
   - Limitations

7. Conclusion + Future Work

**Target venues** (rank theo realistic):

| Venue | Tier | Acceptance rate | Lead time |
|---|---|---|---|
| DEXA (Database and Expert Systems) | B | ~25% | 6 tháng |
| FSKD (Fuzzy Systems & Knowledge Discovery) | B | ~30% | 6 tháng |
| ICAI (Adv. in AI) | B | ~35% | 6 tháng |
| **Applied Intelligence (Springer)** | **Q2** | **~30%** | **6-9 tháng** |
| KAIS (Knowledge and Info Systems) | Q1/Q2 | ~25% | 9-12 tháng |
| ESWA (Expert Systems with Apps) | Q1 | ~20% | 9-12 tháng |
| IDA (Intelligent Data Analysis) | Q3/Q2 | ~30% | 6 tháng |

**Recommend**: aim Applied Intelligence (Springer) — đúng phạm vi, Q2, lead time hợp lý.

---

## Phase 5 — REPRODUCIBILITY ARTIFACT (~1 tuần)

- GitHub repo public với README chuẩn:
  - Setup instructions
  - Single-command reproducibility (`make all` hoặc `bash run_all.sh`)
  - Tất cả CSV data có sẵn (đã có ✅)
  - Pre-computed results để verify

- Docker image (optional nhưng plus điểm)

- Zenodo DOI cho code + data

---

## Timeline tổng thể (đề xuất)

| Tuần | Việc | Status |
|---|---|---|
| 1 | Boosted CMAR T=5 baseline | ⏳ Đang chạy |
| 2 | Phân tích kết quả + tune T, thêm twist 1-2 | ⏸️ |
| 3 | Diversity constraint + χ²-weighted reweight | ⏸️ |
| 4 | Modern baselines (Weka/sklearn) | ⏸️ |
| 5 | Stat tests + CD diagram | ⏸️ |
| 6 | Re-run all experiments với final config | ⏸️ |
| 7-8 | Paper draft | ⏸️ |
| 9 | Review nội bộ + fix | ⏸️ |
| 10 | Submit | ⏸️ |

---

## Risk register

| Risk | Mitigation |
|---|---|
| Boosting overfit nhỏ datasets (Labor 57, Lymph 148) | Early stopping nếu err tăng |
| Train time tăng T× | Parallelize T classifiers nếu cần |
| Modern ML beat CMAR everywhere | Highlight interpretability + small data |
| Reviewer hỏi "tại sao không neural?" | Section "limitations vs DL" |
| Paper rejected | Submit lại tier thấp hơn / dùng comment improve |

---

## Backup directions nếu Boosted CMAR fail

- **Bayesian CMAR** (Hướng B): nếu Boosted không gain ≥2%, switch sang Bayesian voting
- **Multi-resolution mining** (Hướng C): kết hợp với Boosted nếu cần thêm gain
- **DL hybrid** (Hướng D): nếu có time và muốn nhắm A-tier
