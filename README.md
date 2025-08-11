# Coffee Shot Timer

<p align="center">
  <img src="docs/icon.png" alt="Coffee Shot Timer Icon" width="128" height="128">
</p>

<p align="center">
  <strong>Espresso timer with analytics for home baristas</strong>
</p>

## Overview

Coffee Shot Timer is a precision Android app designed specifically for espresso machine enthusiasts who want to systematically improve their brewing skills. The app combines intelligent timing with detailed data capture to help you achieve consistent, dialed-in espresso extractions.

## Features

- Precision timer with clear visual feedback
- Brew ratio tracking with intuitive sliders; supports single-shot baskets (down to 5 g)
- Shot logging and analysis: times, grinder settings, tasting notes, and trends over time
- Bean management with roast dates, freshness indicators, and attachable package photos
- Grinder setup tailored to your equipment; record and compare grinder settings
- Simple onboarding and streamlined navigation; About screen and in-app feedback
- Built for daily use: 100% offline, autosave drafts, and a fast, stress-free UI

## Screenshots

<p align="center">
  <img src="docs/main_screen.png" alt="Main Screen" width="200">
  <img src="docs/light_mode.png" alt="Light Mode" width="200">
  <img src="docs/shot_history.png" alt="Shot History" width="200">
  <img src="docs/shot_details.png" alt="Shot Details" width="200">
</p>

<p align="center">
  <img src="docs/shot_analysis.png" alt="Shot Analysis" width="200">
  <img src="docs/filter_shots.png" alt="Filter Shots" width="200">
  <img src="docs/bean_management.png" alt="Bean Management" width="200">
  <img src="docs/grinder_settings.png" alt="Grinder Settings" width="200">
  <img src="docs/bean_photo.png" alt="Bean Package Photo" width="200">
</p>

## Perfect For

- ☕ Espresso machine or manual espresso maker owners
- 🎯 Coffee enthusiasts wanting reproducible results
- 📚 Systematic learners who like data-driven improvement
- 🔧 Baristas looking to refine their technique
- 🏠 Home coffee enthusiasts serious about quality

## Technology Stack

This is an Android application built with:
- **Kotlin** — primary programming language
- **Android Gradle Plugin** — build system
- **Hilt** — dependency injection
- **Room Database** — local data storage

## Getting Started

### Prerequisites
- Android Studio
- Android SDK
- Gradle

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/jodli/CoffeeShotTimer.git
cd CoffeeShotTimer
```

2. Open the project in Android Studio.

3. Build the project:
```bash
./gradlew build
```

4. Run on a device or emulator:
```bash
./gradlew installDevDebug
```

### Release Build

The project includes configuration for release builds. A pre-built release is available in `app/release/app-release.aab`.

## Project Structure

```
app/src/main/           # Main application source code
app/src/test/           # Unit tests
app/src/androidTest/    # Instrumentation tests
docs/                   # Documentation and screenshots
gradle/                 # Gradle configuration
```

## Contributing

Contributions are welcome! Please feel free to submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the terms found in the [LICENSE](LICENSE) file.

## Philosophy

Coffee Shot Timer transforms espresso making from guesswork into precise craftsmanship—without any stress. The app supports you in achieving consistent espresso quality and developing your personal taste preferences through a structured, data-driven approach.

---

<p align="center">
  Made with ☕ for the home barista community
</p>
