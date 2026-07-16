# Handoff: Pebble — Android TV Web Browser

## Overview
Pebble is a minimal, Material-3 web browser for **Android TV** ("10-foot UI", driven by a
remote-control **D-pad** — Up/Down/Left/Right + OK/Select + Back). It covers: a Home/new-tab
page, an active browsing view with desktop/mobile rendering modes, a tab switcher, and
bookmarks / history / downloads / settings managers — unified by a left navigation rail.

This document is **self-sufficient**: a developer who was not part of the design conversation
should be able to implement the app from this README plus the bundled references.

## About the design files
The files in `design_references/` are a **design reference created in HTML/React** — a working
prototype that demonstrates the intended look, layout, and D-pad interaction. **They are not
production code to ship.** The task is to **recreate these designs natively** in the target
environment (see below), using its idiomatic patterns and components.

- `design_references/index.html` — the full interactive prototype. Open it and drive with arrow
  keys / Enter / Backspace to see every screen and the focus behavior.
- `design_references/*.jsx`, `styles.css` — the prototype's source (logic + styling), useful as a
  precise spec for layout, copy, and state.
- `colors_and_type.css` — the canonical **design tokens** (color/type/spacing/radius/elevation/
  motion/focus/layout) as CSS variables. This is the single source of truth for values.
- `DESIGN_SYSTEM.md` — the full design system: content/voice rules, visual foundations,
  iconography, and the D-pad navigation model in depth.

## Target environment (confirmed with stakeholder)
**Native Android TV — Kotlin + Jetpack Compose for TV** (`androidx.tv:tv-material3`), hosting an
Android **`WebView`** for actual page rendering. This is greenfield. Recommended libraries:
- UI: `androidx.tv:tv-material3`, `androidx.tv:tv-foundation` (TvLazyColumn/Row/Grid)
- Web content: `android.webkit.WebView`
- Downloads: `android.app.DownloadManager` (system service)
- Persistence: **Room** (bookmarks, history, downloads metadata) + **DataStore** (settings, default path)
- DI/arch: ViewModel + StateFlow (MVVM), or your team's standard

> Do **not** wrap the HTML in a WebView as the app shell. The chrome must be native Compose so
> D-pad focus, performance, and TV focus-restoration work correctly. Only *web page content*
> goes in a WebView.

## Fidelity
**High-fidelity.** Colors, typography, spacing, radii, and interactions are final. Recreate the
UI to match, mapping the tokens below to Compose theme primitives. Pixel values are given at the
**1080p (1920×1080) design canvas** — on real devices, use `dp` and let Compose scale; the
relative scale and the focus treatment are what must be preserved.

---

## DESIGN TOKENS → Compose mapping

All raw values live in `colors_and_type.css`. Build a `PebbleTheme` from them.

### Color → `darkColorScheme`
Surfaces (cool near-black ramp, lighter = closer to viewer):
| Token | Hex | Role |
|---|---|---|
| bg | `#0E1013` | app background / page canvas backdrop |
| surface | `#14161B` | nav rail, chrome bar |
| surface-1 | `#181B21` | resting cards & rows |
| surface-2 | `#1D2027` | inputs, focused rows, segmented control |
| surface-3 | `#23272F` | chips, icon wells, thumbnails |
| surface-4 | `#2A2F38` | switch track (off) |
| scrim | `rgba(6,7,9,.72)` | modal/menu backdrop |

Text / outline: on-surface `#E6E9EF`, on-surface-muted `#AAB1BD`, on-surface-faint `#6F7782`,
outline `#353B45`, outline-faint `#23272F`.

Accent — **one accent only**: primary `#8AA9CF`, primary-strong `#A8C2E2`, primary-dim `#5D7E9F`,
primary-container `#233246`, on-primary `#0C1722`, on-primary-container `#CFE0F5`.

Status (downloads only): good `#84B59A`, warn `#CDAB74`, danger `#C98B8B`.

Suggested `ColorScheme` slots: `background=bg`, `surface=surface-1`,
`surfaceVariant=surface-2`, `primary=primary`, `primaryContainer=primary-container`,
`onPrimaryContainer=on-primary-container`, `onSurface=on-surface`,
`onSurfaceVariant=on-surface-muted`, `outline=outline`, `error=danger`. Keep the rest of the
ramp as custom values (an `object PebbleColors`), since M3 has no slot for a 6-step surface ramp.

