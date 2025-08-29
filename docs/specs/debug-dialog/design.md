# Design Document

## Overview

The debug dialog feature adds a hidden developer utility to the Record Shot screen that provides database management capabilities for testing and screenshot creation. The feature is implemented using conditional compilation to ensure it's only available in debug builds, maintaining production app security and performance.

The dialog is activated through a secret gesture (5 consecutive taps on the "Record Shot" header) and provides two primary functions: populating the database with realistic test data and clearing all database content. This enables developers to quickly set up scenarios for testing and creating Play Store screenshots.

## Architecture

### Component Structure

```
RecordShotScreen
├── DebugTapDetector (debug only)
├── DebugDialog (debug only)
└── DatabasePopulator (debug only)
```

### Conditional Compilation Strategy

The feature uses Kotlin's conditional compilation through build variants:
- Debug builds: Include all debug functionality
- Release builds: Completely exclude debug code using `if (BuildConfig.DEBUG)` checks
- No performance impact on release builds as debug code is not compiled

### State Management

The debug functionality integrates with the existing `ShotRecordingViewModel` pattern:
- `DebugViewModel` handles debug-specific state and operations
- Follows the same MVVM pattern as the main app
- Uses Hilt dependency injection for consistency

## Components and Interfaces

### 1. DebugTapDetector

**Purpose**: Detects the secret tap gesture on the header text

**Implementation**:
```kotlin
@Composable
fun DebugTapDetector(
    onDebugActivated: () -> Unit,
    content: @Composable () -> Unit
) {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    // Reset counter after 3 seconds of inactivity
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            delay(3000)
            tapCount = 0
        }
    }
    
    Box(
        modifier = Modifier.clickable {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime > 3000) {
                tapCount = 1
            } else {
                tapCount++
            }
            lastTapTime = currentTime
            
            if (tapCount >= 5) {
                onDebugActivated()
                tapCount = 0
            }
        }
    ) {
        content()
    }
}
```

### 2. DebugDialog

**Purpose**: Provides the debug interface with database management buttons

**Key Features**:
- Modal dialog with clear visual indication of debug mode
- Two primary action buttons: "Fill Database" and "Clear Database"
- Loading states and result feedback
- Confirmation dialog for destructive operations

**Interface**:
```kotlin
@Composable
fun DebugDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onFillDatabase: () -> Unit,
    onClearDatabase: () -> Unit,
    isLoading: Boolean,
    operationResult: String?
)
```

### 3. DatabasePopulator

**Purpose**: Handles database operations for testing data

**Key Methods**:
```kotlin
class DatabasePopulator @Inject constructor(
    private val beanDao: BeanDao,
    private val shotDao: ShotDao
) {
    suspend fun populateForScreenshots()
    suspend fun clearAllData()
    suspend fun addMoreShots(count: Int = 10)
}
```

**Test Data Strategy**:
- Creates 3-5 realistic coffee bean profiles with varied roast dates
- Generates 15-25 shot records with realistic parameters
- Ensures data variety for comprehensive screenshots
- Uses realistic coffee terminology and measurements

### 4. DebugViewModel

**Purpose**: Manages debug dialog state and coordinates database operations

**State Properties**:
```kotlin
data class DebugUiState(
    val isDialogVisible: Boolean = false,
    val isLoading: Boolean = false,
    val operationResult: String? = null,
    val showConfirmation: Boolean = false
)
```

## Data Models

### Test Data Specifications

**Bean Test Data**:
- Bean names: "Ethiopian Yirgacheffe", "Colombian Supremo", "Brazilian Santos", "Guatemalan Antigua"
- Roast dates: Varied from 3-20 days ago to show freshness indicators
- Grinder settings: Realistic values like "2.5", "3.0", "2.8"
- Notes: Realistic tasting notes and brewing observations

**Shot Test Data**:
- Coffee weights: 18-22g input, 36-50g output (realistic espresso ratios)
- Extraction times: 25-35 seconds (optimal espresso range)
- Grinder settings: Correlated with bean preferences
- Timestamps: Spread across recent days to show history
- Notes: Varied feedback like "Perfect crema", "Slightly sour", "Great balance"

## Error Handling

### Database Operation Errors

**Fill Database Errors**:
- Database constraint violations
- Insufficient storage space
- Concurrent access issues

**Clear Database Errors**:
- Foreign key constraint issues
- Database lock conflicts
- Permission errors

**Error Recovery Strategy**:
- Graceful error messages in the dialog
- Automatic dialog dismissal after error display
- Logging for debugging purposes (debug builds only)

### User Experience Considerations

**Confirmation Flow**:
- Clear database requires explicit confirmation
- Fill database shows preview of what will be added
- Loading indicators prevent multiple operations

**Visual Feedback**:
- Success messages with operation summary
- Error messages with actionable information
- Progress indicators for long-running operations

## Testing Strategy

### Unit Tests

**DebugViewModel Tests**:
- State management verification
- Error handling scenarios
- Operation coordination logic

**DatabasePopulator Tests**:
- Data generation accuracy
- Database operation success/failure
- Data consistency validation

### Integration Tests

**End-to-End Debug Flow**:
- Tap detection accuracy
- Dialog display and interaction
- Database operations with real data

**Build Variant Testing**:
- Debug build includes functionality
- Release build excludes all debug code
- Performance impact measurement

### Manual Testing Scenarios

**Screenshot Creation Workflow**:
1. Clear database
2. Fill with test data
3. Navigate through app screens
4. Capture screenshots
5. Verify data quality and realism

**Error Scenarios**:
- Database corruption recovery
- Insufficient storage handling
- Concurrent operation conflicts

## Performance Considerations

### Memory Usage

**Debug Build Impact**:
- Minimal memory overhead (< 1MB additional)
- Lazy initialization of debug components
- Efficient test data generation

**Release Build Impact**:
- Zero memory overhead (code excluded)
- No runtime performance impact
- No additional APK size

### Database Performance

**Bulk Operations**:
- Batch inserts for test data generation
- Transaction management for consistency
- Index optimization for query performance

**Cleanup Operations**:
- Efficient cascade deletion
- Foreign key constraint handling
- Database vacuum after clear operations

## Security Considerations

### Production Safety

**Code Exclusion**:
- Complete removal from release builds
- No debug symbols in production
- No accidental activation paths

**Data Protection**:
- No sensitive data in test datasets
- Local-only operations (no network calls)
- Proper data cleanup capabilities

### Debug Build Security

**Access Control**:
- Hidden activation method
- No external API exposure
- Local device operations only

**Data Handling**:
- Realistic but non-sensitive test data
- Proper cleanup capabilities
- No data persistence beyond app lifecycle