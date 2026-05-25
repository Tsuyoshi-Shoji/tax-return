# Tomcat force deploy script for this project.
# Always performs a full clean before deploying to prevent stale resource issues.
param(
    [switch]$ForceClean,        # kept for backward compatibility (always clean now)
    [switch]$ForceWorkClean,    # kept for backward compatibility (always clean now)
    [switch]$KeepRoot,          # set this flag to skip ROOT webapp removal
    [int]$MaxWaitSeconds = 45,
    [int]$PollIntervalSeconds = 1,
    [int]$ProbeTimeoutSeconds = 2,
    [string]$ReadyPath = "/home",
    [int]$DevDbInitFailTimeoutMs = 10000
)

$ErrorActionPreference = "Stop"

$ProjectPath = "C:\Users\ihsoy\OneDrive\Desktop\workspace\src\MySpace\tax-return"
$TomcatPath = "C:\Program Files\apache-tomcat-11.0.21\apache-tomcat-11.0.21"
$AppName = "final-tax-return-1.0-SNAPSHOT"
$WarFile = Join-Path $ProjectPath "target\$AppName.war"
$TomcatWebapps = Join-Path $TomcatPath "webapps"
$TomcatWar = Join-Path $TomcatWebapps "$AppName.war"
$TomcatAppDir = Join-Path $TomcatWebapps $AppName
$TomcatWork = Join-Path $TomcatPath "work"
$TomcatRootDir = Join-Path $TomcatWebapps "ROOT"
$TomcatRootWar = Join-Path $TomcatWebapps "ROOT.war"
$LocalEnvScript = Join-Path $ProjectPath "scripts\set-db-env.local.ps1"

function Get-EnvVarValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($Name, "User")
    }
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($Name, "Machine")
    }

    return $value
}

function Is-MissingOrPlaceholder {
    param(
        [string]$Value
    )

    return [string]::IsNullOrWhiteSpace($Value) -or $Value -match '^<.+>$'
}

function Is-LocalHostValue {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }
    $normalized = $Value.Trim().ToLowerInvariant()
    return $normalized -eq "localhost" -or $normalized -eq "127.0.0.1" -or $normalized -eq "::1"
}

function Normalize-ReadyPath {
    param(
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return "/home"
    }
    if ($Path.StartsWith("/")) {
        return $Path
    }
    return "/$Path"
}

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "FORCE DEPLOY (Full Clean Mode)" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

if ($MaxWaitSeconds -lt 5) {
    Write-Host "[ERROR] MaxWaitSeconds must be 5 or more." -ForegroundColor Red
    exit 1
}
if ($PollIntervalSeconds -lt 1) {
    Write-Host "[ERROR] PollIntervalSeconds must be 1 or more." -ForegroundColor Red
    exit 1
}
if ($ProbeTimeoutSeconds -lt 1) {
    Write-Host "[ERROR] ProbeTimeoutSeconds must be 1 or more." -ForegroundColor Red
    exit 1
}
if ($DevDbInitFailTimeoutMs -lt 1000) {
    Write-Host "[ERROR] DevDbInitFailTimeoutMs must be 1000 or more." -ForegroundColor Red
    exit 1
}

if (Test-Path $LocalEnvScript) {
    . $LocalEnvScript
    Write-Host "  [OK] Loaded local DB env script (values hidden)" -ForegroundColor Green
}

Write-Host "`n[1/7] Checking required DB environment variables..." -ForegroundColor Yellow

$DbHost = Get-EnvVarValue -Name "DB_HOST"
$DbPort = Get-EnvVarValue -Name "DB_PORT"
$DbName = Get-EnvVarValue -Name "DB_NAME"

