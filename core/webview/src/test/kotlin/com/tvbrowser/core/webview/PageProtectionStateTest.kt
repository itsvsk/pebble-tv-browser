package com.tvbrowser.core.webview

import org.junit.Assert.*
import org.junit.Test

class PageProtectionStateTest {
    @Test fun onlySubresourcesAreBlockedAndCounted() {
        val state = PageProtectionState()
        state.started("https://page.test/a")
        assertFalse(state.intercept("https://page.test/a", true) { _, _ -> true })
        assertTrue(state.intercept("https://ads.test", false) { _, page -> page == "https://page.test/a" })
        assertEquals(1, state.stats().blockedRequests)
    }
    @Test fun pauseSurvivesReloadButEndsWhenHostChangesAndDoesNotAffectOtherTabs() {
        val state = PageProtectionState()
        state.started("https://page.test/a")
        state.setPaused(true)
        state.started("https://page.test/b")
        assertTrue(state.stats().paused)
        assertFalse(state.intercept("https://ads.test", false) { _, _ -> true })
        val other = PageProtectionState().also { it.started("https://page.test") }
        assertTrue(other.intercept("https://ads.test", false) { _, _ -> true })
        state.started("https://other.test")
        assertFalse(state.stats().paused)
        assertTrue(state.intercept("https://ads.test", false) { _, _ -> true })
    }
    @Test fun disabledProtectionAndUnknownPageContextDoNotBlock() {
        assertFalse(PageProtectionState().intercept("https://ads.test", false) { _, _ -> true })
        val state = PageProtectionState { false }
        state.started("https://page.test")
        assertFalse(state.intercept("https://ads.test", false) { _, _ -> true })
    }
    @Test fun earlySubresourcesUseTheDestinationPolicyInEitherCallbackOrder() {
        val allowed = "https://allowed.test/page"
        val protected = "https://protected.test/page"
        val settings = mapOf(allowed to false, protected to true)
        for (pageStartFirst in listOf(true, false)) {
            val state = PageProtectionState(settings::getValue)
            // Exercise both directions without a UI prepare callback before navigation.
            for (page in listOf(allowed, protected, allowed)) {
                if (pageStartFirst) state.started(page)
                else assertFalse(state.intercept(page, true) { _, _ -> true })

                assertEquals(
                    "Early request for $page; pageStartFirst=$pageStartFirst",
                    settings.getValue(page),
                    state.intercept("https://ads.test", false) { _, document -> document == page },
                )

                if (pageStartFirst) assertFalse(state.intercept(page, true) { _, _ -> true })
                else state.started(page)
                assertEquals(if (page == protected) 1 else 0, state.stats().blockedRequests)
            }
        }
    }
    @Test fun settingsRefreshUsesTheCurrentDocumentAndPreservesItsCount() {
        val page = "https://page.test/a"
        var enabled = true
        val state = PageProtectionState { url -> url == page && enabled }
        state.started(page)
        assertTrue(state.intercept("https://ads.test", false) { _, _ -> true })

        enabled = false
        state.refreshPolicy()
        assertFalse(state.intercept("https://ads.test", false) { _, _ -> true })
        assertEquals(1, state.stats().blockedRequests)

        enabled = true
        state.refreshPolicy()
        assertTrue(state.intercept("https://ads.test", false) { _, _ -> true })
        assertEquals(2, state.stats().blockedRequests)
    }
    @Test fun mainRequestUsesPolicyChangedAfterNavigationWasPrepared() {
        val page = "https://page.test/a"
        var enabled = false
        val state = PageProtectionState { enabled }
        state.prepare(page)

        enabled = true
        assertFalse(state.intercept(page, true) { _, _ -> true })
        assertTrue(state.intercept("https://ads.test", false) { _, _ -> true })
    }
    @Test fun earlyRequestsAndLatePageStartBelongToTheSamePreparedDocument() {
        val state = PageProtectionState()
        state.prepare("https://page.test/a")
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.started("https://page.test/a")
        assertEquals(1, state.stats().blockedRequests)
    }
    @Test fun mainRequestAndPageStartCoalesceInEitherOrderButReloadResetsCounts() {
        val state = PageProtectionState()
        state.intercept("https://page.test/a", true) { _, _ -> false }
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.started("https://page.test/a")
        assertEquals(1, state.stats().blockedRequests)
        state.started("https://page.test/a")
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.intercept("https://page.test/a", true) { _, _ -> false }
        assertEquals(1, state.stats().blockedRequests)
        state.intercept("https://page.test/a", true) { _, _ -> false }
        assertEquals(0, state.stats().blockedRequests)
    }
    @Test fun fragmentsAndCanonicalHostFormsDoNotSplitDocumentIdentity() {
        val state = PageProtectionState()
        state.prepare("HTTPS://PAGE.test:443/a#one")
        state.intercept("https://page.test/a", true) { _, _ -> false }
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.started("https://page.test/a#one")
        state.prepare("https://page.test/a#two")
        assertEquals(1, state.stats().blockedRequests)
    }
    @Test fun explicitReloadStartsOneProvisionalDocumentBeforeEitherCallback() {
        val state = PageProtectionState()
        val page = "https://page.test/a"
        state.intercept(page, true) { _, _ -> false }
        state.started(page)
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.beginNavigation(page)
        state.intercept("https://ads.test", false) { _, _ -> true }
        state.intercept(page, true) { _, _ -> false }
        state.started(page)
        assertEquals(1, state.stats().blockedRequests)
    }
    @Test fun pauseCannotApplyToANavigationThatHasMovedToAnotherHost() {
        val state = PageProtectionState()
        state.started("https://first.test/a")
        state.intercept("https://second.test/a", true) { _, _ -> false }
        assertNull(state.setPaused(true, "https://first.test/a"))
        assertFalse(state.stats().paused)
    }
    @Test fun lateRequestCannotCountAgainstANewerDocument() {
        val state = PageProtectionState()
        state.started("https://page.test/a")
        state.intercept("https://ads.test", false) { _, _ -> state.started("https://page.test/b"); true }
        assertEquals(0, state.stats().blockedRequests)
    }
}
