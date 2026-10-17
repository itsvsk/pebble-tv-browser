package com.tvbrowser.feature.home

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JtTextStyle
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    homeNavFocus: FocusRequester,
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
    val firstSpeedDialFocus = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
        if (spoken != null) {
            query = spoken
            vm.openInNewTab(spoken) { tabId -> onOpenUrl(tabId) }
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

    val quickLinks = remember(mostVisited) {
        val defaults = listOf(
            MostVisitedView("https://www.youtube.com", "YouTube 4K", 0, 0),
            MostVisitedView("https://www.twitch.tv", "Twitch Interactive", 0, 0),
            MostVisitedView("https://www.netflix.com", "Netflix Stream", 0, 0),
            MostVisitedView("https://www.reddit.com", "Reddit TV Hub", 0, 0),
            MostVisitedView("https://github.com", "GitHub Dashboard", 0, 0),
            MostVisitedView("https://www.wikipedia.org", "Wikipedia Daily", 0, 0),
            MostVisitedView("https://www.theverge.com", "The Verge Media", 0, 0),
            MostVisitedView("https://open.spotify.com", "Spotify Web Player", 0, 0),
        )
        (mostVisited + defaults).distinctBy { UrlUtils.hostOf(it.url) }.take(8)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PebbleDimens.Gutter, vertical = PebbleDimens.Sp5)
                .padding(bottom = PebbleDimens.HintH),
        ) {
            Text("✦  PEBBLE ENGINE  ·  TV READY  ·  WEB AT A DISTANCE", style = PebbleType.Overline, color = PebbleColors.Primary)
            Spacer(Modifier.height(PebbleDimens.Sp4))
            SearchField(
                value = query,
                onValueChange = { query = it },
                onSubmit = {
                    if (query.isNotBlank()) {
                        vm.openInNewTab(query) { tabId -> onOpenUrl(tabId) }
                    }
                },
                onVoice = { voiceLauncher.launch(voiceIntent) },
                focusRequester = omniboxFocus,
                onUp = { homeNavFocus.requestFocus() },
                onDown = { firstSpeedDialFocus.requestFocus() },
            )
            Spacer(Modifier.height(PebbleDimens.Sp3))
            Text("OK  Type       MIC  Speak       ▼  Jump to Speed Dials", style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint)
            Spacer(Modifier.height(PebbleDimens.Sp8))
            MostVisitedSection(
                items = quickLinks,
                onClick = { item ->
                    vm.openInNewTab(item.url) { tabId -> onOpenUrl(tabId) }
                },
                onLongClick = { item -> vm.showMenu(item) },
                firstTileFocus = firstSpeedDialFocus,
            )
            Spacer(Modifier.height(PebbleDimens.Sp8))
            ExploreSection { url -> vm.openInNewTab(url) { onOpenUrl(it) } }
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
private fun Hero(time: LocalTime, date: LocalDate) {
    val clockText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val hour = time.hour
    val greet = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
    val dayName = date.dayOfWeek.getDisplayName(JtTextStyle.FULL, Locale.getDefault()).uppercase(Locale.getDefault())
    val monthName = date.month.getDisplayName(JtTextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault())
    val dateLine = "$dayName, ${date.dayOfMonth} $monthName"

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
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pebbleFocus(shape = PebbleShape.Full, lift = false)
                .background(PebbleColors.GlassHigh, PebbleShape.Full)
                .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Full)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionUp -> { onUp(); true }
                        Key.DirectionDown -> { onDown(); true }
                        Key.DirectionRight -> { focusManager.moveFocus(FocusDirection.Right); true }
                        else -> false
                    }
                }
                .padding(horizontal = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = PebbleColors.OnSurfaceMuted,
                    modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.width(16.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PebbleColors.OnSurface,
                        fontFamily = PebbleType.sans,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.W400,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    cursorBrush = SolidColor(PebbleColors.Primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                "Search the web or enter a URL",
                                style = PebbleType.Title,
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
                if (dir == FocusDirection.Down) onDown() else focusManager.moveFocus(dir)
            },
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
            .size(54.dp)
            .pebbleFocus(shape = CircleShape, lift = false)
            .background(PebbleColors.Primary, CircleShape)
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
            tint = PebbleColors.OnPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MostVisitedSection(
    items: List<MostVisitedView>,
    onClick: (MostVisitedView) -> Unit,
    onLongClick: (MostVisitedView) -> Unit,
    firstTileFocus: FocusRequester,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Speed Dials", "${items.size} quick links")
        Spacer(Modifier.height(PebbleDimens.Sp4))
        Column(verticalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3)) {
            items.chunked(4).forEachIndexed { rowIndex, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3)) {
                    rowItems.forEachIndexed { index, item ->
                        Tile(
                            item = item,
                            onClick = { onClick(item) },
                            onLongClick = { onLongClick(item) },
                            modifier = Modifier.weight(1f)
                                .then(if (rowIndex == 0 && index == 0) Modifier.focusRequester(firstTileFocus) else Modifier),
                        )
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 3.dp, height = 22.dp).background(PebbleColors.Primary, PebbleShape.Full))
        Spacer(Modifier.width(10.dp))
        Text(title, style = PebbleType.TitleLg.copy(fontWeight = FontWeight.W700), color = PebbleColors.OnSurface)
        Spacer(Modifier.width(10.dp))
        Text(subtitle, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ExploreSection(onOpen: (String) -> Unit) {
    val portals = listOf(
        Triple("Watch in 4K", "YouTube · video and live streams", "https://www.youtube.com"),
        Triple("Explore live", "Twitch · interactive broadcasts", "https://www.twitch.tv"),
        Triple("Discover more", "The Verge · technology and culture", "https://www.theverge.com"),
    )
    val artwork = listOf(R.drawable.feature_city, R.drawable.feature_ocean, R.drawable.feature_prism)
    Column {
        SectionTitle("Explore the Web", "Featured portals")
        Spacer(Modifier.height(PebbleDimens.Sp4))
        Row(horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp3)) {
            portals.forEachIndexed { index, (title, detail, url) ->
                Box(
                    modifier = Modifier.weight(1f)
                        .height(158.dp)
                        .pebbleFocus(shape = PebbleShape.Md)
                        .clip(PebbleShape.Md)
                        .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Md)
                        .pebbleClickable(onClick = { onOpen(url) })
                ) {
                    Image(painter = painterResource(artwork[index]), contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        listOf(PebbleColors.Bg.copy(alpha = 0.1f), PebbleColors.Bg.copy(alpha = 0.84f)))))
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("✦  FEATURED", style = PebbleType.Overline, color = PebbleColors.Primary)
                        Column {
                            Text(title, style = PebbleType.Title, color = PebbleColors.OnSurface, maxLines = 1)
                            Text(detail, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceMuted, maxLines = 2)
                        }
                    }
                }
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
            .background(PebbleColors.Glass, PebbleShape.Md)
            .border(1.dp, PebbleColors.GlassEdge, PebbleShape.Md)
            .pebbleClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(PebbleDimens.Sp3)
            .height(108.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(PebbleShape.Xs)
                    .background(PebbleColors.Surface3, PebbleShape.Xs),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = "https://www.google.com/s2/favicons?domain=$host&sz=64",
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(PebbleShape.Xs),
                )
            }
            Column {
                Text(item.title?.takeIf { it.isNotBlank() } ?: host, style = PebbleType.Label.copy(fontWeight = FontWeight.W600), color = PebbleColors.OnSurface, maxLines = 1)
                Text(host, style = PebbleType.LabelSm, color = PebbleColors.OnSurfaceFaint, maxLines = 1)
            }
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
                if (item.visitCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    MenuRow(
                        label = "Remove from history",
                        icon = Icons.Outlined.Delete,
                        onClick = onDelete,
                        danger = true,
                    )
                }
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
private val _unusedFallback = Icons.Outlined.Public
