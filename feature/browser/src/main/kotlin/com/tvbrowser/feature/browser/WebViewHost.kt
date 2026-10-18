package com.tvbrowser.feature.browser

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.TabUiCallbacks
import com.tvbrowser.core.webview.TabWebViewClient

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
    modifier: Modifier = Modifier,
) {
    val callbackOwner = remember(tabId) { Any() }
    val latestPageStarted = rememberUpdatedState(onPageStarted)
    val latestPageFinished = rememberUpdatedState(onPageFinished)
    val latestProgress = rememberUpdatedState(onProgress)
    val latestDownload = rememberUpdatedState(onDownload)

    key(tabId) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = {
                val view = tabManager.getOrCreate(tabId, config)
                (view.parent as? ViewGroup)?.removeView(view)
                (view.webViewClient as TabWebViewClient).attach(
                    callbackOwner,
                    TabUiCallbacks(
                        onPageStarted = { latestPageStarted.value(it) },
                        onPageFinished = { url, title -> latestPageFinished.value(url, title) },
                        onProgress = { latestProgress.value(it) },
                        onDownload = { url, ua, disposition, mime ->
                            latestDownload.value(url, ua, disposition, mime)
                        },
                    ),
                )
                cursorController.attachTarget(view)
                if (view.url.isNullOrEmpty() && initialUrl.isNotEmpty()) view.loadUrl(initialUrl)
                view
            },
            update = { view ->
                (view.webViewClient as? TabWebViewClient)?.adblockEnabled = config.adblockEnabled
            },
            onRelease = { view ->
                if ((view.webViewClient as? TabWebViewClient)?.detach(callbackOwner) == true) {
                    cursorController.detachTarget(view)
                }
            },
        )
    }
}
