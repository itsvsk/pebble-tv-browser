package com.tvbrowser.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tvbrowser.core.adblock.AdBlockInterceptor

class TabConfig(
    val isIncognito: Boolean = false,
    val desktopUa: Boolean = false,
    val adblockEnabled: Boolean = true,
)

class TabUiCallbacks(
    val onPageStarted: (String) -> Unit,
    val onPageFinished: (String, String?) -> Unit,
    val onProgress: (Int) -> Unit,
    val onDownload: (String, String?, String?, String?) -> Unit,
)

/** Keeps tab persistence independent of whichever browser screen currently owns the WebView. */
internal class TabCallbackRelay(
    private val persistPageFinished: (String, String?) -> Unit,
) {
    private var owner: Any? = null
    private var ui: TabUiCallbacks? = null

    fun attach(owner: Any, callbacks: TabUiCallbacks) {
        this.owner = owner
        ui = callbacks
    }

    fun detach(owner: Any): Boolean {
        if (this.owner !== owner) return false
        this.owner = null
        ui = null
        return true
    }

    fun pageStarted(url: String) { ui?.onPageStarted?.invoke(url) }
    fun pageFinished(url: String, title: String?) {
        persistPageFinished(url, title)
        ui?.onPageFinished?.invoke(url, title)
    }
    fun progress(value: Int) { ui?.onProgress?.invoke(value) }
    fun download(url: String, userAgent: String?, contentDisposition: String?, mime: String?) {
        ui?.onDownload?.invoke(url, userAgent, contentDisposition, mime)
    }
}

class TabWebViewClient(
    private val adBlock: AdBlockInterceptor,
    adblockEnabled: Boolean,
    onPageFinished: (String, String?) -> Unit,
) : WebViewClient(), DownloadListener {
    private val callbacks = TabCallbackRelay(onPageFinished)

    @Volatile var adblockEnabled: Boolean = adblockEnabled

    fun attach(owner: Any, uiCallbacks: TabUiCallbacks) = callbacks.attach(owner, uiCallbacks)
    fun detach(owner: Any): Boolean = callbacks.detach(owner)
    fun onProgress(value: Int) = callbacks.progress(value)

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!adblockEnabled) return null
        val url = request.url?.toString() ?: return null
        return if (adBlock.shouldBlock(url)) {
            WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
        } else null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        callbacks.pageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        callbacks.pageFinished(url, view.title)
    }

    override fun onDownloadStart(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
        callbacks.download(url, userAgent, contentDisposition, mimeType)
    }
}

object TabWebViewFactory {

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        config: TabConfig,
        adBlock: AdBlockInterceptor,
        onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
    ): WebView {
        val wv = WebView(context)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            if (config.desktopUa) userAgentString = UserAgents.DESKTOP
            if (config.isIncognito) {
                cacheMode = WebSettings.LOAD_NO_CACHE
                saveFormData = false
            }
        }
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !config.isIncognito)

        val client = TabWebViewClient(adBlock, config.adblockEnabled, onPageFinished)
        wv.webViewClient = client
        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                client.onProgress(newProgress)
            }
        }
        wv.setDownloadListener(client)
        return wv
    }
}
