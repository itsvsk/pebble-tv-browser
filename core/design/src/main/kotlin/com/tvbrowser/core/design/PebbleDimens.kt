package com.tvbrowser.core.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/**
 * Pebble design tokens — spacing, layout, focus, motion.
 * Shared TV measurements used by the glass UI.
 */
object PebbleDimens {
    // 4dp base grid
    val Sp1  = 4.dp
    val Sp2  = 8.dp
    val Sp3  = 12.dp
    val Sp4  = 16.dp
    val Sp5  = 20.dp
    val Sp6  = 24.dp
    val Sp8  = 32.dp
    val Sp10 = 40.dp
    val Sp14 = 56.dp

    // Layout
    val Gutter   = 48.dp   // TV safe-area inset
    val RailW    = 116.dp  // navigation rail width
    val ChromeH  = 76.dp

    // Cyan outer ring with a narrow inner separation keeps D-pad position clear.
    val FocusGap        = 2.dp
    val FocusRing       = 2.dp
    val FocusGapTight   = 1.dp
    val FocusRingTight  = 2.dp
    val FocusLiftY      = (-3).dp
    const val FocusLiftScale = 1.05f
}

object PebbleElevation {
    val E1 = 8.dp   // resting card shadow
    val E2 = 24.dp  // focused card lift
    val E3 = 48.dp  // menus / sheets
}

object PebbleMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val DurationMs = 260
    const val DurationFastMs = 160
    const val ScreenEnterMs = 260
    const val ScreenExitMs = 180
}
