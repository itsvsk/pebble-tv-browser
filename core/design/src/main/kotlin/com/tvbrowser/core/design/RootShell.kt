package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens

/**
 * Persistent app shell: left nav rail (116dp) + content + bottom key-hint bar (56dp).
 * The hint bar is rendered as an overlay on top of content (with a gradient protection)
 * so screens have their full vertical space for layout.
 */
@Composable
fun RootShell(
    active: NavDestination,
    onSelect: (NavDestination) -> Unit,
    hints: List<KeyHint> = listOf(
        KeyHint("▲▼◀▶", "Navigate"),
        KeyHint("OK", "Select"),
        KeyHint("Back", "Return"),
    ),
    showRail: Boolean = true,
    showHints: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PebbleColors.Bg),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                NavRail(active = active, onSelect = onSelect)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
        if (showHints) {
            KeyHintBar(
                hints = hints,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
