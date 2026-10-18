package com.tvbrowser.feature.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.cursor.CursorOverlay
import com.tvbrowser.core.cursor.CursorState
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.downloads.DownloadManagerRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowserEntryPoint {
    fun cursorController(): CursorController
    fun downloads(): DownloadManagerRepo
}

@Composable
fun BrowserScreen(
    tabId: String,
    cursorState: CursorState,
    chromeVisible: Boolean,
    topNavFocusRequester: FocusRequester,
    onWebNavigation: () -> Unit,
    onResumePointer: () -> Unit,
    onShowChrome: () -> Unit,
    onBackHome: () -> Unit,
    vm: BrowserViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val entry = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, BrowserEntryPoint::class.java)
    }
    val ui by vm.ui.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val controlsFocusRequester = remember { FocusRequester() }
    val currentOnWebNavigation by rememberUpdatedState(onWebNavigation)
    val currentOnShowChrome by rememberUpdatedState(onShowChrome)
    var focusControlsWhenShown by remember { mutableStateOf(false) }

    DisposableEffect(entry) {
        entry.cursorController().setControlsFocusHandler {
            focusControlsWhenShown = true
            currentOnShowChrome()
        }
        entry.cursorController().setWebNavigationHandler { currentOnWebNavigation() }
        onDispose {
            entry.cursorController().setControlsFocusHandler(null)
            entry.cursorController().setWebNavigationHandler(null)
        }
    }

    LaunchedEffect(tabId) { vm.bind(tabId) }
    LaunchedEffect(chromeVisible, focusControlsWhenShown) {
        if (chromeVisible && focusControlsWhenShown) {
            controlsFocusRequester.requestFocus()
            focusControlsWhenShown = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (chromeVisible) AddressBar(
            controlsFocusRequester = controlsFocusRequester,
            topNavFocusRequester = topNavFocusRequester,
            pointerPreferred = cursorState.preferredActive,
            onResumePointer = onResumePointer,
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
            onBookmark = { vm.toggleBookmark() },
            onToggleUa = {
                vm.toggleDesktopUa()
                val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                wv.settings.userAgentString =
                    if (!ui.desktopUa) com.tvbrowser.core.webview.UserAgents.DESKTOP
                    else android.webkit.WebSettings.getDefaultUserAgent(context)
                wv.reload()
            },
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (ui.url.isNotEmpty()) {
                WebViewHost(
                    tabId = tabId,
                    initialUrl = ui.url,
                    config = vm.makeConfig(),
                    tabManager = vm.tabManager,
                    cursorController = entry.cursorController(),
                    onPageStarted = { vm.onPageStarted(it) },
                    onPageFinished = { url, title -> vm.onPageFinished(url, title) },
                    onProgress = { vm.onProgress(it) },
                    onDownload = { url, ua, cd, mime ->
                        scope.launch { runCatching { entry.downloads().enqueue(url, ua, cd, mime) } }
                    },
                )
            }
            CursorOverlay(state = cursorState)

            if (ui.progress in 1..99) {
                LinearProgressIndicator(
                    progress = { ui.progress / 100f },
                    color = PebbleColors.Primary,
                    trackColor = PebbleColors.Surface3,
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                )
            }
        }
    }
}

