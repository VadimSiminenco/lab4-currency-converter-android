# Specification 001 — Mobile Currency Converter

**Project:** Lab 4 — Native Android Currency Converter  
**Language:** Kotlin  
**UI toolkit:** Jetpack Compose + Material 3  
**Revision:** 1.1.0 (2026-11-04)

---

## 1. Purpose and Scope

Standalone native Android application that converts amounts between six currencies supported by the National Bank of Moldova (BNM) and displays exchange-rate history with analytical statistics for 7-day and 30-day windows. All logic is written from scratch in Kotlin; no source code from Lab 3 (Python) is copied or mechanically translated.

---

## 2. Supported Currencies

| Code | Name                        | Region        |
|------|-----------------------------|---------------|
| MDL  | Moldovan Leu                | Moldova       |
| EUR  | Euro                        | Eurozone      |
| USD  | US Dollar                   | United States |
| RON  | Romanian Leu                | Romania       |
| GBP  | British Pound Sterling      | United Kingdom |
| UAH  | Ukrainian Hryvnia           | Ukraine       |

MDL is the base currency published by BNM. BNM Value is the price in MDL of Nominal foreign-currency units. Value / Nominal is MDL per one foreign-currency unit. MDL itself has a base rate of 1.

---

## 3. Screen Architecture

The application has **three separate top-level screens** accessible through bottom navigation:

### 3.1 Converter Screen
- **Inputs:** amount (numeric), from-currency selector, to-currency selector.
- **Action:** "Convert" button.
- **Output:** converted amount displayed with explicit currency label.
- **Validation (without network):**
  - Amount must be a valid positive decimal; empty or non-numeric → show inline error.
  - From-currency equals to-currency → show inline notice "Same currency selected", result = input amount, no network call.
  - Amount ≤ 0 → show inline error.
  - Amount too large to represent with BigDecimal safely → show inline error.
- **Precision:** calculation in `BigDecimal`, rounded to 2 decimal places (HALF_UP), displayed with 2 decimal places and thousands-separator.

### 3.2 Analytics Screen — 7 Days
- **Period definition:** the 7 calendar days ending today, inclusive: [today−6, today]. Inject Clock and ZoneId. Show requested window and actual observation dates; stale cache does not move the window into the past.
- **Content:**
  - Currency pair selector (From / To).
  - Line chart of the computed rate over the period.
  - Statistics cards: Min, Max, Mean, Absolute change (Δ), Percentage change (%), Trend indicator.

### 3.3 Analytics Screen — 30 Days
- Same as 3.2 but with a 30-calendar-day window [today−29, today].
- Must be fully functional immediately after first data load — no waiting 30 real days.

---

## 4. Analytical Formulas

> [!IMPORTANT]  
> These are the canonical formulas. Implementations MUST use these exact definitions.

Let **observations** = the list of `(date, rate)` pairs for the selected currency pair, **sorted ascending by date**, restricted to the chosen calendar window.

Where `rate` for a foreign currency vs. MDL is:
```
rate(date) = Value(date) / Nominal(date)   // rate in MDL per 1 unit of foreign currency
```
If computing foreign→foreign, convert through MDL:
```
rate_A_to_B(date) = rate_A(date) / rate_B(date)
// or for MDL→X: 1 / rate_X(date)
```

### 4.1 Statistics Definitions

| Statistic | Formula |
|-----------|---------|
| **Min** | `min(observations.map { it.rate })` |
| **Max** | `max(observations.map { it.rate })` |
| **Mean** | `sum(rates) / count` |
| **Delta (Δ)** | `last.rate − first.rate` where first = observations[0], last = observations[last_index] |
| **Percent (%)** | `(last.rate − first.rate) / first.rate × 100` |
| **Trend** | see §4.2 |

### 4.2 Trend and edge cases

Use the sign of working, unrounded delta: positive Rising, negative Falling, zero No change. No artificial dead band. Text plus arrow is required; colour alone is insufficient. An unavailable trend is distinct from No change.

| Observations | Result |
|---|---|
| Empty | InsufficientData; no numeric statistics, not zeros |
| One | min=max=mean=value; delta, percent and trend unavailable; 'Not enough observations to compare' |
| Two or more | Compute on sorted, unique effective dates for the same currency pair |
| Zero denominator | Percentage unavailable, never divide by zero; reject non-positive source rates before persistence |
| Missing dates | Do not invent/interpolate/carry-forward rates; display actual coverage and gaps |

