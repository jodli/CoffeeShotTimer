# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Home baristas with their own espresso machine and grinder who regularly open new bags of beans and want to get to a good shot quickly. They use the app at the machine, in the kitchen, while pulling a shot: one eye on the cup, often with wet or coffee-covered hands.

## Product Purpose

Coffee Shot Timer times espresso shots, logs them per bean, and tells the user what to change next. After a shot the user gives taste feedback (sour, perfect, bitter) and the app suggests a concrete grind adjustment for the next shot on that bean. Success means dialing in a new bean in 3-4 shots instead of 7-10 (target from `docs/specs/product.md`).

## Positioning

A brewing coach, not a logbook. The core loop is record → taste → grind recommendation → apply, saved per bean as next-shot guidance. Precise tracking and history exist to feed that loop. It does this fully offline, with no account, which most comparable apps can't claim.

Achievements and streaks (added in 2.1.0) stay, but as a light side feature. They are never the point and never compete with the guidance for attention.

## Operating Context

- Workflow: choose bean → grind → dose → extract (timer) → record weights → taste feedback → recommendation. Order of entry is flexible.
- Equipment is set up once in onboarding: grinder scale and step size, basket size (single baskets down to 5 g).
- Beans carry roast date, package photo, status (active, low, finished) and dial-in state (New Bean, Experimenting, Dialed In).
- Phones primarily; tablet and landscape are supported via a navigation rail.

## Capabilities and Constraints

- **Fully offline.** No INTERNET permission, no accounts, no analytics, no crash reporting. Only VIBRATE and CAMERA permissions. Rules out cloud sync and any remote feature unless this decision is revisited.
- **English and German parity.** Every user-facing string ships in both `values/` and `values-de/`.
- **Free and open source.** No paywall or premium tier. Donations via Buy Me a Coffee only.
- Recommendations are rule-based, not ML. Don't describe them as AI.
- No export/import exists today.
- Distribution: Google Play (internal, open beta, production tracks).
- Terminology in the UI: "Coffee In", "Weight Out", "Brew Ratio" (1:x), "Extraction Time", "Grinder Setting", "Basket", "Taste Feedback", "Quality score", "finer/coarser grind". Copy currently mixes "Coffee In"/"dose" and "Weight Out"/"yield"; open whether to unify.

## Brand Commitments

- Name: Coffee Shot Timer.
- Voice: warm, encouraging, coach-like, speaks in first person ("I'll help you dial in your beans"). Occasional emoji in coaching messages.
- Assets: launcher icon (`app/src/main/ic_launcher-playstore.png`, `docs/icon.png`), website graphics in `docs/assets/images/`, banner `docs/banner.png`.
- Website: jodli.github.io/CoffeeShotTimer (served from `docs/`).

## Evidence on Hand

- Localized screenshots, 10 per language: `docs/en-US/`, `docs/de-DE/` (regenerated with `update-screenshots.sh`).
- Product spec and principles: `docs/specs/product.md`, `.kiro/steering/product.md`.
- Release history: `CHANGELOG.md`.
- No user testimonials, ratings, download numbers or measured dial-in results are on hand. Don't invent them.

## Product Principles

1. Guide, don't just record: every shot should end in a clear next step.
2. Show what matters now: the screen at the machine carries only what the current shot needs.
3. Progressive disclosure: analytics and detail are there when asked for, not in the way.
4. Invisible power: the logic does the work, the user sees a plain suggestion.
5. Private by default: nothing leaves the device.

## Accessibility & Inclusion

- Touch targets at least 48dp and haptic feedback on the recording flow, because it is used at the machine with wet hands. Current practice, not yet formalized as a requirement.
- Content descriptions for interactive and status elements (`cd_*` strings) in both languages.
