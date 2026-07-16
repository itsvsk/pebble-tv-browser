package com.tvbrowser.feature.browser

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.TabWebViewFactory

@Composable
fun WebViewHost(
    tabId: String,
    initialUrl: String,
    config: TabConfig,
    tabManager: TabManager,
    adBlock: AdBlockInterceptor,
    cursorController: CursorController,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgress: (Int) -> Unit,
    onDownload: (url: String, ua: String?, contentDisposition: String?, mime: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val wv = tabManager.getOrCreate(tabId, config).also { existing ->
                if (existing.url.isNullOrEmpty()) existing.loadUrl(initialUrl)
            }
            attachCallbacks(wv, adBlock, config, onPageStarted, onPageFinished, onProgress, onDownload)
            cursorController.attachTarget(wv)
            wv.also {
                (it.parent as? ViewGroup)?.removeView(it)
            }
        },
        update = { wv ->
            cursorController.attachTarget(wv)
            attachCallbacks(wv, adBlock, config, onPageStarted, onPageFinished, onProgress, onDownload)
        },
    )

    DisposableEffect(tabId) {
        onDispose {
            cursorController.attachTarget(null)
        }
    }

    LaunchedEffect(tabId, initialUrl) {
        val wv = tabManager.getOrCreate(tabId, config)
        if (wv.url.isNullOrEmpty() && initialUrl.isNotEmpty()) {
            wv.loadUrl(initialUrl)
        }
    }
}

private fun attachCallbacks(
    wv: WebView,
    adBlock: AdBlockInterceptor,
    config: TabConfig,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgress: (Int) -> Unit,
    onDownload: (String, String?, String?, String?) -> Unit,
) {
    val client = wv.webViewClient
    if (client is android.webkit.WebViewClient && client !is HostedWebViewClient) {
        wv.webViewClient = HostedWebViewClient(adBlock, config, onPageStarted, onPageFinished)
    }
    wv.webChromeClient = object : android.webkit.WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) { onProgress(newProgress) }
    }
    wv.setDownloadListener { url, ua, contentDisposition, mime, _ ->
        onDownload(url, ua, contentDisposition, mime)
    }
}

private class HostedWebViewClient(
    private val adBlock: AdBlockInterceptor,
    private val config: TabConfig,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
) : android.webkit.WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: android.webkit.WebResourceRequest,
    ): android.webkit.WebResourceResponse? {
        if (!config.adblockEnabled) return null
        val url = request.url?.toString() ?: return null
        return if (adBlock.shouldBlock(url)) {
            android.webkit.WebResourceResponse(
                "text/plain", "utf-8",
                java.io.ByteArrayInputStream(ByteArray(0)),
            )
        } else null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        onPageFinished(url, view.title)
    }
}