Independent synthetic acceptance example: USD=18 MDL/unit, EUR=20 MDL/unit; 100 USD -> 90 EUR and 90 EUR -> 100 USD. These are test values, not live quotes.

Snapshots use a single effective date. Cross-rates join both currencies on that date, never by list index or different dates. Distinct dates may legitimately have equal rates.

---

## 5. BNM Data Source

### 5.1 Verified source shape

Endpoint: https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY

Actual independently captured HTTP responses are in [evidence/observations.json](evidence/observations.json). Checked 2026-11-04: requests for 04.11.2026, Sunday 01.11.2026 and historical 06.10.2026 each returned HTTP200, ValCurs with requested Date and 40 entries. A future 05.11.2026 request returned HTTP404; no particular Errors XML body was verified.

```xml
<ValCurs Date="04.11.2026" name="Official exchange rate">
  <Valute ID="47">
    <NumCode>978</NumCode><CharCode>EUR</CharCode>
    <Nominal>1</Nominal><Name>Euro</Name><Value>20.1960</Value>
  </Valute>
</ValCurs>
```

CharCode/Nominal/Value are child elements, not attributes. RON and UAH both have Nominal 1 in the captured sample; normalize generically and use other currencies or synthetic fixtures for Nominal !=1 tests. Effective date comes from the response. Do not skip weekends/holidays or claim a fixed publication time. The observed Sunday result contradicts the earlier blanket weekend-empty assumption.

Validate root/date, positive decimal Value/Nominal and unique currency codes. Extra valid currencies may be ignored. Reject malformed snapshots before persistence. Missing selected currency gives an explicit unavailable error, not substitution. Successful empty/no-publication response, HTTP404, malformed/error payload and transient network failure have distinct typed outcomes. The exact error XML schema and publication schedule remain unverified.

### 5.2 History and refresh

First load requests each day in the current inclusive 30-calendar-day window, a full snapshot per day rather than per currency. Seven-day analytics reuses the data. Maximum 3 concurrent requests, bounded timeout/retries, cancellation, 200ms spacing between batches: application politeness policy, not an official BNM rate limit. No claim that 32 requests guarantee a complete month, or that a date-by-date API is the only possible BNM interface.

Store requested-date outcomes separately from effective-date snapshots. Deduplicate by effective date; failed requests remain retryable and distinguishable from successful empty responses. Startup retries unresolved dates and revalidates today plus the recent 7-day range. Explicit Refresh revalidates the entire requested window so corrections can be observed. Same-date replacement is atomic; invalid updates preserve old valid data. Show partial progress, real observation count/range and unresolved requests. Fewer observations alone do not prove a network failure.

### 5.3 Calendar boundaries and fallback

```text
7-day window:  [today - 6 days, today]
30-day window: [today - 29 days, today]
```

Inject Clock and device ZoneId. Freeze reference date for a refresh. Do not silently shift either window to the latest cached date. Stale cache outside the window gives no current-period data, with explanation. No invented zero points or interpolation; chart spacing follows actual dates.

Converter only: if a date has no snapshot, search at most 14 calendar days back and show the actual effective date. On source failure use the latest valid saved snapshot with its age, or show actionable error. Analytics does not expand its window using converter fallback. Source/retrieval metadata and partial/offline notices remain visible.

---

## 6. Data Model

See `data-model.md` for entity and DTO definitions.

---

## 7. Architecture — SOLID Layers

### 7.1 Module Separation

```
app/
├── ui/
│   ├── converter/          # ConverterScreen + ConverterViewModel
│   ├── analytics7/         # Analytics7Screen + Analytics7ViewModel
│   ├── analytics30/        # Analytics30Screen + Analytics30ViewModel
│   └── common/             # Shared Composables (StatCard, ChartView, ErrorBanner)
├── domain/
│   ├── model/              # Currency, ExchangeRate, AnalyticsResult, Trend
│   ├── usecase/            # ConvertUseCase, GetAnalyticsUseCase
│   ├── repository/         # RateRepository interface
│   └── validator/          # AmountValidator, CurrencyPairValidator
├── data/
│   ├── remote/             # BnmXmlFetcher, BnmXmlParser
│   ├── local/              # RoomDatabase, RateDao, RateEntity
│   └── repository/         # RateRepositoryImpl
└── di/                     # Dependency injection (Hilt or manual DI)
```

