# Responsive layouts and live streaming — 27 October 2026

## Environment and scope

Tested an isolated Android TV API 34 ARM64 emulator at 1920 × 1080 with Google System WebView **155.0.8059.46**. The physical TV was not queried, controlled or updated. Public pages were tested while logged out; forms were not submitted and no purchases or account actions were performed. Existing ad-blocking preferences were retained.

This pass extends the [social and entertainment checks](platform-compatibility-2026-10-27.md). It distinguishes rendered layout, interactive response and actual decoded playback. It does not certify every page, authenticated service, codec or DRM session.

## Public-page layout checks

The six pages below were opened in both actual **Mobile site** and **Desktop site** modes, giving 12 page/mode checks. Native Down inputs exercised pointer edge scrolling and toolbar hiding. Screenshots and read-only DOM metrics were captured before and after scrolling.

| Page type | Public sample | Result in both modes |
| --- | --- | --- |
| Article | [Wikipedia: Internet](https://en.wikipedia.org/wiki/Internet) | Text and navigation render; page scrolls. Mobile and desktop versions adopt their respective layouts. |
| News | [BBC News](https://www.bbc.com/news) | Headlines, images and navigation render; document scrolls by 648 CSS pixels in the sampled input sequence. Publisher-reserved whitespace remains part of the webpage. |
| Catalogue | [Google Merch Shop](https://shop.merch.google/) | Product imagery and site controls render; document scrolls by 648 CSS pixels. A cookie prompt overlays the page; checkout was not tested. |
| Image grid | [Unsplash](https://unsplash.com/) | Hero, categories and image content render; document scrolls by 648 CSS pixels. Account and download flows were not tested. |
| Form | [httpbin form](https://httpbin.org/forms/post) | Labels, text fields, radio buttons and checkboxes render. The short document scrolls to its lower limit, about 108.5 CSS pixels after the bars hide. |
| Map | [Leaflet quick-start example](https://leafletjs.com/examples/quick-start/example.html) | Map tiles, markers, popup and zoom controls render. Its author-defined fixed map rectangle is preserved; the document has no vertical overflow. |

A follow-up on the updated build verified editing the form's name field with native input (`Pebble_QA`, no submission). A measured tap on the map's zoom-in control changed the old tile layer scale from 1 to 2 and loaded a new tile layer (8 → 12 loaded tiles).

None of these sampled documents created horizontal document overflow (`scrollWidth <= innerWidth`). Most viewport heights changed from **410 to 540 CSS pixels** as Pebble's bars hid. Pages without a responsive viewport declaration used WebView's scaled wide viewport, for example the form's 980 CSS-pixel width. This is distinct from the native WebView collapsing or losing height.

Some pages recorded network/JavaScript failures despite visible content: BBC fetch/XHR failures, catalogue connection refusals and an uncaught exception, a desktop Unsplash HTTP 401 document response, and HTTP 404 responses for other resources on two mobile pages. These checks establish the observed layout and scrolling, not complete service health or the success of every third-party request.

## Live and on-demand playback

Each playback check sampled media state every five seconds over roughly 30 seconds after an initial load wait. The sequence included native remote input, toolbar visibility changes, a user-gesture fullscreen request, a fullscreen screenshot, and native Back. Playback was assessed using ready state, advancing media time, decoded frames and buffered/seekable ranges, rather than posters or `canPlayType()` alone.

| Source, Desktop mode | Observed playback | Fullscreen and Back |
| --- | --- | --- |
| [DASH-IF live simulator](https://livesim.dashif.org/assets), `testpic_2s/Manifest.mpd`, in the [dash.js URL-parameter sample](https://reference.dashif.org/dash.js/latest/samples/getting-started/load-with-url-params.html) | Genuine dynamic live playback. In the final updated-build check, media time advanced from 1793096799.27 to 1793096823.70; decoded frames rose from 64 to 751. The moving live seek-window end advanced by about 31.2 seconds. Decoded size: 640 × 360. | Fullscreen filled the window. A brief buffer underrun near entry recovered by the next sample; Back restored the page and bars, and playback advanced afterward. |
| [Twitch public live channel](https://www.twitch.tv/djbomtokyo) | Live playback time advanced from 2.56 to 34.38 seconds, decoded frames from 84 to 1039, and the buffered end from 3.50 to 36.80 seconds. Duration remained infinite. Adaptive quality selected 720p and 1080p during the run. | Playback time continued advancing through fullscreen and Back, with the page restored afterward. |
| [hls.js official demo](https://hlsjs.video-dev.org/demo/) using its default Big Buck Bunny source | **HLS VOD**, not a live-stream test. Time advanced from 4.11 to 33.56 seconds and decoded frames from 241 to 2022; HLS manifests and transport-stream segments returned HTTP 200. | Playback continued through fullscreen and Back. The player adapted from 720p to 1080p. |
| [Vimeo public player](https://player.vimeo.com/video/76979871) | **Failed.** Explicit Play produced “Please try again in a supported browser.” Media time and decoded frames remained zero; no buffered range developed. The earlier pass reproduced this in both site modes. | Not counted as a streaming pass merely because controls or a poster rendered. |

The DASH sample was loaded with a raw `url=https://livesim.dashif.org/livesim2/testpic_2s/Manifest.mpd` query value and `autoplay=true`; its current sample parser does not decode a percent-encoded source URL. The initial encoded test was corrected before the passing run.

Twitch's homepage carousel also played live video, but its source/timeline changed during the observation. The channel page above was tested separately to establish continuous playback on one public stream. Channel availability can change after this dated check.

The emulator dropped frames, particularly when adaptive players selected 1080p. These are functional playback results, **not evidence of smooth real-TV frame performance**. The emulator was started without audio output, so audio decoder activity does not establish audible output, lip-sync or surround-sound behavior. The first DASH run showed startup buffering before reaching ready state 4. The updated-build retry reached ready state 4 at the first sample, then briefly reached the buffered end near fullscreen entry (ready state 2) and recovered to ready state 4 afterward; this was not counted as uninterrupted playback.

### Vimeo investigation

The WebView reports H.264/AAC capability, and the player creates a MediaSource-backed `blob:` URL, but neither fact establishes successful playback. A follow-up trace captured aborted XHRs to `vod-adaptive-ak.vimeocdn.com`; no decisive media decoder error was reported. The service's error and the absent decoded frames remain the reliable observations. The precise failure cause and physical-TV outcome are unresolved; no blanket permission grant, user-agent spoofing workaround or external-player bypass was added.

## Resize race fixed

A deterministic test reproduced a pending pointer wheel probe dispatching after its native viewport changed. Its hit-test coordinates belonged to the earlier viewport, so a delayed callback could scroll at the wrong point after the bars resized the page.

`CursorController` now invalidates pending web probes when a positive viewport width or height actually changes. Unchanged layout callbacks retain valid held-key work. Existing URL, target-attachment and pointer-mode guards remain in effect. The reproduction failed before the fix (one stale wheel event instead of zero).

The new offline responsive fixture uses the real WebView factory and checks **720 × 480 → 1280 × 720 → 1920 × 1080 → 720 × 480 native bounds**. It verifies CSS viewport dimensions, nonzero/resized `100vh`, long article text, image bounds, form controls, horizontal overflow and a 16:9 video surface after rendered-state synchronization. The video surface check is layout-only; real decoding is established by the public playback checks above.

## Verification

- **24 unit tests:** passed.
- **23 emulator regressions:** passed, including the new responsive fixture and the resize callback regression, plus navigation, pointer scrolling, nested panels, downloads, fullscreen and app-link handling.
- Debug and optimized release compilation, Android lint and `git diff --check`: passed.
- The stale callback test failed before implementation and passed afterward. A separate review found no required source/test/CI changes.

## Reproduction and boundaries

The [developer guide](../DEVELOPMENT.md) and existing Android TV CI selection include `ResponsiveViewportTest` and the callback-lifetime regression. Deterministic fixtures are offline; public-site availability is kept out of CI.

Run-local screenshots and sanitized metrics are in `/tmp/pebble-responsive-qa/`; website captures and signed streaming URLs are not committed as product assets. The app's [streaming guide](../STREAMING.md) describes System WebView, fullscreen, DRM and device-codec boundaries. Subscription/DRM playback, authenticated feeds, all embedded iframe players, background-media lifecycle, real-TV decoding performance and audio output require separate checks.

Reference implementations: [hls.js](https://github.com/video-dev/hls.js), [DASH-IF live simulator](https://github.com/Dash-Industry-Forum/livesim2), [Android WebView visual-state synchronization](https://developer.android.com/reference/android/webkit/WebView#postVisualStateCallback(long,%20android.webkit.WebView.VisualStateCallback)).
