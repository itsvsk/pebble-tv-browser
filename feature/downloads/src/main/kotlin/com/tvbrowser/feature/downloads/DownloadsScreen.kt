package com.tvbrowser.feature.downloads

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.repo.DownloadRepository
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus
import com.tvbrowser.core.downloads.DownloadManagerRepo
import com.tvbrowser.core.downloads.DownloadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repo: DownloadRepository,
    private val dmRepo: DownloadManagerRepo,
) : ViewModel() {

    val records: StateFlow<List<DownloadRecord>> = repo.all()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress: StateFlow<Map<Long, DownloadProgress>> = records
        .flatMapLatest { list ->
            val active = list.filter {
                it.status == DownloadRecord.STATUS_RUNNING || it.status == DownloadRecord.STATUS_PAUSED
            }.map { it.dmId }
            dmRepo.observeProgress(active)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selectedForMenu = MutableStateFlow<DownloadRecord?>(null)
    val selectedForMenu: StateFlow<DownloadRecord?> = _selectedForMenu.asStateFlow()

    fun showMenu(r: DownloadRecord) { _selectedForMenu.value = r }
    fun dismissMenu() { _selectedForMenu.value = null }

    fun cancel(record: DownloadRecord) { dmRepo.cancel(record.dmId) }
    fun open(record: DownloadRecord) { dmRepo.openFile(record) }

    fun deleteRecord(record: DownloadRecord) {
        viewModelScope.launch {
            dmRepo.cancel(record.dmId)
            repo.delete(record.dmId)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DownloadsScreen(vm: DownloadsViewModel = hiltViewModel()) {
    val records by vm.records.collectAsState()
    val progress by vm.progress.collectAsState()
    val menuRecord by vm.selectedForMenu.collectAsState()
    var filter by remember { mutableStateOf("All") }

    val visibleRecords = records.filter { matchesFilter(it, filter) }
    val active = visibleRecords.filter { it.status == DownloadRecord.STATUS_RUNNING || it.status == DownloadRecord.STATUS_PAUSED }
    val done = visibleRecords.filter { it.status == DownloadRecord.STATUS_SUCCESS }
    val failed = visibleRecords.filter { it.status == DownloadRecord.STATUS_FAILED }

    Box(modifier = Modifier.fillMaxSize().background(PebbleColors.Bg)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp5)) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(Brush.linearGradient(listOf(PebbleColors.PrimaryContainer, PebbleColors.Surface2, PebbleColors.Bg)), PebbleShape.Md)
                    .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Md),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("◉  PEBBLE DOWNLOADS", style = PebbleType.Overline, color = PebbleColors.Primary)
                    Spacer(Modifier.height(10.dp))
                    Text("Your files, in one place", style = PebbleType.Display, color = PebbleColors.OnSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("${records.size} files  ·  ${records.count { it.status == DownloadRecord.STATUS_RUNNING }} active",
                        style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                }
            }
            Spacer(Modifier.width(PebbleDimens.Sp4))
            Column(
                modifier = Modifier.width(420.dp).fillMaxHeight()
                    .background(PebbleColors.GlassHigh, PebbleShape.Md)
                    .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Md)
                    .padding(18.dp),
            ) {
                Header(active = active.size, done = done.size)
                Spacer(Modifier.height(PebbleDimens.Sp4))
                DefaultLocationCard(path = "/storage/emulated/0/Download")
                Spacer(Modifier.height(PebbleDimens.Sp4))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("All", "Videos", "Documents", "Packages").forEach { option ->
                        FilterChip(option, filter == option) { filter = option }
                    }
                }
                Spacer(Modifier.height(PebbleDimens.Sp4))
                if (visibleRecords.isEmpty()) {
                    Text(if (records.isEmpty()) "No downloads yet." else "No $filter downloads.",
                        style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
                }
                TvLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3),
                    modifier = Modifier.weight(1f).padding(bottom = PebbleDimens.HintH),
                ) {
                if (active.isNotEmpty()) {
                    item(key = "h-active") { GroupHeader("In progress") }
                    items(active, key = { it.dmId }) { r ->
                        ActiveCard(
                            record = r,
                            progress = progress[r.dmId],
                            onLongClick = { vm.showMenu(r) },
                        )
                    }
                }
                if (done.isNotEmpty()) {
                    item(key = "h-done") { GroupHeader("Completed") }
                    items(done, key = { it.dmId }) { r ->
                        DoneCard(
                            record = r,
                            onOpen = { vm.open(r) },
                            onLongClick = { vm.showMenu(r) },
                        )
                    }
                }
                if (failed.isNotEmpty()) {
                    item(key = "h-failed") { GroupHeader("Failed") }
                    items(failed, key = { it.dmId }) { r ->
                        FailedCard(
                            record = r,
                            onLongClick = { vm.showMenu(r) },
                        )
                    }
                }
                }
            }
        }
    }
    menuRecord?.let { record ->
        DownloadContextMenu(
            record = record,
            onOpen = {
                vm.dismissMenu()
                vm.open(record)
            },
            onCancel = {
                vm.dismissMenu()
                vm.cancel(record)
            },
            onDelete = {
                vm.dismissMenu()
                vm.deleteRecord(record)
            },
            onDismiss = { vm.dismissMenu() },
        )
    }
}

