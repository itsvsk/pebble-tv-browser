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
            val wv = tabManager.getOrCreate(tabId, config)
            attachCallbacks(wv, tabId, tabManager, adBlock, config, onPageStarted, onPageFinished, onProgress, onDownload)
            cursorController.attachTarget(wv)
            wv.apply {
                (parent as? ViewGroup)?.removeView(this)
                if (url.isNullOrEmpty() && initialUrl.isNotEmpty()) loadUrl(initialUrl)
                // Browser entry should put remote navigation inside the page, not
                // on a removed shell item or the first toolbar button.
                post { requestFocus() }
            }
        },
        update = { wv ->
            cursorController.attachTarget(wv)
            attachCallbacks(wv, tabId, tabManager, adBlock, config, onPageStarted, onPageFinished, onProgress, onDownload)
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
    tabId: String,
    tabManager: TabManager,
    adBlock: AdBlockInterceptor,
    config: TabConfig,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgress: (Int) -> Unit,
    onDownload: (String, String?, String?, String?) -> Unit,
) {
    wv.webViewClient = HostedWebViewClient(
        tabId, tabManager, adBlock, config, onPageStarted, onPageFinished,
    )
    wv.webChromeClient = object : android.webkit.WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) { onProgress(newProgress) }
    }
    wv.setDownloadListener { url, ua, contentDisposition, mime, _ ->
        onDownload(url, ua, contentDisposition, mime)
    }
}

private class HostedWebViewClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val adBlock: AdBlockInterceptor,
    private val config: TabConfig,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
) : android.webkit.WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: android.webkit.WebResourceRequest,
    ): android.webkit.WebResourceResponse? {
        // Never replace the page itself with an empty ad-block response. Host-only
        // EasyList rules are intentionally coarse and may include a navigated host.
        if (!config.adblockEnabled || request.isForMainFrame) return null
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
        tabManager.persistPage(tabId, url, view.title, config.isIncognito)
        onPageFinished(url, view.title)
    }
}
