package com.tvbrowser.core.design

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Two-part focus ring: inner gap (bg color) + outer ring (primary).
 * Card-like elements additionally lift: translateY(-3dp) + scale(1.03) + E2 shadow.
 * Reserves ring space always so layout doesn't shift when focus arrives.
 *
 * Use [tight] for rail/menu items (2dp+2dp instead of 3dp+3dp).
 */
@Composable
fun Modifier.pebbleFocus(
    shape: Shape = PebbleShape.Md,
    lift: Boolean = true,
    tight: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val gap = if (tight) PebbleDimens.FocusGapTight else PebbleDimens.FocusGap
    val ring = if (tight) PebbleDimens.FocusRingTight else PebbleDimens.FocusRing
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()

    val ringColor = if (focused) PebbleColors.Primary else Color.Transparent
    val gapColor = if (focused) PebbleColors.Bg else Color.Transparent

    val scale by animateFloatAsState(
        if (focused && lift) PebbleDimens.FocusLiftScale else 1f,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "pebble-focus-scale",
    )
    val ty by animateDpAsState(
        if (focused && lift) PebbleDimens.FocusLiftY else 0.dp,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "pebble-focus-ty",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = ty.toPx()
        }
        .then(
            if (focused && lift) Modifier.shadow(
                PebbleElevation.E2,
                shape,
                ambientColor = PebbleColors.Primary.copy(alpha = 0.30f),
                spotColor = PebbleColors.Primary.copy(alpha = 0.30f),
            ) else Modifier
        )
        .border(ring, ringColor, shape)
        .padding(ring)
        .border(gap, gapColor, shape)
        .padding(gap)
        .focusable(interactionSource = src)
}

/**
 * Single-focusable activation modifier.
 *
 * Use AFTER [pebbleFocus] to wire CENTER/Enter (and touch tap) to onClick without adding
 * a second focusable node — [androidx.compose.foundation.clickable] would do that and
 * cause "two D-pad presses to activate" because focus traversal walks into the inner focusable
 * first, then activates on second press.
 *
 * Long-press: ≥500ms hold of CENTER fires onLongClick instead of onClick.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.pebbleClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    longPressMs: Long = 500L,
): Modifier = composed {
    var downAt by remember { mutableStateOf(0L) }
    var longFired by remember { mutableStateOf(false) }
    this
        .onPreviewKeyEvent { e ->
            if (e.key != Key.DirectionCenter && e.key != Key.Enter && e.key != Key.NumPadEnter) {
                return@onPreviewKeyEvent false
            }
            when (e.type) {
                KeyEventType.KeyDown -> {
                    if (e.nativeKeyEvent.repeatCount == 0) {
                        downAt = SystemClock.uptimeMillis()
                        longFired = false
                        true
                    } else if (onLongClick != null && !longFired &&
                        (e.nativeKeyEvent.isLongPress ||
                            SystemClock.uptimeMillis() - downAt >= longPressMs)
                    ) {
                        longFired = true
                        onLongClick()
                        true
                    } else true
                }
                KeyEventType.KeyUp -> {
                    // Ignore stray KeyUp with no matching KeyDown — happens when long-press on
                    // another element opens a dialog that steals focus, and the CENTER release
                    // is delivered to the newly-focused element here.
                    if (downAt == 0L) return@onPreviewKeyEvent false
                    val held = SystemClock.uptimeMillis() - downAt
                    downAt = 0L
                    when {
                        longFired -> { longFired = false; true }
                        onLongClick != null && held >= longPressMs -> { onLongClick(); true }
                        else -> { onClick(); true }
                    }
                }
                else -> false
            }
        }
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = onLongClick?.let { lc -> { lc() } },
            )
        }
}

