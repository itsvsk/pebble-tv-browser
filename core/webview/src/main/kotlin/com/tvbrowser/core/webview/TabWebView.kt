package com.tvbrowser.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.RenderProcessGoneDetail
import androidx.annotation.RequiresApi
import android.widget.Toast
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
    val onPageError: (WebPageError) -> Unit = {},
)

/** Keeps tab persistence independent of whichever browser screen currently owns the WebView. */
internal class TabCallbackRelay(
    private val persistPageFinished: (String, String?) -> Unit,
) {
    private var owner: Any? = null
    private var ui: TabUiCallbacks? = null
    private var error: WebPageError? = null
    val hasUiOwner: Boolean get() = ui != null

    fun attach(owner: Any, callbacks: TabUiCallbacks) {
        this.owner = owner
        ui = callbacks
        error?.let(callbacks.onPageError)
    }

    fun detach(owner: Any): Boolean {
        if (this.owner !== owner) return false
        this.owner = null
        ui = null
        return true
    }

    fun owns(owner: Any): Boolean = this.owner === owner

    fun pageStarted(url: String) { error = null; ui?.onPageStarted?.invoke(url) }
    fun pageError(value: WebPageError) { error = value; ui?.onPageError?.invoke(value) }
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
    private val hideFullscreen: () -> Unit = {},
    private val persistError: (WebPageError) -> Unit = {},
    private val configureNavigation: (WebView, String) -> Unit = { _, _ -> },
    private val documentAvailable: (WebView) -> Unit = {},
    private val rendererGone: (WebView) -> Unit = { view ->
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
    },
) : WebViewClient(), DownloadListener {
    private val callbacks = TabCallbackRelay(onPageFinished)
    private val documents = PageDocumentState()

    @Volatile var adblockEnabled: Boolean = adblockEnabled

    fun attach(owner: Any, uiCallbacks: TabUiCallbacks) = callbacks.attach(owner, uiCallbacks)
    fun detach(owner: Any): Boolean = callbacks.detach(owner).also { if (it) hideFullscreen() }
    fun owns(owner: Any): Boolean = callbacks.owns(owner)
    val hasUiOwner: Boolean get() = callbacks.hasUiOwner
    fun onProgress(value: Int) = callbacks.progress(value)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (isWebDocumentScheme(request.url.scheme)) {
            if (request.isForMainFrame) {
                documents.started(request.url.toString(), view.url)
                configureNavigation(view, request.url.toString())
            }
            return false
        }
        if (request.isForMainFrame && request.hasGesture()) {
            Toast.makeText(view.context, "This link requires another app. Try the website's desktop mode.", Toast.LENGTH_LONG).show()
        }
        return true
    }

    @Deprecated("Compatibility with older WebView providers")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        !isWebDocumentScheme(Uri.parse(url).scheme)

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!adblockEnabled) return null
        val url = request.url?.toString() ?: return null
        return if (adBlock.shouldBlock(url)) {
            WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
        } else null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        documents.started(url, view.url)
        hideFullscreen()
        configureNavigation(view, url)
        documentAvailable(view)
        callbacks.pageStarted(url)
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        // The new DOM is available before slow images/styles finish loading.
        // Reapply current policy even if this is a late visual callback.
        documentAvailable(view)
    }

    override fun onPageFinished(view: WebView, url: String) {
        documents.finished(url)
        if (!isCurrentDocument(view, url)) return
        if (GoogleResultsFocusGuard.appliesTo(url)) {
            view.evaluateJavascript(GoogleResultsFocusGuard.script, null)
        }
        callbacks.pageFinished(url, view.title)
    }

    private fun isCurrentDocument(view: WebView, url: String): Boolean = documents.isCurrent(url, view.url)

    private fun reportError(error: WebPageError) {
        hideFullscreen()
        persistError(error)
        callbacks.pageError(error)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame && isCurrentDocument(view, request.url.toString())) reportError(WebPageError(request.url.toString(),
            when (error.errorCode) {
                ERROR_HOST_LOOKUP, ERROR_CONNECT -> "Could not connect to this website. Check your connection and try again."
                ERROR_TIMEOUT -> "This website took too long to respond."
                ERROR_FAILED_SSL_HANDSHAKE -> "A secure connection could not be established."
                else -> "This page could not be loaded."
            }))
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        // Keep website-provided sign-in, challenge and not-found responses accessible.
        if (request.isForMainFrame && isCurrentDocument(view, request.url.toString()) && response.statusCode in 500..599) {
            reportError(WebPageError(request.url.toString(), "This website returned HTTP ${response.statusCode}."))
        }
    }

    @RequiresApi(26)
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        reportError(WebPageError(view.url.orEmpty(), "This page stopped responding. Reload it to continue.", rendererGone = true))
        rendererGone(view)
        return true
    }

    override fun onDownloadStart(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
        callbacks.download(url, userAgent, contentDisposition, mimeType)
    }
}

object TabWebViewFactory {

    /** The platform client getter is unavailable on Android 7; retain our owned client on the view. */
    fun clientOf(view: WebView): TabWebViewClient =
        view.getTag(R.id.pebble_tab_webview_client) as TabWebViewClient

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        config: TabConfig,
        adBlock: AdBlockInterceptor,
        videoController: WebVideoController,
        onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
        onPageError: (WebPageError) -> Unit = {},
        onNavigation: (WebView, String) -> Unit = { _, _ -> },
        onDocumentAvailable: (WebView) -> Unit = {},
        onRendererGone: (WebView) -> Unit = { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        },
    ): WebView {
        val wv = WebView(context).apply {
            // AndroidView otherwise supplies WRAP_CONTENT, giving Blink an unbounded
            // height: streaming sites using 100%/100vh then collapse to zero pixels.
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString = UserAgents.forMode(WebSettings.getDefaultUserAgent(context), config.desktopUa)
            if (config.isIncognito) {
                cacheMode = WebSettings.LOAD_NO_CACHE
                saveFormData = false
            }
        }
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !config.isIncognito)

        val client = TabWebViewClient(adBlock, config.adblockEnabled, onPageFinished,
            hideFullscreen = { videoController.hide(wv) }, persistError = onPageError, configureNavigation = onNavigation,
            documentAvailable = onDocumentAvailable, rendererGone = onRendererGone)
        wv.setTag(R.id.pebble_tab_webview_client, client)
        wv.webViewClient = client
        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                if (!client.hasUiOwner || !wv.isAttachedToWindow || !wv.isShown) {
                    callback.onCustomViewHidden()
                    return
                }
                videoController.show(wv, view, callback)
            }

            override fun onHideCustomView() {
                videoController.hide(wv)
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                client.onProgress(newProgress)
            }
        }
        wv.setDownloadListener(client)
        return wv
    }
}
