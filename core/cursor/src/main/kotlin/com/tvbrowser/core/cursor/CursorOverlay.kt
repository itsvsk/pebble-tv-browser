package com.tvbrowser.core.cursor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CursorOverlay(
    controller: CursorController,
    color: Color = Color(0xFF5FB0A8),
    radiusPx: Float = 18f,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    if (!state.active) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val c = Offset(state.x, state.y)
        drawCircle(color = color.copy(alpha = 0.25f), radius = radiusPx * 1.5f, center = c)
        drawCircle(color = color, radius = radiusPx, center = c, style = Stroke(width = 4f))
        drawCircle(color = color, radius = 3f, center = c)
    }
}
