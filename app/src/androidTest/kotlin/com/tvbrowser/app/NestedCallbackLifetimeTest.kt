package com.tvbrowser.app

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.cursor.CursorController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class NestedCallbackLifetimeTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun staleUpProbeCannotHandFocusToChromeAfterPointerModeReactivates() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachBrowser(controller)
        try {
            instrumentation.runOnMainSync {
                var handoffs = 0
                controller.setControlsFocusHandler { handoffs++ }
                controller.setPreferredActive(true)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_UP)
                assertEquals(0f, controller.state.value.y, 0f)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_UP)
                assertEquals(1, browser.pendingCount)

                controller.setActive(false)
                controller.setActive(true)
                browser.completeNext("null")

                assertEquals("An old probe must not steal focus from a reactivated pointer", 0, handoffs)
                assertTrue(controller.state.value.active)
            }
        } finally {
            closeBrowser(browser, controller)
        }
    }

    @Test
    fun staleWheelProbeCannotScrollAfterTheSameWebViewIsReattached() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachBrowser(controller)
        try {
            instrumentation.runOnMainSync {
                controller.setPreferredActive(true)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_RIGHT)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_RIGHT)
                assertEquals(1, browser.pendingCount)

                controller.detachTarget(browser)
                controller.attachTarget(browser)
                browser.completeNext("[50,50]")

                assertEquals("An old callback must not inject scrolling into a new attachment", 0, browser.wheelEvents)
                assertTrue(controller.state.value.active)
            }
        } finally {
            closeBrowser(browser, controller)
        }
    }

    @Test
    fun heldEdgeRepeatsKeepTheirCallbacksWhileTheContextIsUnchanged() {
        val controller = CursorController(animationsEnabled = false)
        val browser = attachBrowser(controller)
        try {
            instrumentation.runOnMainSync {
                controller.setPreferredActive(true)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_DOWN)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_DOWN)
                sendDown(controller, KeyEvent.KEYCODE_DPAD_DOWN)
                assertEquals(2, browser.pendingCount)

                // Reasserting the same mode must not invalidate legitimate held-key work.
                controller.setActive(true)
                browser.completeNext("[50,50]")
                browser.completeNext("[50,50]")

                assertEquals("Held repeats must retain both scrolling steps", 2, browser.wheelEvents)
            }
        } finally {
            closeBrowser(browser, controller)
        }
    }

    private fun attachBrowser(controller: CursorController): DeferredWebView {
        lateinit var browser: DeferredWebView
        activity.scenario.onActivity { host ->
            browser = DeferredWebView(host)
            host.setContentView(browser, ViewGroup.LayoutParams(100, 100))
            browser.layout(0, 0, 100, 100)
            controller.attachTarget(browser)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals(100, controller.state.value.viewportH)
            assertEquals(100, controller.state.value.viewportW)
        }
        return browser
    }

    private fun closeBrowser(browser: DeferredWebView, controller: CursorController) {
        instrumentation.runOnMainSync {
            controller.detachTarget(browser)
            (browser.parent as? ViewGroup)?.removeView(browser)
            browser.destroy()
        }
    }

    private fun sendDown(controller: CursorController, keyCode: Int) {
        controller.handleKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 3))
    }

    /** Holds only the asynchronous bridge boundary; the real controller is exercised unchanged. */
    private class DeferredWebView(context: Context) : WebView(context) {
        private val callbacks = ArrayDeque<ValueCallback<String>>()
        val pendingCount: Int get() = callbacks.size
        var wheelEvents = 0
            private set

        override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
            callbacks.addLast(checkNotNull(resultCallback))
        }

        override fun canScrollVertically(direction: Int): Boolean = false
        override fun canScrollHorizontally(direction: Int): Boolean = false

        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_SCROLL) wheelEvents++
            return true
        }

        fun completeNext(result: String) {
            callbacks.removeFirst().onReceiveValue(result)
        }
    }
}
