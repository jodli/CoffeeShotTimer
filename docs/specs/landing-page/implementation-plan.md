# Coffee Shot Timer Landing Page - Complete Implementation Plan

## Overview
This document provides a complete implementation plan for transforming the current privacy policy page into a professional landing page for Coffee Shot Timer, maintaining brand consistency with the Android app.

## Project Context

### Current State
- **Location**: `/mnt/quickstuff/git/CoffeeShotTimer_landing_page/docs/`
- **Current Content**: Single `index.html` with privacy policy only
- **Deployment**: GitHub Pages at `https://[username].github.io/CoffeeShotTimer_landing_page/`

### Related Resources
- **App Source**: `/mnt/quickstuff/git/CoffeeShotTimer_main/`
- **Theme Colors**: `/mnt/quickstuff/git/CoffeeShotTimer_main/app/src/main/java/com/jodli/coffeeshottimer/ui/theme/Color.kt`
- **Design System**: `/mnt/quickstuff/git/CoffeeShotTimer_main/app/src/main/java/com/jodli/coffeeshottimer/ui/theme/Theme.kt`
- **UI Guidelines**: `/mnt/quickstuff/git/CoffeeShotTimer_main/.kiro/steering/ui-ux-consistency.md`
- **Store Descriptions**: `/mnt/quickstuff/git/CoffeeShotTimer_landing_page/docs/en-US/store-entry.md`
- **Initial Ideas**: `/mnt/quickstuff/git/CoffeeShotTimer_landing_page/docs/specs/landing-page/idea.md`

## Design System Specifications

### Color Palette
Extract from app's Material3 theme to maintain brand consistency:

```
Light Theme:
- Primary: #B8763D (Warm Caramel)
- Primary Container: #E8DDD0 (Soft Beige)
- Secondary: #7BA5A3 (Soft Teal)
- Background: #F2E6D3 (Creamy Beige)
- Surface: #FAF5F0 (Light Cream)
- On Background/Surface: #2D1B0F (Rich Espresso)
- On Surface Variant: #5D3A1A (Medium Coffee)
- Error: #D32F2F

Dark Theme:
- Primary: #E6B577 (Light Caramel)
- Primary Container: #5D3A1A (Medium Coffee)
- Secondary: #9BC5C3 (Warm Teal)
- Background: #1A0F08 (Deep Brown)
- Surface: #2D1B0F (Rich Espresso)
- On Background/Surface: #D4C4B0 (Dark Cream)
- On Surface Variant: #E6B577 (Light Caramel)
- Error: #D32F2F
```

### Spacing System
Based on 4dp/px base unit from app:
```
- xs: 4px (extraSmall)
- s: 8px (small)
- m: 16px (medium)
- l: 24px (large)
- xl: 32px (extraLarge)
- Card padding: 16px
- Screen padding: 16px
- Touch target minimum: 44px
```

### Typography Scale
Following Material Design hierarchy:
```
- Display: 48px/1.2 (hero headline)
- Headline Large: 32px/1.25 (section titles)
- Headline Medium: 24px/1.3 (card titles)
- Title Large: 20px/1.4
- Title Medium: 16px/1.5
- Body Large: 16px/1.5
- Body Medium: 14px/1.5
- Label Large: 14px/1.4 (buttons)
- Label Medium: 12px/1.4
- Body Small: 12px/1.5 (captions)
```

### Component Specifications

#### Cards (CoffeeCard style)
- Border radius: 16px
- Padding: 16px
- Background: var(--color-surface)
- Box shadow: 0 2px 4px rgba(0,0,0,0.1)
- Full width on mobile, constrained on desktop

#### Buttons
- Height: 44px minimum
- Border radius: 8px
- Padding: 12px 24px
- Font: 14px/1.4, medium weight
- Primary: filled with --color-primary
- Secondary: outlined with --color-primary border

#### Quality Indicators
- Size: 8px diameter circles
- Colors: primary (good), outline (neutral), error (poor)
- Used for feature checkmarks and status indicators

### Responsive Breakpoints
```
- Mobile: < 768px (single column)
- Tablet: 768px - 1024px (flexible columns)
- Desktop: > 1024px (multi-column)
- Max content width: 1200px
```

## File Structure

