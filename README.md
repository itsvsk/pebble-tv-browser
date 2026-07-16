# Pebble TV Browser

Pebble TV Browser is an Android TV web browser built with Kotlin, Jetpack Compose, AndroidX TV components, WebView, Room, Hilt, WorkManager, and Gradle. The app is designed around remote-first navigation, a TV-scale visual system, tab management, bookmarks, history, downloads, ad blocking, and an optional D-pad cursor overlay for WebView interaction.

## Features

- Android TV home screen with omnibox and most-visited shortcuts
- WebView-based browser screen with address bar, back/forward/reload controls, and desktop/mobile user-agent support
- Multi-tab model backed by Room with an LRU WebView manager
- Bookmarks, history, downloads, and settings screens
- EasyList-based ad-block parsing and request interception
- DownloadManager integration with progress tracking
- D-pad cursor controller and Compose cursor overlay for TV remote interaction
- Pebble design system with shared colors, type, spacing, shape, focus, and shell components

## Tech Stack

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Java 17
- Jetpack Compose with Compose BOM 2024.12.01
- AndroidX TV Material and TV Foundation
- Hilt, Room, WorkManager, DataStore, Navigation Compose, OkHttp
- Compile SDK 35, min SDK 24, target SDK 35

## Project Structure

```text
:app                  MainActivity, app navigation, manifest, app theme
:core:common          dispatchers, URL utilities, DataStore preferences
:core:design          Pebble TV design system and shared shell components
:core:db              Room database, DAOs, entities, repositories
:core:webview         WebView factory, tab manager, user-agent presets
:core:adblock         EasyList parser, ad-block interceptor, refresh worker
:core:cursor          D-pad cursor controller and overlay
:core:downloads       DownloadManager wrapper and progress flow
:feature:home         Home screen and omnibox
:feature:browser      Browser screen, address bar, WebView host
:feature:tabs         Tabs overview
:feature:bookmarks    Bookmarks screen
:feature:history      History screen
:feature:downloads    Downloads screen
:feature:settings     Settings screen
```

## Getting Started

Prerequisites:

- JDK 17
- Android Studio Ladybug 2024.2.1 or newer
- Android SDK platform 35
- Android TV emulator or physical Android TV device

Open the project in Android Studio and let Gradle sync. For an emulator, create an Android TV 1080p AVD with an API 34 Google APIs x86_64 image.

## Build And Run

```sh
./gradlew :app:assembleDebug
./gradlew installDebug
adb shell am start -n com.tvbrowser.app.debug/com.tvbrowser.app.MainActivity
```

Run unit tests:

```sh
./gradlew test
```

Run the EasyList parser tests directly:

```sh
./gradlew :core:adblock:testDebugUnitTest
```

## Documentation

- [SETUP.md](SETUP.md) has local setup, CLI commands, module notes, and v1 limitations.
- [TEST_REPORT.md](TEST_REPORT.md) captures the latest manual Android TV emulator test pass, fixed issues, and remaining known issues.
- [design_handoff_pebble_tv_browser/README.md](design_handoff_pebble_tv_browser/README.md) documents the Pebble visual design handoff and reference implementation.

## Current Status

The debug build has been validated on an Android TV emulator. Core navigation, feature screen rendering, omnibox submission, and WebView loading are working. Known follow-up work includes a deeper cursor long-press fix, WebView TV-scale tuning, fuller History/Downloads testing, settings persistence checks, and tab/focus restoration polish.

## License

No license file is currently included.
