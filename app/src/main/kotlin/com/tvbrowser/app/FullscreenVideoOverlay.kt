package com.tvbrowser.app

import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.webview.FullscreenVideo
import com.tvbrowser.core.webview.WebVideoController

@Composable
internal fun FullscreenVideoOverlay(
    session: FullscreenVideo,
    controller: WebVideoController,
    cursor: CursorController,
    window: Window,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    BackHandler { controller.exit() }
    DisposableEffect(session) {
        val wasPointerActive = cursor.state.value.active
        val flags = window.attributes.flags
        val videoFlags = WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        window.addFlags(videoFlags)
        // Fullscreen player controls own remote input; the virtual webpage pointer is suspended.
        cursor.setActive(false)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) controller.hide(session)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            window.clearFlags(videoFlags and flags.inv())
            controller.hide(session)
            cursor.setActive(wasPointerActive)
            if (controller.fullscreen.value == null) session.owner.requestFocus()
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).testTag("browser-fullscreen-video")) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (session.view.parent as? ViewGroup)?.removeView(session.view)
                session.view.apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    isFocusableInTouchMode = true
                    requestFocus()
                    post { if (controller.fullscreen.value === session) requestFocus() }
                }
            },
        )
    }
}