### Phase 1: Core Landing Page
```
/docs/
├── index.html                    # Main landing page
├── privacy.html                  # Privacy policy (moved from index)
├── assets/
│   ├── css/
│   │   ├── style.css            # Main stylesheet
│   │   └── components.css       # Reusable component styles
│   ├── js/
│   │   └── theme.js             # Theme switcher
│   └── images/
│       ├── icon-192.png         # App icon (multiple sizes)
│       ├── icon-512.png
│       ├── hero-timer.svg       # Animated timer graphic
│       ├── feature-timer.png    # Feature screenshots
│       ├── feature-beans.png
│       ├── feature-analysis.png
│       └── og-image.png         # Open Graph preview
└── sitemap.xml
```

### Phase 2: Additional Pages
```
/docs/
├── roadmap.html                  # Roadmap & changelog
├── contribute.html               # Contribution guide
├── press.html                    # Press kit
├── assets/
│   └── downloads/               # Press kit assets
│       ├── logo-pack.zip
│       └── screenshots.zip
```

### Phase 3: Blog Section
```
/docs/
└── guides/
    ├── index.html               # Blog listing
    ├── understanding-brew-ratios.html
    ├── dialing-in-espresso.html
    └── common-espresso-mistakes.html
```

## Implementation Details

### Phase 1: Core Landing Page

#### 1. HTML Structure (`index.html`)
```html
<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <!-- Meta tags -->
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Track, analyze, and perfect your espresso extractions with intelligent timing and detailed data capture. 100% offline, privacy-focused.">
    
    <!-- Open Graph -->
    <meta property="og:title" content="Coffee Shot Timer - Precision Espresso Tracking">
    <meta property="og:description" content="The precision app for perfect espresso extraction">
    <meta property="og:image" content="https://[username].github.io/CoffeeShotTimer_landing_page/assets/images/og-image.png">
    
    <!-- Schema.org markup -->
    <script type="application/ld+json">
    {
        "@context": "https://schema.org",
        "@type": "MobileApplication",
        "name": "Coffee Shot Timer",
        "operatingSystem": "Android",
        "applicationCategory": "LifestyleApplication",
        "description": "Espresso timer with analytics for home baristas",
        "offers": {
            "@type": "Offer",
            "price": "0",
            "priceCurrency": "USD"
        }
    }
    </script>
    
    <title>Coffee Shot Timer - Precision Espresso Tracking</title>
    <link rel="stylesheet" href="assets/css/style.css">
    <link rel="stylesheet" href="assets/css/components.css">
</head>
<body>
    <!-- Content sections as specified below -->
</body>
</html>
```

#### 2. Navigation Bar
- Fixed/sticky header with blur backdrop
- Logo + app name on left
- Navigation items: Features, Guides, Roadmap, Contribute
- Theme toggle button
- Download CTA button (highlighted)

#### 3. Hero Section
Components:
- Large headline with gradient or emphasis
- Supporting subheadline
- Two CTA buttons (Play Store primary, GitHub secondary)
- Animated SVG timer showing extraction progress
- Background: subtle gradient using primary colors

