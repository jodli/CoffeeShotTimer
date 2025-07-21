# Implementation Plan

**Note:** After completing each task, commit all changes with conventional commit messages and clear reasoning in the commit body.

- [x] 1. Set up Android project structure and dependencies
  - Create new Android project with Kotlin and Jetpack Compose
  - Configure build.gradle files with required dependencies (Room, Hilt, Navigation Compose)
  - Set up project structure following MVVM architecture
  - Configure minimum SDK version (API 24+)
  - _Requirements: 6.1, 6.2, 7.1_

- [x] 2. Implement core data models and database schema
  - Implement Bean data class with Room annotations and validation
  - Implement Shot data class with Room annotations, foreign key relationship, and validation
  - Create AppDatabase class with Room database configuration and indexes
  - Add type converters for LocalDate and LocalDateTime
  - _Requirements: 3.1, 3.5, 1.1, 1.2, 4.1, 5.1, 5.2, 6.2, 6.3_

- [x] 3. Implement data access layer
  - Create Bean DAO with CRUD operations and filtering queries
  - Create Shot DAO with filtering, history queries, and statistics
  - Create BeanRepository with business logic and error handling
  - Create ShotRepository with business logic and validation
  - _Requirements: 3.1, 3.3, 3.4, 4.4, 2.1, 2.3, 2.4, 6.1, 6.2, 6.3_

- [x] 4. Create domain layer use cases
  - Implement RecordShotUseCase with timer functionality and validation
  - Implement bean management use cases (Add, Update, GetActive, GetHistory)
  - Implement shot history and filtering use cases (GetHistory, GetDetails, GetStatistics)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.1, 5.2, 3.1, 3.2, 3.3, 4.4, 2.1, 2.2, 2.3, 2.4, 5.3_

- [x] 5. Set up dependency injection with Hilt
  - Configure Hilt application class and modules
  - Create database module for Room dependencies
  - Create repository module for dependency injection
  - Set up ViewModels with Hilt injection
  - _Requirements: 6.1, 6.2_

- [x] 6. Implement UI foundation and navigation
  - Create main navigation graph with bottom navigation
  - Implement navigation between Record Shot, Shot History, and Bean Management screens
  - Add modal navigation for detailed views
  - Update MainActivity to use navigation
  - Enhance app theme with colors, typography, and spacing
  - Create reusable components for input fields, buttons, and cards
  - Add touch-friendly design with 44dp minimum touch targets
  - Implement accessibility features and content descriptions
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 7. Implement Shot Recording screen
  - Design main recording interface with weight inputs and timer
  - Implement bean selection dropdown with current bean highlighting
  - Add large, prominent timer display with start/stop controls
  - Create numeric input fields with decimal precision
  - Integrate ShotRecordingViewModel with RecordShotUseCase
  - Implement timer functionality with accurate time tracking
  - Add real-time brew ratio calculation and display
  - Implement form validation with inline error messages
  - Implement save shot functionality with validation
  - Add auto-save draft functionality to prevent data loss
  - Integrate with repository for data persistence
  - Add success feedback and error handling
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.2, 5.1, 5.2, 6.2, 7.1, 7.2, 7.3, 7.4_

- [x] 8. Implement Bean Management screen
  - Design bean list interface with active beans and roast dates
  - Implement add/edit bean modal with form inputs
  - Add visual indicators for days since roasting
  - Create delete confirmation dialogs
  - Create BeanManagementViewModel with CRUD operations
  - Add form validation for bean name, roast date, and notes
  - Implement active bean management and selection
  - Add grinder setting memory per bean
  - Connect bean selection in shot recording to bean management
  - Implement grinder setting suggestions based on last used setting
  - Add seamless navigation between bean management and shot recording
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.4, 7.1_

