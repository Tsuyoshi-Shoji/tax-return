$ErrorActionPreference = "Stop"

$defaultTomcatHome = "C:\Program Files\apache-tomcat-11.0.21\apache-tomcat-11.0.21"
$defaultJavaHome = "C:\Program Files\Java\jdk-26"

$tomcatHome = if ($env:CATALINA_HOME) { $env:CATALINA_HOME } else { $defaultTomcatHome }
$tomcatBase = if ($env:CATALINA_BASE) { $env:CATALINA_BASE } else { $tomcatHome }
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { $defaultJavaHome }

$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatBase
$env:JAVA_HOME = $javaHome

$shutdownBat = Join-Path $tomcatHome "bin\shutdown.bat"
$startupBat = Join-Path $tomcatHome "bin\startup.bat"

if (-not (Test-Path $shutdownBat)) {
    throw "shutdown.bat not found: $shutdownBat"
}
if (-not (Test-Path $startupBat)) {
    throw "startup.bat not found: $startupBat"
}

Write-Host "Stopping Tomcat..."
& $shutdownBat
Start-Sleep -Seconds 2

Write-Host "Starting Tomcat..."
& $startupBat
Start-Sleep -Seconds 3

try {
    $response = Invoke-WebRequest "http://localhost:8080/" -UseBasicParsing -TimeoutSec 15
    Write-Host "Tomcat restart finished. HTTP status: $($response.StatusCode)"
} catch {
    Write-Warning "Tomcat restarted, but HTTP check failed: $($_.Exception.Message)"
}

