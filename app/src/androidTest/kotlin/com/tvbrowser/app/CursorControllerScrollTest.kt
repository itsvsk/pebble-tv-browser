package com.tvbrowser.app

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.cursor.CursorController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CursorControllerScrollTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun repeatedScrollKeysAddToThePendingAnimationTarget() {
        val controller = CursorController(animationsEnabled = true)
        val view = attachScrollableView(controller)
        instrumentation.runOnMainSync {
            controller.setPreferredActive(true)
            repeat(5) { sendDownKey(controller, it) }
        }

        try {
            waitForScrollY(view, expected = 192)
        } finally {
            instrumentation.runOnMainSync { controller.detachTarget(view) }
        }
    }

    @Test
    fun scrollKeysMoveImmediatelyWhenAnimationsAreDisabled() {
        val controller = CursorController(animationsEnabled = false)
        val view = attachScrollableView(controller)
        instrumentation.runOnMainSync {
            controller.setPreferredActive(true)
            repeat(5) { sendDownKey(controller, it) }
            try {
                assertEquals(192, view.scrollY)
            } finally {
                controller.detachTarget(view)
            }
        }
    }

    private fun sendDownKey(controller: CursorController, repeatCount: Int) {
        controller.handleKeyEvent(
            KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, repeatCount),
        )
    }

    private fun attachScrollableView(controller: CursorController): ScrollableView {
        lateinit var view: ScrollableView
        activity.scenario.onActivity { host ->
            view = ScrollableView(host).apply { keepScreenOn = true }
            host.setContentView(view, ViewGroup.LayoutParams(100, 100))
            controller.attachTarget(view)
            view.layout(0, 0, 100, 100)
            assertTrue("Animation target must be attached to a visible activity", view.isAttachedToWindow)
        }
        instrumentation.waitForIdleSync()
        return view
    }

    private class ScrollableView(context: Context) : View(context) {
        override fun canScrollVertically(direction: Int): Boolean =
            when {
                direction > 0 -> scrollY < 1_000
                direction < 0 -> scrollY > 0
                else -> false
            }
    }

    private fun waitForScrollY(view: View, expected: Int) {
        var actual = 0
        // Verify eventual completion, not frame timing on a software-rendered CI emulator.
        val deadline = SystemClock.uptimeMillis() + 10_000L
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync { actual = view.scrollY }
            if (actual == expected) return
            SystemClock.sleep(50)
        }
        assertEquals(expected, actual)
    }
}
