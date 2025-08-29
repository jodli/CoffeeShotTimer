# Requirements Document

## Introduction

The Beginner Onboarding Experience is designed to transform intimidating espresso brewing into an approachable, systematic learning journey for new users. The feature is split into two milestones: first getting users successfully recording their first shot, then providing ongoing education and guidance through a friendly bean character.

## Milestone 1: First Start Experience

This milestone focuses on getting new users successfully set up and recording their first shot without overwhelming them with educational content.

### Requirements

#### Requirement 1: App Introduction and Navigation

**User Story:** As a new espresso enthusiast, I want a clear introduction to the app's main features and flexible workflow options, so that I can understand how to navigate and use the Coffee Shot Timer effectively according to my brewing style.

##### Acceptance Criteria

1. WHEN a new user opens the app for the first time THEN the system SHALL display an interactive walkthrough of the main screens
2. WHEN the user progresses through the walkthrough THEN the system SHALL highlight key UI elements with clear explanations of their purpose
3. WHEN the user views the shot recording screen THEN the system SHALL explain that fields can be filled in any order to accommodate different brewing workflows
4. WHEN the user sees measurement fields THEN the system SHALL provide simple explanations of what each measurement means (coffee in, grind setting, extraction time, coffee out)
5. WHEN the user completes the app introduction THEN the system SHALL provide a clear path to equipment setup
6. WHEN the user encounters the timer for the first time THEN the system SHALL explain its flexible usage (can be started before, during, or after other measurements)
7. IF the user skips the walkthrough THEN the system SHALL provide access to replay it from settings

#### Requirement 2: Enhanced Equipment Setup with Grinder Scale Flexibility

**User Story:** As a beginner with a specific grinder model, I want to configure my equipment with grinder-appropriate settings, so that I receive relevant brewing guidance tailored to my hardware.

##### Acceptance Criteria

1. WHEN the user starts equipment setup THEN the system SHALL present a single coffee machine configuration (no multiple machine support)
2. WHEN the user selects their grinder type THEN the system SHALL prompt them to configure their grinder's scale range
3. WHEN the user sets up their grinder scale THEN the system SHALL allow them to define the minimum and maximum integer values (e.g., 1-10, 0-100, 50-60)
4. WHEN the user records grind settings THEN the system SHALL present a slider with their configured min/max values as limits
5. WHEN the user adjusts the grind setting slider THEN the system SHALL only allow selection of integer values within their configured range
6. WHEN the user completes equipment setup THEN the system SHALL save the grinder scale configuration and provide appropriate starting parameters
7. IF the user needs to modify their grinder scale later THEN the system SHALL allow reconfiguration through equipment settings

#### Requirement 3: First Shot Completion

**User Story:** As a new user, I want to successfully record my first espresso shot after completing the setup, so that I can start using the app effectively.

##### Acceptance Criteria

1. WHEN the user completes equipment setup THEN the system SHALL guide them to the shot recording screen
2. WHEN the user views the shot recording screen for the first time THEN the system SHALL provide a brief celebration of completing setup
3. WHEN the user records their first shot THEN the system SHALL save all data and provide positive reinforcement
4. WHEN the user completes their first shot THEN the system SHALL mark the first-time setup as complete
5. WHEN the user finishes their first shot THEN the system SHALL provide a clear path to record additional shots
6. IF the user exits during first shot recording THEN the system SHALL save progress and allow resumption later

## Milestone 2: Beginner Education and Progressive Learning

This milestone introduces the friendly bean character and ongoing educational features to help users improve their brewing over time.

### Requirements

#### Requirement 4: Basic Metrics Education with Bean Character

**User Story:** As a user who has recorded their first shot, I want to understand what brew ratio, extraction time, and yield mean through friendly, approachable guidance, so that I can make informed decisions without feeling overwhelmed.

##### Acceptance Criteria

1. WHEN the user encounters a metric for the second time THEN the system SHALL present a friendly espresso bean character that provides simple, jargon-free explanations
2. WHEN the user taps on a metric field THEN the bean character SHALL appear with a brief, contextual explanation
3. WHEN the user views brew ratio THEN the bean character SHALL explain it as "coffee in : espresso out" with a friendly tone
4. WHEN the user views extraction time THEN the bean character SHALL provide encouraging guidance about timing without overwhelming detail
5. WHEN the user requests more information THEN the bean character SHALL offer optional deeper explanations
6. IF metrics fall outside typical ranges THEN the bean character SHALL provide gentle, non-alarming suggestions

#### Requirement 5: Extraction Fundamentals with Bean Character

**User Story:** As a beginner who has recorded some shots, I want to learn the difference between over-extracted and under-extracted shots through friendly guidance, so that I can identify taste issues without feeling intimidated.

##### Acceptance Criteria

1. WHEN the user accesses taste training THEN the bean character SHALL present simple visual examples with encouraging explanations
2. WHEN the user learns about under-extraction THEN the bean character SHALL explain "sour = grind finer" with a friendly, memorable approach
3. WHEN the user learns about over-extraction THEN the bean character SHALL explain "bitter = grind coarser" with positive reinforcement
4. WHEN the user completes basic training THEN the bean character SHALL offer optional practice exercises without pressure
5. WHEN the user identifies tastes correctly THEN the bean character SHALL celebrate with encouraging feedback
6. IF the user wants to skip training THEN the system SHALL allow progression without forcing educational content

#### Requirement 6: Quick Taste Recording Integration

**User Story:** As a user recording shots, I want simple taste buttons integrated into the shot recording flow, so that I can quickly capture taste impressions and receive immediate adjustment suggestions.

##### Acceptance Criteria

1. WHEN the user records a shot THEN the system SHALL provide prominent taste buttons (sour, bitter, balanced)
2. WHEN the user selects "sour" THEN the bean character SHALL suggest grinding finer and explain the reasoning
3. WHEN the user selects "bitter" THEN the bean character SHALL suggest grinding coarser and explain the reasoning
4. WHEN the user selects "balanced" THEN the bean character SHALL celebrate the success and encourage consistency
5. WHEN the user makes taste selections over time THEN the system SHALL track improvement patterns
6. IF the user doesn't select a taste THEN the system SHALL still save the shot data without forcing taste input

#### Requirement 7: Progressive Learning with Bean Character

**User Story:** As a developing home barista, I want the bean character to offer additional tips and knowledge over time, so that I can learn at my own pace without initial overwhelm.

##### Acceptance Criteria

1. WHEN the user has recorded several shots THEN the bean character SHALL occasionally offer gentle tips based on their patterns
2. WHEN the user demonstrates consistency THEN the bean character SHALL celebrate progress and suggest optional next steps
3. WHEN the user shows improvement THEN the bean character SHALL provide encouraging feedback without overwhelming detail
4. WHEN the user requests help THEN the bean character SHALL appear with contextual, friendly guidance
5. WHEN the user prefers minimal guidance THEN the system SHALL allow the bean character to be minimized or disabled
6. IF the user wants to learn more THEN the bean character SHALL provide opt-in educational content
