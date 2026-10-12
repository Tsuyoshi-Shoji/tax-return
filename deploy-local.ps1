# Tomcat force deploy script (Local H2 Database version)
param([int]$MaxWaitSeconds = 45)

$ErrorActionPreference = "Stop"
$ProjectPath = "c:\Users\ihsoy\OneDrive\Desktop\workspace\src\MySpace\tax-return"
$TomcatPath = "C:\Program Files\apache-tomcat-11.0.21\apache-tomcat-11.0.21"
$AppName = "final-tax-return-1.0-SNAPSHOT"
$WarFile = "$ProjectPath\target\$AppName.war"
$TomcatWebapps = "$TomcatPath\webapps"
$TomcatWar = "$TomcatWebapps\$AppName.war"
$TomcatAppDir = "$TomcatWebapps\$AppName"
$TomcatWork = "$TomcatPath\work"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "FORCE DEPLOY (H2 Local Database)" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

Write-Host "
[1/6] Stopping Tomcat..." -ForegroundColor Yellow
$pids = (Get-Process java -ErrorAction SilentlyContinue).Id
if ($pids) {
    foreach ($p in $pids) { taskkill /PID $p /F 2>$null }
}
Start-Sleep -Seconds 3
Write-Host "  [OK] Tomcat stopped" -ForegroundColor Green

Write-Host "
[2/6] Cleaning artifacts..." -ForegroundColor Yellow
Remove-Item $TomcatWar -Force -ErrorAction SilentlyContinue
Remove-Item $TomcatAppDir -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $TomcatWork -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $TomcatWork -Force | Out-Null
Remove-Item "$ProjectPath\target" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  [OK] All cleaned" -ForegroundColor Green

Write-Host "
[3/6] Maven build..." -ForegroundColor Yellow
Set-Location $ProjectPath
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }
Write-Host "  [OK] Build successful" -ForegroundColor Green

Write-Host "
[4/6] Deploying WAR..." -ForegroundColor Yellow
Copy-Item $WarFile -Destination $TomcatWar -Force
Write-Host "  [OK] WAR deployed" -ForegroundColor Green

Write-Host "
[5/6] Starting Tomcat..." -ForegroundColor Yellow
$env:CATALINA_HOME = $TomcatPath
$env:CATALINA_BASE = $TomcatPath
Start-Process -FilePath "$TomcatPath\bin\catalina.bat" -ArgumentList "start" -WindowStyle Hidden
Start-Sleep -Seconds 8
Write-Host "  [OK] Tomcat started" -ForegroundColor Green

Write-Host "
[6/6] Waiting for app..." -ForegroundColor Yellow
$Url = "http://localhost:8080/$AppName/home"
$Ready = $false
for ($i = 0; $i -lt $MaxWaitSeconds; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) { $Ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}

if ($Ready) {
    Write-Host "  [OK] App responded" -ForegroundColor Green
} else {
    Write-Host "  [WARN] App not responding yet (may still be starting)" -ForegroundColor Yellow
}

Write-Host "
=====================================" -ForegroundColor Green
Write-Host "DEPLOY FINISHED" -ForegroundColor Green
Write-Host "URL: http://localhost:8080/$AppName/" -ForegroundColor Cyan
Write-Host "Database: H2 (In-Memory)" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Green
