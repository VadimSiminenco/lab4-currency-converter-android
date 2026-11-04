# Acceptance Checklist — Lab 4: Mobile Currency Converter

**Version:** 1.1.0 (2026-11-04)  
Status legend: `[ ]` = not checked · `[x]` = verified · `[~]` = partial · `[N/A]` = not applicable

Fill in this checklist during Phase 4 before final submission.

---

## 1. Project Setup

- [ ] Repository is `lab4-currency-converter-android` — separate from Lab 3.
- [ ] No Lab 3 Python source code is present or copied.
- [ ] No auto-translated Python→Kotlin code present.
- [ ] Application language: Kotlin only.
- [ ] UI toolkit: Jetpack Compose + Material 3.
- [ ] Builds successfully with `./gradlew assembleDebug`.
- [ ] Runs on Pixel 5 emulator without crash.

---

## 2. Compatibility Matrix

- [ ] AGP version: **8.11.1** (confirmed in `app/build.gradle.kts`).
- [ ] Kotlin version: **2.2.21** (confirmed in `build.gradle.kts`).
- [ ] Gradle wrapper: **8.13** (candidate; actual build pending) (confirmed in `gradle/wrapper/gradle-wrapper.properties`).
- [ ] `compileSdk`: **36**.
- [ ] `minSdk`: **26**.
- [ ] `targetSdk`: **36**.
- [ ] Compose BOM version pinned and documented.

---

## 3. Supported Currencies

- [ ] MDL is present and selectable.
- [ ] EUR is present and selectable.
- [ ] USD is present and selectable.
- [ ] RON is present and selectable.
- [ ] GBP is present and selectable.
- [ ] UAH is present and selectable.
- [ ] Exactly 6 currencies — no extras, no missing.

---

## 4. Screen Architecture

- [ ] Exactly 3 screens accessible via bottom navigation.
- [ ] Tab 1: Converter screen.
- [ ] Tab 2: 7-Day Analytics screen.
- [ ] Tab 3: 30-Day Analytics screen.
- [ ] Navigation between all 3 screens works correctly.

---

## 5. Converter Screen

- [ ] Amount input field present.
- [ ] From-currency selector present.
- [ ] To-currency selector present.
- [ ] Convert button present.
- [ ] Result displayed with correct currency label.
- [ ] Result rounded to exactly 2 decimal places (HALF_UP).
- [ ] Empty amount → inline error shown.
- [ ] Negative amount → inline error shown.
- [ ] Non-numeric input → inline error shown.
- [ ] Same-currency selection → notice shown; result = input amount; **no network call**.
- [ ] Rate date shown with the result.
- [ ] Conversion uses BigDecimal (not Float/Double) for calculation.

---

## 6. Analytics Screens — Common

- [ ] Currency pair selector present on both analytics screens.
- [ ] Line chart visible and readable.
- [ ] Chart X-axis shows dates.
- [ ] Chart Y-axis shows rate values with currency label.
- [ ] Min statistic card present.
- [ ] Max statistic card present.
- [ ] Mean statistic card present.
- [ ] Delta (Δ) statistic card present.
- [ ] Percent (%) statistic card present.
- [ ] Trend indicator present with text label (not colour only).
- [ ] Trend shows "↑ Rising", "↓ Falling", or "→ No change".
- [ ] Loading state shown while fetching.
- [ ] Error state shown with retry button when network fails.
- [ ] Empty state shown when no data available.

---

## 7. Analytics 7-Day Screen

- [ ] Period = [today − 6, today] (7 days inclusive).
- [ ] Chart uses actual effective-date points; weekends are not automatically excluded.
- [ ] Actual range/count and unresolved request dates shown; no invented points.
- [ ] Screen is fully functional immediately after first data load.

---

## 8. Analytics 30-Day Screen

- [ ] Period = [today − 29, today] (30 days inclusive).
- [ ] Chart shows up to 30 data points.
- [ ] Actual range/count and unresolved dates shown; stale cache never shifts window.
- [ ] Screen is **fully functional immediately after first data load** (no waiting 30 real days).

---

## 9. Analytical Formulas

- [ ] `delta = last.rate − first.rate` (observations sorted ascending by date).
- [ ] `percent = (last.rate − first.rate) / first.rate × 100`.
- [ ] `mean = sum(rates) / count`.
- [ ] Empty set → `InsufficientData` returned — **not zeros**.
- [ ] Single point → `SinglePoint` returned with "Only one data point available" notice.
- [ ] Zero base rate → `percent = null`; "Cannot compute percentage (zero base)" shown.
- [ ] Trend follows sign of working delta; one-point delta/percent/trend unavailable.
- [ ] All intermediate calculations use BigDecimal.

---

## 10. BNM Data Source