### 7.2 Interface Contracts

```kotlin
interface RateRepository {
    suspend fun getRatesForPeriod(currency: Currency, startDate: LocalDate, endDate: LocalDate): List<ExchangeRate>
    suspend fun refreshRates(currencies: List<Currency>, targetDate: LocalDate)
    fun observeLastUpdated(): Flow<LocalDate?>
}

interface RateLocalSource {
    suspend fun getSnapshots(start: LocalDate, end: LocalDate): List<RateSnapshot>
    suspend fun upsertSnapshots(snapshots: List<RateSnapshot>)
    suspend fun getLatestDate(currency: Currency): LocalDate?
}

interface RateRemoteSource {
    suspend fun fetchRatesForDate(date: LocalDate): RemoteOutcome
}
```

Implementations for `RateRemoteSource`:
- `BnmRemoteSource` — real HTTP + XML fetch (production).
- `FakeRemoteSource` — deterministic test data (test builds only).

### 7.3 State and dependency contracts

ViewModels expose immutable StateFlow state. Loading, Ready, Empty and Error are typed states. Ready may preserve cached content with refreshing=true and a non-blocking warning. Fatal error has no usable data. Do not use untyped Any for cached content or an undeclared generic T.

Domain interfaces return domain snapshots/models, not Room entities. Local storage DTO mapping belongs to the data layer. The repository coordinates persistence for every provider; fake/real source substitution must not silently remove caching. Inject Clock/ZoneId and focused source/storage interfaces. Constructor injection is sufficient; Hilt is optional only if justified. Contract tests cover both real-adapter-with-fake-HTTP and fake source outcomes, dates, units and failures.

---

## 8. Offline and Cache Behaviour

| Situation | Behaviour |
|-----------|-----------|
| Network available, cache empty | Fetch current 30-calendar-day window, populate Room transactionally, display partial progress |
| Network available, cache stale | Retry unresolved dates; revalidate recent dates and full window on explicit Refresh; merge valid snapshots |
| Network unavailable, cache present | Show cached data with banner: "Offline — showing data as of [actual_date]" |
| Network unavailable, cache empty | Show Error state: "No data available. Connect to the internet to load exchange rates." |
| Partial cache (< 7 days for 7-day window) | Show available points + notice: "Incomplete data: X of 7 days available" |
| Update error (non-fatal) | Log error, retain valid cache, show non-blocking warning banner |
| Update error (fatal/cache corrupt) | Show Error state with retry option; do NOT silently show zeros |

---

## 9. Numerical Precision

| Operation | Type | Note |
|-----------|------|------|
| Stored rate | `BigDecimal` decimal string | Non-terminating divisions use DECIMAL128 |
| Conversion calculation | `BigDecimal` | Exact multiplication/subtraction; DECIMAL128 for non-terminating divisions |
| Display (conversion result) | `BigDecimal.setScale(2, HALF_UP)` | 2 decimal places |
| Statistics (min, max, mean, delta) | `BigDecimal` | DECIMAL128 for division; do not pre-round delta/trend |
| Statistics (percent) | `BigDecimal` | Rounded to 4 dp for display |
| Chart rendering | `Float` | Only at the final rendering step in Compose |

---

## 10. UI / UX Requirements

- **Navigation:** `BottomNavigation` with 3 tabs: Converter, 7 Days, 30 Days.
- **Chart:** line chart with labeled X-axis (dates, abbreviated), labeled Y-axis (rate value with currency label). Rendered using a lightweight Compose chart library (e.g., `com.patrykandpatrick.vico`).
- **Statistics cards:** material cards per statistic. Each card shows label + value.
- **Trend indicator:** arrow symbol + text ("↑ Rising" / "↓ Falling" / "→ No change"). Must use text in addition to colour for accessibility.
- **Loading state:** circular progress indicator centred on screen.
- **Error state:** icon + descriptive message + retry button.
- **Empty state:** illustration/icon + message.
- **Offline banner:** persistent top banner in orange/amber when showing cached data.
- **Incomplete data notice:** inline text beneath the chart when < full period data is available.

---

## 11. Testing Requirements

### 11.1 Unit Tests (JUnit 4 + Kotlin)