if (-not [string]::IsNullOrWhiteSpace($DbHost)) {
    if (Is-LocalHostValue -Value $DbHost) {
        Write-Host "[ERROR] DB_HOST must be an AWS RDS endpoint. localhost/127.0.0.1/::1 is not allowed." -ForegroundColor Red
        exit 1
    }

    if ([string]::IsNullOrWhiteSpace($DbPort) -or [string]::IsNullOrWhiteSpace($DbName)) {
        Write-Host "[ERROR] DB_HOST is set but DB_PORT or DB_NAME is missing. RDS settings are incomplete." -ForegroundColor Red
        exit 1
    }

    $DbUrl = "jdbc:mysql://${DbHost}`:${DbPort}/${DbName}?sslMode=REQUIRED&serverTimezone=Asia%2FTokyo&characterEncoding=UTF-8"
    [Environment]::SetEnvironmentVariable("DB_URL", $DbUrl, "Process")
    Write-Host "  [OK] DB_URL was composed from DB_HOST/DB_PORT/DB_NAME (values hidden)" -ForegroundColor Green
} else {
    $DbUrl = Get-EnvVarValue -Name "DB_URL"
}

$RequiredEnvVars = @("DB_URL", "DB_USERNAME", "DB_PASSWORD")
$MissingEnvVars = @()
foreach ($varName in $RequiredEnvVars) {
    if (Is-MissingOrPlaceholder -Value (Get-EnvVarValue -Name $varName)) {
        $MissingEnvVars += $varName
    }
}
if ($MissingEnvVars.Count -gt 0) {
    Write-Host "[ERROR] Missing required environment variables: $($MissingEnvVars -join ', ')" -ForegroundColor Red
    Write-Host "        Set DB_URL / DB_USERNAME / DB_PASSWORD before running deploy-force.ps1 (secrets must not be hardcoded)." -ForegroundColor Red
    exit 1
}

if ($DbUrl -match "jdbc:mysql://(localhost|127\.0\.0\.1|\[::1\]|::1)(:|/)") {
    Write-Host "[ERROR] DB_URL must target an AWS RDS endpoint. localhost URL is not allowed." -ForegroundColor Red
    exit 1
}

$DbInitFailTimeout = Get-EnvVarValue -Name "DB_INIT_FAIL_TIMEOUT_MS"
if (Is-MissingOrPlaceholder -Value $DbInitFailTimeout) {
    $DbInitFailTimeout = "$DevDbInitFailTimeoutMs"
}
if ($DbInitFailTimeout -notmatch '^\d+$') {
    Write-Host "[ERROR] DB_INIT_FAIL_TIMEOUT_MS must be a positive integer (milliseconds)." -ForegroundColor Red
    exit 1
}
[Environment]::SetEnvironmentVariable("DB_INIT_FAIL_TIMEOUT_MS", $DbInitFailTimeout, "Process")
Write-Host "  [OK] DB environment variables are set (values hidden)" -ForegroundColor Green
Write-Host "  [OK] DB_INIT_FAIL_TIMEOUT_MS is set for this deploy process" -ForegroundColor Green

Write-Host "`n[2/7] Stopping Tomcat..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Write-Host "  [OK] Tomcat stopped" -ForegroundColor Green

Write-Host "`n[3/7] Full clean of all artifacts..." -ForegroundColor Yellow

# Remove deployed WAR and expanded directory
Remove-Item $TomcatWar -Force -ErrorAction SilentlyContinue
Write-Host "  [OK] Removed deployed WAR: $TomcatWar" -ForegroundColor Green

Remove-Item $TomcatAppDir -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  [OK] Removed expanded app directory: $TomcatAppDir" -ForegroundColor Green

# Always clean Tomcat work directory (clears JSP compile cache and stale resources)
Remove-Item $TomcatWork -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $TomcatWork -Force | Out-Null
Write-Host "  [OK] Cleaned Tomcat work directory (JSP cache cleared)" -ForegroundColor Green

# Remove ROOT webapp unless explicitly kept
if (-not $KeepRoot) {
    if (Test-Path $TomcatRootWar) {
        Remove-Item $TomcatRootWar -Force -ErrorAction SilentlyContinue
        Write-Host "  [OK] Removed ROOT.war" -ForegroundColor Green
    }
    if (Test-Path $TomcatRootDir) {
        Remove-Item $TomcatRootDir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "  [OK] Removed ROOT webapp directory (prevents port 8080 conflict)" -ForegroundColor Green
    }
} else {
    Write-Host "  [SKIP] ROOT webapp kept (-KeepRoot flag was set)" -ForegroundColor Yellow
}

