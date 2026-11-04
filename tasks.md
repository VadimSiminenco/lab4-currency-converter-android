# Task List — Lab 4: Mobile Currency Converter

**Version:** 1.2.0 (2026-11-04)
Status legend: `[ ]` = todo · `[~]` = in progress · `[x]` = done · `[!]` = blocked

---

## Phase 0 — Specification & Planning

| # | Task | Branch | Status |
|---|------|--------|--------|
| 0.1 | Write `specs/001-mobile-currency-converter/spec.md` | `specs/mobile-currency-converter` | [x] |
| 0.2 | Write `plan.md` | `specs/mobile-currency-converter` | [x] |
| 0.3 | Write `tasks.md` | `specs/mobile-currency-converter` | [x] |
| 0.4 | Write `data-model.md` | `specs/mobile-currency-converter` | [x] |
| 0.5 | Write `research.md` with environment inventory | `specs/mobile-currency-converter` | [x] |
| 0.6 | Write `checklists/acceptance.md` | `specs/mobile-currency-converter` | [x] |
| 0.7 | Write initial `README.md` | `specs/mobile-currency-converter` | [x] |
| 0.8 | Write `REPORT.md` template | `specs/mobile-currency-converter` | [x] |
| 0.9 | Create PR: `specs/mobile-currency-converter` → `main` | — | [x] |

---

## Phase 1 — Base App Scaffold

| # | Task | Branch | Status |
|---|------|--------|--------|
| 1.1 | Scaffold in existing repository; import in Android Studio (Compose, minSdk 26, Kotlin DSL) | `feature/base-app` | [x] |
| 1.2 | Verify `build.gradle.kts`: AGP 8.11.1, Kotlin 2.2.21, Gradle 8.13 with JDK 17 and matching Kotlin/Compose compiler plugin2.2.21 | `feature/base-app` | [x] |
| 1.3 | Add Compose BOM, Material 3, Navigation Compose | `feature/base-app` | [x] |
| 1.4 | Choose constructor injection; add Hilt only if justified; pin compatible KSP when required | `feature/base-app` | [x] |
| 1.5 | Add Room runtime + KSP | `feature/base-app` | [x] |
| 1.6 | Add OkHttp, Kotlin Coroutines | `feature/base-app` | [x] |
| 1.7 | Add chart library with analytics implementation (deferred to feature/analytics) | `feature/base-app` | [x] |
| 1.8 | Implement `Currency` enum (6 currencies) | `feature/base-app` | [x] |
| 1.9 | Implement `RateEntity` + `RateDao` + `AppDatabase` (Room) | `feature/base-app` | [x] |
| 1.10 | Persist decimal Strings with explicit domain/entity mapping (no redundant TypeConverter) | `feature/base-app` | [x] |
| 1.11 | Implement `RateRemoteSource` interface | `feature/base-app` | [x] |
| 1.12 | Implement `RateLocalSource` interface | `feature/base-app` | [x] |
| 1.13 | Implement `RateRepository` interface | `feature/base-app` | [x] |
| 1.14 | Implement `BnmXmlFetcher` (OkHttp, coroutine) | `feature/base-app` | [x] |
| 1.15 | Implement portable XML pull parser for observed ValCurs and typed no-data/error/malformed outcomes | `feature/base-app` | [x] |
| 1.16 | Implement `RateRepositoryImpl` with Room upsert + BNM fetch | `feature/base-app` | [x] |
| 1.17 | Implement initial history fetch (current 30-calendar-day window, semaphore(3), 200ms batch delay) | `feature/base-app` | [x] |
| 1.18 | Wire narrow source/storage/Clock interfaces via constructor injection; Hilt optional | `feature/base-app` | [x] |
| 1.19 | Implement `BottomNavigation` with 3 tabs (Converter, 7 Days, 30 Days) | `feature/base-app` | [x] |
| 1.20 | Implement `ConverterScreen` composable (amount input, currency dropdowns, button, result) | `feature/base-app` | [x] |
| 1.21 | Implement `ConverterViewModel` with `StateFlow<ConverterUiState>` | `feature/base-app` | [x] |
| 1.22 | Implement `AmountValidator` (blank, non-numeric, ≤ 0, > 1B) | `feature/base-app` | [x] |
| 1.23 | Implement `ConvertUseCase` (BigDecimal, same-currency shortcut, cross-rate) | `feature/base-app` | [x] |
| 1.24 | Implement offline banner composable | `feature/base-app` | [x] |
| 1.25 | Enable root Compose semantics testTagsAsResourceId; no Manifest meta-data | `feature/base-app` | [x] |
| 1.26 | Add `testTag` modifiers to all interactive elements in `ConverterScreen` | `feature/base-app` | [x] |
| 1.27 | Verify app builds and runs on Pixel 5 emulator; converter screen functional | `feature/base-app` | [x] |
| 1.28 | Create PR: `feature/base-app` → `main` | — | [x] |

