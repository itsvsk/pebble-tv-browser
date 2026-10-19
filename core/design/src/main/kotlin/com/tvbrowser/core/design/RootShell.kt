package com.tvbrowser.core.design

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import kotlin.random.Random

/**
 * Persistent glass navigation shared by all native screens.
 */
@Composable
fun RootShell(
    active: NavDestination,
    onSelect: (NavDestination) -> Unit,
    showRail: Boolean = true,
    focusRequesters: Map<NavDestination, FocusRequester>,
    activeDownFocusRequester: FocusRequester? = null,
    onFocusedDestinationChanged: (NavDestination, Boolean) -> Unit,
    cursorModeActive: Boolean?,
    onToggleCursor: () -> Unit,
    content: @Composable () -> Unit,
) {
    val grain = remember { createPaperGrain() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                // A warm sand-colored paper canvas lets the cool glass keep its depth.
                val topGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF9EB).copy(alpha = 0.86f),
                        Color(0xFFFFF4DF).copy(alpha = 0.32f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.43f, -size.height * 0.12f),
                    radius = size.width * 0.42f,
                )
                val sideGlow = Brush.radialGradient(
                    colors = listOf(PebbleColors.GlassMint.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.73f, size.height * 0.26f),
                    radius = size.width * 0.26f,
                )
                val sandWash = Brush.radialGradient(
                    colors = listOf(Color(0xFFB7935F).copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.27f, size.height * 0.92f),
                    radius = size.width * 0.26f,
                )
                val paperTexture = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
                onDrawBehind {
                    drawRect(PebbleColors.Bg)
                    drawRect(topGlow)
                    drawRect(sideGlow)
                    drawRect(sandWash)
                    drawRect(paperTexture)
                }
            },
    ) {
        // Keep navigation above the WebView's first paint during browser transitions.
        if (showRail) Box(modifier = Modifier.zIndex(1f)) {
            TopNav(
                active = active,
                onSelect = onSelect,
                focusRequesters = focusRequesters,
                activeDownFocusRequester = activeDownFocusRequester,
                onFocusedDestinationChanged = onFocusedDestinationChanged,
                cursorModeActive = cursorModeActive,
                onToggleCursor = onToggleCursor,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

/** A small deterministic tile, generated once and repeated by the GPU. */
private fun createPaperGrain(): ImageBitmap {
    val side = 256
    val pixels = IntArray(side * side)
    val random = Random(0x5EA61A55)
    for (index in pixels.indices) {
        val light = random.nextBoolean()
        val alpha = if (random.nextInt(32) == 0) random.nextInt(18, 32)
                    else random.nextInt(3, 14)
        pixels[index] = if (light) AndroidColor.argb(alpha, 255, 250, 236)
                        else AndroidColor.argb(alpha, 116, 91, 58)
    }
    return Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, side, 0, 0, side, side)
    }.asImageBitmap()
}
