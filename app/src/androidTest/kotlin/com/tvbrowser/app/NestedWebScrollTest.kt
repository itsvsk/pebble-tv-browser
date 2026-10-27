package com.tvbrowser.app

import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.cursor.CursorController
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class NestedWebScrollTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun holdingDownAtPointerEdgeScrollsTheNestedFeedWithoutMovingTheDocument() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachFixture(controller)
        try {
            val baseline = awaitReady(browser, controller)
            var nativeCanScrollDown = true
            instrumentation.runOnMainSync { nativeCanScrollDown = browser.canScrollVertically(1) }
            assertFalse("Fixture must have no native document scroll: $baseline", nativeCanScrollDown)
            assertEquals(0, baseline.getInt("windowY"))
            assertTrue("Fixture must contain nested overflow", baseline.getInt("feedScrollHeight") > baseline.getInt("feedHeight"))

            instrumentation.runOnMainSync {
                controller.setPreferredActive(true)
                repeat(40) { repeatCount ->
                    controller.handleKeyEvent(
                        KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, repeatCount),
                    )
                }
            }
            val pointer = controller.state.value
            assertTrue("Pointer must reach the bottom edge before scrolling", pointer.y >= pointer.viewportH - 1f)
            var after = JSONObject()
            waitUntil("Nested feed did not scroll after repeated edge navigation; nativeCanScrollDown=$nativeCanScrollDown, before=$baseline") {
                after = evaluate(browser)
                after.optInt("feedY") > 0
            }
            assertEquals("Nested scrolling must not move the outer document: $after", 0, after.getInt("windowY"))
        } finally {
            closeFixture(browser, controller)
        }
    }

    @Test
    fun upScrollsTheNestedFeedBeforeHandingFocusToChromeAtItsTop() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachFixture(controller)
        val handoffs = AtomicInteger()
        try {
            awaitReady(browser, controller)
            val seeded = evaluate(browser, "feed.scrollTop = 120;")
            assertTrue("Fixture must start away from the nested top", seeded.getInt("feedY") > 0)
            awaitVisualUpdate(browser)
            instrumentation.runOnMainSync {
                assertFalse("Outer document must not scroll upward", browser.canScrollVertically(-1))
                controller.setControlsFocusHandler { handoffs.incrementAndGet() }
                controller.setPreferredActive(true)
            }
            movePointerToEdge(controller, KeyEvent.KEYCODE_DPAD_UP)
            assertEquals("Moving the pointer toward the top must not hand off focus", 0, handoffs.get())

            sendKey(controller, KeyEvent.KEYCODE_DPAD_UP)
            var after = JSONObject()
            waitUntil("Up at the pointer edge did not scroll the nested feed") {
                after = evaluate(browser)
                after.getInt("feedY") < seeded.getInt("feedY") || handoffs.get() > 0
            }
            assertEquals("Chrome must not capture Up while the nested feed can still scroll", 0, handoffs.get())
            assertTrue("The first Up should move toward the top", after.getInt("feedY") < seeded.getInt("feedY"))

            var steps = 0
            while (after.getInt("feedY") > 0 && steps++ < 40) {
                val previousY = after.getInt("feedY")
                sendKey(controller, KeyEvent.KEYCODE_DPAD_UP)
                waitUntil("Nested Up made no progress from $previousY") {
                    after = evaluate(browser)
                    after.getInt("feedY") < previousY || handoffs.get() > 0
                }
                assertEquals("Chrome handoff must wait until the feed has reached its top", 0, handoffs.get())
            }
            assertEquals("Repeated Up must reach the nested top", 0, after.getInt("feedY"))
            assertEquals(0, after.getInt("windowY"))
            sendKey(controller, KeyEvent.KEYCODE_DPAD_UP)
            waitUntil("Another Up at the nested top must reveal chrome") { handoffs.get() > 0 }
            assertEquals("A single Up must request chrome exactly once", 1, handoffs.get())
            assertFalse("Pointer input must yield to chrome", controller.state.value.active)
        } finally {
            closeFixture(browser, controller)
        }
    }

    @Test
    fun horizontalPointerEdgesScrollTheNestedPanelInBothDirections() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachFixture(controller, horizontal = true)
        try {
            val baseline = awaitReady(browser, controller)
            instrumentation.runOnMainSync {
                assertFalse("Outer document must not scroll horizontally", browser.canScrollHorizontally(1))
                controller.setPreferredActive(true)
            }
            assertTrue("Fixture must contain horizontal inner overflow",
                baseline.getInt("feedScrollWidth") > baseline.getInt("feedWidth"))
            movePointerToEdge(controller, KeyEvent.KEYCODE_DPAD_RIGHT)
            sendKey(controller, KeyEvent.KEYCODE_DPAD_RIGHT)
            var afterRight = JSONObject()
            waitUntil("Right edge navigation did not scroll the inner panel") {
                afterRight = evaluate(browser)
                afterRight.getInt("feedX") > 0
            }
            assertEquals(0, afterRight.getInt("windowX"))
            movePointerToEdge(controller, KeyEvent.KEYCODE_DPAD_LEFT)
            sendKey(controller, KeyEvent.KEYCODE_DPAD_LEFT)
            var afterLeft = JSONObject()
            waitUntil("Left edge navigation did not reverse the inner panel") {
                afterLeft = evaluate(browser)
                afterLeft.getInt("feedX") < afterRight.getInt("feedX")
            }
            assertEquals("Horizontal nested scrolling must leave the document fixed", 0, afterLeft.getInt("windowX"))
        } finally {
            closeFixture(browser, controller)
        }
    }

    private fun attachFixture(controller: CursorController, horizontal: Boolean = false): WebView {
        lateinit var browser: WebView
        activity.scenario.onActivity { host ->
            browser = WebView(host).apply {
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                keepScreenOn = true
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            host.setContentView(browser)
            controller.attachTarget(browser)
            browser.loadDataWithBaseURL(
                "https://pebble.invalid/nested-feed-fixture",
                """
                    <!doctype html><html><head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        html, body { margin: 0; width: 100%; height: 100%; overflow: hidden; }
                        #feed { width: 100%; height: 100%; overflow: auto; }
                        #content { ${if (horizontal) "width: 4000px; height: 100%;" else "height: 4000px;"}
                            background: linear-gradient(#cdf, #528); }
                    </style></head><body><main id="feed"><section id="content">
                    Offline nested social feed fixture
                    </section></main></body></html>
                """.trimIndent(),
                "text/html", "UTF-8", null,
            )
        }
        return browser
    }

    private fun awaitReady(browser: WebView, controller: CursorController): JSONObject {
        var baseline = JSONObject()
        waitUntil("The nested feed fixture did not finish loading") {
            baseline = evaluate(browser)
            baseline.optBoolean("ready") && baseline.optDouble("feedHeight") > 100
        }
        waitUntil("The pointer viewport was not initialized") { controller.state.value.viewportH > 0 }
        awaitVisualUpdate(browser)
        return baseline
    }

    private fun awaitVisualUpdate(browser: WebView) {
        val drawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            browser.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    browser.postOnAnimation { browser.postOnAnimation { drawn.countDown() } }
                }
            })
        }
        assertTrue("The fixture must be rendered before injecting wheel input", drawn.await(5, TimeUnit.SECONDS))
    }

    private fun closeFixture(browser: WebView, controller: CursorController) {
        instrumentation.runOnMainSync {
            controller.detachTarget(browser)
            (browser.parent as? ViewGroup)?.removeView(browser)
            browser.destroy()
        }
    }

    private fun movePointerToEdge(controller: CursorController, keyCode: Int) {
        instrumentation.runOnMainSync {
            repeat(100) {
                val state = controller.state.value
                val atEdge = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> state.y <= 0
                    KeyEvent.KEYCODE_DPAD_LEFT -> state.x <= 0
                    else -> state.x >= state.viewportW
                }
                if (atEdge) return@runOnMainSync
                controller.handleKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 3))
            }
        }
    }

    private fun sendKey(controller: CursorController, keyCode: Int) {
        instrumentation.runOnMainSync {
            controller.handleKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 3))
            controller.handleKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    private fun evaluate(browser: WebView, action: String = ""): JSONObject {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        instrumentation.runOnMainSync {
            browser.evaluateJavascript(
                """
                    (function() {
                        var feed = document.getElementById('feed');
                        $action
                        return JSON.stringify({
                            ready: document.readyState === 'complete' && !!feed,
                            windowX: window.scrollX || 0,
                            windowY: window.scrollY || 0,
                            feedX: feed ? feed.scrollLeft : 0,
                            feedY: feed ? feed.scrollTop : 0,
                            feedWidth: feed ? feed.clientWidth : 0,
                            feedHeight: feed ? feed.clientHeight : 0,
                            feedScrollWidth: feed ? feed.scrollWidth : 0,
                            feedScrollHeight: feed ? feed.scrollHeight : 0
                        });
                    })()
                """.trimIndent(),
            ) { value -> result.set(value); callback.countDown() }
        }
        assertTrue("WebView JavaScript callback timed out", callback.await(5, TimeUnit.SECONDS))
        return JSONObject(JSONTokener(result.get()).nextValue() as String)
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue(message, condition())
    }
}
