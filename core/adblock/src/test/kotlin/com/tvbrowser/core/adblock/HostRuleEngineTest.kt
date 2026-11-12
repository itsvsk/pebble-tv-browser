package com.tvbrowser.core.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class HostRuleEngineTest {
    @Test fun matchesDomainAndSubdomainsWithoutMatchingUnrelatedSuffixes() {
        val engine = engine("||tracker.example^")
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://news.example"))
        assertTrue(engine.shouldBlock("https://a.tracker.example/pixel", "https://news.example"))
        assertFalse(engine.shouldBlock("https://nottracker.example/pixel", "https://news.example"))
        assertFalse(engine.shouldBlock("https://tracker.example.evil.test/pixel", "https://news.example"))
    }

    @Test fun publicSuffixAwarePartyChecksKeepSiblingHostsFirstParty() {
        val engine = engine("||cdn.example.co.uk^${'$'}third-party")
        assertFalse(engine.shouldBlock("https://cdn.example.co.uk/pixel", "https://www.example.co.uk"))
        assertTrue(engine.shouldBlock("https://cdn.example.co.uk/pixel", "https://other.co.uk"))
    }

    @Test fun privatePublicSuffixEntriesKeepSeparateTenantsThirdParty() {
        val engine = engine("||tenant-a.github.io^${'$'}third-party")
        assertTrue(engine.shouldBlock("https://tenant-a.github.io/pixel", "https://tenant-b.github.io"))
        assertFalse(engine.shouldBlock("https://tenant-a.github.io/pixel", "https://sub.tenant-a.github.io"))
    }

    @Test fun trailingDotsDoNotChangePublicSuffixPartyClassification() {
        val engine = engine("||cdn.example.co.uk^${'$'}third-party")
        assertFalse(engine.shouldBlock("https://cdn.example.co.uk./pixel", "https://www.example.co.uk"))
        assertFalse(engine.shouldBlock("https://cdn.example.co.uk/pixel", "https://www.example.co.uk."))
        assertTrue(engine.shouldBlock("https://cdn.example.co.uk./pixel", "https://other.co.uk."))
    }

    @Test fun trailingDotsPreservePrivateSuffixTenantSeparation() {
        val engine = engine("||tenant-a.github.io^${'$'}third-party")
        assertTrue(engine.shouldBlock("https://tenant-a.github.io./pixel", "https://tenant-b.github.io."))
        assertFalse(engine.shouldBlock("https://tenant-a.github.io./pixel", "https://sub.tenant-a.github.io."))
    }

    @Test fun firstPartyOnlyRulesDoNotBlockThirdPartyRequests() {
        val engine = engine("||cdn.example.com^${'$'}~third-party")
        assertTrue(engine.shouldBlock("https://cdn.example.com/pixel", "https://www.example.com"))
        assertFalse(engine.shouldBlock("https://cdn.example.com/pixel", "https://other.example"))
    }

    @Test fun domainIncludesAndExcludesUsePageHostAndSubdomains() {
        val engine = engine("||tracker.example^${'$'}domain=news.example|~video.news.example")
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://www.news.example/article"))
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://video.news.example"))
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://sub.video.news.example"))
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://notnews.example"))
    }

    @Test fun nearestDomainScopeAppliesToBlockingRules() {
        val engine = engine("||tracker.example^${'$'}domain=~news.example|sports.news.example|~video.sports.news.example")
        val request = "https://tracker.example/pixel"
        assertTrue(engine.shouldBlock(request, "https://sports.news.example"))
        assertTrue(engine.shouldBlock(request, "https://live.sports.news.example"))
        assertFalse(engine.shouldBlock(request, "https://news.example"))
        assertFalse(engine.shouldBlock(request, "https://video.sports.news.example"))
        assertFalse(engine.shouldBlock(request, "https://other.example"))
    }

    @Test fun nearestDomainScopeAppliesToNetworkExceptions() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example^${'$'}domain=sports.news.example|~news.example
        """.trimIndent())
        val request = "https://cdn.example/item"
        assertFalse(engine.shouldBlock(request, "https://live.sports.news.example"))
        assertTrue(engine.shouldBlock(request, "https://other.news.example"))
        assertTrue(engine.shouldBlock(request, "https://sports.news.example.other.example"))
    }

    @Test fun exclusionOnlyDomainAppliesEverywhereExceptExcludedWebsite() {
        val engine = engine("||tracker.example^${'$'}domain=~news.example")
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://other.example"))
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://news.example"))
    }

    @Test fun matchingNetworkExceptionOverridesBroadParentBlock() {
        val engine = engine("""
            ||example.com^
            @@||safe.example.com^${'$'}domain=news.example
        """.trimIndent())
        assertFalse(engine.shouldBlock("https://safe.example.com/script", "https://news.example"))
        assertTrue(engine.shouldBlock("https://safe.example.com/script", "https://other.example"))
        assertTrue(engine.shouldBlock("https://ads.example.com/script", "https://news.example"))
    }

    @Test fun networkExceptionDoesNotDisableProtectionForTheEntirePage() {
        val engine = engine("""
            ||tracker.example^
            @@||news.example^
        """.trimIndent())
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://news.example"))
    }

    @Test fun documentExceptionDisablesBlockingOnlyOnItsWebsite() {
        val engine = engine("""
            ||tracker.example^
            @@||news.example^${'$'}document
        """.trimIndent())
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://sub.news.example"))
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://other.example"))
        assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://notnews.example"))
    }

    @Test fun unsupportedTypeExceptionIsWidenedWithinItsSupportedScope() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example^${'$'}script,third-party,domain=news.example|~video.news.example
        """.trimIndent())
        // Resource type is unavailable in WebResourceRequest: conservatively allow
        // this host on the scoped page instead of discarding its exception.
        assertFalse(engine.shouldBlock("https://cdn.example/image.png", "https://news.example"))
        assertTrue(engine.shouldBlock("https://cdn.example/image.png", "https://other.example"))
        assertTrue(engine.shouldBlock("https://cdn.example/image.png", "https://video.news.example"))
    }

    @Test fun unsupportedPathExceptionRetainsHostAndDomainScope() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example/allowed-script.js${'$'}domain=news.example
        """.trimIndent())
        assertFalse(engine.shouldBlock("https://cdn.example/another-script.js", "https://news.example"))
        assertTrue(engine.shouldBlock("https://cdn.example/allowed-script.js", "https://other.example"))
    }

    @Test fun caseSensitivePathExceptionConservativelyAllowsWithinItsHostAndSiteScope() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example/Allowed-Script.js${'$'}match-case,domain=news.example
        """.trimIndent())
        assertFalse(engine.shouldBlock("https://cdn.example/Allowed-Script.js", "https://news.example"))
        assertFalse(engine.shouldBlock("https://cdn.example/other-script.js", "https://news.example"))
        assertTrue(engine.shouldBlock("https://cdn.example/Allowed-Script.js", "https://other.example"))
    }

    @Test fun documentExceptionDoesNotGuessTheUnknownNavigationInitiatorParty() {
        for (party in listOf("third-party", "~third-party")) {
            val engine = engine("""
                ||tracker.example^
                @@||news.example^${'$'}document,$party,domain=news.example
            """.trimIndent())
            assertFalse(engine.shouldBlock("https://tracker.example/pixel", "https://news.example"))
            assertTrue(engine.shouldBlock("https://tracker.example/pixel", "https://other.example"))
        }
    }

    @Test fun cosmeticAndUnknownExceptionsDoNotDisableNetworkBlocking() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example^${'$'}elemhide
            @@||cdn.example^${'$'}generichide
            @@||cdn.example^${'$'}removeparam=tracking
            @@||cdn.example^${'$'}unknown-option
        """.trimIndent())
        assertTrue(engine.shouldBlock("https://cdn.example/pixel", "https://news.example"))
    }

    @Test fun badfilterDisablesEquivalentRuleAcrossSubscriptions() {
        val first = parse("||cdn.example^${'$'}third-party,domain=a.example|b.example")
        val second = parse("||cdn.example^${'$'}domain=b.example|a.example,badfilter,third-party")
        val engine = HostRuleEngine(listOf(first, second))
        assertEquals(0, engine.ruleCount)
        assertFalse(engine.shouldBlock("https://cdn.example/file", "https://a.example"))
    }

    @Test fun badfilterCanDisableAnExceptionWithoutDisablingTheBlock() {
        val engine = engine("""
            ||cdn.example^
            @@||cdn.example^${'$'}domain=news.example
            @@||cdn.example^${'$'}badfilter,domain=news.example
        """.trimIndent())
        assertTrue(engine.shouldBlock("https://cdn.example/file", "https://news.example"))
    }

    @Test fun normalizesIdnCaseAndTrailingDotsInRulesAndRequests() {
        val engine = engine("||BÜCHER.Example.^")
        assertTrue(engine.shouldBlock("https://xn--bcher-kva.example/file", "https://news.example"))
        assertTrue(engine.shouldBlock("https://bücher.example./file", "https://news.example"))
    }

    @Test fun treatsSameIpAsFirstPartyAndDifferentIpsAsThirdParty() {
        val engine = engine("||192.168.1.40^${'$'}third-party")
        assertFalse(engine.shouldBlock("https://192.168.1.40/file", "https://192.168.1.40/page"))
        assertTrue(engine.shouldBlock("https://192.168.1.40/file", "https://192.168.1.41/page"))
    }

    @Test fun invalidOrNonHttpContextDoesNotCauseAnUnconditionalBlock() {
        val engine = engine("||tracker.example^")
        assertFalse(engine.shouldBlock("https://tracker.example/pixel", "about:blank"))
        assertFalse(engine.shouldBlock("blob:https://tracker.example/file", "https://news.example"))
        assertFalse(engine.shouldBlock("not a URL", "https://news.example"))
    }

    private fun parse(source: String) = EasyListParser.parse(BufferedReader(StringReader(source)))
    private fun engine(source: String) = HostRuleEngine(listOf(parse(source)))
}