### Type → `Typography` (Roboto; prefer Google Sans Text for body on-device)
| Style | size / weight / tracking | Usage |
|---|---|---|
| clock | 92sp / W200 / -2 | Home hero clock (the one display moment) |
| display | 38sp / W500 / -0.5 | view titles (Settings, History…) |
| greet | 30sp / W300 | Home greeting |
| titleLg | 23sp / W400 | address bar & search input |
| title | 19sp / W500 | address text, focused list item |
| body | 18sp / W500 | row & settings titles |
| bodySm | 17sp / W400 | view subtitles, menu items |
| label | 15sp / W500 | pill buttons, meta |
| labelSm | 14sp / W400–600 | captions, group labels |
| overline | 13sp / W600 / +0.5 / UPPER | section labels, mode chip |

Roboto Mono for file paths only. Tabular figures for clock/percent/size
(`fontFeatureSettings = "tnum"`).

### Shape → `Shapes` (RoundedCornerShape)
xs 8 · sm 12 · md **18** (cards/rows/rail-pill) · lg 28 (menus/sheets) · xl 36 · full (pills,
inputs, switches = `CircleShape`/`RoundedCornerShape(50)`).

### Elevation (shadow only — no tonal overlays beyond the ramp)
e1 `0 2 8 / .35`, e2 `0 8 24 / .45` (focused-card lift), e3 `0 18 48 / .55` (menus/sheets). Apply
via `Modifier.shadow(elevation, shape)`.

### Spacing & layout
4dp base grid. Page gutter **56dp**. Nav rail **116dp**. Chrome bar **92dp**. Key-hint bar 56dp.

### Motion
Standard ease `cubicBezier(0.2, 0, 0, 1)` at **180ms** (fast 120ms). Use for focus ring/scale,
mode-width reflow, switch knob, toast fade. No bounces or looping motion.

---

## THE FOCUS MODEL (most important — TV interaction)
Every focusable shows a **two-part ring**: a 3dp gap in the background color, then a 3dp
`primary` ring. Card-like elements additionally **lift**: `translateY(-3dp) scale(1.03)` + e2.
Rail/menu items use a tighter 2dp+2dp ring.

Compose implementation sketch:
```kotlin
@Composable
fun Modifier.pebbleFocus(lift: Boolean = false): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused && lift) 1.03f else 1f, tween(180, easing = PebbleEasing))
    val ringColor = if (focused) PebbleColors.primary else Color.Transparent
    return this
        .scale(scale)
        .then(if (focused && lift) Modifier.offset(y = (-3).dp).shadow(12.dp, MaterialTheme.shapes.medium) else Modifier)
        // outer ring with a bg-colored gap:
        .border(3.dp, PebbleColors.bg, MaterialTheme.shapes.medium)
        .border(BorderStroke(3.dp, ringColor), MaterialTheme.shapes.medium) // draw on top via padding/layered borders
        .focusable(interactionSource = interaction)
}
```
(Use `Modifier.drawBehind`/layered borders to get the gap-then-ring look exactly; the prototype's
`--focus-ring` = `0 0 0 3px bg, 0 0 0 6px primary` is the target.)

Navigation rules:
- Spatial focus is provided by the framework — use `TvLazyColumn/Row/VerticalGrid` and
  `Modifier.focusable()`. The prototype's `nav.jsx` reimplements this manually for the web; on
  Compose for TV you get nearest-neighbor traversal for free.
- Use `focusRestorer()` on the rail and on each screen's content so focus returns sensibly after
  Back. Set initial focus per screen (the prototype marks it `data-autofocus`).
- **OK/DPAD_CENTER** = activate focused element. **Back** = pop view (close overflow menu first
  if open).

---

## SCREENS / VIEWS

### 1. Navigation rail (persistent, left, 116dp)
- Top: round pebble logo mark (slate-blue radial gradient + soft glow), 52dp.
- Items (icon 28dp + 11sp label, 76×60dp pill): **Home** (`home`), **Tabs** (`tab`),
  **Saved** (`bookmark`), **History** (`history`), **Files** (`download`); pushed to bottom:
  **Settings** (`settings`).
