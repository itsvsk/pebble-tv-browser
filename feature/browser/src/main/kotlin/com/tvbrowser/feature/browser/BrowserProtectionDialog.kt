package com.tvbrowser.feature.browser

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvbrowser.core.adblock.FilterStatus
import com.tvbrowser.core.design.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun BrowserProtectionDialog(
    ui: BrowserUi,
    status: FilterStatus,
    onPause: () -> Unit,
    onToggleSite: () -> Unit,
    onResetSite: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight)
                .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 8.dp).padding(20.dp)
                .testTag("browser-protection-dialog"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ad & tracker protection", style = PebbleType.Title, color = PebbleColors.OnSurface)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ui.siteHost ?: "Current page", style = PebbleType.Body, color = PebbleColors.OnSurface)
                    Text(when {
                        !ui.adblockEnabled -> "Protection off for this website"
                        ui.protectionPaused -> "Paused for this visit"
                        else -> "Protection on"
                    }, style = PebbleType.Label, color = PebbleColors.Primary)
                    Text("${ui.blockedRequests} requests blocked on this page", style = PebbleType.BodySm,
                        color = PebbleColors.OnSurfaceMuted, modifier = Modifier.testTag("browser-blocked-count"))
                    if (ui.siteHost != null) {
                        if (ui.adblockEnabled) ProtectionButton(if (ui.protectionPaused) "Resume protection" else "Pause for this visit",
                            "browser-protection-pause", onPause)
                        Text("Pause applies only to this tab and host, including reloads. It ends when you leave this host or close the tab.",
                            style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
                        ProtectionButton(if (ui.adblockEnabled) "Always turn off for this website" else "Always turn on for this website",
                            "browser-protection-site", onToggleSite, Modifier.focusRequester(firstFocus))
                        if (ui.adblockOverridden) ProtectionButton("Use global protection default", "browser-protection-reset", { firstFocus.requestFocus(); onResetSite() })
                        Text(if (ui.adblockOverridden) "Saved website override" else "Using global protection default",
                            style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
                    }
                    Text(when {
                        status.refreshing -> "Updating filter lists…"
                        !status.ready -> "Bundled protection active; loading saved lists…"
                        status.lists.any { it.error != null } -> "Some lists could not update. Previous protection is still active."
                        status.lists.any { it.bundled } -> "Bundled fallback active. Update lists for broader coverage."
                        else -> "Downloaded filter lists active"
                    }, style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                    ProtectionButton(if (status.refreshing) "Updating…" else "Update filter lists now", "browser-protection-update",
                        { if (!status.refreshing) onRefresh() })
                    Text("Host-based network protection. Some ads and service-worker requests may remain. Manage defaults and saved website overrides in Settings.",
                        style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
                }
                ProtectionButton("Done", "browser-protection-done", onDismiss)
            }
            LaunchedEffect(Unit) {
                androidx.compose.runtime.withFrameNanos { }
                if (ui.siteHost != null) firstFocus.requestFocus()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtectionButton(label: String, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().testTag(tag).pebbleFocus(PebbleShape.Md, lift = false)
        .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 2.dp)
        .pebbleClickable(onClick = onClick, onClickLabel = label).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(label, style = PebbleType.Label, color = PebbleColors.OnSurface)
    }
}
