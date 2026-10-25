package com.tvbrowser.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.db.repo.HistoryRepository
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUi(
    val url: String = "",
    val title: String? = null,
    val isBookmarked: Boolean = false,
    val isIncognito: Boolean = false,
    val desktopUa: Boolean = false,
    val adblockEnabled: Boolean = true,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabRepo: TabRepository,
    private val historyRepo: HistoryRepository,
    private val bookmarkRepo: BookmarkRepository,
    private val settings: SettingsRepository,
    val tabManager: TabManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(BrowserUi())
    val ui: StateFlow<BrowserUi> = _ui.asStateFlow()
    private val _progress = MutableStateFlow(100)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    private var boundTabId: String? = null

    fun bind(tabId: String) {
        boundTabId = tabId
        viewModelScope.launch {
            val tab = tabManager.privateTab(tabId) ?: tabRepo.get(tabId)
            val desktop = settings.desktopUa.first()
            val adblock = settings.adblockEnabled.first()
            _ui.value = BrowserUi(
                url = tab?.url.orEmpty(),
                title = tab?.title,
                isIncognito = tab?.isIncognito == true,
                desktopUa = desktop,
                adblockEnabled = adblock,
            )
            _progress.value = 100
        }
    }

    fun makeConfig(): TabConfig {
        val u = _ui.value
        return TabConfig(isIncognito = u.isIncognito, desktopUa = u.desktopUa, adblockEnabled = u.adblockEnabled)
    }

    fun onPageStarted(url: String) {
        _ui.value = _ui.value.copy(url = url)
        _progress.value = 0
    }

    fun onPageFinished(url: String, title: String?) {
        _ui.value = _ui.value.copy(url = url, title = title)
        _progress.value = 100
        if (!_ui.value.isIncognito && url.startsWith("http")) {
            viewModelScope.launch {
                historyRepo.record(url, title)
                _ui.value = _ui.value.copy(isBookmarked = bookmarkRepo.isBookmarked(url))
            }
        }
    }

    fun onProgress(p: Int) { _progress.value = p.coerceIn(0, 100) }

    override fun onCleared() {
        // A private tab is absent from the persisted Tabs list. The navigation entry owns it,
        // so remove its WebView and in-memory metadata when that entry leaves the back stack.
        boundTabId?.takeIf { tabManager.privateTab(it) != null }?.let(tabManager::release)
        super.onCleared()
    }

    fun submitOmnibox(input: String): String {
        val target = UrlUtils.toQueryOrUrl(input)
        _ui.value = _ui.value.copy(url = target)
        _progress.value = 0
        return target
    }

    fun toggleBookmark() {
        val u = _ui.value
        if (u.url.isBlank()) return
        viewModelScope.launch {
            if (bookmarkRepo.isBookmarked(u.url)) {
                bookmarkRepo.removeByUrl(u.url)
                _ui.value = u.copy(isBookmarked = false)
            } else {
                bookmarkRepo.add(u.url, u.title ?: u.url)
                _ui.value = u.copy(isBookmarked = true)
            }
        }
    }

    fun toggleDesktopUa() {
        viewModelScope.launch {
            val next = !_ui.value.desktopUa
            settings.set(PrefKeys.DesktopUa, next)
            _ui.value = _ui.value.copy(desktopUa = next)
        }
    }
}
