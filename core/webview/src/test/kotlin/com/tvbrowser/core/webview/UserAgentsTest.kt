package com.tvbrowser.core.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentsTest {
    private val platform = "Mozilla/5.0 (Linux; Android 14; TV; wv) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/4.0 Chrome/155.0.8059.46 Mobile Safari/537.36"

    @Test fun mobileUsesThePlatformIdentityUnchanged() {
        assertEquals(platform, UserAgents.forMode(platform, desktop = false))
    }

    @Test fun desktopRetainsTheRealEngineWithoutMobileOrEmbeddedMarkers() {
        val desktop = UserAgents.forMode(platform, desktop = true)
        assertTrue(desktop.contains("Chrome/155.0.8059.46"))
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
        assertFalse(desktop.contains("; wv"))
        assertFalse(desktop.contains("Version/4.0"))
    }

    @Test fun unknownEngineDoesNotInventAChromeVersion() {
        assertEquals("Custom engine", UserAgents.forMode("Custom engine", desktop = true))
    }
}
