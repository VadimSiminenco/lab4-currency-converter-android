# Phase 3 verification · 05.11.2026

Branch feature/tests, based on merged analytics PR#3 (main2c95396). Pixel_5 Android14/API34, emulator-5554.

| Check | Actual result |
|---|---|
| Ordinary APK + Android test APK |BUILD SUCCESSFUL |
| Opt-in e2e APK + Kotlin test module |BUILD SUCCESSFUL |
| JVM unit/contract |49 passed,0 failed/errors/skipped |
| Native instrumentation |11 passed;4 opt-in cases skipped; runner OK(15 tests) |
| Appium/Selenium |6 passed,0 failed/errors/skipped |
| Ordinary APK DEX inspection |BnmRemoteSource present; FakeRemoteSource/E2eControlReceiver absent |

The6 E2E cases assert100USD=90EUR/reverse, all6 weekly and monthly metrics with independent literal expectations, failure→Retry recovery, same-currency output with unchanged request count and offline Room survival after force-stop/relaunch. Only the e2e package is cleared between independent scenarios. Offline case never clears or uninstalls the populated app; original network settings restored in finally.

Verified matrix: Appium3.1.0, UiAutomator2 5.0.3, Java Client9.5.0, Selenium4.34.0, JUnit4.13.2, Kotlin2.2.21/JDK17, Node22.19.0/npm10.9.3. All npm-transitive packages locked. Setup and commands: [E2E guide](e2e.md). Existing app unit/instrumentation commands remain available without the -Pe2e flag.

Actual corrections: npm recursive mkdir EPERM required integrity-verified local dependency extraction in this execution environment; Appium service was kept in an active tool session because short-lived tool processes do not retain background children; missing popup resource IDs fixed by adding testTagsAsResourceId to DropdownMenu's separate Compose window. The first external run failed at currency choice, was corrected and rerun. User manually restarted Pixel5 after it became unreachable. No Windows acceleration/security settings changed.

An early offline run hit UiAutomator2's missing accessibility root while the app was still opening. The test now retries that specific startup response within its 60-second explicit wait. The offline case passed individually before the full run.

Ordinary APK SHA256: `6b3ce9e198b2d632757fe664c563545940c8b1ba474ed8974347b103982f12e2`

E2E APK SHA256: `efc4128222f47dc116fd4d5843aefd5752469bb5db2230b62ea86cbcbdf679a2`

Screenshots/page trees: e2e-tests/build/evidence/. They contain explicitly synthetic E2E fixtures, not observed BNM market values. HTML report: e2e-tests/build/reports/tests/test/index.html. JVM report: app/build/reports/tests/testDebugUnitTest/index.html. Logs are local ignored .tools files.

Agent updated main and created feature/tests; no commit, push or PR merge was performed. Review/commit/push feature/tests and merge its PR before creating docs/final. Final report, student identity and personal Python/Kotlin learning comparison are still pending. This stage does not declare the whole laboratory finished.
