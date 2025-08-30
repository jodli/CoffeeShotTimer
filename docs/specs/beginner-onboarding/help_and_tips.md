# Help & Tips Feature - Implementation Plan

## Overview

Create a scalable, maintainable help system that reuses educational content from onboarding while supporting future expansion with taste training, troubleshooting, and advanced guides. This feature provides permanent access to educational content after onboarding, supporting continuous learning and systematic brewing improvement.

## Core Principles

1. **Single Source of Truth**: All educational content stored in one place
2. **No Duplication**: Content defined once, used everywhere (onboarding and help)
3. **Reusable Components**: Share UI components between onboarding and help screens
4. **Progressive Disclosure**: Start simple, reveal complexity as needed
5. **Extensible Structure**: Easy to add new content types (videos, troubleshooting, taste training)
6. **Offline-First**: All core educational content available without internet

## Architecture

### Data Layer

#### Educational Content Model

```kotlin
// data/educational/EducationalContent.kt
sealed class EducationalContent {
    data class TextContent(
        val id: String,
        val titleResId: Int,
        val descriptionResId: Int,
        val category: ContentCategory,
        val tags: List<ContentTag>,
        val iconResId: Int? = null
    ) : EducationalContent()

    data class GuideContent(
        val id: String,
        val titleResId: Int,
        val steps: List<GuideStep>,
        val category: ContentCategory,
        val tags: List<ContentTag>
    ) : EducationalContent()

    data class TipContent(
        val id: String,
        val titleResId: Int,
        val tipResId: Int,
        val category: ContentCategory,
        val priority: TipPriority
    ) : EducationalContent()

    // Future expansion
    data class VideoContent(...) : EducationalContent()
    data class InteractiveContent(...) : EducationalContent()
    data class TasteTrainingContent(...) : EducationalContent()
}

enum class ContentCategory {
    GETTING_STARTED,
    BREWING_BASICS,
    EQUIPMENT,
    BEAN_MANAGEMENT,
    TROUBLESHOOTING,
    TASTE_TRAINING,
    ADVANCED_TECHNIQUES
}

enum class ContentTag {
    EXTRACTION,
    GRINDER,
    RATIO,
    TIMING,
    FRESHNESS,
    WORKFLOW,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
```

#### Repository Structure

```kotlin
// data/repository/EducationalContentRepository.kt
interface EducationalContentRepository {
    fun getContentByCategory(category: ContentCategory): Flow<List<EducationalContent>>
    fun getContentByTags(tags: List<ContentTag>): Flow<List<EducationalContent>>
    fun getOnboardingContent(): Flow<List<EducationalContent>>
    fun getQuickTips(): Flow<List<TipContent>>
    fun searchContent(query: String): Flow<List<EducationalContent>>
}
```

### Domain Layer

#### Use Cases

- `GetEducationalContentUseCase` - Fetches content with filtering
- `GetOnboardingContentUseCase` - Provides content for onboarding flows
- `GetHelpContentByCategoryUseCase` - Category-specific content
- `GetQuickTipsUseCase` - Priority-ordered tips
- `SearchHelpContentUseCase` - Text search across all content

### UI Layer

#### Shared Components

```
ui/components/educational/
├── EducationalCard.kt           # Base card for displaying content
├── EducationalSlide.kt          # For carousel-style content
├── ExpandableHelpCard.kt        # Collapsible detailed content
├── ContentCategorySelector.kt   # Category filter chips
├── QuickTipCard.kt              # Compact tip display
└── GuideStepList.kt             # Step-by-step guide display
```

#### Help & Tips Screen Structure

```
HelpAndTipsScreen
├── Search Bar (future)
├── Category Tabs/Chips
│   ├── Getting Started
│   ├── Brewing Basics
│   ├── Equipment
│   ├── Bean Management
│   └── More (Troubleshooting, Taste Training - future)
├── Content Sections
│   ├── Quick Actions
│   │   ├── Re-run Equipment Setup
│   │   ├── Review App Introduction
│   │   └── Practice Bean Creation
│   ├── Featured Guides
│   │   ├── Why Track Your Beans?
│   │   ├── Understanding Extraction
│   │   └── Your Workflow, Your Way
│   └── Tips & Best Practices
│       └── Context-aware tips based on user's experience
```

## Implementation Phases

### Phase 1: Foundation (Priority: Highest)

1. **Create data models** for educational content
2. **Build repository** with hardcoded initial content
3. **Implement core use cases**
4. **Extract strings** from existing onboarding to shared resources

### Phase 2: UI Components (Priority: High)

1. **Build reusable educational components**
2. **Create ExpandableHelpCard** for detailed content
3. **Implement ContentCategorySelector**
4. **Build GuideStepList** for multi-step guides

### Phase 3: Help & Tips Screen (Priority: High)

1. **Create HelpAndTipsScreen** with category filtering
2. **Add navigation** from MoreScreen
3. **Wire up** in AppNavigation
4. **Implement** ViewModel with content loading

### Phase 4: Content Migration (Priority: Medium)

