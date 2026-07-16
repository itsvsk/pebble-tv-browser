package com.tvbrowser.feature.home

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.tvbrowser.core.common.UrlUtils
import com.tvbrowser.core.db.entity.MostVisitedView
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleLogo
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleFocus
import com.tvbrowser.core.design.pebbleClickable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenUrl: (tabId: String) -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val mostVisited by vm.mostVisited.collectAsState()
    val bookmarked by vm.bookmarkedUrls.collectAsState()
    val menuItem by vm.selectedForMenu.collectAsState()
    val omniboxFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    var query by remember { mutableStateOf("") }
    var privateMode by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
        if (spoken != null) {
            query = spoken
            vm.openInNewTab(spoken, isIncognito = privateMode) { tabId -> onOpenUrl(tabId) }
        }
    }
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
        }
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PebbleColors.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark()
            Spacer(Modifier.height(PebbleDimens.Sp10))
            Hero(now = now)
            Spacer(Modifier.height(PebbleDimens.Sp10))
            SearchField(
                value = query,
                onValueChange = { query = it },
                onSubmit = {
                    if (query.isNotBlank()) {
                        vm.openInNewTab(query, isIncognito = privateMode) { tabId -> onOpenUrl(tabId) }
                    }
                },
                onVoice = { voiceLauncher.launch(voiceIntent) },
                privateMode = privateMode,
                onTogglePrivate = { privateMode = !privateMode },
                focusRequester = omniboxFocus,
            )
            Spacer(Modifier.height(PebbleDimens.Sp10))
            MostVisitedSection(
                items = mostVisited,
                onClick = { item ->
                    vm.openInNewTab(item.url) { tabId -> onOpenUrl(tabId) }
                },
                onLongClick = { item -> vm.showMenu(item) },
            )
        }
        menuItem?.let { item ->
            TileContextMenu(
                item = item,
                isBookmarked = bookmarked.contains(item.url),
                onOpen = {
                    vm.dismissMenu()
                    vm.openInNewTab(item.url) { tabId -> onOpenUrl(tabId) }
                },
                onToggleBookmark = { vm.toggleBookmark(item) },
                onDelete = { vm.deleteEntry(item) },
                onDismiss = { vm.dismissMenu() },
            )
        }
    }
}

@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PebbleLogo(size = 28.dp)
        Spacer(Modifier.width(12.dp))
        Row {
            val tracking = 4.sp
            Text("PEBB", style = PebbleType.Title.copy(fontWeight = FontWeight.W600, letterSpacing = tracking, fontSize = 20.sp), color = PebbleColors.OnSurface.copy(alpha = 0.92f))
            Text("L", style = PebbleType.Title.copy(fontWeight = FontWeight.W600, letterSpacing = tracking, fontSize = 20.sp), color = PebbleColors.PrimaryStrong)
            Text("E", style = PebbleType.Title.copy(fontWeight = FontWeight.W600, letterSpacing = tracking, fontSize = 20.sp), color = PebbleColors.OnSurface.copy(alpha = 0.92f))
        }
    }
}

@Composable
private fun Hero(now: Long) {
    val locale = Locale.getDefault()
    val clockText = remember(now, locale) { SimpleDateFormat("HH:mm", locale).format(Date(now)) }
    val hour = remember(now) {
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    }
    val greet = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
    val dateLine = remember(now, locale) {
        SimpleDateFormat("EEEE, d MMM", locale).format(Date(now)).uppercase(locale)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            clockText,
            style = PebbleType.Clock.copy(fontSize = 64.sp, lineHeight = 68.sp),
            color = PebbleColors.OnSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            greet,
            style = PebbleType.Greet.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = PebbleColors.OnSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            dateLine,
            style = PebbleType.Overline.copy(fontSize = 14.sp, letterSpacing = 1.4.sp),
            color = PebbleColors.OnSurfaceFaint,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoice: () -> Unit,
    privateMode: Boolean,
    onTogglePrivate: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .widthIn(min = 720.dp, max = 1100.dp)
            .fillMaxWidth(0.7f)
            .height(78.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pebbleFocus(shape = PebbleShape.Full, lift = false)
                .background(PebbleColors.Surface2, PebbleShape.Full)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                        Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                        Key.DirectionRight -> { focusManager.moveFocus(FocusDirection.Right); true }
                        else -> false
                    }
                }
                .padding(horizontal = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = PebbleColors.OnSurfaceMuted,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PebbleColors.OnSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W400,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    cursorBrush = SolidColor(PebbleColors.Primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                "Search Google or type a URL",
                                style = PebbleType.TitleLg,
                                color = PebbleColors.OnSurfaceFaint,
                            )
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        MicButton(
            onClick = onVoice,
            onUpDown = { dir ->
                focusManager.moveFocus(dir)
            },
        )
        Spacer(Modifier.width(12.dp))
        PrivateButton(active = privateMode, onClick = onTogglePrivate)
    }
}

