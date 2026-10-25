# Pebble roadmap tasks

**Next execution order (2026-10-24):** 5 (measure first-launch motion), 4 (remote and accessibility regression coverage), 3's end-to-end download check plus 7 (browser reliability), then 6 (layout polish). Private storage isolation and a Room migration remain conditional on exposing private browsing and changing the schema.

## 1. Correct private browsing behavior

- [x] Keep an open private tab's URL and private flag in memory through page navigation, and release it when its Browser navigation entry is removed.
- [x] Remove the misleading clear action that affected shared WebView storage; keep private browsing unavailable in the UI.
- [x] Add focused tests for private-tab metadata and cleanup.
- [ ] Before exposing private browsing, isolate WebView cookies/site storage and verify separation from normal tabs.

**Likely files:** `core/webview/TabManager.kt`, `core/webview/TabWebView.kt`, `feature/browser/BrowserViewModel.kt`, `feature/settings/SettingsScreen.kt`
**Scope:** Medium to large; split storage isolation into a separate task if needed.
**Verification:** Focused registry tests and debug build passed. Full private storage isolation cannot be verified until the feature is implemented.
**Depends on:** None.

## 2. Protect persisted data during upgrades

- [x] Export the version 1 Room schema and remove destructive migration fallback for user data.
- [x] Add an emulator test that preserves bookmarks, history, tabs, and downloads through close/reopen on version 1.
- [ ] Add an explicit version 1-to-2 migration and migration test when the version 2 schema is defined.

**Likely files:** `core/db/AppDatabase.kt`, `core/db/build.gradle.kts`, database test sources
**Scope:** Medium.
**Verification:** Version 1 persistence test passed on emulator; debug build passed.
**Depends on:** None.

## 3. Make downloads accurate and recoverable

- [x] Display the actual app-owned destination on Downloads and make Settings navigate there.
- [x] Surface download-start failure with an immediate retry; report missing files or unavailable open handlers.
- [x] Test filename sanitization and unique destinations for existing and pending downloads.
- [ ] Exercise successful and failed downloads end to end on a TV emulator.

**Likely files:** `feature/settings/SettingsScreen.kt`, `feature/browser/BrowserScreen.kt`, `core/downloads/DownloadManagerRepo.kt`
**Scope:** Medium.
**Verification:** Focused unit tests and debug build passed; end-to-end download flows remain to be checked on a TV emulator.
**Depends on:** None.

## Checkpoint after 1-3

- [x] Focused unit and database persistence tests pass.
- [x] `./gradlew :app:assembleDebug` succeeds.
- [ ] Verify normal and private browsing storage separation if private browsing is added to the UI.

## 4. Make TV controls testable and accessible

- [ ] Expose click and long-click semantics, roles, and labels on custom focusable controls without adding a second focus target.
- [ ] Add emulator D-pad tests for Home row wrapping, New Tab to first tab, Back to top navigation, and focus after list updates.
- [ ] Confirm every visible action can be reached and activated with the remote.

**Likely files:** `core/design/PebbleFocus.kt`, `feature/home/HomeScreen.kt`, `feature/tabs/TabsOverviewScreen.kt`, app instrumentation tests
**Scope:** Medium.
**Verification:** Instrumentation tests on emulator and accessibility tree inspection.
**Depends on:** None.

## 5. Measure and reduce navigation latency

- [x] Move sea-glass background rasterization off the UI thread; confirm the updated TV navigation test still passes (`d200605`).
- [ ] Record repeatable release-like cold startup with immediate D-pad input, Home-to-Browse, menu-open, and long-list scroll timing and frame data on the same device.
- [ ] Compare live context WebView previews with a static or cached preview; adopt the change only if it improves results without harming usability.
- [ ] Add a Baseline Profile for proven startup and critical navigation paths, then verify the gain against the unprofiled build.

**Likely files:** Home, Tabs, Bookmarks, and History screens; new benchmark/profile module
**Scope:** Medium tasks, implemented sequentially.
**Verification:** Keep a before/after benchmark record with cold-start time and frame timing for the same journeys and device; review slow-frame traces before further code changes.
**Depends on:** None; measurement precedes implementation.

## 6. Polish layouts across TV sizes

- [ ] Check Home speed dials, Bookmarks, Tabs, History, and Downloads at 720p/1080p and larger font settings.
- [ ] Remove any confirmed clipping or cramped spacing by adapting column counts and pane widths.
- [ ] Give empty Bookmarks an obvious focusable Browse action.

**Likely files:** `feature/home/HomeScreen.kt`, `feature/bookmarks/BookmarksScreen.kt`, other affected feature screens
**Scope:** Small to medium per screen.
**Verification:** Emulator screenshots and D-pad traversal at both sizes.
**Depends on:** None.

## 7. Verify browser reliability on TV

- [ ] Navigate a page backward and forward, switch tabs, and relaunch the app; confirm each action returns to the expected page and focus target.
- [ ] Change the theme, restart the app, and confirm the selected theme and readable focus states persist.

**Likely files:** `feature/browser/BrowserScreen.kt`, `app/navigation/AppNavHost.kt`, `feature/settings/SettingsScreen.kt` if failures are observed.
**Scope:** Small verification pass; split any observed defects into focused fixes.
**Verification:** Record the D-pad steps and results on Android TV; add a regression test for each reproducible defect.
**Depends on:** None.

## Final checkpoint

- [ ] Relevant unit and instrumentation tests pass.
- [ ] Debug build succeeds; release-like build is profiled.
- [ ] No focus traps, clipped content, or misleading privacy/download wording remain in reviewed flows.
