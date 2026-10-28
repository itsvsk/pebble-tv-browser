package com.tvbrowser.core.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationPolicyTest {
    @Test fun normalDocumentsKeepTheirNativeWebViewBehavior() {
        listOf("https", "http", "HTTPS", "about", "blob", "data", "javascript").forEach {
            assertTrue(it, isWebDocumentScheme(it))
        }
    }

    @Test fun appLinksCannotOpenExternalComponentsOrReplaceTheWebsite() {
        listOf("fb", "instagram", "intent", "market", "file", "content", "tel", "mailto", "unknown").forEach {
            assertFalse(it, isWebDocumentScheme(it))
        }
        assertFalse(isWebDocumentScheme(null))
    }
}
