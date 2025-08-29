# Design Document - Tablet-Optimized Layouts

## Overview

This design document outlines the implementation of tablet-optimized layouts for the Coffee Shot Timer app on devices with sw600dp+ screens. The solution will provide enhanced UI components that take advantage of larger screen real estate while **extending and preserving the existing UI/UX consistency guidelines** and coffee-focused design language.

## Design System Compatibility

This tablet design **extends the existing UI/UX consistency guidelines** established in the Coffee Shot Timer app:

### Preserving Existing Design System Foundation
- **Spacing System**: Extends `LocalSpacing` with tablet-specific enhanced values
- **Color Scheme**: Maintains existing coffee-inspired Material 3 colors (WarmCaramel, SoftTeal, CreamyBeige)
- **Component Standards**: Builds upon existing `CoffeeCard`, `CoffeePrimaryButton`, `CardHeader` components with larger sizing
- **Typography Hierarchy**: Preserves existing typography scale while optimizing for larger screens

### Extending Component Standards for Tablets
- **Cards**: Enhances existing `CoffeeCard` with larger padding and improved spacing for tablet screens
- **Buttons**: Scales `CoffeePrimaryButton`/`CoffeeSecondaryButton` appropriately while maintaining touch target standards
- **Timer Components**: Enlarges existing clickable timer for better visibility on larger screens
- **Grid Layouts**: Introduces multi-column layouts using existing card components

## Architecture

### Tablet Configuration Detection

**Screen Size Detection**
- Use `Configuration.screenWidthDp >= 600` to detect tablet-sized screens
- Implement `LocalIsTablet` composition local for reactive tablet detection
- Create tablet-specific layout variants for key screens
- Support both tablet portrait and tablet landscape orientations

**Tablet Layout System**
- Extend existing `LocalSpacing` system with tablet-enhanced values
- Create `TabletContainer` composable for tablet-specific layouts
- Implement `TabletGrid` for multi-column layouts using existing components
- Use master-detail patterns where appropriate

### Enhanced Spacing for Tablets

**Tablet Spacing Extensions**
```kotlin
// Extend existing Spacing data class with tablet values
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
    
    // New tablet extensions
    val tabletCardPaddingExtra: Dp = 8.dp,
    val tabletScreenPaddingExtra: Dp = 16.dp,
    val tabletTimerSize: Dp = 280.dp,
    val tabletContentMaxWidth: Dp = 1200.dp
)

// Helper functions for tablet spacing
@Composable
fun Spacing.tabletCardPadding(): Dp {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    return if (isTablet) cardPadding + tabletCardPaddingExtra else cardPadding
}

@Composable
fun Spacing.tabletScreenPadding(): Dp {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    return if (isTablet) screenPadding + tabletScreenPaddingExtra else screenPadding
}

@Composable
fun Spacing.tabletTimerSize(): Dp {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    return if (isTablet) tabletTimerSize else timerSize
}
```

## Components and Interfaces

### Tablet Layout Components

**TabletContainer**
```kotlin
@Composable
fun TabletContainer(
    modifier: Modifier = Modifier,
    phoneContent: @Composable () -> Unit,
    tabletContent: @Composable (() -> Unit)? = null
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    if (isTablet && tabletContent != null) {
        tabletContent()
    } else {
        phoneContent()
    }
}
```

**TabletGrid (Following Existing LazyColumn Patterns)**
```kotlin
@Composable
fun TabletGrid(
    columns: Int = 2,
    modifier: Modifier = Modifier,
    content: @Composable LazyGridScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    if (isTablet) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(spacing.tabletScreenPadding()),
            content = content
        )
    } else {
        // Fallback to single column for phones
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(spacing.screenPadding)
        ) {
            // Convert grid content to column content
            content()
        }
    }
}
```

**TabletMasterDetail**
```kotlin
@Composable
fun TabletMasterDetail(
    masterContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    masterWeight: Float = 0.4f,
    detailWeight: Float = 0.6f,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    if (isTablet) {
        Row(
            modifier = modifier.padding(spacing.tabletScreenPadding()),
            horizontalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            Box(modifier = Modifier.weight(masterWeight)) {
                masterContent()
            }
            Box(modifier = Modifier.weight(detailWeight)) {
                detailContent()
            }
        }
    } else {
        // Fallback to single content for phones
        masterContent()
    }
}
```

### Screen-Specific Tablet Layouts

