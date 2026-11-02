# Downloads layout verification — 29 October 2026

Source commit: `16b11ab`.

Downloads now uses the existing full-width sea-glass panel for both empty and populated states. The old populated-state marketing panel and narrow drawer were removed. The actual download directory appears to the right of the title as non-focusable metadata, with a bounded two-line path; the separate location card is gone. Long filenames use ellipsis. Filtering, grouped lists, download handling, and context-menu actions retain their existing implementation.

## Verification

- Debug compilation, Downloads lint, and optimized release compilation passed. The layout detector reported no findings; native screenshots supplied the visual evidence.
- A temporary local screenshot harness rendered the actual Downloads screen and root shell on an API 34 TV emulator at 1920×1080. It used an isolated in-memory repository rather than saved downloads. Empty and populated light/dark states, an empty Packages filter, a long filename, the file context menu, and a 1.3 font scale were inspected. The header stayed consistent, the directory remained beside the title, and the long text stayed inside its bounds.
- The existing two `DownloadActionsTest` cases passed, covering the chooser's temporary read access and completion handling without duplicate alerts. Together with the temporary capture harness, the instrumentation run completed three cases successfully.
- The harness source was removed after capture. The in-memory fixtures were discarded; no real files were downloaded for the layout captures. Synthetic screenshots and the harness remain local, and the emulator was stopped.

The initial local optimized APK remained version 0.1.1 and used the existing development certificate that matches the TV's installation. SHA-256: `e3bdac9997cdda26f7ad9b3d6ab057529d42cc6ef6ac9513b7d5cb76af5bbbf1`. This was not a new public release. The TV was unavailable during that initial pass; the completed physical follow-up is recorded below.

## Physical TV follow-up — 2 November 2026

The update was installed in place on the authorized LUMIO VISION TV, running Android 14 and WebView 156.0.8078.47. The baseline was six saved tabs and an empty download list. The matching app certificate allowed installation without uninstalling or clearing data; the original first-install timestamp was preserved.

- The empty screen displayed the directory to the right of Downloads. After three tiny HTTPS binaries completed, the populated screen retained the same full-width sea-glass panel. File rows measured 1568 pixels wide in the 1920-pixel viewport; the former marketing panel and separate directory card were absent.
- The Packages filter showed its empty state; All restored the completed files. Remote Down moved through all three files and scrolled the off-screen third item into view with its focus ring visible.
- The completion toast was captured. Long-press opened the file menu. Open with invoked Android's chooser, which reported no installed handler for the generic test binary; no handler or user media was opened.

Testing also found a browser defect: WebView can hand a navigation to its download listener without sending `onPageFinished`, leaving the native page progress bar stuck. Source commit `6fa6714` clears page progress at the download handoff, independently of whether enqueue succeeds. The regression failed before the fix; it passes afterward and verifies that a subsequent page load can display progress again. All 18 cases across browser recovery, top-navigation focus and download actions passed on the API 34 emulator. Browser lint and optimized release compilation passed.

The final patched optimized APK was installed on the TV, still using the matching development certificate and version 0.1.1. SHA-256: `2fb82bae2a9bfc76c4fec5ebe6f0449d215da94e075f9250c6a36af0c14deee8`. A further real HTTPS download completed. Two native UI samples confirmed the page progress indicator was absent after handoff and did not reappear later.

All four test download records and their size-checked files were removed. Both temporary test tabs were closed, restoring the original six-tab count and empty download list. Pebble was left on Home and the TV remained connected. The final app-process log had no fatal exception, fatal signal, out-of-memory or app ANR markers. No TV resolution, density, font scale, network configuration or app-data resets were used. Normal public test-page browsing history may remain; raw screenshots and diagnostics stay local.

These are functional checks, not a frame-timing benchmark. Live in-progress layout and forced failed-download retry were not physically exercised in this pass; existing automated coverage still applies. No public release or GitHub main branch was changed.
