# Plan — Lab 4: Mobile Currency Converter

**Version:** 1.1.0 (2026-11-04)  
**Language:** Kotlin · Jetpack Compose · Material 3  
**Target:** Existing Pixel_5 emulator · Android API 34 (compile/targetSdk36 candidate)

---

## Phases Overview

```
Phase 0 — Specification & Planning    [current branch: specs/mobile-currency-converter]
Phase 1 — Base App Scaffold           [branch: feature/base-app]
Phase 2 — Analytics Feature           [branch: feature/analytics]
Phase 3 — Tests                       [branch: feature/tests]
Phase 4 — Documentation & Report      [branch: docs/final]
```

---

## Phase 0 — Specification & Planning ✅

**Branch:** `specs/mobile-currency-converter`  
**Goal:** Complete all planning documents before writing a single line of Kotlin.

Deliverables:
- [x] `specs/001-mobile-currency-converter/spec.md`
- [x] `plan.md`
- [x] `tasks.md`
- [x] `data-model.md`
- [x] `research.md`
- [x] `checklists/acceptance.md`
- [x] `README.md`
- [x] `REPORT.md` (template)

**No implementation code is created in this phase.**

---

## Phase 1 — Base App Scaffold

**Branch:** `feature/base-app`  
**Goal:** Working app skeleton that runs on Pixel 5 emulator with navigation and empty screens.

### 1.1 Project Creation (Android Studio)

Create a new Android project in installed Android Studio (codename not inferred):
- **Template:** Empty Activity (Compose)
- **Package name:** `md.bnm.currencyconverter`
- **Language:** Kotlin
- **Minimum SDK:** API 26 (Android 8.0)
- **Build system:** Gradle (Kotlin DSL)

Verify `build.gradle.kts` aligns with the compatibility matrix in `research.md`:
```
AGP: 8.11.1
Kotlin: 2.2.21
Gradle wrapper: 8.13
Gradle runtime / JVM target: JDK 17
Compose compiler plugin: org.jetbrains.kotlin.plugin.compose (version 2.2.21)
compileSdk: 36
targetSdk: 36
minSdk: 26
```

### 1.2 Dependencies Setup

Add to `app/build.gradle.kts`:
- Compose BOM (exact compatible version selected/pinned during scaffold)
- Material 3
- Navigation Compose
- Constructor DI (Hilt optional only with justification)
- Room (runtime + KSP annotation processor)
- OkHttp
- Kotlin Coroutines
- Vico chart library

### 1.3 Architecture Skeleton

Create empty implementations of:
- `Currency` enum
- `RateRepository` interface
- `RateLocalSource` interface
- `RateRemoteSource` interface
- `AppDatabase` + `RateDao`
- `ConverterViewModel`
- `Analytics7ViewModel`
- `Analytics30ViewModel`
- Constructor DI wiring

### 1.4 Navigation

Implement `BottomNavigation` with 3 tabs:
- Converter
- 7 Days
- 30 Days

Verify navigation works on Pixel 5 emulator.

### 1.5 Converter Screen (Full Implementation)

Implement `ConverterScreen` with:
- Amount input field (validation inline)
- From/To currency dropdowns
- Convert button
- Result display
- Same-currency notice
- BNM rate fetch (via `BnmRemoteSource`)
- Room cache read/write
- Offline banner

### 1.6 Offline Infrastructure

Implement:
- `BnmXmlFetcher` (OkHttp, coroutine-based)
- `BnmXmlParser` (XmlPullParser)
- `RateRepositoryImpl` with Room + BNM source
- History fetch for current 30-calendar-day window, max 3 concurrent requests; deduplicate effective dates and revalidate recent/full ranges

---

## Phase 2 — Analytics Feature

**Branch:** `feature/analytics`  
**Goal:** Both analytics screens fully functional with chart, statistics, and correct edge-case handling.

### 2.1 Domain Logic

