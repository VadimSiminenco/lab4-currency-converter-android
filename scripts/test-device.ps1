param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$LiveBnm,
    [switch]$OfflineCache
)
$ErrorActionPreference = 'Stop'
if ($LiveBnm -and $OfflineCache) { throw 'Choose either LiveBnm or OfflineCache.' }
$restoreNetwork = $false
$projectDirectory = Split-Path -Parent $PSScriptRoot
Push-Location $projectDirectory
try {
    & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Android test APK build failed.' }
    $sdkDirectory = $env:ANDROID_HOME
    if (-not $sdkDirectory) {
        $sdkLine = Get-Content -LiteralPath 'local.properties' | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1
        if (-not $sdkLine) { throw 'Set ANDROID_HOME or open the project in Android Studio to create local.properties.' }
        $sdkDirectory = ($sdkLine -replace '^sdk.dir=', '').Replace('\:', ':').Replace('\\', '\')
    }
    $adb = Join-Path $sdkDirectory 'platform-tools/adb.exe'
    & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Application installation failed.' }
    & $adb -s $Serial install -r -t 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Test application installation failed.' }
    $instrumentArguments = @('-s', $Serial, 'shell', 'am', 'instrument', '-w')
    if ($LiveBnm) { $instrumentArguments += @('-e', 'liveBnm', 'true', '-e', 'class', 'md.bnm.currencyconverter.LiveBnmSmokeTest#liveConversionShowsAmountSourceAndEffectiveDate') }
    if ($OfflineCache) {
        if ($Serial -notmatch '^emulator-') { throw 'OfflineCache changes emulator networking; use an emulator serial only.' }
        $previousAirplane = (& $adb -s $Serial shell settings get global airplane_mode_on | Out-String).Trim()
        $previousWifi = (& $adb -s $Serial shell settings get global wifi_on | Out-String).Trim()
        if ($previousAirplane -notin @('0','1') -or $previousWifi -notin @('0','1')) { throw 'Could not determine a restorable emulator network state.' }
        $restoreNetwork = $true
        & $adb -s $Serial shell cmd connectivity airplane-mode enable
        if ($LASTEXITCODE -ne 0) { throw 'Could not enable emulator airplane mode.' }
        & $adb -s $Serial shell svc wifi disable
        if ($LASTEXITCODE -ne 0) { throw 'Could not disable emulator Wi-Fi.' }
        & $adb -s $Serial shell am force-stop md.bnm.currencyconverter
        $instrumentArguments += @('-e', 'offlineBnm', 'true', '-e', 'class', 'md.bnm.currencyconverter.LiveBnmSmokeTest#offlineConversionUsesPersistentRoomData')
    }
    $instrumentArguments += 'md.bnm.currencyconverter.test/androidx.test.runner.AndroidJUnitRunner'
    $result = (& $adb @instrumentArguments 2>&1 | Out-String)
    Write-Output $result
    if ($LASTEXITCODE -ne 0 -or $result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_RESULT: shortMsg' -or $result -notmatch 'OK \(\d+ tests?\)') {
        throw 'Device tests failed. See instrumentation output above.'
    }
} finally {
    if ($restoreNetwork) {
        if ($previousAirplane -eq '0') { & $adb -s $Serial shell cmd connectivity airplane-mode disable }
        if ($previousWifi -eq '1') { & $adb -s $Serial shell svc wifi enable }
    }
    Pop-Location
}
