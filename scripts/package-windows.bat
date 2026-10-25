@echo off
setlocal
cd /d "%~dp0.."
if exist "dist\windows\Everything Engine" (
    attrib -R "dist\windows\Everything Engine\*" /S /D
    rmdir /S /Q "dist\windows\Everything Engine"
)
if not exist dist\windows mkdir dist\windows
where jpackage >nul 2>&1
if errorlevel 1 (
    echo jpackage was not found. Install a JDK 25 distribution with jpackage.
    exit /b 1
)
if exist mvnw.cmd (
    call mvnw.cmd -B clean package
) else (
    call mvn -B clean package
)
if errorlevel 1 exit /b %errorlevel%
copy /Y engine-desktop\target\everything-engine-desktop-1.0-SNAPSHOT.jar engine-desktop\target\package-input\everything-engine-desktop-1.0-SNAPSHOT.jar >nul
if errorlevel 1 exit /b %errorlevel%
jpackage --type app-image --input engine-desktop\target\package-input --main-jar everything-engine-desktop-1.0-SNAPSHOT.jar --main-class com.everythingengine.desktop.DesktopLauncher --name "Everything Engine" --app-version 1.0.0 --dest dist\windows --java-options "--module-path=$APPDIR\lib" --java-options "--add-modules=javafx.controls"
if errorlevel 1 exit /b %errorlevel%
echo Windows application image created in dist\windows\Everything Engine\
