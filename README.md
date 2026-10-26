# Pebble TV Browser

[![Android CI](https://github.com/itsvsk/pebble-tv-browser/actions/workflows/android-ci.yml/badge.svg)](https://github.com/itsvsk/pebble-tv-browser/actions/workflows/android-ci.yml)
[![GitHub release](https://img.shields.io/github/v/release/itsvsk/pebble-tv-browser)](https://github.com/itsvsk/pebble-tv-browser/releases/latest)

A browser built for Android TV and a remote control. Pebble combines a Sea Glass interface, textured sand backgrounds, light and dark themes, and pointer navigation for browsing websites from the sofa.

## Download and install

Download the **APK** from [GitHub Releases](https://github.com/itsvsk/pebble-tv-browser/releases/latest). The source-code ZIP is for development, not installation.

Requires **Android TV 7.0 / API 24 or newer** and a current Android System WebView. Allow installation from your chosen file manager, then open the APK on the TV. Alternatively:

```sh
adb connect TV_IP:DEBUGGING_PORT
adb install -r pebble-v0.1.0.apk
adb shell am start -n com.tvbrowser.app/com.tvbrowser.app.MainActivity
```

Releases include `SHA256SUMS` and the public signing certificate. Verify the APK with `sha256sum -c SHA256SUMS` on Linux or `shasum -a 256 -c SHA256SUMS` on macOS.

Official releases share one signing key for updates. An experimental APK signed with another key cannot be updated in place; uninstalling it removes its app data. Debug builds use `com.tvbrowser.app.debug` and can coexist with the release app.

## Features

- **Sea Glass design:** shared glass controls, soft focus glows, textured backgrounds, and persistent System / Light / Dark appearance settings.
- **Home:** search, URL entry, voice search when a speech service is available, and frequently visited Speed Dial sites.
- **Browse:** pointer mode by default, edge scrolling, auto-hiding controls, back/forward/reload, and desktop/mobile page modes.
- **Tabs:** persisted tabs with predictable remote navigation and lightweight site context menus.
- **Library:** bookmarks and grouped browsing history with a compact header while scrolling.
- **Downloads:** DownloadManager integration, progress and failure states, retry feedback, and the actual app-owned storage path.
- **Ad blocking:** EasyList-based request filtering with background updates.
- **Performance and accessibility:** cached backgrounds, inactive WebView pausing, isolated pointer/progress updates, and accessible actions on custom controls.

## Remote controls

| Control | Action |
| --- | --- |
| D-pad | Move focus or the browser pointer |
| Select / OK | Activate an item; select the browser address field to edit it |
| Hold Select | Open supported site, tab, bookmark, and history context menus |
| Back | Return focus to the active top navigation item in native screens |
| Pointer at a page edge + continued direction input | Scroll the webpage |
| Up at the top of a webpage | Reveal browser controls and move focus upward |

Top navigation wraps at both ends. Speed Dial moves between rows in reading order and returns to search at the first/last item boundary. The pointer/D-pad control in Browse changes navigation mode.

## Build locally

Install **JDK 17**, Android SDK Platform **35**, and Build Tools **34.0.0**. Open the project in Android Studio, or set `ANDROID_HOME` / create an untracked `local.properties` containing `sdk.dir`.

```sh
git clone https://github.com/itsvsk/pebble-tv-browser.git
cd pebble-tv-browser
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.tvbrowser.app.debug/com.tvbrowser.app.MainActivity
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Run CI's static checks and builds locally:

```sh
./gradlew lintDebug testDebugUnitTest \
  :app:assembleDebug :app:assembleRelease \
  :app:assembleDebugAndroidTest :core:db:assembleDebugAndroidTest
```

Run deterministic integration checks on an Android TV emulator:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tvbrowser.app.TopNavFocusTest,com.tvbrowser.app.CursorControllerScrollTest
./gradlew :core:db:connectedDebugAndroidTest
```

`DownloadFailureTest` is a separate online integration check against a W3C missing-file URL; it is not part of the deterministic emulator CI suite.

## GitHub Actions and releases

| Automation | Trigger | Result |
| --- | --- | --- |
| [Android CI](.github/workflows/android-ci.yml) | PR into `main`, push to `main`, or manual run | Lint, unit tests, debug/release compilation, Android TV API 24 and 34 tests, reports, and debug APK artifact |
| [Dependency review](.github/workflows/android-ci.yml) | PR into `main` | Checks dependency changes for high or critical known vulnerabilities |
| [Release APK](.github/workflows/release.yml) | Push `vMAJOR.MINOR.PATCH`, or retry an existing tag manually from `main` | Re-runs CI, signs and verifies the APK, then publishes a GitHub Release |
| [Dependabot](.github/dependabot.yml) | Weekly | Proposes Gradle and GitHub Actions dependency updates |

Signing secrets live in the **`github-release`** environment, restricted to `main` and `v*` tags. PR jobs do not receive signing secrets. Actions are pinned to commit hashes; only the publishing job receives repository write permission.

After merging changes into `main`, publish the next version:

```sh
git switch main
git pull --ff-only
git tag -a v0.1.1 -m "Pebble TV Browser 0.1.1"
git push origin v0.1.1
```

The tag sets the APK version. `versionCode = major × 1,000,000 + minor × 1,000 + patch`; minor/patch are 0–999 and major is 0–2000. `0.0.0` and leading zeroes are rejected. Use increasing versions. The workflow requires a tagged commit from `main` and refuses to replace a published release.

See [RELEASING.md](docs/RELEASING.md) for signing setup, retries, local release builds, and recovery. [CHANGELOG.md](CHANGELOG.md) describes user-facing changes.

## Project structure and stack

| Module | Purpose |
| --- | --- |
| `app` | Activity, navigation, application setup, integration tests |
| `core/common` | Preferences, dispatchers, URL helpers |
| `core/design` | Sea Glass themes, typography, focus, shared controls and shell |
| `core/db` | Room schema, bookmarks, history, tabs, downloads |
| `core/webview` | WebView lifecycle, tab manager, page modes and navigation guards |
| `core/adblock` | EasyList parsing, request filtering and refresh work |
| `core/cursor` | Pointer movement, scrolling and overlay |
| `core/downloads` | DownloadManager integration and status updates |
| `feature/*` | Home, Browser, Tabs, Bookmarks, History, Downloads and Settings |

Kotlin 2.0.21 · AGP 8.7.3 · Gradle 8.10.2 · Jetpack Compose · AndroidX TV · Hilt · Room · WorkManager · DataStore · Navigation Compose · Coil · OkHttp. Compile/target SDK: 35; minimum SDK: 24.

## Current limitations

- Private browsing is unavailable until cookies and site storage can be isolated from normal tabs.
- Website compatibility depends on Android System WebView; native themes do not recolor websites.
- The Room schema is version 1. Future changes require explicit migrations; destructive migration is disabled.
- Performance still needs repeatable benchmarks across more TVs. Emulator functional checks do not establish physical-TV frame performance.

[tasks/todo.md](tasks/todo.md) tracks remaining work. [SETUP.md](SETUP.md), [TEST_REPORT.md](TEST_REPORT.md), and [DESIGN.md](DESIGN.md) contain earlier notes and may describe superseded behavior.

## License

No application license has been selected yet. Bundled Plus Jakarta Sans uses the [SIL Open Font License](core/design/licenses/plus-jakarta-sans-OFL.txt).