1. **Refactor IntroductionScreen** to use shared content
2. **Refactor GuidedBeanCreationScreen** education phase
3. **Update EquipmentSetupFlowScreen** explanations
4. **Ensure** no string duplication

### Phase 5: Re-entrant Flows (Priority: Medium)

1. **Add "tutorial mode"** parameter to onboarding screens
2. **Implement Equipment Reconfiguration** (preserves existing data)
3. **Add Review Introduction** flow (read-only)
4. **Create Practice Bean Creation** (doesn't save)

### Phase 6: Future Expansion (Priority: Low)

1. **Taste Training Module** - Interactive sour vs bitter exercises
2. **Troubleshooting Guide** - Common problems and solutions
3. **Video Content Support** - Links to tutorial videos
4. **Personalized Tips** - Based on user's shot history

## File Structure

```
app/src/main/java/com/jodli/coffeeshottimer/
├── data/
│   ├── educational/
│   │   ├── EducationalContent.kt
│   │   ├── ContentCategory.kt
│   │   ├── ContentTag.kt
│   │   └── ContentProvider.kt
│   └── repository/
│       └── EducationalContentRepository.kt
├── domain/
│   └── usecase/
│       ├── GetEducationalContentUseCase.kt
│       ├── GetHelpContentByCategoryUseCase.kt
│       ├── GetQuickTipsUseCase.kt
│       └── SearchHelpContentUseCase.kt
├── ui/
│   ├── components/
│   │   └── educational/
│   │       ├── EducationalCard.kt
│   │       ├── ExpandableHelpCard.kt
│   │       ├── ContentCategorySelector.kt
│   │       ├── QuickTipCard.kt
│   │       └── GuideStepList.kt
│   ├── screens/
│   │   └── HelpAndTipsScreen.kt
│   └── viewmodel/
│       └── HelpAndTipsViewModel.kt
```

## String Resources Strategy

### Hierarchical Organization

```xml
<!-- res/values/strings_educational.xml -->
<!-- Base educational content - shared across app -->
<string name="edu_extraction_title">Understanding Extraction</string>
<string name="edu_extraction_description">Learn how extraction time affects taste...</string>
<string name="edu_extraction_tip_under">If sour, grind finer or extract longer</string>
<string name="edu_extraction_tip_over">If bitter, grind coarser or extract shorter</string>

<string name="edu_beans_freshness_title">Coffee Freshness Matters</string>
<string name="edu_beans_freshness_description">Beans peak 4-21 days after roasting...</string>

<string name="edu_workflow_title">Your Workflow, Your Way</string>
<string name="edu_workflow_description">No rigid steps to follow...</string>

<!-- Help-specific strings -->
<string name="help_quick_actions_title">Quick Actions</string>
<string name="help_featured_guides_title">Featured Guides</string>
<string name="help_search_placeholder">Search help topics...</string>
```

### Usage Example

```kotlin
// In onboarding
IntroSlide(
    title = stringResource(R.string.edu_workflow_title),
    description = stringResource(R.string.edu_workflow_description)
)

// In help screen - same strings
HelpCard(
    title = stringResource(R.string.edu_workflow_title),
    description = stringResource(R.string.edu_workflow_description)
)
```

## Navigation Updates

### Add to NavigationDestinations

```kotlin
object HelpAndTips : NavigationDestination {
    override val route = "help_and_tips"
    override val titleRes = R.string.title_help_and_tips

    // Optional: deep link to specific category
    const val categoryArg = "category"
    val routeWithArgs = "$route?$categoryArg={$categoryArg}"
}
```

### Update MoreScreen

```kotlin
// Add new navigation item
HelpAndTipsCard(
    onClick = onNavigateToHelpAndTips,
    icon = Icons.Default.Help,
    title = stringResource(R.string.title_help_and_tips),
    description = stringResource(R.string.help_description)
)
```

## Benefits

1. **No Duplication**: Content defined once, used everywhere
2. **Easy Updates**: Change content in one place affects all uses
3. **Consistent UX**: Same components and content across app
4. **Scalable**: Easy to add new content types and categories
5. **Maintainable**: Clear separation of concerns
6. **Future-Proof**: Structure supports planned features (taste training, troubleshooting)
7. **User Value**: Permanent access to helpful information improves learning

## Success Metrics

- Users can access help content after onboarding
- Reduced support requests about basic concepts
- Users successfully re-run equipment setup when needed
- Content is easily discoverable through categories
- No performance impact from content loading
- Smooth integration with existing onboarding flows

## Future Enhancements

### Milestone Integration (from milestones.md)

- **Taste Training Module**: Interactive exercises from Milestone 2
- **Troubleshooting Guide**: Solutions from Milestone 3
- **Progress Tracking**: Visual improvement metrics
- **Personalized Guidance**: Bean-specific tips

### Advanced Features (from idea.md)

- Audio descriptions for taste characteristics
- Quick taste buttons (sour, bitter, balanced)
- Equipment-specific starting parameters
- One-on-one virtual coaching (premium)

## Notes

- Keep initial implementation simple and focused
- Prioritize most-requested help topics based on user feedback
- Ensure offline capability for all core content
- Consider analytics to track which help topics are most viewed
- Maintain consistency with existing app design patterns
