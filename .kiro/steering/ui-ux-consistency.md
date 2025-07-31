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
**Screen Titles**: `headlineMedium` with `FontWeight.Bold`
**Section Headers**: `titleMedium` with `FontWeight.Bold`
**Card Titles**: `titleMedium` with `FontWeight.Medium`
**Body Text**: `bodyMedium` for standard content
**Labels**: `labelMedium` for form labels and small UI text
**Captions**: `bodySmall` for secondary information

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

## Implementation Guidelines

### Component Reuse
- Always use existing components before creating new ones
- Extend existing components rather than duplicating
- Maintain consistent prop interfaces across similar components

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