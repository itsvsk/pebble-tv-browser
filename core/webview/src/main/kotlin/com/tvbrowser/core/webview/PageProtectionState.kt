package com.tvbrowser.core.webview

import com.tvbrowser.core.common.normalizedSiteHost
import com.tvbrowser.core.adblock.protectionDocumentUrl
import java.util.concurrent.atomic.AtomicInteger

/** Per-WebView context. Main-resource interception and page-start may arrive in either order. */
internal class PageProtectionState {
    private class Document(val url: String, val host: String?, val blocked: AtomicInteger = AtomicInteger()) {
        var startSeen = false
        var mainRequestSeen = false
    }
    private val navigation = Any()
    @Volatile private var document = Document("", null)
    @Volatile private var pausedHost: String? = null
    @Volatile var enabled = true

    fun started(url: String) = boundary(url, pageStart = true)

    private fun boundary(url: String, pageStart: Boolean) = synchronized(navigation) {
        val identity = protectionDocumentUrl(url)
        val current = document
        val repeated = if (pageStart) current.startSeen else current.mainRequestSeen
        val next = if (current.url != identity || repeated) newDocument(identity) else current
        if (pageStart) next.startSeen = true else next.mainRequestSeen = true
        document = next
    }
    private fun newDocument(url: String): Document {
        val host = normalizedSiteHost(url)
        if (pausedHost != host) pausedHost = null
        return Document(url, host)
    }
    fun pageUrl(): String = document.url
    fun prepare(url: String) = synchronized(navigation) {
        val identity = protectionDocumentUrl(url)
        if (document.url != identity) document = newDocument(identity)
    }
    fun beginNavigation(url: String) = synchronized(navigation) {
        document = newDocument(protectionDocumentUrl(url))
    }
    fun setPaused(paused: Boolean, expectedUrl: String? = null): String? = synchronized(navigation) {
        val current = document
        if (current.host == null || (expectedUrl != null && normalizedSiteHost(expectedUrl) != current.host))
            return@synchronized null
        pausedHost = current.host.takeIf { paused }
        current.url
    }
    fun stats(): PageProtectionStats {
        val current = document
        return PageProtectionStats(current.host != null && pausedHost == current.host, current.blocked.get())
    }
    fun intercept(url: String, mainFrame: Boolean, matches: (String, String) -> Boolean): Boolean {
        if (mainFrame) {
            boundary(url, pageStart = false)
            return false
        }
        val current = document
        if (!enabled || current.host == null || pausedHost == current.host) return false
        if (!matches(url, current.url)) return false
        if (document === current) current.blocked.incrementAndGet()
        return true
    }
}

data class PageProtectionStats(val paused: Boolean = false, val blockedRequests: Int = 0)
