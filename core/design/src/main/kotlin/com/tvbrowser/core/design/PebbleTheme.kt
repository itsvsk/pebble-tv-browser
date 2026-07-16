package com.tvbrowser.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Pebble theme — wires the design tokens into TV Material 3.
 *
 * M3 has slots for primary/surface/etc but NO slot for a 6-step surface ramp.
 * Strategy:
 *  - Map what M3 has slots for into [darkColorScheme].
 *  - Expose the rest of the ramp + status colors via [LocalPebbleColors] / [PebbleTheme.colors].
 *
 * Slot mapping (per README §"DESIGN TOKENS → Compose mapping"):
 *   background          = bg
 *   surface             = surface-1  (resting cards & rows — most common surface)
 *   surfaceVariant      = surface-2  (inputs, focused rows, segmented control)
 *   primary             = primary
 *   primaryContainer    = primary-container
 *   onPrimary           = on-primary
 *   onPrimaryContainer  = on-primary-container
 *   onSurface           = on-surface
 *   onSurfaceVariant    = on-surface-muted
 *   border              = outline
 *   error               = danger
 */
@OptIn(ExperimentalTvMaterial3Api::class)
private val PebbleColorScheme = darkColorScheme(
    primary = PebbleColors.Primary,
    onPrimary = PebbleColors.OnPrimary,
    primaryContainer = PebbleColors.PrimaryContainer,
    onPrimaryContainer = PebbleColors.OnPrimaryContainer,
    background = PebbleColors.Bg,
    onBackground = PebbleColors.OnSurface,
    surface = PebbleColors.Surface1,
    onSurface = PebbleColors.OnSurface,
    surfaceVariant = PebbleColors.Surface2,
    onSurfaceVariant = PebbleColors.OnSurfaceMuted,
    border = PebbleColors.Outline,
    borderVariant = PebbleColors.OutlineFaint,
    error = PebbleColors.Danger,
    onError = PebbleColors.OnPrimary,
    scrim = PebbleColors.Scrim,
)

/** Holder for tokens that don't fit M3's slot scheme (full surface ramp + status). */
data class PebbleExtendedColors(
    val bg: androidx.compose.ui.graphics.Color = PebbleColors.Bg,
    val surface: androidx.compose.ui.graphics.Color = PebbleColors.Surface,
    val surface1: androidx.compose.ui.graphics.Color = PebbleColors.Surface1,
    val surface2: androidx.compose.ui.graphics.Color = PebbleColors.Surface2,
    val surface3: androidx.compose.ui.graphics.Color = PebbleColors.Surface3,
    val surface4: androidx.compose.ui.graphics.Color = PebbleColors.Surface4,
    val onSurfaceFaint: androidx.compose.ui.graphics.Color = PebbleColors.OnSurfaceFaint,
    val outlineFaint: androidx.compose.ui.graphics.Color = PebbleColors.OutlineFaint,
    val primaryStrong: androidx.compose.ui.graphics.Color = PebbleColors.PrimaryStrong,
    val primaryDim: androidx.compose.ui.graphics.Color = PebbleColors.PrimaryDim,
    val good: androidx.compose.ui.graphics.Color = PebbleColors.Good,
    val warn: androidx.compose.ui.graphics.Color = PebbleColors.Warn,
    val danger: androidx.compose.ui.graphics.Color = PebbleColors.Danger,
)

val LocalPebbleColors = staticCompositionLocalOf { PebbleExtendedColors() }

object PebbleTheme {
    val colors: PebbleExtendedColors
        @Composable get() = LocalPebbleColors.current
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PebbleTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPebbleColors provides PebbleExtendedColors()) {
        MaterialTheme(
            colorScheme = PebbleColorScheme,
            typography = PebbleTypography,
            shapes = PebbleShapes,
            content = content,
        )
    }
}
