package com.tvbrowser.core.design

import androidx.compose.ui.graphics.Color

/**
 * Pebble design tokens — color.
 * Source of truth: design_handoff_pebble_tv_browser/colors_and_type.css §1–4.
 * Near-monochrome cool ramp + single slate-blue accent + 3 muted status colors.
 */
object PebbleColors {
    // 1. Surface ramp (cool near-black; higher index = closer to viewer)
    val Bg        = Color(0xFF0E1013)
    val Surface   = Color(0xFF14161B)
    val Surface1  = Color(0xFF181B21)
    val Surface2  = Color(0xFF1D2027)
    val Surface3  = Color(0xFF23272F)
    val Surface4  = Color(0xFF2A2F38)
    val Scrim     = Color(0xB8060709) // rgba(6,7,9,.72)

    // 2. Text / outline
    val OnSurface       = Color(0xFFE6E9EF)
    val OnSurfaceMuted  = Color(0xFFAAB1BD)
    val OnSurfaceFaint  = Color(0xFF6F7782)
    val Outline         = Color(0xFF353B45)
    val OutlineFaint    = Color(0xFF23272F)

    // 3. Accent (ONE accent only — slate-blue)
    val Primary             = Color(0xFF8AA9CF)
    val PrimaryStrong       = Color(0xFFA8C2E2)
    val PrimaryDim          = Color(0xFF5D7E9F)
    val PrimaryContainer    = Color(0xFF233246)
    val OnPrimary           = Color(0xFF0C1722)
    val OnPrimaryContainer  = Color(0xFFCFE0F5)

    // 4. Functional status — downloads only
    val Good   = Color(0xFF84B59A)
    val Warn   = Color(0xFFCDAB74)
    val Danger = Color(0xFFC98B8B)
}
