package com.tvbrowser.core.design

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.lightColorScheme

/**
 * Pebble theme — wires the design tokens into TV Material 3.
 *
 * Maps the shared Pebble colors into TV Material 3; additional colors remain in [PebbleColors].
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
private val PebbleColorScheme = lightColorScheme(
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PebbleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PebbleColorScheme,
        typography = PebbleTypography,
        shapes = PebbleShapes,
        content = content,
    )
}
