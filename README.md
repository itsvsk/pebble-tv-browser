<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/media/hero-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/media/hero-light.svg">
  <img src="docs/media/hero-light.svg" alt="Pebble — The web, from your sofa." width="1440">
</picture>

<p align="center">
  <strong>A browser built for Android TV and the remote in your hand.</strong><br>
  Search, explore, and return to your favorite sites in a calm Sea Glass interface.
</p>

<p align="center">
  <a href="https://github.com/itsvsk/pebble-tv-browser/releases/latest"><img src="docs/media/download.svg" alt="Download Pebble for Android TV (APK)" width="248"></a>
</p>

<p align="center">
  <a href="#try-it-on-your-tv">Installation</a>
  &nbsp; · &nbsp; <a href="#built-for-your-remote">Remote controls</a>
</p>

<p align="center">
  <a href="https://github.com/itsvsk/pebble-tv-browser/releases/latest"><img src="https://img.shields.io/github/v/release/itsvsk/pebble-tv-browser?color=177963&label=Latest%20release" alt="Latest GitHub release"></a>
  <a href="https://github.com/itsvsk/pebble-tv-browser/actions/workflows/android-ci.yml"><img src="https://github.com/itsvsk/pebble-tv-browser/actions/workflows/android-ci.yml/badge.svg" alt="Android CI status"></a>
</p>

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/media/home-dark.webp">
  <source media="(prefers-color-scheme: light)" srcset="docs/media/home-light.webp">
  <img src="docs/media/home-light.webp" alt="Pebble Home with a search field and Speed Dials built from frequently visited websites." width="1920">
</picture>

<p align="center"><sub>Real captures of Pebble v0.1.0 on an Android TV emulator, with an example browsing session.</sub></p>

## Built for your remote

Move the pointer with the D-pad. Press OK to select a link. Keep pressing toward a page edge to scroll. Browser controls step out of the way while you explore; move up at the top of the page to bring them back.

<img src="docs/media/browse-dark.webp" alt="A webpage open in Pebble, with the pointer navigation indicator and browser controls visible." width="1920">

<details>
<summary><strong>A quick guide to the controls</strong></summary>

| On your remote | In Pebble |
| --- | --- |
| **D-pad** | Move through the app or move the webpage pointer |
| **OK / Select** | Open an item or activate a link |
| **Hold Select** | Open supported site, tab, bookmark, and history context menus |
| **Back** | Return focus to the active top navigation item on native screens |
| **Keep pressing at a page edge** | Scroll the webpage in pointer mode |
| **Up at the top of a page** | Reveal the browser controls and move focus upward |

Top navigation wraps at both ends. Browse starts in pointer mode; its navigation control lets you switch to D-pad mode. Select the browser address field when you want to edit a URL or search query.

</details>

## Pick up where you left off

Your most-visited pages become Speed Dials. Keep multiple tabs, save a bookmark, or find a page again in browsing history.

| Keep your tabs together | Save a place to return to |
| --- | --- |
| <img src="docs/media/tabs-dark.webp" alt="Pebble's Tabs screen showing the open pages and the New tab action." width="960"> | <img src="docs/media/bookmarks-dark.webp" alt="Saved websites in Pebble's bookmarks library." width="960"> |

| Make it yours | What you can do |
| --- | --- |
| **Search your way** | Enter a URL, search the web, or use voice search when a speech service is available |
| **Choose the page view** | Switch between mobile and desktop website modes |
| **Keep distractions down** | Use the built-in EasyList-based ad blocker |
| **Manage downloads** | Follow progress and see failure states in the Downloads screen |

## Two ways to feel at home

Warm sand and sea-glass greens in Light. Deep sea-green surfaces and soft mint highlights in Dark. Choose either in Settings, or let Pebble follow your TV's appearance.

| Light | Dark |
| --- | --- |
| <img src="docs/media/home-light.webp" alt="Pebble's light theme with warm sand backgrounds and green glass controls." width="960"> | <img src="docs/media/home-dark.webp" alt="Pebble's dark theme with deep sea-green backgrounds and mint focus highlights." width="960"> |

## Try it on your TV

**Requires Android TV 7.0 or newer** and a current Android System WebView.

1. **[Download the APK](https://github.com/itsvsk/pebble-tv-browser/releases/latest)** from the latest release. Choose the `.apk` file.
2. **Transfer it to your TV** and open it with a file manager. Allow installation from that file manager if Android asks.
3. **Launch Pebble** from your TV's app launcher and start browsing.

Prefer ADB, or want to verify the download? See the [installation guide](docs/INSTALLING.md) for commands, checksums, and update compatibility.

<details>
<summary><strong>A few things to know before you start</strong></summary>

- This is an early public release. Website compatibility depends on your TV's Android System WebView.
- The native light and dark themes change Pebble's interface; websites keep their own appearance.
- Private browsing is currently unavailable; it requires separate cookie and site-storage isolation.
- Voice search needs an installed speech-recognition service.
- Official release APKs share a signing key so future releases can update the app. Builds signed with a different key cannot update it in place.

</details>

## Help shape Pebble

Found a website that behaves unexpectedly, or have an idea for better remote browsing? [Open an issue](https://github.com/itsvsk/pebble-tv-browser/issues). Include your TV model, Android version, and steps to reproduce when reporting a bug.

For development, see the [build and architecture guide](docs/DEVELOPMENT.md), [release guide](docs/RELEASING.md), and [changelog](CHANGELOG.md).

---

Sea Glass-inspired design · Kotlin and Jetpack Compose for Android TV

No application license has been selected yet. Plus Jakarta Sans is bundled under the [SIL Open Font License](core/design/licenses/plus-jakarta-sans-OFL.txt). [Artwork and screenshot sources](docs/media/README.md).
