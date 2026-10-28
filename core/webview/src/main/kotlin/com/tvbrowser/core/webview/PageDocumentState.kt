package com.tvbrowser.core.webview

import java.util.Locale

/** Matches callbacks to the pending or committed document without accepting earlier loads. */
internal class PageDocumentState {
    private var startedUrl: String? = null
    private var nativeUrlAtStart: String? = null
    private var pending = false

    fun started(url: String, nativeUrl: String?) {
        startedUrl = normalized(url)
        nativeUrlAtStart = nativeDocument(nativeUrl)
        pending = true
    }

    fun finished(url: String) {
        if (normalized(url) == startedUrl) pending = false
    }

    fun isCurrent(url: String, nativeUrl: String?): Boolean {
        val native = nativeDocument(nativeUrl)
        val current = if (pending && (native == null || native == nativeUrlAtStart)) startedUrl
            else native ?: startedUrl
        return current == null || normalized(url) == current
    }

    private fun nativeDocument(url: String?) = normalized(url)?.takeUnless { it.startsWith("chrome-error:") }

    private fun normalized(url: String?): String? = url?.substringBefore('#')?.let { value ->
        val colon = value.indexOf(':')
        if (colon < 0) value else value.substring(0, colon).lowercase(Locale.ROOT) + value.substring(colon)
    }
}
