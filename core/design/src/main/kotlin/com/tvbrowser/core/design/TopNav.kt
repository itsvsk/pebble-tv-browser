package com.tvbrowser.core.design

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
        Spacer(Modifier.width(30.dp))
        Row(
            modifier = Modifier
                .seaGlass(PebbleShape.Full, PebbleColors.GlassLagoon, 5.dp)
                .padding(4.dp),
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
                    onFocusChanged = { focused -> onFocusedDestinationChanged(destination, focused) },
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
                    .then(if (cursorFocused) Modifier.seaGlass(
                        PebbleShape.Full, PebbleColors.GlassFocus, 10.dp, emphasized = true,
                    ) else Modifier.seaGlass(PebbleShape.Full, PebbleColors.GlassLagoon, 1.dp))
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
                .then(if (settingsFocused) Modifier.seaGlass(
                    CircleShape, PebbleColors.GlassFocus, 10.dp, emphasized = true,
                ) else Modifier)
                .pebbleClickable(onClick = { onSelect(NavDestination.Settings) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = if (settingsFocused) PebbleColors.Primary else PebbleColors.PrimaryDim, modifier = Modifier.size(18.dp))
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
    onLeft: (() -> Unit)?,
    onDown: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier.height(36.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .then(if (onLeft != null || onDown != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (onLeft != null) { onLeft(); true } else false
                    Key.DirectionDown -> if (onDown != null) { onDown(); true } else false
                    else -> false
                }
            } else Modifier)
            .pebbleFocus(
                shape = PebbleShape.Full, lift = false, tight = true,
                interactionSource = focusSource,
                ringColorOverride = Color.Transparent,
                gapColorOverride = Color.Transparent,
            )
            .then(when {
                focused -> Modifier.seaGlass(PebbleShape.Full, PebbleColors.GlassFocus, 11.dp, emphasized = true)
                selected -> Modifier.background(
                    Brush.verticalGradient(listOf(
                        PebbleColors.GlassMint.copy(alpha = 0.80f),
                        PebbleColors.PrimaryContainer.copy(alpha = 0.58f),
                    )),
                    PebbleShape.Full,
                )
                else -> Modifier
            })
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            destination.label,
            style = PebbleType.Label.copy(fontWeight = if (selected || focused) FontWeight.W700 else FontWeight.W500),
            color = if (selected || focused) PebbleColors.OnPrimaryContainer else PebbleColors.OnSurfaceMuted,
        )
    }
}
