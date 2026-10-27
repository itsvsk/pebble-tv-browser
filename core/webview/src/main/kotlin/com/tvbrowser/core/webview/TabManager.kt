package com.tvbrowser.core.webview

import android.content.Context
import android.webkit.WebView
import android.view.ViewGroup
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.db.repo.TabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepo: TabRepository,
    private val adBlock: AdBlockInterceptor,
    private val videoController: WebVideoController,
    private val settings: SettingsRepository,
) {
    companion object { const val MAX_LIVE = 8 }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val privateTabs = PrivateTabRegistry()
    private val closedTabs = ClosedTabs()
    private val tabMutation = Mutex()
    val recentlyClosed = closedTabs.last
    private val media = MutableStateFlow<Map<String, TabMediaState>>(emptyMap())
    val mediaStates = media.asStateFlow()
    private val foregroundTabs = mutableSetOf<String>()
    private val pageErrors = mutableMapOf<String, WebPageError>()
    fun pageError(tabId: String): WebPageError? = pageErrors[tabId]
    fun clearPageError(tabId: String) { pageErrors.remove(tabId) }
    fun isLiveView(tabId: String, view: WebView): Boolean = live[tabId] === view
    fun discardCrashedView(view: WebView) {
        val id = live.entries.firstOrNull { it.value === view }?.key ?: return
        pageErrors[id] = pageErrors[id] ?: WebPageError(view.url.orEmpty(), "Reload this page to continue.", true)
        live.remove(id)
        foregroundTabs.remove(id)
        videoController.hide(view)
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }


    fun mediaState(tabId: String): TabMediaState = media.value[tabId] ?: TabMediaState()
    fun setTabMuted(tabId: String, muted: Boolean) {
        media.value = media.value + (tabId to mediaState(tabId).copy(muted = muted))
        updateMedia(tabId)
    }
    fun setTabBackgroundPlayback(tabId: String, enabled: Boolean) {
        media.value = media.value + (tabId to mediaState(tabId).copy(backgroundPlayback = enabled))
        updateMedia(tabId)
    }
    fun setTabForeground(tabId: String, foreground: Boolean) {
        if (foreground) foregroundTabs.add(tabId) else foregroundTabs.remove(tabId)
        updateMedia(tabId)
    }
    private fun updateMedia(tabId: String) {
        live[tabId]?.let { view ->
            val state = mediaState(tabId)
            val foreground = tabId in foregroundTabs
            if (foreground || state.backgroundPlayback) view.onResume() else view.onPause()
            applyTabMediaPolicy(view, state, foreground)
        }
    }

    private val live = object : LinkedHashMap<String, WebView>(MAX_LIVE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, WebView>?): Boolean {
            val over = size > MAX_LIVE
            if (over && eldest != null) {
                videoController.hide(eldest.value)
                eldest.value.destroy()
            }
            return over
        }
    }

    fun prepareNavigation(view: WebView, url: String) {
        val preferences = settings.cachedPagePreferences(url)
        TabWebViewFactory.clientOf(view).adblockEnabled = preferences.adblockEnabled
        val desired = UserAgents.forMode(android.webkit.WebSettings.getDefaultUserAgent(context), preferences.desktopUa)
        if (view.settings.userAgentString != desired) view.settings.userAgentString = desired
    }

    fun reloadLiveTab(tabId: String) {
        live[tabId]?.let { view ->
            prepareNavigation(view, view.url.orEmpty())
            view.reload()
        }
    }

    fun navigate(tabId: String, url: String, config: TabConfig) {
        clearPageError(tabId)
        val view = getOrCreate(tabId, config)
        prepareNavigation(view, url)
        view.loadUrl(url)
    }

    @Synchronized
    fun getOrCreate(tabId: String, config: TabConfig = TabConfig()): WebView {
        live[tabId]?.let { return it }
        val privateTab = privateTabs.get(tabId) != null
        val effectiveConfig = if (privateTab && !config.isIncognito) {
            TabConfig(isIncognito = true, desktopUa = config.desktopUa, adblockEnabled = config.adblockEnabled)
        } else config
        val wv = TabWebViewFactory.create(
            context = context,
            config = effectiveConfig,
            adBlock = adBlock,
            videoController = videoController,
            onNavigation = { view, url -> clearPageError(tabId); prepareNavigation(view, url) },
            onPageError = { pageErrors[tabId] = it },
            onRendererGone = ::discardCrashedView,
            onPageFinished = { url, title ->
                updateMedia(tabId)
                if (pageErrors[tabId] != null) return@create
                if (effectiveConfig.isIncognito) {
                    privateTabs.updatePage(tabId, url, title)
                } else {
                    scope.launch {
                        tabRepo.get(tabId)?.let { t ->
                            tabRepo.upsert(t.copy(url = url, title = title, lastActiveAt = System.currentTimeMillis()))
                        }
                    }
                }
            },
        )
        live[tabId] = wv
        return wv
    }

    @Synchronized
    fun release(tabId: String) {
        live.remove(tabId)?.let { videoController.hide(it); it.destroy() }
        pageErrors.remove(tabId)
        privateTabs.remove(tabId)
        foregroundTabs.remove(tabId)
        media.value = media.value - tabId
    }

    @Synchronized
    fun destroyAll() {
        live.values.forEach { videoController.hide(it); it.destroy() }
        live.clear()
        pageErrors.clear()
        privateTabs.clear()
        foregroundTabs.clear()
        media.value = emptyMap()
    }

    suspend fun closeTab(tabId: String) = tabMutation.withLock {
        val tab = tabRepo.get(tabId)
        release(tabId)
        tabRepo.delete(tabId)
        tab?.let(closedTabs::record)
    }

    suspend fun reopenClosedTab(): String? = tabMutation.withLock {
        val tab = closedTabs.take() ?: return@withLock null
        try {
            val restored = tab.copy(id = UUID.randomUUID().toString(), lastActiveAt = System.currentTimeMillis())
            tabRepo.upsert(restored)
            restored.id
        } catch (error: Exception) {
            closedTabs.record(tab)
            throw error
        }
    }

    fun privateTab(tabId: String): TabEntity? = privateTabs.get(tabId)

    suspend fun newTab(initialUrl: String, isIncognito: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        if (isIncognito) {
            privateTabs.add(id, initialUrl)
        } else {
            tabRepo.upsert(TabEntity(id = id, url = initialUrl, isIncognito = false))
        }
        return id
    }
}
