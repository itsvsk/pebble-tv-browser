package com.tvbrowser.core.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class EasyListParserTest {
    @Test fun parsesUnconditionalHostRulesAndScopedExceptions() {
        val list = parse("""
            ! comment
            [Adblock Plus]
            ||example.com^
            ||sub.example.com^
            @@||allow.example.com^
            ##.banner
            ||tracker.io^
        """.trimIndent())
        assertEquals(4, list.ruleCount)
        assertEquals(3, list.blockRuleCount)
        assertEquals(1, list.unsupportedRules)
    }

    @Test fun doesNotTurnPartyRestrictedRuleIntoAnUnconditionalHostBlock() {
        val engine = engine("||cdn.example^${'$'}third-party")
        assertFalse(engine.shouldBlock("https://cdn.example/file", "https://cdn.example/page"))
        assertTrue(engine.shouldBlock("https://cdn.example/file", "https://news.example/page"))
    }

    @Test fun doesNotTurnSiteRestrictedRuleIntoAnUnconditionalHostBlock() {
        val engine = engine("||cdn.example^${'$'}domain=news.example")
        assertFalse(engine.shouldBlock("https://cdn.example/file", "https://video.example/page"))
        assertTrue(engine.shouldBlock("https://cdn.example/file", "https://news.example/page"))
    }

    @Test fun doesNotTurnResourceTypeRestrictedRuleIntoAnUnconditionalHostBlock() {
        val list = parse("||cdn.example^${'$'}script")
        assertEquals(0, list.ruleCount)
        assertEquals(1, list.unsupportedRules)
        assertFalse(HostRuleEngine(listOf(list)).shouldBlock("https://cdn.example/image.png", "https://news.example"))
    }

    @Test fun doesNotActivateDisabledFilterAsAHostBlock() {
        val list = parse("||cdn.example^${'$'}badfilter")
        assertEquals(0, list.blockRuleCount)
        assertFalse(HostRuleEngine(listOf(list)).shouldBlock("https://cdn.example/file", "https://news.example"))
    }

    @Test fun doesNotWidenPathQualifiedPatternToTheEntireHost() {
        val list = parse("||cdn.example^assets/ads.js")
        assertEquals(0, list.ruleCount)
        assertEquals(1, list.unsupportedRules)
        assertFalse(HostRuleEngine(listOf(list)).shouldBlock("https://cdn.example/image.png", "https://news.example"))
    }

    @Test fun rejectsUnsupportedModifiersAndMalformedHosts() {
        val list = parse("""
            ||example.com^${'$'}redirect=noopjs
            ||example.com^${'$'}script,third-party
            ||example.com^${'$'}domain=
            ||example.com^${'$'}third-party,~third-party
            ||*.example.com^
            ||bad_host.example^
            ||example.com
        """.trimIndent())
        assertEquals(0, list.ruleCount)
        assertEquals(7, list.unsupportedRules)
    }

    @Test fun deduplicatesRulesAndCanonicalizesBadfilterOptionOrder() {
        val list = parse("""
            ||cdn.example^${'$'}domain=b.example|a.example,third-party
            ||cdn.example^${'$'}third-party,domain=a.example|b.example
            ||cdn.example^${'$'}badfilter,third-party,domain=b.example|a.example
        """.trimIndent())
        assertEquals(0, list.ruleCount)
        assertEquals(0, list.blockRuleCount)
    }

    private fun parse(source: String): ParsedFilterList =
        EasyListParser.parse(BufferedReader(StringReader(source)))

    private fun engine(source: String) = HostRuleEngine(listOf(parse(source)))
}
