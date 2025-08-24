# UI/UX Consistency Guidelines for Coffee Shot Timer

## Overview
This document establishes consistent UI/UX patterns for the Coffee Shot Timer app, ensuring a cohesive user experience across all screens while maintaining the coffee-focused design language.

## Design System Foundation

### Spacing System
Use the centralized `LocalSpacing` system consistently:
- `extraSmall: 4.dp` - Minimal spacing between related elements
- `small: 8.dp` - Small gaps within components
- `medium: 16.dp` - Standard spacing between components
- `large: 24.dp` - Section spacing
- `extraLarge: 32.dp` - Major section breaks
- `touchTarget: 44.dp` - Minimum touch target for accessibility
- `cardPadding: 16.dp` - Internal card padding
- `screenPadding: 16.dp` - Screen edge margins

### Color Scheme
Follow the coffee-inspired Material 3 color scheme:
- **Primary**: Warm caramel tones for main actions and highlights
- **Secondary**: Soft teal for complementary elements
- **Surface**: Light cream for cards and elevated surfaces
- **Background**: Creamy beige for screen backgrounds
- **Error**: Consistent red for validation errors and warnings

## Component Standards

### Cards (`CoffeeCard`)
**Usage**: Primary container for grouped content
**Standards**:
- Always use `CoffeeCard` instead of raw Material 3 `Card`
- 12.dp rounded corners for modern feel
- 4.dp elevation for subtle depth
- `spacing.cardPadding` (16.dp) internal padding
- `fillMaxWidth()` by default
- Optional `onClick` for interactive cards

**Example**:
```kotlin
CoffeeCard(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    // Content with automatic padding
}
```

### Buttons
**Primary Actions**: Use `CoffeePrimaryButton`
- Full width by default
- `spacing.touchTarget` (44.dp) minimum height
- 8.dp rounded corners
- Optional leading icon with 8.dp spacing
- `labelLarge` typography

**Secondary Actions**: Use `CoffeeSecondaryButton`
- Same sizing as primary but outlined style
- Use for cancel, dismiss, or alternative actions

**Floating Actions**: Use Material 3 `FloatingActionButton`
- 56.dp for main actions, 40.dp for secondary
- Circular shape
- Use for primary screen actions (Add, Save, etc.)

### Text Input (`CoffeeTextField`)
**Standards**:
- Always use `CoffeeTextField` for consistency
- 8.dp rounded corners
- Full width by default
- Support for leading/trailing icons
- Integrated error state with red text below field
- `spacing.medium` start padding for error messages
- `spacing.extraSmall` top padding for error messages

### Typography Hierarchy
**Screen Titles**: `headlineMedium` with `FontWeight.Bold` (use `headlineSmall` in landscape)
**Section Headers**: `titleMedium` with `FontWeight.Bold`
**Card Titles**: `titleMedium` with `FontWeight.Medium`
**Body Text**: `bodyMedium` for standard content
**Labels**: `labelMedium` for form labels and small UI text
**Captions**: `bodySmall` for secondary information

**Important**: Always use `headlineMedium` for main screen titles, not `headlineLarge`. Scale down to `headlineSmall` in landscape orientation for better space utilization.

### Loading States
**Use `LoadingIndicator` component**:
- Centered circular progress indicator
- Optional message below spinner
- `spacing.medium` gap between spinner and text
- Primary color for spinner

### Empty States
**Use `EmptyState` component**:
- 64.dp icon at top
- Title with `titleLarge` typography
- Description with `bodyMedium` typography
- Optional action button (max 200.dp width)
- Centered layout with proper spacing

## Screen Layout Patterns

### Screen Structure
1. **Top App Bar** (when needed)
   - Navigation icon (back arrow)
   - Screen title with `FontWeight.Bold`
   - Action icons on the right

2. **Content Area**
   - `spacing.screenPadding` (16.dp) horizontal margins
   - `spacing.medium` (16.dp) vertical spacing between sections
   - Scrollable content when needed

3. **Bottom Spacing**
   - `spacing.large` (24.dp) bottom padding for navigation clearance

### Responsive Layout Patterns

#### Onboarding/Educational Screen Layout
**Portrait**:
- Single column layout with centered content
- Hero icon at top with `spacing.large` margins
- Content sections with `spacing.medium` vertical spacing
- Action buttons at bottom with horizontal arrangement

