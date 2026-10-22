package com.tvbrowser.core.webview

import com.tvbrowser.core.db.entity.TabEntity

/** Private tab metadata lives only as long as its browser process and open tab. */
internal class PrivateTabRegistry {
    private val tabs = mutableMapOf<String, TabEntity>()

    @Synchronized
    fun add(id: String, url: String) {
        tabs[id] = TabEntity(id = id, url = url, isIncognito = true)
    }

    @Synchronized
    fun get(id: String): TabEntity? = tabs[id]

    @Synchronized
    fun updatePage(id: String, url: String, title: String?) {
        tabs[id]?.let { tabs[id] = it.copy(url = url, title = title, lastActiveAt = System.currentTimeMillis()) }
    }

    @Synchronized
    fun remove(id: String) {
        tabs.remove(id)
    }

    @Synchronized
    fun clear() {
        tabs.clear()
    }
}
