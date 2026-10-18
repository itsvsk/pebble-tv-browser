package com.tvbrowser.core.cursor

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class WebDpadScrollTest {
    @Test
    fun verticalDpadScrollsFocusedPageInBothDirections() {
        assertEquals(360, webDpadScrollDelta(KeyEvent.KEYCODE_DPAD_DOWN, 1080, false, true))
        assertEquals(-360, webDpadScrollDelta(KeyEvent.KEYCODE_DPAD_UP, 1080, false, true))
    }

    @Test
    fun doesNotTakeKeysFromChromeOrPointerMode() {
        assertEquals(0, webDpadScrollDelta(KeyEvent.KEYCODE_DPAD_DOWN, 1080, false, false))
        assertEquals(0, webDpadScrollDelta(KeyEvent.KEYCODE_DPAD_DOWN, 1080, true, true))
        assertEquals(0, webDpadScrollDelta(KeyEvent.KEYCODE_DPAD_RIGHT, 1080, false, true))
    }
}
