package com.tvbrowser.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.MostVisitedView
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.db.repo.HistoryRepository
import com.tvbrowser.core.webview.TabManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepo: HistoryRepository,
    private val bookmarkRepo: BookmarkRepository,
    private val tabManager: TabManager,
) : ViewModel() {

    val mostVisited: StateFlow<List<MostVisitedView>> = historyRepo.topVisited(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarkedUrls: StateFlow<Set<String>> = bookmarkRepo.all()
        .map { list -> list.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _selectedForMenu = MutableStateFlow<MostVisitedView?>(null)
    val selectedForMenu: StateFlow<MostVisitedView?> = _selectedForMenu.asStateFlow()

    fun openInNewTab(input: String, isIncognito: Boolean = false, onReady: (String) -> Unit) {
        val url = UrlUtils.toQueryOrUrl(input)
        viewModelScope.launch {
            val id = tabManager.newTab(url, isIncognito)
            onReady(id)
        }
    }

    fun showMenu(item: MostVisitedView) { _selectedForMenu.value = item }
    fun dismissMenu() { _selectedForMenu.value = null }

    fun toggleBookmark(item: MostVisitedView) {
        viewModelScope.launch {
            val url = item.url
            if (bookmarkRepo.isBookmarked(url)) {
                bookmarkRepo.removeByUrl(url)
            } else {
                val host = UrlUtils.hostOf(url) ?: url
                val title = item.title?.takeIf { it.isNotBlank() } ?: host
                val favicon = "https://www.google.com/s2/favicons?domain=$host&sz=64"
                bookmarkRepo.add(url = url, title = title, faviconUrl = favicon)
            }
        }
    }

    fun deleteEntry(item: MostVisitedView) {
        viewModelScope.launch {
            historyRepo.deleteByUrl(item.url)
            _selectedForMenu.value = null
        }
    }
}
