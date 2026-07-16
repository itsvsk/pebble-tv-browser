package com.tvbrowser.core.cursor

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.tvbrowser.core.common.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class CursorState(
    val active: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val viewportW: Int = 0,
    val viewportH: Int = 0,
)

/**
 * Hybrid spatial-focus + virtual-cursor controller.
 *
 * - Long-press DPAD_CENTER (>500ms) toggles cursor mode on/off
 * - In cursor mode: arrow keys move pointer; CENTER taps; BACK exits
 * - Outside cursor mode: returns false so spatial focus traversal works normally
 */
@Singleton
class CursorController @Inject constructor(
    settings: SettingsRepository,
) {

    private val _state = MutableStateFlow(CursorState())
    val state: StateFlow<CursorState> = _state.asStateFlow()

    private var targetView: View? = null
    private var centerDownTime: Long = 0L
    private var centerLongPressed: Boolean = false
    private var touchDownTime: Long = 0L
    @Volatile private var longPressEnabled: Boolean = true
    private val longPressMs = 500L
    private val baseStepPx = 24f
    private val acceleratedStepPx = 96f

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settings.cursorToggleLongPress.collect { longPressEnabled = it }
        }
    }

    fun attachTarget(view: View?) {
        if (view == null && touchDownTime != 0L) dispatchTouch(MotionEvent.ACTION_CANCEL)
        targetView = view
        if (view == null) setActive(false)
        view?.let { v ->
            v.post {
                _state.value = _state.value.copy(
                    viewportW = v.width,
                    viewportH = v.height,
                    x = if (_state.value.x == 0f) v.width / 2f else _state.value.x.coerceIn(0f, v.width.toFloat()),
                    y = if (_state.value.y == 0f) v.height / 2f else _state.value.y.coerceIn(0f, v.height.toFloat()),
                )
            }
        }
    }

    fun setActive(active: Boolean) {
        _state.value = _state.value.copy(active = active)
    }

    fun handleKeyEvent(
        event: KeyEvent,
        dispatchShortPress: (keyCode: Int) -> Unit = {},
    ): Boolean {
        val code = event.keyCode
        val action = event.action

        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            return handleCenterKey(event, dispatchShortPress)
        }

        if (!_state.value.active) return false

        return when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) moveCursor(code, event.repeatCount)
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (action == KeyEvent.ACTION_UP) setActive(false)
                true
            }
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (action == KeyEvent.ACTION_DOWN) targetView?.scrollBy(0, -200)
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) targetView?.scrollBy(0, 200)
                true
            }
            else -> false
        }
    }

    private fun handleCenterKey(event: KeyEvent, dispatchShortPress: (Int) -> Unit): Boolean {
        val now = SystemClock.uptimeMillis()
        val cursorAvailable = targetView != null
        if (!_state.value.active && (!cursorAvailable || !longPressEnabled)) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    centerDownTime = now
                    centerLongPressed = false
                } else if (cursorAvailable && !centerLongPressed &&
                    (event.isLongPress || now - centerDownTime >= longPressMs)
                ) {
                    centerLongPressed = true
                    if (_state.value.active) dispatchTouch(MotionEvent.ACTION_CANCEL)
                    setActive(!_state.value.active)
                    return true
                }
                if (_state.value.active && !centerLongPressed && event.repeatCount == 0) {
                    dispatchTouch(MotionEvent.ACTION_DOWN)
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                val held = now - centerDownTime
                if (centerLongPressed) {
                    centerLongPressed = false
                    return true
                }
                if (cursorAvailable && held >= longPressMs) {
                    if (_state.value.active) dispatchTouch(MotionEvent.ACTION_CANCEL)
                    setActive(!_state.value.active)
                    return true
                }
                if (_state.value.active) {
                    dispatchTouch(MotionEvent.ACTION_UP)
                    return true
                }
                dispatchShortPress(event.keyCode)
                return true
            }
            else -> return false
        }
    }

    private fun moveCursor(keyCode: Int, repeatCount: Int) {
        val step = if (repeatCount > 2) acceleratedStepPx else baseStepPx
        val s = _state.value
        var nx = s.x; var ny = s.y
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> nx -= step
            KeyEvent.KEYCODE_DPAD_RIGHT -> nx += step
            KeyEvent.KEYCODE_DPAD_UP -> ny -= step
            KeyEvent.KEYCODE_DPAD_DOWN -> ny += step
        }
        nx = nx.coerceIn(0f, s.viewportW.toFloat().coerceAtLeast(0f))
        ny = ny.coerceIn(0f, s.viewportH.toFloat().coerceAtLeast(0f))
        _state.value = s.copy(x = nx, y = ny)
    }

    private fun dispatchTouch(action: Int) {
        val v = targetView ?: return
        val s = _state.value
        val now = SystemClock.uptimeMillis()
        if (action == MotionEvent.ACTION_DOWN) touchDownTime = now
        val downTime = touchDownTime.takeIf { it != 0L } ?: now
        val ev = MotionEvent.obtain(downTime, now, action, s.x, s.y, 0)
        v.dispatchTouchEvent(ev)
        ev.recycle()
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) touchDownTime = 0L
    }
}
