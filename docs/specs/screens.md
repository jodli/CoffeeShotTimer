# Coffee Shot Timer - Screen Structure

**Overview of all app screens and navigation flow**

---

## 📱 Screen Hierarchy

### First-Time User Flow
```
IntroductionScreen
    ↓
EquipmentSetupWelcomeScreen
    ↓
EquipmentSetupFlowScreen
    ├→ GrinderSetupStepScreen
    └→ BasketSetupStepScreen
    ↓
EquipmentSetupSummaryScreen
    ↓
GuidedBeanCreationScreen (3 phases)
    ├→ Education phase
    ├→ AddEditBeanScreen (form)
    └→ Success phase
    ↓
RecordShotScreen (main app)
```

### Main App Navigation (Bottom Nav)
```
RecordShotScreen ←→ ShotHistoryScreen ←→ BeanManagementScreen ←→ MoreScreen
     (Timer)           (History)            (Beans)             (Settings)
```

---

## 🗺️ Navigation Map

### Core Recording Flow
```
RecordShotScreen
    ├→ BeanManagementScreen (bean selector)
    │   ├→ AddEditBeanScreen (add/edit bean)
    │   └→ GuidedBeanCreationScreen (first bean)
    │
    └→ ShotDetailsScreen (after save)
        ├→ Previous/Next shot navigation
        └→ Edit taste feedback
```

### History & Analysis Flow
```
ShotHistoryScreen
    ├→ Filter dialog (bean, date, settings)
    ├→ Analysis view (toggle)
    └→ ShotDetailsScreen (tap shot)
        ├→ Edit notes
        ├→ Edit taste feedback
        └→ Delete shot
```

### Bean Management Flow
```
BeanManagementScreen
    ├→ AddEditBeanScreen (create new bean)
    │   ├→ Photo capture/select
    │   └→ Date picker
    │
    ├→ AddEditBeanScreen (edit existing)
    └→ RecordShotScreen (use for shot)
```

### Settings Flow
```
MoreScreen
    ├→ GrinderSettingsScreen
    │   └→ Configure grinder range
    │
    ├→ BasketSettingsScreen
    │   └→ Configure basket size
    │
    ├→ AboutScreen
    │   ├→ GitHub link
    │   ├→ Privacy Policy
    │   └→ Debug menu (hidden)
    │
    └→ Send Feedback (email)
```

---

## 📋 Screen Details

### 🏠 Main App Screens (Bottom Navigation)

#### RecordShotScreen
**Purpose:** Record espresso shots with timer  
**Key features:**
- Massive timer (target: 320dp)
- Bean selector
- Weight inputs (in/out)
- Grinder setting
- Save shot → taste feedback dialog

**Navigation:**
- → BeanManagementScreen (select bean)
- → ShotDetailsScreen (after save)
- Bottom nav to other main screens

---

#### ShotHistoryScreen
**Purpose:** View past shots and analytics  
**Key features:**
- List of recorded shots
- Analysis view toggle
- Filtering (bean, date, settings)
- Success scoring

**Navigation:**
- → ShotDetailsScreen (tap shot)
- → Filter dialog
- Bottom nav to other main screens

---

#### BeanManagementScreen
**Purpose:** Manage coffee beans  
**Key features:**
- List of beans (active/inactive)
- Search and filter
- Bean cards with freshness indicators
- Performance metrics (planned)

**Navigation:**
- → AddEditBeanScreen (add/edit)
- → RecordShotScreen (use for shot)
- Bottom nav to other main screens

---

#### MoreScreen
**Purpose:** Settings and additional options  
**Key features:**
- Equipment settings link
- Basket settings link
- About link
- Send feedback

**Navigation:**
- → GrinderSettingsScreen
- → BasketSettingsScreen
- → AboutScreen
- Bottom nav to other main screens

---

### 📝 Detail & Form Screens

#### ShotDetailsScreen
**Purpose:** Detailed view of single shot  
**Key features:**
- Shot overview (ratio, time, taste)
- Bean information
- Parameters
- Grind recommendations
- Prev/next navigation
- Edit notes
- Edit taste feedback

**Navigation:**
- ← Back to ShotHistoryScreen
- ↔ Previous/Next shot
- Modal: Taste feedback editor
- Delete → back to history

---

#### AddEditBeanScreen
**Purpose:** Create or edit bean entry  
**Key features:**
- Name, roast date, notes
- Photo (camera/gallery)
- Active status toggle
- Validation

**Navigation:**
- ← Back to BeanManagementScreen
- Modal: Date picker
- Modal: Photo action sheet
- Save → back to list

---

### ⚙️ Settings Screens

#### GrinderSettingsScreen
**Purpose:** Configure grinder parameters  
**Key features:**
- Scale min/max
- Step size
- Presets for common grinders
- Validation

**Navigation:**
- ← Back to MoreScreen
- Save → back to More

---

#### BasketSettingsScreen
**Purpose:** Configure portafilter basket  
**Key features:**
- Coffee in min/max
- Coffee out min/max
- Presets (15g, 18g, 21g)
- Validation

**Navigation:**
- ← Back to MoreScreen
- Save → back to More

---

