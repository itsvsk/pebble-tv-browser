package com.tvbrowser.core.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

data class TabMediaState(val muted: Boolean = false, val backgroundPlayback: Boolean = false)

/** Engine mute protects cross-origin audio; DOM pause is best effort and never auto-resumes. */
internal fun applyTabMediaPolicy(view: WebView, state: TabMediaState, foreground: Boolean) {
    val suspendMedia = !foreground && !state.backgroundPlayback
    if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
        WebViewCompat.setAudioMuted(view, state.muted || suspendMedia)
    }
    view.evaluateJavascript("""
        (function() {
            function apply(w) {
                try {
                    var d = w.document;
                    d.__pebbleSuspendMedia = $suspendMedia;
                    d.__pebbleMuteMedia = ${state.muted};
                    function enforce(e) {
                        var v = e.target;
                        if (!v || !/^(VIDEO|AUDIO)$/.test(v.tagName)) return;
                        if (d.__pebbleSuspendMedia) v.pause();
                        if (d.__pebbleMuteMedia && !v.muted) {
                            v.__pebbleMuted = true; v.muted = true;
                        } else if (!d.__pebbleMuteMedia && v.__pebbleMuted) {
                            v.muted = false; v.__pebbleMuted = false;
                        }
                    }
                    if (!d.__pebbleMediaGuard) {
                        d.__pebbleMediaGuard = enforce;
                        d.addEventListener('play', enforce, true);
                    }
                    var media = d.querySelectorAll('video,audio');
                    for (var i = 0; i < media.length; i++) enforce({target:media[i]});
                    for (var j = 0; j < w.frames.length; j++) apply(w.frames[j]);
                } catch (_) { /* Cross-origin frames are protected by engine mute when supported. */ }
            }
            apply(window);
        })()
    """.trimIndent(), null)
}
