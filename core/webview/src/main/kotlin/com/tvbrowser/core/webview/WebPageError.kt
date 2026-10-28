package com.tvbrowser.core.webview

/** A main-document failure. Subresource errors must never replace a usable page. */
data class WebPageError(val url: String, val message: String, val rendererGone: Boolean = false)
