@echo off
setlocal
cd /d "%~dp0.."
if exist mvnw.cmd (
    call mvnw.cmd -B test
) else (
    call mvn -B test
)
if errorlevel 1 exit /b %errorlevel%
echo Tests completed.
