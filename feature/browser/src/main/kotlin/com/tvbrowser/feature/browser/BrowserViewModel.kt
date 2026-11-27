package com.tvbrowser.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.normalizedSiteHost
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.db.repo.HistoryRepository
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.WebPageError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import com.tvbrowser.core.adblock.EasyListLoader

data class BrowserUi(
    val url: String = "",
    val title: String? = null,
    val isBookmarked: Boolean = false,
    val isIncognito: Boolean = false,
    val desktopUa: Boolean = false,
    val adblockEnabled: Boolean = true,
    val siteHost: String? = null,
    val siteOverridden: Boolean = false,
    val adblockOverridden: Boolean = false,
    val protectionPaused: Boolean = false,
    val blockedRequests: Int = 0,
    val error: WebPageError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabRepo: TabRepository,
    private val historyRepo: HistoryRepository,
    private val bookmarkRepo: BookmarkRepository,
    private val settings: SettingsRepository,
    val tabManager: TabManager,
    private val filters: EasyListLoader,
) : ViewModel() {
    private val _ui = MutableStateFlow(BrowserUi())
    val ui = _ui.asStateFlow()
    private val _progress = MutableStateFlow(100)
    val progress = _progress.asStateFlow()
    private var boundTabId: String? = null
    private var binding: Job? = null
    private val siteMutation = Mutex()
    val filterStatus = filters.status
    private var filterRefresh: Job? = null
    private var protectionChange: Job? = null

    fun bind(tabId: String) {
        boundTabId = tabId
        binding?.cancel()
        binding = viewModelScope.launch {
            val tab = tabManager.privateTab(tabId) ?: tabRepo.get(tabId)
            val url = tab?.url.orEmpty()
            val prefs = settings.pagePreferences(url).first()
            _ui.value = BrowserUi(url = url, title = tab?.title, isIncognito = tab?.isIncognito == true,
                desktopUa = prefs.desktopUa, adblockEnabled = prefs.adblockEnabled,
                siteHost = prefs.host, siteOverridden = prefs.overridden, adblockOverridden = prefs.adblockOverridden,
                error = tabManager.pageError(tabId), isBookmarked = bookmarkRepo.isBookmarked(url))
            _progress.value = 100
            _ui.map { it.url }.distinctUntilChanged().flatMapLatest(settings::pagePreferences).collect { page ->
                if (page.host == normalizedSiteHost(_ui.value.url)) {
                    _ui.value = _ui.value.copy(desktopUa = page.desktopUa, adblockEnabled = page.adblockEnabled,
                        siteHost = page.host, siteOverridden = page.overridden, adblockOverridden = page.adblockOverridden)
                    tabManager.refreshProtectionPolicy(tabId)
                }
            }
        }
    }

    fun makeConfig() = _ui.value.let { TabConfig(it.isIncognito, it.desktopUa) }
    fun onPageStarted(url: String) {
        val page = settings.cachedPagePreferences(url)
        _ui.value = _ui.value.copy(url = url, error = null, desktopUa = page.desktopUa,
            adblockEnabled = page.adblockEnabled, siteHost = page.host, siteOverridden = page.overridden, adblockOverridden = page.adblockOverridden)
        _progress.value = 0
    }
    fun onPageError(error: WebPageError) {
        _ui.value = _ui.value.copy(error = error)
        _progress.value = 100
    }
    fun onPageFinished(url: String, title: String?) {
        if (_ui.value.error != null) return
        _ui.value = _ui.value.copy(url = url, title = title)
        _progress.value = 100
        if (!_ui.value.isIncognito && url.startsWith("http")) viewModelScope.launch {
            historyRepo.record(url, title)
            val bookmarked = bookmarkRepo.isBookmarked(url)
            if (_ui.value.url == url) _ui.value = _ui.value.copy(isBookmarked = bookmarked)
        }
    }
    fun onProgress(p: Int) { if (_ui.value.error == null) _progress.value = p.coerceIn(0, 100) }

    override fun onCleared() {
        boundTabId?.takeIf { tabManager.privateTab(it) != null }?.let(tabManager::release)
        super.onCleared()
    }
    fun submitOmnibox(input: String): String {
        val target = UrlUtils.toQueryOrUrl(input)
        boundTabId?.let(tabManager::clearPageError)
        onPageStarted(target)
        return target
    }
    fun retryPage() {
        val id = boundTabId ?: return
        val url = _ui.value.error?.url?.takeIf { it.isNotBlank() } ?: _ui.value.url
        _ui.value = _ui.value.copy(error = null)
        tabManager.navigate(id, url, makeConfig())
    }
    fun toggleBookmark() {
        val u = _ui.value
        if (u.url.isBlank()) return
        viewModelScope.launch {
            val next = !bookmarkRepo.isBookmarked(u.url)
            if (next) bookmarkRepo.add(u.url, u.title ?: u.url) else bookmarkRepo.removeByUrl(u.url)
            if (_ui.value.url == u.url) _ui.value = _ui.value.copy(isBookmarked = next)
        }
    }
    fun toggleDesktopUa() {
        val u = _ui.value
        viewModelScope.launch { siteMutation.withLock { settings.setSiteDesktop(u.url, !u.desktopUa) } }
    }
    fun toggleSiteAdblock() {
        if (protectionChange?.isActive == true) return
        val u = _ui.value
        val tabId = boundTabId
        protectionChange = viewModelScope.launch {
            siteMutation.withLock {
                settings.setSiteAdblock(u.url, !u.adblockEnabled)
                settings.pagePreferences(u.url).first { it.adblockEnabled == !u.adblockEnabled }
                if (_ui.value.url == u.url && boundTabId == tabId && _ui.value.error == null) tabId?.let {
                    tabManager.setProtectionPaused(it, false)
                    tabManager.reloadLiveTab(it)
                }
            }
        }
    }
    fun toggleProtectionPause() {
        val id = boundTabId ?: return
        val paused = !tabManager.protectionStats(id).paused
        if (!tabManager.setProtectionPaused(id, paused, _ui.value.url)) return
        _ui.value = _ui.value.copy(protectionPaused = paused)
        tabManager.reloadLiveTab(id)
    }
    fun resetSiteProtection() {
        if (protectionChange?.isActive == true) return
        val before = _ui.value
        val tabId = boundTabId
        protectionChange = viewModelScope.launch {
            siteMutation.withLock {
                settings.resetSiteAdblock(before.url)
                settings.pagePreferences(before.url).first { !it.adblockOverridden }
                if (_ui.value.url == before.url && boundTabId == tabId) tabId?.let {
                    tabManager.setProtectionPaused(it, false)
                    tabManager.reloadLiveTab(it)
                }
            }
        }
    }
    fun refreshProtection() {
        if (filterRefresh?.isActive == true) return
        filterRefresh = viewModelScope.launch { filters.refresh() }
    }
    fun updateProtectionStats() {
        val state = boundTabId?.let(tabManager::protectionStats) ?: return
        _ui.value = _ui.value.copy(protectionPaused = state.paused, blockedRequests = state.blockedRequests)
    }

    fun resetSite() {
        val before = _ui.value
        viewModelScope.launch {
            siteMutation.withLock {
                settings.resetSite(before.url)
                val reset = settings.pagePreferences(before.url).first { !it.overridden }
                if (_ui.value.url == before.url && _ui.value.error == null && reset.desktopUa == before.desktopUa) {
                    boundTabId?.let(tabManager::reloadLiveTab)
                }
            }
        }
    }
}
