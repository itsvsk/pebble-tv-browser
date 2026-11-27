# Ad and tracker protection

Pebble filters supported network requests using a deliberately limited set of host-based EasyList and EasyPrivacy rules. It does not provide the full filtering capabilities of a desktop extension.

## Controls

In pointer mode, press **Menu**, or move Up again at the top of the page, to return to the browser controls. In Browse, select the shield beside the address bar to see the current website's effective protection state and the number of requests blocked on the current document.

- **Pause for this visit** temporarily stops blocking in this tab on this exact host, including reloads. Leaving the host, closing the tab or ending the app session clears the pause. A discarded WebView does not turn the temporary choice into a permanent exception.
- **Resume protection** ends the temporary pause and reloads the page.
- **Always turn off/on for this website** saves an exact-host override. It does not affect sibling hosts, subdomains or other websites.
- **Use global protection default** removes only the protection override. Desktop/Mobile mode is kept.
- **Update filter lists now** fetches the configured subscriptions. A failed update keeps previous working protection.

In **Settings → Ad & tracker protection**, change the global default, inspect each list's supported-rule count, last successful update and failure/fallback state, refresh subscriptions, and remove saved overrides individually or together. The default applies to websites without a saved override. Reload open pages after changing it. Counts describe accepted network rules, not complete EasyList coverage.

The blocked-request counter resets for each new document/reload. It counts blocked requests, not unique advertisers, unique trackers, bandwidth saved or attempts outside Pebble's filtering path. Counters are kept in memory; the app does not record a URL-by-URL blocking log or upload blocking statistics.

## Updates and fallback

A small, hand-maintained in-memory seed supplies protection immediately, including offline startup. Richer saved subscriptions load in the background. Settings identifies bundled fallback separately from successfully downloaded lists; the seed is not a complete bundled EasyList/EasyPrivacy release.

Subscriptions are the existing official sources:

- [EasyList](https://easylist.to/easylist/easylist.txt)
- [EasyPrivacy](https://easylist.to/easylist/easyprivacy.txt)

Periodic background work attempts an update weekly when connected; users can request an update immediately. Each subscription keeps its own validated original text and update timestamp. If one fails, its last working snapshot remains active while another successfully updated subscription can advance. Failed work retries with WorkManager backoff, bounded to four attempts per scheduled run.

Responses have connection/read/overall timeouts and an 8 MiB decompressed-body limit per subscription. The expected ABP header and a nonempty supported block-rule set must be present. Cancellation cancels the HTTP request. Source text and metadata are committed by one atomic same-directory rename after flushing the temporary file; an unfinished temporary file cannot replace the prior snapshot. A SHA-256 checksum detects local cache corruption; it is not an upstream signature. Download authenticity relies on HTTPS.

Old `adblock/hosts.txt` caches are ignored because the previous implementation already removed rule constraints. They are left untouched; the app uses the new original-source snapshot directory or bundled fallback.

## Supported rule subset

- Anchored host rules such as `||ads.example^`, including matching subdomains.
- `$third-party` and `$~third-party`, using public-suffix-aware site comparisons, including private suffixes.
- `$domain=` includes/excludes, applied to the top-level page host.
- Scoped network exceptions and document exceptions.
- Canonical `$badfilter` disabling, including across the two subscriptions.
- Host normalization for case, trailing dots and IDNs.

Path-, resource-type-, regex-, rewrite-, redirect- and cosmetic blocking rules are not converted into unconditional host blocks. Unsupported blocking rules are skipped. Recognized path/type/case exception restrictions are conservatively widened within the supported host/domain/party scope so an explicit allowance does not become an accidental block. Document exceptions cannot recover the original navigation initiator; their party restriction is widened conservatively. These choices may allow extra requests. Unknown and cosmetic-only exceptions are not treated as general network allowances. Settings reports unsupported rules, including conservatively handled exceptions, rather than suggesting complete compatibility.

Matching reads an immutable indexed snapshot. Parsing, disk IO, public-suffix warm-up and network refresh happen off the UI thread. Navigation captures page context; request callbacks do not read `WebView.url` from a background thread. Native document navigation remains available; the blocker does not replace a top-level website with an unexplained blank response.

## Limits and compatibility

This is network host filtering, not cosmetic ad hiding, fingerprinting protection, a VPN or system-wide filtering. Some same-host ads, in-stream ads, injected elements and service-worker traffic can remain. Service-worker interception is not enabled because the callback does not identify an owning tab; applying the foreground tab's exceptions could affect unrelated websites. Android also limits interception of redirect destinations and `blob:`/`javascript:` resources.

If a site breaks, use the shield to pause temporarily and reload. Save an exception only if wanted; Pebble does not automatically disable protection when streaming fails. Compare Desktop/Mobile mode separately, and include the website, app/WebView versions and effective protection setting in a bug report. Sign-in, codecs, DRM and a service's WebView restrictions remain independent compatibility factors.

References: [ABP filter syntax](https://adblockplus.org/filter-cheatsheet), [Android request interception](https://developer.android.com/reference/android/webkit/WebViewClient#shouldInterceptRequest(android.webkit.WebView,android.webkit.WebResourceRequest)), [service-worker callbacks](https://developer.android.com/reference/android/webkit/ServiceWorkerController).