@Composable
private fun PrivateButton(active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .pebbleFocus(shape = CircleShape, lift = false)
            .background(
                if (active) PebbleColors.PrimaryContainer else PebbleColors.Surface2,
                CircleShape,
            )
            .pebbleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.VisibilityOff,
            contentDescription = if (active) "Private search enabled" else "Private search disabled",
            tint = if (active) PebbleColors.PrimaryStrong else PebbleColors.OnSurfaceMuted,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun MicButton(
    onClick: () -> Unit,
    onUpDown: (FocusDirection) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .pebbleFocus(shape = CircleShape, lift = false)
            .background(PebbleColors.Surface2, CircleShape)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { onUpDown(FocusDirection.Up); true }
                    Key.DirectionDown -> { onUpDown(FocusDirection.Down); true }
                    else -> false
                }
            }
            .pebbleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Mic,
            contentDescription = "Voice search",
            tint = PebbleColors.Primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MostVisitedSection(
    items: List<MostVisitedView>,
    onClick: (MostVisitedView) -> Unit,
    onLongClick: (MostVisitedView) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "MOST VISITED",
            style = PebbleType.Overline,
            color = PebbleColors.OnSurfaceFaint,
        )
        Spacer(Modifier.height(PebbleDimens.Sp4))
        if (items.isEmpty()) {
            Text(
                "Your most-visited sites will appear here.",
                style = PebbleType.BodySm,
                color = PebbleColors.OnSurfaceMuted,
            )
            return@Column
        }
        val firstTileFocus = remember { FocusRequester() }
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp4),
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { enter = { firstTileFocus } },
        ) {
            itemsIndexed(items, key = { _, it -> it.url }) { index, item ->
                Tile(
                    item = item,
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                    modifier = if (index == 0) Modifier.focusRequester(firstTileFocus) else Modifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Tile(
    item: MostVisitedView,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = UrlUtils.hostOf(item.url) ?: item.url
    Box(
        modifier = modifier
            .pebbleFocus(shape = PebbleShape.Md, lift = true)
            .background(PebbleColors.Surface1, PebbleShape.Md)
            .border(1.dp, PebbleColors.OutlineFaint, PebbleShape.Md)
            .pebbleClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(PebbleDimens.Sp3)
            .width(140.dp)
            .height(120.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PebbleColors.Surface3, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = "https://www.google.com/s2/favicons?domain=$host&sz=64",
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(PebbleShape.Sm),
                )
            }
            Spacer(Modifier.height(PebbleDimens.Sp2))
            Text(
                item.title?.takeIf { it.isNotBlank() } ?: host,
                style = PebbleType.LabelSm,
                color = PebbleColors.OnSurface,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TileContextMenu(
    item: MostVisitedView,
    isBookmarked: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val host = UrlUtils.hostOf(item.url) ?: item.url
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
                WebViewPreview(
                    url = item.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(PebbleColors.Surface3, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    item.title?.takeIf { it.isNotBlank() } ?: host,
                    style = PebbleType.Title,
                    color = PebbleColors.OnSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    host,
                    style = PebbleType.BodySm,
                    color = PebbleColors.OnSurfaceMuted,
                    maxLines = 1,
                )
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
                    icon = Icons.Outlined.Delete,
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
private fun WebViewPreview(url: String, modifier: Modifier = Modifier) {
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

@Suppress("unused")
private val _unusedFallback = Icons.Outlined.Public
