# Downloads layout verification — 29 October 2026

Source commit: `16b11ab`.

Downloads now uses the existing full-width sea-glass panel for both empty and populated states. The old populated-state marketing panel and narrow drawer were removed. The actual download directory appears to the right of the title as non-focusable metadata, with a bounded two-line path; the separate location card is gone. Long filenames use ellipsis. Filtering, grouped lists, download handling, and context-menu actions retain their existing implementation.

## Verification

- Debug compilation, Downloads lint, and optimized release compilation passed. The layout detector reported no findings; native screenshots supplied the visual evidence.
- A temporary local screenshot harness rendered the actual Downloads screen and root shell on an API 34 TV emulator at 1920×1080. It used an isolated in-memory repository rather than saved downloads. Empty and populated light/dark states, an empty Packages filter, a long filename, the file context menu, and a 1.3 font scale were inspected. The header stayed consistent, the directory remained beside the title, and the long text stayed inside its bounds.
- The existing two `DownloadActionsTest` cases passed, covering the chooser's temporary read access and completion handling without duplicate alerts. Together with the temporary capture harness, the instrumentation run completed three cases successfully.
- The harness source was removed after capture. The in-memory fixtures were discarded; no real files were downloaded for the layout captures. Synthetic screenshots and the harness remain local, and the emulator was stopped.

The local optimized APK remains version 0.1.1 and uses the existing development certificate that matches the TV's installation. SHA-256: `e3bdac9997cdda26f7ad9b3d6ab057529d42cc6ef6ac9513b7d5cb76af5bbbf1`. This is not a new public release. The TV was unavailable at its saved connection, so this layout update has not been installed or physically verified there. No TV display settings or saved downloads were changed.
