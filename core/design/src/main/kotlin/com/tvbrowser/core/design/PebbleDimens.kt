package com.tvbrowser.core.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/**
 * Pebble design tokens — spacing, layout, focus, motion.
 * Source: colors_and_type.css §7, §9, §10, §11, §12.
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
    val Gutter   = 56.dp   // page side gutter / safe-area inset
    val RailW    = 116.dp  // navigation rail width
    val ChromeH  = 92.dp   // browser top bar
    val HintH    = 56.dp   // key-hint bar

    // Focus — two-part ring: 3dp bg gap + 3dp primary ring; rail/menu use 2+2
    val FocusGap        = 3.dp
    val FocusRing       = 3.dp
    val FocusGapTight   = 2.dp
    val FocusRingTight  = 2.dp
    val FocusLiftY      = (-3).dp
    const val FocusLiftScale = 1.03f
}

object PebbleElevation {
    val E1 = 8.dp   // resting card shadow
    val E2 = 24.dp  // focused card lift
    val E3 = 48.dp  // menus / sheets
}

object PebbleMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val DurationMs = 180
    const val DurationFastMs = 120
}
