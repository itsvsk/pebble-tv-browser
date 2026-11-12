package com.tvbrowser.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UrlUtilsTest {
    @Test fun opensBareHostAndPortAsHttps() {
        val addresses = listOf(
            "192.168.1.2:8443",
            "localhost:8080/status",
            "example.com:65535?view=tv",
            "example.com:443#details",
        )
        for (address in addresses) {
            assertEquals("https://$address", UrlUtils.toQueryOrUrl(address))
        }
    }

    @Test fun preservesExplicitHttpAndHttpsAddresses() {
        val addresses = listOf("http://localhost:8080/status", "https://example.com:8443/path")
        for (address in addresses) {
            assertEquals(address, UrlUtils.toQueryOrUrl(address))
        }
    }

    @Test fun classifiesSearchTermsAndInvalidPortsAsQueries() {
        for (input in listOf("android tv", "example.com:0", "example.com:65536", "localhost:abc")) {
            assertFalse(input, UrlUtils.isLikelyUrl(input))
        }
    }
}
