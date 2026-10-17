package com.tvbrowser.core.cursor

internal enum class CursorDirection { Left, Right, Up, Down }

internal data class CursorMotion(
    val x: Float,
    val y: Float,
    val scrollX: Int,
    val scrollY: Int,
)

internal fun planCursorMotion(
    state: CursorState,
    direction: CursorDirection,
    repeatCount: Int,
): CursorMotion {
    val step = if (repeatCount > 2) 96f else 24f
    val dx = when (direction) {
        CursorDirection.Left -> -1
        CursorDirection.Right -> 1
        else -> 0
    }
    val dy = when (direction) {
        CursorDirection.Up -> -1
        CursorDirection.Down -> 1
        else -> 0
    }
    val width = state.viewportW.coerceAtLeast(0).toFloat()
    val height = state.viewportH.coerceAtLeast(0).toFloat()
    val atHorizontalEdge = width > 0f &&
        ((dx < 0 && state.x <= 0f) || (dx > 0 && state.x >= width))
    val atVerticalEdge = height > 0f &&
        ((dy < 0 && state.y <= 0f) || (dy > 0 && state.y >= height))

    return CursorMotion(
        x = (state.x + dx * step).coerceIn(0f, width),
        y = (state.y + dy * step).coerceIn(0f, height),
        scrollX = if (repeatCount > 0 && atHorizontalEdge) (dx * step).toInt() else 0,
        scrollY = if (repeatCount > 0 && atVerticalEdge) (dy * step).toInt() else 0,
    )
}
