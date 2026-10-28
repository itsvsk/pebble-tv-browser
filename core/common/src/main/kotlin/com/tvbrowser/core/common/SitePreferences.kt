package com.tvbrowser.core.common

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import java.net.IDN
import java.net.URI
import java.util.Locale

data class SitePreferences(val host: String?, val desktopUa: Boolean, val adblockEnabled: Boolean, val overridden: Boolean)

fun normalizedSiteHost(url: String): String? = runCatching {
    val uri = URI(url)
    if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return null
    val host = (uri.host ?: uri.rawAuthority?.substringAfterLast('@')?.substringBefore(':'))
        ?.trimEnd('.')?.takeIf { it.isNotBlank() } ?: return null
    if (host.startsWith("[")) host.lowercase(Locale.ROOT)
    else IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
}.getOrNull()

internal fun siteDesktopKey(host: String) = booleanPreferencesKey("site.desktop.$host")
internal fun siteAdblockKey(host: String) = booleanPreferencesKey("site.adblock.$host")

internal fun Preferences.sitePreferences(url: String): SitePreferences {
    val host = normalizedSiteHost(url)
    val desktop = host?.let { this[siteDesktopKey(it)] }
    val block = host?.let { this[siteAdblockKey(it)] }
    return SitePreferences(host, desktop ?: (this[PrefKeys.DesktopUa] ?: false),
        block ?: (this[PrefKeys.AdblockEnabled] ?: true), desktop != null || block != null)
}
