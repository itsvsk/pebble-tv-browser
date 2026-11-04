# Ad and tracker protection verification — 4 November 2026

## Result

Implementation and independent code review completed on `codex/adblock-protection`. All required review findings were resolved. The existing unrelated roadmap files were preserved; the reviewed feature plan and task list are in [tasks/adblock-protection](../../tasks/adblock-protection/plan.md).

- **93 JVM unit cases passed**, zero failures/errors: adblock 42, WebView 29, preferences 6, cursor 11 and downloads 5.
- **30 distinct emulator cases passed** across the new protection tests and affected existing browser suites. The 29-case regression command passed; the final five-case real-WebView class rerun added the cached-subscription case and retained its four original passing cases.
- `lintDebug`, debug APK, optimized release APK and debug instrumentation APK builds passed. The release APK retains OkHttp's `publicsuffixes.gz` resource.
- Independent five-axis code review approved the implementation after the fixes below. `git diff --check` is clean.
- Existing CI API 24/34 jobs now include all three protection instrumentation classes. No job, runner tier, credential, signing or publication setting was added.

## Environment

Android TV API 34 emulator (`Pebble_Readme_TV_API34`, arm64), Android System WebView `156.0.8078.47`, native 1920×1080 capture. Emulator ran headless with SwiftShader and audio output disabled. A graphics disconnection interrupted one attempted rerun; the emulator was restarted with Vulkan disabled, then the complete 29-case command passed. No physical TV or TV display settings were used or changed.

SDK path: `/Users/vishal/Library/Android/sdk`. JDK: local OpenJDK 21. Gradle wrapper and cached dependencies used offline for builds.

## Plan and review

Applied planning-and-task-breakdown, incremental-implementation, git-workflow-and-versioning, test-driven-development, code-review-and-quality, security-and-hardening, android-emulator-qa and ci-cd-and-automation skills.

Subagents independently reviewed the plan/core/integration, implemented rule-engine tests and real WebView fixtures, and implemented/reviewed Settings/preferences and UI acceptance tests. The reviewed scope is a bounded host-rule upgrade, not a full ABP engine, cosmetic blocker, private browser or system-wide blocker.

Required findings resolved:

- Preserve supported domain/party restrictions and exception precedence; skip unsupported blocking syntax instead of widening it.
- Conservatively widen recognized path/type/case exceptions within evaluable scope. Do not treat cosmetic or unknown exceptions as general network allowances; do not guess document navigation-initiator party state.
- Keep original per-source snapshots, validate the remote ABP header/nonempty supported block set, cap response size, cancel transport and preserve prior working sources after partial failure.
- Reuse validated parsed snapshots rather than reparsing whole subscriptions twice.
- Restore focus to mounted targets before removing override rows and after closing dialogs. Initial dialog focus must wait until subcomposed/lazy targets attach.
- Keep temporary pauses by logical tab/exact host, including view eviction/recreation; capture pause identity atomically and reject stale-host UI actions.
- Canonicalize document URLs without fragments, begin an explicit provisional document on reload, and coalesce main-frame interception/page-start callbacks in either order.

## Regression evidence

The first parser reproductions ran against the prior implementation: six cases, five new failures. New matcher tests exercise supported modifier semantics, scoped exceptions, cross-list badfilter, unsupported rules, IDNs and public/private suffix relationships.

A malformed HTTP-200 response containing a rule snippet reproduced an unsafe successful replacement; header validation now rejects it and preserves prior data. Cache/refresh tests cover raw-text round trip, checksum corruption, unfinished temporary files, invalid/empty/oversize replacements, UTF-8 byte bounds, cancellation, serialized refresh, last-good source preservation and process reconstruction from saved sources.

Actual WebView tracing exposed a counter race: Chromium requested a subresource before or concurrently with `onPageStarted`; replacing context in that later callback lost the count. Two additional unit cases failed before correction. The final state machine pairs main-resource/start callbacks, preserves early requests and resets genuine reloads, including canonical URL/fragment cases. The real WebView class now passes all five cases, including cached conditional rules and exceptions.

A deliberate exception-precedence mutation inverted the matcher result: **19 of 21 matcher tests failed**. The original code was restored automatically before the final build and test gates.

