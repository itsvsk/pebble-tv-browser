# Changelog

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
