# Design Document

## Overview

This design outlines the systematic approach to standardizing UI/UX patterns across the Coffee Shot Timer app. The solution focuses on enforcing consistent usage of existing design system components while identifying and fixing deviations from established patterns.

## Architecture

### Component Standardization Strategy
The design follows a component-first approach where all UI elements must use standardized components from the existing design system:

- **CoffeeCard**: Primary container for all grouped content
- **CoffeePrimaryButton/CoffeeSecondaryButton**: All interactive buttons
- **CoffeeTextField**: All text input fields
- **LoadingIndicator**: All loading states
- **LocalSpacing**: All spacing and padding values

### Implementation Phases
1. **Phase 1**: Card standardization and button consistency
2. **Phase 2**: Typography and spacing normalization  
3. **Phase 3**: Loading states and error handling consistency
4. **Phase 4**: String externalization and internationalization support

## Components and Interfaces

### Card Component Usage
```kotlin
// Standard pattern for all cards
CoffeeCard(
    onClick = { /* optional action */ },
    modifier = Modifier.fillMaxWidth()
) {
    // Content automatically gets spacing.cardPadding
    CardHeader(icon, title, optionalActions)
    CardContent()
}
```

### Button Component Usage
```kotlin
// Primary actions
CoffeePrimaryButton(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text("Action")
}

// Secondary actions
CoffeeSecondaryButton(
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth()
) {
    Text("Cancel")
}
```

### Typography Standardization
```kotlin
// Screen titles
Text(
    text = "Screen Title",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold
)

// Card titles
Text(
    text = "Card Title", 
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Medium
)

// Secondary text
Text(
    text = "Secondary info",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

### Spacing System Usage
```kotlin
val spacing = LocalSpacing.current

// Screen padding
modifier = Modifier.padding(horizontal = spacing.screenPadding)

// Card spacing
Spacer(modifier = Modifier.height(spacing.medium))

// Internal component spacing
modifier = Modifier.padding(spacing.cardPadding)
```



### Loading State Standardization
```kotlin
// Consistent loading indicator usage
LoadingIndicator(
    modifier = Modifier.fillMaxWidth(),
    message = stringResource(R.string.loading_shots)
)

// Error state with retry functionality
ErrorState(
    message = stringResource(R.string.error_loading_data),
    onRetry = { /* retry action */ },
    modifier = Modifier.fillMaxWidth()
)
```

### String Externalization Pattern
```kotlin
// All hardcoded strings must be externalized
Text(
    text = stringResource(R.string.screen_title),
    style = MaterialTheme.typography.headlineMedium
)

// Dynamic strings with parameters
Text(
    text = stringResource(
        R.string.shot_time_format, 
        timeInSeconds
    )
)

// Plurals support for internationalization
Text(
    text = pluralStringResource(
        R.plurals.days_since_roast,
        dayCount,
        dayCount
    )
)
```

## Data Models

### Component Audit Model
```kotlin
data class ComponentAudit(
    val screenName: String,
    val componentType: ComponentType,
    val isStandardized: Boolean,
    val issuesFound: List<String>,
    val fixRequired: Boolean
)

enum class ComponentType {
    CARD, BUTTON, TEXT_FIELD, TYPOGRAPHY, SPACING, LOADING_STATE
}
```

### Standardization Checklist
```kotlin
data class StandardizationTask(
    val component: String,
    val currentImplementation: String,
    val targetImplementation: String,
    val priority: Priority,
    val estimatedEffort: Int
)
```

### Internationalization Model
```kotlin
data class StringResource(
    val key: String,
    val defaultValue: String,
    val hasPlurals: Boolean = false,
    val hasParameters: Boolean = false,
    val parameterCount: Int = 0
)

data class LocalizationAudit(
    val screenName: String,
    val hardcodedStrings: List<String>,
    val externalizedStrings: List<StringResource>,
    val translationStatus: TranslationStatus
)

enum class TranslationStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETE, NEEDS_UPDATE
}
```



## Error Handling

### Component Migration Errors
- **Missing imports**: Ensure all standardized components are properly imported
- **Prop mismatches**: Handle cases where existing props don't match new component interfaces
- **Layout breaks**: Test all changes to ensure layouts remain functional

### Validation Strategy
- **Automated checks**: Use lint rules to detect non-standard component usage
- **Visual regression testing**: Compare before/after screenshots
- **Accessibility testing**: Ensure changes don't break accessibility features

## Testing Strategy

### Component Standardization Testing
1. **Unit Tests**: Verify each standardized component renders correctly
2. **Integration Tests**: Test component interactions and state management
3. **Visual Tests**: Screenshot comparisons for layout consistency
4. **Accessibility Tests**: Ensure all components meet accessibility standards

### Testing Approach per Phase
- **Phase 1**: Focus on card and button rendering across all screens
- **Phase 2**: Validate typography hierarchy and spacing consistency
- **Phase 3**: Test loading states and error handling flows
- **Phase 4**: Validate string externalization and test language switching

### Regression Prevention
- **Component usage linting**: Prevent future non-standard component usage
- **Design system documentation**: Clear guidelines for component usage
- **Code review checklists**: Ensure new code follows standardization patterns