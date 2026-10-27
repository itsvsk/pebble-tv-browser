package com.tvbrowser.app

import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.room.Room
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.db.AppDatabase
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabManager
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Exercises real HTML media without an external service, permission prompt or audible sample. */
@RunWith(AndroidJUnit4::class)
class TabMediaPolicyTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun leavingTheTabPausesMediaAndReturningDoesNotRestartIt() = withMedia { manager, browser, id ->
        startPlayback(browser)
        instrumentation.runOnMainSync { manager.setTabForeground(id, false) }
        await("Leaving a tab must pause its media") { metrics(browser).optBoolean("paused") }
        instrumentation.runOnMainSync { manager.setTabForeground(id, true) }
        // Synchronize with the policy script and a later event-loop turn; a paused state
        // immediately after onResume alone would miss a queued automatic play().
        evaluate(browser, "window.returnedToMediaTab = false; setTimeout(function() { window.returnedToMediaTab = true; }, 150); true")
        await("The resumed document must process a later event-loop turn") {
            evaluate(browser, "window.returnedToMediaTab") == true
        }
        assertTrue("Returning to the tab must respect its paused state", metrics(browser).getBoolean("paused"))
    }

    @Test
    fun inactiveTabCannotRestartPlaybackFromALateAutoplayAttempt() = withMedia { manager, browser, id ->
        startPlayback(browser)
        instrumentation.runOnMainSync { manager.setTabForeground(id, false) }
        await("The inactive tab must pause first") { metrics(browser).optBoolean("paused") }
        evaluate(browser, """
            window.latePlaySettled = false;
            var attempt = document.getElementById('media').play();
            if (attempt && attempt.then) {
                attempt.then(function() { window.latePlaySettled = true; },
                    function() { window.latePlaySettled = true; });
            } else { window.latePlaySettled = true; }
            true;
        """.trimIndent())
        await("The late autoplay attempt must settle") { evaluate(browser, "window.latePlaySettled") == true }
        assertTrue("A page's later autoplay must not defeat the inactive-tab policy", metrics(browser).getBoolean("paused"))
    }

    @Test
    fun backgroundOptInKeepsPlaybackAdvancingUntilTheUserRevokesIt() = withMedia { manager, browser, id ->
        instrumentation.runOnMainSync { manager.setTabBackgroundPlayback(id, true) }
        startPlayback(browser)
        val before = metrics(browser).getDouble("time")
        instrumentation.runOnMainSync { manager.setTabForeground(id, false) }
        await("Opted-in background playback must continue advancing") {
            val state = metrics(browser)
            !state.optBoolean("paused", true) && state.optDouble("time") > before + 0.15
        }
        instrumentation.runOnMainSync { manager.setTabBackgroundPlayback(id, false) }
        await("Revoking background playback must pause the inactive tab") { metrics(browser).optBoolean("paused") }
    }

    @Test
    fun siteVolumeChangesCannotOverrideTheTabsFallbackMute() = withMedia { manager, browser, id ->
        startPlayback(browser)
        instrumentation.runOnMainSync { manager.setTabMuted(id, true) }
        await("The DOM fallback must mute the playing media") {
            evaluate(browser, "document.getElementById('media').muted") == true
        }
        evaluate(browser, """
            window.volumeProbeSettled = false;
            var media = document.getElementById('media');
            media.addEventListener('volumechange', function settled() {
                media.removeEventListener('volumechange', settled);
                setTimeout(function() { window.volumeProbeSettled = true; }, 50);
            });
            media.muted = false;
            true;
        """.trimIndent())
        await("The site's volume change must be processed") { evaluate(browser, "window.volumeProbeSettled") == true }
        assertTrue("The tab mute must survive a site changing volume without another play event",
            evaluate(browser, "document.getElementById('media').muted") == true)
        instrumentation.runOnMainSync { manager.setTabMuted(id, false) }
        await("Explicit unmute must not be trapped by the volume-change guard") {
            evaluate(browser, "document.getElementById('media').muted") == false
        }
    }

    @Test
    fun explicitMuteSurvivesForegroundChangesAndUnmuteRestoresTheUserChoice() = withMedia { manager, browser, id ->
        startPlayback(browser)
        instrumentation.runOnMainSync {
            manager.setTabMuted(id, true)
            assertTrue(manager.mediaState(id).muted)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
                assertTrue("Explicit mute must reach the WebView engine", WebViewCompat.isAudioMuted(browser))
            }
            manager.setTabForeground(id, false)
            manager.setTabForeground(id, true)
            assertTrue("Lifecycle changes must preserve the user's mute choice", manager.mediaState(id).muted)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
                assertTrue(WebViewCompat.isAudioMuted(browser))
            }
            manager.setTabMuted(id, false)
            assertFalse(manager.mediaState(id).muted)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
                assertFalse("An active explicitly unmuted tab must be unmuted in the engine", WebViewCompat.isAudioMuted(browser))
            }
        }
    }

    private fun withMedia(check: (TabManager, WebView, String) -> Unit) {
        val context = instrumentation.targetContext.applicationContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        lateinit var manager: TabManager
        lateinit var browser: WebView
        val id = "media-policy-${UUID.randomUUID()}"
        activity.scenario.onActivity { host ->
            manager = TabManager(context, TabRepository(database.tabDao()), AdBlockInterceptor(context), host.videoController, com.tvbrowser.core.common.SettingsRepository(context))
            browser = manager.getOrCreate(id)
            val root = FrameLayout(host)
            (browser.parent as? ViewGroup)?.removeView(browser)
            root.addView(browser, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            host.setContentView(root)
            manager.setTabForeground(id, true)
            browser.loadDataWithBaseURL("https://pebble.invalid/media-policy-fixture", silentAudioFixture(), "text/html", "UTF-8", null)
        }
        try {
            await("The offline media fixture did not finish loading") { metrics(browser).optBoolean("ready") }
            check(manager, browser, id)
        } finally {
            instrumentation.runOnMainSync {
                (browser.parent as? ViewGroup)?.removeView(browser)
                manager.release(id)
            }
            database.close()
        }
    }

    private fun startPlayback(browser: WebView) {
        evaluate(browser, "document.getElementById('media').play(); true")
        await("The silent fixture must actually play before checking pause policy") {
            val state = metrics(browser)
            !state.optBoolean("paused", true) && state.optDouble("time") > 0.1
        }
    }

    private fun metrics(browser: WebView): JSONObject {
        val encoded = evaluate(browser, """
            (function() {
                var media = document.getElementById('media');
                return JSON.stringify({ready: document.readyState === 'complete' && !!media,
                    paused: media ? media.paused : true, time: media ? media.currentTime : 0});
            })()
        """.trimIndent())
        return if (encoded is String) JSONObject(encoded) else JSONObject()
    }

    private fun evaluate(browser: WebView, script: String): Any? {
        val value = AtomicReference<String>()
        val callback = CountDownLatch(1)
        instrumentation.runOnMainSync { browser.evaluateJavascript(script) { value.set(it); callback.countDown() } }
        assertTrue("WebView media probe timed out", callback.await(5, TimeUnit.SECONDS))
        return JSONTokener(value.get() ?: "null").nextValue()
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue(message, condition())
    }


}