---

## Phase 2 — Analytics Feature

| # | Task | Branch | Status |
|---|------|--------|--------|
| 2.1 | Implement dated `PairObservation` domain model | `feature/analytics` | [x] |
| 2.2 | Implement `AnalyticsResult` sealed class (InsufficientData / SinglePoint / Full) | `feature/analytics` | [x] |
| 2.3 | Implement `Trend` enum with `displayText()` | `feature/analytics` | [x] |
| 2.4 | Implement `StatisticsCalculator`: min, max, mean, delta, percent (nullable), trend | `feature/analytics` | [x] |
| 2.5 | Implement empty-set guard → `InsufficientData` (NOT zeros) | `feature/analytics` | [x] |
| 2.6 | Implement single-point guard → `SinglePoint` with notice | `feature/analytics` | [x] |
| 2.7 | Implement zero-base guard → `percent = null` with message | `feature/analytics` | [x] |
| 2.8 | Implement `GetAnalyticsUseCase` (window calculation, calls `StatisticsCalculator`) | `feature/analytics` | [x] |
| 2.9 | Implement parameterized `AnalyticsViewModel(7)` with `StateFlow<AnalyticsUiState>` | `feature/analytics` | [x] |
| 2.10 | Reuse `AnalyticsViewModel(30)` for the monthly window | `feature/analytics` | [x] |
| 2.11 | Map BigDecimal to chart-only Float/Double and actual relative date offsets in presentation | `feature/analytics` | [x] |
| 2.12 | Implement shared `AnalyticsScreen` for 7 days (chart, stats cards, states) | `feature/analytics` | [x] |
| 2.13 | Reuse `AnalyticsScreen` for 30 days | `feature/analytics` | [x] |
| 2.14 | Implement `StatCard` composable (label + value card) | `feature/analytics` | [x] |
| 2.15 | Implement `RateLineChart` composable using Vico (dates on X, rate on Y) | `feature/analytics` | [x] |
| 2.16 | Implement trend text display ("↑ Rising" / "↓ Falling" / "→ No change") | `feature/analytics` | [x] |
| 2.17 | Implement incomplete data notice composable | `feature/analytics` | [x] |
| 2.18 | Add `testTag` modifiers to all analytics elements | `feature/analytics` | [x] |
| 2.19 | Verify 7-day and 30-day screens fully functional on emulator after first data load | `feature/analytics` | [x] |
| 2.20 | Create PR: `feature/analytics` → `main` | — | [ ] |

---

## Phase 3 — Tests

| # | Task | Branch | Status |
|---|------|--------|--------|
| **Unit Tests** | | | |
| 3.1 | Configure JUnit4 app tests, JVM XML implementation and Room Android instrumentation | `feature/tests` | [ ] |
| 3.2 | Write `BnmXmlParserTest`: valid XML, malformed XML, Errors XML, missing currencies | `feature/tests` | [ ] |
| 3.3 | Write `BnmXmlParserTest`: Nominal ≠ 1 normalisation | `feature/tests` | [ ] |
| 3.4 | Write `ConvertUseCaseTest`: MDL↔EUR, USD↔GBP, same-currency | `feature/tests` | [ ] |
| 3.5 | Write `ConvertUseCaseTest`: precision, DECIMAL128 division, monetary HALF_UP to2dp | `feature/tests` | [ ] |
| 3.6 | Write `StatisticsCalculatorTest`: min, max, mean for complete datasets | `feature/tests` | [ ] |
| 3.7 | Write `StatisticsCalculatorTest`: delta = last − first | `feature/tests` | [ ] |
| 3.8 | Write `StatisticsCalculatorTest`: percent = (last − first) / first × 100 | `feature/tests` | [ ] |
| 3.9 | Write `StatisticsCalculatorTest`: empty set → `InsufficientData` | `feature/tests` | [ ] |
| 3.10 | Write `StatisticsCalculatorTest`: single point → `SinglePoint` with notice | `feature/tests` | [ ] |
| 3.11 | Write `StatisticsCalculatorTest`: zero base rate → percent = null | `feature/tests` | [ ] |
| 3.12 | Write `StatisticsCalculatorTest`: trend detection (rising, falling, no-change, tiny positive/negative deltas) | `feature/tests` | [ ] |
| 3.13 | Write `AmountValidatorTest`: empty, negative, zero, non-numeric, too large | `feature/tests` | [ ] |
| 3.14 | Confirm all unit tests pass: `./gradlew :app:testDebugUnitTest` | `feature/tests` | [ ] |
| **E2E Tests** | | | |
| 3.15 | Implement `FakeRemoteSource` with deterministic fixture data (multiple pairs × current 30-day window, gaps, fake Clock and failure cases) | `feature/tests` | [ ] |
| 3.16 | Create e2e flavor with FakeRemoteSource/Clock; normal debug/release retain BNM | `feature/tests` | [ ] |
| 3.17 | Check/pin compatible Appium server/UiAutomator2/Java Client/Selenium matrix; create Kotlin JVM e2e module | `feature/tests` | [ ] |
| 3.18 | Write `ConversionE2ETest`: enter amount, select currencies, tap Convert, assert result | `feature/tests` | [ ] |
| 3.19 | Write `Analytics7E2ETest`: navigate to 7-day tab, assert dated chart + actual metric values and coverage | `feature/tests` | [ ] |
| 3.20 | Write `Analytics30E2ETest`: navigate to 30-day tab, assert dated chart + actual metric values and coverage | `feature/tests` | [ ] |
| 3.21 | Write `ErrorStateE2ETest`: simulate offline, assert error banner + retry button | `feature/tests` | [ ] |
| 3.22 | Write `OfflineCacheE2ETest`: load data, disconnect, restart app, assert cached data + banner | `feature/tests` | [ ] |
| 3.23 | Write `SameCurrencyE2ETest`: select EUR→EUR, assert inline notice, no network call | `feature/tests` | [ ] |
| 3.24 | Document E2E run command and Appium server setup in README | `feature/tests` | [ ] |
| 3.25 | Create PR: `feature/tests` → `main` | — | [ ] |

