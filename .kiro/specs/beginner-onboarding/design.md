# Design Document - Milestone 1: First Start Experience

## Overview

This design document outlines the implementation approach for Milestone 1 of the Beginner Onboarding Experience. The focus is on creating a smooth first-time user experience that gets users successfully set up and recording their first shot without overwhelming them with educational content.

The design leverages the existing Coffee Shot Timer app architecture, UI components, and navigation patterns while adding new onboarding-specific screens and flows.

## Architecture

### High-Level Flow
```
App Launch → First Time Check → App Introduction → Equipment Setup → First Shot Guidance → Normal App Usage
```

### Integration Points
- **MainActivity**: Detect first-time users and route to onboarding
- **Navigation**: Add onboarding destinations to existing navigation graph
- **SharedPreferences**: Store onboarding completion state
- **Existing Screens**: Integrate with RecordShotScreen for first shot experience

### State Management
- **OnboardingViewModel**: Manages onboarding flow state and progress
- **EquipmentSetupViewModel**: Handles grinder configuration and validation
- **Integration with ShotRecordingViewModel**: Seamless transition to normal app usage

## Components and Interfaces

### 1. Onboarding Detection and Routing

#### OnboardingManager
```kotlin
interface OnboardingManager {
    suspend fun isFirstTimeUser(): Boolean
    suspend fun markOnboardingComplete()
    suspend fun getOnboardingProgress(): OnboardingProgress
    suspend fun updateOnboardingProgress(progress: OnboardingProgress)
}

data class OnboardingProgress(
    val hasSeenIntroduction: Boolean = false,
    val hasCompletedEquipmentSetup: Boolean = false,
    val hasRecordedFirstShot: Boolean = false
)
```

#### MainActivity Integration
- Check onboarding status on app launch
- Route to appropriate starting screen based on progress
- Handle deep links and navigation during onboarding

### 2. App Introduction Screen

#### IntroductionScreen
**Purpose**: Interactive walkthrough of main app features and flexible workflow explanation

**Key Components**:
- **WalkthroughPager**: Swipeable introduction slides
- **FeatureHighlight**: Highlight key UI elements with explanations
- **FlexibilityExplanation**: Explain that fields can be filled in any order
- **NavigationButtons**: Skip, Previous, Next, Get Started

**Design Elements**:
```kotlin
@Composable
fun IntroductionScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
)

data class IntroSlide(
    val title: String,
    val description: String,
    val illustration: ImageVector,
    val highlights: List<FeatureHighlight>
)

data class FeatureHighlight(
    val title: String,
    val description: String,
    val icon: ImageVector
)
```

**Slides Content**:
1. **Welcome**: App purpose and benefits
2. **Main Screens**: Overview of RecordShot, History, Bean Management
3. **Flexible Workflow**: Explain measurement fields can be filled in any order
4. **Timer Usage**: Timer can be started before, during, or after other measurements
5. **Get Started**: Transition to equipment setup

### 3. Equipment Setup Screen

#### EquipmentSetupScreen
**Purpose**: Configure grinder scale settings for personalized brewing guidance

**Key Components**:
- **GrinderScaleSetup**: Configure min/max integer values for grinder
- **CommonPresets**: Quick setup buttons for popular ranges (1-10, 30-80, 50-60)
- **CustomRange**: Manual min/max input fields
- **ValidationFeedback**: Real-time validation of scale ranges

**Design Elements**:
```kotlin
@Composable
fun EquipmentSetupScreen(
    onComplete: (GrinderConfig) -> Unit,
    onBack: () -> Unit
)

data class GrinderConfig(
    val scaleMin: Int,
    val scaleMax: Int
)
```

**Grinder Scale Configuration**:
- **Range Input**: Min/max integer fields with validation
- **Common Presets**: Quick setup for popular grinder models (1-10, 30-80, 50-60)
- **Validation**: Ensure min < max, reasonable range size (3-50 steps)

### 4. First Shot Completion Integration

#### Enhanced RecordShotScreen
**Purpose**: Integrate first-time user guidance into existing shot recording

**Modifications to Existing Screen**:
- **FirstTimeOverlay**: Subtle guidance overlay for first-time users
- **ProgressIndicator**: Show "First Shot" progress in header
- **CelebrationDialog**: Special dialog for first shot completion

**Design Elements**:
```kotlin
@Composable
fun FirstTimeUserOverlay(
    isVisible: Boolean,
    currentStep: FirstShotStep,
    onDismiss: () -> Unit
)

enum class FirstShotStep {
    BEAN_SELECTION,
    MEASUREMENTS,
    TIMER_USAGE,
    COMPLETION
}

@Composable
fun FirstShotCelebrationDialog(
    onContinue: () -> Unit,
    onViewShot: (String) -> Unit,
    shotId: String
)
```

## Data Models

### Onboarding State Management
```kotlin
data class OnboardingState(
    val currentStep: OnboardingStep,
    val progress: OnboardingProgress,
    val equipmentConfig: EquipmentConfig?,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class OnboardingStep {
    INTRODUCTION,
    EQUIPMENT_SETUP,
    FIRST_SHOT,
    COMPLETED
}
```

