# Changelog

## Unreleased

### Added

- Clear the entire address from Browse and immediately type a replacement; the current page stays open until submission.
- Download start, completion, and failure alerts, including completion handling outside the Downloads screen.
- An "Open with" chooser for downloaded files, with file-type detection and temporary read access.

### Changed

- Adopted the Tidal Lens logo with a textured Tumbled Jade finish across the app, Android TV launcher, and product artwork.

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
