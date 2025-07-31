# Technology Stack

## Core Technologies
- **Language**: Kotlin (primary)
- **Platform**: Android (minSdk 24, targetSdk 36, compileSdk 36)
- **Build System**: Gradle with Kotlin DSL
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Hilt dependency injection

## Key Libraries & Frameworks
- **Database**: Room (local SQLite with coroutines support)
- **Dependency Injection**: Hilt (Dagger-based)
- **Navigation**: Navigation Compose
- **Lifecycle**: ViewModel and LiveData with Compose integration
- **Serialization**: Kotlinx Serialization JSON
- **Testing**: JUnit, MockK, Robolectric, Espresso

## Build Configuration
- **Java Version**: 11 (source and target compatibility)
- **Build Variants**: devDebug, prodDebug, devRelease, prodRelease
- **Signing**: Supports both local keystore and CI/CD environment variables

## Common Commands
```bash
# Always set the JAVA_HOME env var when in a new terminal session.
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Development
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