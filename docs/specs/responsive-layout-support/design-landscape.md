# Design Document - Landscape Support

## Overview

This design document outlines the implementation of landscape orientation support for the Coffee Shot Timer app. The solution will provide adaptive UI components that respond to landscape orientation while **extending and preserving the existing UI/UX consistency guidelines** and coffee-focused design language, ensuring timer accuracy across orientation changes.

## Design System Compatibility

This landscape design **extends the existing UI/UX consistency guidelines** established in the Coffee Shot Timer app:

### Preserving Existing Design System Foundation
- **Spacing System**: Extends `LocalSpacing` with landscape-specific helper functions
- **Color Scheme**: Maintains existing coffee-inspired Material 3 colors (WarmCaramel, SoftTeal, CreamyBeige)
- **Component Standards**: Builds upon existing `CoffeeCard`, `CoffeePrimaryButton`, `CardHeader` components
- **Typography Hierarchy**: Preserves existing typography scale and coffee-specific styling

### Extending Component Standards for Landscape
- **Cards**: Enhances existing `CoffeeCard` with landscape-optimized layouts while maintaining styling
- **Buttons**: Preserves `CoffeePrimaryButton`/`CoffeeSecondaryButton` with maintained 44.dp touch targets
- **Timer Components**: Adapts existing clickable timer for landscape with appropriate sizing
- **Form Layouts**: Reorganizes existing form components for horizontal space utilization

## Architecture

### Landscape Configuration Detection

**Orientation Detection**
- Use `Configuration.orientation` to detect landscape mode
- Implement `LocalIsLandscape` composition local for reactive orientation changes
- Create landscape-specific layout variants for key screens

**Landscape Layout System**
- Extend existing `LocalSpacing` system with landscape values
- Create `LandscapeContainer` composable for landscape-specific layouts
- Use `BoxWithConstraints` for width-based responsive behavior

### State Preservation Strategy

**Configuration Change Handling**
- Remove `android:screenOrientation="portrait"` from AndroidManifest.xml
- Implement proper `ViewModel` state preservation for timer operations
- Use `rememberSaveable` for UI state that needs to survive orientation changes
- Ensure `Room` database operations are unaffected by orientation changes

**Timer State Management**
- Enhance `ShotRecordingViewModel` to handle orientation changes gracefully
- Implement timer state serialization for process death scenarios
- Use `SavedStateHandle` for critical timer data preservation
- Maintain timer accuracy through `SystemClock.elapsedRealtime()` timestamps

## Components and Interfaces

### Landscape Layout Components

**LandscapeContainer**
```kotlin
@Composable
fun LandscapeContainer(
    modifier: Modifier = Modifier,
    portraitContent: @Composable () -> Unit,
    landscapeContent: @Composable (() -> Unit)? = null
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape && landscapeContent != null) {
        landscapeContent()
    } else {
        portraitContent()
    }
}
```

**LandscapeRow (Extending Existing Layout Patterns)**
```kotlin
@Composable
fun LandscapeRow(
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Start,
    alignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = arrangement,
        verticalAlignment = alignment,
        content = content
    )
}
```

### Screen-Specific Landscape Layouts

**RecordShotScreen Landscape Design (Extending Existing Structure)**
- **Portrait**: Keep existing vertical `Column` layout with `CoffeeCard` components
- **Landscape**: Horizontal `Row` with:
  - Left side: Timer `CoffeeCard` with compact 160.dp timer
  - Right side: Scrollable `Column` with form `CoffeeCard`s (bean selection, weights, grinder, notes)
  - Maintain existing `CoffeeCard` styling and interactions

**ShotHistoryScreen Landscape Design (Extending Existing Components)**
- **Portrait**: Keep existing `LazyColumn` with `CoffeeCard` shot items
- **Landscape**: Same `LazyColumn` structure but:
  - Wider `CoffeeCard`s that utilize horizontal space
  - Horizontal `MetricChip` layout within cards
  - More shot information visible per card
  - Maintain existing shot item interactions

**BeanManagementScreen Landscape Design (Extending Existing Bean Cards)**
- **Portrait**: Keep existing `LazyColumn` with `BeanListItem` `CoffeeCard` components
- **Landscape**: Two-column grid using `LazyVerticalGrid`:
  - Use existing `BeanListItem` components
  - Maintain existing `CoffeeCard` styling
  - Preserve all existing interactions (edit, delete, photo viewing)

## Data Models

### Landscape Configuration Models

**LandscapeConfiguration**
```kotlin
data class LandscapeConfiguration(
    val isLandscape: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val useTwoColumnLayout: Boolean,
    val timerSize: Dp,
    val contentMaxWidth: Dp
)
```

**Landscape Spacing Extensions**
```kotlin
// Extend existing Spacing data class with landscape values
data class Spacing(
    // Existing values preserved
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val touchTarget: Dp = 44.dp,
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    val timerSize: Dp = 200.dp,
    // ... existing values ...
    
    // New landscape extensions
    val landscapeTimerSize: Dp = 160.dp,
    val landscapeContentSpacing: Dp = 20.dp
)

// Helper functions for landscape spacing
@Composable
fun Spacing.landscapeTimerSize(): Dp {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (isLandscape) landscapeTimerSize else timerSize
}

@Composable
fun Spacing.landscapeSpacing(): Dp {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return if (isLandscape) landscapeContentSpacing else medium
}
```

## Enhanced Component Adaptations

**Extending Existing CoffeeCard Component**
```kotlin
@Composable
fun CoffeeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Use existing CoffeeCard implementation
    // No changes needed - existing card works well in landscape
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(12.dp), // Existing styling
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Existing elevation
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding),
            content = content
        )
    }
}
```

