# TV Browser — Project Memory

Android TV web browser. Greenfield, single-developer (vishal.k@satsure.co).

## Stack

- Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10.2, JDK 17
- Jetpack Compose for TV (`androidx.tv:tv-material:1.0.0`) — NOT `compose.material3` for focusable elements (no D-pad visuals)
- minSdk 24, targetSdk 35
- System WebView engine
- Hilt 2.52, Room 2.6.1, Navigation Compose 2.8.5, Coil 2.7.0, WorkManager 2.10.0, OkHttp 4.12.0, DataStore 1.1.1
- Compose BOM 2024.12.01

## Module map

```
:app                  MainActivity, NavHost, theme, manifest, dispatchKeyEvent
:core:common          dispatchers (Hilt), URL utils, DataStore prefs
:core:db              Room — Bookmark, HistoryEntry, TabEntity, DownloadRecord, MostVisitedView
:core:webview         TabManager (LRU 8 WebViews), TabWebView factory, UserAgents
:core:adblock         EasyListLoader + AdBlockInterceptor + weekly WorkManager refresh
:core:cursor          CursorController, Compose overlay (24dp circle)
:core:downloads       DownloadManager wrapper + progress callbackFlow (1s poll)
:feature:home         HomeScreen (omnibox + 4×2 MostVisited grid, no WebView)
:feature:browser      BrowserScreen + AddressBar + WebViewHost
:feature:tabs         TabsOverviewScreen
:feature:bookmarks :feature:history :feature:downloads :feature:settings
```

## Locked design decisions

- **Remote nav**: hybrid. Default spatial focus. Long-press `KEYCODE_DPAD_CENTER` >500ms toggles virtual cursor overlay. Cursor arrows = move (8dp, accel to 32dp on repeat). CENTER = synthesize `MotionEvent` to WebView. Channel up/down = scroll.
- **Tabs**: LRU `LinkedHashMap<String, WebView>(8, 0.75f, true)`. Max 8 live. Evicted tabs reload URL from `TabEntity` (no WebView state save — privacy + simplicity).
- **Private browsing**: not exposed in the UI. Internal private-tab metadata stays in memory and suppresses history writes, but WebView cookies and site storage are shared with normal tabs. Do not advertise isolated private browsing or a private-only clear action until a separate WebView data directory and its lifecycle are implemented and verified.
- **Adblock**: DIY EasyList host-based. Skipped `org.adblockplus:adblock-android` (stale, heavy .so). Ship `assets/easylist_min.txt` snapshot for first-run. Weekly refresh via WorkManager.
- **Theme default**: Muted Teal / Charcoal. primary `#5FB0A8`, background `#0E1413`, surface `#171E1D`. 10-foot type scale: bodyMedium 18sp, headlineMedium 32sp, displayMedium 52sp.
- **UA toggle**: mobile = system default; desktop = Chrome 120 Linux UA. On switch: set `userAgentString`, inject viewport meta via `evaluateJavascript`, reload.
- **Home**: pure Compose, NOT WebView. Coil favicons from `google.com/s2/favicons`.

## v1 scope

IN: browsing, tabs, bookmarks, history, downloads, home, UA toggle, EasyList adblock.
OUT: isolated private browsing, PiP, background play, sync, extensions, WebView state restore.

## Build / run

```sh
./gradlew :app:assembleDebug
./gradlew installDebug
adb shell am start -n com.tvbrowser.app.debug/com.tvbrowser.app.MainActivity
```

AVD: Television → Android TV 1080p, API 34 Google APIs x86_64.

## Build pitfalls (already fixed — don't re-hit)

