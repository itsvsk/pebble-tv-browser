package com.tvbrowser.core.webview

import org.junit.Assert.*
import org.junit.Test

class VisitProtectionPausesTest {
    @Test fun pauseSurvivesRecreatedViewAndDoesNotCrossTabOrHostBoundary() {
        val pauses = VisitProtectionPauses()
        pauses.set("tab", "https://PAGE.test/a", true)
        assertTrue(pauses.forPage("tab", "https://page.test/new-view"))
        assertFalse(pauses.forPage("other-tab", "https://page.test/a"))
        assertFalse(pauses.forPage("tab", "https://other.test"))
        assertFalse(pauses.forPage("tab", "https://page.test/back"))
    }
    @Test fun explicitResumeAndTabClosureRemovePause() {
        val pauses = VisitProtectionPauses()
        pauses.set("tab", "https://page.test", true)
        pauses.set("tab", "https://page.test", false)
        assertFalse(pauses.forPage("tab", "https://page.test"))
        pauses.set("tab", "https://page.test", true)
        pauses.clear("tab")
        assertFalse(pauses.forPage("tab", "https://page.test"))
    }
}
