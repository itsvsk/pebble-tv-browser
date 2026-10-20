package com.tvbrowser.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwipeRightAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNav(
    active: NavDestination,
    onSelect: (NavDestination) -> Unit,
    focusRequesters: Map<NavDestination, FocusRequester>,
    activeDownFocusRequester: FocusRequester?,
    onFocusedDestinationChanged: (NavDestination, Boolean) -> Unit,
    cursorModeActive: Boolean?,
    onToggleCursor: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    val settingsFocusSource = remember { MutableInteractionSource() }
    val settingsFocused by settingsFocusSource.collectIsFocusedAsState()
    val cursorFocusSource = remember { MutableInteractionSource() }
    val cursorFocused by cursorFocusSource.collectIsFocusedAsState()
    val cursorTint by animateColorAsState(
        if (cursorFocused) PebbleColors.GlassFocus else PebbleColors.GlassLagoon,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "cursor-control-tint",
    )
    val cursorElevation by animateDpAsState(
        if (cursorFocused) 10.dp else 1.dp,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "cursor-control-elevation",
    )
    val settingsGlow by animateFloatAsState(
        if (settingsFocused) 1f else 0f,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "settings-focus-glow",
    )
    val settingsTint by animateColorAsState(
        if (settingsFocused) PebbleColors.Primary else PebbleColors.PrimaryDim,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "settings-icon-tint",
    )
    val settingsElevation by animateDpAsState(
        if (settingsFocused) 10.dp else 0.dp,
        tween(PebbleMotion.DurationMs, easing = PebbleMotion.Standard),
        label = "settings-focus-elevation",
    )
    var focusedPill by remember { mutableStateOf<NavDestination?>(null) }
    var navOrigin by remember { mutableStateOf<Offset?>(null) }
    var pillBounds by remember { mutableStateOf<Map<NavDestination, Rect>>(emptyMap()) }
    val highlightBounds = pillBounds[focusedPill] ?: pillBounds[active]
    val highlightTarget = highlightBounds?.let { bounds ->
        navOrigin?.let { origin ->
            Rect(
                bounds.left - origin.x, bounds.top - origin.y,
                bounds.right - origin.x, bounds.bottom - origin.y,
            )
        }
    }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (focusedPill != null) 1f else 0f,
        animationSpec = tween(PebbleMotion.DurationFastMs),
        label = "nav-highlight-alpha",
    )
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(30_000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(PebbleColors.Surface1.copy(alpha = 0.75f))
            .padding(horizontal = PebbleDimens.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleLogo(size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text("Pebble", style = PebbleType.Title.copy(fontWeight = FontWeight.W700), color = PebbleColors.OnSurface)
        Spacer(Modifier.width(PebbleDimens.Sp5))
        Box(
            modifier = Modifier
                .seaGlass(PebbleShape.Full, PebbleColors.GlassLagoon, 5.dp)
                .onGloballyPositioned { navOrigin = it.boundsInRoot().topLeft }
                .onFocusChanged { if (!it.hasFocus) focusedPill = null }
                .focusGroup(),
        ) {
            if (highlightTarget != null) {
                val highlightRect by animateRectAsState(
                    targetValue = highlightTarget,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "nav-highlight-rect",
                )
                Box(
                    Modifier
                        .size(100.dp, 36.dp)
                        .graphicsLayer {
                            val rect = highlightRect
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = rect.left
                            translationY = rect.top
                            scaleX = if (size.width > 0f) rect.width / size.width else 0f
                            scaleY = if (size.height > 0f) rect.height / size.height else 0f
                            alpha = highlightAlpha
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                        .seaGlass(PebbleShape.Full, PebbleColors.GlassFocus, 10.dp, emphasized = true),
                )
            }
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    NavDestination.Home, NavDestination.Browser, NavDestination.Tabs,
                    NavDestination.Saved, NavDestination.Files,
                ).forEach { destination ->
                    NavPill(
                        destination = destination,
                        selected = active == destination,
                        focusRequester = focusRequesters.getValue(destination),
                        onFocusChanged = { focused ->
                            if (focused) focusedPill = destination
                            onFocusedDestinationChanged(destination, focused)
                        },
                        onPositioned = { bounds ->
                            if (pillBounds[destination] != bounds) {
                                pillBounds = pillBounds + (destination to bounds)
                            }
                        },
                        onLeft = if (destination == NavDestination.Home) {
                            { focusRequesters.getValue(NavDestination.Settings).requestFocus() }
                        } else null,
                        onDown = if (destination == active && activeDownFocusRequester != null) {
                            { activeDownFocusRequester.requestFocus() }
                        } else null,
                        onClick = { onSelect(destination) },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (cursorModeActive != null) {
            Row(
                modifier = Modifier
                    .pebbleFocus(
                        shape = PebbleShape.Full, lift = false, tight = true,
                        interactionSource = cursorFocusSource,
                        ringColorOverride = Color.Transparent,
                        gapColorOverride = Color.Transparent,
                    )
                    .seaGlass(PebbleShape.Full, cursorTint, cursorElevation)
                    .pebbleClickable(onClick = onToggleCursor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (cursorModeActive) Icons.Outlined.Mouse else Icons.Outlined.SwipeRightAlt,
                    contentDescription = if (cursorModeActive) "Pointer mode" else "D-pad mode",
                    tint = PebbleColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    if (cursorModeActive) "Pointer" else "D-pad",
                    style = PebbleType.LabelSm,
                    color = PebbleColors.OnSurface,
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        Text(now.format(DateTimeFormatter.ofPattern("hh:mm a")), style = PebbleType.Label, color = PebbleColors.OnSurface)
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier.size(34.dp)
                .focusRequester(focusRequesters.getValue(NavDestination.Settings))
                .onFocusChanged { onFocusedDestinationChanged(NavDestination.Settings, it.isFocused) }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                        focusRequesters.getValue(NavDestination.Home).requestFocus()
                        true
                    } else false
                }
                .pebbleFocus(
                    shape = CircleShape, lift = false, tight = true,
                    interactionSource = settingsFocusSource,
                    ringColorOverride = Color.Transparent,
                    gapColorOverride = Color.Transparent,
                )
                .shadow(
                    settingsElevation,
                    CircleShape,
                    ambientColor = PebbleColors.Primary.copy(alpha = 0.55f * settingsGlow),
                    spotColor = PebbleColors.Primary.copy(alpha = 0.35f * settingsGlow),
                )
                .background(PebbleColors.GlassFocus.copy(alpha = 0.7f * settingsGlow), CircleShape)
                .border(1.dp, Color.White.copy(alpha = settingsGlow), CircleShape)
                .pebbleClickable(onClick = { onSelect(NavDestination.Settings) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = settingsTint, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavPill(
    destination: NavDestination,
    selected: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onPositioned: (Rect) -> Unit,
    onLeft: (() -> Unit)?,
    onDown: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    val dark = PebbleColors.IsDark
    val textColor = when {
        selected && dark -> PebbleColors.PrimaryStrong
        selected || focused -> PebbleColors.OnPrimaryContainer
        else -> PebbleColors.OnSurfaceMuted
    }
    Box(
        modifier = Modifier.width(96.dp).height(36.dp)
            .focusRequester(focusRequester)
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .then(if (onLeft != null || onDown != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (onLeft != null) { onLeft(); true } else false
                    Key.DirectionDown -> if (onDown != null) { onDown(); true } else false
                    else -> false
                }
            } else Modifier)
            .padding(PebbleDimens.FocusRingTight + PebbleDimens.FocusGapTight)
            .focusable(interactionSource = focusSource)
            .then(if (selected && dark) Modifier.shadow(
                8.dp, PebbleShape.Full,
                ambientColor = PebbleColors.Primary.copy(alpha = 0.48f),
                spotColor = PebbleColors.Primary.copy(alpha = 0.32f),
            ) else Modifier)
            .then(when {
                selected -> Modifier.background(
                    Brush.verticalGradient(listOf(
                        if (dark) PebbleColors.GlassFocus.copy(alpha = 0.78f)
                        else PebbleColors.GlassMint.copy(alpha = 0.80f),
                        PebbleColors.PrimaryContainer.copy(alpha = if (dark) 0.92f else 0.58f),
                    )),
                    PebbleShape.Full,
                )
                else -> Modifier
            })
            .pebbleClickable(onClick = onClick)
            .padding(PebbleDimens.Sp1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            destination.label,
            style = PebbleType.LabelSm.copy(fontWeight = if (selected) FontWeight.W700 else FontWeight.W500),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
