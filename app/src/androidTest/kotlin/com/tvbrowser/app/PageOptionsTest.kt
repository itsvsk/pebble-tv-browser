package com.tvbrowser.app

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageOptionsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun pageMenuSupportsRemoteFocusMuteAndBackgroundChoice() {
        compose.onNodeWithTag("top-nav-browser").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("browser-address").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Page options")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithContentDescription("Page options").performClick()
        compose.onNodeWithTag("page-mute").assertIsFocused().performClick()
        compose.onNodeWithTag("page-mute").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("page-background").assertIsFocused().performClick()
        compose.onNodeWithTag("page-menu-close").performClick()
        compose.onNodeWithTag("page-mute").assertDoesNotExist()
        compose.onNodeWithContentDescription("Page options").assertIsFocused()
        // Restore the two session-only choices for subsequent test classes.
        compose.onNodeWithContentDescription("Page options").performClick()
        compose.onNodeWithTag("page-mute").performClick()
        compose.onNodeWithTag("page-background").performClick()
        compose.onNodeWithTag("page-menu-close").performClick()
    }
}
