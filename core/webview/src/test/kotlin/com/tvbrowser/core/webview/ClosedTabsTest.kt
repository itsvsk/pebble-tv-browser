package com.tvbrowser.core.webview

import com.tvbrowser.core.db.entity.TabEntity
import org.junit.Assert.*
import org.junit.Test

class ClosedTabsTest {
    @Test fun keepsLatestNormalTabAndExcludesPrivateTabs() {
        val history = ClosedTabs()
        history.record(TabEntity("one", "https://one.test", title = "One"))
        history.record(TabEntity("private", "https://private.test", isIncognito = true))
        assertEquals("One", history.last.value?.title)
        assertEquals("https://one.test", history.take()?.url)
        assertNull(history.take())
    }
    @Test fun keepsOnlyABoundedHistoryAndConsumesInReverseOrder() {
        val history = ClosedTabs(capacity = 2)
        (1..3).forEach { history.record(TabEntity("$it", "https://$it.test")) }
        assertEquals("3", history.take()?.id)
        assertEquals("2", history.take()?.id)
        assertNull(history.last.value)
    }
}