The browser UI test originally requested semantic native focus while the pointer was active. It now follows the real remote Menu handoff before native Select; production pointer defaults remain intact. No failed assertions were removed or replaced with semantic clicks.

## Emulator coverage

New classes:

- `AdblockWebViewTest` — five real WebView cases: blocked versus allowed scripts; blocklisted main document remains navigable; global off/on; pause/resume; reload counts; cross-host cleanup; cached third-party restrictions and site exceptions.
- `ProtectionSettingsTest` — three actual-app cases: remote entry, global default persistence, list/update access, independent reset retaining Desktop mode, row-removal focus and dismissal focus.
- `BrowserProtectionUiTest` — two actual-app cases: saved exception/reset with Desktop preserved and stable focus, temporary pause/resume without saving an exception. Uses real Menu/Select input and verifies the selected fixture host.

Existing suites exercised: `TopNavFocusTest`, `PageOptionsTest`, `StreamingLayoutTest`, `BrowserRecoveryTest`, `ResponsiveViewportTest`.

WebView fixture responses are owned HTTPS HTML/JS, with isolated filter-cache directories. Temporary website preferences and seeded tab records are cleaned up. The global protection default is restored. No test requires third-party website markup or a live stream.

## Manual update and visual check

The actual **Update filter lists now** action successfully fetched both official subscriptions on the emulator. UI reported:

| Source | Supported rules | Outside the supported subset |
| --- | ---: | ---: |
| EasyList | 45,930 | 32,202 |
| EasyPrivacy | 48,030 | 9,244 |

These are source-specific rule counts at this run, not distinct blocked hosts, unique trackers, total active rules after cross-list deduplication, or a completeness score. Original-source snapshots were saved independently (about 2.06 MB and 1.51 MB including metadata). This confirms the earlier workstation HTTP 403 did not prevent the emulator's update.

Captured and inspected the dark sea-glass Settings and browser shield panels, keyboard/remote focus and downloaded-list status. Manually traversed the address toolbar with the remote on a normal public document and opened the shield. Local captures are `pebble-protection-settings-2026-11-04.png` , `pebble-protection-lists-2026-11-04.png` and `pebble-protection-browser-2026-11-04.png` in the Codex visualization artifact directory. Source cards can be reached/read with the remote and the fixed Done control stays available while content scrolls.

## Commands and logs

With `ANDROID_HOME` and `JAVA_HOME` set to the paths above:

```sh
./gradlew testDebugUnitTest lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --offline --console=plain
./gradlew testDebugUnitTest --offline --console=plain
adb -s emulator-5554 shell am instrument -w -r -e class com.tvbrowser.app.AdblockWebViewTest,com.tvbrowser.app.ProtectionSettingsTest,com.tvbrowser.app.BrowserProtectionUiTest,com.tvbrowser.app.TopNavFocusTest,com.tvbrowser.app.PageOptionsTest,com.tvbrowser.app.StreamingLayoutTest,com.tvbrowser.app.BrowserRecoveryTest,com.tvbrowser.app.ResponsiveViewportTest com.tvbrowser.app.debug.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am instrument -w -r -e class com.tvbrowser.app.AdblockWebViewTest com.tvbrowser.app.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Final local evidence: `/tmp/pebble-protection-final-checks.log`, `/tmp/pebble-protection-unit-final.log`, `/tmp/pebble-protection-acceptance-emulator.log`, `/tmp/pebble-protection-cached-rules-emulator.log`. Earlier RED/mutation/diagnostic logs use the same `pebble-` prefix in `/tmp`; they are not release assets.

## Limits

API 24 is configured in CI but was not run locally for this change. Tests used the debug app; the optimized release artifact was built and its PSL resource checked, not installed as a public signed release. No audible-output, frame-time benchmark, live-service ad-removal or physical-TV claim is made.

This remains supported host-based network filtering. Unsupported path/type blocking rules, cosmetics, same-host/in-stream ads, service-worker traffic and Android interception limitations remain as documented in [ADBLOCK.md](../ADBLOCK.md). Conservative exception widening can allow extra requests. No new external filter service, analytics collection or native engine dependency was introduced.
