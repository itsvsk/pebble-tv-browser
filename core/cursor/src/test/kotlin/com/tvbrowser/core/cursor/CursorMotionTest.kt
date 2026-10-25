package com.tvbrowser.core.cursor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorMotionTest {
    @Test
    fun movesWithinTheViewportWithoutScrolling() {
        val result = planCursorMotion(
            CursorState(x = 40f, y = 30f, viewportW = 100, viewportH = 80),
            CursorDirection.Right,
            repeatCount = 0,
        )

        assertEquals(64f, result.x)
        assertEquals(30f, result.y)
        assertEquals(0, result.scrollX)
        assertEquals(0, result.scrollY)
    }

    @Test
    fun startsScrollingOnlyAfterThePointerReachesAnEdgeAndTheKeyRepeats() {
        val nearEdge = CursorState(x = 90f, y = 70f, viewportW = 100, viewportH = 80)
        val reached = planCursorMotion(nearEdge, CursorDirection.Right, repeatCount = 1)
        val held = planCursorMotion(nearEdge.copy(x = reached.x), CursorDirection.Right, repeatCount = 3)

        assertEquals(100f, reached.x)
        assertEquals(0, reached.scrollX)
        assertEquals(100f, held.x)
        assertEquals(96, held.scrollX)
    }

    @Test
    fun scrollsInTheHeldDirectionAtAllFourEdges() {
        val bottomRight = CursorState(x = 100f, y = 80f, viewportW = 100, viewportH = 80)
        val topLeft = bottomRight.copy(x = 0f, y = 0f)

        assertEquals(96, planCursorMotion(bottomRight, CursorDirection.Right, 3).scrollX)
        assertEquals(96, planCursorMotion(bottomRight, CursorDirection.Down, 3).scrollY)
        assertEquals(-96, planCursorMotion(topLeft, CursorDirection.Left, 3).scrollX)
        assertEquals(-96, planCursorMotion(topLeft, CursorDirection.Up, 3).scrollY)
        assertEquals(24, planCursorMotion(bottomRight, CursorDirection.Down, 0).scrollY)
    }

    @Test
    fun separatePressesAtAnEdgeScrollThePage() {
        val top = CursorState(x = 50f, y = 0f, viewportW = 100, viewportH = 80)
        val bottom = top.copy(y = 80f)

        assertEquals(-24, planCursorMotion(top, CursorDirection.Up, 0).scrollY)
        assertEquals(24, planCursorMotion(bottom, CursorDirection.Down, 0).scrollY)
    }

    @Test
    fun pointerUpReturnsToControlsOnlyAtTheTopOfThePage() {
        val pointer = CursorState(active = true, x = 50f, y = 0f, viewportW = 100, viewportH = 80)

        assertTrue(shouldReturnToControlsFromPointer(pointer, canScrollUp = false))
        assertFalse(shouldReturnToControlsFromPointer(pointer, canScrollUp = true))
        assertFalse(shouldReturnToControlsFromPointer(pointer.copy(y = 1f), canScrollUp = false))
        assertFalse(shouldReturnToControlsFromPointer(pointer.copy(active = false), canScrollUp = false))
    }

    @Test
    fun viewportResizeKeepsPointerInsideTheWebPage() {
        val pointer = CursorState(active = true, x = 190f, y = 90f, viewportW = 200, viewportH = 100)

        val resized = pointer.withViewport(120, 60, positionInitialized = true)

        assertEquals(120f, resized.x)
        assertEquals(60f, resized.y)
        assertEquals(120, resized.viewportW)
        assertEquals(60, resized.viewportH)
    }
}
