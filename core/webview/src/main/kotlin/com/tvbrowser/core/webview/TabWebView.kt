package com.tvbrowser.core.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
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

object TabWebViewFactory {

    const val DEFAULT_PAGE_SCALE_PERCENT = 80
    const val DEFAULT_TEXT_ZOOM_PERCENT = 120

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        config: TabConfig,
        adBlock: AdBlockInterceptor,
        onPageStarted: (url: String) -> Unit = {},
        onPageFinished: (url: String, title: String?) -> Unit = { _, _ -> },
        onProgress: (Int) -> Unit = {},
    ): WebView {
        val wv = WebView(context)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            textZoom = DEFAULT_TEXT_ZOOM_PERCENT
            minimumFontSize = 10
            minimumLogicalFontSize = 10
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            if (config.desktopUa) userAgentString = UserAgents.DESKTOP
            if (config.isIncognito) {
                cacheMode = WebSettings.LOAD_NO_CACHE
                saveFormData = false
            }
        }
        // Fit desktop-sized pages into a 1080p TV viewport. Text zoom above keeps
        // body copy readable while forms and dialogs remain fully reachable.
        wv.setInitialScale(DEFAULT_PAGE_SCALE_PERCENT)
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, !config.isIncognito)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? {
                // A host rule must never blank a top-level navigation. Ad blocking
                // only applies to subresources loaded by the current document.
                if (!config.adblockEnabled || request.isForMainFrame) return null
                val url = request.url?.toString() ?: return null
                return if (adBlock.shouldBlock(url)) {
                    WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                } else null
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                onPageStarted(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                onPageFinished(url, view.title)
            }
        }
        wv.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress(newProgress)
            }
        }
        return wv
    }
}
