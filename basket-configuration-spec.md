# Basket Size Configuration Specification

## Overview
Add basket size configuration to the equipment setup, allowing users to select between single shot, double shot, or manually configure their coffee weight ranges. This will automatically adjust the weight sliders in the shot recording screen.

## Feature Description

### Core Functionality
- Select between preset basket configurations (Single, Double)
- Manual configuration option for custom weight ranges
- Automatically adjusts Coffee In and Coffee Out slider ranges in RecordShotScreen
- Saves configuration to database for persistence

### User Interface

#### Basket Configuration Component
Following the same pattern as `GrinderScaleSetup.kt`, create a new `BasketSetup` component:

```kotlin
@Composable
fun BasketSetup(
    coffeeInMin: String,
    coffeeInMax: String,
    coffeeOutMin: String,
    coffeeOutMax: String,
    onCoffeeInMinChange: (String) -> Unit,
    onCoffeeInMaxChange: (String) -> Unit,
    onCoffeeOutMinChange: (String) -> Unit,
    onCoffeeOutMaxChange: (String) -> Unit,
    onPresetSelected: (BasketPreset) -> Unit,
    coffeeInMinError: String?,
    coffeeInMaxError: String?,
    coffeeOutMinError: String?,
    coffeeOutMaxError: String?,
    generalError: String?,
    validationSuggestion: String?,
    modifier: Modifier = Modifier,
    showDescription: Boolean = true,
    showPresets: Boolean = true
)
```

#### Preset Options

**Single Shot Basket**
- Coffee In: 7-12g
- Coffee Out: 20-40g
- Button label: "Single"

**Double Shot Basket** 
- Coffee In: 14-22g  
- Coffee Out: 28-55g
- Button label: "Double"

**Manual Configuration**
- Four input fields:
  - Coffee In Minimum (g)
  - Coffee In Maximum (g)
  - Coffee Out Minimum (g)
  - Coffee Out Maximum (g)

### Data Model

```kotlin
data class BasketConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val coffeeInMin: Float,
    val coffeeInMax: Float,
    val coffeeOutMin: Float,
    val coffeeOutMax: Float,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (coffeeInMin < 5f) errors.add("Coffee in minimum cannot be less than 5g")
        if (coffeeInMax > 30f) errors.add("Coffee in maximum cannot exceed 30g")
        if (coffeeInMin >= coffeeInMax) errors.add("Coffee in minimum must be less than maximum")
        
        if (coffeeOutMin < 10f) errors.add("Coffee out minimum cannot be less than 10g")
        if (coffeeOutMax > 80f) errors.add("Coffee out maximum cannot exceed 80g")
        if (coffeeOutMin >= coffeeOutMax) errors.add("Coffee out minimum must be less than maximum")
        
        // Ensure reasonable ratio range (at least 1:1 to 1:4 possible)
        if (coffeeOutMin / coffeeInMax < 0.8f) {
            errors.add("Weight ranges don't allow reasonable brew ratios")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    companion object {
        val SINGLE_SHOT = BasketConfiguration(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            coffeeOutMin = 20f,
            coffeeOutMax = 40f
        )
        
        val DOUBLE_SHOT = BasketConfiguration(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
        
        val DEFAULT = DOUBLE_SHOT
    }
}

enum class BasketPreset {
    SINGLE,
    DOUBLE
}
```

### Database Schema

```sql
CREATE TABLE basket_configuration (
    id TEXT PRIMARY KEY,
    coffee_in_min REAL NOT NULL,
    coffee_in_max REAL NOT NULL,
    coffee_out_min REAL NOT NULL,
    coffee_out_max REAL NOT NULL,
    created_at TEXT NOT NULL,
    is_active INTEGER DEFAULT 1
);
```

## Integration Details

### Current Weight Validation System
The app uses a comprehensive validation system with:
- `WeightSliderConstants` defines current hardcoded limits (COFFEE_IN: 5-20g, COFFEE_OUT: 10-55g)
- `ValidationUtils` validates weights using these constants
- `ValidationExtensions.kt` provides enhanced validation with contextual messages
- Weight validation happens in `validateCoffeeWeightIn()` and `validateCoffeeWeightOut()` extension functions

### Required Refactoring

#### 1. Replace WeightSliderConstants with dynamic configuration
```kotlin
// Current (hardcoded)
object WeightSliderConstants {
    const val COFFEE_IN_MIN_WEIGHT = 5f
    const val COFFEE_IN_MAX_WEIGHT = 20f
    const val COFFEE_OUT_MIN_WEIGHT = 10f
    const val COFFEE_OUT_MAX_WEIGHT = 55f
}

// New approach - load from BasketConfiguration
class WeightSliderConfig(
    val coffeeInMin: Float,
    val coffeeInMax: Float,
    val coffeeOutMin: Float,
    val coffeeOutMax: Float
) {
    companion object {
        val DEFAULT = WeightSliderConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
    }
}
```

