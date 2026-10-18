package com.tvbrowser.feature.history

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.repo.BookmarkRepository
import com.tvbrowser.core.db.repo.HistoryRepository
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.seaGlass
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JtTextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository,
    private val tabManager: TabManager,
    private val bookmarks: BookmarkRepository,
) : ViewModel() {
    val entries: StateFlow<List<HistoryEntry>> = repo.recent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarkedUrls: StateFlow<Set<String>> = bookmarks.all()
        .map { list -> list.map { it.url }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _selectedForMenu = MutableStateFlow<HistoryEntry?>(null)
    val selectedForMenu: StateFlow<HistoryEntry?> = _selectedForMenu.asStateFlow()

    fun showMenu(e: HistoryEntry) { _selectedForMenu.value = e }
    fun dismissMenu() { _selectedForMenu.value = null }

    fun open(entry: HistoryEntry, onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(tabManager.newTab(entry.url)) }
    }

    fun toggleBookmark(entry: HistoryEntry) {
        viewModelScope.launch {
            if (bookmarks.isBookmarked(entry.url)) {
                bookmarks.removeByUrl(entry.url)
            } else {
                bookmarks.add(entry.url, entry.title?.takeIf { it.isNotBlank() } ?: entry.url)
            }
        }
    }

    fun deleteEntry(entry: HistoryEntry) {
        viewModelScope.launch { repo.deleteByUrl(entry.url) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clear() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenUrl: (tabId: String) -> Unit,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val entries by vm.entries.collectAsState()
    val bookmarkedUrls by vm.bookmarkedUrls.collectAsState()
    val menuEntry by vm.selectedForMenu.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp8)) {
            Header(onClear = { vm.clearAll() })
            Spacer(Modifier.height(PebbleDimens.Sp6))
            if (entries.isEmpty()) {
                Text("No history yet.", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                return@Column
            }
            val groups = remember(entries) { groupByDay(entries) }
            TvLazyColumn(verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3)) {
                groups.forEach { (label, rows) ->
                    item(key = "h-$label") { DayHeader(label) }
                    items(rows, key = { it.id }) { e ->
                        HistoryRow(
                            entry = e,
                            onOpen = { vm.open(e) { onOpenUrl(it) } },
                            onLongClick = { vm.showMenu(e) },
                        )
                    }
                }
            }
        }
    }
    menuEntry?.let { entry ->
        HistoryContextMenu(
            entry = entry,
            isBookmarked = bookmarkedUrls.contains(entry.url),
            onOpen = {
                vm.dismissMenu()
                vm.open(entry) { onOpenUrl(it) }
            },
            onToggleBookmark = {
                vm.toggleBookmark(entry)
                vm.dismissMenu()
            },
            onDelete = {
                vm.dismissMenu()
                vm.deleteEntry(entry)
            },
            onDismiss = { vm.dismissMenu() },
        )
    }
}

@Composable
private fun Header(onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("History", style = PebbleType.Display, color = PebbleColors.OnSurface)
            Text("Recently visited", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
        }
        Spacer(Modifier.weight(1f))
        ClearPill(onClick = onClear)
    }
}

@Composable
private fun ClearPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .seaGlass(PebbleShape.Full, PebbleColors.GlassRose, 3.dp)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Clear browsing data", style = PebbleType.Label, color = PebbleColors.OnSurface)
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(label.uppercase(Locale.getDefault()), style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: () -> Unit, onLongClick: () -> Unit) {
    val host = UrlUtils.hostOf(entry.url) ?: entry.url
    val time = remember(entry.visitedAt) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.visitedAt), ZoneId.systemDefault()).format(timeFmt)
    }
    Row(
        modifier = Modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .height(72.dp)
            .seaGlass(PebbleShape.Md, PebbleColors.GlassLagoon, 4.dp)
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
            Text(entry.title ?: host, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Text(host, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint, maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        Text(time, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HistoryContextMenu(
    entry: HistoryEntry,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val host = UrlUtils.hostOf(entry.url) ?: entry.url
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
                HistoryWebViewPreview(
                    url = entry.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .seaGlass(RoundedCornerShape(10.dp), PebbleColors.GlassLagoon, 1.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    entry.title?.takeIf { it.isNotBlank() } ?: host,
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
                    label = "Delete",
                    icon = Icons.Outlined.DeleteOutline,
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
private fun HistoryWebViewPreview(url: String, modifier: Modifier = Modifier) {
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

private fun groupByDay(entries: List<HistoryEntry>): List<Pair<String, List<HistoryEntry>>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val zone = ZoneId.systemDefault()
    return entries.groupBy {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it.visitedAt), zone).toLocalDate()
    }.toSortedMap(compareByDescending { it }).map { (date, rows) ->
        val label = when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> "${date.dayOfWeek.getDisplayName(JtTextStyle.SHORT, Locale.getDefault())}, ${date.dayOfMonth} ${date.month.getDisplayName(JtTextStyle.SHORT, Locale.getDefault())}"
        }
        label to rows
    }
}
