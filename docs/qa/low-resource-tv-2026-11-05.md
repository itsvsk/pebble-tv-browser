# Lower-resource TV memory improvements — 2026-11-05

## Scope

Two changes based on the memory review of `ccfdf61`:

1. Reduce allocations while initializing cached ad/tracker subscriptions.
2. Budget retained WebViews by device resources, with safe eviction and navigation restoration.

No new dependency, visual redesign, filter reduction, or delayed protection initialization.

## Implementation

Filter hosts that are already ASCII use allocation-light STD3 label validation. Unicode hosts
still use IDN conversion. The immutable matcher builds its host indexes in one pass, with
small initial buckets, rather than making several intermediate full-rule collections. Rule
deduplication, cross-subscription `badfilter`, exceptions, and party/domain conditions remain.

WebView budgets use the normal memory class, reported total RAM, and low-RAM flag:

| Device characteristics | Normal live-view budget |
| --- | ---: |
| Low-RAM flag, heap class ≤128 MiB, or reported RAM ≤2 GiB | 2 |
| Otherwise heap class ≤192 MiB or reported RAM ≤3 GiB | 4 |
| Otherwise | 8 |

These are conservative starting budgets, not universal hardware thresholds. A temporary
excess is allowed for attached/UI-owned/foreground views and explicit background-playback
tabs. Those views are never evicted just to meet a number. Unprotected views leave in LRU
order. Older Android running-memory callbacks reduce the budget; `UI_HIDDEN` releases all
unprotected views. The ordinary budget does not depend on receiving a memory callback.

Eviction saves up to 16 navigation records in memory. Each history Bundle must serialize
within 256 KiB (at most 4 MiB serialized history, excluding provider/heap overhead). A missing,
oversized, or unserializable history falls back to the last URL. Restoring navigation applies
the existing site/protection preferences before loading. Explicit navigation discards old
saved history. Closing a tab clears its saved state, including private state; none is written
to disk. Eviction reloads the document on return: live DOM, forms, JavaScript state, and media
playback position are not guaranteed to survive. Owned pages and background-playback tabs
are protected to avoid disrupting active use. Views are detached before destruction.

Memory callbacks are registered only while views exist and removed on empty/destroyed
managers. Serialization failures are a URL fallback rather than a main-thread crash.

## Measurements

Android TV API 34 AVD `Pebble_Readme_TV_API34`, arm64, 1536 MiB RAM, two cores,
headless SwiftShader. Android reports 192 MiB normal / 384 MiB large heap class and
`isLowRamDevice=false`; the RAM check correctly selects two views. Optimized R8 release
builds were installed under an isolated application ID. Existing app packages/data and
the physical TV were untouched. Both builds used identical cached public subscriptions.

### Cached-filter cold starts

Three fresh-process starts per build; `dumpsys meminfo` sampled three seconds after
`am start -W -S`. These are app-process PSS measurements, not live heap size or a leak.

| Build | App PSS samples (KiB) | Median (MiB) |
| --- | --- | ---: |
| Original | 66326, 294946, 294161 | 287.3 |
| Filter optimization | 164413, 164437, 164567 | 160.6 |

Median reduction: **44.1%**. The original had a lower outlier; GC timing affects these
samples. The earlier audit also observed transient startup pressure settling after about
15 seconds. Do not interpret this as a 44% steady-state RAM reduction or a physical-TV
frame-rate improvement. First-draw timings were too variable for a speedup claim.

A second matched three-start cohort confirmed the reduction: original 302043/303069/302939
KiB (median **295.8 MiB**), optimized 171773/171870/171860 KiB (median **167.8 MiB**),
**43.3% lower**. This repeat retained the eight fixture tab records in both builds, but
each fresh process displayed native Home with no live WebViews. Compare within each
cohort: tab metadata, GC and shared-page accounting can shift total PSS between cohorts.

### Tab retention

Eight persisted tabs were opened through the real Tabs/Browse UI. Each checkpoint has
three main-process samples plus the associated shared renderer's PSS. No heap collection
was forced. The workload was run with blank pages, then with offline content fixtures
(2000 paragraphs and an initialized 8 MiB JavaScript typed array per page). The latter
deliberately stresses retained document state; it is not a benchmark of a particular site.

Blank pages confirmed the ordinary view budget dropped from eight to two. Combined app
and renderer PSS did **not** show a meaningful improvement in that workload: the shared
engine and allocator retain a substantial floor. Count reductions alone are not RAM gains.

The content-heavy workload showed a clear combined-memory benefit:

| Checkpoint | Original app + renderer (MiB) | Optimized app + renderer (MiB) |
| --- | ---: | ---: |
| After eight pages | 170.1 + 189.2 = 359.3 | 143.8 + 107.4 = 251.2 |
| Return to native Home, settle 15 seconds | 168.4 + 188.5 = 356.9 | 143.5 + 105.2 = 248.7 |

After eight pages, combined PSS was **30.1% lower**. This compares the complete original
and optimized builds, including the filter optimization, not an isolated cache-only binary.
The blank-page result and separate filter-only startup measurements help distinguish the
shared engine floor and initialization pressure from document retention. These are three
memory samples at each checkpoint of one navigation sequence per build, not three independent
end-to-end workload runs. Pages were the same offline fixture in each build; actual sites,
WebView versions, allocator/GC timing, and devices will differ.

## Verification

- All 97 JVM unit tests passed, with no skips/failures; debug lint and normal debug/R8 release builds passed.
- Full cached subscriptions showed identical before/after counts in the real Settings UI:
  EasyList 46111 supported / 32205 unsupported; EasyPrivacy 48036 / 9251. Snapshot checksums
  and existing protection initialization were retained.
- 26 existing API 34 emulator tests passed across real WebView ad blocking, page recovery,
  QoL persistence, top-navigation focus, and explicit mute/foreground transitions.
- Three new real-provider tests passed for eviction/back-forward restoration and explicit
  navigation replacement, owned-host/background-playback protection during trim, and
  clearing evicted private navigation on close.
- Boundary mutations in the cache budget and host-label validation were caught by their
  tests; the mutations were restored before final checks.
- Independent code review found the history-serialization crash path; it was fixed and
  the final review found no remaining required changes.
- `TabCacheTest` is included in the existing API 24/34 CI test selection. API 24 and
  physical low-end TV execution were not performed locally.

Raw memory samples, fixture helper/build scripts, logs, and APKs for this local run:
`/tmp/pebble-low-resource-implementation/`. These temporary artifacts are not release APKs.

Public filter fixture byte sizes: EasyList 2062617; EasyPrivacy 1513227. Both builds share
the same checksum-verified snapshots; subscriptions were not refreshed between comparisons.

Platform references: [memory APIs](https://developer.android.com/reference/android/app/ActivityManager),
[trim callback behavior](https://developer.android.com/reference/android/content/ComponentCallbacks2),
and [WebView navigation state](https://developer.android.com/reference/android/webkit/WebView#saveState(android.os.Bundle)).
