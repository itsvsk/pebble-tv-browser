# Pebble next-step roadmap

This plan is based on a read-only code review on 2026-10-22. The opportunities below are not measured regressions unless explicitly stated. Validate visual and performance changes on an Android TV emulator before shipping.

**Implementation update:** The first three priorities have been addressed for the app's current feature set. Private browsing has no user-facing entry yet, so the unsafe clear action was removed; true isolated private storage remains future work. Room version 1 now has an exported schema and no destructive fallback; an explicit migration is required when version 2 is designed. Downloads display their app-owned location and report start/open failures.

## Goals

1. Make private browsing and downloads behave as their UI promises.
2. Keep bookmarks, history, tabs, and download records safe across upgrades.
3. Make remote navigation reliable and accessible.
4. Improve responsiveness using release-build measurements.

## Order and decisions

1. **Privacy correctness.** Preserve private-tab state while the tab is open. Keep private browsing unavailable in the UI until separate WebView storage can be verified. The old clear action operated on shared WebView storage and has been removed.
2. **Database durability.** Export the Room schema and remove destructive fallback for user data. Add an explicit migration path and migration test with the next schema version.
3. **Download recovery.** Show the actual download destination, make the location action useful, and surface enqueue failures to the user.
4. **Remote navigation quality.** Add semantics to custom clickable controls and emulator tests for Home wrapping, Tabs entry/exit, Back, and focus after list changes.
5. **Measured performance.** Benchmark startup, Home-to-Browse, context-menu open, and long-list scroll in a release-like build. Then replace live WebView context previews if they materially affect latency or memory. Add a Baseline Profile for verified critical journeys.
6. **Layout polish.** Check Home and Library at 720p and 1080p with larger text. Adjust fixed grids/sidebar widths where clipping occurs. Add a focusable Browse action to empty Bookmarks if the screen remains otherwise non-actionable.

## Dependency and checkpoints

- Privacy behavior and its tests should land together; do not label a flow private until storage behavior is verified.
- Database migrations should be in place before any schema change.
- Measure performance before and after changing preview or animation code.
- Run focused tests and a debug build after tasks 1-3; run emulator focus/layout checks after tasks 4-6.

## Risks and open questions

- Android WebView data storage is process scoped; separate private storage may require a dedicated process and architectural work. A narrower, accurately labeled private mode is a possible interim choice.
- Cached page thumbnails need a storage and invalidation policy. A static favicon/title preview is the simplest first experiment.
- The existing `CLAUDE.md` describes older design and navigation behavior. Verify against current code and the current app before using it as a product spec.

See [todo.md](todo.md) for executable tasks and acceptance criteria.