#### AboutScreen
**Purpose:** App information and links  
**Key features:**
- App version
- Description
- Philosophy statement
- GitHub link
- Privacy Policy link
- Debug menu (hidden, dev only)

**Navigation:**
- ← Back to MoreScreen
- External: GitHub, Privacy Policy

---

### 🎓 Onboarding Screens

#### IntroductionScreen
**Purpose:** Welcome first-time users  
**Key features:**
- App value proposition
- Key benefits
- Get started / Skip

**Navigation:**
- → EquipmentSetupWelcomeScreen
- Skip → RecordShotScreen

---

#### EquipmentSetupWelcomeScreen
**Purpose:** Introduction to equipment setup  
**Key features:**
- Explain why setup matters
- Set expectations

**Navigation:**
- → EquipmentSetupFlowScreen
- Skip → GuidedBeanCreationScreen

---

#### EquipmentSetupFlowScreen
**Purpose:** Orchestrate setup steps  
**Key features:**
- Step navigation
- Progress tracking

**Navigation:**
- → GrinderSetupStepScreen
- → BasketSetupStepScreen
- → EquipmentSetupSummaryScreen

---

#### GrinderSetupStepScreen
**Purpose:** Configure grinder during onboarding  
**Key features:**
- Same as GrinderSettingsScreen
- Onboarding context

**Navigation:**
- ← Back to EquipmentSetupFlowScreen
- → Next step (Basket)

---

#### BasketSetupStepScreen
**Purpose:** Configure basket during onboarding  
**Key features:**
- Same as BasketSettingsScreen
- Onboarding context

**Navigation:**
- ← Back to previous step
- → EquipmentSetupSummaryScreen

---

#### EquipmentSetupSummaryScreen
**Purpose:** Review equipment configuration  
**Key features:**
- Show grinder settings
- Show basket settings
- Confirm or edit

**Navigation:**
- Edit → back to specific step
- Continue → GuidedBeanCreationScreen

---

#### GuidedBeanCreationScreen
**Purpose:** Help user create first bean  
**3 Phases:**
1. **Education:** Why track beans?
2. **Form:** AddEditBeanScreen (onboarding mode)
3. **Success:** Celebration + next steps

**Navigation:**
- Phase 1: Continue or Skip
- Phase 2: AddEditBeanScreen (embedded)
- Phase 3: → RecordShotScreen
- Skip → RecordShotScreen

---

## 🔄 State-Based Navigation

### Conditional Routes
- **First launch:** IntroductionScreen → Equipment Setup → Bean Creation
- **Existing user, no beans:** GuidedBeanCreationScreen
- **Existing user, incomplete setup:** Equipment Setup
- **Normal flow:** RecordShotScreen (main app)

### Modal/Dialog Screens
- **Taste Feedback:** Dialog from RecordShotScreen
- **Date Picker:** Dialog from AddEditBeanScreen
- **Photo Actions:** Bottom sheet from AddEditBeanScreen
- **Bean Selector:** Bottom sheet from RecordShotScreen
- **Shot Filter:** Dialog from ShotHistoryScreen
- **Delete Confirmation:** Dialog from ShotDetailsScreen

---

## 📊 Screen Count Summary

**Total screens:** 16

**By category:**
- Main app: 4 (bottom nav)
- Detail/Form: 2
- Settings: 3
- Onboarding: 7

**By priority:**
- P1 (Core): 3 (Record, History, Details)
- P2 (Important): 7 (Bean management, settings)
- P3 (Support): 6 (About, onboarding polish)

---

## 🎯 Key User Journeys

### 1. Record First Shot (New User)
```
Intro → Equipment Setup → Bean Creation → Record Shot → Taste Feedback
```

### 2. Daily Brewing (Regular User)
```
Record Shot → Save → Taste Feedback → Apply Recommendation → Done
```

### 3. Dial In New Bean
```
Bean Management → Add Bean → Record Shot → Taste Feedback → 
Apply Recommendation → Record Shot (repeat) → Dialed In
```

### 4. Review Progress
```
History → View Analysis → Filter by Bean → See Trends
```

### 5. Change Equipment
```
More → Grinder Settings → Update Range → Save
```

---

## 🗂️ Screen Grouping

### Primary Flow (80% usage)
- RecordShotScreen
- ShotHistoryScreen
- ShotDetailsScreen

### Bean Management (15% usage)
- BeanManagementScreen
- AddEditBeanScreen

### Configuration (5% usage)
- MoreScreen
- GrinderSettingsScreen
- BasketSettingsScreen
- AboutScreen

### One-Time Setup (<1% usage)
- All onboarding screens
- GuidedBeanCreationScreen

---

## 🔧 Technical Notes

**Bottom Navigation:**
- Persistent across main 4 screens
- State preserved on navigation
- Deep links to specific tabs

**Modal Presentations:**
- Bottom sheets for selections
- Full-screen for forms
- Dialogs for confirmations

**Responsive Layouts:**
- All screens support portrait/landscape
- `LandscapeContainer` wrapper used
- Adaptive spacing and sizing

**Screen State:**
- ViewModels handle business logic
- UI state collected via StateFlow
- Navigation via NavController

---

**This document provides a high-level overview. For detailed screen requirements, see individual analysis documents in `docs/screen_analysis/`**
