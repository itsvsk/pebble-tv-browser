package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens

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
    homeFocusRequester: FocusRequester,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PebbleColors.Surface2, PebbleColors.Bg, PebbleColors.Bg))),
    ) {
        if (showRail) TopNav(active = active, onSelect = onSelect, homeFocusRequester = homeFocusRequester)
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
            if (showHints) KeyHintBar(hints = hints, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