1. KSP reserves `default` / `main` as Dispatcher fn names → use `defaultDispatcher()`, `mainDispatcher()`.
2. `Preferences.Key` not visible to dependents → DataStore must be `api`, not `implementation`, in `:core:common/build.gradle.kts`.
3. `focusGroup` lives in `androidx.compose.foundation`, NOT `androidx.compose.ui.focus`.
4. TV `Surface(onClick=…)` wants `ClickableSurfaceShape`, not raw `RoundedCornerShape`. Wrap: `ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp))`.
5. Feature screen roots MUST be TV `Surface(containerColor=colorScheme.background)` — raw `Column` gives `LocalContentColor = Unspecified` → invisible headlines. Set explicit `color = colorScheme.onBackground` on headline `Text`.

## Runtime gotchas (already fixed)

- Home: NO `LaunchedEffect { omniboxFocus.requestFocus() }` — pops IME on TV launch. User reaches omnibox via D-pad DOWN.
- Omnibox `BasicTextField` traps D-pad UP/DOWN (cursor traversal). Wrap container in `Modifier.onPreviewKeyEvent` and call `focusManager.moveFocus()` on Up/Down. Also intercept `DirectionRight` → moveFocus(Right) so RIGHT reaches sibling Mic button.
- Mic must be its OWN focusable composable, NOT a child Icon inside the omnibox Box. Wrap in Box + `pebbleFocus(shape = CircleShape) + .clickable`. Inline Icon inside the search container is never reachable by D-pad.
- Voice search wired via `rememberLauncherForActivityResult(StartActivityForResult)` + `Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)` with `EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM`. On result read `EXTRA_RESULTS[0]` → set query + open new tab. Requires `androidx.activity:activity-compose` dep in `:feature:home`.
- TV `Tile` Boxes need explicit `.clickable(onClick = onClick)` — `pebbleFocus` alone makes them focusable but CENTER press fires nothing.
- `TvLazyRow` remembers last-focused child by default. To force first-item focus on every entry: attach `FocusRequester` to index 0 + apply `Modifier.focusProperties { enter = { firstFocus } }` on the row. Use `itemsIndexed` (not `items`) to tag the first. Needs `@OptIn(ExperimentalComposeUiApi)`.
- Home 10-foot type scale: full `PebbleType.Clock` (92sp) too big — use `.copy(fontSize = 64.sp, lineHeight = 68.sp)`, Greet 24sp, date 14sp for balance with 78dp omnibox.
- Most Visited must be `TvLazyRow` (single row, horizontal scroll), NOT `TvLazyVerticalGrid(Fixed(8))` — grid wraps multi-row + clips at bottom because parent Column has no scroll.

## Open issues (KI-1 … KI-5, see TEST_REPORT.md)

- **KI-1 (MEDIUM) Cursor long-press race**: `CursorController.handleCenterKey` returns false on `ACTION_DOWN repeatCount==0`, so DOWN propagates to focused view (Back IconButton in `AddressBar`) → fires `onBackHome()` before long-press detection on UP runs. Partial fix: `event.isLongPress` short-circuit. Real fix options: (a) consume DOWN, inject synthetic short-click on UP if <longPressMs; (b) initial Browser focus → WebView not Back btn; (c) different keycode for cursor toggle (e.g. `KEYCODE_MENU`).
- **KI-2 (LOW) WebView small scale on TV**: set `useWideViewPort=true` + `loadWithOverviewMode=true`, or `setInitialScale`.
- **KI-3 (LOW)** History/Downloads only tested empty state.
- **KI-4 (LOW)** BACK on Home immediately finishes activity — consider double-back-to-exit.
- **KI-5 (LOW)** No focus-restore returning from feature screen — snaps to first TopBar icon.

## Files modified in last test pass (2026-05-31)

```
core/cursor/.../CursorController.kt          isLongPress detection
feature/home/.../HomeScreen.kt               removed autofocus + Dpad escape from omnibox
feature/{settings,tabs,bookmarks,history,downloads}/.../*Screen.kt   Surface wrap + headline color
```

## Home-screen pass (2026-06-01)

