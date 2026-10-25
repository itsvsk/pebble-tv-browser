package com.tvbrowser.app

import android.os.Bundle
import android.view.KeyEvent
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import com.tvbrowser.app.navigation.AppNavHost
import com.tvbrowser.core.design.DarkPebblePalette
import com.tvbrowser.core.design.LightPebblePalette
import com.tvbrowser.core.design.PebbleTheme
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.ThemePreference
import com.tvbrowser.core.cursor.CursorController
import com.tvbrowser.core.cursor.CursorMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cursorController: CursorController
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cursorMode by cursorController.mode.collectAsState(initial = CursorMode())
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
                AppNavHost(
                    cursorMode = cursorMode,
                    onSetCursorActive = cursorController::setActive,
                    onSetCursorPreferred = cursorController::setPreferredActive,
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK && cursorController.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
