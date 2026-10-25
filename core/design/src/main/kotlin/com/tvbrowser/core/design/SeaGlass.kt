package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Soft frosted fill, luminous rim and transmitted color beneath TV controls. */
@Composable
fun Modifier.seaGlass(
    shape: Shape = PebbleShape.Md,
    tint: Color? = null,
    elevation: Dp = 9.dp,
    emphasized: Boolean = false,
): Modifier {
    val palette = LocalPebblePalette.current
    val glassTint = tint ?: palette.GlassMint
    val glassFill = if (palette.isDark) {
        Brush.verticalGradient(
            0f to palette.GlassHigh.copy(alpha = 0.98f),
            0.34f to glassTint.copy(alpha = 0.92f),
            0.78f to glassTint.copy(alpha = 0.86f),
            1f to palette.Surface1.copy(alpha = 0.98f),
        )
    } else {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.98f),
            0.30f to glassTint.copy(alpha = if (emphasized) 0.95f else 0.74f),
            0.77f to glassTint.copy(alpha = if (emphasized) 0.98f else 0.91f),
            1f to Color.White.copy(alpha = 0.96f),
        )
    }
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = glassTint.copy(alpha = if (emphasized) 0.75f else 0.42f),
        spotColor = palette.Primary.copy(alpha = if (emphasized) 0.38f else 0.18f),
    )
    .background(glassFill, shape)
    .background(
        Brush.radialGradient(
            colors = listOf(
                if (palette.isDark) palette.Primary.copy(alpha = if (emphasized) 0.13f else 0.07f)
                else Color.White.copy(alpha = if (emphasized) 0.28f else 0.62f),
                Color.Transparent,
            ),
            radius = 440f,
        ),
        shape,
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            0f to if (palette.isDark) palette.GlassEdge.copy(alpha = 0.78f) else Color.White,
            0.5f to palette.OutlineFaint.copy(alpha = if (emphasized) 0.9f else 0.65f),
            1f to if (palette.isDark) palette.OutlineFaint.copy(alpha = 0.8f) else Color.White,
        ),
        shape,
    )
}