```
feature/home/build.gradle.kts                added androidx.activity:activity-compose
feature/home/.../HomeScreen.kt               mic as focusable MicButton + voice intent (RecognizerIntent)
                                             clock 92→64sp, greet 30→24sp, date 16→14sp
                                             MostVisited TvLazyVerticalGrid(Fixed(8)) → TvLazyRow
                                             Tile: added .clickable + modifier param
                                             focusProperties{enter=firstTileFocus} on TvLazyRow → always first item
                                             omnibox onPreviewKeyEvent now handles DirectionRight → mic
```

Verified on emu (1920×1080, API 34): mic D-pad reachable, CENTER fires "Speak to search", first MostVisited tile focused on every entry.

## MostVisited context menu + WebView preview (2026-06-01 part 2)

```
core/cursor/.../CursorController.kt          gate long-press toggle on targetView != null
                                             (Home has no WebView attached → long-press CENTER falls through to Compose)
core/db/.../HistoryDao.kt                    + deleteByUrl(url)
core/db/.../Repositories.kt                  HistoryRepository.deleteByUrl
feature/home/.../HomeViewModel.kt            + bookmarkedUrls StateFlow, selectedForMenu state
                                             + toggleBookmark / deleteEntry / showMenu / dismissMenu
feature/home/.../HomeScreen.kt               Tile: combinedClickable(onClick,onLongClick) +
                                               explicit onPreviewKeyEvent measuring CENTER DOWN→UP duration
                                               (≥500ms → onLongClick). combinedClickable alone unreliable
                                               for synthetic keyboard long-press.
                                             + TileContextMenu Dialog (CenterEnd, 540dp, scrim 0.55)
                                               WebView preview 220dp + title/host + 3 MenuRows
                                               Delete row uses PebbleColors.Danger
                                             + WebViewPreview composable (AndroidView WebView,
                                               JS+DOM storage, wide viewport, initialScale 50,
                                               onRelease destroy chain)
```

Runtime gotchas added this round:
- `CursorController` must NOT consume long-press CENTER when no WebView is bound — Home screen has `targetView == null`. Gate every cursor-toggle branch on `cursorAvailable = targetView != null` so Compose Tile can receive long-press.
- Compose `combinedClickable.onLongClick` does NOT fire reliably from synthetic D-pad keyboard long-press (`adb shell input keyevent --longpress KEYCODE_DPAD_CENTER`). Add explicit `onPreviewKeyEvent` handler timing DOWN→UP with `SystemClock.uptimeMillis()`; threshold 500ms.
- Dialog with `WebViewPreview` 220dp: don't constrain Column with `fillMaxHeight(0.88f)` — overflow squishes lower rows. Let Column wrap content; dialog scrim is the full-screen Box.
- WebView in Compose must `onRelease { stopLoading; loadUrl("about:blank"); onPause; removeAllViews; destroy }` to avoid leak when Dialog dismisses.

Verified on emu: D-pad long-press CENTER on focused tile → menu opens with live Google neon doodle preview, Open (focused), Save to bookmarks, Delete (red). 0 FATAL/ANR.

## Single-CENTER activation fix (2026-06-01 part 3)

Bug: Every focusable element needed TWO CENTER presses to activate. Root cause: `pebbleFocus()` adds `Modifier.focusable()`; downstream `.clickable(...)` or `.combinedClickable(...)` adds a SECOND focusable. Focus traversal lands on the inner focusable first (first CENTER = noop / focus shift), activation only on second press.

Fix: new `Modifier.pebbleClickable(onClick, onLongClick?)` in `core/design/PebbleFocus.kt`. Uses `onPreviewKeyEvent` (CENTER/Enter/NumPadEnter; KeyDown latch + KeyUp dispatch with 500ms long-press threshold) + `pointerInput { detectTapGestures }` for touch. NO `focusable()` — pebbleFocus owns focus.

