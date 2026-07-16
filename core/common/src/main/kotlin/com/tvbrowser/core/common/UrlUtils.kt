package com.tvbrowser.core.common

import android.net.Uri

object UrlUtils {
    private val urlRegex = Regex("""^([a-zA-Z][a-zA-Z0-9+\-.]*://|//).+""")
    private val domainRegex = Regex("""^[\w-]+(\.[\w-]+)+(/.*)?$""")

    fun isLikelyUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(' ')) return false
        return urlRegex.matches(trimmed) || domainRegex.matches(trimmed) || trimmed == "localhost"
    }

    fun normalize(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    }

    fun toQueryOrUrl(input: String): String {
        return if (isLikelyUrl(input)) normalize(input)
        else "https://www.google.com/search?q=" + Uri.encode(input.trim())
    }

    fun hostOf(url: String): String? = try {
        Uri.parse(url).host
    } catch (_: Exception) {
        null
    }
}
