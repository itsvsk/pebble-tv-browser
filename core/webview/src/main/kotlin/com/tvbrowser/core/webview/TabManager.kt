package com.tvbrowser.core.webview

import android.content.Context
import android.webkit.WebView
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.db.repo.TabRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
            onPageFinished = { url, title ->
                if (!config.isIncognito) {
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
        live.remove(tabId)?.destroy()
    }

    @Synchronized
    fun destroyAll() {
        live.values.forEach { it.destroy() }
        live.clear()
    }

    suspend fun newTab(initialUrl: String, isIncognito: Boolean = false): String {
        val id = UUID.randomUUID().toString()
        if (!isIncognito) {
            tabRepo.upsert(TabEntity(id = id, url = initialUrl, isIncognito = false))
        }
        return id
    }
}
