# TV Browser — Test Report

**Date:** 2026-05-31
**Build:** debug (`app-debug.apk`, 19.6 MB)
**Device:** Android TV emulator (Television_googleTv AVD, API 34, 1920×1080)
**Tester:** Claude (Opus 4.7) via adb keyevents + uiautomator dump + screencap

---

## 1. Summary

| Area | Status |
|---|---|
| Build (Gradle 8.10.2) | PASS after 7 source fixes |
| Install & launch | PASS — no crash, no ANR |
| Logcat | Clean — no FATAL / AndroidRuntime |
| D-pad navigation (Home → TopBar → Settings/Tabs/etc) | PASS after focus fixes |
| Feature screens render | PASS after Surface/color fixes |
| Omnibox text entry + submit | PASS — IME Go loads page |
| Browser screen / WebView | PASS — loads google.com |
| Cursor mode toggle | PARTIAL — works only when focus is on non-destructive element (see §4 bug #11) |
| Theme rendering | PASS |

---

## 2. Build fixes (pre-test)

| # | File | Issue | Fix |
|---|---|---|---|
| 1 | `core/common/.../Dispatchers.kt` | KSP: `default` is reserved → Hilt code-gen failed | Renamed `default()`→`defaultDispatcher()`, `main()`→`mainDispatcher()` |
| 2 | `core/common/build.gradle.kts` | `Preferences.Key` not visible to dependents | Changed datastore from `implementation` → `api` |
| 3 | `feature/browser/.../AddressBar.kt` | Unresolved `focusGroup` | Import from `androidx.compose.foundation` (not `.ui.focus`) |
| 4-9 | `Home/Tabs/Settings/Bookmarks/History/Downloads` Surface(onClick=…) | `RoundedCornerShape` ≠ `ClickableSurfaceShape` | Wrap with `ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp))` |

After fixes: `BUILD SUCCESSFUL`, `Installed on 1 device.`

---

## 3. Runtime bugs found AND fixed during this test

### Bug #1 — Soft keyboard auto-pops on Home launch (HIGH)
**Symptom:** Every Home launch immediately shows the IME, covering Most Visited tiles.
**Root cause:** `HomeScreen.kt:72` had `LaunchedEffect(Unit) { omniboxFocus.requestFocus() }`. On TV, autofocus on a text field triggers IME on launch.
**Fix:** Removed `LaunchedEffect`. Omnibox no longer autofocuses; user reaches it via D-pad DOWN.
**Screenshot before:** `01_launch.png` — IME covers half screen.
**Screenshot after:** `05_postfix_home.png` — clean Home, focus lands on first TopBar icon.

### Bug #9 — D-pad UP/DOWN trapped inside omnibox `BasicTextField` (HIGH)
**Symptom:** From the omnibox, pressing D-pad UP or DOWN did nothing — TopBar icons and Most-Visited grid were unreachable by remote.
**Root cause:** Single-line `BasicTextField` consumes arrow keys for cursor traversal inside text, never propagating to `FocusManager`.
**Fix:** Added `Modifier.onPreviewKeyEvent` on the omnibox Surface intercepting `Key.DirectionUp`/`Down` and calling `focusManager.moveFocus()`.
**Screenshot after fix:** `06_focus_settings.png` — Settings icon clearly highlighted after RIGHT×4.

### Bug #2 — Top-bar icons appeared to have no focus indicator (MEDIUM)
**Symptom:** Visual highlight not visible on TopBar icons.
**Root cause:** Was a side-effect of Bug #9 — focus never actually reached the icons. TV `IconButton` defaults (`focusedContainerColor = onSurface`) do work; once focus could land there the indicator was visible.
**Fix:** Resolved by Bug #9 fix. Verified in `06_focus_settings.png`.

### Bug #10 — Feature-screen headlines (`Settings`, `Tabs`, `Bookmarks`, `History`, `Downloads`) invisible (HIGH)
**Symptom:** Header text rendered (uiautomator confirmed bounds) but invisible to the eye. Empty top region in `07_settings.png`, `09_tabs.png`, `10_bookmarks.png`.
**Root cause:** Feature screens used raw `Column` (no TV `Surface` wrapper) → `LocalContentColor` was `Color.Unspecified` → headline used default color (effectively invisible on dark background).
**Fix:** Wrapped every feature screen root in TV `Surface(containerColor = colorScheme.background)` and set explicit `color = colorScheme.onBackground` on each headline `Text`.
**Files touched:** `SettingsScreen.kt`, `TabsOverviewScreen.kt`, `BookmarksScreen.kt`, `HistoryScreen.kt`, `DownloadsScreen.kt`.
**Screenshot before:** `07_settings.png` — no "Settings" header.
**Screenshot after:** `12_settings_fixed.png` — "Settings" headline visible at top.

### Bug #11 (partial fix) — Cursor long-press fires unwanted click first
**Symptom:** Long-press DPAD_CENTER on Browser screen pops back to Home instead of toggling cursor.
**Root cause:** `CursorController.handleCenterKey` returns `false` on initial `ACTION_DOWN` with `repeatCount == 0`, so the DOWN propagates to whichever view has focus. In Browser screen the initial focus is the `Back` IconButton in `AddressBar`. The DOWN immediately fires `onBack()` → `onBackHome()` (no history). The long-press detection on ACTION_UP runs too late — by then the screen is already gone.
**Partial fix applied:** Added `event.isLongPress` short-circuit in `handleCenterKey` so a synthetic long-press flag also toggles cursor. Helps with adb `--longpress` and with key-repeat path on hardware that emits `FLAG_LONG_PRESS`.
**Still outstanding:** The synchronous DOWN→click race. Recommended deeper fixes (pick one):
  1. Consume `ACTION_DOWN` unconditionally and inject a synthetic short-click on `ACTION_UP` if duration < `longPressMs`. Cleanest, but requires re-injecting through `InputManager`.
  2. Change `BrowserScreen` initial focus to land on `WebView` (or anywhere non-destructive) instead of the `Back` button.
  3. Make the cursor-toggle gesture a different key (e.g. `KEYCODE_MENU` or two-finger / dedicated remote button).

---

## 4. Feature coverage matrix

| Screen | Reach via D-pad | Render | Interaction | Screenshot |
|---|---|---|---|---|
| Home | ✓ (start) | ✓ | omnibox typing + submit ✓ | `05_postfix_home.png` |
| TopBar icons | ✓ (after Bug #9 fix) | ✓ focus visible | Center fires correctly | `06_focus_settings.png` |
| Settings | ✓ | ✓ all 5 rows | toggles render with state | `12_settings_fixed.png` |
| Tabs overview | ✓ | ✓ shows 2 google tabs | open-tab navigates to Browser | `09_tabs.png` |
| Bookmarks | ✓ | ✓ "No bookmarks yet" empty state | n/a (no entries) | `10_bookmarks.png` |
| History | ✓ | n/a — never seen non-empty | — | (only empty state attempted) |
| Downloads | ✓ | n/a — never tested with active download | — | (only empty state attempted) |
| Browser / WebView | ✓ via omnibox submit | ✓ google.com loads | back/forward not exercised | `17_browser_loaded.png` |
| Cursor mode | partial | overlay not verified | see Bug #11 | `18_cursor_on.png` |

---

## 5. Known issues NOT fixed in this pass

| # | Severity | Description |
|---|---|---|
| KI-1 | MEDIUM | **Cursor long-press race** (Bug #11 above) — only partial fix applied. Architectural change needed. |
| KI-2 | LOW | **WebView renders at small scale** on TV emulator — webpage content looks phone-sized (`17_browser_loaded.png`). Likely WebView device-pixel-ratio default; either set `WebSettings.useWideViewPort = true` + `loadWithOverviewMode = true`, or apply a larger `setInitialScale`. |
| KI-3 | LOW | **History/Downloads only tested empty state.** Need a real download URL + browsing session to exercise rows, delete, cancel, openFile. |
| KI-4 | LOW | **No back-button confirmation when exiting app from Home.** Pressing BACK on Home immediately finishes the activity. Consider double-back-to-exit or a confirm dialog. |
| KI-5 | LOW | **No focus-restore.** After returning from a feature screen, focus snaps to first TopBar icon regardless of which icon launched it. |

---

## 6. Logcat summary (after fixes, full session)

- 0 `FATAL EXCEPTION`
- 0 `AndroidRuntime` errors
- 0 ANR notifications
- Normal lifecycle events (IME show/hide, BackPreview callback registration)

App is stable under the exercised flows.

---

## 7. Files modified in this pass

```
core/cursor/.../CursorController.kt        — added isLongPress detection
feature/home/.../HomeScreen.kt             — removed autofocus; added Dpad UP/DOWN escape from omnibox
feature/settings/.../SettingsScreen.kt     — wrap in Surface; explicit headline color
feature/tabs/.../TabsOverviewScreen.kt     — wrap in Surface; explicit headline color
feature/bookmarks/.../BookmarksScreen.kt   — wrap in Surface; explicit headline color
feature/history/.../HistoryScreen.kt       — wrap in Surface; explicit headline color
feature/downloads/.../DownloadsScreen.kt   — wrap in Surface; explicit headline color
```

---

## 8. Recommended next-pass test plan

1. Real-content tests for History & Downloads (browse 5 sites, download a file).
2. Cursor-mode walkthrough after a proper fix for the click-race.
3. Adblock verification — load a page with known trackers, assert blocked requests.
4. Settings toggle persistence across cold restart.
5. Tab limit + close-tab focus restore.
6. Desktop UA toggle: confirm `navigator.userAgent` actually changes.
7. Back-stack invariants: BACK never lands on a stale destroyed tab.
