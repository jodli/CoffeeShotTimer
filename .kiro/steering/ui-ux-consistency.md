# UI/UX Consistency Guidelines for Coffee Shot Timer (Condensed)

## Overview
Cohesive, coffee-inspired UI across screens. Reuse shared components, spacing, and typography. Favor clarity, accessibility, and responsive behavior.

## Design System
- Spacing (LocalSpacing): 4, 8, 16, 24, 32; touch target 44; card/screen padding 16
- Colors (Material 3): Primary caramel; secondary soft teal; surface/background cream; error red

## Components
- Cards: Use CoffeeCard; 12dp radius; light elevation; 16dp internal padding; full width; optional onClick
- Buttons: CoffeePrimary for main; CoffeeSecondary for alternatives; FAB for key actions; min height 44dp
- Text Input: CoffeeTextField; full width; rounded; icons optional; integrated error text with small spacing
- Typography: Screen titles headlineMedium (bold); use headlineSmall in landscape. Sections titleMedium; body bodyMedium; labels labelMedium; captions bodySmall
- Loading/Empty: Use LoadingIndicator and EmptyState with clear message and consistent spacing

## Layout
- Screen structure: optional TopAppBar; content with 16dp horizontal padding, 16dp vertical spacing; add 24dp bottom padding for nav clearance
- Responsive patterns:
  - Onboarding/Education portrait: centered single column; actions at bottom
  - Landscape: two columns ~30%/70% (visual/branding vs content/actions); consistent landscape spacing; enable vertical scroll for overflow
  - Typography scales down in landscape (headline → smaller, title/body/label one step down)
- Card patterns: Information (icon + title + content); Forms (group inputs, consistent spacing, inline validation); List items (left info, right actions/meta, ripple)
- Management screens (More): Each feature as a tappable CoffeeCard row with icon, title, optional support text, trailing chevron; use touch-target heights; concise labels

## Interaction
- Touch targets: minimum 44dp; maintain separation between adjacent targets
- Feedback: ripples, state color changes; subtle press scale
- Haptics: light for start; medium for stop/destructive
- States: show loading; disable during async; clear error messages with retry when useful; success uses primary color and short confirmations

## Navigation
- Main navigation screens (RecordShot, ShotHistory, BeanManagement, More): no TopAppBar; rely on active nav item; content starts immediately
- Sub-screens (About, Equipment Settings, Add/Edit Bean, Shot Details): TopAppBar with bold title, back button, optional actions
- Onboarding/Modal: contextual TopAppBar; appropriate navigation (back/skip/progress)
- Back button standards: AutoMirrored ArrowBack icon; contentDescription cd_back; navigate back via pop; confirm on unsaved changes
- System UI: edge-to-edge; let containers handle status bars (Scaffold or LandscapeContainer); avoid manual statusBarsPadding; test both orientations
- Transitions/Sheets/Dialogs: standard navigation patterns; ModalBottomSheet for selections; AlertDialog for confirmations with clear titles and Cancel/Confirm layout

## Accessibility
- Content descriptions for icons and interactive elements; proper semantics for screen readers
- Contrast: maintain accessible ratios; don’t rely on color alone; add text/icons
- Touch accessibility: 44dp targets, spacing between controls; support large text/display scaling

## Coffee-Specific Patterns
- Bean info: primary name; days since roast with freshness; color-coded freshness; grinder settings as secondary
- Shot metrics: prominent brew ratio (1:X.X); color-coded ranges; time seconds <60, MM:SS otherwise; weight with “g”
- Timer: large, prominent; color-coded progress; tappable; haptic interactions
- Quality indicators: small dots; primary for good, outline for neutral/poor; consistent size and tight grouping

## Strings
- Naming: intro_*, bean_education_*, bean_creation_success_*, onboarding_*, equipment_setup_*; buttons: button_*; content descriptions: cd_* (cd_[element]_[context])
- Content guidelines: concise (2–3 sentences), user-benefit focused, active voice (“you”), explain why before how, appropriate coffee terms
- Success messages: positive lead-in + achievement + next step; keep tone encouraging
- Button text: action verbs; 2–4 words; specific; Title Case
- Localization: ensure consistent terms and feature meaning across locales; validate translations for accuracy and UI alignment; maintain tone

## Implementation
- Reuse/extend existing components; keep prop interfaces consistent
- Responsive: LandscapeContainer for onboarding/education; 30/70 landscape weight; scale type down one step; use landscape spacing; preserve hierarchy
- State mgmt: consistent patterns; clear user feedback for all actions
- Performance: remember for expensive work; stable params; proper list keys; minimize recomposition
- Testing: semantic properties; predictable structure; ensure all interactives are testable
- Structure & naming: group related components; descriptive file names; split complex pieces; use “Coffee” prefix; clear function/variable names; consistent params
- Documentation: KDoc for complex components; keep this guide updated as patterns evolve