- Active item: `primary-container` pill + **filled** icon (Material Symbols FILL 1) +
  `on-primary-container` text. Inactive: `on-surface-muted`, outlined icon. Focus: tight ring.
- Surface `surface`; right edge hairline `outline-faint`.

### 2. Home / New tab
- Centered column. Top: small pebble mark + "PEBB**L**E" wordmark (20sp, tracking 4, the L in
  `primary-strong`), opacity .92.
- **Hero**: live clock `HH:MM` (92sp W200, tabular), greeting (30sp W300, time-aware:
  morning <12, afternoon <17, evening <21, else night), date overline ("SUNDAY, 31 MAY", 16sp,
  tracking 1.4, UPPER, `on-surface-faint`). Clock updates every second.
- **Search field**: 78dp tall, full-radius, `surface-2`, leading `search` icon (30dp), input
  (23sp) placeholder "Search Google or type a URL", trailing `mic` (accent). Submitting: if the
  text looks like a domain (has a dot, no spaces) navigate to it, else Google-search it.
- **Most visited**: overline "MOST VISITED" then an 8-column grid of tiles (favicon chip 54dp +
  name 14sp). Tiles are `surface-1`, hairline border, **focus-lift**.

### 3. Browser view (chrome + page)
- **Chrome bar** (92dp, `surface`): icon buttons (60dp round) back / forward(disabled) / reload;
  then the **address bar** (flex, 60dp, full-radius, `surface-2`): leading lock/info, dimmed
  `https://` scheme + URL (ellipsized), trailing **mode chip** (DESKTOP/MOBILE pill). Focusing the
  address bar enters an editable input (select-all). Then: mode toggle button, bookmark **star**
  (filled + `primary-strong` when saved), tab-count button with badge, overflow `more_vert`.
- **Page area**: the `WebView`. Desktop vs mobile = swap `WebSettings.userAgentString` +
  `useWideViewPort`/`loadWithOverviewMode`; the prototype simulates this by constraining the mock
  page to 460px in mobile and reflowing. Toggling re-renders with a 180ms width transition.
- **Overflow menu** (`more_vert`): scrim + 340dp sheet (radius 28) top-right with: Add bookmark /
  Bookmarked, Desktop/Mobile site, Find in page, Share, divider, Bookmarks, History, Downloads,
  Settings. Focus-trap; first item auto-focused; Back closes it.

### 4. Tab switcher
- Header: title "Tabs" + "N open"; actions **New tab** (tonal pill) and **Close all** (pill).
- 4-column grid of tab cards (radius 18, `surface-1`): header row = favicon (or `home` glyph) +
  title (ellipsized) + close `×`; thumbnail (168dp) = abstract page skeleton (or a `search` glyph
  for home tabs). Active tab card border = `primary`. A dashed "New tab" card ends the grid.
  Cards are **focus-lift**. Closing the active tab selects the previous; closing the last creates
  a fresh home tab.

### 5. Bookmarks
- Header: "Bookmarks", "N saved across M folders", Grid/List segmented control.
- Grouped by folder (overline + `folder` glyph): "Bookmarks bar", "Reading", "Watch later". Each
  group = 3-column grid of rows (favicon + title + url + `more_vert`). Rows on `surface-1`,
  hairline border, focus → `surface-2` + ring.

### 6. History
- Header: "History", subtitle, **Clear browsing data** pill.
- Grouped by day (overline: "Today", "Yesterday", "Mon, 25 May"). Full-width rows: favicon +
  title + url + time (`on-surface-faint`) + close `×`. 24h times.

### 7. Downloads
- Header: "Downloads", "N active · M completed", **Open folder** pill.
- **Default download location** card (top): `folder` icon + label "DEFAULT DOWNLOAD LOCATION" +
  mono path `/storage/emulated/0/Download` + **Change** pill (opens a folder picker;
  `ACTION_OPEN_DOCUMENT_TREE`).
- **In progress** group: cards with kind icon, name, meta ("5.1 MB of 8.4 MB · 2.4 MB/s · 14s
  left" / "Paused · 142 MB"), a 6dp progress bar colored by status (`primary` active / `warn`
  paused), percent (tabular), and actions (pause/×, or play for paused).