**RecordShotScreen Tablet Design (Extending Existing Structure)**
- **Phone**: Keep existing vertical `Column` layout with `CoffeeCard` components
- **Tablet Portrait**: Same structure with larger `spacing.tabletCardPadding()` and enhanced timer size
- **Tablet Landscape**: Two-column layout:
  - Left column: Timer `CoffeeCard` with 280.dp timer and bean selection
  - Right column: Form `CoffeeCard`s (weights, grinder, notes) with enhanced spacing
  - Maintain existing `CoffeeCard` styling and interactions

**ShotHistoryScreen Tablet Design (Extending Existing Components)**
- **Phone**: Keep existing `LazyColumn` with `CoffeeCard` shot items
- **Tablet Portrait**: Two-column `TabletGrid` using existing shot item `CoffeeCard` components
- **Tablet Landscape**: Master-detail layout:
  - Master: `LazyColumn` with existing shot items (40% width)
  - Detail: Shot details panel using existing `ShotDetailsScreen` components (60% width)
  - Maintain existing shot item interactions and filtering

**BeanManagementScreen Tablet Design (Extending Existing Bean Cards)**
- **Phone**: Keep existing `LazyColumn` with `BeanListItem` `CoffeeCard` components
- **Tablet Portrait**: Three-column `TabletGrid` with existing `BeanListItem` components
- **Tablet Landscape**: Master-detail layout:
  - Master: Two-column grid with existing `BeanListItem` components (50% width)
  - Detail: `AddEditBeanScreen` components for editing (50% width)
  - Preserve all existing interactions (edit, delete, photo viewing)

## Data Models

### Tablet Configuration Models

**TabletConfiguration**
```kotlin
data class TabletConfiguration(
    val isTablet: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean,
    val gridColumns: Int,
    val useMasterDetail: Boolean,
    val timerSize: Dp,
    val contentMaxWidth: Dp
)

@Composable
fun rememberTabletConfiguration(): TabletConfiguration {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val isTablet = configuration.screenWidthDp >= 600
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        
        TabletConfiguration(
            isTablet = isTablet,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            isLandscape = isLandscape,
            gridColumns = when {
                !isTablet -> 1
                isLandscape -> 3
                else -> 2
            },
            useMasterDetail = isTablet && isLandscape,
            timerSize = if (isTablet) 280.dp else 200.dp,
            contentMaxWidth = if (isTablet) 1200.dp else Dp.Unspecified
        )
    }
}
```

## Enhanced Component Adaptations

**Extending Existing CoffeeCard Component for Tablets**
```kotlin
@Composable
fun CoffeeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = LocalSpacing.current
    
    // Use tablet-aware padding
    val adaptivePadding = spacing.tabletCardPadding()
    
    // Use existing CoffeeCard implementation with enhanced padding
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(12.dp), // Existing styling
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Existing elevation
    ) {
        Column(
            modifier = Modifier.padding(adaptivePadding),
            content = content
        )
    }
}
```

**Extending Existing Timer Components for Tablets**
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
    
    // Use tablet-aware timer sizing
    val timerSize = spacing.tabletTimerSize()
    
    // Use existing TimerControls implementation with enhanced sizing
    // Maintain existing clickable timer functionality and haptic feedback
    // Scale reset button appropriately for tablet screens
}
```

**Extending Existing CardHeader Component for Tablets**
```kotlin
@Composable
fun CardHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    // Use enhanced spacing for tablets
    val adaptiveSpacing = if (isTablet) spacing.medium + 4.dp else spacing.medium
    val adaptiveIconSize = if (isTablet) 28.dp else 24.dp
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(adaptiveSpacing)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(adaptiveIconSize)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        actions?.invoke()
    }
}
```

## Master-Detail Patterns

### Shot History Master-Detail

**Implementation Strategy**
- Master panel: Existing `LazyColumn` with shot items (40% width)
- Detail panel: Existing `ShotDetailsScreen` content (60% width)
- Maintain existing shot selection and filtering functionality
- Preserve existing navigation patterns with tablet enhancements

### Bean Management Master-Detail

**Implementation Strategy**
- Master panel: Two-column grid with existing `BeanListItem` components (50% width)
- Detail panel: Existing `AddEditBeanScreen` content (50% width)
- Maintain existing bean editing and photo management functionality
- Preserve existing form validation and state management

## Testing Strategy

### Unit Testing

**Tablet Detection Tests**
```kotlin
class TabletConfigurationTest {
    @Test
    fun `detects tablet screen size correctly`()
    
    @Test
    fun `calculates appropriate grid columns for tablet`()
    
    @Test
    fun `determines master-detail usage correctly`()
}
```

**Tablet Layout Tests**
```kotlin
class TabletLayoutTest {
    @Test
    fun `TabletContainer shows correct layout for tablet`()
    
    @Test
    fun `TabletGrid displays appropriate number of columns`()
    
