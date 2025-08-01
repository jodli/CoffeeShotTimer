# Project Structure

## Root Directory Layout
```
├── app/                    # Main Android application module
├── docs/                   # Documentation, screenshots, and assets
├── gradle/                 # Gradle configuration and wrapper
├── .github/                # GitHub workflows (CI/CD)
├── .kiro/                  # Kiro AI assistant configuration
└── build files            # Gradle build scripts and properties
```

## Application Module (`app/`)
```
app/
├── src/
│   ├── main/              # Main source code
│   │   ├── java/          # Kotlin source files
│   │   ├── res/           # Android resources
│   │   └── AndroidManifest.xml
│   ├── test/              # Unit tests
│   └── androidTest/       # Instrumentation tests
├── schemas/               # Room database schemas
├── release/               # Pre-built release artifacts
└── build.gradle.kts       # Module build configuration
```

## Key Configuration Files
- `build.gradle.kts` - Root project build configuration
- `app/build.gradle.kts` - App module build configuration with flavors and signing
- `gradle/libs.versions.toml` - Centralized dependency version management
- `settings.gradle.kts` - Project settings and module inclusion
- `keystore.properties` - Local signing configuration (not in VCS)

## Package Structure Conventions
- Use `com.jodli.coffeeshottimer` as base package
- Follow standard Android architecture patterns with clear separation:
  - UI layer (Compose screens and components)
  - Domain layer (use cases and business logic)
  - Data layer (repositories, Room entities, DAOs)
- Organize by feature when possible, not by layer

## Resource Organization
- `res/values/` - Strings, colors, dimensions, styles
- `res/drawable/` - Vector drawables and images
- `res/mipmap/` - App icons for different densities
- Use meaningful resource names with consistent prefixes

## Testing Structure
- Unit tests in `src/test/` using JUnit, MockK, and Robolectric
- Integration tests in `src/androidTest/` using Espresso
- Test naming convention: `MethodName_StateUnderTest_ExpectedBehavior`