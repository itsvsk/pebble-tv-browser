package com.tvbrowser.core.adblock

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val hosts = AtomicReference<Set<String>>(emptySet())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch { loadFromDiskOrAssets() }
    }

    private fun loadFromDiskOrAssets() {
        val disk = File(context.filesDir, "adblock/hosts.txt")
        val set = if (disk.exists()) {
            disk.bufferedReader().use { EasyListParser.parse(it) }
        } else {
            runCatching {
                context.assets.open("easylist_min.txt").use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { EasyListParser.parse(it) }
                }
            }.getOrDefault(emptySet())
        }
        hosts.set(set)
    }

    fun replace(newHosts: Set<String>) { hosts.set(newHosts) }

    fun shouldBlock(url: String): Boolean {
        val current = hosts.get()
        if (current.isEmpty()) return false
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
        if (current.contains(host)) return true
        var idx = host.indexOf('.')
        while (idx >= 0 && idx < host.length - 1) {
            if (current.contains(host.substring(idx + 1))) return true
            idx = host.indexOf('.', idx + 1)
        }
        return false
    }
}
