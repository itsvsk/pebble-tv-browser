package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    homeFocusRequester: FocusRequester,
    cursorModeActive: Boolean?,
    onToggleCursor: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
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
            .background(PebbleColors.Glass)
            .border(0.5.dp, PebbleColors.GlassEdge)
            .padding(horizontal = PebbleDimens.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PebbleLogo(size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text("Pebble", style = PebbleType.Title.copy(fontWeight = FontWeight.W700), color = PebbleColors.OnSurface)
        Spacer(Modifier.width(30.dp))
        Row(
            modifier = Modifier
                .background(PebbleColors.GlassHigh, PebbleShape.Full)
                .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Full)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(
                NavDestination.Home, NavDestination.Browser, NavDestination.Tabs,
                NavDestination.Saved, NavDestination.Files,
            ).forEach { destination ->
                NavPill(
                    destination = destination,
                    selected = active == destination,
                    focusRequester = if (destination == NavDestination.Home) homeFocusRequester else null,
                    onClick = { onSelect(destination) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (cursorModeActive != null) {
            Row(
                modifier = Modifier
                    .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
                    .background(PebbleColors.GlassHigh, PebbleShape.Full)
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
                .pebbleFocus(shape = CircleShape, lift = false, tight = true)
                .background(PebbleColors.PrimaryStrong, CircleShape)
                .pebbleClickable(onClick = { onSelect(NavDestination.Settings) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = PebbleColors.OnPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavPill(
    destination: NavDestination,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.height(36.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .background(if (selected) PebbleColors.Primary else PebbleColors.GlassHigh, PebbleShape.Full)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            destination.label,
            style = PebbleType.Label.copy(fontWeight = if (selected) FontWeight.W700 else FontWeight.W500),
            color = if (selected) PebbleColors.OnPrimary else PebbleColors.OnSurfaceMuted,
        )
    }
}
