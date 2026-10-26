# Streaming in Pebble

Pebble uses the TV's Android System WebView and the streaming site's original player. Keep System WebView updated. Pebble does not transcode streams or replace a site's player with an external playback engine.

| Playback path | Handling |
| --- | --- |
| HTML video/audio, including MP4 | Rendered by WebView; codec support depends on the engine and TV decoder. |
| Live streams using MediaSource, HLS, or DASH | The site's player and WebView negotiate playback. A direct playlist URL does not open a separate native streaming player. |
| Fullscreen video | The webpage's fullscreen button opens a full-window player above Pebble's bars. Native player controls receive D-pad input; Back returns to the page and restores pointer state. |
| DRM or subscription services | Not guaranteed. Playback depends on device certification, the service's WebView policy, codecs, and protected-media permissions. Pebble does not automatically grant protected-media identification, camera, or microphone access. |
| Background media | WebView lifecycle processing is paused when the browser leaves the foreground, but explicit audio/video suspension is not currently managed. This does not guarantee that every site's audio stops. |
| Downloaded media | Opening a completed download offers Android's app chooser with temporary access to the selected file. |

## Twitch layout fix

The native WebView previously received `WRAP_CONTENT` dimensions from Compose. Twitch could report a normal `window.innerHeight` while its percentage-height page root and `100vh` video container computed to zero height. The stream could play behind a blank or compressed page.

WebViews now request `MATCH_PARENT` dimensions before attaching to Compose. An offline regression reproduces the percentage-height and viewport-height layout without relying on Twitch's changing markup. Desktop mode retains the installed Chromium version rather than advertising Chrome 120; reopening a cached tab synchronizes its actual user agent with the displayed mode.

If a service fails, compare Mobile site and Desktop site modes, then try the same page with **Block ads & trackers** disabled. These are diagnostic choices; Pebble does not silently disable blocking or bypass service restrictions. Report the URL, page mode, TV model, WebView version, and any player error code.

References: [Chromium WebView page layout](https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/web-page-layout.md), [Android fullscreen callbacks](https://developer.android.com/reference/android/webkit/WebChromeClient#onShowCustomView(android.view.View,%20android.webkit.WebChromeClient.CustomViewCallback)), [WebView pause behavior](https://developer.android.com/reference/android/webkit/WebView#onPause()).
