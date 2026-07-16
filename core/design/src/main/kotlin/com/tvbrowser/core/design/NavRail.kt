package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType

enum class NavDestination(val label: String, val outlined: ImageVector, val filled: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tabs("Tabs", Icons.Outlined.Tab, Icons.Filled.Tab),
    Saved("Saved", Icons.Outlined.Bookmark, Icons.Filled.Bookmark),
    History("History", Icons.Outlined.History, Icons.Filled.History),
    Files("Files", Icons.Outlined.Download, Icons.Filled.Download),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/**
 * Persistent 116dp left navigation rail.
 * Top: pebble logo. Items: Home / Tabs / Saved / History / Files.
 * Pushed to bottom: Settings.
 *
 * Active pill = primary-container bg + filled icon + on-primary-container.
 * Inactive = transparent + outlined icon + on-surface-muted.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NavRail(
    active: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(PebbleDimens.RailW)
            .background(PebbleColors.Surface),
    ) {
        // right-edge hairline
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.CenterEnd)
                .background(PebbleColors.OutlineFaint),
        )

        Column(
            modifier = Modifier.fillMaxHeight().padding(vertical = PebbleDimens.Sp6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PebbleLogo(size = 52.dp)
            Spacer(Modifier.height(PebbleDimens.Sp10))

            val topItems = listOf(
                NavDestination.Home, NavDestination.Tabs, NavDestination.Saved,
                NavDestination.History, NavDestination.Files,
            )
            topItems.forEach { dest ->
                RailItem(dest, active == dest) { onSelect(dest) }
                Spacer(Modifier.height(PebbleDimens.Sp2))
            }

            Spacer(Modifier.weight(1f))
            RailItem(NavDestination.Settings, active == NavDestination.Settings) {
                onSelect(NavDestination.Settings)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailItem(dest: NavDestination, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) PebbleColors.PrimaryContainer else Color.Transparent
    val tint = if (active) PebbleColors.OnPrimaryContainer else PebbleColors.OnSurfaceMuted
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 60.dp)
            .pebbleFocus(shape = RoundedCornerShape(18.dp), lift = false, tight = true)
            .background(bg, RoundedCornerShape(18.dp))
            .pebbleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (active) dest.filled else dest.outlined,
                contentDescription = dest.label,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                dest.label,
                style = PebbleType.LabelSm.copy(fontSize = 11.sp),
                color = tint,
            )
        }
    }
}
