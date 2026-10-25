# Pebble next-step roadmap

This plan was reviewed against the code on 2026-10-24. The opportunities below are not measured regressions unless explicitly stated. Validate visual changes on Android TV and measure performance in a release-like build on the same device before and after each change.

**Implementation update:** The first three priorities have been addressed for the app's current feature set. Private browsing has no user-facing entry yet, so the unsafe clear action was removed; true isolated private storage remains future work. Room version 1 now has an exported schema and no destructive fallback; an explicit migration is required when version 2 is designed. Downloads display their app-owned location and report start/open failures.

**Latest update:** The sea-glass background is now prepared off the UI thread (`d200605`), and the TV navigation regression test passes. The change has not yet been compared with a release-like startup or frame-timing benchmark. That measurement is the next checkpoint.

**Next execution order:** task 5 (performance baseline), task 4 (remote navigation and accessibility), the remaining end-to-end check in task 3 plus task 7 (browser reliability), then task 6 (layout polish).

## Goals

1. Make private browsing and downloads behave as their UI promises.
2. Keep bookmarks, history, tabs, and download records safe across upgrades.
3. Make remote navigation reliable and accessible.
4. Improve responsiveness using release-build measurements.

## Order and decisions

1. **Privacy correctness.** Preserve private-tab state while the tab is open. Keep private browsing unavailable in the UI until separate WebView storage can be verified. The old clear action operated on shared WebView storage and has been removed.
2. **Database durability.** Export the Room schema and remove destructive fallback for user data. Add an explicit migration path and migration test with the next schema version.
3. **Download recovery.** Show the actual download destination, make the location action useful, and surface enqueue failures to the user.
4. **Remote navigation quality.** Add semantics to custom clickable controls and emulator tests for Home wrapping, Tabs entry/exit, Back, and focus after list changes. Verify the same core paths on the TV.
5. **Measured performance (next).** Benchmark cold launch plus immediate D-pad input, Home-to-Browse, and long-list scroll in a release-like build. Inspect slow frames before changing more animation or preview code. Add a Baseline Profile for critical paths only after a repeatable baseline exists.
6. **Layout polish.** Check Home and Library at 720p and 1080p with larger text. Adjust fixed grids/sidebar widths where clipping occurs. Add a focusable Browse action to empty Bookmarks if the screen remains otherwise non-actionable.
7. **Browser reliability.** Exercise browser back/forward, tab restore, and theme persistence on Android TV. Fix only observed failures.

## Dependency and checkpoints

- Privacy behavior and its tests should land together; do not label a flow private until storage behavior is verified.
- Database migrations should be in place before any schema change.
- Measure performance before and after changing preview or animation code.
- Run focused tests and a debug build after tasks 1-3; run emulator focus/layout checks and a TV smoke pass after tasks 4-7.

## Risks and open questions

- Android WebView data storage is process scoped; separate private storage may require a dedicated process and architectural work. A narrower, accurately labeled private mode is a possible interim choice.
- Cached page thumbnails need a storage and invalidation policy. A static favicon/title preview is the simplest first experiment.
- The existing `CLAUDE.md` describes older design and navigation behavior. Verify against current code and the current app before using it as a product spec.

See [todo.md](todo.md) for executable tasks and acceptance criteria.
