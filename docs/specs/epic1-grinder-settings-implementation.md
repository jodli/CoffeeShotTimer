# Epic 1: Configurable Grinder Settings - Implementation Plan

## Overview
Enable precise grind adjustments tailored to each user's equipment by adding configurable step sizes to the grinder configuration.

## Current State
- **GrinderConfiguration** entity stores only `scaleMin` and `scaleMax`
- System hardcoded to 0.5 step increments
- UI allows setting min/max but not step size

## Goal State
- Add `stepSize` field to GrinderConfiguration
- Support common step sizes: 0.1, 0.2, 0.25, 0.5, 1.0, and custom values
- Validate step size configuration
- Migrate existing data smoothly
- Update UI to configure step size

## Implementation Steps

### Step 1: Update Data Model
**File:** `app/src/main/java/com/jodli/coffeeshottimer/data/model/GrinderConfiguration.kt`

Add `stepSize` field with:
- Default value: 0.5 (maintains backward compatibility)
- Validation: min 0.01, max 10.0
- Must evenly divide the range or provide close approximation

### Step 2: Create Database Migration
**File:** `app/src/main/java/com/jodli/coffeeshottimer/data/database/AppDatabase.kt`

Migration 3→4:
- Add `stepSize` column with default 0.5
- Preserve existing data

### Step 3: Update Repository
**File:** `app/src/main/java/com/jodli/coffeeshottimer/data/repository/GrinderConfigRepository.kt`

- Handle stepSize in CRUD operations
- Ensure validation includes step size

### Step 4: Create Step Size Selector Component
**New File:** `app/src/main/java/com/jodli/coffeeshottimer/ui/components/StepSizeSelector.kt`

- Preset buttons for common values
- Custom input field with validation
- Visual feedback for selected option

### Step 5: Update GrinderScaleSetup Component
**File:** `app/src/main/java/com/jodli/coffeeshottimer/ui/components/GrinderScaleSetup.kt`

- Integrate StepSizeSelector
- Show current step size
- Validate step size with range

### Step 6: Update ViewModel
**File:** `app/src/main/java/com/jodli/coffeeshottimer/ui/viewmodel/EquipmentSettingsViewModel.kt`

- Add stepSize to UI state
- Implement validation logic
- Format grind values according to step size

### Step 7: Update Grind Display Throughout App
- Shot recording screen: format grind according to step size
- Shot history: display with correct precision
- Statistics: use step size for calculations

### Step 8: Testing
- Unit tests for validation logic
- Integration tests for migration
- UI tests for step size selection

## Detailed Changes

### Data Model Changes

```kotlin
// GrinderConfiguration.kt
data class GrinderConfiguration(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scaleMin: Int,
    val scaleMax: Int,
    val stepSize: Double = 0.5, // NEW FIELD
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Existing validations...
        
        // New step size validations
        if (stepSize < 0.01) {
            errors.add("Step size must be at least 0.01")
        }
        if (stepSize > 10.0) {
            errors.add("Step size cannot exceed 10.0")
        }
        
        // Check if step size makes sense for range
        val rangeSize = scaleMax - scaleMin
        if (stepSize > rangeSize) {
            errors.add("Step size cannot be larger than the range")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    // NEW: Get valid grind values based on step size
    fun getValidGrindValues(): List<Double> {
        val values = mutableListOf<Double>()
        var current = scaleMin.toDouble()
        while (current <= scaleMax) {
            values.add(current)
            current += stepSize
        }
        return values
    }
    
    // NEW: Round value to nearest valid step
    fun roundToNearestStep(value: Double): Double {
        val steps = ((value - scaleMin) / stepSize).roundToInt()
        return scaleMin + (steps * stepSize)
    }
}
```

### Migration

```kotlin
// AppDatabase.kt
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add stepSize column with default value
        database.execSQL(
            "ALTER TABLE grinder_configuration ADD COLUMN stepSize REAL NOT NULL DEFAULT 0.5"
        )
    }
}
```

### UI State

```kotlin
// EquipmentSettingsViewModel.kt
data class EquipmentSettingsUiState(
    val scaleMin: String = "",
    val scaleMax: String = "",
    val stepSize: String = "0.5", // NEW
    val selectedPreset: StepSizePreset? = StepSizePreset.HALF, // NEW
    val minError: String? = null,
    val maxError: String? = null,
    val stepSizeError: String? = null, // NEW
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false
)

enum class StepSizePreset(val value: Double, val label: String) {
    TENTH(0.1, "0.1"),
    FIFTH(0.2, "0.2"),
    QUARTER(0.25, "0.25"),
    HALF(0.5, "0.5"),
    ONE(1.0, "1.0")
}
```

## Testing Plan

### Unit Tests
1. Test step size validation
2. Test roundToNearestStep function
3. Test getValidGrindValues generation
4. Test migration logic

### Integration Tests
1. Test saving/loading with step size
2. Test migration with existing data
3. Test grind value formatting

### Manual Testing
1. Create new grinder config with various step sizes
2. Edit existing config to add step size
3. Verify grind values display correctly throughout app
4. Test preset buttons and custom input

## Rollback Plan
If issues arise:
1. Step size defaults to 0.5 (current behavior)
2. UI can hide step size selector via feature flag
3. Database migration is backward compatible

## Success Criteria
- [ ] Users can configure step sizes from 0.01 to 10.0
- [ ] Common presets available for quick selection
- [ ] Existing data migrates without issues
- [ ] Grind values display with appropriate precision
- [ ] Validation prevents invalid configurations
- [ ] All tests pass

## Next Actions
1. Implement data model changes
2. Create and test migration
3. Build UI components
4. Update ViewModels
5. Write tests
6. Manual testing
7. Code review
8. Merge to main
