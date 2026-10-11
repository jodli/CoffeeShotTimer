---
name: Coffee Shot Timer website
description: The brewing control chart. Graph paper, one ink, three zone colours, every shot a plotted dot.
colors:
  ink: "#17202b"
  ink-2: "#4a5361"
  paper: "#f6f6f1"
  paper-2: "#ecece4"
  plot-grid: "rgb(23 32 43 / 0.12)"
  grid: "rgb(23 32 43 / 0.055)"
  grid-major: "rgb(23 32 43 / 0.09)"
  sour: "#e9c23b"
  sour-tint: "rgb(233 194 59 / 0.24)"
  bitter: "#8e3b2a"
  bitter-tint: "rgb(142 59 42 / 0.13)"
  ideal: "#3e8e5a"
  ideal-ink: "#2a6b41"
  ideal-tint: "rgb(62 142 90 / 0.18)"
  field-ok: "#2c6e44"
  field-slow: "#8e3b2a"
  on-ink-2: "#b9c0ca"
  paper-night: "#10161d"
  paper-2-night: "#18202a"
  ink-night: "#e8e8e1"
  ink-2-night: "#a7b0bc"
  bitter-night: "#c2614b"
  ideal-night: "#5db37b"
  ideal-ink-night: "#7fcb98"
typography:
  display:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "clamp(2.75rem, 5.2vw, 5.25rem)"
    fontWeight: 850
    lineHeight: 0.96
    letterSpacing: "-0.03em"
    fontVariation: "'wdth' 118"
  headline:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "clamp(2.125rem, 4.2vw, 3.75rem)"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.02em"
    fontVariation: "'wdth' 118"
  title:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "clamp(1.375rem, 2vw, 1.75rem)"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "-0.01em"
    fontVariation: "'wdth' 118"
  figure:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.01em"
    fontFeature: "'tnum'"
    fontVariation: "'wdth' 118"
  lead:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "1.1875rem"
    fontWeight: 400
    lineHeight: 1.55
  body:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 400
    lineHeight: 1.55
    fontFeature: "'tnum'"
  label:
    fontFamily: "Archivo, Helvetica Neue, Arial, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 600
    letterSpacing: "0.06em"
    fontVariation: "'wdth' 78"
rounded:
  none: "0px"
  dot: "50%"
spacing:
  cell: "24px"
  gutter: "clamp(20px, 4vw, 56px)"
  section: "clamp(72px, 10vw, 136px)"
  max: "1360px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.none}"
    padding: "0 22px"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.ideal-ink}"
    textColor: "{colors.paper}"
  button-primary-lg:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
    rounded: "{rounded.none}"
    padding: "0 28px"
    height: "60px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "0 22px"
    height: "48px"
  button-outline-hover:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  button-taste:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "0 16px"
    height: "48px"
  button-taste-hover:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper}"
  nav-bar:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    height: "72px"
  chart-frame:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
  zone-field-sour:
    backgroundColor: "{colors.sour}"
    textColor: "{colors.ink}"
    padding: "clamp(20px, 2.6vw, 36px)"
  zone-field-ok:
    backgroundColor: "{colors.field-ok}"
    textColor: "#ffffff"
    padding: "clamp(20px, 2.6vw, 36px)"
  zone-field-slow:
    backgroundColor: "{colors.field-slow}"
    textColor: "#ffffff"
    padding: "clamp(20px, 2.6vw, 36px)"
  plot-dot:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.dot}"
    size: "24px"
---

# Design System: Coffee Shot Timer website

Scope: this file covers the public website in `docs/` only (the "Control Chart" world, served at jodli.github.io/CoffeeShotTimer). The Android app has its own Material 3 theme in `app/src/main/java/com/jodli/coffeeshottimer/ui/theme/`, which this file does not describe and does not govern. The app will be redesigned separately. All tokens and components live in `docs/assets/css/site.css`; the signature interaction lives in `docs/assets/js/chart.js`.

## Overview

**Creative North Star: "The Brewing Control Chart"**

Every page is a sheet of graph paper with a shot plotted on it. The ground is a fine 24px grid with a heavier rule every fifth cell, drawn in the same ink as the type. Shots are numbered dots, the path between them is a dashed trajectory, and the goal is a green target box at 25 to 30 seconds. Three zone colours (sour yellow, bitter rust, ideal green) carry meaning and nothing else. The rest is one ink on paper.

