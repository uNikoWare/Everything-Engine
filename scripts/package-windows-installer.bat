@echo off
setlocal
cd /d "%~dp0.."
call scripts\package-windows.bat
if errorlevel 1 exit /b %errorlevel%

where candle.exe >nul 2>&1
if errorlevel 1 (
    echo WiX Toolset 3.14.1 is required. Add its installation directory to PATH.
    exit /b 1
)
where light.exe >nul 2>&1
if errorlevel 1 (
    echo WiX Toolset 3.14.1 is required. Add its installation directory to PATH.
    exit /b 1
)

for /f "tokens=2 delims==" %%V in ('findstr /B "desktop.version=" engine-desktop\target\classes\everything-engine.properties') do set APP_VERSION=%%V
if not defined APP_VERSION (
    echo Could not determine the application version.
    exit /b 1
)

jpackage --type msi --input engine-desktop\target\package-input --main-jar everything-engine-desktop-1.0-SNAPSHOT.jar --main-class com.everythingengine.desktop.DesktopLauncher --name "Everything Engine" --app-version %APP_VERSION% --icon engine-desktop\src\main\resources\icons\everything-engine.ico --dest dist\windows --vendor "Everything Engine" --description "Modular science-fiction universe engine" --win-upgrade-uuid 3e6ccf80-d40f-4b18-a82f-4a5531fb13a1 --win-per-user-install --win-menu --win-menu-group "Everything Engine" --win-shortcut --java-options "--module-path=$APPDIR\lib" --java-options "--add-modules=javafx.controls"
if errorlevel 1 exit /b %errorlevel%
echo Windows MSI installer created in dist\windows\
