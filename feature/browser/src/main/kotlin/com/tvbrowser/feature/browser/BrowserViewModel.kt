package com.tvbrowser.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.TabEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUi(
    val url: String = "",
    val title: String? = null,
    val progress: Int = 100,
    val isBookmarked: Boolean = false,
    val isIncognito: Boolean = false,
    val desktopUa: Boolean = false,
    val adblockEnabled: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
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

    private var currentTab: TabEntity? = null

    fun bind(tabId: String) {
        viewModelScope.launch {
            val tab = tabManager.getTab(tabId)
            currentTab = tab
            val desktop = settings.desktopUa.first()
            val adblock = settings.adblockEnabled.first()
            _ui.value = BrowserUi(
                url = tab?.url.orEmpty(),
                title = tab?.title,
                isIncognito = tab?.isIncognito == true,
                desktopUa = desktop,
                adblockEnabled = adblock,
            )
        }
    }

    fun makeConfig(): TabConfig {
        val u = _ui.value
        return TabConfig(isIncognito = u.isIncognito, desktopUa = u.desktopUa, adblockEnabled = u.adblockEnabled)
    }

    fun onPageStarted(url: String) {
        _ui.value = _ui.value.copy(url = url, progress = 0)
    }

    fun onPageFinished(url: String, title: String?) {
        _ui.value = _ui.value.copy(url = url, title = title, progress = 100)
        if (!_ui.value.isIncognito && url.startsWith("http")) {
            viewModelScope.launch {
                historyRepo.record(url, title)
                _ui.value = _ui.value.copy(isBookmarked = bookmarkRepo.isBookmarked(url))
            }
        }
    }

    fun onProgress(p: Int) { _ui.value = _ui.value.copy(progress = p) }

    fun onNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _ui.value = _ui.value.copy(canGoBack = canGoBack, canGoForward = canGoForward)
    }

    fun submitOmnibox(input: String): String {
        val target = UrlUtils.toQueryOrUrl(input)
        _ui.value = _ui.value.copy(url = target, progress = 0)
        return target
    }

    fun toggleBookmark() {
        val u = _ui.value
        if (u.url.isBlank() || u.isIncognito) return
        val targetUrl = u.url
        viewModelScope.launch {
            if (bookmarkRepo.isBookmarked(targetUrl)) {
                bookmarkRepo.removeByUrl(targetUrl)
                _ui.update { current ->
                    if (current.url == targetUrl) current.copy(isBookmarked = false) else current
                }
            } else {
                bookmarkRepo.add(targetUrl, u.title ?: targetUrl)
                _ui.update { current ->
                    if (current.url == targetUrl) current.copy(isBookmarked = true) else current
                }
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
