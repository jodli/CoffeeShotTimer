# Coffee Shot Timer - AI Coding Assistant Instructions

## Architecture Overview

This is an **Android espresso tracking app** using **Clean Architecture** with MVVM pattern and Jetpack Compose UI. The app tracks coffee beans, shot timing, and brewing analytics for home baristas.

### Core Layers
- **Data Layer**: Room database with DAOs, repositories, and data models
- **Domain Layer**: Use cases with business logic, data transformation, and validation
- **UI Layer**: Compose screens, ViewModels, and navigation

### Key Package Structure
```
com.jodli.coffeeshottimer/
├── data/           # Room entities, DAOs, repositories, converters
├── domain/usecase/ # Business logic use cases (GetShotHistory, RecordShot, etc.)
├── di/            # Hilt dependency injection modules
└── ui/            # Compose screens, ViewModels, navigation, validation
```

## Critical Patterns & Conventions

### Database Architecture
- **Room with performance indexes**: All entities have comprehensive indexes in `AppDatabase.kt`
- **Foreign key constraints**: Enabled via `PRAGMA foreign_keys=ON`
- **Schema versioning**: Uses `exportSchema = true` with schemas stored in `app/schemas/`
- **Migration strategy**: Template in `AppDatabase.getAllMigrations()`

### Dependency Injection (Hilt)
- **Module organization**: `DatabaseModule` (Room), `RepositoryModule` (repos + utilities)
- **Scoping**: `@Singleton` for databases/repos, standard scope for ViewModels
- **Application class**: `@HiltAndroidApp` annotation on `CoffeeShotTimerApplication`

### Data Models & Validation
- **Entities have built-in validation**: `Bean.validate()` returns `ValidationResult(isValid, errors)`
- **Business rules embedded**: `Bean.isFresh()` checks 4-14 day optimal window
- **Unique constraints**: Bean names must be unique via Room index
- **UUID primary keys**: All entities use String UUIDs, not auto-increment

### UI State Management
- **StateFlow patterns**: ViewModels expose UI state via `StateFlow<XxxUiState>`
- **Validation integration**: `ValidatedViewModel` base class with `isFormValid`, `validationErrors`
- **Data classes for UI state**: Each screen has dedicated `UiState` data class

### Testing Strategy
- **ViewModel unit tests**: Located in `temp_tests/` (MockK + Coroutines Test)
- **Test configuration**: Robolectric with multiple SDK versions in `build.gradle.kts`
- **Memory optimization**: Test JVM args configured for large heap usage

## Essential Workflows

### Build & Release
- **Windows release**: Use `build-release.bat` (includes keystore validation, tests, APK+AAB generation)
- **Signing**: Requires `keystore.properties` file (template available)
- **Build flavors**: `debug` (with suffix) and `release` (minified, signed)

### Database Migrations
1. Update `@Database` version in `AppDatabase.kt`
2. Add migration to `getAllMigrations()` array
3. Update schema exports in `app/schemas/`

### Adding New Screens
1. Create screen in `ui/screens/XxxScreen.kt`
2. Add destination to `NavigationDestinations.kt`
3. Wire navigation in `AppNavigation.kt`
4. Add ViewModel in `ui/viewmodel/` with `@HiltViewModel`
5. Update bottom navigation visibility in `MainActivity.kt` if needed

## Project-Specific Technical Details

### Performance Considerations
- **Memory optimization**: `MemoryOptimizer` utility provided via DI
- **Database indexes**: Composite indexes for complex queries (shots by bean+timestamp)
- **Pagination**: `PaginatedResult` data class for large datasets

### Key Dependencies
- **Room 2.6.1**: Database with KTX extensions
- **Hilt 2.48**: Dependency injection
- **Compose Navigation**: Type-safe navigation with destinations
- **Material3**: UI components with extended icons
- **Kotlinx Serialization**: For data persistence

### Validation System
- **Enhanced validation**: `EnhancedValidationIntegration.kt` provides form state management
- **Real-time feedback**: `ValidationExtensions.kt` for Compose integration
- **Business rule validation**: Embedded in data models (date ranges, string lengths)

### App-Specific Business Logic
- **Shot timing**: Millisecond precision tracking
- **Brew ratios**: Auto-calculated from input/output weights
- **Bean freshness**: 4-14 day window for optimal espresso
- **Grinder settings**: Stored per bean for consistency

## Common Integration Points

### Adding New Use Cases
1. Create in `domain/usecase/XxxUseCase.kt`
2. Inject required repositories via constructor
3. Follow existing patterns (suspend functions, Result/Flow returns)
4. Add to ViewModel via Hilt injection

### Database Schema Changes
- Always increment version in `@Database`
- Add migration in `getAllMigrations()`
- Update indexes in `DatabaseCallback.createIndexes()`
- Test migration with existing data

### Accessibility
- `AccessibilityHelper.kt` provides consistent semantics
- All interactive elements need content descriptions
- Validation errors announced to screen readers