Type is a single variable family, Archivo, used across its width axis: expanded and heavy for headings and figures, regular width for reading and buttons, condensed caps for axis and data labels. Numbers are tabular everywhere. Corners are square, lines are hairlines or 1.5px rules, and depth comes from inverting the sheet or filling a zone with solid colour, never from shadow.

The page reads as an instrument, not a brochure. Density is moderate: generous section rhythm, but each block is ruled and tabular, like a lab sheet. The site follows the system colour scheme (light paper or dark ink-navy) with no manual toggle.

**Key Characteristics:**
- Graph-paper ground: 24px minor cells, 120px major cells, both in ink at low alpha.
- One ink for type, rules and axes; a second, lighter ink for secondary text only.
- Three semantic zone colours: sour yellow, bitter rust, ideal green.
- Archivo across its width axis: 118% for display and figures, 100% for body and controls, 78% for labels.
- Square corners everywhere except plotted dots.
- Plotted dots, dashed trajectories and target boxes are the only ornament.
- Flat. No shadows. Depth by inversion and solid zone fields.

## Colors

One ink on warm graph paper, plus three zone colours that always mean the same thing.

### Primary
- **Chart Ink** (`ink`): all type, all structural rules, chart axes, the primary button fill, the current-shot dot. In the dark scheme it flips to **Night Chalk** (`ink-night`) on the ink-navy ground.
- **Pencil Grey** (`ink-2`, dark `ink-2-night`): secondary text only. Leads, captions, table heads, tick labels, footnotes. Never used for rules.

### Secondary (the zone colours)
- **Underextracted Yellow** (`sour`): the sour, too-fast zone. Solid field in the rule strip (with ink text), the sour taste swatch, and the focus ring and text selection colour. As `sour-tint` it shades the fast zone on the chart and the "too fast" cards in the guides.
- **Overextracted Rust** (`bitter`, dark `bitter-night`): the bitter, too-slow zone. As `field-slow` it is a solid field carrying white text; as `bitter-tint` it shades the slow zone. On the 404 it marks the shot that ran off the chart.
- **Target Green** (`ideal`, dark `ideal-night`): the ideal box outline, the dialed-in dot, the "Perfect" swatch, the square bullets in the hero facts line. `ideal-tint` fills the target box.
- **Target Green Ink** (`ideal-ink`, dark `ideal-ink-night`): green as text. The "Target" chart label, a dialed-in verdict, and the primary button hover.
- **Target Field** (`field-ok`): the scheme-invariant green that carries white text. The "Keep it" rule-strip column and the closing install band.

### Neutral
- **Graph Paper** (`paper`, dark `paper-night`): page ground, nav and footer ground, chart frame ground, text on ink.
- **Paper Shade** (`paper-2`, dark `paper-2-night`): scrollbar track. Not used as a card surface.
- **Grid Lines** (`grid`, `grid-major`): the 24px and 120px background grid. Low alpha ink, lower still in dark.
- **Plot Grid** (`plot-grid`): 1px dividers inside components: chart gridlines, table row lines, spec rows, nav strip divider on mobile, link-grid cell rules.
- **Inverse Pencil** (`on-ink-2`): secondary text on the inverted privacy sheet.

### Named Rules
**The One Ink Rule.** Type, rules and axes are drawn in a single ink. Secondary text uses the pencil grey; nothing else introduces a new neutral.

**The Zone Meaning Rule.** Yellow means sour or too fast, rust means bitter or too slow, green means ideal or go. A zone colour never appears as decoration. Yellow doubles as the focus and selection highlight, the one sanctioned non-zone use.

**The Fixed Field Rule.** Solid fields that carry white text (`field-ok`, `field-slow`) keep the same value in both schemes. White type never sits on the scheme-shifting `ideal` or `bitter` swatches.

## Typography

**Display Font:** Archivo variable (weight 100 to 900, width 62% to 125%), self-hosted as `docs/assets/fonts/archivo-latin-var.woff2`, with Helvetica Neue and Arial fallbacks.
**Body Font:** the same Archivo at 100% width.
**Label/Mono Font:** the same Archivo at 78% width. No monospace; tabular numerals do that job.

