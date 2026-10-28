# Pebble enhancement and quality-of-life review — 27 October 2026

## Basis

Reviewed the current browser, WebView lifecycle/cache, tabs, downloads, library, preferences and existing roadmap using the code-review-and-quality and performance-optimization skills. Findings combine source inspection at `2f417d4` with the [recent emulator compatibility evidence](responsive-streaming-2026-10-27.md). No new runtime, audio, performance or physical-TV testing was performed for this review. No app behavior was changed.

Effort labels below are relative scope, not delivery estimates. These are recommendations; code gaps do not establish how often a user encounters a failure.

## Recommended order

| Order | Enhancement | Evidence and user benefit | Scope |
| --- | --- | --- | --- |
| 1 | Explicit background-media behavior and per-tab mute | WebViewHost currently calls `onPause()` on lifecycle changes/release, but has no explicit media pause/mute policy. Give users a reliable way to silence a tab and choose whether playback continues away from Browse. | Medium |
| 2 | Remember settings per website | The address-bar Desktop/Mobile toggle writes the global preference; ad blocking is also global. Preserve the working combination for each website without changing unrelated sites. | Medium |
| 3 | Retry failed downloads and explain the failure | The failed-download menu only offers removal. Show a useful reason and a focusable Retry action after a transfer fails. | Small–medium; medium if failure reasons must survive restarts |
| 4 | Native page-error UI and renderer recovery | TabWebViewClient has no main-frame error UI callback or renderer-termination handling. Offer Retry, Edit address and Home; preserve the last URL if a renderer is lost. | Medium |
| 5 | Reopen the last closed tab | Closing a tab immediately destroys its WebView and deletes its record. Offer Undo / Reopen closed tab, restoring URL and title first. | Small–medium |
| 6 | Find in page, then library search | No find-in-page controls or history/bookmark search flow are present. Reduce repeated D-pad scrolling on long articles and saved-site lists. | Small–medium per feature |

### 1. Explicit media policy

Source: [WebViewHost lifecycle and release](../../feature/browser/src/main/kotlin/com/tvbrowser/feature/browser/WebViewHost.kt), [live WebView cache](../../core/webview/src/main/kotlin/com/tvbrowser/core/webview/TabManager.kt), [streaming boundaries](../STREAMING.md).

Proposed behavior: pause media when leaving the active browser tab or backgrounding the app by default, with a clear opt-in for background playback. Add a tab mute action; show a muted state accurately. Only label a tab as currently playing if playback activity is actually detected. Resuming a tab should not unexpectedly restart media the user had paused.

