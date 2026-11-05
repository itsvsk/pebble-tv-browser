package com.tvbrowser.core.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader
import java.net.IDN
import java.util.Locale

class EasyListParserTest {
    @Test fun hostNormalizationPreservesStd3ValidationAndInternationalDomains() {
        val hosts = listOf("Ads.Example.", "example..", ".example", "a..example", "-a.com",
            "a-.com", "bad_host.com", "a/b.com", "a:b.com", " a.com", "a.com\u0000",
            "bücher.de", "例え。テスト", "xn--bcher-kva.de", "a".repeat(64) + ".com",
            List(4) { "a".repeat(63) }.joinToString("."), "", ".", "127.0.0.1") +
            (0..127).map { "a${it.toChar()}b.example" }
        for (host in hosts) {
            val expected = runCatching {
                IDN.toASCII(host.trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
                    .takeIf { it.isNotEmpty() && it.length <= 253 &&
                        it.split('.').all { label -> label.isNotEmpty() && label.length <= 63 } }
            }.getOrNull()
            assertEquals("Host: $host", expected, normalizeFilterHost(host))
        }
    }

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
