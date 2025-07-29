---
inclusion: always
---

# Coffee Shot Timer - Product Guidelines

Coffee Shot Timer is a precision Android app for espresso enthusiasts focused on systematic brewing improvement through intelligent timing and detailed data capture.

## Core Product Principles

### User Experience Priorities
- **Touch-first design**: All interactive elements must be large enough for wet/coffee-stained fingers during active brewing
- **Immediate feedback**: Visual and haptic responses for all user actions, especially timer controls
- **Zero data loss**: Auto-save all inputs as drafts - users should never lose work due to interruptions
- **Offline-first**: All functionality must work without internet connectivity

### Domain-Specific Rules
- **Bean-centric data model**: All shots, settings, and analysis are tied to specific coffee beans - changing beans resets the optimization process
- **Precision timing**: Timer accuracy is critical - use appropriate Android timing APIs and handle lifecycle correctly
- **Brewing workflow optimization**: UI flow should match real espresso brewing sequence (bean selection → grind → dose → extract → record)

## Feature Implementation Guidelines

### Timer Component
- Large, prominent display with color-coded feedback (green for optimal 25-30s extraction)
- Touch targets minimum 48dp for start/stop/reset actions
- Maintain timing accuracy during screen rotation and app backgrounding
- Visual progress indicators for extraction phases

### Data Entry Patterns
- Quick numeric input for weights (coffee in, espresso out) with decimal precision
- Dropdown/picker patterns for grinder settings and bean selection
- Optional fields should have clear "skip" affordances
- Validate brew ratios and provide feedback (typical 1:2 to 1:3 ratios)

### Bean Management
- Roast date tracking with freshness indicators (peak flavor typically 7-21 days post-roast)
- Bean switching should prompt user about starting fresh optimization cycle
- Support for multiple active beans with clear visual distinction

### Analysis & History
- Focus on trends over time rather than individual shot perfection
- Highlight patterns that lead to better extractions
- Group analysis by bean type for meaningful comparisons

## UI/UX Conventions
- Use Material 3 design system with coffee-appropriate color palette
- Prioritize readability in various lighting conditions (kitchen environments)
- Implement proper accessibility support for vision-impaired users
- Handle interruptions gracefully (phone calls, notifications during brewing)

## Development Considerations
- Implement proper state management for timer operations across app lifecycle
- Use Room database relationships to enforce bean-centric data integrity
- Consider battery optimization - timer should work reliably even with aggressive power management
- Test thoroughly on various screen sizes and orientations common in kitchen use