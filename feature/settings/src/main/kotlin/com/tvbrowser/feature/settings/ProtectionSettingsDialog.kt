package com.tvbrowser.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvbrowser.core.adblock.FilterListStatus
import com.tvbrowser.core.adblock.FilterStatus
import com.tvbrowser.core.common.SiteAdblockOverride
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import com.tvbrowser.core.design.seaGlass
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ProtectionSettingsDialog(
    enabled: Boolean,
    status: FilterStatus,
    overrides: List<SiteAdblockOverride>,
    onSetEnabled: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onResetSite: (String) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val doneFocus = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(PebbleDimens.Sp6), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(max = maxHeight)
                    .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 8.dp)
                    .padding(PebbleDimens.Sp5).testTag("protection-settings-dialog"),
                verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
            ) {
                Text("Ad & tracker protection", style = PebbleType.TitleLg, color = PebbleColors.OnSurface)
                LazyColumn(
                    Modifier.weight(1f, fill = false).fillMaxWidth(),
                    contentPadding = PaddingValues(PebbleDimens.Sp2),
                    verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
                ) {
                    item(key = "default") {
                        ProtectionAction(
                            title = "Default protection: ${if (enabled) "On" else "Off"}",
                            detail = "Applies to websites without a saved override. Reload open pages after changing this default.",
                            onClick = { onSetEnabled(!enabled) },
                            modifier = Modifier.fillMaxWidth().focusRequester(firstFocus).testTag("protection-default")
                                .semantics {
                                    stateDescription = if (enabled) "On" else "Off"
                                    toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off
                                },
                            role = Role.Switch,
                        )
                        LaunchedEffect(Unit) {
                            if (!initialFocusRequested) {
                                // Lazy items attach during subcomposition, after the dialog itself.
                                withFrameNanos { }
                                firstFocus.requestFocus()
                                initialFocusRequested = true
                            }
                        }
                    }
                    item(key = "refresh") {
                        ProtectionAction(
                            title = if (status.refreshing) "Updating filter lists…" else "Update filter lists now",
                            detail = if (status.ready) "Automatic updates also run in the background." else "Loading saved filters. Bundled protection is active.",
                            onClick = { if (!status.refreshing) onRefresh() },
                            modifier = Modifier.fillMaxWidth().testTag("protection-update"),
                        )
                    }
                    items(status.lists, key = { it.source.name }) { FilterStatusCard(it) }
                    item(key = "scope") {
                        Text(
                            "Filters supported network requests to known ad and tracker hosts. Some ads, in-stream ads, cosmetic elements and service-worker requests may remain.",
                            style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted,
                        )
                    }
                    item(key = "overrides-title") {
                        Text("WEBSITE OVERRIDES", style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
                    }
                    item(key = "reset-all") {
                        ProtectionAction(
                            title = "Reset all website protection overrides",
                            detail = if (overrides.isEmpty()) "No overrides saved. Website display modes are kept." else "${overrides.size} saved · Returns these websites to the global default. Display modes are kept.",
                            onClick = {
                                if (overrides.isNotEmpty()) {
                                    doneFocus.requestFocus()
                                    onResetAll()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("protection-reset-all"),
                        )
                    }
                    items(overrides, key = { "site:${it.host}" }) { site ->
                        ProtectionAction(
                            title = site.host,
                            detail = "Protection ${if (site.enabled) "On" else "Off"} · Select to use the global default",
                            onClick = {
                                // The fixed footer remains mounted when a focused lazy-list row is removed.
                                doneFocus.requestFocus()
                                onResetSite("https://${site.host}")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("protection-override-${site.host}"),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ProtectionAction(
                        title = "Done", onClick = onDismiss,
                        modifier = Modifier.widthIn(min = 120.dp).focusRequester(doneFocus).testTag("protection-done"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterStatusCard(list: FilterListStatus) {
    val updateLabel = remember(list.updatedAtMillis) {
        list.updatedAtMillis?.let { "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))}" }
            ?: "No successful download yet"
    }
    Column(
        Modifier.fillMaxWidth().testTag("protection-list-${list.source.name}")
            .pebbleFocus(PebbleShape.Md, lift = false).semantics(mergeDescendants = true) {}
            .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 2.dp).padding(PebbleDimens.Sp3),
        verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp1),
    ) {
        Text(list.source.title, style = PebbleType.Body, color = PebbleColors.OnSurface)
        Text("${list.ruleCount} supported rules · ${if (list.bundled) "Bundled fallback" else "Downloaded list"}",
            style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
        Text(updateLabel, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
        if (list.unsupportedRules > 0) Text("${list.unsupportedRules} unsupported rules handled conservatively",
            style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
        list.error?.let { Text(it, style = PebbleType.LabelSm, color = PebbleColors.Danger) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProtectionAction(
    title: String,
    detail: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
) {
    Column(
        modifier.pebbleFocus(PebbleShape.Md, lift = false)
            .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 2.dp)
            .pebbleClickable(onClick = onClick, onClickLabel = title, role = role)
            .padding(horizontal = PebbleDimens.Sp4, vertical = PebbleDimens.Sp3),
        verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp1),
    ) {
        Text(title, style = PebbleType.Label, color = PebbleColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        detail?.let { Text(it, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted) }
    }
}
