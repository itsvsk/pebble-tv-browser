package com.tvbrowser.feature.browser

import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.TabUiCallbacks
import com.tvbrowser.core.webview.TabWebViewFactory
import com.tvbrowser.core.webview.UserAgents

@Composable
fun WebViewHost(
    tabId: String,
    initialUrl: String,
    config: TabConfig,
    tabManager: TabManager,
    cursorController: CursorController,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgress: (Int) -> Unit,
    onDownload: (url: String, ua: String?, contentDisposition: String?, mime: String?) -> Unit,
    onPageError: (com.tvbrowser.core.webview.WebPageError) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val desiredUserAgent = remember(context, config.desktopUa) {
        UserAgents.forMode(WebSettings.getDefaultUserAgent(context), config.desktopUa)
    }
    val callbackOwner = remember(tabId) { Any() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember(tabId) { arrayOfNulls<Pair<LifecycleOwner, LifecycleEventObserver>>(1) }
    val latestPageStarted = rememberUpdatedState(onPageStarted)
    val latestPageFinished = rememberUpdatedState(onPageFinished)
    val latestProgress = rememberUpdatedState(onProgress)
    val latestDownload = rememberUpdatedState(onDownload)
    val latestError = rememberUpdatedState(onPageError)

    key(tabId) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = {
                val view = tabManager.getOrCreate(tabId, config)
                (view.parent as? ViewGroup)?.removeView(view)
                val client = TabWebViewFactory.clientOf(view)
                client.attach(
                    callbackOwner,
                    TabUiCallbacks(
                        onPageStarted = { latestPageStarted.value(it) },
                        onPageError = { latestError.value(it) },
                        onPageFinished = { url, title -> latestPageFinished.value(url, title) },
                        onProgress = { latestProgress.value(it) },
                        onDownload = { url, ua, disposition, mime ->
                            latestDownload.value(url, ua, disposition, mime)
                        },
                    ),
                )
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
                val observer = LifecycleEventObserver { _, event ->
                    if (client.owns(callbackOwner) && tabManager.isLiveView(tabId, view)) {
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> { view.onResume(); tabManager.setTabForeground(tabId, true) }
                            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                                tabManager.setTabForeground(tabId, false)
                                view.onPause()
                            }
                            else -> Unit
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                lifecycleObserver[0] = lifecycleOwner to observer
                cursorController.attachTarget(view)
                if (view.url.isNullOrEmpty() && initialUrl.isNotEmpty()) view.loadUrl(initialUrl)
                view
            },
            update = update@ { view ->
                if (!tabManager.isLiveView(tabId, view)) return@update
                TabWebViewFactory.clientOf(view).adblockEnabled = config.adblockEnabled
                if (view.settings.userAgentString != desiredUserAgent) {
                    view.settings.userAgentString = desiredUserAgent
                    view.reload()
                }
            },
            onRelease = { view ->
                lifecycleObserver[0]?.let { (owner, observer) -> owner.lifecycle.removeObserver(observer) }
                lifecycleObserver[0] = null
                if (TabWebViewFactory.clientOf(view).detach(callbackOwner)) {
                    cursorController.detachTarget(view)
                    if (tabManager.isLiveView(tabId, view)) {
                        tabManager.setTabForeground(tabId, false)
                        view.onPause()
                    }
                }
            },
        )
    }
}
