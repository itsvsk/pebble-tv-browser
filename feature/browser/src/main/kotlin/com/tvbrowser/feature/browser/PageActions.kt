package com.tvbrowser.feature.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tvbrowser.core.design.*
import com.tvbrowser.core.webview.TabMediaState
import com.tvbrowser.core.webview.WebPageError
import androidx.tv.material3.Text
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PageMenu(ui: BrowserUi, media: TabMediaState, onDesktop: () -> Unit, onBlock: () -> Unit,
    onReset: () -> Unit, onMute: () -> Unit, onBackground: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.width(480.dp).heightIn(max = 480.dp)
            .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 8.dp)
            .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Page options", style = PebbleType.Title, color = PebbleColors.OnSurface)
            Text(ui.siteHost ?: "Current tab", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
            PageAction(if (media.muted) "Unmute tab" else "Mute tab", "page-mute", onMute, Modifier.focusRequester(focus))
            PageAction("Background playback: ${if (media.backgroundPlayback) "Allowed" else "Off"}", "page-background", onBackground)
            Text("Applies to this tab for this session. Some sites pause when hidden.", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
            if (ui.siteHost != null) {
                Text(if (ui.siteOverridden) "Settings saved for this website" else "Using global defaults",
                    style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                PageAction("Site mode: ${if (ui.desktopUa) "Desktop" else "Mobile"}", "page-site-mode", onDesktop)
                PageAction("Block ads & trackers: ${if (ui.adblockEnabled) "On" else "Off"}", "page-site-blocking", onBlock)
                if (ui.siteOverridden) PageAction("Reset website to defaults", "page-site-reset", onReset)
            }
            PageAction("Close", "page-menu-close", onDismiss)
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PageErrorPanel(error: WebPageError, onRetry: () -> Unit, onEdit: () -> Unit, onHome: () -> Unit) {
    val focus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(PebbleColors.Bg).testTag("browser-page-error"), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp).seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 8.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (error.rendererGone) "Page needs to reload" else "Unable to load this page", style = PebbleType.Title, color = PebbleColors.OnSurface)
            Text(error.message, style = PebbleType.Body, color = PebbleColors.OnSurfaceMuted)
            PageAction("Retry", "browser-error-retry", onRetry, Modifier.focusRequester(focus))
            PageAction("Edit address", "browser-error-edit", onEdit)
            PageAction("Home", "browser-error-home", onHome)
        }
        LaunchedEffect(error) { focus.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PageAction(label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().testTag(tag).pebbleFocus(PebbleShape.Md, lift = false)
        .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 2.dp)
        .pebbleClickable(onClick = onClick, onClickLabel = label).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(label, style = PebbleType.Label, color = PebbleColors.OnSurface)
    }
}