- [ ] XML endpoint used: `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY`.
- [ ] `Nominal` applied: `rate = Value / Nominal`.
- [ ] Generic Nominal!=1 normalization tested; no hardcoded RON assumption.
- [ ] `<Errors>` XML response handled gracefully (not crash).
- [ ] Malformed XML handled gracefully.
- [ ] Initial load fetches current 30-calendar-day window; unresolved/recent/full revalidation policy verified.
- [ ] Maximum 3 concurrent BNM requests (semaphore enforced).
- [ ] No data invented or hardcoded for dates without actual BNM publication.

---

## 11. Data Persistence (Room)

- [ ] Room database persists across app restarts.
- [ ] `INSERT OR REPLACE` used (upsert) — duplicates never stored.
- [ ] `BigDecimal` stored as `String` (not `Double`) in Room.
- [ ] `toPlainString()` used — no scientific notation in stored values.
- [ ] Database survives process kill and restart.
- [ ] No data loss on update error.

---

## 12. Offline Behaviour

- [ ] Offline with cached data: data shown with banner "Offline — showing data as of [date]".
- [ ] Offline with no cache: error state with message "No data available. Connect to the internet to load exchange rates."
- [ ] Update error: valid cache preserved; non-blocking warning shown.
- [ ] Partial cache: notice "Incomplete data: X of Y days available" shown.
- [ ] Banner shows the **actual date** of cached data, not today.

---

## 13. Architecture (SOLID)

- [ ] UI layer: `ConverterScreen`, `Analytics7Screen`, `Analytics30Screen` composables.
- [ ] ViewModel layer: separate ViewModels for each screen.
- [ ] Domain layer: `ConvertUseCase`, `GetAnalyticsUseCase`, `StatisticsCalculator`, `AmountValidator`.
- [ ] Data layer: `BnmXmlFetcher`, `BnmXmlParser`, `RateRepositoryImpl`, `RateDao`, `AppDatabase`.
- [ ] `RateRepository`, `RateLocalSource`, `RateRemoteSource` are **interfaces**.
- [ ] `FakeRemoteSource` exists and can substitute for `BnmRemoteSource` in test builds.
- [ ] Business logic does NOT depend on Android `Context` directly.

---

## 14. Unit Tests

- [ ] JUnit4 and portable JVM XML parser dependencies configured for app tests; Room instrumentation present.
- [ ] XML parser tests: valid, malformed, Errors response, missing currencies, Nominal ≠ 1.
- [ ] Conversion tests: MDL↔EUR, USD↔GBP, same-currency, precision, rounding.
- [ ] Statistics tests: all 6 statistics for complete data.
- [ ] Edge-case tests: empty set, single point, zero base.
- [ ] Trend tests: rising, falling, no-change, tiny positive/negative deltas.
- [ ] Validation tests: empty, negative, zero, non-numeric, too large.
- [ ] All unit tests pass: `./gradlew :app:testDebugUnitTest` exits with code 0.

---

## 15. E2E Tests (Appium)

- [ ] Appium Java Client version pinned in `build.gradle.kts`.
- [ ] Selenium WebDriver version pinned.
- [ ] UiAutomator2 driver version pinned.
- [ ] Root Compose semantics testTagsAsResourceId=true; IDs verified with UiAutomator2.
- [ ] All interactive elements have `testTag` modifiers.
- [ ] `FakeRemoteSource` used for E2E — deterministic test data.
- [ ] E2E test: conversion flow passes.
- [ ] E2E test: 7-day analytics screen passes.
- [ ] E2E test: 30-day analytics screen passes.
- [ ] E2E test: error state passes.
- [ ] E2E test: offline + cache restart passes.
- [ ] E2E test: same-currency validation passes.

---

## 16. Git Workflow

- [ ] Branches created: `specs/mobile-currency-converter`, `feature/base-app`, `feature/analytics`, `feature/tests`, `docs/final`.
- [ ] All merges to `main` via Pull Request.
- [ ] No direct commits to `main`.
- [ ] No automatic push/merge by tools.

---

## 17. REPORT.md

- [ ] Topic described.
- [ ] Python → Kotlin transition described.
- [ ] BNM source and its limitations described (actual observations, not invented).
- [ ] IDE tools actually used listed.
- [ ] AI tools / model names actually used listed (not fabricated).
- [ ] Actual errors encountered described.
- [ ] Comparative section: working in Kotlin vs Python.
- [ ] Items not yet completed marked as "заполнить после выполнения" (fill in after completion).

---

## 18. README.md

- [ ] Project description present.
- [ ] Setup instructions (Android Studio version, emulator setup).
- [ ] How to build and run the app.
- [ ] How to run unit tests.
- [ ] How to run E2E tests (Appium setup + run command).
- [ ] Git branching strategy described.
- [ ] Known limitations documented.

## Revision1.1 review additions

Document creation status does not mean application acceptance. No APK/build/tests exist yet. Requirements below remain unchecked until implemented and verified.

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