**Landscape**:
- Two-column layout using `Row` with `Arrangement.spacedBy(landscapeSpacing * 2)`
- Left column (30% weight): Hero icon + title, center-aligned
- Right column (70% weight): Content + actions, start-aligned
- Consistent `landscapeSpacing` for internal component spacing
- Vertical scrolling for content overflow

**Weight Distribution Standard**:
```kotlin
// Left side: Visual/branding (30% width)
Column(modifier = Modifier.weight(0.3f)) { /* Icon + Title */ }

// Right side: Content/actions (70% width) 
Column(modifier = Modifier.weight(0.7f)) { /* Content + Actions */ }
```

#### Typography Scaling for Orientation
**Portrait to Landscape Typography Scaling**:
- `headlineLarge` → `headlineSmall`
- `headlineMedium` → `headlineSmall` 
- `titleMedium` → `titleSmall`
- `bodyMedium` → `bodySmall`
- `labelLarge` → `labelMedium`

### Card Layouts
**Information Cards**:
- Header with icon and title
- Content with consistent spacing
- Optional actions in top-right or bottom

**Form Cards**:
- Group related inputs
- Consistent field spacing
- Validation messages below fields

**List Items**:
- Left content (main info)
- Right content (actions/metadata)
- `spacing.small` internal spacing
- Clickable with ripple effect

### Management Screens (More)
Purpose: Central place for non-core features (settings, data management, legal, about, etc.).
Structure:
- Screen title using `headlineMedium` bold
- Each management feature in its own `CoffeeCard` that leads to a sub-screen
- Rows inside cards are touch-target height (`spacing.touchTarget`), with:
  - Leading icon (primary tint)
  - Title (titleMedium) and optional supporting text (bodySmall, onSurfaceVariant)
  - Trailing chevron (AutoMirrored ArrowForward, onSurfaceVariant)
Spacing:
- Screen padding: `spacing.screenPadding`
- Vertical spacing between cards: `spacing.medium`
- Row padding inside cards: horizontal `spacing.cardPadding`, vertical `spacing.small`
Behavior:
- Entire card is clickable with ripple effect (use `CoffeeCard(onClick = ...)`)
- Use concise labels; keep supporting text short
- No section headers - each management feature is a standalone card

## Interactive Elements

### Touch Targets
- Minimum 44.dp for all interactive elements
- Use `spacing.touchTarget` constant
- Ensure adequate spacing between adjacent targets

### Feedback
**Visual Feedback**:
- Ripple effects for clickable items
- Color changes for state (selected, pressed)
- Scale animations for button presses (0.95f scale)

**Haptic Feedback**:
- Light haptic (10ms) for start actions
- Medium haptic (25ms) for stop/destructive actions
- Use `triggerHapticFeedback()` utility function

### State Management
**Loading States**:
- Show loading indicators during async operations
- Disable interactive elements during loading
- Provide loading messages for context

**Error States**:
- Use error color scheme consistently
- Show error messages with clear actions
- Provide retry mechanisms where appropriate

**Success States**:
- Use primary color for success indicators
- Temporary success messages with dismiss actions
- Clear visual confirmation of completed actions

## Navigation Patterns

### Screen Header Guidelines
The app follows a consistent header pattern based on navigation context:

#### Main Navigation Screens (No TopAppBar)
**When**: Navigation bar is visible at bottom (portrait) or side (landscape)
**Screens**: RecordShot, ShotHistory, BeanManagement, More
**Pattern**:
- No TopAppBar or screen title header
- Users can see current screen from highlighted navigation item
- Content starts immediately with functional elements
- Creates clean, focused interface for core app functions

**Rationale**: Since users can clearly see which screen they're on from the active navigation indicator, adding a redundant title wastes valuable screen space and creates visual clutter.

#### Sub-Screens (With TopAppBar + Back Button)
**When**: Navigation bar is hidden (accessed from main screens)
**Screens**: AboutScreen, EquipmentSettingsScreen, AddEditBeanScreen, ShotDetailsScreen
**Pattern**:
- TopAppBar with screen title using `headlineMedium` + `FontWeight.Bold`
- Leading navigation icon (back arrow) using `Icons.AutoMirrored.Filled.ArrowBack`
- Optional trailing action icons (save, delete, etc.)
- Clear visual hierarchy with title and back navigation

