package com.tvbrowser.core.webview

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

class FullscreenVideo internal constructor(
    val owner: WebView,
    val view: View,
    internal val callback: WebChromeClient.CustomViewCallback,
)

/** Main-thread ownership of WebView's fullscreen surface and its one-shot exit callback. */
@Singleton
class WebVideoController @Inject constructor() {
    private val _fullscreen = MutableStateFlow<FullscreenVideo?>(null)
    val fullscreen = _fullscreen.asStateFlow()

    fun show(owner: WebView, view: View, callback: WebChromeClient.CustomViewCallback) {
        if (_fullscreen.value != null) {
            callback.onCustomViewHidden()
            return
        }
        _fullscreen.value = FullscreenVideo(owner, view, callback)
    }

    fun hide(owner: WebView) {
        if (_fullscreen.value?.owner === owner) exit()
    }

    fun hide(session: FullscreenVideo) {
        if (_fullscreen.value === session) exit()
    }

    fun exit() {
        val session = _fullscreen.value ?: return
        // Clear before notifying Chromium: its callback can re-enter onHideCustomView.
        _fullscreen.value = null
        session.callback.onCustomViewHidden()
    }
}
