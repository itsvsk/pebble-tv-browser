package com.tvbrowser.core.webview

object UserAgents {
    private val chromiumVersion = Regex("Chrome/[0-9]+(?:\\.[0-9]+)*")

    /** Retain the installed engine version instead of advertising an obsolete Chrome. */
    fun forMode(defaultUserAgent: String, desktop: Boolean): String {
        if (!desktop) return defaultUserAgent
        val version = chromiumVersion.find(defaultUserAgent)?.value ?: return defaultUserAgent
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) $version Safari/537.36"
    }
}
