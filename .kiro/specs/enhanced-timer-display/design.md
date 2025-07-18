# Design Document

## Overview

The Enhanced Timer Display feature improves the existing circular timer component in the Coffee Shot Timer app by adding intelligent time formatting and color-coded visual feedback. The enhancement focuses on providing immediate visual cues about extraction quality while maintaining the existing timer functionality and integrating seamlessly with the current UI architecture.

## Architecture

### Component Architecture

The enhancement will modify the existing `TimerComponents.kt` file and related UI components without changing the underlying timer logic in the `RecordShotUseCase` or `ShotRecordingViewModel`. The design follows the existing MVVM pattern and Jetpack Compose architecture.

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Enhanced)                      │
├─────────────────────────────────────────────────────────────┤
│ • CircularTimer (Enhanced with color coding)               │
│ • CompactTimer (Enhanced formatting)                        │
│ • TimerComponents (New formatting utilities)                │
│ • RecordShotScreen (Updated timer integration)             │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                 Domain Layer (Unchanged)                    │
├─────────────────────────────────────────────────────────────┤
│ • RecordShotUseCase (Existing timer logic)                 │
│ • TimerState (Existing state management)                   │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

- **TimerComponents.kt**: Enhanced with new formatting and color logic
- **RecordShotScreen.kt**: Updated to use enhanced timer display
- **ShotHistoryScreen.kt**: Updated for consistent time formatting
- **ShotDetailsScreen.kt**: Updated for consistent time formatting
- **Theme.kt**: New color definitions for timer states

## Components and Interfaces

### Enhanced Timer Components

#### 1. Enhanced CircularTimer
```kotlin
@Composable
fun CircularTimer(
    currentTime: Long,
    targetTime: Long?,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showColorCoding: Boolean = true // New parameter for color coding
)
```

**Key Enhancements:**
- Dynamic color calculation based on extraction time
- Smooth color transitions using `animateColorAsState`
- Seconds-only formatting for times under 60 seconds
- Maintains existing functionality for longer times

#### 2. Enhanced CompactTimer
```kotlin
@Composable
fun CompactTimer(
    currentTime: Long,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
    showColorCoding: Boolean = true // New parameter
)
```

**Key Enhancements:**
- Color-coded text based on extraction quality
- Consistent time formatting with main timer
- Maintains compact size for list displays

#### 3. New Utility Functions
```kotlin
// Time formatting utilities
fun formatExtractionTime(timeMs: Long): String
fun getExtractionTimeColor(timeMs: Long, isRunning: Boolean): Color
fun getExtractionQuality(timeMs: Long): ExtractionQuality

enum class ExtractionQuality {
    UNDER_EXTRACTED, // < 20s - Yellow
    OPTIMAL,         // 20-35s - Green  
    OVER_EXTRACTED,  // > 35s - Red
    NEUTRAL          // Not running - Gray
}
```

### Color Scheme Design

#### Timer Progress Colors
- **Under-extraction (< 20s)**: `Color(0xFFFFC107)` - Amber/Yellow
- **Optimal range (20-35s)**: `Color(0xFF4CAF50)` - Green
- **Over-extraction (> 35s)**: `Color(0xFFF44336)` - Red
- **Neutral/Stopped**: `Color.Gray.copy(alpha = 0.3f)` - Light Gray

#### Color Transition Logic
```kotlin
fun getTimerColor(elapsedSeconds: Int, isRunning: Boolean): Color {
    return when {
        !isRunning -> Color.Gray.copy(alpha = 0.3f)
        elapsedSeconds < 20 -> Color(0xFFFFC107) // Yellow
        elapsedSeconds <= 35 -> Color(0xFF4CAF50) // Green
        else -> Color(0xFFF44336) // Red
    }
}
```

### Time Formatting Logic

#### Formatting Rules
```kotlin
fun formatExtractionTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        else -> {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
```

#### Display Examples
- `0s` to `59s` for times under one minute
- `01:00` to `02:30` for longer extractions
- Consistent formatting across all timer displays

## Data Models

### Enhanced Timer State (Extension)

