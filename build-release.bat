@echo off
REM Coffee Shot Timer - Release Build Script for Windows
REM This script prepares and builds a release version of the app

echo 🚀 Coffee Shot Timer - Release Build Script
echo ==========================================

REM Check if keystore.properties exists
if not exist "keystore.properties" (
    echo ❌ ERROR: keystore.properties not found!
    echo Please create keystore.properties with your signing configuration.
    echo See keystore.properties for template.
    exit /b 1
)

echo ✅ Keystore configuration found

REM Clean previous builds
echo 🧹 Cleaning previous builds...
call gradlew.bat clean

REM Run tests
echo 🧪 Running tests...
call gradlew.bat test

if errorlevel 1 (
    echo ❌ Tests failed!
    exit /b 1
)

echo ✅ All tests passed

REM Build release APK
echo 📦 Building release APK...
call gradlew.bat assembleRelease

REM Build release AAB (App Bundle)
echo 📦 Building release AAB (App Bundle)...
call gradlew.bat bundleRelease

REM Verify outputs
set APK_PATH=app\build\outputs\apk\release\app-release.apk
set AAB_PATH=app\build\outputs\bundle\release\app-release.aab

if exist "%APK_PATH%" (
    echo ✅ Release APK created: %APK_PATH%
) else (
    echo ❌ ERROR: Release APK not found at %APK_PATH%
    exit /b 1
)

if exist "%AAB_PATH%" (
    echo ✅ Release AAB created: %AAB_PATH%
) else (
    echo ❌ ERROR: Release AAB not found at %AAB_PATH%
    exit /b 1
)

REM Create outputs directory for easy access
set OUTPUTS_DIR=release-outputs
if not exist "%OUTPUTS_DIR%" mkdir "%OUTPUTS_DIR%"
copy "%APK_PATH%" "%OUTPUTS_DIR%\"
copy "%AAB_PATH%" "%OUTPUTS_DIR%\"

echo.
echo 🎉 Release build completed successfully!
echo ==========================================
echo 📁 Release files copied to: %OUTPUTS_DIR%\
echo    - app-release.apk (for sideloading/testing)
echo    - app-release.aab (for Google Play Store)
echo.
echo 📋 Next steps:
echo    1. Test the release APK on real devices
echo    2. Upload app-release.aab to Google Play Console
echo    3. Complete Play Console listing with metadata
echo    4. Submit for review
echo.
echo 📚 See DEPLOYMENT_GUIDE.md for detailed deployment instructions

pause
