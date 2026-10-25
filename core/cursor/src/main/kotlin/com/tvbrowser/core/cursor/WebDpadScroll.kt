package com.tvbrowser.core.cursor

import android.view.KeyEvent

/** Vertical D-pad keys scroll a focused page instead of traversing its controls. */
internal fun webDpadScrollDelta(
    keyCode: Int,
    viewportHeight: Int,
    pointerActive: Boolean,
    webFocused: Boolean,
): Int {
    if (pointerActive || !webFocused) return 0
    val direction = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> -1
        KeyEvent.KEYCODE_DPAD_DOWN -> 1
        else -> return 0
    }
    return direction * (viewportHeight / 3).coerceIn(96, 600)
}
