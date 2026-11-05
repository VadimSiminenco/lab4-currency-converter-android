# Research — Lab 4

Revision1.1.0 · 2026-11-04. Separate observed facts, selected candidates and build verification. Cache presence is not compatibility proof.

## Environment directly inspected

| Component | Observation |
|---|---|
| Android Studio | product-info.json build AI-261.26222.65.2614.16379836; dataDirectoryName AndroidStudio2026.1.4; codename not inferred |
| Bundled JBR | JAVA_VERSION=25.0.3; IDE runtime, not the selected Gradle runtime |
| CLI Java | OpenJDK 17.0.18 Temurin, java -version |
| Android SDK | C:/Users/user/AppData/Local/Android/Sdk; platforms android-34, android-36, android-36.1 present |
| Pixel_5 AVD | android-34/google_apis/x86_64; configuration present, emulator not running |
| adb devices -l | No connected/running device at check |
| Application / wrapper | Not created; no build/test performed |

Missing SDK environment variables do not mean the SDK is absent. compileSdk 36 does not require API36 on the emulator: the existing API 34 AVD meets minSdk 26.

## Phase0 baseline before implementation (historical)

JDK 17 for Gradle and bytecode; Gradle 8.13; AGP 8.11.1; Kotlin Android and org.jetbrains.kotlin.plugin.compose (version 2.2.21); compile/targetSdk36, minSdk 26.

