package com.tvbrowser.core.webview

import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/** Session-only metadata; private pages and live WebViews never enter this history. */
internal class ClosedTabs(private val capacity: Int = 10) {
    private val entries = ArrayDeque<TabEntity>()
    private val current = MutableStateFlow<TabEntity?>(null)
    val last = current.asStateFlow()

    fun record(tab: TabEntity) {
        if (tab.isIncognito) return
        entries.addFirst(tab)
        while (entries.size > capacity) entries.removeLast()
        current.value = entries.peekFirst()
    }

    fun take(): TabEntity? = entries.pollFirst().also { current.value = entries.peekFirst() }
}
