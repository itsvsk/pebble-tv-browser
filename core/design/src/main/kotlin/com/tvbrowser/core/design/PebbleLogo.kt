package com.tvbrowser.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tvbrowser.core.design.PebbleColors

/**
 * Pebble logo mark — slate-blue radial-gradient circle with soft outer glow.
 * Recreated in Compose from the prototype's CSS radial gradient.
 */
@Composable
fun PebbleLogo(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val primary = PebbleColors.Primary
    val primaryStrong = PebbleColors.PrimaryStrong
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // soft outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to primary.copy(alpha = 0.35f),
                    1f to primary.copy(alpha = 0f),
                ),
                center = center,
                radius = r * 1.6f,
            ),
            radius = r * 1.6f,
            center = center,
        )

        // pebble body (PrimaryStrong → Primary radial)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to primaryStrong,
                    1f to primary,
                ),
                center = Offset(center.x - r * 0.25f, center.y - r * 0.3f),
                radius = r,
            ),
            radius = r * 0.94f,
            center = center,
        )
    }
}
