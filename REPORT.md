# REPORT — Lab 4: Native Android Currency Converter

**Subject:** Mobile Application Development  
**Lab:** 4  
**Title:** Native Android Currency Converter  
**Language:** Kotlin  
**Framework:** Jetpack Compose + Material 3  
**Student:** [заполнить после выполнения / fill in after completion]  
**Date submitted:** [заполнить после выполнения / fill in after completion]

---

## 1. Topic

Native Android application for currency conversion and exchange-rate analytics. The application fetches official exchange rates from the National Bank of Moldova (BNM) XML API, stores history locally using Room, and presents a converter and two analytics screens (7-day and 30-day windows) with charts and statistical indicators.

---

## 2. Lab 3 → Lab 4: Python to Kotlin Transition

**Lab 3** was implemented in Python with a Streamlit web interface. **Lab 4** is a native Android application written entirely in Kotlin.

### 2.1 Language Differences Observed

> [!NOTE]
> Fill in with actual observations during implementation. Examples below are placeholders — replace with real experience.

| Aspect | Python (Lab 3) | Kotlin (Lab 4) |
|--------|----------------|----------------|
| Type system | Dynamic | Static, null-safe |
| Concurrency model | [заполнить] | Kotlin Coroutines, structured concurrency |
| BigDecimal handling | `decimal.Decimal` | `java.math.BigDecimal` |
| XML parsing | [заполнить: library used in Lab 3] | XmlPullParser + kXML2 2.3.0 for JVM tests |
| Data persistence | [заполнить] | Room (SQLite) |
| UI | [заполнить] | Jetpack Compose |
| DI / modularity | [заполнить] | Constructor injection; no Hilt |

### 2.2 Comparative Experience

[заполнить после выполнения — опиши реальный опыт перехода: что было проще, что сложнее, какие концепции Kotlin оказались неожиданными по сравнению с Python.]

---

## 3. Data Source: BNM and Its Limitations

**Source:** National Bank of Moldova (BNM) official XML feed.  
**Endpoint:** `https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY`

### 3.1 Source Limitations Actually Observed

> [!NOTE]
> Fill in with actual observations during development and testing. Do not invent results.

| Limitation | Observed? | Notes |
|------------|-----------|-------|
| Sunday response | Yes,2026-11-04 | 01.11.2026 returned HTTP200, ValCurs dated01.11.2026; categorical weekend-empty claim disproved |
| No publication on Moldovan public holidays | [заполнить] | [заполнить] |
| Generic Nominal !=1 | Yes, captured XML | RON/UAH have Nominal 1 in04.11 sample; other source currencies provide larger nominal |
| Future-date HTTP failure | Yes | 05.11.2026 returned HTTP404; a particular Errors XML body unverified |
| Rate corrections (same date, different value) | [заполнить] | [заполнить] |
| Partial day: some currencies absent | [заполнить] | [заполнить] |
| Actual time of daily publication | [заполнить] | [заполнить] |

### 3.2 Impact on Implementation

[заполнить после выполнения — какие ограничения источника потребовали особых решений в коде.]

---

## 4. IDE and AI Tools Used

### 4.1 IDE

| Tool | Version | Usage |
|------|---------|-------|
| Android Studio | AndroidStudio2026.1.4 configuration; codename not inferred | Installed; Pixel_5 API34 used for device checks |
| [заполнить: другие инструменты] | [версия] | [назначение] |

### 4.2 AI Tools

> [!CAUTION]
> List only AI tools and models that were **actually** used. Do not invent names or versions. If a tool was used, describe what it was used for and whether its output was correct.

| Tool / Model | Version / Date | Used for | Accuracy of output |
|--------------|---------------|----------|--------------------|
| [заполнить] | [заполнить] | [заполнить] | [заполнить] |

---

## 5. Actual Errors and Corrections

> [!IMPORTANT]
> Document **real** errors encountered during the lab — in AI output, in generated code, in understanding, or in tooling. Do not fabricate errors.

| # | Context | Error / Problem | How it was resolved |
|---|---------|-----------------|---------------------|
| 1 | [заполнить] | [заполнить] | [заполнить] |
| 2 | [заполнить] | [заполнить] | [заполнить] |

---

## 6. Test Results

> [!CAUTION]
> Fill in with actual test results after running the tests. Do not invent results.

### 6.1 Unit Tests