### Equipment Configuration Storage
```kotlin
// Simple grinder configuration storage
data class GrinderConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val scaleMin: Int,
    val scaleMax: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

## Error Handling

### Validation Strategy
- **Progressive Validation**: Validate as user types, not on submit
- **Gentle Feedback**: Use helpful suggestions rather than harsh errors
- **Recovery Paths**: Always provide clear ways to fix issues

### Error Scenarios
1. **Invalid Grinder Range**: Min >= Max
2. **Unreasonable Range**: Too small (< 3 steps) or too large (> 50 steps)
3. **Network Issues**: Graceful offline handling
4. **Storage Failures**: Retry mechanisms with user feedback

### Error UI Components
```kotlin
@Composable
fun GentleValidationMessage(
    message: String,
    suggestion: String,
    onFixClick: () -> Unit
)

@Composable
fun OnboardingErrorCard(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onSkip: (() -> Unit)? = null
)
```

## Testing Strategy

### Unit Testing
- **OnboardingManager**: State persistence and retrieval
- **EquipmentSetupViewModel**: Validation logic and state management
- **Range Validation**: Edge cases for grinder scale configuration

### Integration Testing
- **Navigation Flow**: Complete onboarding journey
- **State Persistence**: Onboarding progress across app restarts
- **Equipment Integration**: Grinder settings in shot recording

### UI Testing
- **Introduction Walkthrough**: All slides and navigation
- **Equipment Setup**: Form validation and submission
- **First Shot Integration**: Overlay behavior and completion

## Implementation Details

### Navigation Integration
```kotlin
// Add to NavigationDestinations.kt
object OnboardingIntroduction : NavigationDestinations("onboarding_introduction")
object OnboardingEquipmentSetup : NavigationDestinations("onboarding_equipment_setup")

// Modify AppNavigation.kt
composable(NavigationDestinations.OnboardingIntroduction.route) {
    IntroductionScreen(
        onComplete = { navController.navigate(NavigationDestinations.OnboardingEquipmentSetup.route) },
        onSkip = { navController.navigate(NavigationDestinations.RecordShot.route) }
    )
}
```

### SharedPreferences Integration
```kotlin
// Add to existing preferences or create OnboardingPreferences
class OnboardingPreferences @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun isFirstTimeUser(): Boolean = 
        !sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    
    fun markOnboardingComplete() {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
```

### Database Integration
```kotlin
// Add to existing Room database
@Entity(tableName = "grinder_configuration")
data class GrinderConfigurationEntity(
    @PrimaryKey val id: String,
    val scaleMin: Int,
    val scaleMax: Int,
    val createdAt: String
)

@Dao
interface GrinderConfigDao {
    @Query("SELECT * FROM grinder_configuration ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentConfig(): GrinderConfigurationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: GrinderConfigurationEntity)
}
```

## UI/UX Considerations

### Design System Integration
- **Consistent Components**: Use existing CoffeeCard, CoffeePrimaryButton, etc.
- **Spacing**: Follow LocalSpacing system for consistent layout
- **Colors**: Use existing coffee-themed color scheme
- **Typography**: Maintain existing typography hierarchy

### Performance
- **Lazy Loading**: Load introduction slides on demand
- **State Preservation**: Handle configuration changes gracefully
- **Memory Management**: Dispose of resources properly
- **Offline Support**: Core functionality works without network

## Integration with Existing Features

### Shot Recording Integration
- **Grinder Slider**: Use configured min/max values from grinder setup
- **Validation**: Apply grinder range validation in existing GrinderSettingSlider
- **Default Values**: Suggest middle of range as starting point
- **Seamless Integration**: Existing GrinderSettingSlider component automatically uses configured range

### Bean Management Integration
- **First Bean Prompt**: Encourage adding first bean after equipment setup
- **Default Bean**: Create sample bean if none exists
- **Bean Selection**: Integrate with existing bean selection flow

### Navigation Integration
- **Bottom Navigation**: Hide during onboarding, show after completion
- **Back Navigation**: Handle back button appropriately in onboarding flow
- **Deep Links**: Handle external navigation during onboarding

### Localization
- **String Resources**: All text in string resources for translation
- **Number Formats**: Respect locale-specific number formatting

## Offline Capability

### Local Storage Strategy
- **SharedPreferences**: Onboarding progress and completion state
- **Room Database**: Equipment configuration and any sample data
- **No Network Dependency**: All onboarding functionality works offline

## Future Considerations

### Analytics Integration
- **Onboarding Funnel**: Track completion rates at each step
- **Drop-off Points**: Identify where users abandon onboarding
- **Equipment Preferences**: Popular grinder configurations
- **Success Metrics**: First shot completion rates

### Customization Options
- **Skip Options**: Allow experienced users to skip steps
- **Replay Access**: Settings option to replay introduction
- **Equipment Updates**: Easy way to modify equipment configuration
- **Multiple Machines**: Future expansion to support multiple machines
