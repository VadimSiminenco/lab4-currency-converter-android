# 💱 Lab4 — Currency Converter for Android

**Kotlin · Jetpack Compose · Material3 · Room · BNM XML · SOLID**

New Android converter plus 7/30-day exchange-rate analytics. Separate repository and independent Kotlin implementation; no Lab 3 Python code/fixtures may be copied or mechanically translated.

> **Phase 0: specifications revised 2026-11-04.** No Android project, wrapper, APK or tests exist yet. The following describes the planned application, not completed functionality.

## Three screens

| Screen | Planned content |
|---|---|
| Converter | MDL/EUR/USD/RON/GBP/UAH, positive amount, Swap, result and real source/date |
| 7 Days | Current-calendar chart, min/max/mean, absolute/percentage change and trend |
| 30 Days | Same analytics using fetched history from first load, without waiting a month |

Business calculations use BigDecimal. BNM Value/Nominal gives MDL per unit; A->B=rateA/rateB. Room cache survives process relaunch. Offline and partial/stale data are labelled. Empty/single-point data never implies zero change. Windows stay anchored to today, even with old cached snapshots.

## Documents

- [Specification](specs/001-mobile-currency-converter/spec.md)
- [Plan](plan.md) · [Tasks](tasks.md) · [Data model](data-model.md)
- [Research and environment](research.md)
- [Real BNM evidence](specs/001-mobile-currency-converter/evidence/observations.json)
- [Acceptance checklist](checklists/acceptance.md)
- [Report in progress](REPORT.md)

## Toolchain candidate

JDK 17 for Gradle, Gradle 8.13, AGP 8.11.1, Kotlin 2.2.21 and matching org.jetbrains.kotlin.plugin.compose. compile/targetSdk36, minSdk 26. Official matrices support this candidate; actual build is pending. Remaining Compose BOM/Room/KSP/chart/HTTP/test dependencies must be selected and pinned during scaffold.

Local SDK: C:/Users/user/AppData/Local/Android/Sdk. Existing Pixel_5 AVD is API 34/google_apis/x86_64 and meets minSdk 26; no API36 emulator replacement is necessary. Android Studio's bundled JBR 25 is an IDE runtime, not the selected Gradle runtime. Configure Gradle JDK 17 explicitly in IDE and CLI.

## Build/test commands — usable after scaffold

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :e2e:test
```

App local tests: JUnit4/Kotlin and portable JVM XML parser implementation. Room: Android instrumentation. E2E: Appium+UiAutomator2 with Kotlin/Appium Java Client and Selenium WebDriver APIs. Server/driver/client/Selenium exact compatibility, setup and APK flavor commands remain to be verified. Fake source/time belongs to an e2e flavor only; normal debug/release uses BNM. Compose IDs use root semantics testTagsAsResourceId, not Manifest meta-data.

## Source observations and limits

On2026-11-04 independently captured BNM responses for today, a historical date and a Sunday returned valid XML. A checked future date returned404. Fields are child elements, not attributes. Do not assume weekends are empty or claim an exact publication hour/official rate limit without evidence.

History requests current calendar dates, stores actual effective dates and deduplicates snapshots. Missing points are never fabricated. Revalidation of recent/full windows supports discovering corrections. Concurrency3/200ms batch spacing is an app policy, not a measured BNM SLA. No claim that 32 days guarantees full history.

## Git workflow and tools

Stages: specs/mobile-currency-converter -> feature/base-app -> feature/analytics -> feature/tests -> docs/final. Meaningful incremental commits; changes to main only through reviewed PRs. Do not commit/push/merge automatically.

Antigravity drafted initial docs; Codex corrected them and researched real environment/source behavior. Record actually used tools/model names and genuine Python->Kotlin experience in REPORT. UI development and emulator verification follow specification PR; no manual source-code writing is required from the student.
