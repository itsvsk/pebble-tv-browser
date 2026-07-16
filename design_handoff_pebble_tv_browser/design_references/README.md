# Pebble TV — UI Kit

A high-fidelity, interactive reference implementation of the Pebble Android TV browser.
`index.html` is the **full working prototype** — open it and drive it with the keyboard
(it's the canonical click-thru). The component files are factored so they can be lifted into
new screens.

## Run it
Open `index.html`. Navigate with:
- **Arrow keys** — move focus (spatial D-pad nav)
- **Enter** — OK / select
- **Backspace / Esc** — back (closes an open menu first)

## Files

| File | What's inside |
|---|---|
| `index.html` | App shell: Roboto + Material Symbols, the 1920×1080 scaling stage, React/Babel mounts. |
| `styles.css` | All component CSS, built on the tokens in `../../colors_and_type.css`. |
| `nav.jsx` | **`SpatialNav`** (nearest-neighbor D-pad focus), `useGlobalKeys`, `useAutoFocus`. The TV navigation engine. |
| `data.jsx` | Seed content: `MOST_VISITED`, `BOOKMARKS`, `HISTORY`, `DOWNLOADS`, `INITIAL_TABS`, favicon palette. |
| `components.jsx` | Primitives: `Icon`, `Fav`, `NavRail`, `KeyHints`, `Toast`, `MockPage`, `MiniWeb`. |
| `views.jsx` | Screens: `HomeView` (+`HomeClock`), `BrowserView`, `TabsView`, `BookmarksView`, `HistoryView`, `DownloadsView`, `SettingsView`. |
| `app.jsx` | Root state, view routing, tab model, keyboard wiring, overflow menu. |

## Component coverage

**Chrome & navigation**
- `NavRail` — left rail; items take `{view, onNav}`. Active = tonal pill + filled icon.
- `BrowserView` chrome — back/forward/reload icon buttons, address bar (focus to edit URL),
  mode toggle, bookmark star, tab-count badge, overflow menu trigger.
- `KeyHints` — contextual bottom hint bar; pass `hints={[{k,label}]}`.
- `OverflowMenu` (in `app.jsx`) — scrim + sheet, focus-trapped list.

**Content surfaces**
- `HomeView` — live `HomeClock` (time-aware greeting + date), Google search field, most-visited grid.
- `MockPage` — light web-page mock that reflows between `mode="desktop"` and `mode="mobile"`
  (stand-in for a real `WebView`).
- `TabsView` — 4-up grid of `tab-card`s with `MiniWeb` thumbnails + "new tab" affordance.
- `BookmarksView` / `HistoryView` — foldered / day-grouped `row` lists with `Fav` chips.
- `DownloadsView` — default-path card + active (`active`/`paused`) and past (`done`/`failed`)
  download cards with progress bars colored by status.
- `SettingsView` — centered column of `set-row` toggles + the download-location row.

**Primitives**
- `Icon` — Material Symbols wrapper; `fill` prop flips to the filled axis.
- `Fav` — rounded favicon chip from a `{bg,fg,ch}` descriptor; neutral fallback when null.

## Adding a new screen (the rules)
1. Build it from existing `row` / `pill-btn` / `set-row` / card classes in `styles.css`.
2. Put `class="focusable"` on every interactive element (+ `tabindex=0` if not a `<button>`).
   Add `focus-lift` for cards, `focus-soft` for compact items.
3. Mark the entry focus target with `data-autofocus="true"`.
4. Wrap long content in `.view-scroll` so D-pad focus scrolls it into view.
5. Route it in `app.jsx`'s view switch and add a `NavRail` item if it's top-level.

## Production note
This kit simulates the browser in HTML/React for design review. The shippable target is
**Compose for TV** (`androidx.tv.material3`) hosting an Android `WebView` for page content —
see the root `README.md` "D-Pad Navigation Model" section for the 1:1 mapping.
