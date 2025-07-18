#!/bin/bash

# Coffee Shot Timer - Release Build Script
# This script prepares and builds a release version of the app

set -e  # Exit on any error

echo "🚀 Coffee Shot Timer - Release Build Script"
echo "=========================================="

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    echo "❌ ERROR: keystore.properties not found!"
    echo "Please create keystore.properties with your signing configuration."
    echo "See keystore.properties for template."
    exit 1
fi

# Check if keystore file exists
KEYSTORE_FILE=$(grep "storeFile=" keystore.properties | cut -d'=' -f2)
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ ERROR: Keystore file '$KEYSTORE_FILE' not found!"
    echo "Please generate a keystore file or update keystore.properties."
    echo ""
    echo "To generate a new keystore:"
    echo "keytool -genkey -v -keystore release-key.keystore -alias coffeeshottimer -keyalg RSA -keysize 2048 -validity 10000"
    exit 1
fi

echo "✅ Keystore configuration verified"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Run tests
echo "🧪 Running tests..."
./gradlew test

echo "✅ All tests passed"

# Build release APK
echo "📦 Building release APK..."
./gradlew assembleRelease

# Build release AAB (App Bundle)
echo "📦 Building release AAB (App Bundle)..."
./gradlew bundleRelease

# Verify outputs
APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

if [ -f "$APK_PATH" ]; then
    echo "✅ Release APK created: $APK_PATH"
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "   APK Size: $APK_SIZE"
else
    echo "❌ ERROR: Release APK not found at $APK_PATH"
    exit 1
fi

if [ -f "$AAB_PATH" ]; then
    echo "✅ Release AAB created: $AAB_PATH"
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    echo "   AAB Size: $AAB_SIZE"
else
    echo "❌ ERROR: Release AAB not found at $AAB_PATH"
    exit 1
fi

# Verify APK signature
echo "🔐 Verifying APK signature..."
if command -v apksigner &> /dev/null; then
    apksigner verify "$APK_PATH"
    echo "✅ APK signature verified"
else
    echo "⚠️  apksigner not found - skipping signature verification"
fi

# Create outputs directory for easy access
OUTPUTS_DIR="release-outputs"
mkdir -p "$OUTPUTS_DIR"
cp "$APK_PATH" "$OUTPUTS_DIR/"
cp "$AAB_PATH" "$OUTPUTS_DIR/"

echo ""
echo "🎉 Release build completed successfully!"
echo "=========================================="
echo "📁 Release files copied to: $OUTPUTS_DIR/"
echo "   - app-release.apk (for sideloading/testing)"
echo "   - app-release.aab (for Google Play Store)"
echo ""
echo "📋 Next steps:"
echo "   1. Test the release APK on real devices"
echo "   2. Upload app-release.aab to Google Play Console"
echo "   3. Complete Play Console listing with metadata"
echo "   4. Submit for review"
echo ""
echo "📚 See DEPLOYMENT_GUIDE.md for detailed deployment instructions"
