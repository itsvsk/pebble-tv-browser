# TV Browser — Setup

## Prerequisites

- **JDK 17** (already detected on this machine)
- **Android Studio Ladybug | 2024.2.1 or newer** — provides Android SDK + Gradle wrapper jar
- **Android TV emulator image** — API 34 (Google APIs), x86_64

## First-time setup

1. Open `Android Studio` → **Open** → select `/Users/vishal/Documents/Projects/tv-app`.
2. Android Studio will:
   - Generate `gradle/wrapper/gradle-wrapper.jar` + `gradlew` script (uses Gradle 8.10.2 per `gradle-wrapper.properties`)
   - Prompt to install SDK Platform 35, Build Tools 35.x — accept
   - Run initial Gradle sync (downloads ~500MB of dependencies the first time)
3. Create AVD:
   - **Device Manager** → **Create Device** → **Television** → "Android TV (1080p)"
   - System image: **API 34, Google APIs, x86_64**
4. Run:
   - Select `app` configuration + the AVD → click **Run**

## CLI build (after wrapper jar exists)

```sh
./gradlew :app:assembleDebug
./gradlew installDebug
adb shell am start -n com.tvbrowser.app.debug/com.tvbrowser.app.MainActivity
```

## Module map

```
:app                  MainActivity, navigation, theme, manifest
:core:common          dispatchers, URL utils, DataStore prefs
:core:db              Room: bookmarks/history/tabs/downloads + MostVisited view
:core:webview         TabManager (LRU 8), WebView factory, UA strings
:core:adblock         EasyList parser + URL interceptor + weekly WorkManager refresh
:core:cursor          D-pad cursor controller + Compose overlay
:core:downloads       DownloadManager wrapper + progress flow
:feature:home         HomeScreen (omnibox + most-visited grid)
:feature:browser      BrowserScreen (address bar + WebView + cursor overlay)
:feature:tabs         TabsOverviewScreen
:feature:bookmarks    BookmarksScreen
:feature:history      HistoryScreen
:feature:downloads    DownloadsScreen
:feature:settings     SettingsScreen
```

## Design swap later

To apply Claude Design output:
- **Colors** → edit `app/src/main/kotlin/com/tvbrowser/app/theme/Color.kt`
- **Typography** → `theme/Type.kt`
- **Spacing/sizing** → `theme/Dimens.kt`
- **Shapes** → `theme/Shape.kt`

Screens reference these tokens via `MaterialTheme.colorScheme.*` and `Dimens.*`. No hardcoded hex/size values inside screen composables.

## Verification flows

See `/Users/vishal/.claude/plans/i-need-to-create-elegant-eagle.md` § Verification.

## Known v1 limitations

- Incognito = single process (cookies isolated only at session level, not strict). True isolation requires `:incognito` process — planned v2.
- No PiP / background play / voice search / sync in v1.
- Browser extensions not supported (System WebView limitation).
- No saved WebView state across cold restart — URLs reload from `TabEntity`.