#### 2. Update ValidationUtils and ValidationExtensions
- Modify `validateCoffeeWeightIn()` and `validateCoffeeWeightOut()` to accept dynamic min/max parameters
- Pass basket configuration through validation chain
- Maintain backward compatibility for tests

#### 3. Update WeightSliderComponents.kt
The `CoffeeWeightInSlider` and `CoffeeWeightOutSlider` components need modification:

```kotlin
// Current
@Composable
fun CoffeeWeightInSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        // ...
        minWeight = WeightSliderConstants.COFFEE_IN_MIN_WEIGHT,
        maxWeight = WeightSliderConstants.COFFEE_IN_MAX_WEIGHT,
        // ...
    )
}

// Updated to accept dynamic ranges
@Composable
fun CoffeeWeightInSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minWeight: Float = WeightSliderConstants.COFFEE_IN_MIN_WEIGHT,
    maxWeight: Float = WeightSliderConstants.COFFEE_IN_MAX_WEIGHT,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    WeightSlider(
        // ...
        minWeight = minWeight,
        maxWeight = maxWeight,
        // ...
    )
}
```

#### 4. Update RecordShotViewModel
- Load basket configuration on initialization
- Expose configuration as StateFlow for UI consumption
- Handle missing configuration gracefully (use defaults)

#### 5. Update Shot model validation
- `Shot.validate()` currently uses hardcoded limits (0.1-50g input, 0.1-100g output)
- These broader limits in the model can remain as absolute boundaries
- UI validation will be more restrictive based on basket configuration

### Integration Points

#### Equipment Setup Screen
Add basket configuration section below the existing grinder scale setup:

```kotlin
// In EquipmentSetupScreen.kt
BasketSetup(
    coffeeInMin = uiState.coffeeInMin,
    coffeeInMax = uiState.coffeeInMax,
    coffeeOutMin = uiState.coffeeOutMin,
    coffeeOutMax = uiState.coffeeOutMax,
    onCoffeeInMinChange = viewModel::updateCoffeeInMin,
    onCoffeeInMaxChange = viewModel::updateCoffeeInMax,
    onCoffeeOutMinChange = viewModel::updateCoffeeOutMin,
    onCoffeeOutMaxChange = viewModel::updateCoffeeOutMax,
    onPresetSelected = viewModel::selectBasketPreset,
    // ... error states
)
```

#### RecordShotScreen Integration
The `WeightSlidersSection` component is used in `RecordShotContent`. Update to pass basket configuration:

```kotlin
// In RecordShotContent
WeightSlidersSection(
    coffeeWeightIn = coffeeWeightIn,
    onCoffeeWeightInChange = onCoffeeWeightInChange,
    coffeeWeightInError = coffeeWeightInError,
    coffeeWeightOut = coffeeWeightOut,
    onCoffeeWeightOutChange = onCoffeeWeightOutChange,
    coffeeWeightOutError = coffeeWeightOutError,
    coffeeInMin = basketConfig.coffeeInMin,  // NEW
    coffeeInMax = basketConfig.coffeeInMax,  // NEW
    coffeeOutMin = basketConfig.coffeeOutMin, // NEW
    coffeeOutMax = basketConfig.coffeeOutMax, // NEW
    modifier = Modifier.fillMaxWidth(),
    enabled = !isLoading
)
```

## UX Considerations from GrinderScaleSetup Analysis

### Layout Pattern
The `GrinderScaleSetup` component uses a successful pattern we should replicate:
1. **Card-based container** with icon and title header
2. **Description text** explaining the purpose (optional via `showDescription`)
3. **Preset buttons** in a 2x2 grid for common configurations
4. **Manual input section** with side-by-side text fields
5. **Validation feedback** shown inline with helpful suggestions
6. **Success indicator** when valid range is entered ("✓ Range looks good!")

### For BasketSetup, adapt the pattern:
1. **Single row of 2 preset buttons** (Single, Double) - simpler than grinder's 4 presets
2. **2x2 grid of input fields** for the 4 weight values (instead of 1x2 for grinder)
3. **Visual preview** showing the weight ranges as sliders (optional enhancement)
4. **Validation** ensuring coffee out range is larger than coffee in range

### Consistency Points
- Use same spacing values from `LocalSpacing`
- Use `CoffeeCard` container with `CardHeader`
- Use `CoffeeSecondaryButton` for presets
- Use `CoffeeTextField` for manual inputs
- Use `GentleValidationMessage` for errors
- Show success message when configuration is valid

## User Flow

1. **During Onboarding:**
   - After grinder setup, show basket configuration
   - Presets visible with "Single" and "Double" buttons
   - Manual configuration fields below
   - Continue button saves configuration

2. **In Settings (More → Equipment Settings):**
   - New "Basket Configuration" section
   - Same UI as onboarding but with Save button
   - Shows current configuration values

3. **In Shot Recording:**
   - Weight sliders automatically use configured ranges
   - Default values set to midpoint of ranges
   - No visible indication needed - just works

## Validation Rules