The existing `TimerState` from the domain layer remains unchanged. The UI layer will add computed properties for display:

```kotlin
// Extension functions for UI layer
fun TimerState.getDisplayTime(): String = formatExtractionTime(elapsedTimeSeconds * 1000L)
fun TimerState.getTimerColor(): Color = getTimerColor(elapsedTimeSeconds, isRunning)
fun TimerState.getExtractionQuality(): ExtractionQuality = getExtractionQuality(elapsedTimeSeconds * 1000L)
```

### Color Animation State

```kotlin
@Composable
fun rememberTimerColorState(
    elapsedSeconds: Int,
    isRunning: Boolean
): State<Color> {
    val targetColor = getTimerColor(elapsedSeconds, isRunning)
    return animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "timer_color"
    )
}
```

## Error Handling

### Graceful Degradation

#### Color Coding Failures
- If color animation fails, fall back to static green color
- Maintain timer functionality even if visual enhancements fail
- Log errors for debugging without affecting user experience

#### Time Formatting Edge Cases
- Handle negative time values (display as "0s")
- Handle extremely large values (cap display at reasonable maximum)
- Ensure consistent formatting across different locales

#### Performance Considerations
- Limit color animation frequency to avoid excessive recomposition
- Use `remember` and `derivedStateOf` for expensive calculations
- Optimize color calculations for smooth 60fps animations

## Testing Strategy

### Unit Testing

#### Time Formatting Tests
```kotlin
@Test
fun `formatExtractionTime formats seconds correctly for under 60 seconds`()

@Test
fun `formatExtractionTime formats MM:SS correctly for over 60 seconds`()

@Test
fun `getTimerColor returns correct colors for different time ranges`()
```

#### Color Logic Tests
```kotlin
@Test
fun `timer color is yellow for under-extraction range`()

@Test
fun `timer color is green for optimal range`()

@Test
fun `timer color is red for over-extraction range`()

@Test
fun `timer color is gray when not running`()
```

### Integration Testing

#### UI Component Tests
- Test color transitions during timer progression
- Verify time format changes at 60-second boundary
- Test color coding with different timer states
- Verify accessibility with color coding

#### Visual Regression Tests
- Screenshot tests for different timer states
- Color accuracy verification
- Animation smoothness validation
- Cross-device compatibility testing

### User Acceptance Testing

#### Real-world Usage Scenarios
- Test during actual espresso extraction (20-40 second range)
- Verify color coding helps with timing decisions
- Test readability in different lighting conditions
- Validate that colors are distinguishable for color-blind users

## Implementation Considerations

### Accessibility

#### Color Accessibility
- Ensure sufficient contrast ratios for all timer colors
- Provide alternative indicators beyond just color (text labels)
- Support high contrast mode and accessibility settings
- Test with color blindness simulation tools

#### Screen Reader Support
- Add content descriptions that include extraction quality
- Announce time format changes appropriately
- Provide audio cues for optimal timing ranges

### Performance Optimization

#### Animation Performance
- Use `animateColorAsState` with appropriate duration (500ms)
- Limit recomposition frequency for smooth animations
- Cache color calculations using `remember`
- Optimize Canvas drawing operations

#### Memory Management
- Avoid creating new Color objects on each recomposition
- Use color constants where possible
- Implement efficient color interpolation

### Backward Compatibility

#### Existing Functionality
- Maintain all existing timer features
- Preserve existing API contracts
- Ensure no breaking changes to timer logic
- Support disabling enhancements if needed

#### Migration Strategy
- Gradual rollout of enhanced components
- Feature flags for enabling/disabling enhancements
- Fallback to original timer if issues occur
- User preference for color coding (future enhancement)

### Platform Considerations

#### Android Specific
- Test on different Android versions (API 24+)
- Verify color accuracy across different displays
- Test performance on lower-end devices
- Ensure compatibility with dark/light themes

#### Material Design Compliance
- Use Material Design 3 color system
- Follow Material motion guidelines for animations
- Maintain consistency with app's design language
- Respect system accessibility settings