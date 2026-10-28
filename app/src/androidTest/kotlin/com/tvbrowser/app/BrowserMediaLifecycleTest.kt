package com.tvbrowser.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Real Browse-to-Tabs host disposal, rather than calling the policy controller directly. */
@RunWith(AndroidJUnit4::class)
class BrowserMediaLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun leavingBrowsePausesAudioAndReturningDoesNotRestartIt() {
        val browser = fixture()
        background(false)
        play(browser)
        compose.onNodeWithTag("top-nav-tabs").performClick()
        compose.waitUntil(10_000) { state(browser).optBoolean("paused") }
        compose.onNodeWithTag("top-nav-browser").performClick()
        compose.waitUntil(10_000) { hostAttached(browser) }
        assertTrue(state(browser).getBoolean("paused"))
    }

    @Test fun optedInAudioAdvancesAfterTheBrowserHostIsReleased() {
        val browser = fixture()
        try {
            background(true)
            play(browser)
            val before = state(browser).getDouble("time")
            compose.onNodeWithTag("top-nav-tabs").performClick()
            try {
                compose.waitUntil(10_000) {
                    val sample = state(browser)
                    !hostAttached(browser) && !sample.optBoolean("paused", true) &&
                        kotlin.math.abs(sample.optDouble("time") - before) > 0.15
                }
            } catch (failure: Throwable) {
                throw AssertionError("Host playback before=$before state=${state(browser)} attached=${hostAttached(browser)}", failure)
            }
        } finally {
            // Stop the fixture even if navigation cleanup itself fails, avoiding audio-focus
            // interference with the next instrumentation test.
            evaluate(browser, "document.getElementById('media').pause(); true")
            compose.onNodeWithTag("top-nav-browser").performClick()
            background(false)
        }
    }

    private fun fixture(): WebView {
        compose.onNodeWithTag("top-nav-browser").performClick()
        var browser: WebView? = null
        compose.waitUntil(10_000) { compose.runOnUiThread { browser = find(compose.activity.window.decorView) }; browser != null }
        val view = checkNotNull(browser)
        compose.runOnUiThread { view.loadDataWithBaseURL("about:blank", silentAudioFixture(), "text/html", "UTF-8", null) }
        compose.waitUntil(10_000) { state(view).optBoolean("ready") }
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        return view
    }
    private fun background(enabled: Boolean) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Page options").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithContentDescription("Page options").performClick()
        compose.onNodeWithTag("page-background").assertIsDisplayed()
        val opposite = "Background playback: ${if (enabled) "Off" else "Allowed"}"
        if (compose.onAllNodesWithText(opposite).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("page-background").performClick()
        }
        compose.onNodeWithTag("page-menu-close").performClick()
    }
    private fun play(browser: WebView) {
        evaluate(browser, "document.getElementById('media').play(); true")
        try {
            compose.waitUntil(10_000) { state(browser).let { !it.optBoolean("paused", true) && it.optDouble("time") > 0.1 } }
        } catch (failure: Throwable) {
            var gesture = false
            compose.runOnUiThread { gesture = browser.settings.mediaPlaybackRequiresUserGesture }
            throw AssertionError("Playback state=${state(browser)} gestureRequired=$gesture", failure)
        }
    }
    private fun state(browser: WebView): JSONObject {
        val result = evaluate(browser, """
            (function() { var v=document.getElementById('media'); return JSON.stringify({
            ready: document.readyState === 'complete' && !!v,
            suspended: document.__pebbleSuspendMedia, mutedPolicy: document.__pebbleMuteMedia,
            muted: !!v && v.muted,
            paused: !v || v.paused, time: v ? v.currentTime : 0 }); })()
        """.trimIndent())
        return if (result is String) JSONObject(result) else JSONObject()
    }
    private fun hostAttached(browser: WebView): Boolean {
        // Old WebViews can report document.visibilityState="visible" after native detach.
        var attached = false
        compose.runOnUiThread { attached = browser.parent != null && browser.isAttachedToWindow }
        return attached
    }
    private fun evaluate(browser: WebView, script: String): Any? {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        compose.runOnUiThread { browser.evaluateJavascript(script) { result.set(it); callback.countDown() } }
        assertTrue(callback.await(5, TimeUnit.SECONDS))
        return JSONTokener(result.get() ?: "null").nextValue()
    }
    private fun find(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
}