- **Completed** group: done cards (size · timestamp, `folder_open` + `more_vert`) and failed
  cards (`danger` icon + "Failed · … · network error" + retry `refresh`). Back the list with
  `DownloadManager` queries + a Room mirror for history.

### 8. Settings (centered, 880dp column)
- Centered header "Settings" / "Pebble · version 1.0".
- Sections (overline labels), each a `set-row` (icon well 48dp + title 18sp + subtitle 14sp +
  trailing control):
  - **Page rendering**: "Request desktop site by default" (switch; controls new-tab mode),
    "Block pop-ups & redirects" (switch).
  - **Downloads**: "Download location" (mono path + Change pill), "Ask where to save each file"
    (switch).
  - **Privacy**: "Send 'Do Not Track' request" (switch), "Clear browsing data" (chevron → action).
- Switch: 58×34 track, knob 24; on = `primary-container` track + `primary` border + `primary-strong`
  knob; off = `surface-4` track + `on-surface-faint` knob. 180ms slide.

### Key-hint bar (bottom, 56dp, all screens)
Contextual hints over a bottom protection gradient (`linear-gradient(0deg, rgba(10,11,14,.92),
0)`): e.g. "▲▼◀▶ Navigate · OK Select · Back Return". Non-interactive.

---

## INTERACTIONS & BEHAVIOR
- **Navigation**: rail item selects a top-level view. "Home" takes the **current tab** to the
  home page (standard browser home button). Back pops to the previous view (closing an open
  overflow menu first).
- **Address entry**: focus address bar → editable; Enter commits. Domain-like text → navigate;
  otherwise → Google search. Esc/blur cancels and restores the URL.
- **Mode toggle**: flips desktop/mobile for the active tab; page reflows (180ms). New tabs
  inherit the "Request desktop by default" setting.
- **Bookmark star**: toggles bookmark for the current URL; shows a toast.
- **Tabs**: new/close/close-all; close-active selects previous; closing the last spawns a fresh
  home tab.
- **Toasts**: transient confirmations (~2s) bottom-center (`surface-3` pill, e2).
- **Transitions**: focus ring/lift, mode width, switch knob, toast — all 180ms M3 ease.

## STATE MANAGEMENT
Per ViewModel + StateFlow:
- `tabs: List<Tab>` where `Tab(id, kind: home|page, title, url, favicon, mode: desktop|mobile, secure)`;
  `activeTabId`; `currentView: home|browser|tabs|bookmarks|history|downloads|settings`;
  `overflowMenuOpen`.
- `bookmarks` (Room), `history` (Room), `downloads` (DownloadManager + Room mirror).
- `settings` (DataStore): `defaultDesktop`, `blockPopups`, `askWhere`, `dnt`, `downloadPath`.
- Clock: a 1s ticker (`flow { while(true){ emit(now); delay(1000) } }`) feeding the Home hero.

## ASSETS
- **Icons**: Material Symbols (Outlined) — map to `androidx.compose.material.icons` equivalents
  or bundle the Material Symbols variable font. FILL flips 0→1 on active/selected.
- **Logo**: the "pebble" mark is a CSS radial gradient + glow — recreate with a Compose radial
  `Brush` (`primary-strong` → `primary`) on a circle with a soft outer glow; no raster asset
  needed. No other imagery in the chrome.
- **Favicons**: real site favicons in production; rounded 12dp chip frame with a neutral
  fallback (`public` glyph on `surface-3`). The prototype's color-chip palette is demo-only.
- **Fonts**: Roboto (+ Roboto Mono); Google Sans Text preferred for body if licensed. Currently
  CDN-loaded in the prototype — bundle the font files in the app.

## FILES (in this bundle)
- `design_references/index.html` — interactive prototype (open & drive with the D-pad keys)
- `design_references/{nav,data,components,views,app}.jsx`, `styles.css` — prototype source/spec
- `design_references/README.md` — UI-kit component reference & rules for new screens
- `colors_and_type.css` — canonical design tokens
- `DESIGN_SYSTEM.md` — full design system (voice, visual foundations, iconography, D-pad model)
