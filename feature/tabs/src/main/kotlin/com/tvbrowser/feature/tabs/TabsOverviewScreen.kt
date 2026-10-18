package com.tvbrowser.feature.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.TabEntity
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleElevation
import com.tvbrowser.core.design.PebbleMotion
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.shape.RoundedCornerShape as RcShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TabsOverviewScreen(
    onOpenTab: (tabId: String) -> Unit,
    onNewTab: (String) -> Unit,
    vm: TabsViewModel = hiltViewModel(),
) {
    val tabs by vm.tabs.collectAsState()
    val bookmarkedUrls by vm.bookmarkedUrls.collectAsState()
    val menuTab by vm.selectedForMenu.collectAsState()
    val currentTabId = tabs.maxByOrNull { it.lastActiveAt }?.id
    var showCloseAll by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp8),
        ) {
            Header(
                count = tabs.size,
                onNewTab = { vm.newTab(onNewTab) },
                onCloseAll = { if (tabs.isNotEmpty()) showCloseAll = true },
            )
            Spacer(Modifier.height(PebbleDimens.Sp10))
            TvLazyRow(
                horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp4),
                modifier = Modifier.fillMaxWidth().height(316.dp),
            ) {
                items(count = tabs.size, key = { tabs[it].id }) { index ->
                    val tab = tabs[index]
                    TabCard(
                        tab = tab,
                        isCurrent = tab.id == currentTabId,
                        onClick = { onOpenTab(tab.id) },
                        onClose = { vm.close(tab) },
                        onLongClick = { vm.showMenu(tab) },
                    )
                }
                item { NewTabCard(onClick = { vm.newTab(onNewTab) }) }
            }
            Spacer(Modifier.height(PebbleDimens.Sp6))
            Text("OK  Open tab       Hold OK  Tab actions       BACK  Return", style = PebbleType.Label, color = PebbleColors.OnSurfaceMuted)
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
                    .background(PebbleColors.Surface1, PebbleShape.Lg)
                    .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Lg)
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
    val bg = if (danger) PebbleColors.Danger.copy(alpha = 0.18f) else PebbleColors.Surface2
    val fg = if (danger) PebbleColors.Danger else PebbleColors.OnSurface
    Box(
        modifier = modifier
            .height(48.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .background(bg, PebbleShape.Full)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PebbleType.Label, color = fg)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Header(count: Int, onNewTab: () -> Unit, onCloseAll: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Active Tabs", style = PebbleType.Display, color = PebbleColors.OnSurface)
        Spacer(Modifier.width(PebbleDimens.Sp3))
        Text("$count", style = PebbleType.Label, color = PebbleColors.OnPrimary,
            modifier = Modifier.background(PebbleColors.Primary, PebbleShape.Full).padding(horizontal = 10.dp, vertical = 3.dp))
        Spacer(Modifier.weight(1f))
        Pill(label = "New tab", icon = Icons.Outlined.Add, primary = true, onClick = onNewTab)
        Spacer(Modifier.width(PebbleDimens.Sp3))
        Pill(label = "Close all", icon = Icons.Outlined.Close, primary = false, onClick = onCloseAll)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean, onClick: () -> Unit) {
    val bg = if (primary) PebbleColors.PrimaryContainer else PebbleColors.Surface2
    val fg = if (primary) PebbleColors.OnPrimaryContainer else PebbleColors.OnSurface
    Row(
        modifier = Modifier
            .height(48.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .background(bg, PebbleShape.Full)
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
        if (focused) {
            // An opaque backing keeps the soft shadow behind the tab content.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        PebbleElevation.E3,
                        PebbleShape.Md,
                        ambientColor = PebbleColors.Primary.copy(alpha = 0.85f),
                        spotColor = PebbleColors.Primary.copy(alpha = 0.85f),
                    )
                    .background(PebbleColors.Bg, PebbleShape.Md),
            )
        }
        Column(
            modifier = Modifier
                .pebbleFocus(shape = PebbleShape.Md, lift = false, interactionSource = focusSource)
                .background(PebbleColors.Bg, PebbleShape.Md)
                .background(if (isCurrent) PebbleColors.GlassHigh else PebbleColors.Glass, PebbleShape.Md)
                .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) PebbleColors.Primary else PebbleColors.GlassEdge, PebbleShape.Md)
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
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(PebbleColors.PrimaryContainer, PebbleColors.Surface3, PebbleColors.Surface2)
                    ), PebbleShape.Sm)
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
            modifier = Modifier.size(size).background(PebbleColors.Surface3, PebbleShape.Sm),
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
private fun NewTabCard(onClick: () -> Unit) {
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
                .pebbleFocus(shape = PebbleShape.Md, lift = false, interactionSource = focusSource)
                .background(PebbleColors.Glass, PebbleShape.Md)
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

private fun Modifier.tabBloom(alpha: Float): Modifier = drawBehind {
    // Keep the halo in a rounded layer behind the card instead of using a hardware shadow.
    if (alpha > 0f) {
        listOf(0.dp to 0.04f, 4.dp to 0.08f, 7.dp to 0.12f).forEach { (inset, strength) ->
            val px = inset.toPx()
            drawRoundRect(
                color = PebbleColors.Primary.copy(alpha = strength * alpha),
                topLeft = Offset(px, px),
                size = Size(size.width - 2 * px, size.height - 2 * px),
                cornerRadius = CornerRadius((26.dp - inset).toPx()),
            )
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
                    .background(PebbleColors.Surface1, PebbleShape.Lg)
                    .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Lg)
                    .padding(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(PebbleColors.Surface3, RcShape(10.dp))
                        .clip(RcShape(10.dp)),
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
            .background(PebbleColors.Surface2, PebbleShape.Md)
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
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
    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {}
            override fun onResume(owner: LifecycleOwner) {}
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@Suppress("unused")
private val unusedRefs = arrayOf(SolidColor(PebbleColors.Primary), 14.sp)
