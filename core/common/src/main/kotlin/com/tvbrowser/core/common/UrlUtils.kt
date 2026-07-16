package com.tvbrowser.core.common

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlUtils {
    private val urlRegex = Regex("""^([a-zA-Z][a-zA-Z0-9+\-.]*://|//).+""")
    private val domainRegex = Regex("""^[\w-]+(\.[\w-]+)+(?::\d{1,5})?(/.*)?$""")
    private val localhostRegex = Regex("""^localhost(?::\d{1,5})?(/.*)?$""")

    fun isLikelyUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(' ')) return false
        return urlRegex.matches(trimmed) || domainRegex.matches(trimmed) || localhostRegex.matches(trimmed)
    }

    fun normalize(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            urlRegex.matches(trimmed) -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun toQueryOrUrl(input: String): String {
        return if (isLikelyUrl(input)) normalize(input)
        else "https://www.google.com/search?q=" +
            URLEncoder.encode(input.trim(), StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    fun hostOf(url: String): String? = try {
        Uri.parse(url).host
    } catch (_: Exception) {
        null
    }
}
