# Developing Pebble

[Product overview](../README.md) · [Installation](INSTALLING.md) · [Release signing and publication](RELEASING.md)

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
  -Pandroid.testInstrumentationRunnerArguments.class=com.tvbrowser.app.TopNavFocusTest,com.tvbrowser.app.CursorControllerScrollTest,com.tvbrowser.app.DownloadActionsTest
./gradlew :core:db:connectedDebugAndroidTest
```

`DownloadFailureTest` separately checks real completion broadcasts and missing-file failures against W3C PDF URLs; it is not part of the deterministic emulator CI suite.

## GitHub Actions and releases

| Automation | Trigger | Result |
| --- | --- | --- |
| [Android CI](../.github/workflows/android-ci.yml) | PR into `main`, push to `main`, or manual run | Lint, unit tests, debug/release compilation, Android TV API 24 and 34 tests, reports, and debug APK artifact |
| [Dependency review](../.github/workflows/android-ci.yml) | PR into `main` | Checks dependency changes for high or critical known vulnerabilities |
| [Release APK](../.github/workflows/release.yml) | Push `vMAJOR.MINOR.PATCH`, or retry an existing tag manually from `main` | Re-runs CI, signs and verifies the APK, then publishes a GitHub Release |
| [Dependabot](../.github/dependabot.yml) | Weekly | Proposes Gradle and GitHub Actions dependency updates |

Signing secrets live in the **`github-release`** environment, restricted to `main` and `v*` tags. PR jobs do not receive signing secrets. Actions are pinned to commit hashes; only the publishing job receives repository write permission.

After merging changes into `main`, publish the next version:

```sh
git switch main
git pull --ff-only
git tag -a v0.1.1 -m "Pebble TV Browser 0.1.1"
git push origin v0.1.1
```

The tag sets the APK version. `versionCode = major × 1,000,000 + minor × 1,000 + patch`; minor/patch are 0–999 and major is 0–2000. `0.0.0` and leading zeroes are rejected. Use increasing versions. The workflow requires a tagged commit from `main` and refuses to replace a published release.

See [RELEASING.md](RELEASING.md) for signing setup, retries, local release builds, and recovery. [CHANGELOG.md](../CHANGELOG.md) describes user-facing changes.

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


## Project notes

[tasks/todo.md](../tasks/todo.md) tracks remaining work. [SETUP.md](../SETUP.md), [TEST_REPORT.md](../TEST_REPORT.md), and [DESIGN.md](../DESIGN.md) are historical notes and may describe superseded behavior. The Room schema is version 1; future changes require explicit migrations, and destructive migration is disabled. Emulator functional checks do not establish physical-TV frame performance.
