# CoffeeShotTimer build commands
# Run `just --list` to see available recipes

export JAVA_HOME := "/usr/lib/jvm/java-21-openjdk"

# Default recipe - show available commands
default:
    @just --list

# --- Build & Test ---

# Run detekt static analysis
detekt:
    ./gradlew detekt

# Run Android lint (devDebug)
lint:
    ./gradlew lintDevDebug

# Run unit tests (devDebug)
test:
    ./gradlew testDevDebugUnitTest

# Build debug APK (devDebug)
build:
    ./gradlew assembleDevDebug

# Clean build artifacts
clean:
    ./gradlew clean

# Run all checks (detekt + lint + tests)
check: detekt lint test

# --- Device ---

# Install debug APK on connected device
install:
    ./gradlew installDevDebug

# Pair with device (one-time, requires pairing code from phone)
adb-pair ip port:
    adb pair {{ip}}:{{port}}

# Auto-discover and connect via mDNS
adb-connect:
    #!/usr/bin/env bash
    echo "Scanning for devices..."
    DEVICE=$(adb mdns services 2>/dev/null | grep "_adb-tls-connect" | head -1)
    if [ -z "$DEVICE" ]; then
        echo "No devices found. Is Wireless Debugging enabled?"
        exit 1
    fi
    ADDR=$(echo "$DEVICE" | awk '{print $NF}')
    echo "Found: $ADDR"
    adb connect "$ADDR"
    adb devices

# Show connected devices
adb-status:
    @adb devices

# Restart ADB server
adb-restart:
    adb kill-server
    adb start-server
    @adb devices
