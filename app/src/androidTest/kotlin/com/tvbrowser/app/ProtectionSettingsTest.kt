package com.tvbrowser.app

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProtectionSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun defaultProtectionPersistsAndDoneReturnsFocusToSettings() {
        val settings = SettingsRepository(app)
        val original = runBlocking { settings.adblockEnabled.first() }
        try {
            openProtectionWithRemote()
            assertDefaultState(original)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            runBlocking { withTimeout(5_000) { settings.adblockEnabled.first { it == !original } } }
            compose.waitForIdle()
            assertDefaultState(!original)
            assertEquals(!original, runBlocking { SettingsRepository(app).adblockEnabled.first() })

            compose.onNodeWithTag("protection-done")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.onNodeWithTag("protection-settings-dialog").assertDoesNotExist()
            compose.onNodeWithTag("settings-protection").assertIsFocused()
        } finally {
            runBlocking { settings.set(PrefKeys.AdblockEnabled, original) }
        }
    }

    @Test fun filterListsAndUpdateAreRemoteReachableWithoutRequiringTheNetwork() {
        openProtectionWithRemote()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("protection-update").assertIsFocused().assertHasClickAction()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        // The control remains mounted/focusable whether an update starts, fails, or is already running.
        compose.onNodeWithTag("protection-update").assertIsFocused().assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("protection-list-Ads").assertIsFocused().assertIsDisplayed()
            .assertTextContains("EasyList")
        key(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithTag("protection-list-Trackers").assertIsFocused().assertIsDisplayed()
            .assertTextContains("EasyPrivacy")

        key(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("protection-settings-dialog").assertDoesNotExist()
        compose.onNodeWithTag("settings-protection").assertIsFocused()
    }

    @Test fun resettingAnExactHostKeepsDesktopModeAndMovesFocusToDone() {
        val settings = SettingsRepository(app)
        val original = runBlocking { settings.adblockEnabled.first() }
        val host = "pebble-protection-fixture-${UUID.randomUUID()}.invalid"
        val url = "https://$host/article"
        try {
            runBlocking {
                settings.set(PrefKeys.AdblockEnabled, true)
                settings.setSiteDesktop(url, true)
                settings.setSiteAdblock(url, false)
                withTimeout(5_000) {
                    settings.pagePreferences(url).first { it.desktopUa && it.adblockOverridden && !it.adblockEnabled }
                }
            }
            openProtectionWithRemote()
            val rowTag = "protection-override-$host"
            compose.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("protection-settings-dialog")))
                .performScrollToNode(hasTestTag(rowTag))
            compose.onNodeWithTag(rowTag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            compose.onNodeWithTag(rowTag).assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            val reset = runBlocking {
                withTimeout(5_000) { settings.pagePreferences(url).first { !it.adblockOverridden } }
            }
            assertTrue("Protection reset must retain the saved desktop mode", reset.desktopUa)
            assertTrue("The desktop override still marks this site overridden", reset.overridden)
            assertTrue("Protection returns to the global default", reset.adblockEnabled)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(rowTag).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("protection-done").assertIsFocused()
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            compose.onNodeWithTag("settings-protection").assertIsFocused()
        } finally {
            runBlocking {
                settings.resetSite(url)
                settings.set(PrefKeys.AdblockEnabled, original)
            }
        }
    }

    private fun openProtectionWithRemote() {
        compose.onNodeWithTag("top-nav-home").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(KeyEvent.KEYCODE_DPAD_LEFT)
        compose.onNodeWithTag("top-nav-settings").assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("top-nav-settings").assertIsFocused()
        // System theme -> Light -> Dark -> Desktop site -> Protection.
        repeat(5) { key(KeyEvent.KEYCODE_DPAD_DOWN) }
        compose.onNodeWithTag("settings-protection").assertIsFocused().assertHasClickAction()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("protection-settings-dialog").assertIsDisplayed()
        compose.onNodeWithTag("protection-default").assertIsFocused()
    }

    private fun assertDefaultState(enabled: Boolean) {
        if (enabled) compose.onNodeWithTag("protection-default").assertIsOn()
        else compose.onNodeWithTag("protection-default").assertIsOff()
    }

    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }
}
