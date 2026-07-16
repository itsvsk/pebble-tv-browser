package com.tvbrowser.app.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.Primary,
    onPrimary = BrandColors.OnPrimary,
    primaryContainer = BrandColors.PrimaryContainer,
    onPrimaryContainer = BrandColors.OnPrimaryContainer,
    secondary = BrandColors.Secondary,
    onSecondary = BrandColors.OnSecondary,
    secondaryContainer = BrandColors.SecondaryContainer,
    onSecondaryContainer = BrandColors.OnSecondaryContainer,
    tertiary = BrandColors.Tertiary,
    onTertiary = BrandColors.OnTertiary,
    background = BrandColors.Background,
    onBackground = BrandColors.OnBackground,
    surface = BrandColors.Surface,
    onSurface = BrandColors.OnSurface,
    surfaceVariant = BrandColors.SurfaceVariant,
    onSurfaceVariant = BrandColors.OnSurfaceVariant,
    border = BrandColors.Outline,
    error = BrandColors.Error,
    onError = BrandColors.OnError,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TvTypography,
        shapes = TvShapes,
        content = content,
    )
}
