package com.tvbrowser.feature.bookmarks

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.seaGlass
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleElevation
import com.tvbrowser.core.design.PebbleMotion
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import com.tvbrowser.core.webview.TabManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepo: BookmarkRepository,
    private val tabManager: TabManager,
) : ViewModel() {
    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepo.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedForMenu = MutableStateFlow<Bookmark?>(null)
    val selectedForMenu: StateFlow<Bookmark?> = _selectedForMenu.asStateFlow()

    fun showMenu(bm: Bookmark) { _selectedForMenu.value = bm }
    fun dismissMenu() { _selectedForMenu.value = null }

    fun open(bookmark: Bookmark, onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(tabManager.newTab(bookmark.url)) }
    }

    fun delete(bookmark: Bookmark) {
        viewModelScope.launch { bookmarkRepo.remove(bookmark) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onOpenUrl: (tabId: String) -> Unit,
    onOpenHistory: () -> Unit,
    vm: BookmarksViewModel = hiltViewModel(),
) {
    val bookmarks by vm.bookmarks.collectAsState()
    val menuBm by vm.selectedForMenu.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp5)) {
            Column(
                modifier = Modifier.width(198.dp).fillMaxHeight()
                    .seaGlass(PebbleShape.Md, PebbleColors.GlassLagoon)
                    .padding(14.dp),
            ) {
                Text("Library", style = PebbleType.Title, color = PebbleColors.OnSurface)
                Text("Your collection", style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
                Spacer(Modifier.height(PebbleDimens.Sp6))
                LibrarySideItem("Bookmarks", "${bookmarks.size} saved sites", true, onClick = {})
                Spacer(Modifier.height(PebbleDimens.Sp2))
                LibrarySideItem("History", "Recently visited", false, onClick = onOpenHistory)
            }
            Spacer(Modifier.width(PebbleDimens.Sp4))
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .seaGlass(PebbleShape.Md, if (PebbleColors.IsDark) PebbleColors.GlassLagoon else PebbleColors.GlassRose)
                    .padding(20.dp),
            ) {
                Header(count = bookmarks.size)
                Spacer(Modifier.height(PebbleDimens.Sp2))
                if (bookmarks.isEmpty()) {
                    Text("No bookmarks yet. Save a page from the browser to see it here.",
                        style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp2),
                        verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp2),
                        contentPadding = PaddingValues(horizontal = PebbleDimens.Sp4, vertical = PebbleDimens.Sp5),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            BookmarkRow(
                                bm = bm,
                                onOpen = { vm.open(bm) { onOpenUrl(it) } },
                                onLongClick = { vm.showMenu(bm) },
                            )
                        }
                    }
                }
            }
        }
    }
    menuBm?.let { bm ->
        BookmarkContextMenu(
            bookmark = bm,
            onOpen = {
                vm.dismissMenu()
                vm.open(bm) { onOpenUrl(it) }
            },
            onDelete = {
                vm.dismissMenu()
                vm.delete(bm)
            },
            onDismiss = { vm.dismissMenu() },
        )
    }
}

@Composable
private fun Header(count: Int) {
    Column {
        Text("Bookmarks", style = PebbleType.Display, color = PebbleColors.OnSurface)
        Text("Favorites & sites you saved  ·  $count items", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
    }
}

@Composable
private fun LibrarySideItem(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .pebbleFocus(shape = PebbleShape.Sm, lift = false, tight = true)
            .seaGlass(PebbleShape.Sm, if (selected) PebbleColors.GlassMint else PebbleColors.GlassLagoon, 3.dp, emphasized = selected)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = PebbleType.Label, color = if (selected) PebbleColors.Primary else PebbleColors.OnSurface)
        Text(detail, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted, maxLines = 1)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FolderHeader(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = PebbleColors.OnSurfaceFaint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label.uppercase(), style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BookmarkRow(bm: Bookmark, onOpen: () -> Unit, onLongClick: () -> Unit) {
    val host = UrlUtils.hostOf(bm.url) ?: bm.url
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    val glowAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = PebbleMotion.DurationMs),
        label = "bookmark-glow",
    )
    Box(modifier = Modifier.fillMaxWidth().bookmarkBloom(glowAlpha).padding(6.dp)) {
        Box(
            modifier = Modifier.matchParentSize()
                .shadow(
                    PebbleElevation.E2,
                    PebbleShape.Md,
                    ambientColor = PebbleColors.Primary.copy(alpha = 0.45f * glowAlpha),
                    spotColor = PebbleColors.Primary.copy(alpha = 0.45f * glowAlpha),
                )
                .background(PebbleColors.Bg.copy(alpha = glowAlpha), PebbleShape.Md),
        )
        Row(
            modifier = Modifier
                .pebbleFocus(shape = PebbleShape.Md, lift = false, interactionSource = focusSource, gapWidth = 0.dp)
                .fillMaxWidth()
                .height(104.dp)
                .seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 4.dp)
                .pebbleClickable(onClick = onOpen, onLongClick = onLongClick)
                .padding(PebbleDimens.Sp3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(56.dp)
                    .seaGlass(PebbleShape.Sm, PebbleColors.GlassMint, 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = "https://www.google.com/s2/favicons?domain=$host&sz=128",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(PebbleShape.Sm),
                )
            }
            Spacer(Modifier.width(PebbleDimens.Sp3))
            Column(Modifier.weight(1f)) {
                Text(bm.title, style = PebbleType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.W600),
                    color = PebbleColors.OnSurface, maxLines = 2)
                Spacer(Modifier.height(PebbleDimens.Sp1))
                Text(host, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Modifier.bookmarkBloom(alpha: Float): Modifier {
    val primary = PebbleColors.Primary
    return drawBehind {
        if (alpha > 0f) {
            listOf(0.dp to 0.025f, 2.dp to 0.04f, 4.dp to 0.06f).forEach { (inset, strength) ->
                val px = inset.toPx()
                drawRoundRect(
                    color = primary.copy(alpha = strength * alpha),
                    topLeft = Offset(px, px),
                    size = Size(size.width - 2 * px, size.height - 2 * px),
                    cornerRadius = CornerRadius((22.dp - inset).toPx()),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BookmarkContextMenu(
    bookmark: Bookmark,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val host = UrlUtils.hostOf(bookmark.url) ?: bookmark.url
    val openFocus = remember { FocusRequester() }
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
                BookmarkWebViewPreview(
                    url = bookmark.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .seaGlass(RoundedCornerShape(10.dp), PebbleColors.GlassLagoon, 1.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    bookmark.title.takeIf { it.isNotBlank() } ?: host,
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
                    label = "Remove bookmark",
                    icon = Icons.Outlined.BookmarkRemove,
                    onClick = onDelete,
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
private fun BookmarkWebViewPreview(url: String, modifier: Modifier = Modifier) {
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