@Composable
private fun Header(active: Int, done: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Downloads", style = PebbleType.Display, color = PebbleColors.OnSurface)
            Text("$active active · $done completed", style = PebbleType.BodySm, color = PebbleColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .background(if (selected) PebbleColors.Primary else PebbleColors.Surface3, PebbleShape.Full)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = PebbleType.LabelSm,
            color = if (selected) PebbleColors.OnPrimary else PebbleColors.OnSurfaceMuted)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Pill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false, tight = true)
            .background(PebbleColors.Surface2, PebbleShape.Full)
            .pebbleClickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = PebbleColors.OnSurfaceMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = PebbleType.Label, color = PebbleColors.OnSurface)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DefaultLocationCard(path: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconWell(Icons.Outlined.Folder)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("DEFAULT DOWNLOAD LOCATION", style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
            Spacer(Modifier.height(2.dp))
            Text(path, style = PebbleType.Mono, color = PebbleColors.OnSurface)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IconWell(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color = PebbleColors.OnSurfaceMuted) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(PebbleColors.Surface3, PebbleShape.Sm),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(label.uppercase(), style = PebbleType.Overline, color = PebbleColors.OnSurfaceFaint)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActiveCard(record: DownloadRecord, progress: DownloadProgress?, onLongClick: () -> Unit) {
    val pct = progress?.fractionOrNull?.let { (it * 100).toInt() } ?: 0
    val paused = record.status == DownloadRecord.STATUS_PAUSED || progress?.statusCode == DownloadManager.STATUS_PAUSED
    val so = progress?.bytesSoFar ?: 0L
    val total = progress?.totalBytes ?: 0L
    val meta = if (paused) "Paused · ${formatBytes(so)}" else "${formatBytes(so)} of ${formatBytes(total)}"
    Row(
        modifier = Modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .pebbleClickable(onClick = onLongClick, onLongClick = onLongClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconWell(Icons.Outlined.InsertDriveFile)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.filename, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(meta, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (progress?.fractionOrNull ?: 0f).coerceIn(0f, 1f) },
                color = if (paused) PebbleColors.Warn else PebbleColors.Primary,
                trackColor = PebbleColors.Surface3,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text("$pct%", style = PebbleType.Label.copy(fontFeatureSettings = "tnum"), color = PebbleColors.OnSurface)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DoneCard(record: DownloadRecord, onOpen: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .pebbleClickable(onClick = onOpen, onLongClick = onLongClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconWell(Icons.Outlined.InsertDriveFile, tint = PebbleColors.Good)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.filename, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text("Done", style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FailedCard(record: DownloadRecord, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = false)
            .fillMaxWidth()
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .pebbleClickable(onClick = onLongClick, onLongClick = onLongClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconWell(Icons.Outlined.ErrorOutline, tint = PebbleColors.Danger)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.filename, style = PebbleType.Body, color = PebbleColors.OnSurface, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text("Failed", style = PebbleType.LabelSm, color = PebbleColors.Danger)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DownloadContextMenu(
    record: DownloadRecord,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val running = record.status == DownloadRecord.STATUS_RUNNING
    val success = record.status == DownloadRecord.STATUS_SUCCESS
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconWell(
                        Icons.Outlined.InsertDriveFile,
                        tint = when {
                            success -> PebbleColors.Good
                            record.status == DownloadRecord.STATUS_FAILED -> PebbleColors.Danger
                            else -> PebbleColors.OnSurfaceMuted
                        },
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.filename, style = PebbleType.Title, color = PebbleColors.OnSurface, maxLines = 2)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            when (record.status) {
                                DownloadRecord.STATUS_RUNNING -> "In progress"
                                DownloadRecord.STATUS_SUCCESS -> "Done"
                                DownloadRecord.STATUS_FAILED -> "Failed"
                                else -> ""
                            },
                            style = PebbleType.BodySm,
                            color = PebbleColors.OnSurfaceMuted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (success) {
                    MenuRow(
                        label = "Open",
                        icon = Icons.Outlined.OpenInNew,
                        onClick = onOpen,
                        modifier = Modifier.focusRequester(firstFocus),
                    )
                    Spacer(Modifier.height(8.dp))
                    MenuRow(
                        label = "Remove from list",
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = onDelete,
                        danger = true,
                    )
                } else if (running) {
                    MenuRow(
                        label = "Cancel download",
                        icon = Icons.Outlined.Cancel,
                        onClick = onCancel,
                        danger = true,
                        modifier = Modifier.focusRequester(firstFocus),
                    )
                } else {
                    MenuRow(
                        label = "Retry",
                        icon = Icons.Outlined.Refresh,
                        onClick = { /* TODO retry — v2 */ },
                        modifier = Modifier.focusRequester(firstFocus),
                    )
                    Spacer(Modifier.height(8.dp))
                    MenuRow(
                        label = "Remove from list",
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = onDelete,
                        danger = true,
                    )
                }
            }
        }
        LaunchedEffect(Unit) { firstFocus.requestFocus() }
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

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "?"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble(); var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format("%.1f %s", v, units[i])
}

private fun matchesFilter(record: DownloadRecord, filter: String): Boolean {
    val name = record.filename.lowercase()
    val mime = record.mime.orEmpty().lowercase()
    return when (filter) {
        "Videos" -> mime.startsWith("video/") || listOf(".mp4", ".mkv", ".webm", ".mov").any(name::endsWith)
        "Documents" -> mime.startsWith("text/") || mime == "application/pdf" ||
            listOf(".pdf", ".doc", ".docx", ".txt", ".epub").any(name::endsWith)
        "Packages" -> listOf(".apk", ".zip", ".tar", ".gz").any(name::endsWith)
        else -> true
    }
}
