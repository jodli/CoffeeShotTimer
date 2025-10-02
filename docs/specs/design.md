# Coffee Shot Timer - Design Principles

---

## Design Philosophy

### Intelligence-First Visual Design

Our interface reflects our product philosophy: **guide, don't overwhelm**.

- **Show what matters now** - UI adapts to the user's current cognitive state
- **Progressive complexity** - Beginners see simplicity, experts find power
- **Invisible until needed** - Advanced features hidden but accessible
- **Focus over features** - Screen space allocated by actual usage frequency

---

## Core Design Principles

### 1. Context-Driven Interface

The UI changes based on what the user is doing:

**During extraction (focus state):**

- Massive timer dominates (320dp)
- Minimal distractions
- No scrolling required
- Current settings visible but not demanding attention

**After extraction (reflection state):**

- Taste feedback appears
- Suggestions take center stage
- Actions are clear and prominent

**At rest (preparation state):**

- Ready to start immediately
- Previous settings already applied
- Quick access to bean switching

### 2. Cognitive Load Reduction

**One thing at a time:**

- Each screen has a single primary purpose
- Secondary actions are de-emphasized but accessible
- No competing calls-to-action

**Minimize decisions:**

- Pre-select based on context (e.g., taste feedback from extraction time)
- Apply smart defaults (last used bean, suggested settings)
- Skip unnecessary steps

### 3. Touch-First for Real Brewing

**Large, forgiving targets:**

- Minimum 44dp touch targets
- Generous spacing between interactive elements
- Primary actions use full-width buttons (56dp height)

**Coffee-stained tolerance:**

- Works with wet or partially obscured fingers
- No tiny controls or precise gestures
- One-handed operation where possible

**Physical feedback:**

- Haptics for critical actions (timer start/stop)
- Visual state changes on touch
- Immediate response to all interactions

### 4. Bean-Centric Organization

**Everything revolves around beans:**

- Bean selector prominent in header
- Each bean remembers its settings
- Switching beans = context switch
- Freshness indicators always visible

**Visual hierarchy:**

- Bean name: Primary information
- Days since roast: Secondary with color coding
- Grinder setting: Contextual display

### 5. Clarity Over Decoration

**Purposeful visuals:**

- Color communicates meaning (extraction time quality)
- Icons support text, never replace it
- Spacing creates hierarchy
- Animation indicates state changes

**No clutter:**

- Remove everything that doesn't serve the current task
- White space is a feature, not wasted space
- Typography creates structure

---

## Visual Design System

### Color & Meaning

**Primary palette (coffee-inspired):**

- **Caramel brown:** Primary actions, branding
- **Soft teal:** Secondary actions, accents
- **Cream:** Backgrounds, surfaces
- **Warm red:** Errors, destructive actions

**Semantic colors (extraction feedback):**

- **Green (20-30s):** Optimal extraction time
- **Yellow (<20s):** Too fast, likely sour
- **Red (>30s):** Too slow, likely bitter

**Freshness indicators (bean age):**

- **Green (7-21 days):** Peak flavor window
- **Yellow (3-6 or 22-30 days):** Still good
- **Red (<3 or >30 days):** Past prime

### Typography

**Scale (Material 3):**

- **Screen titles:** `headlineMedium` (bold) - Main screens
- **Section headers:** `titleMedium` - Card titles, groups
- **Body text:** `bodyMedium` - Primary content
- **Labels:** `labelMedium` - Input labels, small actions
- **Captions:** `bodySmall` - Metadata, timestamps

**Timer exception:**

- **48sp** for time display (massive, readable from distance)

**Responsive scaling:**

- Landscape: Reduce by one type scale step
- Support dynamic text sizing up to 200%

### Spacing

**Base scale (4dp grid):**

- **4dp:** Tight groupings
- **8dp:** Related elements
- **16dp:** Standard padding, card internals
- **24dp:** Section separation
- **32dp:** Major screen divisions

**Standard patterns:**

- Screen horizontal padding: 16dp
- Screen vertical spacing: 16dp
- Card internal padding: 16dp
- Bottom padding for navigation: 24dp
- Touch target minimum: 44dp

### Components

**Cards (`CoffeeCard`):**

- 12dp corner radius
- Light elevation
- 16dp internal padding
- Full width by default
- Optional `onClick` for interactive cards

**Buttons:**

