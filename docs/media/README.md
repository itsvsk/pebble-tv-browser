# Product media

The README uses real captures of the current debug development build on an isolated Android TV API 34 emulator at 1920 × 1080, 320 dpi. It includes the approved logo, which will ship in the next release. Public example history, bookmarks and tabs are seeded in the emulator for the captures, not preinstalled Speed Dials or endorsements. The physical TV was not used.

## Artwork source

[Editable Figma artwork](https://www.figma.com/design/d7FAOeaVLFybYFWwpCj48a) contains the Light and Dark README banners and the 1280 × 640 social preview. These use the Pebble palette, Plus Jakarta Sans and the approved textured Tidal Lens mark. See the [brand assets and provenance](../BRANDING.md). SVG sources outline the text; the README uses PNG exports to render the textured mark reliably on GitHub. Font licensing is recorded in [Plus Jakarta Sans OFL](../../core/design/licenses/plus-jakarta-sans-OFL.txt).

- `hero-light.svg` / `hero-dark.svg`: outlined banner sources derived from Figma nodes `2:2` / `2:7`, with the approved full-color logo image. Corresponding PNG files are the README exports.
- `download.svg`: exported from node `5:2`; the image is linked to the latest release in the README.
- `social-preview.png`: exported from updated node `2:12`; ready for the repository's social-preview setting.
- `home-light.webp`, `home-dark.webp`, `browse-dark.webp`, `tabs-dark.webp`, `bookmarks-dark.webp`: direct emulator captures compressed to WebP. App controls and content have not been redrawn.

Captured and exported on 26 October 2026. The app itself remains the source of truth when updating screenshots. Keep example content public, avoid personal browsing data, and retain alt text when replacing media.

The browsing screenshot displays [Wikipedia](https://www.wikipedia.org/), a Wikimedia Foundation project. Wikipedia content is available under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) unless otherwise noted; Wikimedia names and marks belong to their respective owners. The screenshot demonstrates browser compatibility and does not imply endorsement.

To refresh media, install the current APK in an emulator, capture each screen with `adb -s EMULATOR_SERIAL exec-out screencap -p`, and compress the resulting PNGs. State whether it is a published release or development build. Store the assets here so the README needs no screenshot service, scheduled workflow, or external image host.
