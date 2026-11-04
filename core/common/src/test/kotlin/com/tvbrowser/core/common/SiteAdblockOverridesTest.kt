package com.tvbrowser.core.common

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteAdblockOverridesTest {
    @Test fun listsOnlyValidProtectionOverridesInHostOrder() {
        val prefs = mutablePreferencesOf(
            siteAdblockKey("z.example") to false,
            siteAdblockKey("a.example") to true,
            siteDesktopKey("desktop.example") to true,
            booleanPreferencesKey("site.adblock.bad/path") to false,
            stringPreferencesKey("site.adblock.wrong.example") to "false",
        )

        assertEquals(
            listOf(SiteAdblockOverride("a.example", true), SiteAdblockOverride("z.example", false)),
            prefs.siteAdblockOverrides(),
        )
    }

    @Test fun resettingProtectionForOneExactHostPreservesDesktopModeAndOtherSites() {
        val prefs = mutablePreferencesOf(
            PrefKeys.AdblockEnabled to true,
            siteAdblockKey("example.com") to false,
            siteAdblockKey("sub.example.com") to false,
            siteDesktopKey("example.com") to true,
        )

        prefs.resetSiteAdblock("https://EXAMPLE.com./article")

        val page = prefs.sitePreferences("https://example.com")
        assertTrue(page.adblockEnabled)
        assertFalse(page.adblockOverridden)
        assertTrue(page.desktopUa)
        assertTrue(page.overridden)
        assertFalse(prefs.sitePreferences("https://sub.example.com").adblockEnabled)
    }

    @Test fun resettingAllProtectionOverridesPreservesGlobalAndUnrelatedPreferences() {
        val prefs = mutablePreferencesOf(
            PrefKeys.AdblockEnabled to false,
            PrefKeys.Theme to "Dark",
            siteAdblockKey("example.com") to true,
            siteAdblockKey("other.example") to false,
            siteDesktopKey("example.com") to true,
        )

        prefs.resetAllSiteAdblock()

        assertTrue(prefs.siteAdblockOverrides().isEmpty())
        assertFalse(prefs.sitePreferences("https://example.com").adblockEnabled)
        assertTrue(prefs.sitePreferences("https://example.com").desktopUa)
        assertEquals("Dark", prefs[PrefKeys.Theme])
    }

    @Test fun protectionOverrideIsDistinctFromDesktopOnlyOverride() {
        val prefs = mutablePreferencesOf(siteDesktopKey("example.com") to true)
        assertFalse(prefs.sitePreferences("https://example.com").adblockOverridden)

        prefs[siteAdblockKey("example.com")] = true
        assertTrue(prefs.sitePreferences("https://example.com").adblockOverridden)
    }
}
