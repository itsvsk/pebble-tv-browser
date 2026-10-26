package com.tvbrowser.app

import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.webview.WebVideoController
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullscreenVideoTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun sameFrameReplacementAttachesTheNewNativePlayer() {
        lateinit var owner: WebView
        lateinit var first: FrameLayout
        lateinit var replacement: FrameLayout
        compose.runOnUiThread {
            owner = WebView(compose.activity)
            first = FrameLayout(compose.activity)
            replacement = FrameLayout(compose.activity)
            compose.activity.videoController.show(owner, first, WebChromeClient.CustomViewCallback {})
        }
        try {
            compose.onNodeWithTag("browser-fullscreen-video").assertIsDisplayed()
            compose.runOnUiThread {
                compose.activity.videoController.exit()
                compose.activity.videoController.show(owner, replacement, WebChromeClient.CustomViewCallback {})
            }
            compose.waitForIdle()
            compose.runOnUiThread {
                assertTrue("The replacement player must be attached", replacement.isAttachedToWindow)
                assertFalse("The prior player must be detached", first.isAttachedToWindow)
                assertSame(replacement, compose.activity.videoController.fullscreen.value!!.view)
            }
        } finally {
            compose.runOnUiThread { compose.activity.videoController.exit(); owner.destroy() }
        }
    }

    @Test fun backClosesFullscreenOnceAndRestoresWindowAndPointerState() {
        lateinit var owner: WebView
        var exits = 0
        var originalFlags = 0
        compose.runOnUiThread {
            owner = WebView(compose.activity)
            originalFlags = compose.activity.window.attributes.flags
            compose.activity.cursorController.setActive(true)
            compose.activity.videoController.show(owner, FrameLayout(compose.activity), WebChromeClient.CustomViewCallback { exits++ })
        }
        try {
            compose.onNodeWithTag("browser-fullscreen-video").assertIsDisplayed()
            compose.runOnUiThread {
                assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
                assertFalse(compose.activity.cursorController.state.value.active)
            }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_CENTER).forEach { key ->
                instrumentation.sendKeyDownUpSync(key)
            }
            compose.runOnUiThread {
                assertTrue("Remote focus must stay inside the fullscreen player",
                    compose.activity.videoController.fullscreen.value!!.view.hasFocus())
            }
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.onNodeWithTag("browser-fullscreen-video").assertDoesNotExist()
            compose.runOnUiThread {
                assertEquals(1, exits)
                assertEquals(originalFlags, compose.activity.window.attributes.flags)
                assertTrue(compose.activity.cursorController.state.value.active)
                compose.activity.videoController.exit()
                assertEquals(1, exits)
            }
        } finally {
            compose.runOnUiThread { compose.activity.videoController.exit(); owner.destroy() }
        }
    }

    @Test fun staleSessionCannotCloseItsReplacementAndDuplicateRequestsAreRejected() {
        compose.runOnUiThread {
            val owner = WebView(compose.activity)
            val controller = WebVideoController()
            var firstExits = 0
            var rejectedExits = 0
            var replacementExits = 0
            try {
                controller.show(owner, FrameLayout(compose.activity), WebChromeClient.CustomViewCallback { firstExits++ })
                val first = controller.fullscreen.value!!
                controller.show(owner, FrameLayout(compose.activity), WebChromeClient.CustomViewCallback { rejectedExits++ })
                assertSame(first, controller.fullscreen.value)
                assertEquals(1, rejectedExits)
                controller.exit()
                assertEquals(1, firstExits)
                controller.show(owner, FrameLayout(compose.activity), WebChromeClient.CustomViewCallback { replacementExits++ })
                val replacement = controller.fullscreen.value
                controller.hide(first)
                assertSame(replacement, controller.fullscreen.value)
                assertEquals(0, replacementExits)
                controller.hide(owner)
                assertNull(controller.fullscreen.value)
                assertEquals(1, replacementExits)
            } finally {
                controller.exit()
                owner.destroy()
            }
        }
    }
}
