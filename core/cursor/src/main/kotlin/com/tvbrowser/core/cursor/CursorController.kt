package com.tvbrowser.core.cursor

import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class CursorState(
    val active: Boolean = false,
    val preferredActive: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val viewportW: Int = 0,
    val viewportH: Int = 0,
)

data class CursorMode(val active: Boolean = false, val preferredActive: Boolean = false)

/**
 * Hybrid spatial-focus + virtual-cursor controller.
 *
 * - Long-press DPAD_CENTER (>500ms) toggles cursor mode on/off
 * - In cursor mode: arrow keys move pointer; CENTER taps; BACK exits
 * - Outside cursor mode: returns false so spatial focus traversal works normally
 */
@Singleton
class CursorController @Inject constructor() {

    private val _state = MutableStateFlow(CursorState())
    val state: StateFlow<CursorState> = _state.asStateFlow()
    val mode = state.map { CursorMode(it.active, it.preferredActive) }.distinctUntilChanged()

    private var targetView: View? = null
    private var focusControls: (() -> Unit)? = null
    private var onWebNavigation: (() -> Unit)? = null
    private var centerDownTime: Long = 0L
    private var centerLongPressed: Boolean = false
    private val longPressMs = 500L
    private var scrollAnimator: ValueAnimator? = null
    private var scrollTargetX = 0
    private var scrollTargetY = 0
    private var lastScrollDirectionX = 0
    private var lastScrollDirectionY = 0
    private var pointerPositionInitialized = false
    private var pointerTouchDownTime = 0L
    private var pointerTouchDown = false
    private var pointerUpHandoffInProgress = false
    private val layoutListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        if (targetView === view) updateViewport(view)
    }

    fun attachTarget(view: View?) {
        if (targetView !== view && pointerTouchDown) dispatchTouch(MotionEvent.ACTION_CANCEL)
        scrollAnimator?.cancel()
        scrollAnimator = null
        targetView?.removeOnLayoutChangeListener(layoutListener)
        targetView = view
        view?.let { v ->
            v.addOnLayoutChangeListener(layoutListener)
            v.post {
                if (targetView !== v) return@post
                updateViewport(v)
            }
        }
    }

    fun detachTarget(view: View) {
        if (targetView === view) {
            if (pointerTouchDown) dispatchTouch(MotionEvent.ACTION_CANCEL)
            scrollAnimator?.cancel()
            scrollAnimator = null
            view.removeOnLayoutChangeListener(layoutListener)
            targetView = null
        }
    }

    fun setActive(active: Boolean) {
        updateMode(active, _state.value.preferredActive)
    }

    fun setPreferredActive(active: Boolean) {
        updateMode(active, active)
    }

    private fun updateMode(active: Boolean, preferredActive: Boolean) {
        if (!active && pointerTouchDown) dispatchTouch(MotionEvent.ACTION_CANCEL)
        _state.value = _state.value.copy(active = active, preferredActive = preferredActive)
    }

    private fun updateViewport(view: View) {
        if (view.width <= 0 || view.height <= 0) return
        _state.value = _state.value.withViewport(
            view.width,
            view.height,
            pointerPositionInitialized,
        )
        pointerPositionInitialized = true
    }

    fun setControlsFocusHandler(handler: (() -> Unit)?) {
        focusControls = handler
    }

    fun setWebNavigationHandler(handler: (() -> Unit)?) {
        onWebNavigation = handler
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        val action = event.action
        val view = targetView

        if (pointerUpHandoffInProgress && code == KeyEvent.KEYCODE_DPAD_UP) {
            if (action == KeyEvent.ACTION_UP) pointerUpHandoffInProgress = false
            return true
        }
        if (code == KeyEvent.KEYCODE_DPAD_UP && action == KeyEvent.ACTION_DOWN &&
            view != null && focusControls != null &&
            shouldReturnToControlsFromPointer(_state.value, view?.canScrollVertically(-1) == true)
        ) {
            pointerUpHandoffInProgress = true
            scrollAnimator?.cancel()
            setActive(false)
            focusControls?.invoke()
            return true
        }

        if (action == KeyEvent.ACTION_DOWN &&
            (_state.value.active || view?.hasFocus() == true) &&
            code in listOf(
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN,
            )
        ) {
            onWebNavigation?.invoke()
        }

        if (code == KeyEvent.KEYCODE_MENU && focusControls != null) {
            if (action == KeyEvent.ACTION_UP) {
                setActive(false)
                focusControls?.invoke()
            }
            return true
        }

        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            return handleCenterKey(event)
        }

        val webScroll = webDpadScrollDelta(
            code,
            view?.height ?: 0,
            _state.value.active,
            view?.hasFocus() == true,
        )
        if (webScroll != 0) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (view?.canScrollVertically(webScroll.sign()) == true) {
                    animateWebScroll(view, deltaY = webScroll)
                } else if (webScroll < 0) {
                    focusControls?.invoke()
                }
            }
            return true
        }

        if (!_state.value.active) return false

        return when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) moveCursor(code, event.repeatCount)
                true
            }
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (action == KeyEvent.ACTION_DOWN) targetView?.let { animateWebScroll(it, deltaY = -200) }
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) targetView?.let { animateWebScroll(it, deltaY = 200) }
                true
            }
            else -> false
        }
    }

    private fun handleCenterKey(event: KeyEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        val cursorAvailable = targetView != null
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    centerDownTime = now
                    centerLongPressed = false
                } else if (cursorAvailable && !centerLongPressed &&
                    (event.isLongPress || now - centerDownTime >= longPressMs)
                ) {
                    centerLongPressed = true
                    setPreferredActive(!_state.value.active)
                    return true
                }
                if (_state.value.active && !centerLongPressed && event.repeatCount == 0) {
                    dispatchTouch(MotionEvent.ACTION_DOWN)
                }
                return _state.value.active || (cursorAvailable && event.repeatCount > 0)
            }
            KeyEvent.ACTION_UP -> {
                val held = now - centerDownTime
                if (centerLongPressed) {
                    centerLongPressed = false
                    return true
                }
                if (_state.value.active) {
                    onWebNavigation?.invoke()
                    dispatchTouch(MotionEvent.ACTION_UP)
                    return true
                }
                if (cursorAvailable && held >= longPressMs && !centerLongPressed) {
                    centerLongPressed = true
                    setPreferredActive(true)
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    private fun moveCursor(keyCode: Int, repeatCount: Int) {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> CursorDirection.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> CursorDirection.Right
            KeyEvent.KEYCODE_DPAD_UP -> CursorDirection.Up
            else -> CursorDirection.Down
        }
        val motion = planCursorMotion(_state.value, direction, repeatCount)
        _state.value = _state.value.copy(x = motion.x, y = motion.y)
        if (motion.scrollX != 0 || motion.scrollY != 0) {
            targetView?.let { view ->
                val scrollX = motion.scrollX.takeIf { view.canScrollHorizontally(it.sign()) } ?: 0
                val scrollY = motion.scrollY.takeIf { view.canScrollVertically(it.sign()) } ?: 0
                if (scrollX != 0 || scrollY != 0) animateWebScroll(view, scrollX, scrollY)
            }
        }
    }

    private fun animateWebScroll(view: View, deltaX: Int = 0, deltaY: Int = 0) {
        val startX = view.scrollX
        val startY = view.scrollY
        val running = scrollAnimator?.isRunning == true
        val directionX = deltaX.sign()
        val directionY = deltaY.sign()
        val targetX = when {
            deltaX == 0 -> if (running) scrollTargetX else startX
            running && lastScrollDirectionX == directionX -> scrollTargetX + deltaX
            else -> startX + deltaX
        }
        val targetY = when {
            deltaY == 0 -> if (running) scrollTargetY else startY
            running && lastScrollDirectionY == directionY -> scrollTargetY + deltaY
            else -> startY + deltaY
        }
        scrollAnimator?.cancel()
        scrollTargetX = targetX
        scrollTargetY = targetY
        if (deltaX != 0) lastScrollDirectionX = directionX
        if (deltaY != 0) lastScrollDirectionY = directionY
        scrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220L
            addUpdateListener {
                val progress = it.animatedValue as Float
                view.scrollTo(
                    startX + ((targetX - startX) * progress).toInt(),
                    startY + ((targetY - startY) * progress).toInt(),
                )
            }
            start()
        }
    }

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }

    private fun dispatchTouch(action: Int) {
        val v = targetView
        if (v == null) {
            pointerTouchDown = false
            return
        }
        val s = _state.value
        val now = SystemClock.uptimeMillis()
        if (action == MotionEvent.ACTION_DOWN) {
            pointerTouchDownTime = now
            pointerTouchDown = true
        } else if (!pointerTouchDown) return
        val ev = MotionEvent.obtain(pointerTouchDownTime, now, action, s.x, s.y, 0)
        v.dispatchTouchEvent(ev)
        ev.recycle()
        if (action != MotionEvent.ACTION_DOWN) pointerTouchDown = false
    }
}
