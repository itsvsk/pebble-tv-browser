package com.tvbrowser.core.common

import android.net.Uri

object UrlUtils {
    private val urlRegex = Regex("""^([a-zA-Z][a-zA-Z0-9+\-.]*://|//).+""")
    private val bareAddressRegex = Regex("""^(?:[\w-]+(?:\.[\w-]+)+|localhost)(?::([0-9]{1,5}))?(?:[/?#].*)?$""")

    fun isLikelyUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(' ')) return false
        if (urlRegex.matches(trimmed)) return true
        val address = bareAddressRegex.matchEntire(trimmed) ?: return false
        val port = address.groups[1]?.value?.toInt()
        return port == null || port in 1..65535
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
