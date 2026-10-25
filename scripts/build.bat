@echo off
setlocal
cd /d "%~dp0.."
if exist mvnw.cmd (
    call mvnw.cmd -B clean package
) else (
    call mvn -B clean package
)
if errorlevel 1 exit /b %errorlevel%
echo Build completed: engine-desktop\target\everything-engine-desktop-1.0-SNAPSHOT.jar
