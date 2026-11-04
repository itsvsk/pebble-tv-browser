package com.tvbrowser.core.webview

import com.tvbrowser.core.common.normalizedSiteHost

/** Logical-tab visit state survives WebView eviction but is never persisted to disk. UI thread only. */
internal class VisitProtectionPauses {
    private val hosts = mutableMapOf<String, String>()
    fun set(tabId: String, url: String, paused: Boolean) {
        val host = normalizedSiteHost(url)
        if (paused && host != null) hosts[tabId] = host else hosts.remove(tabId)
    }
    fun forPage(tabId: String, url: String): Boolean {
        val saved = hosts[tabId] ?: return false
        if (saved == normalizedSiteHost(url)) return true
        hosts.remove(tabId)
        return false
    }
    fun clear(tabId: String) { hosts.remove(tabId) }
    fun clearAll() { hosts.clear() }
}
