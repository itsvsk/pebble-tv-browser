package com.tvbrowser.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.FocusFinder
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.tvbrowser.app.navigation.AppNavHost
import com.tvbrowser.core.design.DarkPebblePalette
import com.tvbrowser.core.design.LightPebblePalette
import com.tvbrowser.core.design.PebbleTheme
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.ThemePreference
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.cursor.CursorMode
import com.tvbrowser.core.webview.WebVideoController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cursorController: CursorController
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var videoController: WebVideoController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cursorMode by cursorController.mode.collectAsState(initial = CursorMode())
            val fullscreen by videoController.fullscreen.collectAsState()
            val themePreference by settingsRepository.theme.collectAsState(initial = ThemePreference.System)
            val darkTheme = when (themePreference) {
                ThemePreference.System -> isSystemInDarkTheme()
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
            }
            LaunchedEffect(darkTheme) {
                val background = (if (darkTheme) DarkPebblePalette else LightPebblePalette).Bg.toArgb()
                window.setBackgroundDrawable(ColorDrawable(background))
                window.statusBarColor = background
                window.navigationBarColor = background
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            PebbleTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize()) {
                    AppNavHost(
                        cursorMode = cursorMode,
                        onSetCursorActive = cursorController::setActive,
                        onSetCursorPreferred = cursorController::setPreferredActive,
                    )
                    fullscreen?.let { session ->
                        key(session) {
                            FullscreenVideoOverlay(session, videoController, cursorController, window)
                        }
                    }
                }
            }
        }
    }

    // Activity's public callback is inherited through AndroidX Core's restricted base class.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        videoController.fullscreen.value?.let { session ->
            val direction = when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
                KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
                else -> null
            }
            val select = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER
            if (direction != null || select) {
                val handled = session.view.dispatchKeyEvent(event)
                if (!handled && direction != null && event.action == KeyEvent.ACTION_DOWN) {
                    (session.view as? ViewGroup)?.let { group ->
                        FocusFinder.getInstance().findNextFocus(group, group.findFocus(), direction)?.requestFocus(direction)
                    }
                } else if (!handled && select && event.action == KeyEvent.ACTION_UP) {
                    session.view.findFocus()?.performClick()
                }
                // Native player controls may leave an edge key unhandled. Keep that key
                // inside the player instead of moving focus to navigation behind it.
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (event.keyCode != KeyEvent.KEYCODE_BACK && cursorController.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
