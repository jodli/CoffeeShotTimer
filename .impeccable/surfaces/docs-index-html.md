---
version: 1
slug: "docs-index-html"
primary_target: "docs/index.html"
related_targets: ["docs/guides.html","docs/roadmap.html","docs/contribute.html","docs/privacy.html","docs/404.html"]
---

# Landing page and site (docs/)

## Scope and mode
Persuade. docs/index.html is the lead surface; guides, roadmap, contribute, privacy and 404 carry the same world (Read mode inside it). English only. Theme follows the system, no manual toggle.

## Audience, action, proof
- Home barista with a new bag of beans, deciding whether to install.
- Action: install from Google Play (https://play.google.com/store/apps/details?id=com.jodli.coffeeshottimer).
- Belief after the first viewport: it tells me what to change.
- Proof: the recommendation recreated live in HTML from the app's real rules (25-30 s ideal, ratio 1:2.0-1:2.5, 1/2/3 steps for deviation up to 3/6/more seconds, taste decides direction). Demo shots are synthetic and labelled as an example. Real screenshots in docs/en-US/ further down.
- Constraints: no invented numbers (downloads, ratings, "3 shots" as a promise). Recommendations are rule-based, never "AI". No TDS or extraction-yield axes.

## Direction contract
THESIS: The page is a brewing control chart: every shot is a plotted dot, taste feedback moves the next one toward the ideal box. Refuses the phone-mockup-beside-headline hero and the three-card feature row.
OWN-WORLD: Graph-paper ground (light #F6F6F1, dark ink-navy) ruled in a fine grid, one ink colour #17202B for type and rules, three named zone colours: sour yellow #E9C23B, bitter rust #8E3B2A, ideal green #3E8E5A. Archivo across its width axis: expanded heavy for display, condensed for axis labels and data, tabular numerals. Square corners, hairline rules, plotted dots and dashed trajectories as the only ornament.
STORY: Visitor sees a shot land sour, taps a taste, watches the next grind get computed and the next dot move toward the box; understands the loop, believes it's concrete, installs. Below: the loop explained in three plotted steps, real screenshots, private by default, open source, close with install.
FIRST VIEWPORT: Nav strip on top (wordmark, Guides, Roadmap, Contribute, filled ink Google Play button). Left 40%: headline in expanded heavy Archivo, one-sentence sub, primary Google Play button, "Free · Offline · Open source" line. Right 60%: a large chart, x extraction time 15-40 s, y brew ratio 1:1.5-1:3, ideal box 25-30 s x 1:2.0-2.5 in green, sour zone left in yellow, bitter zone right in rust. Shot 1 plotted; taste buttons Sour / Perfect / Bitter beside it; choosing one prints the recommendation ("2 steps finer → 2.8") and plots the next shot along a dashed line. Signature interaction: that taste-to-next-dot loop, playable to the ideal box, with reset.
FORM: The Control Chart, position 1 on the ordered list (control chart, flavor wheel, cupping form, bag label, order ticket, espresso bar signage, grinder dial); chosen as Impeccable's pick over the rolled cupping form. Seed key 58590c9a.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Open
- Screenshots are pre-redesign of the bean card; replace after the app redesign.
- canonical/sitemap URLs point at the old CoffeeShotTimer_landing_page repo; fix to jodli.github.io/CoffeeShotTimer/.
