# Pebble — Design System

A design system for **Pebble**, a minimal web browser for **Android TV**. It defines the
foundations (color, type, spacing, focus), the components, and — most importantly for a TV
product — the **D-pad navigation and focus model** that the whole UI is built around.

This system exists so the development team can extend and modify the app without re-deriving
design decisions. Tokens are centralized in `colors_and_type.css`; the live reference
implementation is the prototype at the project root.

> **Source of truth:** this system was authored *from* the Pebble prototype itself
> (`Pebble TV Browser.html` + `styles.css` + `*.jsx` at the project root). There is no
> external Figma or codebase — the prototype is the canonical artifact.

---

## What Pebble is

A "10-foot UI" browser: viewed from across a living room and driven by a **remote control
D-pad** (Up/Down/Left/Right + OK/Select + Back), not a mouse or touch. Every design decision
follows from that constraint:

- Large hit targets and type (nothing critical below ~15px at 1080p).
- A **visible, high-contrast focus state** at all times — the user must always know where they are.
- Spatial navigation: focus moves to the nearest neighbor in the pressed direction.
- Dark theme by default, to sit comfortably in a dim room and reduce glare.

**Surfaces in the app:** Home / New-tab · Browser chrome + page · Tab switcher · Bookmarks ·
History · Downloads · Settings — unified by a left **navigation rail**.

---

## CONTENT FUNDAMENTALS

**Voice.** Quiet, plain, and functional. The UI never sells or chats; it labels. Think of
system-app copy, not marketing.

- **Casing:** Sentence case for titles and body ("Request desktop site by default", "Ask where
  to save each file"). UPPERCASE only for small overline/section labels ("MOST VISITED",
  "PAGE RENDERING", "DESKTOP"/"MOBILE" mode chip) with `letter-spacing` applied.
- **Person:** Second person, implied. Instructions address the user as "you" but usually drop
  the pronoun ("Ask where to save each file", not "Ask me where…").
- **Length:** Titles are 2–6 words. Subtitles are one short clause explaining the effect
  ("New tabs open pages in desktop layout"). Never two sentences.
- **Numbers & meta:** Terse, tabular. "5.1 MB of 8.4 MB · 2.4 MB/s · 14s left". Times are 24h
  ("14:22"). Dates are "Saturday, 31 May" or short "Mon, 25 May".
- **Greeting:** Time-aware and warm but minimal — "Good morning / afternoon / evening / night".
  No exclamation marks.
- **Emoji:** **None.** Status and meaning come from icons, color, and type — never emoji.
- **Punctuation:** Middle dot ` · ` separates inline meta. Em dash in page titles only
  ("Voyager 1 — Wikipedia"), mirroring real `<title>` conventions.

Example strings: `"Search Google or type a URL"` · `"Close all"` · `"Clear browsing data"` ·
`"Default download location"` · `"Paused · 142 MB"` · `"Failed · Yesterday · network error"`.

---

## VISUAL FOUNDATIONS

**Overall vibe.** Near-monochrome, cool, calm, and dark. A "pebble" — smooth, grey, quiet.
The only chroma in the chrome is a single **desaturated slate-blue** accent; all other color
enters through website **favicons**. This restraint is the brand.

**Color.** A cool near-black tonal ramp (`--bg` `#0e1013` → `--surface-4` `#2a2f38`) carries the
whole UI; closer-to-viewer surfaces are lighter. Text uses three on-surface tiers
(`on-surface` / `muted` / `faint`). Exactly **one accent** (`--primary` `#8aa9cf` and its
strong/dim/container variants). Three **muted** status colors (sage/ochre/rose) appear only on
download states. See `colors_and_type.css` §1–4 and the Colors cards.

**Type.** Roboto (Material default; Google Sans Text preferred for body on-device). One
display moment — the **92px ultralight clock** on Home — then a tight functional scale from
38px view titles down to a 13px overline. Mono (Roboto Mono) is reserved for file paths
(`/storage/emulated/0/Download`). Tabular numerals for clocks, percentages, sizes. See §5–6.

**Spacing & layout.** 4px base grid. Canonical page gutter is **56px** from the screen edge
(a TV safe-area inset). Navigation rail is a fixed **116px**; chrome bar **92px**; key-hint
bar **56px**. Content within manager screens is centered in a constrained column where it
reads better (Settings uses an 880px centered column).

**Backgrounds.** Flat surfaces. The only gradient in the entire system is the subtle radial
vignette behind the letterboxed stage (`radial-gradient(120% 120% at 50% 0%, #0b0d10, #060708)`)
— do **not** introduce decorative gradients, textures, or images into the chrome. Page content
(the WebView / mock page) is light (`#f5f6f7`) — that contrast is intentional and signals
"this is the web, that is the browser".

**Corner radii.** Generous and consistent: cards/rows/rail-pill `18px` (`--r-md`); menus &
sheets `28px`; favicons `12px`; the address bar, search field, pills and switches are fully
rounded (`--r-full`).

**Borders.** Hairline. Cards and rows use a `1px` `--outline-faint` (`#23272f`) border that is
barely visible at rest — structure without noise. Stronger `--outline` (`#353b45`) only for the
switch track and key caps. The active tab card and a focused-on switch use the **accent** as
border.

