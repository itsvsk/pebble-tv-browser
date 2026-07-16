package com.tvbrowser.feature.bookmarks

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
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
    vm: BookmarksViewModel = hiltViewModel(),
) {
    val bookmarks by vm.bookmarks.collectAsState()
    val menuBm by vm.selectedForMenu.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(PebbleColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp8)) {
            Header(count = bookmarks.size)
            Spacer(Modifier.height(PebbleDimens.Sp6))
            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet.", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                return@Column
            }
            FolderHeader(label = "All bookmarks")
            Spacer(Modifier.height(PebbleDimens.Sp3))
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
                verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
                modifier = Modifier.fillMaxSize(),
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
        Text("$count saved", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
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
    Row(
        modifier = Modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .height(72.dp)
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .pebbleClickable(onClick = onOpen, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = "https://www.google.com/s2/favicons?domain=$host&sz=64",
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(PebbleShape.Sm),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(bm.title, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Text(host, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint, maxLines = 1)
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
                    .background(PebbleColors.Surface1, PebbleShape.Lg)
                    .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Lg)
                    .padding(18.dp),
            ) {
                BookmarkWebViewPreview(
                    url = bookmark.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(PebbleColors.Surface3, RoundedCornerShape(10.dp))
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
private fun BookmarkWebViewPreview(url: String, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
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
