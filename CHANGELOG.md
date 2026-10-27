# Changelog

## Unreleased

### Added

- Web video fullscreen with remote controls, Back to exit, and restoration of the prior pointer state.
- Clear the entire address from Browse and immediately type a replacement; the current page stays open until submission.
- Download start, completion, and failure alerts, including completion handling outside the Downloads screen.
- An "Open with" chooser for downloaded files, with file-type detection and temporary read access.

### Changed

- Desktop site identification uses the installed WebView engine version and stays synchronized when reopening cached tabs.
- Adopted the Tidal Lens logo with a textured Tumbled Jade finish across the app, Android TV launcher, and product artwork.

### Fixed

- Discard stale pointer-scroll probes after the browser viewport resizes, so earlier hit-test coordinates cannot affect the resized page.
- Pointer edge scrolling now reaches nested social feeds and chat panels; Up yields to browser controls only after the inner feed reaches its top.
- App-only redirects such as Facebook's `fb://` links no longer replace a usable website with an unsupported-scheme error.
- Streaming pages such as Twitch collapsing against the top of the screen: WebViews now have bounded dimensions so percentage-height and viewport-height players fill their available space.

## 0.1.0

### Added

- Initial GitHub APK release pipeline with persistent signing, signature verification, and checksums.
- CI for lint, unit tests, builds, and Android TV navigation, scrolling, and database persistence checks.
- Sea Glass light/dark themes, textured backgrounds, and adaptive native layouts.
- Pointer browsing, continuous edge scrolling, auto-hiding controls, and predictable remote navigation.
- Frequently visited Speed Dial sites, persisted tabs, bookmarks, grouped history, and tracked downloads.

### Fixed

- Focus wrapping, top navigation highlight rebound, and clipped tile glows.
- Google page controls capturing vertical navigation.
- WebView lifecycle, tab persistence, download status reporting, and truncated storage paths.
- Android 7 compatibility for WebView initialization and date/time APIs.

### Changed

- Context menus show lightweight site cards instead of loading additional webpages.
- Persisted data no longer uses destructive migration fallback.
- Private browsing remains unavailable pending storage isolation.
