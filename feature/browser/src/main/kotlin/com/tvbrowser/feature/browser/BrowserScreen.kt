package com.tvbrowser.feature.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.cursor.CursorOverlay
import com.tvbrowser.core.cursor.CursorMode
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import com.tvbrowser.core.design.seaGlass
import com.tvbrowser.core.downloads.DownloadManagerRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowserEntryPoint {
    fun cursorController(): CursorController
    fun downloads(): DownloadManagerRepo
}

private data class BrowserDownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mime: String?,
)

private data class FailedDownload(val request: BrowserDownloadRequest, val message: String)

@Composable
fun BrowserScreen(
    tabId: String,
    cursorMode: CursorMode,
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
    val mediaStates by vm.tabManager.mediaStates.collectAsState()
    val media = mediaStates[tabId] ?: com.tvbrowser.core.webview.TabMediaState()
    var showPageMenu by remember { mutableStateOf(false) }
    var addressEditRequest by remember { mutableStateOf(0) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val controlsFocusRequester = remember { FocusRequester() }
    val errorRetryFocusRequester = remember { FocusRequester() }
    val currentOnWebNavigation by rememberUpdatedState(onWebNavigation)
    val currentOnShowChrome by rememberUpdatedState(onShowChrome)
    var focusControlsWhenShown by remember { mutableStateOf(false) }
    var failedDownload by remember { mutableStateOf<FailedDownload?>(null) }

    fun startDownload(request: BrowserDownloadRequest) {
        scope.launch {
            try {
                entry.downloads().enqueue(request.url, request.userAgent, request.contentDisposition, request.mime)
                android.widget.Toast.makeText(context, "Download started. See Downloads for progress.",
                    android.widget.Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failedDownload = FailedDownload(request, downloadFailureMessage(error))
            }
        }
    }

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
    LaunchedEffect(ui.error, cursorMode.active) {
        if (ui.error != null) {
            entry.cursorController().setActive(false)
            currentOnShowChrome()
        }
    }
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
            onNavigateDown = when {
                ui.error != null -> { { errorRetryFocusRequester.requestFocus() } }
                cursorMode.preferredActive -> onResumePointer
                else -> null
            },
            url = ui.url,
            isBookmarked = ui.isBookmarked,
            desktopUa = ui.desktopUa,
            onSubmit = { input ->
                val target = vm.submitOmnibox(input)
                vm.tabManager.navigate(tabId, target, vm.makeConfig())
            },
            onBack = {
                if (ui.error?.rendererGone == true) onBackHome() else {
                    val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                    if (wv.canGoBack()) wv.goBack() else onBackHome()
                }
            },
            onForward = {
                if (ui.error?.rendererGone != true) {
                    val wv = vm.tabManager.getOrCreate(tabId, vm.makeConfig())
                    if (wv.canGoForward()) wv.goForward()
                }
            },
            onReload = { if (ui.error != null) vm.retryPage() else vm.tabManager.getOrCreate(tabId, vm.makeConfig()).reload() },
            onBookmark = { vm.toggleBookmark() },
            onToggleUa = vm::toggleDesktopUa,
            onPageMenu = { entry.cursorController().setActive(false); showPageMenu = true },
            editRequest = addressEditRequest,
            modeEnabled = ui.siteHost != null,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (ui.url.isNotEmpty() && ui.error?.rendererGone != true) {
                WebViewHost(
                    tabId = tabId,
                    initialUrl = ui.url,
                    config = vm.makeConfig(),
                    tabManager = vm.tabManager,
                    cursorController = entry.cursorController(),
                    onPageStarted = { vm.onPageStarted(it) },
                    onPageFinished = { url, title -> vm.onPageFinished(url, title) },
                    onProgress = { vm.onProgress(it) },
                    onPageError = vm::onPageError,
                    onDownload = { url, ua, cd, mime ->
                        startDownload(BrowserDownloadRequest(url, ua, cd, mime))
                    },
                )
            }
            CursorOverlay(controller = entry.cursorController())
            if (ui.error == null) BrowserProgressIndicator(vm, Modifier.align(Alignment.TopCenter))
            ui.error?.let { error ->
                PageErrorPanel(error, errorRetryFocusRequester, onRetry = vm::retryPage,
                    onEdit = { currentOnShowChrome(); addressEditRequest++ }, onHome = onBackHome)
            }
        }
    }
    if (showPageMenu) PageMenu(ui, media, vm::toggleDesktopUa, vm::toggleSiteAdblock, vm::resetSite,
        onMute = { vm.tabManager.setTabMuted(tabId, !media.muted) },
        onBackground = { vm.tabManager.setTabBackgroundPlayback(tabId, !media.backgroundPlayback) },
        onDismiss = { showPageMenu = false })
    failedDownload?.let { failed ->
        DownloadFailureDialog(
            message = failed.message,
            onRetry = {
                failedDownload = null
                startDownload(failed.request)
            },
            onDismiss = { failedDownload = null },
        )
    }
}

private fun downloadFailureMessage(error: Exception): String = when (error) {
    is IllegalArgumentException -> "This link cannot be downloaded. Only HTTP and HTTPS links are supported."
    is IOException -> "Download storage is unavailable. Check the TV's storage and try again."
    else -> "Could not start the download. Please try again."
}

@Composable
private fun DownloadFailureDialog(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.width(520.dp)
                .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 8.dp)
                .padding(24.dp),
        ) {
            Text("Download could not start", style = PebbleType.Title, color = PebbleColors.OnSurface)
            Text(message, style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
            Row {
                DownloadDialogAction("Retry", onRetry, Modifier.focusRequester(retryFocus))
                DownloadDialogAction("Dismiss", onDismiss, Modifier.padding(start = 12.dp))
            }
        }
        LaunchedEffect(Unit) { retryFocus.requestFocus() }
    }
}

@Composable
private fun DownloadDialogAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .pebbleFocus(shape = PebbleShape.Full, lift = false)
            .seaGlass(PebbleShape.Full, PebbleColors.GlassMint, 2.dp)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, style = PebbleType.Label, color = PebbleColors.OnSurface)
    }
}

@Composable
private fun BrowserProgressIndicator(vm: BrowserViewModel, modifier: Modifier = Modifier) {
    val progress by vm.progress.collectAsState()
    if (progress in 1..99) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            color = PebbleColors.Primary,
            trackColor = PebbleColors.Surface3,
            modifier = modifier.fillMaxWidth().height(3.dp),
        )
    }
}

