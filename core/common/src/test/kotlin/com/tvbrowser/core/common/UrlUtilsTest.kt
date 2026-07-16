package com.tvbrowser.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {
    @Test fun recognizesDomainsAndLocalPorts() {
        assertTrue(UrlUtils.isLikelyUrl("example.com/path"))
        assertTrue(UrlUtils.isLikelyUrl("localhost:8080/test"))
        assertTrue(UrlUtils.isLikelyUrl("192.168.1.2:8443"))
        assertFalse(UrlUtils.isLikelyUrl("two words"))
    }

    @Test fun preservesExplicitSchemesAndProtocolRelativeUrls() {
        assertEquals("ftp://example.com/file", UrlUtils.normalize("ftp://example.com/file"))
        assertEquals("https://example.com/path", UrlUtils.normalize("//example.com/path"))
        assertEquals("https://example.com", UrlUtils.normalize("example.com"))
    }

    @Test fun convertsSearchTermsAndUrls() {
        assertEquals("https://example.com", UrlUtils.toQueryOrUrl("example.com"))
        assertEquals(
            "https://www.google.com/search?q=android%20tv",
            UrlUtils.toQueryOrUrl("android tv"),
        )
    }
}