---

## Phase 4 — Documentation & Report

| # | Task | Branch | Status |
|---|------|--------|--------|
| 4.1 | Fill in `REPORT.md` with actual observations and experience | `docs/final` | [ ] |
| 4.2 | Record actual errors encountered during implementation | `docs/final` | [ ] |
| 4.3 | Record actual IDE / AI tools / model names used | `docs/final` | [ ] |
| 4.4 | Write comparative section: Python vs Kotlin experience | `docs/final` | [ ] |
| 4.5 | Record BNM source limitations actually observed | `docs/final` | [ ] |
| 4.6 | Finalise `README.md` with complete setup + run + test instructions | `docs/final` | [ ] |
| 4.7 | Review all PR merges into `main` | — | [ ] |
| 4.8 | Final review: all acceptance criteria met per `checklists/acceptance.md` | `docs/final` | [ ] |
| 4.9 | Create PR: `docs/final` → `main` | — | [ ] |

## Revision1.1 review additions

Historical Phase0 note: document creation alone did not mean application acceptance. Phase1/2 evidence is recorded below; Appium acceptance remains pending. Requirements below remain unchecked until implemented and verified.

- [ ] Independent example: USD18 MDL/unit, EUR20 MDL/unit; 100 USD=90 EUR, reverse90 EUR=100 USD.
- [ ] Same-date currency joins and effective-date deduplication verified.
- [ ] Empty/single-point metrics remain unavailable where comparison cannot be made; no zero-change fiction.
- [ ] Current7/30 windows use injected today and do not shift for stale offline cache.
- [ ] Requested-date outcomes distinguish failure from successful no-publication response.
- [ ] Refresh revalidates recent/full windows; invalid snapshot preserves old valid data transactionally.
- [ ] Pixel_5 API 34 execution and explicit JDK 17 Gradle runtime confirmed.
- [ ] Room survives actual process relaunch without clear-data/uninstall.
- [ ] Test-only fake source packaged only in e2e flavor; normal debug/release remain real-data builds.
- [ ] Exact compatible dependency/server/client/driver versions pinned and build proven.
- [ ] Personal Python/Kotlin comparison written after actual implementation experience.

## Phase1 outcome · 04.11.2026

Converter and Room/history infrastructure implemented; APK and unit/instrumentation/live/offline smoke checks passed. Full evidence: docs/phase1-verification.md. Base-app PR #2 is merged. Chart dependency moved with feature/analytics; analytics evidence follows. Appium acceptance stays unchecked.

## Phase2 outcome · 04.11.2026

Analytics implementation, reactive Room observations, Vico2.2.0 and focused tests complete. Shared parameterized ViewModel/Screen replace duplicated weekly/monthly classes; PairObservation is the dated pair-domain model. See docs/phase2-verification.md. 43 JVM and11 deterministic Android tests passed; live/offline analytics checks each passed separately. Task2.20 PR is pending. Phase3 tasks remain the checklist for the dedicated test-stage review, including tests already introduced with features; no Appium execution is claimed.
