package com.tvbrowser.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
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
private fun paletteScheme(palette: PebblePalette) = with(palette) {
    if (isDark) darkColorScheme(
        primary = Primary, onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
        background = Bg, onBackground = OnSurface,
        surface = Surface1, onSurface = OnSurface,
        surfaceVariant = Surface2, onSurfaceVariant = OnSurfaceMuted,
        border = Outline, borderVariant = OutlineFaint,
        error = Danger, onError = Bg, scrim = Scrim,
    ) else lightColorScheme(
        primary = Primary, onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
        background = Bg, onBackground = OnSurface,
        surface = Surface1, onSurface = OnSurface,
        surfaceVariant = Surface2, onSurfaceVariant = OnSurfaceMuted,
        border = Outline, borderVariant = OutlineFaint,
        error = Danger, onError = OnPrimary, scrim = Scrim,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PebbleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPebblePalette else LightPebblePalette
    val colorScheme = remember(palette) { paletteScheme(palette) }
    CompositionLocalProvider(LocalPebblePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PebbleTypography,
            shapes = PebbleShapes,
            content = content,
        )
    }
}
