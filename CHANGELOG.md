# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [Unreleased]

## [2.3.0]

### Changed

- [*] Shot History, Analysis and Shot Details rebuilt around the shot chart: every shot on one ruled line, recent shots plotted against the 25–30 s target, and a verdict that names the next grind step.
- [*] Beans list rebuilt as a ledger: which bag is in use, its dial-in status and roast age at a glance; a single tap selects it for the next shot.
- [*] More is now an equipment ledger: grinder range and basket weights on the rows, one tap to edit; About is a clean colophon.
- Unified bean freshness behind a single 4–21 day window, showing roast age as a colour-coded figure (resting gold, fresh green, past-peak ink).
- Add and Edit Bean forms restructured around ruled sections (details, photo, status) with a live roast freshness verdict and non-destructive delete.
- Grinder and Basket settings rebuilt as ruled sections: presets as a square segmented switch with a true selected state, validation as a plain line instead of a tinted card; the onboarding equipment steps inherit the new forms.
- Fresh new look in light and dark mode.
- New shot timer dial with the 25-30 s target always visible.
- Record Shot is laid out in brewing order, with grind suggestions next to your current setting.
- New app icon and splash screen.
- Onboarding restyled on the same ruled sections, opening on the shot chart.
- Taste feedback uses the chart's colour swatches instead of emoji; the suggested taste is marked with a border and caption. The history filter, taste editor and grind suggestion on Record Shot follow the same flat style.

### Fixed

- Shot History and Shot Details now show the same quality score for a shot.
- Shot position ("shot N of M") in Shot Details was wrong; ranking and neighbouring shots are now correct and tappable.
- Back from Record Shot no longer returns into setup when onboarding resumed part-way.
- A failed shot save now explains why instead of doing nothing.
- On the Beans list, the shot count no longer wraps next to a bag photo; the German manual-timer hint fits inside the dial.

## [2.2.0]

### Added

- [*] Redesigned Bean Management screen with photo-prominent bean cards, smart sorting, and live status indicators (active, low, finished).
- [*] Filter your shot history by bean to focus on one coffee at a time.
- Last-used date shown for each bean so you can see what you've been brewing lately.

### Changed

- Streamlined grind-setting management for a faster, less error-prone bean setup flow.

### Fixed

- Selected bean now reliably reloads on the Record Shot screen after navigation.
- Restored missing German translation for search.

## [2.1.0]

### Added

- [*] Visual shot analysis dashboard with quality score gauge, performance trends chart, and statistics cards to track your brewing progress.
- [*] Shot History now shows bean-specific coaching insights and celebrates achievements like your first perfect shot, dial-in milestones, and consistency streaks.

### Changed

- [*] Redesigned Shot Details screen with conversational insights, quality score breakdown, and shot ranking.

### Fixed

- [ ] Removed empty box in Shot Recorded Dialog.

## [2.0.0]

### Added

- [*] Manually adjust extraction time for accurate shot records.
- Detekt static analysis tool for improved code quality and consistency.

### Changed

- [*] Redesigned RecordShotScreen with intelligence-first layout focusing on the timer.
- [*] Implemented new warm color palette for light and dark modes.
- Compact single-line weights display ("18g → 36g (1:2.0)") with quick +/- adjustments.
- RecordShotScreen layout optimized for no scrolling with flexible timer size.
- Removed unused code throughout the codebase to reduce app size.

## [1.5.0]

### Added

- [*] Configurable grinder step sizes to match your specific grinder's precision.
- [*] Track how your shots taste to build a complete picture of your brewing performance.
- [*] Get personalized grind adjustment recommendations based on taste and extraction time after each shot.
- Persistent next-shot guidance that remembers your last recommendation for each bean, displayed prominently when switching between beans.

### Changed

- Improved onboarding flow to guide existing users through equipment setup and bean configuration when new features are available.
- [*] Bean selection now persists across app restarts and navigation, ensuring your chosen bean remains selected when switching between screens.

### Fixed

- Fixed coffee input weight slider maximum limit for double baskets.
- Fixed "Use for Shot" button not reliably selecting the bean when navigating from Bean Management to Record Shot screen.

## [1.4.1]

### Fixed

- Database migration issues.

## [1.4.0]

### Added

- Enhanced onboarding experience with guided bean creation for first-time users.
- Basket size configuration to customize weight ranges based on your portafilter basket size.

## [1.3.2]

### Added

- Adaptive landscape orientation with a navigation rail and responsive layouts across all screens.

## [1.3.1]

### Fixed

- Improved layout responsiveness and text wrapping across multiple screens for better display on various screen sizes.

## [1.3.0]

### Added

- App introduction and beginner-friendly onboarding for a smoother first run.
- Grinder scale setup to tailor your equipment.
- About screen with project information.
- Feedback button to share your thoughts.

### Changed

- Navigation reworked to accommodate additional app settings.
- Expanded Coffee In slider range to support single-shot baskets (min lowered to 5g).

### Fixed

- Inactive beans now appear in shot history.

## [1.2.1]

### Fixed

- Database migration issues.

## [1.2.0]

### Added

- Attach a photo of the bean package to a bean for easier re-purchasing.

## [1.1.1]

### Fixed

- IME padding on the notes input.

### Changed

- More consistent shot analysis and a dialog shown after recording a new shot.

## [1.1.0]

### Fixed

- Shot history screen not refreshing automatically.
- Settings not persisting after recording a new shot.

### Changed

- UI consistency improvements.

## [1.0.1]

### Added

- Privacy policy linked in Google Play.

### Changed

- Performance improvements.

## [1.0.0]

### Added

- Initial release: record shots, view shot details, and configure multiple beans.
