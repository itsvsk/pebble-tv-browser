package com.tvbrowser.core.webview

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.db.repo.TabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tabRepo: TabRepository,
    private val adBlock: AdBlockInterceptor,
) {
    companion object { const val MAX_LIVE = 8 }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _incognitoTabs = MutableStateFlow<List<TabEntity>>(emptyList())
    val incognitoTabs: StateFlow<List<TabEntity>> = _incognitoTabs.asStateFlow()
    private val live = object : LinkedHashMap<String, WebView>(MAX_LIVE, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, WebView>?): Boolean {
            val over = size > MAX_LIVE
            if (over && eldest != null) eldest.value.destroy()
            return over
        }
    }

    @Synchronized
    fun getOrCreate(tabId: String, config: TabConfig = TabConfig()): WebView {
        live[tabId]?.let { return it }
        val wv = TabWebViewFactory.create(
            context = context,
            config = config,
            adBlock = adBlock,
        )
        live[tabId] = wv
        return wv
    }

    fun persistPage(tabId: String, url: String, title: String?, isIncognito: Boolean) {
        if (isIncognito) {
            _incognitoTabs.value = _incognitoTabs.value.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(url = url, title = title, lastActiveAt = System.currentTimeMillis())
                } else tab
            }
            return
        }
        scope.launch {
            tabRepo.get(tabId)?.let { tab ->
                tabRepo.upsert(
                    tab.copy(
                        url = url,
                        title = title,
                        lastActiveAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    @Synchronized
    fun release(tabId: String) {
        live.remove(tabId)?.destroy()
        val incognito = _incognitoTabs.value.firstOrNull { it.id == tabId } ?: return
        _incognitoTabs.value = _incognitoTabs.value.filterNot { it.id == tabId }
        clearIncognitoOrigin(incognito.url)
    }

    @Synchronized
    fun destroyAll() {
        live.values.forEach { it.destroy() }
        live.clear()
    }

    suspend fun newTab(initialUrl: String, isIncognito: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        if (isIncognito) {
            _incognitoTabs.value = _incognitoTabs.value + TabEntity(
                id = id,
                url = initialUrl,
                isIncognito = true,
                position = _incognitoTabs.value.size,
            )
        } else {
            tabRepo.upsert(
                TabEntity(
                    id = id,
                    url = initialUrl,
                    isIncognito = false,
                    position = tabRepo.nextPosition(),
                )
            )
        }
        return id
    }

    suspend fun getTab(id: String): TabEntity? =
        _incognitoTabs.value.firstOrNull { it.id == id } ?: tabRepo.get(id)

    fun clearIncognitoData() {
        val snapshot = _incognitoTabs.value
        snapshot.forEach { release(it.id) }
        CookieManager.getInstance().removeSessionCookies(null)
    }

    private fun clearIncognitoOrigin(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val origin = if (uri.scheme != null && uri.authority != null) {
            "${uri.scheme}://${uri.authority}"
        } else return
        CookieManager.getInstance().removeSessionCookies(null)
        WebStorage.getInstance().deleteOrigin(origin)
    }
}
