`

# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.
``

Repository: CoffeeShotTimer (Android, Kotlin, Gradle)

Commands you’ll use often

- Build
  - From user rules (preferred): gradle buildDevDebug

- Lint
  - From user rules (preferred): gradle lintDevDebug

- Tests (unit)
  - From user rules (preferred): gradle testDevDebug
  - Run a single unit test class: gradle :app:testDevDebugUnitTest --tests "com.jodli.coffeeshottimer.data.dao.BeanDaoTest"
  - Run a single unit test method: gradle :app:testDevDebugUnitTest --tests "com.jodli.coffeeshottimer.data.dao.BeanDaoTest.methodName"

- Tests (instrumentation/UI)
  - Connected Android tests (devDebug): gradle :app:connectedDevDebugAndroidTest
  - Run a single instrumented test class: gradle :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.jodli.coffeeshottimer.EndToEndTest
  - Filter by package: gradle :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.jodli.coffeeshottimer

- Install on device/emulator
  - Dev debug: gradle :app:installDevDebug

- Release artifacts
  - Assemble APK (prod): .\gradlew :app:assembleProdRelease
  - Build App Bundle (AAB, prod): .\gradlew :app:bundleProdRelease
  - Note: app/build.gradle.kts renames outputs to coffee-shot-timer[-dev]-<version>.apk / .aab automatically.

- Security checks (GitHub Actions workflows)
  - Check for security findings: zizmor .
  - Apply automatic security fixes: zizmor . --fix=all
  - ALWAYS run zizmor when modifying .github/workflows/ files to ensure security best practices

Build/variant notes

- Flavors: dev, prod. Build types: debug, release. Common variants: devDebug, prodDebug, devRelease, prodRelease.
- Lint/build/test commands above prefer devDebug to iterate quickly.

High-level architecture (big picture)
This app follows a Clean Architecture/MVVM structure with explicit separation of UI, Domain, and Data layers, using Hilt for DI, Room for persistence, and Jetpack Compose for UI.

- UI layer (Compose + Navigation)
  - Entry: app/src/main/java/.../MainActivity.kt sets up theme, splash, and calls EspressoShotTrackerApp().
  - Navigation: AppNavigation.kt and NavigationDestinations.kt define the main graph and modal routes.
    - Bottom navigation destinations: RecordShot, ShotHistory, BeanManagement, More.
    - Onboarding flow: Introduction → EquipmentSetup; start destination is chosen at runtime by a RoutingState from MainActivityViewModel.
    - Layout adapts to orientation: portrait uses BottomNavigationBar; landscape uses a NavigationRail.
  - Screens and components live under ui/screens and ui/components. ViewModels expose screen state via observable flows and integrate validation utilities.

- Domain layer (business logic)
  - Use cases in domain/usecase (e.g., RecordShotUseCase, GetShotHistoryUseCase, Add/UpdateBeanUseCase, etc.).
  - Domain models and error codes under domain/model and domain/exception.
  - Validation integrated into flows and UI helpers for real-time feedback during form entry.

- Data layer (Room + repositories)
  - Database: AppDatabase (version 3) with entities Bean, Shot, GrinderConfiguration; TypeConverters for time/date types.
  - DAOs: BeanDao, ShotDao, GrinderConfigDao implement queries, filtering, pagination-friendly access patterns.
  - Repositories: BeanRepository, ShotRepository, GrinderConfigRepository encapsulate persistence and business rules (e.g., bean validation when recording shots).
  - Migrations: AppDatabase.getAllMigrations() includes 1→2 and 2→3 with index normalization and new tables. DatabaseModule wires migrations and enforces PRAGMA foreign_keys=ON.
  - Performance: indices are created for frequent query paths (beanId, timestamp, grinderSetting; bean name/active/roastDate/createdAt). A composite index on (beanId, timestamp) supports history queries.

- Dependency Injection (Hilt)
  - Application class annotated with @HiltAndroidApp: CoffeeShotTimerApplication.
  - Modules:
    - DatabaseModule: provides Room database, DAOs, migration and index callbacks, and a debug-only DatabasePopulator.
    - RepositoryModule: provides repositories, MemoryOptimizer, PerformanceMonitor, and binds PhotoStorageManager/PhotoCaptureManager implementations to interfaces.

- Media/photo handling
  - PhotoCaptureManager and PhotoStorageManager abstractions wrap capture and storage of bean package photos. AndroidManifest configures a FileProvider for secure sharing and declares camera permissions.

- Testing strategy
  - Unit tests: app/src/test cover DAOs, repositories, models, use cases, and ViewModels (MockK, coroutines-test, Robolectric). Robolectric is configured in app/build.gradle.kts with multiple SDKs and increased heap/metaspace for stability.
  - Instrumentation/UI tests: app/src/androidTest include Compose UI tests and Hilt test rules (e.g., EndToEndTest, navigation and accessibility tests). Use connectedDevDebugAndroidTest to run them on an attached device/emulator.

- Build configuration highlights
  - Kotlin DSL, AGP 8.12, Kotlin 2.2, Java 11 target, Compose enabled, core library desugaring on.
  - Product flavors: dev/prod; resource locale filters (en, de); vector drawables support lib; output renaming for APK/AAB by flavor.
  - KSP configured for Room with schema export to app/schemas via ksp { arg("room.schemaLocation", "$projectDir/schemas") }.

Key conventions and rules (from project docs and AI assistant instructions)

- Clean Architecture with clear UI/Domain/Data separation; favor use cases for business logic wiring.
- Room setup uses exportSchema=true with versioned migrations; indices and foreign keys are enforced (foreign_keys=ON).
- Unique constraints (e.g., bean name) and validation helpers surface structured ValidationResult to the UI.
- Hilt modules are the single source of truth for wiring repositories, DAOs, and utilities; repositories are @Singleton.
- State flows from ViewModels drive UI state; compose screens react to validation and navigation events.
