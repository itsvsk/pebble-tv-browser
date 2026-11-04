package com.tvbrowser.core.adblock

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class BoundedFilterInputTest {
    @Test fun rejectsOversizeStreamEvenWhenContentLengthWasUnavailable() {
        try {
            readBoundedFilterText(ByteArrayInputStream(ByteArray(MAX_FILTER_BYTES + 1)))
            fail("Oversize body accepted")
        } catch (_: IOException) { }
    }
    @Test fun decodesUtf8AndRequiresExpectedSubscriptionHeader() {
        val text = "[Adblock Plus 2.0]\n! bücher\n||ads.example^"
        assertEquals(text, readBoundedFilterText(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))))
        validateSubscriptionHeader(text)
        try {
            validateSubscriptionHeader("<html>error</html>\n$text")
            fail("Error page accepted as a subscription")
        } catch (_: IOException) { }
    }
}
