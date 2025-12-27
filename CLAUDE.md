# Coffee Shot Timer

Android espresso tracking app - Clean Architecture + MVVM + Jetpack Compose. Tracks beans, shot timing, and brewing analytics.

## Structure
```
com.jodli.coffeeshottimer/
├── data/           # Room entities, DAOs, repositories
├── domain/usecase/ # Business logic use cases
├── di/             # Hilt modules
└── ui/             # Compose screens, ViewModels, navigation
```

## Development Commands

Run `just --list` to see all recipes.

**Build & Test:**
- `just build` - Build debug APK
- `just test` - Run unit tests
- `just check` - Run all checks (detekt + lint + tests)
- `just install` - Install on device

**Device:**
- `just screenshot` - Screenshot from device to clipboard
- `just adb-connect` - Auto-discover and connect via mDNS

## Key Patterns
- **UUID primary keys** (not auto-increment)
- **Room with indexes** in `AppDatabase.kt`
- **StateFlow** for UI state in ViewModels
- **Hilt** for DI (`@HiltViewModel`, `@Singleton`)

## More Details
See `.github/copilot-instructions.md` for detailed architecture, migrations, and patterns.
