package com.tvbrowser.app

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.db.DbModule
import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BrowserProtectionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val shield = hasContentDescription("Ad protection on") or hasContentDescription("Ad protection off or paused")

    @Test fun savedProtectionResetPreservesDesktopAndReturnsFocusToPersistentAction() = withFixture { settings, url, host ->
        openProtection(host)
        activate("browser-protection-site")
        runBlocking {
            withTimeout(5_000) {
                settings.pagePreferences(url).first { it.adblockOverridden && !it.adblockEnabled }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("browser-protection-reset").fetchSemanticsNodes().isNotEmpty()
        }
        activate("browser-protection-reset")
        val reset = runBlocking {
            withTimeout(5_000) { settings.pagePreferences(url).first { !it.adblockOverridden } }
        }
        assertTrue("Resetting protection keeps the saved desktop mode", reset.desktopUa)
        assertTrue("The desktop override remains saved", reset.overridden)
        assertTrue("Protection inherits the enabled global default", reset.adblockEnabled)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("browser-protection-reset").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("browser-protection-site").assertIsFocused()
        activate("browser-protection-done")
        compose.onNode(shield).assertIsFocused()
    }

    @Test fun visitPauseCanResumeWithoutCreatingASavedWebsiteOverride() = withFixture { settings, url, host ->
        openProtection(host)
        activate("browser-protection-pause")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Paused for this visit").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Resume protection").assertExists()
        assertFalse(runBlocking { settings.pagePreferences(url).first().adblockOverridden })
        assertTrue(runBlocking { settings.siteAdblockOverrides.first().none { it.host == host } })
        activate("browser-protection-done")
        compose.onNode(hasContentDescription("Ad protection off or paused")).assertIsFocused()

        openProtection(host)
        activate("browser-protection-pause")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Protection on").fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(runBlocking { settings.pagePreferences(url).first().adblockOverridden })
        activate("browser-protection-done")
        compose.onNode(hasContentDescription("Ad protection on")).assertIsFocused()
    }

    private fun openProtection(host: String) {
        if (compose.onAllNodesWithTag("browser-address").fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithTag("top-nav-browser").performClick()
        }
        val boundAddress = hasTestTag("browser-address") and hasText(host, substring = true)
        val readyShield = shield and hasClickAction()
        compose.waitUntil(10_000) {
            compose.onAllNodes(boundAddress).fetchSemanticsNodes().size == 1 &&
                compose.onAllNodes(readyShield).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("browser-address").assertTextContains(host, substring = true)
        // Browse starts in pointer mode; Menu performs the remote's real handoff to native controls.
        key(KeyEvent.KEYCODE_MENU)
        compose.onNode(readyShield).assertHasClickAction()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNode(readyShield).assertIsFocused()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        compose.onNodeWithTag("browser-protection-dialog").assertExists()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("browser-protection-site").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(host).assertExists()
        compose.onNodeWithTag("browser-protection-site").assertIsFocused()
    }

    private fun activate(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (tag != "browser-protection-done") node.performScrollTo()
        node.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
    }

    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    private fun withFixture(block: (SettingsRepository, String, String) -> Unit) {
        val settings = SettingsRepository(app)
        val originalBlocking = runBlocking { settings.adblockEnabled.first() }
        val host = "pebble-protection-fixture-${UUID.randomUUID()}.invalid"
        val url = "https://$host/"
        val id = UUID.randomUUID().toString()
        val database = DbModule.database(app)
        try {
            runBlocking {
                settings.set(PrefKeys.AdblockEnabled, true)
                settings.setSiteDesktop(url, true)
                database.tabDao().upsert(TabEntity(id = id, url = url, title = "Protection fixture", lastActiveAt = Long.MAX_VALUE))
                withTimeout(5_000) { settings.pagePreferences(url).first { it.desktopUa && it.adblockEnabled } }
            }
            block(settings, url, host)
        } finally {
            runCatching {
                if (compose.onAllNodesWithTag("browser-protection-done").fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag("browser-protection-done").performClick()
                }
                compose.onNodeWithTag("top-nav-home").performClick()
                compose.waitForIdle()
            }
            runBlocking {
                settings.resetSite(url)
                settings.set(PrefKeys.AdblockEnabled, originalBlocking)
                database.tabDao().delete(id)
            }
            database.close()
        }
    }
}