```
core/design/.../PebbleFocus.kt                + pebbleClickable modifier
core/design/.../NavRail.kt                    .clickable → .pebbleClickable
feature/home/.../HomeScreen.kt                Tile / MicButton / MenuRow → pebbleClickable
                                              (Tile drops inline DOWN/UP timing — pebbleClickable does it)
feature/browser/.../AddressBar.kt             clickableNoIndication → pebbleClickable; helper removed
feature/settings/.../SettingsScreen.kt        SetRow .clickable → .pebbleClickable
                                              PebbleSwitch .clickable dropped — parent SetRow toggles
```

Rule: NEVER stack `.clickable(...)` after `.pebbleFocus()`. Use `.pebbleClickable(...)` instead.

Verified on emu: single CENTER on focused Tile / Mic / NavRail item / SetRow → instant activation. Long-press CENTER on Tile still opens context menu (500ms threshold preserved). 0 FATAL/ANR.

Verification log (2026-06-01 17:47–17:48, API 34 emu):
- Tile (Google) single CENTER → Google search "android tv browser" loaded.
- Mic single CENTER → "Speak to search" dialog opened.
- NavRail History single CENTER → History screen with 4 entries.
- YouTube tile long-press CENTER (`adb shell input keyevent --longpress`) → context menu with live preview, Open (focused), Save to bookmarks, Delete (red).
- Logcat: only unrelated `audioserver` SIGABRT (emu audio service). App-side 0 FATAL/ANR.

## Stray KeyUp fix in pebbleClickable (2026-06-01 part 4)

Bug: Long-press CENTER on Tile opened context menu AND auto-clicked "Open" row. Sequence: Tile fires onLongClick at 500ms (still mid-press) → Dialog opens → `LaunchedEffect { openFocus.requestFocus() }` moves focus to Open MenuRow → user releases CENTER → KeyUp delivered to Open MenuRow's `pebbleClickable` → its handler saw no matching KeyDown (`downAt == 0L`), so `held = now - 0` was huge, treated as long-press, and with `onLongClick == null` fell through to `onClick()` → Open fired.

Fix in `PebbleFocus.kt` `pebbleClickable.onPreviewKeyEvent` KeyUp branch:
- If `downAt == 0L` (no prior matching KeyDown on this element), return false (don't consume, don't fire).
- After handling KeyUp, reset `downAt = 0L` so next stray KeyUp is also ignored.

Verified: long-press CENTER on tile → menu opens with Open focused but NOT activated. Subsequent dedicated CENTER on Open → URL loads.

Rule: any KeyUp-driven activation modifier must guard against KeyUp arriving without its KeyDown (focus moved into element mid-press).

## Tabs page activation + Close all confirm (2026-06-01 part 5)

```
feature/tabs/.../TabsOverviewScreen.kt   Pill / TabCard / Close icon / NewTabCard:
                                          added .pebbleClickable(onClick=...) (were focusable but inert).
                                          New tab pill onClick → NavController to Home route.
                                         Close all pill: opens ConfirmDialog (showCloseAll state).
                                         + ConfirmDialog composable: 480dp Surface1 card, scrim 0.55,
                                           title + message + Cancel + danger confirm. Confirm autofocused
                                           via FocusRequester + LaunchedEffect.
                                         + DialogButton: pebbleFocus(Full, tight) + pebbleClickable,
                                           danger variant = Danger.copy(alpha=0.18f) bg + Danger fg.
                                         Confirm action snapshots tabs list before iterating vm::close
                                           (avoid concurrent mutation of the StateFlow source).
```

Runtime gotchas:
- TabsOverviewScreen had `onClick` parameters threaded through Pill/TabCard/NewTabCard but no actual click wiring. `.pebbleFocus()` makes them focusable but CENTER fires nothing. Always pair `pebbleFocus` with `pebbleClickable` on activatable elements.
- Confirmation dialogs MUST autofocus the action (not Cancel) — TV remote should land on the meaningful default. But danger styling makes accidental activation visually loud; user can BACK out.
- Snapshot iterable state (`tabs.toList()`) before calling close in a loop — vm.close removes from the StateFlow, mutating the list mid-iteration otherwise.