Implement `GetAnalyticsUseCase`:
- Filter `ExchangeRate` list to window
- Compute `AnalyticsResult` (Full / SinglePoint / InsufficientData)
- Correctly handle empty set, single point, zero base rate
- Classify by sign of unrounded delta; one-point delta/percent/trend unavailable

### 2.2 Statistics Engine

Implement `StatisticsCalculator`:
- `min`, `max`, `mean` using BigDecimal arithmetic
- `delta` = last − first (BigDecimal)
- `percent` = delta / first × 100 (nullable BigDecimal)
- `trend` from sign of working `delta`, no artificial dead band

### 2.3 Analytics 7-Day Screen

Implement `Analytics7Screen`:
- Currency pair selector
- Vico line chart
- Statistics cards (Min, Max, Mean, Delta, Percent, Trend)
- Incomplete data notice
- Loading / Error / Empty states

### 2.4 Analytics 30-Day Screen

Same as 2.3 with 30-day window. Reuse composables where possible.

### 2.5 Cross-Rate Calculation

For foreign-to-foreign pairs:
```
rate_A_to_B(date) = source_rate_mdl_per_unit / target_rate_mdl_per_unit
```
Implement in `ConvertUseCase` and `GetAnalyticsUseCase`.

---

## Phase 3 — Tests

**Branch:** `feature/tests`

### 3.1 Unit Tests

Configure JUnit4 app local tests and a JVM-compatible XML pull-parser implementation; add Room Android instrumentation. Focused tests should accompany each implementation phase. Cover all items listed in `spec.md §11.1`.

Run command:
```bash
./gradlew :app:testDebugUnitTest
```

### 3.2 E2E Tests

Create `e2e` module (separate Gradle module or separate source set).
- Configure an exact compatible Appium server/UiAutomator2/Java Client/Selenium matrix after official checks
- Implement `FakeRemoteSource` with deterministic fixture data
- Enable root Compose semantics testTagsAsResourceId; dedicated e2e flavor for fake source/time
- Implement 6 test scenarios from `spec.md §11.2`

---

## Phase 4 — Documentation & Report

**Branch:** `docs/final`

- Fill in `REPORT.md` with actual experience, errors encountered, IDE/AI tools used.
- Final `README.md` with setup + run instructions.
- Merge all feature branches into `main` via Pull Requests.

---

## Dependency Graph

```
Phase 0 → Phase 1 (scaffold) → Phase 2 (analytics) → Phase 3 (tests) → Phase 4 (docs)
                ↑                        ↑
           Must compile             Must have correct
           and run on               domain logic before
           emulator                 writing tests
```

---

## Time Estimates (reference only)

| Phase | Estimated Effort |
|-------|-----------------|
| Phase 0 (spec) | 2–3 hours |
| Phase 1 (scaffold + converter) | 4–6 hours |
| Phase 2 (analytics) | 3–5 hours |
| Phase 3 (tests) | 4–6 hours |
| Phase 4 (docs) | 1–2 hours |

---

## Tools Checklist Before Starting Phase 1

- [ ] installed Android Studio (codename not inferred) open and functional
- [ ] Pixel 5 emulator created and boots to home screen
- [ ] After scaffold, `.\gradlew.bat --version` reports selected Gradle 8.13/JVM17
- [ ] Gradle runtime explicitly JDK 17 in IDE and CLI; JBR 25 remains IDE runtime
- [ ] Internet available (for Gradle dependency download on first build)

## Review gates and responsibility

Phase 0 document creation is complete; specification PR and app acceptance are pending. Antigravity created draft docs; Codex corrected them using local/source evidence. No wrapper/build/APK/tests exist yet. Scaffold in this existing repository without nested Git or overwriting specs. Android Studio imports the generated project; manual source writing is not required. Hilt is optional: constructor injection is sufficient if simpler. Pin remaining library versions only after checking compatibility. Current reference-date periods and corrected formulas in spec.md are authoritative.
