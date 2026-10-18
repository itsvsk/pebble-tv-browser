package com.tvbrowser.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tvbrowser.app.navigation.AppNavHost
import com.tvbrowser.core.design.PebbleTheme
import com.tvbrowser.core.cursor.CursorController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cursorController: CursorController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cursorState by cursorController.state.collectAsState()
            PebbleTheme {
                AppNavHost(
                    cursorState = cursorState,
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
