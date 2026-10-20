package com.tvbrowser.core.design

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
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
    val palette = LocalPebblePalette.current
    val grain = remember(palette.isDark) { createPaperGrain(palette.isDark) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                // Rasterize the sea-glass canvas once per size/theme. Focus movement then
                // draws one image rather than five full-screen shader passes per frame.
                val background = createBackgroundBitmap(
                    size.width.roundToInt().coerceAtLeast(1),
                    size.height.roundToInt().coerceAtLeast(1),
                    palette,
                    grain,
                )
                onDrawBehind {
                    drawImage(background)
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
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .then(if (active != NavDestination.Browser) Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                } else Modifier),
        ) {
            content()
        }
    }
}

/** A small deterministic tile, generated once and repeated across the cached canvas. */
private fun createPaperGrain(dark: Boolean): Bitmap {
    val side = 256
    val pixels = IntArray(side * side)
    val random = Random(0x5EA61A55)
    for (index in pixels.indices) {
        val light = random.nextBoolean()
        val alpha = if (random.nextInt(32) == 0) random.nextInt(18, 32)
                    else random.nextInt(3, 14)
        pixels[index] = if (dark) {
            if (light) AndroidColor.argb(alpha, 154, 202, 185)
            else AndroidColor.argb(alpha, 2, 14, 17)
        } else {
            if (light) AndroidColor.argb(alpha, 255, 250, 236)
            else AndroidColor.argb(alpha, 116, 91, 58)
        }
    }
    return Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, side, 0, 0, side, side)
    }
}

private fun createBackgroundBitmap(
    width: Int,
    height: Int,
    palette: PebblePalette,
    grain: Bitmap,
): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(palette.Bg.toArgb())

    fun radialGlow(cx: Float, cy: Float, radius: Float, colors: List<Color>) {
        paint.shader = RadialGradient(
            cx, cy, radius,
            colors.map(Color::toArgb).toIntArray(), null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    radialGlow(width * 0.43f, -height * 0.12f, width * 0.42f,
        if (palette.isDark) listOf(
            Color(0xFF315D55).copy(alpha = 0.42f),
            Color(0xFF1D3A39).copy(alpha = 0.19f),
            Color.Transparent,
        ) else listOf(
            Color(0xFFFFF9EB).copy(alpha = 0.86f),
            Color(0xFFFFF4DF).copy(alpha = 0.32f),
            Color.Transparent,
        ),
    )
    radialGlow(width * 0.73f, height * 0.26f, width * 0.26f,
        listOf(palette.GlassMint.copy(alpha = if (palette.isDark) 0.32f else 0.28f), Color.Transparent),
    )
    radialGlow(width * 0.27f, height * 0.92f, width * 0.26f,
        listOf(
            if (palette.isDark) Color(0xFF205054).copy(alpha = 0.22f)
            else Color(0xFFB7935F).copy(alpha = 0.13f),
            Color.Transparent,
        ),
    )
    paint.shader = BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    return bitmap.asImageBitmap()
}