Covered areas:
- XML parsing: valid XML, malformed XML, Errors XML, missing currencies, `Nominal ≠ 1`.
- Nominal normalisation: `Value/Nominal` correctness.
- Conversion: MDL↔EUR, USD↔GBP, same-currency (no network), precision, rounding.
- Statistics: all 6 statistics for complete datasets.
- Edge cases: empty set → `InsufficientData`; single point; zero base rate → null percent.
- Trend detection: rising, falling, flat, tiny positive/negative deltas.
- Validation: empty amount, negative, non-numeric, same currencies.

**Run command:**
```bash
./gradlew :app:testDebugUnitTest
```

### 11.2 E2E Tests (Appium + UiAutomator2, Kotlin)

**Exact dependency versions pending scaffold compatibility checks (not pinned yet):**
- Select mutually compatible Appium server, UiAutomator2 driver, Appium Java Client/Selenium and Kotlin JVM test runner using official compatibility documentation. No x/+ versions in resolved builds.
- Appium Java Client uses Selenium WebDriver APIs; Selenium ChromeDriver alone does not drive native Compose.
- App unit tests use JUnit4 with a portable XmlPullParser API and a pinned JVM implementation (candidate kXML2), not android.util.Xml JVM stubs. Prove parser tests work off-device.
- Room persistence requires Android instrumentation tests as well as process-relaunch E2E.

**Compose testability:**
- All interactive and observable elements carry `Modifier.testTag("tag_name")`.
- Enable root `Modifier.semantics { testTagsAsResourceId = true }`; there is no Manifest meta-data switch.
- Package FakeRemoteSource and fake time only in an e2e build flavor. Normal debug/release use real BNM. Hilt instrumentation-test overrides alone do not alter a normal APK launched externally by Appium.

**E2E test scenarios:**
1. **Conversion test:** enter amount, select currencies, tap Convert, assert result visible.
2. **7-day analytics:** navigate to 7-day tab, assert chart visible, assert actual metric values, dates and coverage, not just card presence.
3. **30-day analytics:** navigate to 30-day tab, assert chart visible, assert actual metric values, dates and coverage, not just card presence.
4. **Error state:** simulate network failure, assert error banner visible, retry button present.
5. **Offline with cache:** load data, kill network, terminate/relaunch process without clear-data/uninstall, assert Room data and actual dates/offline banner; restore changed network settings.
6. **Same-currency validation:** select EUR→EUR, assert inline notice shown, no network call.

---

## 12. Git Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, reviewed code only |
| `specs/mobile-currency-converter` | Specification documents (current) |
| `feature/base-app` | Project scaffold, navigation, DI |
| `feature/analytics` | Analytics screens, chart, statistics |
| `feature/tests` | Unit tests + E2E tests |
| `docs/final` | Final documentation and REPORT.md |

**Rules:**
- All merges to `main` via Pull Request.
- No direct commits to `main`.
- No automatic push/merge by tools or scripts.

---

## 13. Toolchain candidate — actual build pending

| Component | Selected candidate / observation |
|---|---|
| Gradle runtime / JVM target | JDK 17 explicitly configured in CLI and Android Studio |
| Wrapper | Gradle 8.13 |
| AGP | 8.11.1 |
| Kotlin Android + Compose compiler plugin | 2.2.21, including org.jetbrains.kotlin.plugin.compose |
| SDK | compile/target36; min26 |
| Existing Pixel_5 | API 34 google_apis/x86_64; not running at check |
| Libraries / test tools | Exact compatible versions pinned during scaffold; not 'latest' or inferred from cache |

Official compatibility sources are linked in research.md. Android Studio installation and JBR 25 are distinct from Gradle's JDK 17 runtime. No wrapper/app exists, so no successful build, installation or test result is claimed.

---

## 14. Out of Scope

- Currency flag images (optional enhancement, not required).
- Push notifications.
- Widget support.
- Tablet / foldable layout (adaptive layout is nice-to-have, not required).
- Login / user accounts.
- Currency rate alerts.

## 15. Revision provenance and open gates

Antigravity created the draft; Codex corrected specification/source/toolchain errors and independently captured BNM evidence on 2026-11-04. No Lab 3 code/fixtures were read or copied for this research. Preserve evidence and meaningful staged PRs. Phase 0 document review is not application acceptance. Remaining gates: specification PR, successful scaffold/dependency resolution, emulator execution, complete unit/instrumentation/Appium tests and real student REPORT.
