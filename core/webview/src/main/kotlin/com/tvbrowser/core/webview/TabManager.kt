package com.tvbrowser.core.webview

import android.content.Context
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cacheCapacity = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).let { manager ->
        val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        TabCachePolicy.capacity(manager.isLowRamDevice, manager.memoryClass, memory.totalMem)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val privateTabs = PrivateTabRegistry()
    private val closedTabs = ClosedTabs()
    private val protectionPauses = VisitProtectionPauses()
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
        unregisterMemoryCallbacksIfEmpty()
    }


    fun mediaState(tabId: String): TabMediaState = media.value[tabId] ?: TabMediaState()
    fun setTabMuted(tabId: String, muted: Boolean) {
        media.value = media.value + (tabId to mediaState(tabId).copy(muted = muted))
        updateMedia(tabId)
    }
    fun setTabBackgroundPlayback(tabId: String, enabled: Boolean) {
        val current = media.value
        val state = current[tabId] ?: TabMediaState()
        if (state.backgroundPlayback == enabled) return
        val previous = if (enabled) {
            current.entries.firstOrNull { it.key != tabId && it.value.backgroundPlayback }
        } else null
        val updated = current.toMutableMap()
        previous?.let { updated[it.key] = it.value.copy(backgroundPlayback = false) }
        updated[tabId] = state.copy(backgroundPlayback = enabled)
        media.value = updated
        previous?.let { updateMedia(it.key) }
        updateMedia(tabId)
        scheduleCacheTrim()
    }
    fun setTabForeground(tabId: String, foreground: Boolean) {
        if (foreground) foregroundTabs.add(tabId) else foregroundTabs.remove(tabId)
        updateMedia(tabId)
        scheduleCacheTrim()
    }

    private fun scheduleCacheTrim() {
        // AndroidView may still own its parent during onRelease. Trim after it detaches.
        mainHandler.removeCallbacks(trimCache)
        mainHandler.post(trimCache)
    }
    private fun updateMedia(tabId: String) {
        live[tabId]?.let { view ->
            val state = mediaState(tabId)
            val foreground = tabId in foregroundTabs
            if (foreground || state.backgroundPlayback) view.onResume() else view.onPause()
            applyTabMediaPolicy(view, state, foreground)
        }
    }

    private val live = LinkedHashMap<String, WebView>(cacheCapacity, 0.75f, true)
    private data class SavedPage(val url: String, val history: Bundle?)
    // Navigation-only state stays in memory, including for private tabs. Never persist DOM data.
    private val savedPages = object : LinkedHashMap<String, SavedPage>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, SavedPage>?): Boolean = size > 16
    }
    private val trimCache = Runnable { trimLiveViews(cacheCapacity) }

    private var callbacksRegistered = false
    private val memoryCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        override fun onLowMemory() { scheduleMemoryTrim(0) }
        override fun onTrimMemory(level: Int) {
            // UI_HIDDEN is delivered on modern Android too; running-pressure callbacks
            // are only useful on older versions. Never rely on them for the normal cap.
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> scheduleMemoryTrim(0)
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> scheduleMemoryTrim(1)
            }
        }
    }

    private fun scheduleMemoryTrim(capacity: Int) {
        mainHandler.removeCallbacks(trimCache)
        mainHandler.post { trimLiveViews(capacity, preserveHistory = false) }
    }

    private fun registerMemoryCallbacks() {
        if (!callbacksRegistered) {
            context.registerComponentCallbacks(memoryCallbacks)
            callbacksRegistered = true
        }
    }

    private fun unregisterMemoryCallbacksIfEmpty() {
        if (live.isEmpty() && callbacksRegistered) {
            context.unregisterComponentCallbacks(memoryCallbacks)
            callbacksRegistered = false
        }
    }

    private fun trimLiveViews(capacity: Int, incoming: String? = null, preserveHistory: Boolean = true) {
        val protected = live.filter { (id, view) ->
            id == incoming || id in foregroundTabs || mediaState(id).backgroundPlayback ||
                view.parent != null || TabWebViewFactory.clientOf(view).hasUiOwner
        }.keys
        for (id in TabCachePolicy.evictions(live.keys.toList(), protected, capacity)) {
            val view = live.remove(id) ?: continue
            val history = if (preserveHistory) captureHistory(view) else null
            savedPages[id] = SavedPage(view.url.orEmpty(), history)
            disposeView(view)
        }
        unregisterMemoryCallbacksIfEmpty()
    }

    private fun captureHistory(view: WebView): Bundle? {
        val history = Bundle()
        if (runCatching { view.saveState(history) }.getOrNull() == null) return null
        // At most 4 MiB of serialized history; provider state also has heap overhead.
        val parcel = Parcel.obtain()
        return try {
            runCatching {
                parcel.writeBundle(history)
                history.takeIf { parcel.dataSize() <= 256 * 1024 }
            }.getOrNull()
        } finally {
            parcel.recycle()
        }
    }

    private fun disposeView(view: WebView) {
        videoController.hide(view)
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    }

    fun prepareNavigation(view: WebView, url: String) {
        val preferences = settings.cachedPagePreferences(url)
        val paused = live.entries.firstOrNull { it.value === view }?.key?.let { protectionPauses.forPage(it, url) }
        TabWebViewFactory.clientOf(view).prepareProtection(url, paused)
        val desired = UserAgents.forMode(android.webkit.WebSettings.getDefaultUserAgent(context), preferences.desktopUa)
        if (view.settings.userAgentString != desired) view.settings.userAgentString = desired
    }

    fun refreshProtectionPolicy(tabId: String) {
        live[tabId]?.let { TabWebViewFactory.clientOf(it).refreshProtectionPolicy() }
    }

    fun protectionStats(tabId: String): PageProtectionStats =
        live[tabId]?.let { TabWebViewFactory.clientOf(it).protectionStats() } ?: PageProtectionStats()

    fun setProtectionPaused(tabId: String, paused: Boolean, expectedUrl: String? = null): Boolean {
        val view = live[tabId] ?: return false
        val url = TabWebViewFactory.clientOf(view).setProtectionPaused(paused, expectedUrl) ?: return false
        protectionPauses.set(tabId, url, paused)
        return true
    }

    fun reloadLiveTab(tabId: String) {
        live[tabId]?.let { view ->
            TabWebViewFactory.clientOf(view).beginProtectionNavigation(view.url.orEmpty())
            prepareNavigation(view, view.url.orEmpty())
            view.reload()
        }
    }

    fun navigate(tabId: String, url: String, config: TabConfig) {
        clearPageError(tabId)
        val view = getOrCreate(tabId, config, restoreHistory = false)
        TabWebViewFactory.clientOf(view).beginProtectionNavigation(url)
        prepareNavigation(view, url)
        view.loadUrl(url)
    }

    @Synchronized
    fun getOrCreate(tabId: String, config: TabConfig = TabConfig(), restoreHistory: Boolean = true): WebView {
        live[tabId]?.let { return it }
        registerMemoryCallbacks()
        val saved = savedPages.remove(tabId).takeIf { restoreHistory }
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
            protectionEnabledForPage = { url -> settings.cachedPagePreferences(url).adblockEnabled },
            onDocumentAvailable = { updateMedia(tabId) },
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
        if (saved != null && saved.url.isNotEmpty()) {
            TabWebViewFactory.clientOf(wv).beginProtectionNavigation(saved.url)
            prepareNavigation(wv, saved.url)
            val restored = saved.history?.let { runCatching { wv.restoreState(it) }.getOrNull() }
            if (restored == null) wv.loadUrl(saved.url)
        }
        trimLiveViews(cacheCapacity, incoming = tabId)
        return wv
    }

    @Synchronized
    fun release(tabId: String) {
        live.remove(tabId)?.let(::disposeView)
        savedPages.remove(tabId)
        pageErrors.remove(tabId)
        privateTabs.remove(tabId)
        protectionPauses.clear(tabId)
        foregroundTabs.remove(tabId)
        media.value = media.value - tabId
        unregisterMemoryCallbacksIfEmpty()
    }

    @Synchronized
    fun destroyAll() {
        live.values.forEach(::disposeView)
        live.clear()
        savedPages.clear()
        pageErrors.clear()
        privateTabs.clear()
        protectionPauses.clearAll()
        foregroundTabs.clear()
        media.value = emptyMap()
        mainHandler.removeCallbacksAndMessages(null)
        unregisterMemoryCallbacksIfEmpty()
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
