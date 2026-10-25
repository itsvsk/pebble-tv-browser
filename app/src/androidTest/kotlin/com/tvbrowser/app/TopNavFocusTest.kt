package com.tvbrowser.app

import android.view.KeyEvent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
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

    @Test
    fun navigationWrapsAtBothEndsAndBackReturnsToActiveHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.onNodeWithTag("top-nav-home").assertIsFocused().assertHasClickAction()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("top-nav-settings").assertIsFocused().assertHasClickAction()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("top-nav-home").assertIsFocused()

        repeat(4) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT) }
        compose.onNodeWithTag("top-nav-files").assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithTag("top-nav-settings").assertIsFocused()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("top-nav-home").assertIsFocused()
    }

    @Test
    fun tabsEnterThroughNewTabAndReturnThroughIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(2) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT) }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitForIdle()
        compose.onNodeWithTag("top-nav-tabs").assertIsFocused()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("tabs-new-tab").assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("tabs-list-first").assertIsFocused()

        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithTag("tabs-new-tab").assertIsFocused()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP)
        compose.onNodeWithTag("top-nav-tabs").assertIsFocused()
    }

    @Test
    fun addressEditorOpensOnlyAfterSelect() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("browser-address").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("browser-address-editor", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("browser-address").assertHasClickAction().performClick()
        compose.onNodeWithTag("browser-address-editor", useUnmergedTree = true).assertIsFocused()
    }
}