- `CoffeePrimaryButton`: Main actions (caramel)
- `CoffeeSecondaryButton`: Alternative actions (teal)
- FAB: Key floating actions
- Minimum height: 44dp
- Full-width for primary screen actions

**Input fields (`CoffeeTextField`):**

- Full width
- Rounded corners
- Optional leading/trailing icons
- Integrated error text
- Small spacing between field and error

**Timer (custom):**

- Circular progress indicator
- Large time display (48sp)
- Color-coded by extraction quality
- Full circle is tappable
- Haptic feedback on state change

**Bottom sheets:**

- Modal for selections and settings
- Rounded top corners
- Drag handle
- Full width content with 16dp padding

---

## Layout Patterns

### Recording Screen (Intelligence-First)

**Structure (496dp total):**

```
Header (70dp)
  ├─ Bean selector dropdown
  ├─ Days since roast
  └─ Current grinder setting (tap to adjust)

Timer (320dp)
  └─ Massive circular display with time

Weights (50dp)
  ├─ Coffee in → Coffee out (ratio)
  └─ [-] [+] buttons for quick adjustment

Action (56dp)
  └─ Full-width "Save Shot" button
```

**Key principles:**

- No scrolling
- Timer visible 100% of time
- Settings displayed, not demanding attention
- One clear primary action

### Feedback & Suggestion Flow

**Taste feedback screen:**

- Large, tappable emotion buttons
- Pre-selected based on extraction time
- Optional modifiers (weak/strong)
- Skip option for rushed users

**Suggestion screen:**

- Clear explanation of issue
- Specific recommendation
- Reasoning ("Why")
- Two clear actions: Apply or Skip

### Management Screens

**List patterns:**

- Each item in a `CoffeeCard`
- Icon + title + optional support text
- Trailing chevron for navigation
- Full-width touch target
- Consistent height (44dp+)

**Forms:**

- Group related inputs
- Consistent spacing (16dp between fields)
- Inline validation
- Clear error messages
- Action buttons at bottom

---

## Interaction Standards

### States & Feedback

**Loading:**

- Use `LoadingIndicator` component
- Disable controls during async operations
- Show progress when possible
- Never leave user wondering

**Empty states:**

- Use `EmptyState` component
- Clear message explaining why empty
- Suggested action to populate
- Friendly, encouraging tone

**Errors:**

- Red text with error icon
- Specific, actionable message
- Retry option when appropriate
- Don't blame the user

**Success:**

- Brief confirmation (toast or inline)
- Use primary color
- Positive, encouraging language
- Auto-dismiss or single tap to dismiss

### Haptic Feedback

**Light haptic:**

- Timer start
- Button taps
- Selection changes

**Medium haptic:**

- Timer stop
- Destructive actions (delete, archive)
- Important confirmations

### Navigation

**Main screens (bottom nav):**

- No top app bar
- Content starts immediately
- Active nav item indicates location

**Sub-screens:**

- Top app bar with title
- Back button (auto-mirrored arrow)
- Optional action buttons
- Confirm on unsaved changes

**Modals & sheets:**

- Bottom sheet for selections/adjustments
- Dialog for confirmations
- Clear Cancel/Confirm buttons

---

## Responsive Design

### Portrait (Primary)

**Recording screen:**

- Single column
- Vertical stack
- Large timer
- Full-width actions

**Other screens:**

- Single column layout
- Cards stack vertically
- Actions at bottom

### Landscape

**Onboarding/Education:**

- Two columns (30% / 70%)
- Visual/branding left
- Content/actions right
- Typography scales down one step

**Recording screen:**

- Same layout as portrait
- Timer remains dominant
- Optimized for quick glances

**General guidance:**

- Enable vertical scrolling when needed
- Maintain spacing scale
- Preserve hierarchy

---

## Coffee-Specific Patterns

### Bean Display

**Primary presentation:**

- Bean name (bold, prominent)
- Days since roast (secondary)
- Freshness indicator (color-coded)
- Grinder setting (contextual)

**In lists:**

- Name + roast date first line
- Freshness + shots count second line
- Trailing chevron or action

### Shot Metrics

**Brew ratio:**

- Format: "18g → 36g (1:2.0)"
- Ratio prominent
- Color-coded ranges:
  - Green: 1:1.5 - 1:2.5
  - Yellow: Outside optimal range

**Extraction time:**

- Under 60s: "27s"
- Over 60s: "1:15"
- Color-coded by quality

**Weight display:**

- Always include "g" unit
- Use decimal precision (one place)
- +/- buttons for adjustments