**Elevation / shadows.** Used sparingly. Resting cards are flat (border only). Shadows appear on
**lift** (focused cards: `--e2`), on floating surfaces (overflow menu: `--e3`), and on the stage
itself. No tonal elevation overlays beyond the surface ramp.

**Focus state (the defining interaction).** Every focusable element shows a **two-part ring**:
a 3px gap in the background color, then a 3px `--primary` ring (`--focus-ring`) — so the accent
reads cleanly against any surface. Card-like elements (tiles, tab cards, most-visited, bookmark
rows) additionally **lift**: `translateY(-3px) scale(1.03)` + `--e2`. Rail items and menu items
use a tighter ring (`--focus-ring-tight`). Transitions run at `180ms cubic-bezier(.2,0,0,1)`
(standard M3 easing) on transform, box-shadow, and background.

**Hover vs. press vs. focus.** This is a **focus-first** product — focus is the primary state.
Hover (mouse, secondary) lightens the surface one step and brightens text. There is no separate
press scale; "OK" triggers the action directly. Active/selected state uses
`--primary-container` tonal fill (nav rail pill, segmented control, menu selection) with
`--on-primary-container` text.

**Transparency & blur.** Almost none — surfaces are opaque for legibility at distance. The only
translucency: the modal **scrim** (`--scrim`, 72% near-black) behind the overflow menu, and the
key-hint bar's bottom **protection gradient** (`linear-gradient(0deg, rgba(10,11,14,.92), 0)`)
that lets hints sit over content without a hard edge. No backdrop-blur.

**Animation.** Restrained and functional: focus ring/lift transitions, page-mode width reflow,
switch knob slide, toast fade. No bounces, no parallax, no looping motion. Easing is always the
M3 standard curve.

**Imagery.** The chrome ships **no photography or illustration**. Website thumbnails in the tab
switcher are abstract grey skeletons (`MiniWeb`); image placeholders in mock pages are diagonal
hatch fills. Real imagery only ever comes from rendered web content.

---

## ICONOGRAPHY

- **System:** Google **Material Symbols (Outlined)**, loaded from the Google Fonts CDN as a
  variable icon font. Settings: `FILL 0, weight 300, GRAD 0, opsz 40` at rest; filled variants
  (`FILL 1`) mark active/selected (active rail item, bookmarked star).
- **Sizes:** 28px in the rail and chrome buttons, 20–26px inline in rows/menus, 36–40px for
  empty states and the "New tab" affordance.
- **On Android:** map to `androidx.compose.material.icons` (Material Symbols equivalents) or
  bundle the Material Symbols variable font. Keep the Outlined style and the FILL-on-active rule.
- **Favicons:** represented as a rounded color chip with a letter/glyph (`Fav` component) using a
  curated low-chroma palette. In production these become real site favicons; keep the rounded
  `12px` chip frame and a neutral fallback (`public` glyph on `--surface-3`).
- **Emoji / unicode as icons:** not used in chrome. A few unicode glyphs seed demo favicons only
  (▶ ☀ ♫ ◉) and are not part of the system.

CDN link (already in the prototype):
`https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-25..0`

---

## INDEX — what's in this system

| File / folder | Purpose |
|---|---|
| `README.md` | This document — context, content & visual foundations, iconography. |
| `colors_and_type.css` | **All design tokens** as CSS custom properties (color, type, spacing, radius, elevation, motion, focus, layout) + semantic helper classes. Import first. |
| `preview/` | Design-system reference cards (the Design System tab): color ramps, type specimens, spacing/radius/elevation, components, focus model, brand. |
| `ui_kits/pebble-tv/` | High-fidelity component reference: `index.html` gallery + JSX components, and a link to the full working prototype. |
| `SKILL.md` | Agent-Skill manifest so this system can be downloaded and used in Claude Code. |
| `../Pebble TV Browser.html` | The **live, working prototype** — the canonical reference implementation. Open it to see every component and the D-pad model in motion. |

### Token quick reference
- **Surfaces:** `--bg` → `--surface-1..4` (lighter = closer)
- **Text:** `--on-surface` / `--on-surface-muted` / `--on-surface-faint`
- **Accent (only one):** `--primary` `#8aa9cf` (+ `-strong` / `-dim` / `-container`)
- **Status (downloads only):** `--good` / `--warn` / `--danger`
- **Radius:** `--r-md 18px` (cards), `--r-full` (pills/inputs)
- **Focus:** `--focus-ring`, `--focus-lift`
- **Gutter:** `--gutter 56px`

---

## D-PAD NAVIGATION MODEL (read before building new screens)

1. Every interactive element gets `class="focusable"` (and `tabindex=0` if not a native button).
2. Arrow keys move focus to the **nearest** focusable in that direction (spatial nav — see
   `nav.jsx`, `SpatialNav.move`). The scorer prefers cross-axis alignment.
3. **Enter/OK** clicks the focused element. **Back/Esc** pops to the previous view (closes a menu
   first if open).
4. Mark the element that should receive focus on view entry with `data-autofocus="true"`.
5. Long lists/grids live in a scroll container (`.view-scroll` / `.page-scroll`); focus moves
   keep the focused item in view via `SpatialNav.scrollWithin`.

On real Android TV, this maps directly to Compose's focus system (`Modifier.focusable()`,
`focusRestorer()`, `TvLazyColumn/Row/Grid`) — the spatial model is the same; you get it for free
from the framework instead of the JS controller.
