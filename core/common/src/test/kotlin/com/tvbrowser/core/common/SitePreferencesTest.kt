package com.tvbrowser.core.common

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.*
import org.junit.Test

class SitePreferencesTest {
    @Test fun normalizesExactHostsAndIgnoresNonWebDocuments() {
        assertEquals("example.com", normalizedSiteHost("https://EXAMPLE.com.:8443/a?q=b"))
        assertEquals("xn--bcher-kva.de", normalizedSiteHost("https://bücher.de/a"))
        assertNull(normalizedSiteHost("about:blank"))
        assertNull(normalizedSiteHost("javascript:alert(1)"))
    }
    @Test fun overridesOnlyTheExactHostAndResetRestoresGlobalDefaults() {
        val prefs = mutablePreferencesOf(PrefKeys.DesktopUa to false, PrefKeys.AdblockEnabled to true)
        prefs[siteDesktopKey("example.com")] = true
        prefs[siteAdblockKey("example.com")] = false
        assertTrue(prefs.sitePreferences("https://example.com/a").desktopUa)
        assertFalse(prefs.sitePreferences("https://example.com/b").adblockEnabled)
        assertFalse(prefs.sitePreferences("https://sub.example.com").desktopUa)
        assertFalse(prefs.sitePreferences("https://elsewhere.test").overridden)
        prefs.remove(siteDesktopKey("example.com")); prefs.remove(siteAdblockKey("example.com"))
        assertFalse(prefs.sitePreferences("https://example.com").overridden)
        assertTrue(prefs.sitePreferences("https://example.com").adblockEnabled)
    }
}
