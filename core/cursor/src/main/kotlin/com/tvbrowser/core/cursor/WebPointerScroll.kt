package com.tvbrowser.core.cursor

import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import org.json.JSONArray

/** Find a nested feed beneath the pointer, including feeds behind a fixed edge toolbar. */
internal fun findNestedScrollTarget(
    view: WebView,
    x: Float,
    y: Float,
    deltaX: Int,
    deltaY: Int,
    onResult: (Pair<Float, Float>?) -> Unit,
) {
    val width = view.width.coerceAtLeast(1)
    val height = view.height.coerceAtLeast(1)
    val px = x.coerceIn(0f, (width - 1).toFloat())
    val py = y.coerceIn(0f, (height - 1).toFloat())
    // Inspect geometry only. Let Chromium's native wheel hit testing perform the scroll,
    // rather than changing site focus or rewriting the site's scrolling behavior.
    view.evaluateJavascript("""
        (function() {
            var vw = window.visualViewport ? window.visualViewport.width : window.innerWidth;
            var vh = window.visualViewport ? window.visualViewport.height : window.innerHeight;
            var x = $px / $width * vw, y = $py / $height * vh;
            var dx = $deltaX, dy = $deltaY;
            function eligible(e) {
                if (!e || e === document.body || e === document.documentElement) return false;
                var position = dy ? e.scrollTop : e.scrollLeft;
                var available = dy ? e.scrollHeight - e.clientHeight : e.scrollWidth - e.clientWidth;
                if (available <= 1 || ((dy || dx) < 0 ? position <= 0 : position >= available - 1)) return false;
                var s = getComputedStyle(e);
                var overflow = dy ? s.overflowY : s.overflowX;
                return overflow === 'auto' || overflow === 'scroll' || overflow === 'overlay';
            }
            var hit = document.elementFromPoint(x, y), e = hit;
            while (e) {
                if (eligible(e)) return [x / vw * $width, y / vh * $height];
                e = e.parentElement;
            }
            var nodes = document.querySelectorAll('*'), best = null, score = 0;
            for (var i = 0, limit = Math.min(nodes.length, 4096); i < limit; i++) {
                e = nodes[i];
                if (e.clientWidth < 40 || e.clientHeight < 40 || !eligible(e)) continue;
                var r = e.getBoundingClientRect();
                var l = Math.max(0, r.left), t = Math.max(0, r.top);
                var right = Math.min(vw, r.right), bottom = Math.min(vh, r.bottom);
                if (right <= l || bottom <= t || (dy ? x < l || x >= right : y < t || y >= bottom)) continue;
                var area = (right - l) * (bottom - t);
                if (area > score) { score = area; best = [l, t, right, bottom]; }
            }
            return best ? [(best[0] + best[2]) / 2 / vw * $width,
                           (best[1] + best[3]) / 2 / vh * $height] : null;
        })();
    """.trimIndent()) { value ->
        val point = runCatching {
            val a = JSONArray(value)
            a.getDouble(0).toFloat() to a.getDouble(1).toFloat()
        }.getOrNull()
        onResult(point)
    }
}

/** A real mouse-wheel event also reaches scroll areas inside cross-origin frames. */
internal fun dispatchPointerWheel(view: View, x: Float, y: Float, deltaX: Int, deltaY: Int, horizontalSign: Float = 1f) {
    val config = ViewConfiguration.get(view.context)
    val fallback = TypedValue().let { value ->
        if (view.context.theme.resolveAttribute(android.R.attr.listPreferredItemHeight, value, true))
            value.getDimension(view.resources.displayMetrics)
        else 48f * view.resources.displayMetrics.density
    }.coerceAtLeast(1f)
    val vertical = if (Build.VERSION.SDK_INT >= 26) config.scaledVerticalScrollFactor.coerceAtLeast(1f) else fallback
    val horizontal = if (Build.VERSION.SDK_INT >= 26) config.scaledHorizontalScrollFactor.coerceAtLeast(1f) else fallback
    val properties = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE }
    val coords = MotionEvent.PointerCoords().apply {
        val maxX = (view.width - 1).coerceAtLeast(0).toFloat()
        val maxY = (view.height - 1).coerceAtLeast(0).toFloat()
        this.x = x.coerceIn(if (maxX > 0) 1f else 0f, maxX)
        this.y = y.coerceIn(if (maxY > 0) 1f else 0f, maxY)
        setAxisValue(MotionEvent.AXIS_VSCROLL, -deltaY / vertical)
        setAxisValue(MotionEvent.AXIS_HSCROLL, horizontalSign * deltaX / horizontal)
    }
    val now = SystemClock.uptimeMillis()
    val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1,
        arrayOf(properties), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0)
    try { view.dispatchGenericMotionEvent(event) } finally { event.recycle() }
}
