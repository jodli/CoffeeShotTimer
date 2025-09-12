# Equipment Setup Versioning Guidelines

## Overview

The app uses an equipment setup versioning system to force existing users through new equipment configuration features without requiring them to complete the entire onboarding flow.

## When to Use

Use this versioning system when adding **new equipment configuration features** such as:
- New grinder settings (e.g., step size configuration)
- New basket/portafilter options
- Additional hardware setup parameters
- Any feature that affects shot recording accuracy

**Do NOT use for:**
- UI improvements or bug fixes
- New screens unrelated to equipment setup
- Features that don't require user configuration

## How It Works

### 1. Version Tracking
```kotlin
// OnboardingProgress.kt
companion object {
    const val CURRENT_EQUIPMENT_SETUP_VERSION = 2 // INCREMENT THIS
}

data class OnboardingProgress(
    val equipmentSetupVersion: Int = 1, // User's completed version
    // ... other fields
)
```

### 2. Detection Logic
```kotlin
fun needsEquipmentSetup(): Boolean {
    return !hasCompletedEquipmentSetup || 
           equipmentSetupVersion < CURRENT_EQUIPMENT_SETUP_VERSION
}
```

### 3. Routing Behavior
- **New users**: See complete onboarding flow including new features
- **Existing users**: Skip intro/bean creation, only see equipment setup screens
- **Up-to-date users**: Skip equipment setup entirely

## Implementation Steps

### Step 1: Add New Equipment Feature
Add your new configuration options to:
- Data models (e.g., `GrinderConfiguration`, `BasketConfiguration`)
- UI components in equipment setup flow
- Validation logic
- Database migrations if needed

**Note**: The `EquipmentSetupFlowViewModel` automatically loads existing configurations on initialization, so users will see their current settings pre-filled when forced through equipment setup.

### Step 2: Increment Version
```kotlin
// In OnboardingProgress.kt
const val CURRENT_EQUIPMENT_SETUP_VERSION = 3 // Was 2, now 3
```

### Step 3: Test User Flows
Verify that:
- **New users** see the new feature during onboarding
- **Existing users** are prompted to configure the new feature
- **Users who skip** are still marked with current version
- **Up-to-date users** skip equipment setup

## Example: Adding Step Size Configuration

**Before (Version 2):**
- Grinder setup: min/max scale only
- Basket setup: weight ranges

**After (Version 3):**
```kotlin
// 1. Updated GrinderConfiguration with stepSize field
// 2. Added step size UI to grinder setup screen
// 3. Incremented version:
const val CURRENT_EQUIPMENT_SETUP_VERSION = 3
```

**Result:**
- Existing users get prompted to configure step size on next app launch
- They skip intro screens and go straight to equipment setup
- New users see step size configuration as part of normal onboarding

## User Experience

### New User Flow
```
App Launch → Introduction → Equipment Setup (with new feature) → Bean Creation → Main App
```

### Existing User Flow (Feature Update)
```
App Launch → Equipment Setup (pre-filled with existing values) → Main App
             ↑ Skips intro and bean creation, existing values loaded
```

### Up-to-date User Flow
```
App Launch → Main App
```

## Pre-filled Configuration Behavior

When existing users are prompted for equipment setup due to version increments:

1. **Existing values are automatically loaded** from the database
2. **All screens show current settings** - users see their existing configuration
3. **Users can click through unchanged screens** quickly
4. **Users stop only at screens with new features** they want to configure
5. **Graceful fallback to defaults** if existing configuration can't be loaded

This makes the update experience much smoother for existing users who may only want to configure the new feature.

## Best Practices

1. **Increment carefully**: Only increment when users MUST configure the new feature
2. **Backward compatibility**: Ensure old configurations still work with sensible defaults
3. **Clear validation**: New features should have helpful validation messages
4. **Skip option**: Allow users to skip if the feature is optional
5. **Version in DB**: Consider adding version fields to configuration entities for future flexibility

## Testing Checklist

- [ ] New users see the new feature in onboarding
- [ ] Existing users are prompted for the new feature only
- [ ] **Existing users see their current settings pre-filled**
- [ ] **Users can click through unchanged screens quickly**
- [ ] Skip functionality works correctly
- [ ] Database migration handles existing data
- [ ] App doesn't crash with old configuration data
- [ ] **Pre-filling gracefully handles missing configurations**
- [ ] Version increment is committed and documented

## Notes

- The version system is stored in `OnboardingProgress` and persisted via `OnboardingManager`
- MainActivityViewModel handles the routing logic based on version checks
- Equipment setup screens are defined in `EquipmentSetupFlowScreen.kt`
- Version increments should be documented in commit messages and release notes