### Quality Indicators

**Taste feedback:**

- Large emoji buttons
- Clear labels (Sour, Perfect, Bitter)
- Optional modifiers
- Pre-selection based on data

**Freshness:**

- Small colored dots
- Consistent size
- Tight grouping
- Tooltip on long-press

---

## Accessibility

### Visual Accessibility

**Contrast:**

- Meet WCAG AA standards minimum
- Never rely on color alone
- Use icons + text
- Test in dark mode

**Text sizing:**

- Support dynamic type
- Minimum touch targets
- Maintain hierarchy at all sizes

### Interactive Accessibility

**Content descriptions:**

- All icons and images
- Format: `cd_[element]_[context]`
- Clear, descriptive
- Contextually appropriate

**Screen readers:**

- Semantic HTML/Compose
- Proper heading hierarchy
- Announce state changes
- Clear focus order

**Touch accessibility:**

- 44dp minimum targets
- Spacing between controls
- No precise gestures required
- Support external keyboards

---

## Content & Tone

### Writing Principles

**Concise:**

- 2-3 sentences maximum
- Remove unnecessary words
- Get to the point

**User-focused:**

- Use "you" and "your"
- Focus on benefits
- Active voice

**Helpful:**

- Explain why before how
- Suggest next steps
- Encouraging, never judgmental

### Button Text

**Action-oriented:**

- Start with verbs
- 2-4 words maximum
- Specific, not generic
- Title Case

**Examples:**

- "Save Shot" (not "Save")
- "Apply Suggestion" (not "OK")
- "Maybe Later" (not "Cancel")

### Success Messages

**Structure:**

- Positive lead-in
- What was achieved
- Next step (optional)

**Example:**

- "Great! Bean saved. Start recording your first shot."

### Error Messages

**Structure:**

- What went wrong
- Why it matters
- What to do next

**Example:**

- "Couldn't save shot. Check your connection and try again."

---

## Implementation Guidelines

### Component Reuse

**Always use existing components:**

- `CoffeeCard` for cards
- `CoffeePrimaryButton` / `CoffeeSecondaryButton`
- `CoffeeTextField` for inputs
- `LoadingIndicator` / `EmptyState`

**Extend, don't duplicate:**

- Add props for variations
- Keep interfaces consistent
- Document new patterns

### State Management

**Consistent patterns:**

- Clear loading states
- Proper error handling
- Success feedback
- Predictable behavior

### Performance

**Optimize recomposition:**

- `remember` for expensive work
- Stable parameters
- Proper list keys
- Avoid unnecessary updates

### Testing

**Testable structure:**

- Use semantic properties
- Predictable component structure
- All interactive elements testable
- Consistent naming

---

## String Resources

### Naming Conventions

**Prefixes:**

- `intro_*` - Onboarding content
- `bean_education_*` - Bean-related education
- `bean_creation_success_*` - Bean creation flow
- `onboarding_*` - First-time user experience
- `equipment_setup_*` - Equipment configuration
- `button_*` - All buttons
- `cd_*` - Content descriptions

**Structure:**

- Descriptive, not abbreviated
- Context-specific
- Group related strings

### Localization

**Current languages:**

- English (primary)
- German

**Guidelines:**

- Maintain consistent terminology
- Validate translations for accuracy
- Test UI with translated text
- Preserve tone and meaning
- Consider cultural context for coffee terms

---

## The Design Review Checklist

Before implementing a new screen or feature, verify:

### Intelligence-First

- [ ] Does it show what matters NOW?
- [ ] Is complexity hidden until needed?
- [ ] Does it guide rather than just display?

### Usability

- [ ] Works with coffee-stained fingers?
- [ ] All touch targets ≥44dp?
- [ ] No scrolling during critical actions?
- [ ] Clear primary action?

### Visual Consistency

- [ ] Uses design system colors?
- [ ] Typography follows scale?
- [ ] Spacing uses 4dp grid?
- [ ] Components from library?

### Content

- [ ] Concise and focused?
- [ ] User-benefit language?
- [ ] Active voice?
- [ ] Proper string resources?

### Accessibility

- [ ] Sufficient contrast?
- [ ] Content descriptions?
- [ ] Screen reader tested?
- [ ] Supports large text?

### Responsiveness

- [ ] Works in landscape?
- [ ] Handles small screens?
- [ ] Typography scales?
- [ ] Maintains hierarchy?
