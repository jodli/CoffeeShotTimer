# Design Document

## Overview

The Espresso Shot Tracker is a native Android application designed for home baristas to record, track, and analyze espresso shots. The app focuses on quick data entry during the brewing process while providing comprehensive analysis tools for dialing in espresso parameters. The design emphasizes offline-first functionality, intuitive mobile interactions, and efficient data organization around coffee beans and shot parameters.

## Architecture

### High-Level Architecture

The application follows the MVVM (Model-View-ViewModel) architecture pattern with Android Architecture Components:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  Domain Layer   │    │   Data Layer    │
│                 │    │                 │    │                 │
│ • Activities    │◄──►│ • Use Cases     │◄──►│ • Repository    │
│ • Fragments     │    │ • Models        │    │ • Room Database │
│ • ViewModels    │    │ • Validators    │    │ • DAOs          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Technology Stack

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite)
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Hilt
- **Navigation**: Navigation Compose

## Components and Interfaces

### Core Components

#### 1. Shot Recording Screen
- **Purpose**: Primary interface for recording new espresso shots
- **Key Features**:
  - Bean selection dropdown with current bean highlighted
  - Weight input fields (coffee in/out) with decimal precision
  - Integrated timer with large start/stop buttons
  - Grinder setting input (numeric or text)
  - Auto-calculated brew ratio display
  - Quick save functionality

#### 2. Bean Management Screen
- **Purpose**: Manage coffee bean profiles
- **Key Features**:
  - Add new bean profiles (name, roast date, notes)
  - View active beans with days since roasting
  - Edit/delete bean profiles
  - Set default/current bean

#### 3. Shot History Screen
- **Purpose**: View and analyze recorded shots
- **Key Features**:
  - Chronological list of shots with key metrics
  - Filter by bean type, date range, grinder setting
  - Detailed shot view with all parameters
  - Visual indicators for brew ratios (good/needs adjustment)

#### 4. Analytics Dashboard (Future Enhancement)
- **Purpose**: Provide insights into brewing patterns
- **Key Features**:
  - Average extraction times per bean
  - Brew ratio trends
  - Grinder setting recommendations

### User Interface Design

#### Navigation Structure
```
Main Navigation (Bottom Navigation Bar)
├── Record Shot (Home)
├── Shot History
└── Bean Management

Modal Screens
├── Add/Edit Bean
├── Shot Details
└── Settings
```

#### Key UI Patterns

**Shot Recording Interface**:
- Large, prominent timer display (MM:SS format)
- Numeric input fields with custom keyboard
- Bean selector with visual indication of current selection
- Auto-save draft functionality to prevent data loss
- Immediate visual feedback for calculated ratios

**Mobile-First Design Principles**:
- Minimum 44dp touch targets
- High contrast text and backgrounds
- Single-handed operation support
- Gesture-friendly interactions
- Minimal cognitive load during brewing

## Data Models

### Core Entities

#### Bean Entity
```kotlin
@Entity(tableName = "beans")
data class Bean(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roastDate: LocalDate,
    val notes: String = "",
    val isActive: Boolean = true,
    val lastGrinderSetting: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### Shot Entity
```kotlin
@Entity(
    tableName = "shots",
    foreignKeys = [ForeignKey(
        entity = Bean::class,
        parentColumns = ["id"],
        childColumns = ["beanId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Shot(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val beanId: String,
    val coffeeWeightIn: Double, // grams
    val coffeeWeightOut: Double, // grams
    val extractionTimeSeconds: Int,
    val grinderSetting: String,
    val brewRatio: Double, // calculated field
    val notes: String = "",
    val timestamp: LocalDateTime = LocalDateTime.now()
)
```

#### Calculated Properties
- **Brew Ratio**: `coffeeWeightOut / coffeeWeightIn`
- **Days Since Roast**: `ChronoUnit.DAYS.between(bean.roastDate, LocalDate.now())`
- **Extraction Rate**: Based on standard espresso timing (25-30 seconds optimal)

### Database Schema

```sql
-- Beans table
CREATE TABLE beans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    roast_date TEXT NOT NULL,
    notes TEXT DEFAULT '',
    is_active INTEGER DEFAULT 1,
    last_grinder_setting TEXT,
    created_at TEXT NOT NULL
);

-- Shots table
CREATE TABLE shots (
    id TEXT PRIMARY KEY,
    bean_id TEXT NOT NULL,
    coffee_weight_in REAL NOT NULL,
    coffee_weight_out REAL NOT NULL,
    extraction_time_seconds INTEGER NOT NULL,
    grinder_setting TEXT NOT NULL,
    brew_ratio REAL NOT NULL,
    notes TEXT DEFAULT '',
    timestamp TEXT NOT NULL,
    FOREIGN KEY (bean_id) REFERENCES beans(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_shots_bean_id ON shots(bean_id);
CREATE INDEX idx_shots_timestamp ON shots(timestamp);
CREATE INDEX idx_beans_active ON beans(is_active);
```

## Error Handling

### Validation Rules

#### Shot Recording Validation
- Coffee weight in: 0.1g - 50.0g range
- Coffee weight out: 0.1g - 100.0g range
- Extraction time: 5 - 120 seconds
- Grinder setting: Required, max 50 characters
- Bean selection: Must be valid active bean

#### Bean Management Validation
- Bean name: Required, max 100 characters, unique
- Roast date: Cannot be future date, max 365 days ago
- Notes: Optional, max 500 characters

### Error Handling Strategy

#### User Input Errors
- Real-time validation with inline error messages
- Prevent invalid data entry where possible
- Clear, actionable error messages
- Graceful degradation for edge cases

#### System Errors
- Database operation failures: Retry with exponential backoff
- Storage full: Alert user with cleanup suggestions
- App crashes: Automatic draft recovery on restart

#### Data Integrity
- Foreign key constraints prevent orphaned shots
- Transaction-based operations for data consistency
- Backup validation before critical operations

## Testing Strategy

### Unit Testing
- **Models**: Validation logic, calculated properties
- **ViewModels**: Business logic, state management
- **Repository**: Data access operations
- **Use Cases**: Domain logic validation

### Integration Testing
- **Database Operations**: Room DAO testing
- **Repository Integration**: End-to-end data flow
- **Navigation**: Screen transitions and state preservation

### UI Testing
- **Critical User Flows**: Shot recording, bean management
- **Input Validation**: Form validation and error states
- **Timer Functionality**: Accuracy and state management
- **Offline Functionality**: Data persistence without network

### Performance Testing
- **Database Queries**: Large dataset performance
- **UI Responsiveness**: Smooth scrolling and interactions
- **Memory Usage**: Efficient resource management
- **Battery Impact**: Minimal background processing

### User Acceptance Testing
- **Workflow Testing**: Real brewing scenario testing
- **Usability Testing**: Single-handed operation validation
- **Accessibility Testing**: Screen reader and contrast validation
- **Device Testing**: Various screen sizes and Android versions

## Implementation Considerations

### Offline-First Design
- All core functionality works without internet
- Local SQLite database for all data storage
- No cloud sync in initial version (future enhancement)
- Robust local data backup and recovery

### Performance Optimization
- Lazy loading for shot history
- Efficient database queries with proper indexing
- Image optimization for bean photos (future feature)
- Minimal background processing

### Accessibility
- Content descriptions for all interactive elements
- High contrast mode support
- Large text support
- Voice input compatibility for hands-free operation

### Security and Privacy
- Local data storage only (no cloud transmission)
- No personal data collection
- Standard Android app permissions
- Data export functionality for user control