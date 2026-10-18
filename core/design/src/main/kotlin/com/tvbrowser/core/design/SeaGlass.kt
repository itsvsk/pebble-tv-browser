package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Soft frosted fill, luminous rim and transmitted color beneath TV controls. */
fun Modifier.seaGlass(
    shape: Shape = PebbleShape.Md,
    tint: Color = PebbleColors.GlassMint,
    elevation: Dp = 9.dp,
    emphasized: Boolean = false,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint.copy(alpha = if (emphasized) 0.75f else 0.42f),
        spotColor = PebbleColors.Primary.copy(alpha = if (emphasized) 0.38f else 0.18f),
    )
    .background(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.98f),
            0.30f to tint.copy(alpha = if (emphasized) 0.95f else 0.74f),
            0.77f to tint.copy(alpha = if (emphasized) 0.98f else 0.91f),
            1f to Color.White.copy(alpha = 0.96f),
        ),
        shape,
    )
    .background(
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = if (emphasized) 0.28f else 0.62f), Color.Transparent),
            radius = 440f,
        ),
        shape,
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            0f to Color.White,
            0.5f to PebbleColors.OutlineFaint.copy(alpha = if (emphasized) 0.9f else 0.65f),
            1f to Color.White,
        ),
        shape,
    )