Timer Animation Specification:
- Circular progress ring (like app's timer)
- Animated fill from 0 to 100% over 25 seconds
- Color transitions: red (under) → green (optimal) → yellow (over)
- Display time in center (MM:SS format)
- Loop animation with pause between cycles

#### 4. Value Proposition Section
Three cards in a row (stacked on mobile):
1. **Precision Timing**
   - Icon: Timer/Clock
   - Title + 2-3 bullet points
   - Coffee card styling

2. **Smart Tracking**
   - Icon: Chart/Analytics
   - Title + 2-3 bullet points
   - Coffee card styling

3. **100% Offline**
   - Icon: Cloud-off/Download
   - Title + 2-3 bullet points
   - Coffee card styling

#### 5. Feature Showcase Section
Alternating layout pattern (image left/right):

**Feature 1: Visual Timer**
- Screenshot: Main timer screen
- Title: "Timer with Visual Feedback"
- Description: Brief paragraph
- Feature list with quality dots:
  • Large, touch-friendly display
  • Color-coded extraction zones
  • Haptic feedback on start/stop

**Feature 2: Bean Management**
- Screenshot: Bean management screen
- Title: "Smart Bean Management"
- Description: Brief paragraph
- Feature list with quality dots:
  • Track roast dates automatically
  • Freshness indicators
  • Photo attachments for packages

**Feature 3: Shot Analysis**
- Screenshot: History/analysis screen
- Title: "Comprehensive Analysis"
- Description: Brief paragraph
- Feature list with quality dots:
  • Detailed shot history
  • Pattern visualization
  • Export your data anytime

#### 6. Target Audience Section
"Perfect for:" headline followed by cards/chips:
- Espresso machine owners
- Pour-over enthusiasts
- Coffee shop professionals
- Home barista beginners
- Data-driven perfectionists

Each with a small icon and single-line description.

#### 7. Open Source Section
- GitHub stats badges (stars, forks, issues)
- Brief description of open source commitment
- Links to repository and contribution guide
- "Built with ❤️ by the community" message

#### 8. Footer
Clean, minimal footer with:
- Copyright © 2025 Coffee Shot Timer
- Links: Privacy Policy | Terms | GitHub | Contact
- Social links if applicable

### Phase 2: Additional Pages

#### Roadmap Page (`roadmap.html`)
Structure:
1. **Header**: Consistent with main site
2. **Hero**: "Product Roadmap & Changelog"
3. **Two-column layout**:
   - Left: Roadmap (Planned, In Progress, Completed)
   - Right: Recent Changes (pulls from GitHub releases)
4. **Roadmap items**: Card-based with status badges
5. **Changelog**: Chronological with version numbers

#### Contribute Page (`contribute.html`)
Structure:
1. **Header**: Consistent with main site
2. **Hero**: "Contribute to Coffee Shot Timer"
3. **Sections**:
   - Report a Bug (link to GitHub issues)
   - Request a Feature (link to discussions)
   - Contribute Code (link to contributing.md)
   - Translate the App (if applicable)
   - Support the Project (GitHub sponsors/Ko-fi)

#### Press Kit Page (`press.html`)
Structure:
1. **Header**: Consistent with main site
2. **Hero**: "Press Kit"
3. **Quick Facts**: 
   - App name, developer, release date
   - Platform, category, price
4. **Description**: Short and long versions
5. **Assets**:
   - Logo downloads (PNG, SVG)
   - Screenshots gallery
   - App icon variations
6. **Contact Information**

### Phase 3: Blog Section

#### Blog Index (`guides/index.html`)
- Grid/list of article cards
- Each card: thumbnail, title, excerpt, read time
- Categories/tags for filtering
- Search functionality (client-side)

#### Article Template
- Consistent header/footer
- Article metadata (date, read time, category)
- Table of contents (sticky sidebar on desktop)
- Related articles section
- Share buttons

## CSS Architecture

### Base Styles (`style.css`)
```css
/* Design tokens */
:root {
  /* Light theme colors */
  --color-primary: #B8763D;
  --color-primary-container: #E8DDD0;
  --color-secondary: #7BA5A3;
  --color-background: #F2E6D3;
  --color-surface: #FAF5F0;
  --color-on-background: #2D1B0F;
  --color-on-surface: #2D1B0F;
  --color-on-surface-variant: #5D3A1A;
  --color-error: #D32F2F;
  
  /* Spacing */
  --space-xs: 4px;
  --space-s: 8px;
  --space-m: 16px;
  --space-l: 24px;
  --space-xl: 32px;
  
  /* Border radius */
  --radius-s: 4px;
  --radius-m: 8px;
  --radius-l: 16px;
  
  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md: 0 2px 4px rgba(0,0,0,0.1);
  --shadow-lg: 0 4px 8px rgba(0,0,0,0.15);
}

[data-theme="dark"] {
  --color-primary: #E6B577;
  --color-primary-container: #5D3A1A;
  --color-secondary: #9BC5C3;
  --color-background: #1A0F08;
  --color-surface: #2D1B0F;
  --color-on-background: #D4C4B0;
  --color-on-surface: #D4C4B0;
  --color-on-surface-variant: #E6B577;
}
```

### Component Styles (`components.css`)
Reusable components matching app design:
- `.coffee-card`
- `.coffee-button-primary`
- `.coffee-button-secondary`
- `.quality-indicator`
- `.section-header`
- `.feature-grid`

## JavaScript Functionality

### Theme Switcher (`theme.js`)
```javascript
// Features to implement:
// 1. Toggle between light/dark themes
// 2. Save preference to localStorage
// 3. Respect system preference on first visit
// 4. Smooth transitions between themes
// 5. Update theme-color meta tag
```

### Timer Animation
```javascript
// SVG timer animation:
// 1. Circular progress animation
// 2. Color transitions based on time
// 3. Time display update
// 4. Pause/resume on visibility change
// 5. Smooth, performant animations
```

## SEO & Performance Checklist

### Technical SEO
- [ ] Semantic HTML5 structure
- [ ] Meta descriptions for all pages
- [ ] Open Graph tags
- [ ] Twitter Card tags
- [ ] Schema.org markup
- [ ] XML sitemap
- [ ] Robots.txt
- [ ] Canonical URLs
- [ ] Alt text for all images

### Performance
- [ ] Optimize images (WebP with PNG fallback)
- [ ] Lazy load below-fold images
- [ ] Minify CSS/JS for production
- [ ] Enable gzip compression (GitHub Pages)
- [ ] Inline critical CSS
- [ ] Preload key fonts
- [ ] Service worker for offline access

### Accessibility
- [ ] WCAG 2.1 AA compliance
- [ ] Keyboard navigation
- [ ] Screen reader friendly
- [ ] Sufficient color contrast
- [ ] Focus indicators
- [ ] Skip to content link
- [ ] ARIA labels where needed

## Content Guidelines

### Voice and Tone
- **Professional but approachable**: Technical accuracy with friendly explanations
- **Feature-benefit focused**: Always explain why a feature matters
- **Coffee-literate**: Use appropriate terminology but explain jargon
- **Action-oriented**: Use active voice and clear CTAs

### Key Messages
1. **Precision**: "Perfect your extraction with data-driven insights"
2. **Privacy**: "100% offline, your data stays yours"
3. **Simplicity**: "Complex analysis made simple"
4. **Community**: "Open source and community-driven"

### SEO Keywords to Target
Primary:
- espresso timer app
- coffee shot timer
- espresso extraction timer
- coffee brewing app android

Secondary:
- brew ratio calculator
- espresso shot tracking
- coffee bean management app
- home barista tools

Long-tail:
- how to time espresso shots
- espresso extraction time app
- track coffee brewing parameters
- bean freshness tracker android

## Testing Requirements

### Browser Testing
- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Chrome Android
- Safari iOS

### Device Testing
- Mobile: 360px, 375px, 414px widths
- Tablet: 768px, 834px widths
- Desktop: 1366px, 1920px widths

### Functionality Testing
- [ ] Theme switcher works correctly
- [ ] All links functional
- [ ] Forms submit correctly
- [ ] Animations perform smoothly
- [ ] Images load correctly
- [ ] No console errors

## Deployment Instructions

### GitHub Pages Setup
1. Ensure repository settings have Pages enabled
2. Set source to `docs` folder
3. Custom domain if applicable
4. Enable HTTPS

### Build Process
1. No build step needed (static files)
2. Optionally minify CSS/JS for production
3. Optimize images before commit
4. Update sitemap.xml with new pages

### Maintenance Tasks
- Update roadmap monthly
- Refresh screenshots with app updates
- Update changelog after releases
- Monitor and fix broken links
- Review and update SEO keywords

## Success Metrics

### Key Performance Indicators
- Page load time < 2 seconds
- Lighthouse score > 90
- Mobile-friendly test pass
- Core Web Vitals pass

### User Engagement Metrics
- Average session duration
- Bounce rate < 40%
- Click-through to Play Store
- GitHub star conversions

## Implementation Priority

### Must Have (Phase 1)
- Responsive landing page
- Theme switcher
- Basic SEO
- Privacy policy page
- Mobile optimization

### Should Have (Phase 2)
- Roadmap page
- Contribute page
- Press kit
- Enhanced animations
- Service worker

### Nice to Have (Phase 3)
- Blog section
- Search functionality
- Newsletter signup
- Analytics integration
- Internationalization

## Notes for AI Implementation

### Critical Requirements
1. **Maintain exact color values** from the app's theme
2. **Use spacing multiples of 4px** consistently
3. **Ensure 44px minimum touch targets** everywhere
4. **Follow mobile-first approach** in CSS
5. **Keep HTML semantic** for SEO and accessibility

### File Organization
- Keep CSS modular and organized by section
- Use consistent naming conventions (BEM or similar)
- Comment code sections clearly
- Separate concerns (structure, presentation, behavior)

### Testing Approach
- Test on real devices if possible
- Use browser DevTools device emulation
- Validate HTML/CSS with W3C validators
- Run Lighthouse audits regularly
- Test with screen readers

### Resources and References
- App repository: `/mnt/quickstuff/git/CoffeeShotTimer_main/`
- Current landing page: `/mnt/quickstuff/git/CoffeeShotTimer_landing_page/docs/`
- Play Store link: [To be provided]
- GitHub repository: [To be provided]

## Version History
- v1.0 - Initial plan creation (2025-01-09)
- [Future updates to be logged here]

---

This plan provides complete context for implementation. Any AI agent should be able to follow this blueprint to create a cohesive, professional landing page that maintains brand consistency with the Coffee Shot Timer app.
