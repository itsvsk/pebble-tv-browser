package com.tvbrowser.core.webview

import com.tvbrowser.core.common.normalizedSiteHost
import com.tvbrowser.core.adblock.protectionDocumentUrl
import java.util.concurrent.atomic.AtomicInteger

/** Per-WebView context. Main-resource interception and page-start may arrive in either order. */
internal class PageProtectionState(private val enabledForPage: (String) -> Boolean = { true }) {
    private data class Document(
        val url: String,
        val host: String?,
        val enabled: Boolean,
        val paused: Boolean = false,
        val blocked: AtomicInteger = AtomicInteger(),
        val startSeen: Boolean = false,
        val mainRequestSeen: Boolean = false,
    )

    private val navigation = Any()
    @Volatile private var document = Document("", null, enabled = false)

    fun started(url: String) = boundary(url, pageStart = true)

    private fun boundary(url: String, pageStart: Boolean) = synchronized(navigation) {
        val identity = protectionDocumentUrl(url)
        val current = document
        val repeated = if (pageStart) current.startSeen else current.mainRequestSeen
        val next = if (current.url != identity || repeated) {
            newDocument(identity)
        } else {
            current.copy(enabled = current.host != null && enabledForPage(identity))
        }
        document = next.copy(
            startSeen = next.startSeen || pageStart,
            mainRequestSeen = next.mainRequestSeen || !pageStart,
        )
    }

    private fun newDocument(url: String, paused: Boolean? = null): Document {
        val host = normalizedSiteHost(url)
        val current = document
        return Document(
            url = url,
            host = host,
            enabled = host != null && enabledForPage(url),
            paused = host != null && (paused ?: (current.host == host && current.paused)),
        )
    }

    fun pageUrl(): String = document.url

    fun prepare(url: String, paused: Boolean? = null) = synchronized(navigation) {
        val identity = protectionDocumentUrl(url)
        val current = document
        document = if (current.url == identity) {
            current.copy(
                enabled = current.host != null && enabledForPage(identity),
                paused = current.host != null && (paused ?: current.paused),
            )
        } else {
            newDocument(identity, paused)
        }
    }

    fun beginNavigation(url: String) = synchronized(navigation) {
        document = newDocument(protectionDocumentUrl(url))
    }

    fun refreshPolicy() = synchronized(navigation) {
        val current = document
        if (current.host != null) document = current.copy(enabled = enabledForPage(current.url))
    }

    fun setPaused(paused: Boolean, expectedUrl: String? = null): String? = synchronized(navigation) {
        val current = document
        if (current.host == null || (expectedUrl != null && normalizedSiteHost(expectedUrl) != current.host))
            return@synchronized null
        document = current.copy(paused = paused)
        current.url
    }

    fun stats(): PageProtectionStats {
        val current = document
        return PageProtectionStats(current.paused, current.blocked.get())
    }

    fun intercept(url: String, mainFrame: Boolean, matches: (String, String) -> Boolean): Boolean {
        if (mainFrame) {
            boundary(url, pageStart = false)
            return false
        }
        val current = document
        if (!current.enabled || current.paused) return false
        if (!matches(url, current.url)) return false
        if (document.blocked === current.blocked) current.blocked.incrementAndGet()
        return true
    }
}

data class PageProtectionStats(val paused: Boolean = false, val blockedRequests: Int = 0)
