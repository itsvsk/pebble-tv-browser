package com.tvbrowser.app

import android.view.KeyEvent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopNavFocusTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun leavingDownloadsForSettingsRemovesMovingHighlightWhileHomeIsActive() {
        compose.onNodeWithTag("top-nav-focus-highlight").assertExists()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(4) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT) }
        compose.onNodeWithTag("top-nav-focus-highlight").assertExists()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("top-nav-focus-highlight").assertDoesNotExist()
    }
}
