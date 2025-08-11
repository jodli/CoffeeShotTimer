# Technology Stack

## Core Technologies
- **Language**: Kotlin 2.2.0 (primary)
- **Platform**: Android (minSdk 24, targetSdk 36, compileSdk 36)
- **Build System**: Gradle 8.12.0 with Kotlin DSL
- **UI Framework**: Jetpack Compose with Material 3 (BOM 2025.07.00)
- **Architecture**: MVVM with Hilt dependency injection

## Key Libraries & Frameworks
- **Database**: Room 2.7.2 (local SQLite with coroutines support)
- **Dependency Injection**: Hilt 2.57 (Dagger-based)
- **Navigation**: Navigation Compose 2.9.3
- **Lifecycle**: ViewModel and LiveData with Compose integration
- **Serialization**: Kotlinx Serialization JSON 1.9.0
- **Image Loading**: Coil 2.7.0 for Compose
- **Testing**: JUnit, MockK 1.14.5, Robolectric 4.15.1, Espresso

## Build Configuration
- **Java Version**: 11 (source and target compatibility)
- **Build Variants**: devDebug, prodDebug, devRelease, prodRelease
- **Signing**: Supports both local keystore and CI/CD environment variables
- **Code Processing**: KSP 2.2.0-2.0.2 (replaces KAPT for Room and Hilt)
- **Localization**: English and German support
- **Core Library Desugaring**: Enabled for Java 8+ APIs on older Android versions

## Common Commands
```bash
# Always set the JAVA_HOME env var when in a new terminal session.
# ONLY NECESSARY WHEN ON WINDOWS
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Development ON LINUX (current)
```bash
# Quickly build the project for development
gradle buildDevDebug

# Build the project with all targets
gradle build

# Install debug build (choose flavor)
gradle installDevDebug      # Development flavor
gradle installProdDebug     # Production flavor

# Quickly build the project for development
gradle testDevDebug

# Run tests with all targets
gradle test

# Run instrumentation tests
gradle connectedAndroidTest

# Clean build
gradle clean
```

### Development ON WINDOWS
```bash
# Quickly build the project for development
./gradlew buildDevDebug

# Build the project with all targets
./gradlew build

# Install debug build (choose flavor)
./gradlew installDevDebug      # Development flavor
./gradlew installProdDebug     # Production flavor

# Quickly build the project for development
./gradlew testDevDebug

# Run tests with all targets
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

### Product Flavors
The app has two product flavors:
- **dev**: Development flavor with `.dev.debug` app ID suffix
- **prod**: Production flavor with standard app ID

Available build variants:
- `devDebug` - Development debug build
- `devRelease` - Development release build  
- `prodDebug` - Production debug build
- `prodRelease` - Production release build

### Release
```bash
# Build release APK
./gradlew assembleRelease

# Build release AAB (App Bundle)
./gradlew bundleRelease
```

## Development Setup Requirements
- Android Studio
- Android SDK
- Gradle (wrapper included)
- Java 11 or higher