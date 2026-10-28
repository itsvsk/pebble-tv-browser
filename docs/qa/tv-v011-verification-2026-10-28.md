# v0.1.1 installation and physical TV verification

Verified on 28 October 2026 using a LUMIO VISION TV running Android 14 and Android System WebView 156.0.8078.47. Tests used normal app controls and remote input. No TV resolution, density, font scale, or other display settings were changed.

## Installation and data preservation

The installed development-signed v0.1.0 app could not accept the differently signed public v0.1.1 APK. Android rejected that update before changing the installation. Rather than uninstalling or clearing data, a copy of the published optimized APK was signed with the existing matching local development certificate and installed in place. Its original application entries were verified unchanged; only signing metadata was added. The public GitHub artifact was not modified.

After the fixes below (source through `c56c3fb`), a fresh optimized release build using that same matching development certificate was installed in place. It reports version 0.1.1, version code 1001. Its SHA-256 is `a41c77df8bdddb73e46f23c80aa2d811e89c38c18db3b27ee3ac220e0bfe692c`. This is a local TV verification build, not the public release-signed artifact. The original installation date remains unchanged.

The original six tabs, saved bookmark, and two completed downloads were preserved. A seventh test tab and a new 32-byte download were removed after testing; only that test file was deleted. Original library labels were compared with the baseline capture. Normal browsing history from the public test pages remains. Raw screenshots, UI trees, APKs and logs stay local because they can contain personal browsing information.

## Issues found and fixed

- Settings hardcoded version 1.0 even when the installed package was 0.1.1. It now reads the installed package version once when its header is composed. The corrected version was verified on the TV.
- Using the error screen's Edit address action left a request pending in the browser host. Hiding and recreating the toolbar replayed that request, reopening the URL editor and keyboard without another selection. The address bar now acknowledges the request and the host clears it. On the final TV build, error editing still worked, pointer Down hid the controls, and Up restored them without reopening the native editor or keyboard. The focus returned to the native navigation controls. Webpage search fields were excluded when inspecting native editor state.

The regression failed before the address-request fix and passed afterward. An additional second-error Edit assertion verifies that acknowledging a request does not prevent future editing. The API 34 emulator passed all 14 cases in `BrowserRecoveryTest` and `TopNavFocusTest`. Browser and Settings lint, debug/test compilation, and the optimized signed release build passed. An independent subagent reviewed the final four-file source/test diff and found no blockers.

## TV checks completed

- Home's left navigation wrapped to Settings. Tabs Down reached New tab, another Down reached the first tab, and Up returned through New tab. Creating a temporary tab worked.
- Clear address opened an empty focused native URL editor. A nonexistent test domain displayed Retry, Edit address and Home recovery actions. Editing, dismissing the keyboard, returning to Retry and retrying remained usable. Navigating to a valid public article recovered normally.
- Pointer navigation scrolled a long Wikipedia article and hid the browser bars. Returning to the top restored the bars. The final patched build did not replay address editing during that cycle.
- The official DASH-IF live sample displayed decoded moving video. Its encoded clock advanced from 00:01:35:27 to 00:02:08:08 while fullscreen; remote Back exited fullscreen and playback continued at 00:02:12:23. Fullscreen entry used the page's own button. The test tab was muted and the stream was stopped before cleanup.
- The live sample's fullscreen host occupied the TV viewport, but the picture remained narrower. Inspection of the sample's [deployed CSS](https://reference.dashif.org/dash.js/latest/samples/lib/sample.css) found a 640 CSS-pixel video maximum width, matching the observed width at the TV's pixel ratio. This is a website layout limitation; no global website CSS override was introduced.
- A 32-byte HTTPS attachment completed and appeared in Downloads. The completion toast was captured. Opening it invoked Android's chooser, which reported no compatible installed app for the generic binary. Long-press exposed Open with and Remove from list. Removing the test record restored the two original completed downloads.
- The final running app process had no fatal exception, fatal signal, out-of-memory or app ANR markers in the captured log. Pebble was left on Home without test media playing, with the TV still connected.

## Limits

These are functional checks, not a frame-timing benchmark or certification of every website. Audible speaker output cannot be established from screenshots. DRM and subscription services were not certified. Failed-download retry, migration and other media lifecycle cases retain their existing automated coverage; they were not all repeated on the physical TV during this pass. The two new fixes have not been published to GitHub or released as a new version.
