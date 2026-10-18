package com.tvbrowser.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

/**
 * Persistent glass navigation and key hints shared by all native screens.
 */
@Composable
fun RootShell(
    active: NavDestination,
    onSelect: (NavDestination) -> Unit,
    hints: List<KeyHint> = listOf(
        KeyHint("OK", "Select"),
        KeyHint("BACK", "Return / Exit"),
        KeyHint("D-PAD", "Navigate"),
    ),
    showRail: Boolean = true,
    showHints: Boolean = true,
    focusRequesters: Map<NavDestination, FocusRequester>,
    onFocusedDestinationChanged: (NavDestination, Boolean) -> Unit,
    cursorModeActive: Boolean?,
    onToggleCursor: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                // Sea Glass uses a warm canvas with diffused color passing through it.
                val topGlow = Brush.radialGradient(
                    colors = listOf(
                        PebbleColors.GlassMint.copy(alpha = 0.75f),
                        PebbleColors.GlassMint.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.70f, -size.height * 0.12f),
                    radius = size.width * 0.78f,
                )
                val sideGlow = Brush.radialGradient(
                    colors = listOf(PebbleColors.GlassLagoon.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(-size.width * 0.10f, size.height * 0.64f),
                    radius = size.width * 0.62f,
                )
                onDrawBehind {
                    drawRect(PebbleColors.Bg)
                    drawRect(topGlow)
                    drawRect(sideGlow)
                }
            },
    ) {
        // Keep navigation above the WebView's first paint during browser transitions.
        if (showRail) Box(modifier = Modifier.zIndex(1f)) {
            TopNav(
                active = active,
                onSelect = onSelect,
                focusRequesters = focusRequesters,
                onFocusedDestinationChanged = onFocusedDestinationChanged,
                cursorModeActive = cursorModeActive,
                onToggleCursor = onToggleCursor,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
            if (showHints) KeyHintBar(hints = hints, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
