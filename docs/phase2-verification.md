# Phase 2 verification · 04.11.2026

Branch: feature/analytics. Base-app PR #2 is merged into main. Version0.2.0, Pixel_5 Android14/API34, emulator-5554. JDK17, Gradle8.13, AGP8.11.1, Kotlin2.2.21, Vico2.2.0.

## Implemented behaviour

- Shared parameterized AnalyticsViewModel/AnalyticsScreen serve7 and30 days. Period includes today-6/today-29 through today, supplied by Clock. Resume reanchors a changed calendar day.
- Room Flow updates dated observations without adding a second HTTP history loader. Existing BNM endpoint,6 currencies, Value/Nominal normalization, max3 concurrent requests, converter formula and persistent schema1 are retained.
- GetAnalytics filters window, deduplicates effective dates using newest fetchedAt, joins source/target within the same snapshot and sorts dates. No interpolation or shifted stale window. Equal values on separate dates are separate observations.
- BigDecimal min/max/mean, delta=last-first, percent=delta/first*100, trend=sign(unrounded delta). Empty has no fabricated statistics. One point supplies min/max/mean only. Zero base gives unknown percentage. Tiny nonzero display changes use a less-than indicator instead of implying zero.
- Vico line chart uses actual elapsed-day X coordinates and Double only at final rendering. The padded Y scale is disclosed; four Y labels and compact date labels improve readability. A single observation is shown as one dot without a fake series.
- Pair selection, swap, refresh, loading, empty/retry, read-error and cached-data warning are tagged for native and future Appium tests. Date range, coverage, actual observation dates, source and retrieval time remain visible.
- Retry restarts the saved-history subscription after a read failure, even if the calendar date has not changed. A failing-then-recovered fake repository verifies actual recovery, not only a button callback.

## Executed checks

| Check | Actual result |
|---|---|
| assembleDebug + assembleDebugAndroidTest | BUILD SUCCESSFUL |
| JVM suite |43 passed,0 failed/errors |
| Deterministic Android |11 passed:5 analytics UI,3 converter/navigation,3 Room |
| Default instrumentation summary |OK (15 tests), including4 assumption-gated smoke cases skipped without opt-in |
| LiveAnalytics |1 passed: actual weekly/monthly charts and6 metric cards |
| OfflineAnalytics |1 passed: force-stopped process, no network, Room data available on both periods with warning |

JVM groups: AmountValidator2, XML6, Conversion3, Repository8, Statistics11, GetAnalytics8, AnalyticsViewModel5. Synthetic data and fixed clocks give independent numeric assertions; live-market tests do not hardcode future exchange rates. Android UI tests assert weekly10..16 gives mean13, delta6,+60%; monthly10..39 gives mean24.5, delta29,+290%. Empty/single/error states are checked separately.

The emulator network is restored to airplane_mode_on=0 and wifi_on=1 after offline verification. No uninstall or clear-data was used. Entity schema is unchanged, and3 Room persistence/transaction tests still pass. Native instrumentation is not Appium/Selenium; that suite remains the next stage. The existing UTP-runner issue is bypassed with the documented direct AndroidJUnitRunner script.

## Reproduce in PowerShell

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
.\gradlew.bat :app:testDebugUnitTest
.\scripts\test-device.ps1 -Serial emulator-5554
.\scripts\test-device.ps1 -Serial emulator-5554 -LiveAnalytics
.\scripts\test-device.ps1 -Serial emulator-5554 -OfflineAnalytics
```

Start Pixel_5 first. SDK must be set through local.properties or ANDROID_HOME. OfflineAnalytics temporarily changes only the specified emulator's networking, preserves app data and restores its previous settings in finally. Run LiveAnalytics first to populate history. A previously saved snapshot outside the requested current period correctly gives no analytics data.

## Artifacts and observed data

APK: app/build/outputs/apk/debug/app-debug.apk

SHA256: `af9a1110f5164c6ea64b5d4d6b2afcf74cf6ea7ddd550facae811e00f2233df2`

Actual device screenshots: docs/images/analytics-7.png, analytics-30.png, analytics-7-stats.png, analytics-30-stats.png, analytics-offline.png. They are device captures, not generated mockups. Some captures can show an in-progress refresh while existing data remains available.

Observed month USD/MDL over06.10.2026–04.11.2026:30 observations, min17.823600, max18.103200, average17.969187, delta+0.134700, change+0.7513%, Rising. These are observations of this run, not permanent acceptance fixtures.

## Review and next branch

Review feature/analytics, commit and push it, open PR into main and merge after review. Only then update local main and create feature/tests. Appium Java Client/Selenium + UiAutomator2, contract coverage and final report remain pending. Personal Python/Kotlin experience must be written by the student.

No commit, push or merge was performed by the agent. An unrelated pre-existing untracked file whose name starts with 'istent Room cache' was left untouched and is excluded from suggested staging paths.