**Character:** one family stretched three ways, like a chart where the title is set wide and the axis is set tight. Heavy expanded headings feel engineered, condensed labels feel measured.

### Hierarchy
- **Display** (850, `clamp(2.75rem, 5.2vw, 5.25rem)`, 0.96): the hero headline and page-head titles on reading pages. The 404 and closing band push it larger with the same weight and tracking.
- **Headline** (800, `clamp(2.125rem, 4.2vw, 3.75rem)`, 1): section headings. Reading pages scale it down to `clamp(1.875rem, 3.4vw, 2.75rem)`.
- **Title** (800, `clamp(1.375rem, 2vw, 1.75rem)`, 1): zone headings, card and row headings.
- **Figure** (800, about 1.375rem to 1.75rem, expanded): numbers and verdicts that are the answer: the chart verdict ("2 steps finer"), step counts in the steps table, ledger values, link-grid titles, rule-line statements.
- **Lead** (400, 1.1875rem, 1.55): one-paragraph intros under headings, in pencil grey, 36ch to 60ch.
- **Body** (400, 1.0625rem, 1.55): reading text, capped at 72ch in the guides column. Tabular numerals on the whole body.
- **Label** (600, 0.8125rem, 0.06em, uppercase, condensed): chart captions, readout labels, axis titles, ticks, the rule-strip axis title, and the category tag in a rule line.

### Named Rules
**The Width Axis Rule.** Width carries role: 118% for headings and figures, 100% for body, buttons and nav, 78% for labels and axis data. Don't mix widths inside one element.

**The Data Label Rule.** The condensed caps label voice names data: an axis, a reading, a rule category. It never sits above a heading as a section kicker.

## Layout

A 1360px max container with a fluid gutter (`clamp(20px, 4vw, 56px)`). The background grid is fixed to the body at a 24px cell, so everything sits on graph paper; layout does not snap to it.

- Hero: 5 to 7 column split, copy left, chart right. Collapses to one column below 980px.
- Sections breathe at `clamp(72px, 10vw, 136px)` vertical padding. Section heads cap at 760px wide.
- Two-up splits (6 to 6) for copy beside screenshots, one column below 860px.
- Screenshot strip: four columns on desktop, a horizontal scroll-snap rail below 980px that bleeds to the viewport edge.
- Reading pages: a sticky table of contents (3 parts) beside a body column (9 parts, max 72ch), one column below 860px.
- Lists of actions (roadmap, contribute) are ruled rows: text left, button right, stacked below 700px.
- Tables and spec lists are the default way to present facts: ruled top and bottom at 1.5px, rows divided at 1px.
- Touch targets are at least 44px everywhere, 48px for primary controls.

## Elevation & Depth

The system is flat. There is no box-shadow anywhere. Depth comes from two moves: inverting the sheet (the privacy section turns ink-ground with paper text and keeps a faint major grid) and filling a zone with solid colour (the rule strip and the closing band). Hierarchy otherwise comes from rule weight: 1.5px ink rules frame and close a block, 1px plot-grid lines divide inside it.

### Named Rules
**The Flat Sheet Rule.** Surfaces never lift. If something needs emphasis, invert it, fill it with its zone colour, or frame it with a 1.5px rule.

**The Two Rule Weights Rule.** Structural rules are 1.5px ink. Internal dividers are 1px plot-grid. No third weight.

## Shapes

Square corners throughout (0px). Buttons, chart frame, tables, zone fields, the language switch and screenshots all have hard edges. The only round shape is the plotted dot (50%), which is the data point itself. Swatches inside taste buttons and the bullets in the facts line are small squares. Dashed lines (`6 6` for a trajectory, `3 7` for a preview path) are the only line ornament, and they always mean a path between shots.

### Named Rules
**The Only Round Thing Rule.** A circle is a shot. Nothing else gets a radius.

## Components

### Buttons
Ruled and solid, like a box on a form.
- **Shape:** square (0px), 1.5px ink border, 48px minimum height, 60px for the large hero and close variant.
- **Primary:** ink fill, paper text, weight 700 at 100% width. Optional trailing arrow as an inline stroke SVG (18px, square caps).
- **Hover / Focus:** primary turns target-green ink; outline buttons fill with ink. 160ms colour transition. Focus is a 3px sour-yellow outline offset 3px.
- **Outline:** transparent ground, ink border and text. Used as the second action beside a primary.
- **Text action:** the chart's "Start over" is an underlined text button, no border.
- On the green closing band the primary is pinned to fixed ink and inverts to paper on hover.

