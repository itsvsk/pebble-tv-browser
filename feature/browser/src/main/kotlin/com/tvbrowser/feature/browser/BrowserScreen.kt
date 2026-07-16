package com.tvbrowser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.cursor.CursorOverlay
import com.tvbrowser.core.cursor.CursorState
import com.tvbrowser.core.downloads.DownloadManagerRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowserEntryPoint {
    fun adBlock(): AdBlockInterceptor
    fun cursorController(): CursorController
    fun downloads(): DownloadManagerRepo
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowserScreen(
    tabId: String,
    cursorState: CursorState,
    onBackHome: () -> Unit,
    onOpenTabs: () -> Unit,
    vm: BrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val entry = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, BrowserEntryPoint::class.java)
    }
    val ui by vm.ui.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(tabId) { vm.bind(tabId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AddressBar(
            url = ui.url,
            isBookmarked = ui.isBookmarked,
            desktopUa = ui.desktopUa,
            onSubmit = { input ->
                val target = vm.submitOmnibox(input)
                vm.tabManager.getOrCreate(tabId, vm.makeConfig()).loadUrl(target)
            },
            onBack = {
                val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                if (wv.canGoBack()) wv.goBack() else onBackHome()
            },
            onForward = {
                val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                if (wv.canGoForward()) wv.goForward()
            },
            onReload = { vm.tabManager.getOrCreate(tabId, vm.makeConfig()).reload() },
            onHome = onBackHome,
            onTabs = onOpenTabs,
            onBookmark = { vm.toggleBookmark() },
            onToggleUa = {
                vm.toggleDesktopUa()
                val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                wv.settings.userAgentString =
                    if (!ui.desktopUa) com.tvbrowser.core.webview.UserAgents.DESKTOP
                    else android.webkit.WebSettings.getDefaultUserAgent(context)
                wv.reload()
            },
            cursorActive = cursorState.active,
            onToggleCursor = { entry.cursorController().setActive(!cursorState.active) },
            onMenu = { /* future: overflow */ },
        )

        if (ui.progress in 1..99) {
            LinearProgressIndicator(
                progress = { ui.progress / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (ui.url.isNotEmpty()) {
                WebViewHost(
                    tabId = tabId,
                    initialUrl = ui.url,
                    config = vm.makeConfig(),
                    tabManager = vm.tabManager,
                    adBlock = entry.adBlock(),
                    cursorController = entry.cursorController(),
                    onPageStarted = { vm.onPageStarted(it) },
                    onPageFinished = { url, title -> vm.onPageFinished(url, title) },
                    onProgress = { vm.onProgress(it) },
                    onDownload = { url, ua, cd, mime ->
                        scope.launch {
                            runCatching { entry.downloads().enqueue(url, ua, cd, mime) }
                        }
                    },
                )
            }
            CursorOverlay(state = cursorState)
        }
    }
}

