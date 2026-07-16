package com.tvbrowser.core.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class EasyListParserTest {
    @Test fun parsesHostRules() {
        val src = """
            ! comment
            [Adblock Plus]
            ||example.com^
            ||sub.example.com^${'$'}third-party
            @@||allow.example.com^
            ##.banner
            ||tracker.io^
        """.trimIndent()
        val result = EasyListParser.parse(BufferedReader(StringReader(src)))
        assertEquals(3, result.size)
        assertTrue("example.com" in result)
        assertTrue("sub.example.com" in result)
        assertTrue("tracker.io" in result)
    }
}