Verified on emu (1920×1080, API 34): 91 tabs → Close all → dialog renders with title/message/Cancel/red Close all (focused with ring) → CENTER closes all 91, dismisses dialog, "0 open" + NewTabCard shown. NewTabCard tap → navigates to Home (Pebble home screen with clock + omnibox). 0 FATAL/ANR.

## Tabs page auto-hide scrollbar (2026-06-01 part 6)

```
feature/tabs/.../TabsOverviewScreen.kt   + ScrollIndicator composable (6dp wide, right edge).
                                          - track: 4dp Surface3 @ alpha 0.25
                                          - thumb: 6dp OnSurfaceMuted @ alpha 0.85
                                          - thumb height = visibleCount / total of layoutInfo
                                          - thumb Y = first / (total - visible) * (track - thumb)
                                          - visibility: alpha 0→1 (tween 220ms) when
                                            state.isScrollInProgress; LaunchedEffect delays
                                            800ms after stop then fades out.
                                          - skips render when alpha ≤ 0.01 and when content
                                            fits (no overflow).
                                         Grid wrapped in Box; gridState hoisted via
                                          rememberTvLazyGridState() and passed to both
                                          TvLazyVerticalGrid + ScrollIndicator.
```

Verified on emu (1920×1080, API 34): seeded 65 tabs via direct sqlite write to Room db. D-pad
DOWN through grid scrolls + scrollbar thumb appears on right edge tracking position (mid-track
at row 4 of ~17). After scroll stops, 800ms delay → fades out smoothly. Empty/short grid: bar
never appears.

## TabCard meaningful preview (2026-06-02 part 7)

```
feature/tabs/.../TabsOverviewScreen.kt   TabCard body redesigned:
                                          - top row: small favicon + host (muted LabelSm) + close X
                                          - body (168dp): 44dp favicon hero + page title up to 3 lines
                                            (PebbleType.Title, ellipsis on overflow)
                                         + TabFavicon helper: AsyncImage from s2/favicons sz=128, or
                                          home glyph in Surface3 tile for blank/about:blank tabs.
                                         Skeleton placeholder removed (was opaque info).
                                         Fallback title: host if tab.title blank, "New tab" if home.
```

Title source: `TabManager.onPageFinished` already persists `WebView.title` into `TabEntity.title`
on every navigation, so Google search tabs naturally show e.g. "android tv browser - Google Search"
(the search term is the prefix), YouTube tabs show video title, GitHub shows repo title, etc.
No new plumbing needed.

Verified on emu: 20 seeded tabs render with favicon + page title; Google search rows show search
term + " - Google Search", YouTube row shows video title, Wikipedia/HN/news.ycomb all readable
at 3-line cap.

Follow-up (not done): real WebView bitmap thumbnails captured on `onPageFinished` written to
`filesDir/tab-thumbs/${tabId}.png`, AsyncImage swap-in. ~80 LOC + cache invalidation; defer
until v1 stabilizes.

## TabCard context menu parity with Home (2026-06-02 part 8)

```
feature/tabs/.../TabsViewModel.kt             + BookmarkRepository inject
                                              + bookmarkedUrls: StateFlow<Set<String>>
                                                (BookmarkRepository.all().map { urls.toSet() })
                                              + selectedForMenu: StateFlow<TabEntity?>
                                              + showMenu / dismissMenu / toggleBookmark(tab)
feature/tabs/.../TabsOverviewScreen.kt        TabCard: added onLongClick param wired into
                                                pebbleClickable(onLongClick = ...).
                                              + TabContextMenu Dialog (CenterEnd, 540dp, scrim 0.55)
                                                — WebView preview 220dp (about:blank/home → Home glyph),
                                                  page title + host, MenuRows: Open / Save | Remove
                                                  bookmark / Close tab (red Danger).
                                              + MenuRow + TabWebViewPreview composables (mirror Home).
                                              Confirm action dismisses menu before close to avoid
                                              stray KeyUp into a now-orphaned tab.
```