    @Test
    fun `TabletMasterDetail arranges content correctly`()
    
    @Test
    fun `touch targets meet minimum size requirements on tablets`()
}
```

### Integration Testing

**Multi-Device Testing**
- Tablet layout optimization verification across different tablet sizes
- Master-detail functionality testing
- Touch target accessibility compliance on tablets
- Performance benchmarking on tablet devices

### UI Testing

**Tablet Layout UI Tests**
```kotlin
@Test
fun testTabletGridLayout() {
    // Set tablet screen size
    // Verify multi-column grid is displayed
    // Verify existing components work in grid layout
    // Verify touch targets are accessible
}

@Test
fun testTabletMasterDetail() {
    // Set tablet landscape mode
    // Verify master-detail layout is displayed
    // Verify master panel interactions
    // Verify detail panel updates correctly
}
```

## Implementation Phases

### Phase 1: Foundation (Tablet Infrastructure)
1. **Tablet Detection System**
   - Implement tablet configuration detection
   - Create `LocalIsTablet` composition local
   - Add tablet spacing extensions
   - Test tablet detection across devices

2. **Basic Tablet Components**
   - Implement `TabletContainer` base component
   - Create `TabletGrid` for multi-column layouts
   - Add tablet spacing helper functions
   - Test basic tablet layout switching

### Phase 2: Screen Adaptations (Tablet Layouts)
1. **RecordShotScreen Tablet Layout**
   - Implement enhanced spacing and timer sizing
   - Create two-column layout for tablet landscape
   - Ensure existing functionality is preserved
   - Optimize touch targets for tablet interaction

2. **ShotHistoryScreen Tablet Layout**
   - Implement multi-column grid for shot cards
   - Create master-detail pattern for tablet landscape
   - Maintain existing filtering and analysis functionality
   - Test tablet-specific interactions

3. **BeanManagementScreen Tablet Layout**
   - Implement three-column grid for bean cards
   - Create master-detail editing for tablet landscape
   - Preserve all existing bean management functionality
   - Test photo viewing and editing on tablets

### Phase 3: Master-Detail Implementation
1. **Advanced Tablet Features**
   - Implement `TabletMasterDetail` component
   - Add tablet-specific navigation patterns
   - Optimize animations for tablet interactions
   - Add tablet-specific performance optimizations

2. **Polish and Optimization**
   - Fine-tune spacing and sizing for tablets
   - Optimize layout calculations for tablet screens
   - Add tablet-specific accessibility features
   - Comprehensive testing across tablet devices

## Technical Considerations

### Compose-Specific Implementation Details

**Tablet-Aware Composables**
```kotlin
@Composable
fun rememberIsTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        configuration.screenWidthDp >= 600
    }
}

@Composable
fun <T> rememberTabletState(
    key: String,
    initialValue: T,
    saver: Saver<T, out Any> = autoSaver()
): MutableState<T> {
    return rememberSaveable(
        key = key,
        saver = saver
    ) { mutableStateOf(initialValue) }
}
```

### Performance Considerations

**Tablet Layout Optimization**
- Cache layout calculations for tablet configurations
- Use `derivedStateOf` for expensive tablet calculations
- Optimize recomposition scope for tablet layouts
- Efficient memory management for larger layouts

### Integration with Existing Systems

**Theme System Integration (Preserving Existing Coffee Theme)**
```kotlin
// Extend existing LocalSpacing with tablet awareness
val LocalIsTablet = staticCompositionLocalOf { false }

@Composable
fun CoffeeShotTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    // Preserve existing color scheme and theme logic
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Extend existing theme with tablet awareness
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalIsTablet provides isTablet
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
```

## Accessibility Considerations

### Tablet-Specific Accessibility

**Touch Target Optimization**
- Ensure touch targets scale appropriately for tablet screens
- Maintain minimum 44.dp touch targets with enhanced spacing
- Optimize touch target spacing for finger interaction on larger screens
- Provide alternative interaction methods for complex layouts

**Screen Reader Support**
- Proper content descriptions for tablet-specific layouts
- Logical reading order in multi-column and master-detail layouts
- Semantic markup for grid layouts and master-detail patterns
- Consistent navigation patterns across tablet orientations

## Future Enhancements

### Advanced Tablet Features
- Tablet-specific animations and transitions
- Advanced master-detail patterns with split-screen support
- Tablet-optimized image viewing and editing
- Multi-window support for tablet environments

### Accessibility Enhancements
- Voice control integration optimized for tablet screens
- Enhanced keyboard navigation for tablet layouts
- Tablet-specific accessibility shortcuts
- Motor accessibility improvements for larger screens