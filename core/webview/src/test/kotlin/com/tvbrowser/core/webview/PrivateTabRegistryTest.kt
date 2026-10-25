package com.tvbrowser.core.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateTabRegistryTest {
    @Test
    fun privateTabNavigationStaysInMemoryUntilTabIsClosed() {
        val registry = PrivateTabRegistry()
        registry.add("private", "https://example.com")

        registry.updatePage("private", "https://example.com/next", "Next")

        val tab = registry.get("private")!!
        assertTrue(tab.isIncognito)
        assertEquals("https://example.com/next", tab.url)
        assertEquals("Next", tab.title)

        registry.remove("private")
        assertNull(registry.get("private"))
    }

    @Test
    fun closedTabCannotBeRecreatedByLatePageCallback() {
        val registry = PrivateTabRegistry()
        registry.add("private", "https://example.com")
        registry.remove("private")

        registry.updatePage("private", "https://example.com/late", "Late")

        assertNull(registry.get("private"))
    }
}
