package com.tvbrowser.feature.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val tabRepo: TabRepository,
    private val tabManager: TabManager,
    private val bookmarks: BookmarkRepository,
) : ViewModel() {

    val tabs: StateFlow<List<TabEntity>> = combine(
        tabRepo.persisted(),
        tabManager.incognitoTabs,
    ) { persisted, incognito -> persisted + incognito }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarkedUrls: StateFlow<Set<String>> = bookmarks.all()
        .map { list -> list.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _selectedForMenu = MutableStateFlow<TabEntity?>(null)
    val selectedForMenu: StateFlow<TabEntity?> = _selectedForMenu.asStateFlow()

    fun showMenu(tab: TabEntity) { _selectedForMenu.value = tab }
    fun dismissMenu() { _selectedForMenu.value = null }

    fun close(tab: TabEntity) {
        viewModelScope.launch {
            tabManager.release(tab.id)
            tabRepo.delete(tab.id)
        }
    }

    fun toggleBookmark(tab: TabEntity) {
        if (tab.isIncognito) return
        viewModelScope.launch {
            if (bookmarks.isBookmarked(tab.url)) {
                bookmarks.removeByUrl(tab.url)
            } else {
                bookmarks.add(tab.url, tab.title?.takeIf { it.isNotBlank() } ?: tab.url)
            }
        }
    }
}