**Rationale**: Without the navigation bar context, users need clear indication of:
1. Where they are (screen title)
2. How to get back (back button)
3. Available actions (action icons)

#### Onboarding/Modal Screens (With TopAppBar + Special Navigation)
**When**: Outside normal app flow (first-time setup, modal interactions)
**Screens**: IntroductionScreen, EquipmentSetupScreen
**Pattern**:
- TopAppBar with contextual title
- Navigation appropriate to flow (back to previous step, skip, etc.)
- May include progress indicators
- Special handling for flow completion

**Rationale**: Onboarding and modal contexts require different navigation patterns than the main app, so headers provide necessary context and flow control.

### Back Button Implementation Standards
**Icon**: Always use `Icons.AutoMirrored.Filled.ArrowBack`
- Handles RTL layouts automatically
- Consistent with Material Design standards
- 24.dp size (default icon size)

**Content Description**: Use `stringResource(R.string.cd_back)` 
- Provides accessibility support
- Consistent across all back buttons
- Localizable for different languages

**Action**: Navigate back using `navController.popBackStack()` or provided callback
- Handle navigation in ViewModel or parent component
- Ensure proper state cleanup on back navigation
- Consider confirmation dialogs for unsaved changes

**Example Implementation**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_screen_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Screen content
    }
}
```

### Status Bar and System UI Handling
The app uses edge-to-edge display with proper status bar spacing:

**Portrait Mode (Scaffold Wrapped)**:
- Content automatically receives proper status bar padding via Scaffold's `innerPadding`
- No manual `statusBarsPadding()` needed on screen content
- Navigation bar automatically handled by Scaffold

**Landscape Mode (No Scaffold)**:
- `LandscapeContainer` automatically applies `statusBarsPadding()` only in landscape orientation
- Prevents content from drawing behind status bar
- Manual spacing not needed - handled by container

**Implementation**:
- Use `LandscapeContainer` for screens with landscape-specific layouts
- Use `Scaffold` for sub-screens with TopAppBar
- Never apply manual `statusBarsPadding()` - let containers handle it
- Test in both orientations to ensure proper spacing

**Example**:
```kotlin
// ✅ Correct - LandscapeContainer handles status bar automatically
LandscapeContainer(
    modifier = Modifier.fillMaxSize(), // No statusBarsPadding needed
    portraitContent = { /* content */ },
    landscapeContent = { /* content */ }
)

