package com.tvbrowser.app.navigation

object Routes {
    const val HOME = "home"
    const val BROWSER = "browser/{tabId}"
    const val TABS = "tabs"
    const val BOOKMARKS = "bookmarks"
    const val HISTORY = "history"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    fun browser(tabId: String) = "browser/$tabId"
}
