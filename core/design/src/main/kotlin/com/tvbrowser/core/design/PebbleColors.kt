package com.tvbrowser.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors shared by every native Pebble surface. */
@Suppress("PropertyName") // Preserve the established Pebble color token names.
data class PebblePalette(
    val isDark: Boolean,
    val Bg: Color,
    val Surface: Color,
    val Surface1: Color,
    val Surface2: Color,
    val Surface3: Color,
    val Surface4: Color,
    val Scrim: Color,
    val Glass: Color,
    val GlassHigh: Color,
    val OnSurface: Color,
    val OnSurfaceMuted: Color,
    val OnSurfaceFaint: Color,
    val Outline: Color,
    val OutlineFaint: Color,
    val GlassEdge: Color,
    val Primary: Color,
    val PrimaryStrong: Color,
    val PrimaryDim: Color,
    val PrimaryContainer: Color,
    val OnPrimary: Color,
    val OnPrimaryContainer: Color,
    val GlassMint: Color,
    val GlassLagoon: Color,
    val GlassRose: Color,
    val GlassFocus: Color,
    val Good: Color,
    val Warn: Color,
    val Danger: Color,
)

val LightPebblePalette = PebblePalette(
    isDark = false,
    Bg = Color(0xFFF0E5D2), Surface = Color(0xFFFAF7EE),
    Surface1 = Color(0xFFFCF9F1), Surface2 = Color(0xFFF0EDE1),
    Surface3 = Color(0xFFE4E9DD), Surface4 = Color(0xFFCEDDD2),
    Scrim = Color(0x99132924), Glass = Color(0xCFF7FCF9),
    GlassHigh = Color(0xEDFFFFFF), OnSurface = Color(0xFF17382F),
    OnSurfaceMuted = Color(0xFF4B695E), OnSurfaceFaint = Color(0xFF5E776D),
    Outline = Color(0xFF648C7C), OutlineFaint = Color(0xFFBED5C9),
    GlassEdge = Color(0xFFFFFFFF), Primary = Color(0xFF177963),
    PrimaryStrong = Color(0xFFAFF1D4), PrimaryDim = Color(0xFF3D957C),
    PrimaryContainer = Color(0xFFCDEDDC), OnPrimary = Color(0xFFFFFFFF),
    OnPrimaryContainer = Color(0xFF194C3D), GlassMint = Color(0xFFD5F1E2),
    GlassLagoon = Color(0xFFD9EBF1), GlassRose = Color(0xFFF3E5E4),
    GlassFocus = Color(0xFF78D8B4), Good = Color(0xFF287E65),
    Warn = Color(0xFF98652F), Danger = Color(0xFFAE455D),
)

/** Deep sea-green surfaces and a restrained luminous mint accent for low-light TV viewing. */
val DarkPebblePalette = PebblePalette(
    isDark = true,
    Bg = Color(0xFF0D1A1C), Surface = Color(0xFF142326),
    Surface1 = Color(0xFF192A2D), Surface2 = Color(0xFF213438),
    Surface3 = Color(0xFF294044), Surface4 = Color(0xFF34534E),
    Scrim = Color(0xCC040D0E), Glass = Color(0xD11B3133),
    GlassHigh = Color(0xE32C4948), OnSurface = Color(0xFFE7F3EC),
    OnSurfaceMuted = Color(0xFFB5CAC1), OnSurfaceFaint = Color(0xFFABC7BA),
    Outline = Color(0xFF91B3A6), OutlineFaint = Color(0xFF4A6B62),
    GlassEdge = Color(0xFF97C7B8), Primary = Color(0xFF93E4C4),
    PrimaryStrong = Color(0xFFD4FFE9), PrimaryDim = Color(0xFF8DCBAD),
    PrimaryContainer = Color(0xFF275345), OnPrimary = Color(0xFF073429),
    OnPrimaryContainer = Color(0xFFE3F7ED), GlassMint = Color(0xFF234B42),
    GlassLagoon = Color(0xFF213B45), GlassRose = Color(0xFF493138),
    GlassFocus = Color(0xFF3B7967), Good = Color(0xFF91DAB4),
    Warn = Color(0xFFF2C77E), Danger = Color(0xFFFFACAD),
)

internal val LocalPebblePalette = staticCompositionLocalOf { LightPebblePalette }

/** Existing role names now resolve from the active Compose theme. */
object PebbleColors {
    @get:Composable val IsDark get() = LocalPebblePalette.current.isDark
    @get:Composable val Bg get() = LocalPebblePalette.current.Bg
    @get:Composable val Surface get() = LocalPebblePalette.current.Surface
    @get:Composable val Surface1 get() = LocalPebblePalette.current.Surface1
    @get:Composable val Surface2 get() = LocalPebblePalette.current.Surface2
    @get:Composable val Surface3 get() = LocalPebblePalette.current.Surface3
    @get:Composable val Surface4 get() = LocalPebblePalette.current.Surface4
    @get:Composable val Scrim get() = LocalPebblePalette.current.Scrim
    @get:Composable val Glass get() = LocalPebblePalette.current.Glass
    @get:Composable val GlassHigh get() = LocalPebblePalette.current.GlassHigh
    @get:Composable val OnSurface get() = LocalPebblePalette.current.OnSurface
    @get:Composable val OnSurfaceMuted get() = LocalPebblePalette.current.OnSurfaceMuted
    @get:Composable val OnSurfaceFaint get() = LocalPebblePalette.current.OnSurfaceFaint
    @get:Composable val Outline get() = LocalPebblePalette.current.Outline
    @get:Composable val OutlineFaint get() = LocalPebblePalette.current.OutlineFaint
    @get:Composable val GlassEdge get() = LocalPebblePalette.current.GlassEdge
    @get:Composable val Primary get() = LocalPebblePalette.current.Primary
    @get:Composable val PrimaryStrong get() = LocalPebblePalette.current.PrimaryStrong
    @get:Composable val PrimaryDim get() = LocalPebblePalette.current.PrimaryDim
    @get:Composable val PrimaryContainer get() = LocalPebblePalette.current.PrimaryContainer
    @get:Composable val OnPrimary get() = LocalPebblePalette.current.OnPrimary
    @get:Composable val OnPrimaryContainer get() = LocalPebblePalette.current.OnPrimaryContainer
    @get:Composable val GlassMint get() = LocalPebblePalette.current.GlassMint
    @get:Composable val GlassLagoon get() = LocalPebblePalette.current.GlassLagoon
    @get:Composable val GlassRose get() = LocalPebblePalette.current.GlassRose
    @get:Composable val GlassFocus get() = LocalPebblePalette.current.GlassFocus
    @get:Composable val Good get() = LocalPebblePalette.current.Good
    @get:Composable val Warn get() = LocalPebblePalette.current.Warn
    @get:Composable val Danger get() = LocalPebblePalette.current.Danger
}