// ✅ Correct - Scaffold handles status bar via innerPadding  
Scaffold(
    modifier = modifier, // No statusBarsPadding needed
    topBar = { TopAppBar(/* ... */) }
) { innerPadding ->
    Content(modifier = Modifier.padding(innerPadding))
}
```

### Screen Transitions
- Use standard navigation component patterns
- Consistent back button behavior
- Proper state preservation during navigation

### Bottom Sheets
- Use `ModalBottomSheet` for selection dialogs
- Proper dismiss handling
- Consistent header with title and optional actions

### Dialogs
- Use `AlertDialog` for confirmations
- Clear title and description
- Consistent button layout (Cancel/Confirm)

## Accessibility

### Content Description
- Provide meaningful content descriptions for all icons
- Use descriptive text for interactive elements
- Support screen readers with proper semantics

### Color Contrast
- Ensure sufficient contrast ratios
- Don't rely solely on color for information
- Use additional visual indicators (icons, text)

### Touch Accessibility
- Minimum 44.dp touch targets
- Adequate spacing between interactive elements
- Support for large text and display scaling

## Coffee-Specific UI Patterns

### Bean Information Display
- Bean name as primary text
- Days since roast with freshness indicators
- Color-coded freshness status (green for fresh)
- Grinder settings as secondary information

### Shot Metrics Display
- Brew ratio as prominent display (1:X.X format)
- Color coding for optimal ranges (green) vs. suboptimal (red/amber)
- Time display in seconds for < 60s, MM:SS for longer
- Weight display with "g" suffix

### Timer Components
- Large, prominent timer display (200.dp diameter)
- Color-coded progress (green for optimal, amber/red for suboptimal)
- Clickable timer for improved usability
- Haptic feedback for timer interactions

### Quality Indicators
- Circular dots for binary quality indicators
- Color coding: primary for good, outline for neutral/poor
- Consistent sizing (8.dp diameter)
- Grouped indicators with small spacing

## Onboarding & Educational Patterns

### Hero Icon Display
**Usage**: Primary visual element for onboarding and educational screens
**Standards**:
- **Portrait**: 140.dp container, 120.dp background circle, 70.dp icon
- **Landscape**: 100.dp container, 80.dp background circle, 50.dp icon
- Background circle using `primaryContainer.copy(alpha = 0.1f)`
- 2.dp tonal elevation for subtle depth
- Icon tinted with `primary` color

**Example**:
```kotlin
Box(
    modifier = Modifier.size(140.dp), // 100.dp for landscape
    contentAlignment = Alignment.Center
) {
    Surface(
        modifier = Modifier.size(120.dp), // 80.dp for landscape
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {}
    
    Icon(
        imageVector = illustration,
        contentDescription = title,
        modifier = Modifier.size(70.dp), // 50.dp for landscape
        tint = MaterialTheme.colorScheme.primary
    )
}
```

### Educational Cards
**Usage**: Explain features or concepts in onboarding flows
**Standards**:
- Use `CoffeeCard` as base container
- Left icon (24.dp portrait, 20.dp landscape) with `primary` tint
- Title using `titleMedium` with `FontWeight.Medium`
- Description using `bodyMedium` with `onSurfaceVariant` color
- `spacing.medium` gap between icon and text (portrait)
- `landscapeSpacing` for all spacing in landscape variant

**Pattern Variants**:
- **Simple**: Icon + title + description
- **Highlighted**: Icon with circular background + title + description

### Feature Highlight Cards
**Usage**: Showcase specific app features with enhanced visual treatment
**Standards**:
- Use `CoffeeCard` base with interactive styling
- Circular icon background (32.dp container, 28.dp circle portrait)
- Background color: `primaryContainer.copy(alpha = 0.2f)`
- Icon size: 16.dp (portrait), 12.dp (landscape)
- Title: `titleMedium` + `FontWeight.SemiBold` (portrait), `labelLarge` (landscape)
- Description: `bodyMedium` (portrait), `bodySmall` (landscape)

### Page Indicators
**Usage**: Show progress through onboarding or tutorial flows
**Standards**:
- Active state: 24.dp width, 8.dp height, `primary` color
- Inactive state: 8.dp width, 8.dp height, `onSurfaceVariant.copy(alpha = 0.4f)`
- Circular shape using `CircleShape`
- `spacing.extraSmall` horizontal padding between indicators
- Center alignment in horizontal row

**Example**:
```kotlin
@Composable
fun PageIndicator(isActive: Boolean) {
    val width = if (isActive) 24.dp else 8.dp
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    
    Surface(
        modifier = Modifier.width(width).height(8.dp),
        shape = CircleShape,
        color = color
    ) {}
}
```

## Implementation Guidelines

### Component Reuse
- Always use existing components before creating new ones
- Extend existing components rather than duplicating
- Maintain consistent prop interfaces across similar components

### Responsive Design Principles
- Use `LandscapeContainer` for onboarding/educational screens
- Apply 30%/70% weight distribution for landscape layouts
- Scale typography down for landscape orientation
- Use `landscapeSpacing` consistently in landscape variants
- Maintain content hierarchy across orientations

### State Management
- Use consistent state patterns across screens
- Implement proper loading, error, and success states
- Provide clear user feedback for all actions

### Performance
- Use `remember` for expensive calculations
- Implement proper key handling in lists
- Optimize recomposition with stable parameters

### Testing
- Ensure all interactive elements are testable
- Use semantic properties for test identification
- Maintain consistent component structure for testing

## Code Organization

### File Structure
- Keep related components together
- Use descriptive file names
- Separate complex components into dedicated files

### Naming Conventions
- Use "Coffee" prefix for custom components
- Descriptive function and variable names
- Consistent parameter naming across components

### Documentation
- Document complex components with KDoc
- Include usage examples for reusable components
- Maintain this consistency guide as patterns evolve