Action mapping vs Home menu:
- Open → onOpenTab(tab.id)  (Home: open URL in new tab)
- Save to bookmarks → bookmarks.add / removeByUrl, label toggles
- Close tab → tabManager.release + tabRepo.delete  (replaces Home's history Delete)

Verified on emu (20 seeded tabs, API 34): long-press CENTER on focused first TabCard
("android tv browser - Google Search") → menu opens with live Google search preview + title
"android tv browser - Google Search" + host "www.google.com" + Open (focused with ring) +
Save to bookmarks + Close tab (red). 0 FATAL/ANR.

## Bookmarks page click + context menu (2026-06-02 part 9)

```
feature/bookmarks/.../BookmarksScreen.kt   BookmarksViewModel:
                                            + selectedForMenu: StateFlow<Bookmark?>
                                            + showMenu / dismissMenu
                                            constructor de-dup: single bookmarkRepo param
                                              (was injected twice).
                                           BookmarkRow:
                                            - dropped MoreVert trailing focusable (was a
                                              dead inner focusable inside parent pebbleFocus).
                                            + pebbleClickable(onClick = open, onLongClick = showMenu)
                                              — row was previously focusable but had no click,
                                              so CENTER did nothing.
                                           + BookmarkContextMenu Dialog (mirrors Home + Tabs):
                                             WebView preview 220dp + title + host +
                                             MenuRow Open (focused) + MenuRow Remove bookmark (Danger).
                                           + MenuRow + BookmarkWebViewPreview composables.
```

Cross-source verification (Bookmark functionality #1):
- Home tile menu → `HomeViewModel.toggleBookmark` → `BookmarkRepository.add`
- Tab card menu → `TabsViewModel.toggleBookmark` → same repo
- AddressBar star → `BrowserViewModel.toggleBookmark` → same repo
All three write to the single `BookmarkRepository`, and `BookmarksScreen` observes
`repo.all()` Flow — anything bookmarked from any surface shows up here.

Verified on emu (6 seeded bookmarks, API 34): Saved screen lists all 6 with favicon + title +
host. Long-press CENTER on focused row ("Build apps for Android TV") → menu opens with live
developer.android.com preview + title + host + Open (focused) + Remove bookmark (red). 0
FATAL/ANR.

## History page click + context menu (2026-06-01 part 10)

Mirrors Bookmarks/Tabs treatment. HistoryRow previously had `pebbleFocus` but no
`pebbleClickable` — CENTER was inert (same bug Bookmarks had). Also had a stray trailing
Close icon Box with its own `pebbleFocus` (inner focusable bug); dropped — Delete now lives
in the context menu.

```
feature/history/.../HistoryScreen.kt   HistoryViewModel: + BookmarkRepository inject,
                                         bookmarkedUrls StateFlow, selectedForMenu state,
                                         showMenu/dismissMenu/toggleBookmark/deleteEntry.
                                       HistoryRow signature (entry,onOpen,onLongClick) +
                                         pebbleClickable wires CENTER short→open, long→menu.
                                         trailing Close icon dropped.
                                       + HistoryContextMenu Dialog (CenterEnd, 540dp, scrim 0.55)
                                         WebView preview 220dp + title/host + 3 MenuRows:
                                         Open (focused) / Save to bookmarks ↔ Remove bookmark
                                         (toggles via isBookmarked) / Delete (red).
                                       + HistoryWebViewPreview composable (same lifecycle pattern
                                         as Bookmarks: JS+DOM, wide viewport, initialScale 50,
                                         onRelease destroy chain).
                                       ClearPill now has `.pebbleClickable(onClick=onClear)` —
                                         was inert before.
```

Verified on emu (18 seeded history entries, 6 bookmarks pre-existing): long-press CENTER on
focused row → menu opens with live YouTube preview + Open (focused). DOWN+CENTER on Save
toggles bookmark; re-open menu shows "Remove bookmark" — toggle confirmed. DOWN×2+CENTER on
Delete (red focused) fires deleteByUrl; rows count drops (18→10 after multiple delete tests
since `deleteByUrl` removes all matching URLs). 0 FATAL/ANR.

WAL gotcha (testing only, not a code bug): `adb shell run-as cat databases/tv_browser.db`
shows STALE counts because Room writes to WAL until checkpoint. Pull `.db`, `.db-wal`,
`.db-shm` together (all three to /tmp with matching basename) to see live state in sqlite3.

## Downloads page click + context menu (2026-06-02 part 11)

Mirrors Bookmarks/History pattern. All three card variants (Active/Done/Failed) had
`pebbleFocus` but no `pebbleClickable` — CENTER was inert. Trailing inner RoundButtons
(Cancel/Open/MoreVert/Retry) were never reachable inside parent pebbleFocus; dropped.
Actions moved into context menu, varied by record status.

```
feature/downloads/.../DownloadsScreen.kt   DownloadsViewModel: + DownloadRepository inject
                                             (was `repo` param-only), selectedForMenu state,
                                             showMenu/dismissMenu/deleteRecord
                                             (dmRepo.cancel + repo.delete).
                                           ActiveCard / DoneCard / FailedCard:
                                             + pebbleClickable. DoneCard short→open file,
                                             long→menu. Active/Failed short+long→menu
                                             (no meaningful default action).
                                             Trailing RoundButton/Pill icons removed.
                                           + DownloadContextMenu Dialog (CenterEnd, 540dp,
                                             scrim 0.55). File header row (48dp IconWell
                                             colored by status — Good/Danger/Muted) + filename
                                             Title (2 lines) + status sub. Then status-keyed
                                             MenuRows:
                                               STATUS_SUCCESS: Open (focused) / Remove from list (red)
                                               STATUS_RUNNING: Cancel download (red, focused)
                                               STATUS_FAILED:  Retry (focused, no-op v1) / Remove (red)
                                           + MenuRow composable (same pattern as History).
                                           Pill (Open folder, Change): + pebbleClickable (were inert).
                                           Drops Icons.Outlined.MoreVert / .PlayArrow / .Close
                                             (no longer needed); + .OpenInNew / .DeleteOutline.
                                           No WebView preview — downloads are files, not URLs;
                                             header IconWell is the visual.
```

Status-keyed menu rationale: a completed download wants Open as the verb; a running download
only wants to be cancelled; a failed one wants Retry or Remove. One screen, three menus,
zero conditional bloat in the cards themselves.

Verified on emu (5 seeded rows — 3 done, 1 running, 1 failed): long-press on installer.zip
Done row → menu shows Open (focused) + Remove from list (red). BACK dismisses, long-press
broken.bin Failed row → menu shows Retry (focused) + Remove from list (red). DOWN+CENTER on
Remove → db drops from 5→4 rows (broken.bin removed). 0 FATAL/ANR.

DB-seeding note: `adb push /tmp/seed.db /sdcard/seed.db` then pipe through `adb shell cat |
adb shell run-as cp - databases/tv_browser.db` fails (run-as can't read sdcard). Use
`adb shell cat /sdcard/seed.db | adb shell "run-as <pkg> sh -c 'cat > databases/tv_browser.db'"`
instead. Delete `.db-wal` + `.db-shm` after overwrite or Room ignores the new main file.

## Settings first-item focus on entry (2026-06-02 part 12)

Bug: Moving focus from nav rail to Settings list landed on 2nd ToggleRow
("Block ads & trackers") instead of 1st ("Request desktop site"). Cause: TvLazyColumn
spatial focus picks the focusable nearest in Y to the source (Settings rail item is
near bottom of screen at y≈970; first ToggleRow is at y≈400, second at y≈640 — closer).

Fix mirrors Home MostVisited TvLazyRow pattern (CLAUDE.md):
```
feature/settings/.../SettingsScreen.kt   + FocusRequester firstFocus
                                         TvLazyColumn .focusProperties { enter = { firstFocus } }
                                         First ToggleRow modifier = .focusRequester(firstFocus)
                                         ToggleRow + SetRow signatures gain `modifier: Modifier = Modifier`
```

Verified on emu: nav rail Settings → CENTER → RIGHT → focus ring lands on "Request desktop
site" (top row). Same pattern reusable for any TvLazyColumn entered from off-axis source.

## Test status (2026-05-31, debug APK 19.6MB, API 34 emu)

PASS: build, install, launch, logcat clean (0 FATAL/ANR), D-pad nav, omnibox submit, WebView loads google.com, theme.
PARTIAL: cursor mode (KI-1).
UNTESTED non-empty: History, Downloads, adblock score, UA toggle verify, tab persist cold restart, settings persist.

## References

- Full plan: `/Users/vishal/.claude/plans/i-need-to-create-elegant-eagle.md`
- Setup: `SETUP.md`
- Last test log + screenshots refs: `TEST_REPORT.md`
- Design handoff: `design_handoff_pebble_tv_browser/`

## Theme tokens (swap targets for design handoff)

- Colors → `app/src/main/kotlin/com/tvbrowser/app/theme/Color.kt`
- Typography → `theme/Type.kt`
- Spacing → `theme/Dimens.kt`
- Shapes → `theme/Shape.kt`

Screens MUST reference via `MaterialTheme.colorScheme.*` + `Dimens.*`. No hardcoded hex/dp in screen composables.

## Settings destructive-action confirm (2026-06-02 part 13)

Historical implementation note: the private-data action described below was removed
because it operated on storage shared with normal tabs. Only the history action remains.

```
feature/settings/.../SettingsScreen.kt   + ConfirmDialog + DialogButton private composables
                                           (copied verbatim from feature/tabs/TabsOverviewScreen.kt
                                           parts 5 — 480dp Surface1 card, scrim 0.55,
                                           Cancel + danger Clear focused via FocusRequester).
                                         + showClearHistory / showClearIncognito mutableStateOf.
                                         Clear browsing history ActionRow onClick → showClearHistory=true.
                                         Clear incognito data ActionRow onClick → showClearIncognito=true.
                                         Both dialogs onConfirm call vm.clearHistory/clearIncognito then
                                           reset flag; onCancel resets flag.
```

Rationale: both privacy actions are irreversible (history rows gone, cookies/site-storage
erased). Per Pebble UX rule "destructive actions need confirm with danger-styled confirm
button focused", same pattern as Close all tabs (part 5). Cancel sits left, danger confirm
right with focus ring on entry.

Verified on emu (API 34): Settings → DOWN-stack to Clear browsing history → CENTER →
dialog renders with "Clear browsing history?" title + 2-line "All visited pages will be
removed from this device. This cannot be undone." + Cancel + Clear (red, ring-focused).
DOWN to Clear incognito data → CENTER → analogous "Clear incognito data?" dialog. LEFT +
CENTER on Cancel dismisses dialog cleanly without firing action. 0 FATAL/ANR.

Cancel-safety verified with seeded data: 3 history rows inserted via run-as sqlite seed
(example.com, news.com, wiki.org). Opened Clear browsing history dialog → LEFT to Cancel
(ring) → CENTER. Dialog dismissed, focus restored to row. Post-pull db trio
(.db + .db-wal + .db-shm) shows all 3 rows intact, confirming `vm.clearHistory()` not
fired on Cancel path.

## Next test-pass plan

1. Real History + Downloads content (browse 5 sites, download pdf-test.pdf).
2. Cursor architectural fix + walkthrough.
3. Adblock verify on `adblock-tester.com` (target >70/100).
4. Settings persist across cold restart.
5. Tab limit + close-tab focus restore.
6. Desktop UA toggle — confirm `navigator.userAgent` changes.
7. BACK stack invariants — never land on destroyed tab.
