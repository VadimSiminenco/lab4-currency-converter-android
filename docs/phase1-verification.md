# Phase 1 verification · 04.11.2026

Branch: feature/base-app. App version0.1.0. Actual device: Pixel_5, Android14/API34, emulator-5554. JDK17, Gradle8.13, AGP8.11.1, Kotlin2.2.21; exact dependencies in app/build.gradle.kts.

## Results

- assembleDebug and assembleDebugAndroidTest: successful.
- JVM tests:19 passed,0 failures/errors.
- Deterministic Android instrumentation:6 passed (3 UI/navigation,3 Room);2 optional smoke cases skipped without opt-in. Runner reports8 cases including those skips.
- Live BNM opt-in:1 passed; 100USD ->1806.44MDL, effective04.11.2026.
- Offline opt-in:1 passed after enabling emulator airplane mode/Wi-Fi off, force-stopping and relaunching the process without clearing Room. Saved snapshot date and offline banner verified. Airplane mode restored0, Wi-Fi restored1.
- Existing Pixel5 was started manually by the user after automated launch reported unavailable acceleration. No Windows acceleration/security settings changed.
- The Gradle UTP connected-task could not return results in this environment; scripts/test-device.ps1 instead builds/installs test APK and executes AndroidJUnitRunner through ADB, failing on runner failures or absent OK summary.

## Artifact

APK: app/build/outputs/apk/debug/app-debug.apk

SHA256: `63586b44166a218ac1b7f6cf5cd7413f8cebe2cc0cf82d8e898fca5747e0e667`

Screenshots: docs/images/converter.png, result.png, offline.png. These are actual device captures, not generated mockups. Market values are specific to the observed date, not permanent test fixtures.

## Scope and limitations

Converter, permanent Room cache, current30-day history fetching/revalidation and bottom navigation exist. Analytics tabs are explicitly unfinished placeholders: chart/statistics implementation belongs to feature/analytics. Appium/Selenium E2E is still pending feature/tests. Room reopen was checked by3 deterministic instrumentation tests; actual process relaunch without network additionally checked by the opt-in smoke test.

Normalized cache policy: complete sets of all6 supported currencies are persisted transactionally. A partial source response can report a missing selected currency but cannot overwrite a complete saved snapshot. BigDecimal values are stored as decimal Strings through explicit mapping; no redundant TypeConverter/Hilt framework was introduced.

## Real development corrections

- Initial Windows emulator launch lacked usable acceleration; manually launched emulator became reachable after explicit local ADB connection. Temporary duplicate TCP device entry was disconnected before testing.
- First XML parser and repository JVM checks passed; source was not copied/translated from Lab3.
- A validation label nested inside TextField merged semantics; it was moved into a separate tagged Text for testability.
- Compose assertTextContains defaults to exact matching; substring=true corrected the two test assertions.
- First UTP device runner failed before reporting tests; direct AndroidJUnitRunner produced actionable results.
- Gradle's deprecated kotlinOptions JVM target was replaced by compilerOptions.

No commit, push or merge was performed by the agent. Review and merge this stage before starting analytics.