- **Command:** `./gradlew :app:testDebugUnitTest`
- **Result:** 43 passed / 0 failed / 0 skipped (04.11.2026, Phase2)
- **Observations:** [заполнить]

### 6.2 E2E Tests (Appium)

- **Result:** [заполнить: passed X / failed Y]
- **Flaky tests:** [заполнить: none / list]
- **Observations:** [заполнить]

---

## 7. Architecture Decisions

### 7.1 Constructor injection and whether Hilt was needed

[заполнить после выполнения]

### 7.2 Why OkHttp over Ktor

[заполнить после выполнения — или измени, если выбор изменился при реализации]

### 7.3 Why XmlPullParser over DOM / SAX / Third-party

[заполнить после выполнения]

### 7.4 BigDecimal vs Double — Practical Impact

[заполнить после выполнения — опиши конкретные случаи, где BigDecimal важен]

---

## 8. Known Issues and Deviations from Specification

> [!NOTE]
> Document any intentional or unintentional deviations from the specification.

| # | Specification Requirement | Deviation / Issue | Reason |
|---|--------------------------|-------------------|--------|
| — | — | — | — |

[заполнить после выполнения]

---

## 9. What Would Be Done Differently

[заполнить после выполнения — честный разбор: что нужно было начать раньше, что оказалось сложнее ожидаемого.]

---

## 10. Conclusion

[заполнить после выполнения]

---

*Template prepared: 2026-11-04. All sections marked «заполнить после выполнения» must be completed before submission.*

## Phase 0 actual work log · 2026-11-04

Antigravity created the initial specification documents. OpenAI Codex reviewed/corrected them and independently inspected SDK/AVD/Java plus live BNM responses. Exact AI model names were not independently recorded; do not invent them. Account quota message does not establish whether an Antigravity subscription expired.

Real specification-stage errors corrected:
- Reversed cross-rate: MDL-per-unit rates require source/target, not target/source.
- Invented XML attributes and blanket no-weekend-data claim: captured real child elements and valid Sunday response.
- Unsupported Gradle/JDK compatibility inferred from cache: selected documented candidate Gradle 8.13/JDK 17/AGP 8.11.1/Kotlin 2.2.21; build pending.
- Wrong Manifest testTags switch: root Compose semantics specified instead.
- Single observation treated as Flat/zero change and stale windows shifted to old dates: unknown comparative metrics and today-anchored periods specified.

Android SDK exists; Pixel_5 configuration uses API 34/google_apis/x86_64. adb showed no running device. No Android app/build/tests yet. The student's substantive personal comparison of Python and Kotlin stays pending until actual implementation; specification review is not that experience.

## Phase1 factual update · 04.11.2026

OpenAI Codex generated the new Kotlin implementation, portable XML parser, Room repository, Compose UI and focused tests. Android Studio/Pixel5 supplied the emulator; Gradle/JDK17 built APKs. Exact model name is not independently recorded here. No Lab3 code or fixtures were reused.

Actual results:19 JVM tests and6 deterministic Android checks passed;2 opt-in smoke cases are skipped in routine runs and each passed separately (real BNM; persistent cache after process restart without network). Gradle UTP failed to receive results; a documented direct instrumentation script was used. Compose semantics merging and exact-versus-substring assertions caused early UI test failures and were corrected. APK runs on Pixel5 API34. Graphs/statistics and Appium E2E remain future stages.

These are factual development observations, not the student's personal experience. The substantive comparison of learning/working with Python versus Kotlin must still be completed by the student after using and explaining the application.

## Phase2 factual update · 04.11.2026

Weekly/monthly analytics implemented with a shared parameterized ViewModel/Screen, Vico2.2.0, Room Flow and exact decimal statistics. Empty/single/complete cases are explicit. Domain tests verify sorting, latest-correction deduplication, same-date pair joins, precision, non-shifted windows and sign-based trends. Double is limited to chart rendering. 43 JVM checks and11 deterministic Android checks passed;4 opt-in smoke tests are skipped in routine Android runs. Live analytics and offline process-relaunch analytics each passed in separate runs. Screenshots and commands: docs/phase2-verification.md.

Git base-app PR #2 is merged. Analytics changes await their own PR. Appium Java Client/Selenium tests are not implemented or executed yet. Personal comparison and learning experience remain student-authored sections to complete before submission.