**Extending Existing Timer Components**
```kotlin
@Composable
fun TimerControls(
    isRunning: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    currentTime: Long,
    targetTime: Long?,
    showReset: Boolean = true,
    useClickableTimer: Boolean = true,
    showColorCoding: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    // Use landscape-aware timer sizing
    val timerSize = spacing.landscapeTimerSize()
    
    // Use existing TimerControls implementation with responsive sizing
    // Maintain existing clickable timer functionality and haptic feedback
}
```

## Error Handling

### Landscape-Specific Error Scenarios

**Timer Accuracy Preservation**
- Handle system clock changes during orientation transitions
- Implement fallback timing mechanisms for edge cases
- Validate timer state consistency after orientation changes
- Provide user feedback if timer accuracy is compromised

**Layout Transition Errors**
- Graceful fallback to portrait layout if landscape fails
- Error boundaries for landscape component failures
- Logging and monitoring of landscape layout issues
- User notification for critical layout failures

**State Restoration Failures**
- Implement progressive state restoration (critical data first)
- Fallback to default state if restoration fails
- User notification of data loss scenarios
- Automatic retry mechanisms for transient failures

## Testing Strategy

### Unit Testing

**Landscape Detection Tests**
```kotlin
class LandscapeConfigurationTest {
    @Test
    fun `detects landscape orientation correctly`()
    
    @Test
    fun `handles orientation changes properly`()
    
    @Test
    fun `preserves timer state during rotation`()
}
```

**Landscape Layout Tests**
```kotlin
class LandscapeLayoutTest {
    @Test
    fun `LandscapeContainer shows correct layout for landscape`()
    
    @Test
    fun `RecordShotScreen adapts to landscape properly`()
    
    @Test
    fun `touch targets meet minimum size requirements in landscape`()
}
```

### Integration Testing

**Screen Rotation Tests**
- Automated rotation testing for all major screens
- Timer accuracy validation across rotations
- Form data preservation verification
- Navigation state consistency checks

### UI Testing

**Landscape Layout UI Tests**
```kotlin
@Test
fun testRecordShotScreenLandscapeLayout() {
    // Rotate to landscape
    // Verify horizontal layout is displayed
    // Verify timer and controls are properly positioned
    // Verify all touch targets are accessible
}

@Test
fun testLandscapeAccessibility() {
    // Verify touch target sizes in landscape
    // Verify screen reader compatibility
    // Verify keyboard navigation
}
```

## Implementation Phases

### Phase 1: Foundation (Landscape Infrastructure)
1. **AndroidManifest Updates**
   - Remove portrait orientation lock
   - Add configuration change handling
   - Test basic orientation changes

2. **Basic Landscape Detection**
   - Implement landscape configuration detection
   - Create `LocalIsLandscape` composition local
   - Add landscape spacing extensions
   - Test configuration change handling

3. **Timer State Preservation**
   - Enhance `ShotRecordingViewModel` for orientation changes
   - Implement proper state serialization
   - Test timer accuracy across rotations
   - Validate form data preservation

### Phase 2: Screen Adaptations (Landscape Layouts)
1. **RecordShotScreen Landscape Layout**
   - Implement horizontal layout with timer on left
   - Create scrollable form section on right
   - Ensure timer state preservation
   - Optimize touch targets for landscape

2. **ShotHistoryScreen Landscape Layout**
   - Adapt existing cards for wider layout
   - Implement horizontal metric chip layout
   - Maintain existing functionality
   - Test landscape navigation

3. **BeanManagementScreen Landscape Layout**
   - Implement two-column grid layout
   - Use existing `BeanListItem` components
   - Preserve all existing interactions
   - Test photo viewing in landscape

### Phase 3: Polish and Testing
1. **Landscape Optimization**
   - Fine-tune spacing and sizing
   - Optimize animations for orientation changes
   - Add landscape-specific performance optimizations
   - Comprehensive testing across devices

2. **Accessibility and Compliance**
   - Verify touch target compliance in landscape
   - Test screen reader compatibility
   - Validate keyboard navigation
   - Performance benchmarking

## Technical Considerations

### Compose-Specific Implementation Details

**Landscape-Aware Composables**
```kotlin
@Composable
fun rememberLandscapeConfiguration(): LandscapeConfiguration {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        LandscapeConfiguration(
            isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            useTwoColumnLayout = configuration.screenWidthDp > 600,
            timerSize = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 160.dp else 200.dp,
            contentMaxWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 
                (configuration.screenWidthDp * 0.6f).dp else Dp.Unspecified
        )
    }
}
```

### Performance Considerations

**Layout Calculation Optimization**
- Cache layout calculations for landscape configurations
- Use `derivedStateOf` for expensive landscape calculations
- Optimize recomposition scope for orientation changes
- Background processing for layout calculations

### Integration with Existing Systems

**Theme System Integration (Preserving Existing Coffee Theme)**
```kotlin
// Extend existing LocalSpacing with landscape awareness
val LocalIsLandscape = staticCompositionLocalOf { false }

@Composable
fun CoffeeShotTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Preserve existing color scheme and theme logic
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Extend existing theme with landscape awareness
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalIsLandscape provides isLandscape
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
```

## Future Enhancements

### Advanced Landscape Features
- Landscape-specific animations and transitions
- Optimized landscape navigation patterns
- Landscape-aware image loading and sizing
- Advanced landscape performance optimizations

### Accessibility Enhancements
- Voice control integration for landscape mode
- Enhanced keyboard navigation for landscape layouts
- Landscape-specific accessibility shortcuts
- Motor accessibility improvements for landscape orientation