Sources checked 2026-11-04:
- [AGP8.11](https://developer.android.com/build/releases/agp-8-11-0-release-notes): Gradle 8.13, JDK 17, SDK36 supported.
- [Kotlin matrix](https://kotlinlang.org/docs/gradle-configure-project.html): Kotlin2.2.20-2.2.21 supports Gradle7.6.3-8.14 and AGP7.3.1-8.11.1.
- [Compose compiler](https://kotlinlang.org/docs/compose-compiler-migration-guide.html): use org.jetbrains.kotlin.plugin.compose matching Kotlin.
- [Gradle JVM matrix](https://docs.gradle.org/current/userguide/compatibility.html): JDK25 needs Gradle9.1+ to run Gradle. Explicitly select JDK 17 in IDE and CLI for Gradle 8.13; JBR 25 can remain the IDE runtime.

Room/KSP, Compose BOM/Material3, HTTP, chart and optional Hilt exact versions remain to be pinned during scaffold. A BOM does not version Kotlin compiler or every third-party library. Prefer constructor injection unless Hilt reduces actual complexity. Resolve dependencies and assembleDebug before marking the candidate build-verified.

## Independently captured BNM observations

Endpoint: https://www.bnm.md/en/official_exchange_rates?get_xml=1&date=DD.MM.YYYY

Requests checked 2026-11-04 around11:38UTC. Raw successful bodies and URL/status/hash metadata are in specs/001-mobile-currency-converter/evidence/.

| Requested date | HTTP | Effective date / entries |
|---|---|---|
| 04.11.2026 | 200 | 04.11.2026 /40 Valute |
| 01.11.2026, Sunday | 200 | 01.11.2026 /40 Valute |
| 06.10.2026, historical | 200 | 06.10.2026 /40 Valute |
| 05.11.2026, future at check | 404 | No verified Errors XML body |

Root is ValCurs with Date and name attributes. Valute has child elements NumCode, CharCode, Nominal, Name, Value. RON and UAH both have Nominal 1 in the captured04.11 sample; JPY supplies a real Nominal!=1 example. Preserve actual data; no hardcoded RON rule. Successful Sunday response disproves categorical 'no weekend data', but does not establish the time a rate was first published.

Not verified: official rate limit, publication hour, every holiday pattern, correction schedule, a specific Errors XML schema, or absence of a bulk-history interface. Date-by-date is a chosen strategy, not proof it is the sole API. Error/empty/malformed tests use explicitly synthetic cases as well as independent real samples.

## History and precision design

Initial request window is today-29 through today; snapshot per date, not per currency. Max 3 concurrent requests and200ms spacing between batches are application politeness policies. Successful request date, response effective date and retrieval timestamp are distinct. Deduplicate effective-date snapshots; join both currencies on that date. Keep equal rates on distinct dates. Do not interpolate or fabricate points.

Retry unresolved requests; startup revalidates today/recent 7-day range; explicit Refresh rechecks the full selected period to observe corrections. Validation then transactional Room upsert protects valid cache. Thirty requests need not produce30 unique observations; a32-day fetch cannot guarantee completeness. Converter-only prior-date fallback does not shift analytics windows into the past.

Value/Nominal gives MDL per foreign unit; pair A->B=rateA/rateB. Use BigDecimal strings, exact multiplication/subtraction and DECIMAL128 division. Money display scale2 HALF_UP; percent display4; reference-rate display6. One point gives min/max/mean only, unknown delta/percent/trend. Trend follows exact working delta sign.

## Test setup decisions

App local tests: JUnit4 and portable XmlPullParser API with a JVM implementation (candidate kXML2, exact version pending). Do not call Android-only Xml stubs in plain JVM tests. Room: instrumentation on actual Android. E2E: separate Kotlin JVM module using Appium Java Client/Selenium APIs and UiAutomator2; exact compatible server/client/driver/runner versions pending official checks.

[Compose interoperability](https://developer.android.com/develop/ui/compose/testing/interoperability) enables root Compose semantics testTagsAsResourceId with stable testTag. No Manifest meta-data switch. [Appium Java Client](https://github.com/appium/java-client) supplies its Selenium compatibility matrix. E2E fake source/time belongs in a dedicated APK flavor; normal debug/release uses BNM. External Appium does not automatically load Hilt instrumentation-test bindings into a normal APK.

## Initial AI errors corrected

Antigravity drafted specs; Codex corrected reversed cross-rate, invented XML attributes, blanket weekend claims, unproven toolchain compatibility, wrong Manifest switch, stale-window shifting, misleading one-point trend and scientific-notation precision claim. Record these as real specification-stage errors, not implementation/test results. No Lab 3 source/fixture was copied or translated.

## Phase1 update · 04.11.2026

The selected JDK17/Gradle8.13/AGP8.11.1/Kotlin2.2.21 candidate now builds successfully. KSP2.2.21-2.0.5, Room2.7.2, Compose BOM2025.09.00, OkHttp4.12.0, coroutines1.10.2 and kXML2 2.3.0 are pinned. Constructor injection used; no Hilt. Pixel5 API34 launched and actual app/tests ran. See docs/phase1-verification.md for counts, smoke evidence and UTP-runner limitation. Earlier no-build/no-device observations describe Phase0, not current status.

## Phase2 verified choices · 04.11.2026

Pinned Vico compose-m3 2.2.0 ([release](https://github.com/patrykandpatrick/vico/releases/tag/v2.2.0)); published sources were checked against the actual API. Gradle resolves/builds it with the existing toolchain. Device captures prove actual line rendering. X is elapsed calendar days from window start, not the list index. Fixed padded Y range is disclosed beside the graph; explicit four-label placement improves readability. Decimal-to-Double conversion occurs only inside RateLineChart.

Shared AnalyticsViewModel(days=7|30) and AnalyticsScreen avoid duplicated logic. Reactive Room Flow and injected Clock provide current windows and testable updates. Resume synchronizes the reference date. Domain analytics never performs HTTP and all statistics use BigDecimal. Independent synthetic tests assert exact values and edge cases; real BNM smoke tests assert operational behaviour without freezing market values. See docs/phase2-verification.md for commands/results and screenshots. Appium/Selenium remains the next branch.

## Phase3 actual compatibility · 05.11.2026

Appium3.1.0 / UiAutomator2 5.0.3, Java Client9.5.0 / Selenium4.34.0, Kotlin2.2.21/JDK17, Node22.19.0/npm10.9.3 run6 external native E2E successfully. Node/npm satisfy Appium3 requirements; driver5 targets Appium3; Java Client/Selenium pair follows its official compatibility table (links in docs/e2e.md). npm-transitive versions are locked. All Selenium artifacts are forced to4.34.0 to close the Java Client's version range.

Source-set-selected AppBackend preserves normal task/APK paths while -Pe2e=true opts into the e2e flavor with a separate package. Production and test APKs use the same Room/repository/use cases/UI. The test source handles ready/failure/empty/gaps; Clock is frozen. Device networking loss makes the test source fail, exercising the shared saved-data fallback. Test-only request counters prove same-currency avoidance of remote lookup. Production DEX excludes test control/fake code. Popup menus require their own testTagsAsResourceId scope because they have a separate Compose window.
