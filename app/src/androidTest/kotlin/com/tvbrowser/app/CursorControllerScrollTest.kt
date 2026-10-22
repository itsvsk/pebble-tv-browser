package com.tvbrowser.app

import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.cursor.CursorController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CursorControllerScrollTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun repeatedScrollKeysAddToThePendingAnimationTarget() {
        val controller = CursorController(animationsEnabled = true)
        val view = ScrollableView()
        instrumentation.runOnMainSync {
            controller.attachTarget(view)
            view.layout(0, 0, 100, 100)
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
        val view = ScrollableView()
        instrumentation.runOnMainSync {
            controller.attachTarget(view)
            view.layout(0, 0, 100, 100)
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

    private inner class ScrollableView : View(instrumentation.targetContext) {
        override fun canScrollVertically(direction: Int): Boolean =
            when {
                direction > 0 -> scrollY < 1_000
                direction < 0 -> scrollY > 0
                else -> false
            }
    }

    private fun waitForScrollY(view: View, expected: Int) {
        var actual = 0
        repeat(40) {
            instrumentation.runOnMainSync { actual = view.scrollY }
            if (actual == expected) return
            SystemClock.sleep(50)
        }
        assertEquals(expected, actual)
    }
}