Android documents `WebView.onPause()` as best effort and notes that it does not pause JavaScript. AndroidX WebKit exposes `setAudioMuted()` with a `MUTE_AUDIO` capability check; the API was added in 1.11.0, while Pebble already uses WebKit 1.12.1. Library availability does not guarantee provider support. Mute is not pause: muting alone does not stop decoding or network use. A JavaScript-only pause fallback must not be advertised as covering every cross-origin player. [Android WebView lifecycle](https://developer.android.com/reference/android/webkit/WebView#onPause()), [WebKit mute API](https://developer.android.com/reference/androidx/webkit/WebViewCompat#setAudioMuted(android.webkit.WebView,boolean)).

Verify: two media tabs, user-paused media, leaving Browse for Home/Library, app background/foreground, fullscreen/Back, unsupported mute providers and an embedded cross-origin player. Audible output needs a separate audio-enabled check; the earlier emulator run used no audio output.

### 2. Website preferences

Source: [global mode change](../../feature/browser/src/main/kotlin/com/tvbrowser/feature/browser/BrowserViewModel.kt), [global settings](../../core/common/src/main/kotlin/com/tvbrowser/core/common/Prefs.kt).

Keep global defaults and add explicit site overrides for Desktop/Mobile and blocking. Show the effective choice and a Reset to default action in a compact page menu. Apply overrides on navigation and redirects, not just when a tab is first created. Start with an exact normalized host key; do not silently share overrides with every subdomain or disable blocking automatically on playback failures.

Verify: two sites with different modes, cross-site redirects, cached tab reopen, app relaunch, reset behavior and effective-mode icon consistency. User-agent changes can reload the page, so avoid silently changing mode during form editing.

### 3. Recoverable downloads

Source: [failed card and context menu](../../feature/downloads/src/main/kotlin/com/tvbrowser/feature/downloads/DownloadsScreen.kt), [progress reason](../../core/downloads/src/main/kotlin/com/tvbrowser/core/downloads/DownloadProgress.kt), [persisted record](../../core/db/src/main/kotlin/com/tvbrowser/core/db/entity/DownloadRecord.kt).

Download-start errors already have Retry; this recommendation covers failures after enqueue. Translate system status/reasons into useful text, such as insufficient space or HTTP failure. Do not assume every failure has an available precise cause. Re-enqueue on explicit Retry, disable duplicate activation while starting, and keep focus on a predictable item. Use current cookies/user-agent where needed; do not persist authentication headers as a retry shortcut. Signed or expired URLs may require returning to the original webpage.

Failure reasons currently exist in progress data but not the persisted record. Durable reasons require a deliberate Room migration and migration tests. Keep “Remove from list” distinct from deleting a completed file.

Verify: successful retry, repeat failure, unavailable storage, missing system download row, duplicate remote clicks, expired source and app restart.

### 4. Page failures and renderer recovery

Source: [TabWebViewClient](../../core/webview/src/main/kotlin/com/tvbrowser/core/webview/TabWebView.kt), [cached view ownership](../../core/webview/src/main/kotlin/com/tvbrowser/core/webview/TabManager.kt).

Handle main-frame failures without replacing usable pages when only an image, ad or background request fails. Renderer termination requires discarding the affected WebView and creating a fresh one, rather than reloading the dead instance. Handle every affected cached view, because multiple tabs can share a renderer. Avoid an automatic crash/reload loop. Keep TLS/security failures fail-closed. Renderer termination callbacks apply from Android 8; retain the Android 7 path. [Android termination guidance](https://developer.android.com/develop/ui/views/layout/webapps/handle-termination).

Verify: offline/DNS failure, main-frame HTTP error, subresource failure that leaves the page usable, renderer loss affecting multiple cached tabs, explicit Retry and preserved tab metadata.

### 5. Undo tab close

Source: [TabsViewModel.close](../../feature/tabs/src/main/kotlin/com/tvbrowser/feature/tabs/TabsViewModel.kt).

Offer an accessible Undo or Reopen last closed tab action with predictable remote focus. A bounded in-memory recent-close entry is sufficient for a first version. Restore URL/title and clearly define the scope; restoring those does not restore an unsent form, exact scroll position, video time or complete navigation history. Preserve the current private-tab exclusion policy.

Verify: close first/middle/last tab, undo after the list reflows, repeated close/undo, URL/title restoration and Back behavior.

### 6. Search without long scrolling

Source: [address controls](../../feature/browser/src/main/kotlin/com/tvbrowser/feature/browser/AddressBar.kt), [history query](../../core/db/src/main/kotlin/com/tvbrowser/core/db/dao/HistoryDao.kt), [bookmark query](../../core/db/src/main/kotlin/com/tvbrowser/core/db/dao/BookmarkDao.kt).

Start with Find in page: query, match count, Previous/Next and Close, all reachable with the remote. Keep it separate from editing the address. Then add title/URL search to Bookmarks and History. History currently exposes a capped recent list (default 500), so decide whether search covers that visible subset or all stored history; do not present a recent-only filter as a complete history search.

Verify: no matches, many matches, clearing text, navigating to a new page while searching, Back restoring the previous focus and empty library results.

## Performance work worth doing next

Add release-like startup and navigation benchmarks for cold launch plus immediate D-pad input, Home → Browse, tab return and long-list scrolling. Only then evaluate app-specific Baseline Profiles, memory-pressure-aware WebView retention and other changes. The current cache retains at most eight WebViews, but source inspection alone cannot tell whether that limit is appropriate for a particular TV. Do not lower it blindly and increase reloads.

Macrobenchmark can compare startup/frame timing with and without profiles. Android recommends physical-device measurements; emulator results share host resources and are unsuitable as proof of real-TV smoothness. This is a future controlled measurement task, not permission to interrupt the user's TV session or change its display configuration. [Android profile measurement guidance](https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile).

## Already present or deliberately deferred

- Address clearing/editing, pointer browsing, download start/completion/failure alerts, downloaded-file app chooser, theme selection and an empty-bookmarks Browse action already exist.
- Custom shared clickable controls already expose click/long-click semantics. Menu website previews are static identity cards; they do not load extra WebViews. Some older tasks in `tasks/todo.md` need reconciliation before reuse.
- Vimeo remains an unresolved compatibility investigation, not a reason to promise universal streaming support or add a playback library without a demonstrated fix.
- Private browsing should remain unavailable until cookies and site storage are isolated. The in-memory private-tab metadata alone is insufficient.
- No new animation library, streaming SDK or paid service is justified for the first three recommendations. The existing stack covers their basic needs; provider-dependent capabilities still need runtime checks.