### Taste buttons (chips)
- **Style:** paper ground, 1.5px ink border, a 12px square swatch in the zone colour before the word.
- **State:** hover fills with ink; disabled drops to 38% opacity. On phones they stretch to fill the row.

### Chart frame (signature component)
The landing page's playable control chart.
- A paper panel framed in a 1.5px ink rule, with a caption bar (condensed labels), an SVG plot, a readout panel and a note.
- Plot: x is extraction time 10 to 45 s, y is brew ratio 1:1.5 to 1:3. Fast zone tinted sour, slow zone tinted bitter, target box tinted ideal with a 2.5px green outline. Axes 1.5px ink, gridlines 1px plot-grid.
- Shots: numbered paper dots with a 2px ink ring. The current shot fills with ink and paper numeral. A dialed-in shot fills with target green. Before the first tap a faint dashed ghost path previews where the loop goes.
- Interaction: choose Sour, Perfect or Bitter; the verdict prints in the figure style and the next dot arrives along a dashed trail. Trail draws in 700ms, dot scales in over 420ms, both on `cubic-bezier(0.16, 1, 0.3, 1)`. Reduced motion turns all animation and transition off.
- On phones the SVG type grows in user units so labels stay legible at half scale.

### Rule strip
The three grind rules as solid columns at their true widths on the time axis (15 to 5 to 15), sour yellow, target field and slow field, each with a title, a condensed condition line and the app's own quote at the foot. Below, a segmented bar and the example dots on a dashed path. Stacks to rows below 860px.

### Ruled tables and spec lists
Steps table, spec list, privacy ledger and doc tables share one pattern: 1.5px rule top and bottom, 1px plot-grid between rows, heads in pencil grey, the answer cell set in the figure style. The ledger on the inverted sheet uses `currentColor` rules.

### Rule line
A one-line callout in reading pages: 1.5px rules above and below, a condensed label on the left naming the rule, the rule itself in the figure style. The tip variant sets the text at body weight and width.

### Navigation
- A paper strip, 72px tall, closed by a 1.5px ink rule. Wordmark (icon plus expanded 800 name) left, text links centre-right at weight 500, primary install button at the end.
- Hover and current page: 2px underline. On screens below 760px the links drop to a second row under a plot-grid line and the button shortens to "Get it".
- Footer mirrors the nav: 1.5px top rule, ink links with 44px targets, pencil-grey fine print.

### Links grid
Two-column grid of ruled cells, each an expanded 800 title over a pencil-grey line. Hover underlines the title.

### Language switch
A bordered segmented control: square cells split by 1.5px rules, the pressed cell filled with ink.

## Do's and Don'ts

### Do:
- **Do** set every surface on the 24px graph-paper ground with the 120px major grid.
- **Do** use `sour`, `bitter` and `ideal` only for their zone meanings, and `sour` for focus and selection.
- **Do** put white text only on `field-ok` or `field-slow`, which stay fixed across schemes.
- **Do** frame blocks with 1.5px ink rules and divide inside them with 1px plot-grid lines.
- **Do** present facts as ruled tables or spec rows, with the answer in the expanded 800 figure style.
- **Do** use tabular numerals and real units (s, g, 1:x) for every number.
- **Do** draw any new illustration as a chart: axes, target box, numbered dots, dashed trajectory.
- **Do** keep primary controls at 48px minimum height.

### Don't:
- **Don't** add box-shadows, blurs or lifted cards.
- **Don't** round corners on anything except a plotted dot.
- **Don't** use soft or decorative gradients. Hard-stop linear gradients are fine where they draw grid lines or a segmented bar.
- **Don't** introduce a second typeface or a monospace; use Archivo's width axis instead.
- **Don't** put the condensed label voice above a heading as a kicker.
- **Don't** add icons beyond the inline stroke arrow on install buttons; no glyph or emoji icons.
- **Don't** add a manual theme toggle; the site follows the system scheme.