### Coffee In Range
- Minimum: 5g (absolute minimum for any espresso)
- Maximum: 30g (covers even large triple baskets)
- Range must be at least 3g (min to max)

### Coffee Out Range  
- Minimum: 10g (absolute minimum for ristretto)
- Maximum: 80g (covers lungo/americano styles)
- Range must be at least 10g (min to max)

### Ratio Validation
- The ranges must allow for reasonable brew ratios (1:1 to 1:4)
- Warning if ranges don't overlap well for typical ratios

## Localization

```xml
<!-- English -->
<string name="text_basket_configuration">Basket Configuration</string>
<string name="text_basket_description">Set weight ranges based on your portafilter basket size</string>
<string name="text_basket_presets">Common Baskets</string>
<string name="button_single_basket">Single</string>
<string name="button_double_basket">Double</string>
<string name="label_coffee_in_min">Coffee In Min (g)</string>
<string name="label_coffee_in_max">Coffee In Max (g)</string>
<string name="label_coffee_out_min">Coffee Out Min (g)</string>
<string name="label_coffee_out_max">Coffee Out Max (g)</string>
<string name="text_manual_basket_range">Manual Range</string>

<!-- German -->
<string name="text_basket_configuration">Siebträger-Konfiguration</string>
<string name="text_basket_description">Gewichtsbereiche basierend auf deiner Siebgröße einstellen</string>
<string name="text_basket_presets">Gängige Siebe</string>
<string name="button_single_basket">Einzeln</string>
<string name="button_double_basket">Doppelt</string>
<string name="label_coffee_in_min">Kaffee Rein Min (g)</string>
<string name="label_coffee_in_max">Kaffee Rein Max (g)</string>
<string name="label_coffee_out_min">Kaffee Raus Min (g)</string>
<string name="label_coffee_out_max">Kaffee Raus Max (g)</string>
<string name="text_manual_basket_range">Manueller Bereich</string>
```

## Implementation Tasks

1. **Create BasketConfiguration data model** (30 min)
   - Data class with validation
   - Preset configurations
   
2. **Add database table and repository** (1 hour)
   - Create migration
   - Repository methods for save/load
   
3. **Create BasketSetup UI component** (2 hours)
   - Follow GrinderScaleSetup pattern
   - Two preset buttons
   - Four input fields for manual configuration
   
4. **Refactor weight validation system** (2 hours)
   - Replace WeightSliderConstants with dynamic configuration
   - Update ValidationUtils and ValidationExtensions
   - Update WeightSliderComponents
   
5. **Update EquipmentSetupViewModel** (1 hour)
   - Add basket configuration state
   - Validation logic
   - Save functionality
   
6. **Update RecordShotViewModel** (1 hour)
   - Load basket configuration on init
   - Expose as StateFlow
   - Pass to UI components
   
7. **Integrate with RecordShotScreen** (1 hour)
   - Update WeightSlidersSection
   - Pass dynamic ranges to sliders
   - Handle missing configuration
   
8. **Add to Equipment Settings screen** (30 min)
   - New section for basket configuration
   - Load/save functionality
   
9. **Testing** (1.5 hours)
   - Unit tests for validation
   - UI tests for setup flow
   - Integration tests for slider updates
   - Migration testing

**Total estimated time: 10 hours**

## Testing Strategy

### Test Cases
1. **Migration testing**: Ensure existing users get default configuration
2. **Validation boundary testing**: Test all min/max combinations
3. **UI state persistence**: Configuration survives process death
4. **Integration testing**: Weight sliders update correctly when configuration changes
5. **Accessibility testing**: Screen readers handle the 4-field input grid
6. **Backwards compatibility**: Tests still pass with refactored validation

## Migration Strategy

For existing users:
1. On app update, check if basket configuration exists
2. If not, create default configuration (Double Shot preset)
3. Existing shots remain unchanged
4. New shots use configured ranges

## Success Metrics

- Users can successfully configure basket size
- Weight sliders respect configured ranges
- No breaking changes for existing users
- Configuration persists across app restarts
- Validation provides clear, helpful feedback

## Future Enhancements (Not in scope)

- Per-bean basket preferences
- Basket-specific extraction time targets  
- Smart recommendations based on basket size
- Visual indicators showing which preset is active
- Auto-detect basket based on typical weight patterns

## Summary

This feature provides a simple, focused enhancement that directly improves the shot recording experience by adapting the weight ranges to the user's actual equipment. By following the existing GrinderScaleSetup pattern, we maintain UI consistency while adding valuable functionality.

The main refactoring challenge will be replacing the hardcoded `WeightSliderConstants` with dynamic configuration throughout the validation chain, but this will make the app more flexible and maintainable long-term. The changelog shows Coffee In slider range was already expanded to support single-shot baskets (min lowered to 5g in v1.3.0), indicating user demand for this feature.

<citations>
  <document>
      <document_type>RULE</document_type>
      <document_id>365GaSB5Y04UBkzU2iqzgs</document_id>
  </document>
</citations>
