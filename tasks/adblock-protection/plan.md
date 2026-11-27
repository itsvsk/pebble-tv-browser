# Ad and tracker protection implementation plan

Date: 2026-11-04. Branch: codex/adblock-protection.

## Scope and review

Implement the reviewed host-rule protection upgrade. Preserve the unrelated, incomplete tasks/plan.md and tasks/todo.md roadmap; this feature's task list is todo.md in this directory.

An independent planning subagent reviewed the design before production implementation. Its required changes are incorporated below: conservative unsupported exceptions, original source caching, immediate in-memory fallback, independent protection resets, exact-host settings, explicit coverage limits, and no misleading Standard/Strict modes.

## Decisions

- Retain the lightweight Kotlin/OkHttp architecture; no Rust/JNI engine, VPN, certificate installation or new remote service. Existing EasyList/EasyPrivacy source endpoints remain.
- Parse only anchored host rules with supported party/domain restrictions, network exceptions, document exceptions and canonical badfilter handling. Never flatten unsupported paths/type/rewrite rules into unconditional blocks. Conservatively widen unsupported exception resource/path scope only while retaining evaluable host/domain/party constraints, preferring missed blocks to broken sites. Document the supported subset.
- Seed protection from one in-memory bundled source immediately; load richer validated source snapshots on IO. Warm public-suffix data off the UI thread. Request matching uses immutable snapshots and a canonical page context captured from navigation and main-document request callbacks; no request-time disk/network or view.url access off UI.
- Keep original validated source text plus versioned metadata per list in atomic files. Preserve previous successful sources independently on refresh failure. Serialize manual and worker updates; bound bytes/timeouts; honor cancellation; never replace working data with empty/corrupt data. Ignore legacy hosts.txt because its constraints were already discarded.
- On/Off remains the global default with exact-host overrides. Add independent blocking reset, manageable saved overrides, per-visit pause with explicit tab/host scope and automatic clearing on cross-host navigation, and a shield protection panel reachable from Browse.
- Show requests blocked for the current document, source readiness/fallback/update age/errors and Update now. Batch statistics updates so network callbacks do not drive UI recomposition directly. Counters stay in memory and do not retain URLs or tracker logs.
- Main-frame documents remain navigable; filtering applies to supported network subresources. Do not return an unexplained blank page for a blocked top-level URL.
- No cosmetic/annoyance/Strict modes or service-worker interception in this iteration. Service-worker callbacks lack owning-tab context; document this limit instead of applying the foreground tab's exceptions to unrelated requests. No promise of all streaming-ad removal.
- Cookie/data-deletion controls and the other general app improvements are separate work, not prerequisites for this ad-block repair.

## Slices and dependencies

1. Rule correctness (parser + immutable matcher + regression suite): covers modifier/exception precedence, disabled rules across lists, domain boundaries, IDN/case/trailing dot, same-party public suffixes, unsupported-rule behavior.
2. Durable subscriptions (depends on 1): per-list snapshot store + bounded transport, last-known-good/fallback/status, worker and manual refresh; test failures, corrupted caches, partial refresh and serialization.
3. Usable protection (depends on 2; prefs/UI may develop against contract in parallel): settings status/refresh/managed overrides, browser shield/per-visit pause/document counts, navigation policy integration, scoped resets.
4. Verification and final review: full unit/lint/build checks; owned WebView request fixtures; emulator D-pad/settings/exception/pause/relaunch tests; independent code review and resolve required findings; documentation/changelog.

## Acceptance gates

- Previously failing parser regressions pass; relevant existing tests remain passing.
- Conditional rules do not become broader blocking rules; supported exceptions work; unsupported exceptions do not leave a contradictory broad block in effect.
- A failed/partial/interrupted update preserves usable protection and reports the source error. A corrupt cache falls back safely. Startup never exposes an empty engine.
- Global defaults and exact-host overrides persist. Independent reset never changes Desktop/Mobile. Temporary pause does not become a saved exception, and does not affect another host or tab.
- Main-frame navigation, allowed subresources and blocked subresources behave correctly in actual WebView fixtures; page counters reset per document and are not attributed across stale navigations.
- All new controls are remote-accessible with predictable dialog dismissal and exception-row-removal focus. Visible wording matches implemented capabilities.
- No TV use is required. Use the emulator; leave physical TV and display settings untouched.
- Document test commands, results, residual limitations and independent review. Keep changes in reviewed commits; no release or merge requested in this turn.

## Risks

The subset intentionally cannot represent full ABP resource/cosmetic semantics. Unsupported exceptions may conservatively allow more traffic. Filter availability depends on upstream HTTP access; show failures and retain local data. Public suffix loading and rule compilation stay on IO. Larger lists must not turn request matching into a linear scan over all rules. Tests must avoid requiring live third-party services.
