package com.tvbrowser.core.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleResultsFocusGuardTest {
    @Test
    fun protectsGoogleSearchResults() {
        assertTrue(GoogleResultsFocusGuard.appliesTo("https://www.google.com/search?q=pebble"))
        assertTrue(GoogleResultsFocusGuard.appliesTo("https://google.co.in/search?q=pebble"))
    }

    @Test
    fun leavesGoogleHomeAndOtherSitesUntouched() {
        assertFalse(GoogleResultsFocusGuard.appliesTo("https://www.google.com/"))
        assertFalse(GoogleResultsFocusGuard.appliesTo("https://www.google.com/maps/search?q=pebble"))
        assertFalse(GoogleResultsFocusGuard.appliesTo("https://google.com.evil.test/search?q=pebble"))
        assertFalse(GoogleResultsFocusGuard.appliesTo("https://example.com/search?q=pebble"))
    }
}
