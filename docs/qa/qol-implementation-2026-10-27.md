# Browser quality-of-life enhancements — 27 October 2026

Implements items 1–5 from the [review](qol-review-2026-10-27.md). All testing in this change used the owned Android TV API 34 emulator, 1920 × 1080 with WebView 155.0.8059.46. The physical TV was not accessed or updated.

## Available behavior

1. **Media controls:** Browse → **Page options** (the top-bar ⋮ button) offers Mute/Unmute tab and Background playback. Background playback is off by default and applies to the current tab for this process session. Leaving the active tab pauses HTML media and engine-mutes its audio when supported. Returning does not automatically restart playback. Delayed play events are also checked. Cross-origin DOM pause is best effort; engine mute covers their audio where the provider supports it. Opt-in allows playback but cannot override a service or OS policy that suspends it.
2. **Website preferences:** The Desktop/Mobile address-bar toggle now saves the choice for that exact normalized host. Page options also offers site mode, blocking and Reset website to defaults, showing whether defaults or overrides apply. Other hosts and subdomains retain their own choices/defaults. Global defaults remain in Settings. Website option changes can reload the page; blocking changes explicitly reload to apply the new choice.
3. **Download recovery:** Failed transfers show a readable reason and a Retry action. Retry starts a fresh transfer using current cookies and user agent, preserves the filename with normal collision handling, and removes the old record only after the replacement is created. Concurrent retries of one record cannot create duplicate replacements. Expired URLs may require a fresh link from the original webpage. Existing completed-file chooser/removal behavior is preserved.
4. **Page recovery:** Main-frame network failures and HTTP 5xx server errors show native Retry, Edit address and Home actions. Website-provided 4xx sign-in/challenge/not-found pages remain accessible. Subresource failures and stale failures belonging to another document do not replace the current page. On Android 8+, a terminated renderer's affected WebView is detached and discarded; native navigation stays available. A new view is created on explicit Retry, without a crash/reload loop. TLS errors are not bypassed.
5. **Reopen closed tabs:** Tabs offers Reopen tab after a normal tab is closed, retaining at most 10 recent entries in memory. It opens a new tab with the saved URL/title and ordering metadata. Private entries are excluded. This does not restore a form draft, complete Back/Forward history, exact scroll position or video time. The list clears when the process ends.

## Storage and compatibility

Room schema **2** adds nullable `downloads.failureReason`. The explicit version 1-to-2 migration preserves the existing bookmark, history, tab and download data. Existing failed records without a historic reason use the generic failure explanation. Destructive migration remains disabled.

No playback SDK, animation framework, paid service or production dependency was added. Existing WebKit is also referenced by instrumentation tests to inspect engine mute. The mute path checks `MUTE_AUDIO` support; renderer callbacks are scoped to supported Android versions. API 24 remains a CI target, but this local runtime pass used API 34.

## Verification

- **29 local unit tests passed**, including exact-host/IDN normalization, preference inheritance, failure messages and bounded/private-excluding recent-close history.
- **40 app emulator tests passed**, including 17 new checks for media policy, native recovery, stale errors, preference persistence, retry deduplication, reopened metadata and remote page-menu navigation.
- **Two database emulator tests passed:** current-schema reopen and a genuine exported-v1-file upgrade that verifies all four entity tables, the existing view, nullable reasons and subsequent persisted reason updates.
- Android lint, debug compilation, optimized release compilation and whitespace validation passed.
- Tests demonstrated failures before the fixes: absent closed-tab history, absent host override resolution, missing failure explanations, continued inactive media, absent error actions, an empty schema migration, a stale main-frame failure replacing a newer page, and an authentication response being covered by the recovery screen.
- The fullscreen integration test now waits for the previous native error panel to disappear and Chromium to render before injecting a real screen tap. Its original fullscreen and Back assertions remain intact. DOM readiness alone had allowed the tap to hit the old native Retry control and replace the fixture.
- The nested-Up test waits for wheel scrolling to settle between inputs; observing the first delta had left it using a stale positive scroll position after the feed had already reached zero. Its handoff assertions remain intact.

The existing CI job and developer command include the five new instrumentation classes. No additional CI job or hosted service was introduced. The Android testing skill's stack check confirmed Hilt, JUnit4, Compose UI tests, real WebViews and isolated Room fixtures; no mocking or screenshot-testing framework was needed.

## Additional emulator smoke checks

- A saved Mobile choice for `example.com` produced the Android WebView user agent, survived an app process restart and did not change the Desktop user agent on unrelated `httpbin.org`. Reset restored the inherited Desktop choice in this QA profile. This profile already used Desktop globally; the change did not alter that global preference.
- The page menu was inspected at its initial and scrolled positions, including the conditional Reset action and Close. Native D-pad navigation and automatic scrolling kept the actions reachable.
- An actual DevTools `Page.crash` terminated the active emulator WebView renderer. The app process remained **PID 8429**, native navigation and recovery actions remained visible, and pressing remote OK on Retry restored the page in a new renderer with the same app process. This manual check used one active page; multi-view bookkeeping is handled per affected WebView callback and was source-reviewed.
- The official hls.js VOD demo remained at ready state 4 through toolbar changes, fullscreen and Back. Media time advanced from **3.99 to 33.70 seconds** and decoded frames from **237 to 2053**. Adaptive quality reached 1080p with emulator frame drops; this is functional playback evidence, not physical-TV frame performance.

The real HLS demo also paused when its tab became hidden even with background playback allowed and the app's pause guard off. Native lifecycle handling now honors the opt-in; the menu explicitly notes that some sites pause when hidden. The offline host-lifecycle tests separately verify background audio progression and default pause/no automatic restart through actual Browse-to-Tabs transitions.

Run-local screenshots and sanitized diagnostics are under `/tmp/pebble-qol-qa/`; HLS samples are in `/tmp/pebble-responsive-qa/qol-hls-media.json`. The emulator has no audio output, so tests verify media state and engine mute rather than audible output. Cross-origin pause coverage, real-TV speaker behavior, DRM/subscription services and the existing Vimeo compatibility failure are not newly certified by this change.

Implementation references: [Android WebView lifecycle](https://developer.android.com/reference/android/webkit/WebView#onPause()), [WebKit mute feature](https://developer.android.com/reference/androidx/webkit/WebViewCompat#setAudioMuted(android.webkit.WebView,boolean)), [renderer termination handling](https://developer.android.com/develop/ui/views/layout/webapps/handle-termination), [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions).