# Always clean Maven target for a completely fresh build
Remove-Item (Join-Path $ProjectPath "target") -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  [OK] Cleaned Maven target directory" -ForegroundColor Green

Write-Host "  [OK] All artifacts cleaned" -ForegroundColor Green

Write-Host "`n[4/7] Maven clean package..." -ForegroundColor Yellow
Set-Location $ProjectPath
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [ERROR] Maven build failed" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $WarFile)) {
    Write-Host "  [ERROR] WAR file was not generated: $WarFile" -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] Maven build completed" -ForegroundColor Green

Write-Host "`n[5/7] Deploying WAR..." -ForegroundColor Yellow
Copy-Item $WarFile -Destination $TomcatWar -Force
if (-not (Test-Path $TomcatWar)) {
    Write-Host "  [ERROR] WAR copy failed: $TomcatWar" -ForegroundColor Red
    exit 1
}
Write-Host "  Source WAR : $((Get-Item $WarFile).LastWriteTime)" -ForegroundColor Gray
Write-Host "  Tomcat WAR : $((Get-Item $TomcatWar).LastWriteTime)" -ForegroundColor Gray
Write-Host "  [OK] WAR deployed" -ForegroundColor Green

Write-Host "`n[6/7] Starting Tomcat..." -ForegroundColor Yellow
Start-Process -FilePath (Join-Path $TomcatPath "bin\startup.bat") -WindowStyle Hidden
Start-Sleep -Seconds 5
Write-Host "  [OK] Startup command executed" -ForegroundColor Green

Write-Host "`n[7/7] Waiting for application..." -ForegroundColor Yellow
$ReadyPath = Normalize-ReadyPath -Path $ReadyPath
$RootUrl = "http://localhost:8080/$AppName/"
$Url = "http://localhost:8080/$AppName$ReadyPath"
$Ready = $false
$RedirectLoopDetected = $false
$LoginRedirectCount = 0
for ($elapsed = 0; $elapsed -lt $MaxWaitSeconds; $elapsed += $PollIntervalSeconds) {
    $statusCode = $null
    $location = $null
    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec $ProbeTimeoutSeconds -MaximumRedirection 0 -UseBasicParsing -ErrorAction Stop
        $statusCode = [int]$response.StatusCode
        if ($statusCode -ge 200 -and $statusCode -lt 300) {
            $Ready = $true
            break
        }
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode.value__
            $location = $_.Exception.Response.Headers["Location"]
            if ($statusCode -ge 200 -and $statusCode -lt 300) {
                $Ready = $true
                break
            }
        }
    }

    if ($statusCode -ge 300 -and $statusCode -lt 400 -and $ReadyPath -eq "/home") {
        $LoginRedirectCount += 1
        Write-Host "  [WARN] /home returned redirect ($statusCode) to '$location'" -ForegroundColor Yellow
        if ($LoginRedirectCount -ge 3) {
            $RedirectLoopDetected = $true
            break
        }
    }

    Start-Sleep -Seconds $PollIntervalSeconds
}

if ($Ready) {
    Write-Host "  [OK] Application responded" -ForegroundColor Green
} elseif ($RedirectLoopDetected) {
    Write-Host "  [ERROR] Possible redirect loop detected at readiness URL: $Url" -ForegroundColor Red
    exit 1
} else {
    Write-Host "  [WARN] Application did not respond within timeout. Tomcat may still be starting." -ForegroundColor Yellow
}


Write-Host "`n=====================================" -ForegroundColor Green
Write-Host "DEPLOY FINISHED" -ForegroundColor Green
Write-Host "URL: $RootUrl" -ForegroundColor Cyan
Write-Host "READY CHECK URL: $Url" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Green