- [x] 9. Implement Shot History screen
  - Design chronological shot list with key metrics display
  - Implement filtering controls for date range and bean type
  - Add detailed shot view modal with all parameters
  - Create visual indicators for brew ratios and extraction quality
  - Create ShotHistoryViewModel with pagination and filtering
  - Add date range picker and bean filter functionality
  - Implement shot details view with comprehensive information
  - Add sorting options for different shot parameters
  - Implement brew ratio analysis and recommendations with bean-specific filtering
  - Add extraction time trends and statistics filtered by selected bean
  - Create grinder setting correlation analysis for specific beans
  - Display shot success indicators and patterns
  - Ensure analysis and trends are filtered by bean for meaningful comparison
  - Keep overall statistics global across all beans
  - Create dedicated shot detail screen with comprehensive shot information
  - Display all shot parameters including weights, times, and brew ratio
  - Show bean information and grinder settings used for the shot
  - Add edit functionality for shot notes and rating
  - Implement navigation from shot history list to detail screen
  - Add delete shot functionality with confirmation dialog
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.1, 4.2, 5.1, 5.2, 5.3_

- [x] 10. Implement comprehensive testing suite
  - Write tests for Bean and Shot model validation
  - Test use cases and business logic thoroughly
  - Add tests for calculation accuracy (brew ratios, time tracking)
  - Test error handling and edge cases
  - Test Room database operations and migrations
  - Test repository pattern implementation
  - Add tests for offline functionality and data persistence
  - Test foreign key relationships and data integrity
  - Test complete shot recording workflow
  - Test bean management CRUD operations
  - Test shot history filtering and viewing
  - Test timer functionality accuracy and reliability
  - _Requirements: 1.1, 1.2, 1.4, 2.1, 3.1, 4.1, 5.1, 6.1, 6.2, 6.3, 7.2_

- [x] 11. Implement error handling and data validation

- [x] 11.1 Add comprehensive input validation
  - Implement real-time validation for all input fields
  - Add range validation for weights and extraction times
  - Create user-friendly error messages and feedback
  - Add form state management and recovery
  - _Requirements: 1.2, 4.1, 7.3_

- [x] 12. Optimize performance and finalize app

- [x] 12.1 Implement performance optimizations
  - Add lazy loading for shot history with large datasets
  - Optimize database queries with proper indexing
  - Implement efficient UI rendering and state management
  - Add memory usage optimization and cleanup
  - _Requirements: 2.1, 6.3, 7.1_

- [x] 12.2 Final testing and polish
  - Conduct end-to-end testing of all user workflows
  - Test app performance with large datasets
  - Verify offline functionality works completely
  - Add final UI polish and accessibility improvements
  - _Requirements: 6.1, 6.2, 6.3, 7.1, 7.4_

- [x] 12.3 Prepare app for deployment
  - Configure release build settings and signing
  - Add app icons and splash screen
  - Create app store listing materials
  - Perform final security and privacy review
  - _Requirements: 6.1, 7.1_

- [x] 13. Replace refresh button with pull-to-refresh gesture

  - Remove the refresh button from Shot History screen toolbar
  - Implement SwipeRefresh composable around the shot history list
  - Add pull-to-refresh functionality that reloads shot data
  - Ensure refresh state is properly managed in ShotHistoryViewModel
  - Add visual feedback during refresh operation
  - Test pull-to-refresh gesture works smoothly on different devices
  - _Requirements: 7.1, 7.4_

- [x] 14. Implement slider for weight measurements
  - Create a custom slider component for weight inputs with whole gram increments
  - Replace text input fields with sliders for coffee weight in/out in RecordShotScreen
  - Add visual indicators for typical weight ranges
  - Ensure slider values update the ViewModel state correctly
  - Maintain validation rules for weight inputs
  - Add haptic feedback for slider value changes
  - Test slider usability on different screen sizes
  - _Requirements: 7.1, 7.3_

- [x] 15. Implement slider for grinder settings







  - Create a custom slider component for grinder settings with 0.5 increment steps (when possible refactor and reuse logic in WeightSliderComponents.kt)
  - Replace text input field with slider for grinder setting in RecordShotScreen
  - Display numeric value alongside slider
  - Add visual indicators for previous successful settings
  - Ensure slider values update the ViewModel state correctly
  - Maintain validation rules for grinder settings
  - Add haptic feedback for slider value changes
  - Test slider usability on different screen sizes
  - _Requirements: 4.2, 7.1, 7.4_