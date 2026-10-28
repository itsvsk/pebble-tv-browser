# Social and entertainment compatibility check — 27 October 2026

## Scope

Tested Pebble on an isolated Android TV API 34 ARM64 emulator at 1920 × 1080, using Google System WebView **155.0.8059.46**. The physical TV was not used, updated, or controlled during this work.

The initial pass covered **nine services in both Mobile and Desktop site modes** (18 page/mode checks). The same isolated, logged-out profile was retained between passes. Checks included native D-pad navigation, pointer edge scrolling, page geometry, screenshots, media state, and document/network failures. Existing blocker preferences were retained; no accounts were created or signed in, and no social actions were submitted.

These are public-page observations, not a claim that every authenticated feature of each service works. Site prompts and regional responses can change.

## Results

| Service and public test page | Mobile mode | Desktop mode | Media/account limits |
| --- | --- | --- | --- |
| [Instagram profile](https://www.instagram.com/instagram/) | Public profile renders; app/sign-up prompts appear. | Public profile and thumbnails render and scroll. | Full feeds, Reels playback, posting and signed-in actions not verified. |
| [Facebook page](https://www.facebook.com/facebook/) | Initially redirected to `fb://` and showed an error. After the fix, the public web page renders and scrolls with no recorded navigation error. | Public page, posts and embedded media render. | Buffered post media was observed, but a complete signed-in/playback journey was not verified. |
| [Reddit videos community](https://www.reddit.com/r/videos/) | Public feed renders and scrolls. | Public feed renders and scrolls. | Account actions and every external embedded player not verified. |
| [LinkedIn company page](https://www.linkedin.com/company/linkedin/) | Company page renders beneath a sign-in prompt. | Redirected to LinkedIn's authentication wall; HTTP 999 was observed. | No login or authenticated feed/media testing. |
| [YouTube public sample](https://www.youtube.com/watch?v=aqz-KE-bpKQ) | Video decoded and playback time advanced; page scroll works. | Video decoded and playback time advanced; page scroll works. | Public playback only; account and subscription features not tested. |
| [Twitch](https://www.twitch.tv/) | Live directory renders and scrolls. | Featured live media decoded. The nested feed initially stayed stuck; after the fix its inner scroll position advanced from 0 to 661.5 CSS pixels. | A site-paused/offscreen video is not classified as a decoder failure. Fullscreen and Back are also covered by the prior live-stream check and offline regressions. |
| [Vimeo public player](https://player.vimeo.com/video/76979871) | Player poster and controls render; explicit Play reaches a supported-browser error. | The same supported-browser error occurs after explicit Play, without decoded frames. | Playback failed in this emulator in both modes; the root cause and real-TV outcome remain unverified. |
| [Dailymotion](https://www.dailymotion.com/) | Home/discovery page renders with join and cookie prompts. | Home/discovery page renders with join and cookie prompts. | Playback beyond these prompts was not validated. |
| [Netflix](https://www.netflix.com/) | Public landing page renders and scrolls. | Public landing page renders and scrolls. | Paid playback, DRM and login were not tested. Landing-page success does not establish streaming support. |

YouTube's decoded frames were 640 × 360 in the mobile pass and 1280 × 720 in the desktop pass, with increasing media time in both. This verifies playback, not real-TV resolution or frame-rate performance.

## Browser issues found and fixed

### Nested scrolling

The previous pointer controller only scrolled the outer native WebView. Fixed-height feeds and chat panels could have thousands of pixels of inner overflow while `WebView.canScrollVertically()` returned false.

The controller preserves the existing smooth outer-document animation and falls back to native mouse-wheel input for nested overflow. It hit-tests beneath the pointer, with a bounded geometric search for a visible scroll panel when a fixed edge toolbar covers that point. Wheel coordinates stay inside the view. Up checks whether the inner feed can still scroll before handing focus to browser controls. Horizontal panels are covered too.

Asynchronous probes are scoped to the target, page URL and pointer-mode generation. A callback from an earlier attachment or mode cannot move focus or scroll a newly reopened page. Held repeats remain valid in an unchanged context.

### Unsupported app links

Facebook's mobile page navigated to an app-only `fb://` URI, replacing the website with `ERR_UNKNOWN_URL_SCHEME`. The browser now consumes unsupported app schemes before they reach the error page. Normal web/internal documents retain their WebView behavior. A user-initiated main-frame app link gets an informational message; automatic redirects stay quiet.

The fix does not parse or execute arbitrary Android intents, automatically launch external apps, grant permissions, or bypass a service's login policy. The real Facebook mobile page was retried successfully after the change.

## Verification

- **24 unit tests:** passed.
- **21 emulator regressions:** passed, including original scrolling/navigation/download/fullscreen checks and new nested Down/Up/Left/Right, top-control handoff, callback-lifetime and app-redirect tests.
- Debug build, Android lint and optimized release compilation: passed.
- The new nested-scroll and app-link tests failed before implementation and passed after the fixes.
- The reverse-scroll fixture waits for WebView's visual-state callback and subsequent draw; a DOM-only read can precede compositor scroll readiness.
- Real Facebook mobile and Twitch desktop scrolling were retried after implementation. An emulator DNS failure during retries was diagnosed separately and repaired only in the emulator.

The deterministic tests are included in the existing Android TV CI selection. Public internet sites remain manual smoke checks, so service availability and login walls do not make CI flaky. See the [developer guide](../DEVELOPMENT.md) for commands.

## Remaining boundaries

Signed-in feeds, messages, posting, uploads, camera/microphone consent and paid DRM sessions remain outside this logged-out pass. Cross-origin iframe and closed-shadow-root scrolling have not been exhaustively validated. Vimeo's observed playback restriction needs a separate service/engine investigation; it must not be counted as a pass merely because its poster loads.

Read-only page metrics and screenshots are saved in the run-local `/tmp/pebble-social-qa/` directory. They are not release assets or committed website captures.

Implementation references: [Android wheel events](https://developer.android.com/reference/android/view/MotionEvent#ACTION_SCROLL), [WebView visual-state synchronization](https://developer.android.com/reference/android/webkit/WebView#postVisualStateCallback(long,%20android.webkit.WebView.VisualStateCallback)). General codec/DRM boundaries are documented in the [streaming guide](../STREAMING.md).
