param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference='Stop'
if($Serial -notmatch '^emulator-') { throw 'Use an emulator serial: these tests toggle emulator networking.' }
$repo=Split-Path -Parent $PSScriptRoot
$ownedServer=$null
Push-Location $repo
try {
    if(-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK17 before running.' }
    if(-not $env:ANDROID_HOME) {
        $sdkLine=Get-Content local.properties | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1
        if(-not $sdkLine) { throw 'Set ANDROID_HOME or open the project in Android Studio.' }
        $env:ANDROID_HOME=($sdkLine -replace '^sdk.dir=','').Replace('\:',':').Replace('\\','\')
    }
    $adb=Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
    if((& $adb -s $Serial get-state 2>&1 | Out-String).Trim() -ne 'device') { throw 'Start the Pixel emulator first.' }
    $env:APPIUM_HOME=Join-Path $repo '.tools/appium'
    $entry=Join-Path $env:APPIUM_HOME 'node_modules/appium/index.js'
    if(-not (Test-Path $entry)) {
        New-Item -ItemType Directory -Force $env:APPIUM_HOME | Out-Null
        Copy-Item e2e-tests/appium/package.json,e2e-tests/appium/package-lock.json $env:APPIUM_HOME
        $env:npm_config_cache=Join-Path $repo '.tools/npm-cache'
        Push-Location $env:APPIUM_HOME
        try { & npm.cmd ci --ignore-scripts; if($LASTEXITCODE -ne 0) { throw 'Appium dependency installation failed' } }
        finally { Pop-Location }
    }
    $serverStatus=$null
    try { $serverStatus=Invoke-RestMethod 'http://127.0.0.1:4723/status' -TimeoutSec 2 } catch { }
    if($serverStatus -and $serverStatus.value.build.version -ne '3.1.0') { throw 'Port4723 has a different Appium version; stop it before this run.' }
    if(-not $serverStatus) {
        $node=(Get-Command node.exe).Source
        $ownedServer=Start-Process -FilePath $node -ArgumentList @("`"$entry`"",'--address','127.0.0.1','--port','4723','--log-no-colors','--log',"`"$repo/.tools/appium-server.log`"") -WorkingDirectory $env:APPIUM_HOME -WindowStyle Hidden -PassThru -RedirectStandardOutput "$repo/.tools/appium-stdout.log" -RedirectStandardError "$repo/.tools/appium-stderr.log"
        $deadline=(Get-Date).AddSeconds(90)
        do {
            try { $serverStatus=Invoke-RestMethod 'http://127.0.0.1:4723/status' -TimeoutSec 2 } catch { Start-Sleep -Milliseconds 500 }
            if($ownedServer.HasExited) { throw 'Appium exited; inspect .tools/appium-stderr.log' }
        } until($serverStatus -or (Get-Date) -gt $deadline)
        if(-not $serverStatus) { throw 'Appium did not become ready; inspect .tools/appium-server.log' }
    }
    & .\gradlew.bat -Pe2e=true :app:assembleE2eDebug :e2e-tests:compileTestKotlin
    if($LASTEXITCODE -ne 0) { throw 'E2E build failed' }
    & $adb -s $Serial install -r app/build/outputs/apk/e2e/debug/app-e2e-debug.apk
    if($LASTEXITCODE -ne 0) { throw 'E2E APK installation failed' }
    & .\gradlew.bat :e2e-tests:test -PrunE2e=true "-Pserial=$Serial" "-PadbPath=$adb"
    if($LASTEXITCODE -ne 0) { throw 'E2E tests failed; see e2e-tests/build/reports/tests/test/index.html' }
} finally {
    if($ownedServer -and -not $ownedServer.HasExited) { Stop-Process -Id $ownedServer.Id }
    Pop-Location
}
