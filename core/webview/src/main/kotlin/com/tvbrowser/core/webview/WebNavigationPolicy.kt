package com.tvbrowser.core.webview

/** Web documents stay in WebView; app-only links must not replace the page with an error. */
internal fun isWebDocumentScheme(scheme: String?): Boolean = when (scheme?.lowercase()) {
    "http", "https", "about", "data", "blob", "javascript" -> true
    else -> false
}
