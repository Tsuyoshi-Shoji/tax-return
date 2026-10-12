@echo off
REM Deploy MFA-enabled tax-return app to Tomcat

setlocal enabledelayedexpansion

set TOMCAT_HOME=C:\Program Files\apache-tomcat-11.0.21\apache-tomcat-11.0.21
set WAR_SRC=c:\Users\ihsoy\OneDrive\Desktop\workspace\src\MySpace\tax-return\target\final-tax-return-1.0-SNAPSHOT.war
set WAR_DEST=%TOMCAT_HOME%\webapps\final-tax-return-1.0-SNAPSHOT.war
set APP_NAME=final-tax-return-1.0-SNAPSHOT

echo.
echo === Stopping Tomcat ===
taskkill /FI "WINDOWTITLE eq Tomcat*" /T /F 2>nul
timeout /t 3 /nobreak

echo.
echo === Cleaning old deployment ===
del "%WAR_DEST%" 2>nul
rmdir /s /q "%TOMCAT_HOME%\webapps\%APP_NAME%" 2>nul
echo Old files cleaned

echo.
echo === Deploying new WAR file ===
copy "%WAR_SRC%" "%WAR_DEST%"
if exist "%WAR_DEST%" (
    for %%A in ("%WAR_DEST%") do set size=%%~zA
    echo WAR deployed: %size% bytes
) else (
    echo ERROR: WAR file deployment failed
    exit /b 1
)

echo.
echo === Starting Tomcat ===
cd /d "%TOMCAT_HOME%"
call "%TOMCAT_HOME%\bin\catalina.bat" start
timeout /t 20 /nobreak

echo.
echo ✓ Deployment completed
echo Application URL: http://localhost:8080/%APP_NAME%/login
echo.
