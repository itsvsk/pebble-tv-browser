package com.tvbrowser.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.common.ThemePreference
import com.tvbrowser.core.db.repo.HistoryRepository
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.seaGlass
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val history: HistoryRepository,
) : ViewModel() {
    val desktopUa: StateFlow<Boolean> = settings.desktopUa
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val adblock: StateFlow<Boolean> = settings.adblockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val cursorLongPress: StateFlow<Boolean> = settings.cursorToggleLongPress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val theme: StateFlow<ThemePreference> = settings.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.System)

    fun setDesktopUa(v: Boolean) = viewModelScope.launch { settings.set(PrefKeys.DesktopUa, v) }
    fun setAdblock(v: Boolean) = viewModelScope.launch { settings.set(PrefKeys.AdblockEnabled, v) }
    fun setCursorLongPress(v: Boolean) = viewModelScope.launch { settings.set(PrefKeys.CursorToggleLongPress, v) }
    fun setTheme(v: ThemePreference) = viewModelScope.launch { settings.setTheme(v) }

    fun clearHistory() = viewModelScope.launch { history.clear() }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onOpenDownloads: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val desktop by vm.desktopUa.collectAsState()
    val ad by vm.adblock.collectAsState()
    val longPress by vm.cursorLongPress.collectAsState()
    val theme by vm.theme.collectAsState()
    val firstFocus = remember { FocusRequester() }
    var showClearHistory by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp8),
        ) {
            Header()
            Spacer(Modifier.height(PebbleDimens.Sp4))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
                contentPadding = PaddingValues(horizontal = PebbleDimens.Sp2, vertical = PebbleDimens.Sp2),
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxWidth()
                    .focusProperties { enter = { firstFocus } },
            ) {
                item { SectionHeader("Appearance") }
                item {
                    ThemeOptionRow(
                        mode = ThemePreference.System,
                        subtitle = "Match your TV appearance",
                        selected = theme == ThemePreference.System,
                        icon = Icons.Outlined.BrightnessAuto,
                        onClick = vm::setTheme,
                        modifier = Modifier.focusRequester(firstFocus),
                    )
                }
                item {
                    ThemeOptionRow(
                        mode = ThemePreference.Light,
                        subtitle = "Warm sand and sea glass",
                        selected = theme == ThemePreference.Light,
                        icon = Icons.Outlined.LightMode,
                        onClick = vm::setTheme,
                    )
                }
                item {
                    ThemeOptionRow(
                        mode = ThemePreference.Dark,
                        subtitle = "Deep sea glass for low light",
                        selected = theme == ThemePreference.Dark,
                        icon = Icons.Outlined.DarkMode,
                        onClick = vm::setTheme,
                    )
                }
                item { SectionHeader("Page rendering") }
                item {
                    ToggleRow(
                        icon = Icons.Outlined.DesktopWindows,
                        title = "Request desktop site",
                        subtitle = "Default user-agent for new tabs",
                        value = desktop,
                        onChange = vm::setDesktopUa,
                    )
                }
                item {
                    ToggleRow(
                        icon = Icons.Outlined.AdsClick,
                        title = "Block ads & trackers",
                        subtitle = "Filter known networks via host rules",
                        value = ad,
                        onChange = vm::setAdblock,
                    )
                }
                item { SectionHeader("Input") }
                item {
                    ToggleRow(
                        icon = Icons.Outlined.Mouse,
                        title = "Long-press OK to toggle cursor",
                        subtitle = "Hold OK to switch between D-pad and cursor",
                        value = longPress,
                        onChange = vm::setCursorLongPress,
                    )
                }
                item { SectionHeader("Downloads") }
                item {
                    ActionRow(
                        icon = Icons.Outlined.Folder,
                        title = "View downloads",
                        subtitle = "Saved in Pebble app storage",
                        onClick = onOpenDownloads,
                    )
                }
                item { SectionHeader("Privacy") }
                item {
                    ActionRow(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "Clear browsing history",
                        subtitle = "Remove all visited pages from this device",
                        onClick = { showClearHistory = true },
                    )
                }
            }
        }
        if (showClearHistory) {
            ConfirmDialog(
                title = "Clear browsing history?",
                message = "All visited pages will be removed from this device. This cannot be undone.",
                confirmLabel = "Clear",
                onConfirm = {
                    vm.clearHistory()
                    showClearHistory = false
                },
                onCancel = { showClearHistory = false },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemeOptionRow(
    mode: ThemePreference,
    subtitle: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    SetRow(
        icon = icon,
        title = mode.label,
        subtitle = subtitle,
        onClick = { onClick(mode) },
        modifier = modifier.semantics { this.selected = selected },
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = PebbleColors.Primary,
                modifier = Modifier.size(24.dp),
            )
        } else Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun Header() {
    Column {
        Text("Settings", style = PebbleType.Display, color = PebbleColors.OnSurface)
        Text("Pebble · version 1.0", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(label.uppercase(), style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SetRow(icon = icon, title = title, subtitle = subtitle, onClick = { onChange(!value) }, modifier = modifier) {
        PebbleSwitch(checked = value, onChange = onChange)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SetRow(icon = icon, title = title, subtitle = subtitle, onClick = onClick) {
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = PebbleColors.OnSurfaceFaint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 4.dp)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .seaGlass(PebbleShape.Sm, PebbleColors.GlassLagoon, 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PebbleColors.OnSurfaceMuted, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun PebbleSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val knobX by animateDpAsState(
        targetValue = if (checked) 26.dp else 2.dp,
        animationSpec = tween(durationMillis = 180),
        label = "switchKnob",
    )
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 34.dp)
            .seaGlass(CircleShape, if (checked) PebbleColors.GlassFocus else PebbleColors.GlassLagoon, 2.dp, emphasized = checked),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX)
                .size(30.dp)
                .seaGlass(CircleShape, PebbleColors.GlassHigh, 2.dp),
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val confirmFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 12.dp)
                    .padding(24.dp),
            ) {
                Text(title, style = PebbleType.TitleLg, color = PebbleColors.OnSurface)
                Spacer(Modifier.height(PebbleDimens.Sp3))
                Text(message, style = PebbleType.Body, color = PebbleColors.OnSurfaceMuted)
                Spacer(Modifier.height(PebbleDimens.Sp6))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3, Alignment.End),
                ) {
                    DialogButton(label = "Cancel", danger = false, onClick = onCancel)
                    DialogButton(
                        label = confirmLabel,
                        danger = true,
                        onClick = onConfirm,
                        modifier = Modifier.focusRequester(confirmFocus),
                    )
                }
            }
        }
        LaunchedEffect(Unit) { confirmFocus.requestFocus() }
    }
}

@Composable
private fun DialogButton(
    label: String,
    danger: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fg = if (danger) PebbleColors.Danger else PebbleColors.OnSurface
    Box(
        modifier = modifier
            .height(48.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .seaGlass(PebbleShape.Full, if (danger) PebbleColors.GlassRose else PebbleColors.GlassLagoon, 3.dp)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PebbleType.Label, color = fg)
    }
}
