package com.tvbrowser.feature.tabs

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.seaGlass
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleElevation
import com.tvbrowser.core.design.PebbleMotion
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TabsOverviewScreen(
    topNavFocusRequester: FocusRequester,
    newTabFocusRequester: FocusRequester,
    onOpenTab: (tabId: String) -> Unit,
    onNewTab: (String) -> Unit,
    vm: TabsViewModel = hiltViewModel(),
) {
    val tabs by vm.tabs.collectAsState()
    val bookmarkedUrls by vm.bookmarkedUrls.collectAsState()
    val menuTab by vm.selectedForMenu.collectAsState()
    val currentTabId = tabs.maxByOrNull { it.lastActiveAt }?.id
    var showCloseAll by remember { mutableStateOf(false) }
    val firstTabFocusRequester = remember { FocusRequester() }
    val tabListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var newTabFocused by remember { mutableStateOf(false) }
    val firstTabScrollJob = remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(newTabFocused) {
        if (!newTabFocused) firstTabScrollJob.value?.cancel()
    }
    val focusFirstTab: () -> Unit = {
        if (firstTabScrollJob.value?.isActive != true) {
            firstTabScrollJob.value = scope.launch {
                if (tabListState.firstVisibleItemIndex != 0 || tabListState.firstVisibleItemScrollOffset != 0) {
                    tabListState.animateScrollToItem(0)
                }
                withFrameNanos { }
                if (newTabFocused) firstTabFocusRequester.requestFocus()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = PebbleDimens.Sp8),
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = PebbleDimens.Gutter)) {
                Header(
                    count = tabs.size,
                    newTabFocusRequester = newTabFocusRequester,
                    onNewTabFocusedChanged = { newTabFocused = it },
                    onUp = { topNavFocusRequester.requestFocus() },
                    onDown = focusFirstTab,
                    onNewTab = { vm.newTab(onNewTab) },
                    onCloseAll = { if (tabs.isNotEmpty()) showCloseAll = true },
                )
            }
            Spacer(Modifier.height(PebbleDimens.Sp4))
            LazyRow(
                state = tabListState,
                horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp1),
                contentPadding = PaddingValues(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp5),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(count = tabs.size, key = { tabs[it].id }) { index ->
                    val tab = tabs[index]
                    TabCard(
                        tab = tab,
                        isCurrent = tab.id == currentTabId,
                        onClick = { onOpenTab(tab.id) },
                        onClose = { vm.close(tab) },
                        onLongClick = { vm.showMenu(tab) },
                        focusRequester = firstTabFocusRequester.takeIf { index == 0 },
                        onUp = { newTabFocusRequester.requestFocus() },
                    )
                }
                item {
                    NewTabCard(
                        onClick = { vm.newTab(onNewTab) },
                        focusRequester = firstTabFocusRequester.takeIf { tabs.isEmpty() },
                        onUp = { newTabFocusRequester.requestFocus() },
                    )
                }
            }
        }
    }
    menuTab?.let { tab ->
        TabContextMenu(
            tab = tab,
            isBookmarked = bookmarkedUrls.contains(tab.url),
            onOpen = {
                vm.dismissMenu()
                onOpenTab(tab.id)
            },
            onToggleBookmark = {
                vm.toggleBookmark(tab)
                vm.dismissMenu()
            },
            onClose = {
                vm.dismissMenu()
                vm.close(tab)
            },
            onDismiss = { vm.dismissMenu() },
        )
    }
    if (showCloseAll) {
        ConfirmDialog(
            title = "Close all tabs?",
            message = "This will close all ${tabs.size} open tabs. This cannot be undone.",
            confirmLabel = "Close all",
            onConfirm = {
                val snapshot = tabs.toList()
                showCloseAll = false
                snapshot.forEach(vm::close)
            },
            onCancel = { showCloseAll = false },
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
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

@OptIn(ExperimentalTvMaterial3Api::class)
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Header(
    count: Int,
    newTabFocusRequester: FocusRequester,
    onNewTabFocusedChanged: (Boolean) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onNewTab: () -> Unit,
    onCloseAll: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Active Tabs", style = PebbleType.Display, color = PebbleColors.OnSurface)
        Spacer(Modifier.width(PebbleDimens.Sp3))
        Text("$count", style = PebbleType.Label, color = PebbleColors.OnPrimaryContainer,
            modifier = Modifier.seaGlass(PebbleShape.Full, PebbleColors.GlassMint, 2.dp, emphasized = true).padding(horizontal = 10.dp, vertical = 3.dp))
        Spacer(Modifier.weight(1f))
        Pill(
            label = "New tab", icon = Icons.Outlined.Add, primary = true,
            focusRequester = newTabFocusRequester, onUp = onUp, onDown = onDown,
            onFocusChanged = onNewTabFocusedChanged,
            onClick = onNewTab,
        )
        Spacer(Modifier.width(PebbleDimens.Sp3))
        Pill(label = "Close all", icon = Icons.Outlined.Close, primary = false, onClick = onCloseAll)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Pill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
) {
    val fg = if (primary) PebbleColors.OnPrimaryContainer else PebbleColors.OnSurface
    Row(
        modifier = Modifier
            .height(48.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .then(if (onUp != null || onDown != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> if (onUp != null) { onUp(); true } else false
                    Key.DirectionDown -> if (onDown != null) { onDown(); true } else false
                    else -> false
                }
            } else Modifier)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .seaGlass(PebbleShape.Full, if (primary) PebbleColors.GlassMint else PebbleColors.GlassLagoon, 3.dp, emphasized = primary)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = PebbleType.Label, color = fg)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabCard(
    tab: TabEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: FocusRequester?,
    onUp: () -> Unit,
) {
    val host = UrlUtils.hostOf(tab.url) ?: tab.url
    val isHome = tab.url.isBlank() || tab.url == "about:blank"
    val displayTitle = tab.title?.takeIf { it.isNotBlank() }
        ?: if (isHome) "New tab" else host.removePrefix("www.")
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    val glowAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = PebbleMotion.DurationMs),
        label = "tab-card-glow",
    )
    Box(modifier = Modifier.tabBloom(glowAlpha).padding(10.dp)) {
        // Fade the backing with the bloom so it never appears in a single frame.
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    PebbleElevation.E3,
                    PebbleShape.Md,
                    ambientColor = PebbleColors.Primary.copy(alpha = 0.85f * glowAlpha),
                    spotColor = PebbleColors.Primary.copy(alpha = 0.85f * glowAlpha),
                )
                .background(PebbleColors.Bg.copy(alpha = glowAlpha), PebbleShape.Md),
        )
        Column(
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        onUp()
                        true
                    } else false
                }
                .pebbleFocus(shape = PebbleShape.Md, lift = false, interactionSource = focusSource, gapWidth = 0.dp)
                .seaGlass(PebbleShape.Md, if (isCurrent) PebbleColors.GlassMint else PebbleColors.GlassLagoon, 4.dp)
                .pebbleClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(PebbleDimens.Sp3)
                .width(178.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabFavicon(host = host, isHome = isHome, size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    host,
                    style = PebbleType.LabelSm,
                    color = PebbleColors.OnSurfaceMuted,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .pebbleFocus(shape = CircleShape, lift = false, tight = true)
                        .seaGlass(CircleShape, PebbleColors.GlassRose, 1.dp)
                        .pebbleClickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = PebbleColors.OnSurfaceMuted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(PebbleDimens.Sp3))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .seaGlass(PebbleShape.Sm, PebbleColors.GlassMint, 2.dp)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    TabFavicon(host = host, isHome = isHome, size = 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        displayTitle,
                        style = PebbleType.Title,
                        color = PebbleColors.OnSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (isCurrent) "●  Current tab" else "Open tab", style = PebbleType.LabelSm,
                color = if (isCurrent) PebbleColors.Primary else PebbleColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun TabFavicon(host: String, isHome: Boolean, size: androidx.compose.ui.unit.Dp) {
    if (isHome) {
        Box(
            modifier = Modifier.size(size).seaGlass(PebbleShape.Sm, PebbleColors.GlassLagoon, 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = null,
                tint = PebbleColors.OnSurfaceMuted,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    } else {
        AsyncImage(
            model = "https://www.google.com/s2/favicons?domain=$host&sz=128",
            contentDescription = null,
            modifier = Modifier.size(size).clip(PebbleShape.Sm),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NewTabCard(
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
    onUp: () -> Unit,
) {
    val dashColor = PebbleColors.Outline
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    val glowAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = PebbleMotion.DurationMs),
        label = "new-tab-glow",
    )
    Box(
        modifier = Modifier
            .width(198.dp)
            .height(290.dp)
            .tabBloom(glowAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        onUp()
                        true
                    } else false
                }
                .pebbleFocus(shape = PebbleShape.Md, lift = false, interactionSource = focusSource, gapWidth = 0.dp)
                .seaGlass(PebbleShape.Md, PebbleColors.GlassLagoon, 4.dp)
                .pebbleClickable(onClick = onClick)
                .width(178.dp)
                .height(270.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
                    ),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = PebbleColors.OnSurfaceMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text("New tab", style = PebbleType.Label, color = PebbleColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun Modifier.tabBloom(alpha: Float): Modifier {
    val primary = PebbleColors.Primary
    return drawBehind {
        // Keep the halo in a rounded layer behind the card instead of using a hardware shadow.
        if (alpha > 0f) {
            listOf(0.dp to 0.04f, 4.dp to 0.08f, 7.dp to 0.12f).forEach { (inset, strength) ->
                val px = inset.toPx()
                drawRoundRect(
                    color = primary.copy(alpha = strength * alpha),
                    topLeft = Offset(px, px),
                    size = Size(size.width - 2 * px, size.height - 2 * px),
                    cornerRadius = CornerRadius((26.dp - inset).toPx()),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabContextMenu(
    tab: TabEntity,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val host = UrlUtils.hostOf(tab.url) ?: tab.url
    val openFocus = remember { FocusRequester() }
    val isHome = tab.url.isBlank() || tab.url == "about:blank"
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                modifier = Modifier
                    .width(540.dp)
                    .padding(end = PebbleDimens.Sp4)
                    .seaGlass(PebbleShape.Lg, PebbleColors.GlassLagoon, 12.dp)
                    .padding(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .seaGlass(RoundedCornerShape(10.dp), PebbleColors.GlassLagoon, 1.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isHome) {
                        Icon(
                            Icons.Outlined.Home,
                            contentDescription = null,
                            tint = PebbleColors.OnSurfaceFaint,
                            modifier = Modifier.size(64.dp),
                        )
                    } else {
                        TabWebViewPreview(url = tab.url, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    tab.title?.takeIf { it.isNotBlank() } ?: host,
                    style = PebbleType.Title,
                    color = PebbleColors.OnSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(host, style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted, maxLines = 1)
                Spacer(Modifier.height(16.dp))
                MenuRow(
                    label = "Open",
                    icon = Icons.Outlined.OpenInNew,
                    onClick = onOpen,
                    modifier = Modifier.focusRequester(openFocus),
                )
                Spacer(Modifier.height(8.dp))
                MenuRow(
                    label = if (isBookmarked) "Remove bookmark" else "Save to bookmarks",
                    icon = if (isBookmarked) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkAdd,
                    onClick = onToggleBookmark,
                )
                Spacer(Modifier.height(8.dp))
                MenuRow(
                    label = "Close tab",
                    icon = Icons.Outlined.Delete,
                    onClick = onClose,
                    danger = true,
                )
            }
        }
        LaunchedEffect(Unit) { openFocus.requestFocus() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val tint = if (danger) PebbleColors.Danger else PebbleColors.OnSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pebbleFocus(shape = PebbleShape.Md, lift = false, tight = true)
            .seaGlass(PebbleShape.Md, if (danger) PebbleColors.GlassRose else PebbleColors.GlassLagoon, 3.dp)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = PebbleType.Body, color = tint)
    }
}

@Composable
private fun TabWebViewPreview(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setInitialScale(50)
                isFocusable = false
                isFocusableInTouchMode = false
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
        update = { wv -> if (wv.url != url) wv.loadUrl(url) },
        onRelease = { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.onPause()
            wv.removeAllViews()
            wv.destroy()
        },
    )
}
