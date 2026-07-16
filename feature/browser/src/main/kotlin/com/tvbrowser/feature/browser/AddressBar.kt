package com.tvbrowser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwipeRightAlt
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleShape
import com.tvbrowser.core.design.PebbleType
import com.tvbrowser.core.design.pebbleClickable
import com.tvbrowser.core.design.pebbleFocus

/**
 * Pebble browser chrome bar (92dp, surface).
 * Layout: [back][fwd][reload] · address-pill (lock + scheme + url + mode-chip) · [UA][cursor][star][tabs][overflow].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddressBar(
    url: String,
    isBookmarked: Boolean,
    desktopUa: Boolean,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit, // kept for API compat; rail handles Home now
    onTabs: () -> Unit,
    onBookmark: () -> Unit,
    onToggleUa: () -> Unit,
    cursorActive: Boolean,
    onToggleCursor: () -> Unit,
    onMenu: () -> Unit,
) {
    var text by remember(url) { mutableStateOf(url) }
    var editing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PebbleDimens.ChromeH)
            .background(PebbleColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Sp4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp2),
        ) {
            RoundIcon(Icons.Outlined.ArrowBack, "Back", onClick = onBack)
            RoundIcon(Icons.Outlined.ArrowForward, "Forward", onClick = onForward, enabled = false)
            RoundIcon(Icons.Outlined.Refresh, "Reload", onClick = onReload)

            Spacer(Modifier.width(PebbleDimens.Sp2))

            AddressPill(
                url = url,
                text = text,
                onTextChange = { text = it },
                editing = editing,
                onEditingChange = { editing = it },
                onSubmit = {
                    onSubmit(text)
                    editing = false
                    focusManager.moveFocus(FocusDirection.Down)
                },
                desktopUa = desktopUa,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(PebbleDimens.Sp2))

            RoundIcon(
                imageVector = if (desktopUa) Icons.Outlined.DesktopWindows else Icons.Outlined.PhoneAndroid,
                contentDescription = if (desktopUa) "Desktop site" else "Mobile site",
                onClick = onToggleUa,
            )
            RoundIcon(
                imageVector = if (cursorActive) Icons.Outlined.Mouse else Icons.Outlined.SwipeRightAlt,
                contentDescription = if (cursorActive) "Cursor mode" else "D-pad mode",
                onClick = onToggleCursor,
            )
            RoundIcon(
                imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Bookmark",
                onClick = onBookmark,
                tint = if (isBookmarked) PebbleColors.PrimaryStrong else PebbleColors.OnSurfaceMuted,
            )
            RoundIcon(Icons.Outlined.Tab, "Tabs", onClick = onTabs)
            RoundIcon(Icons.Outlined.MoreVert, "Menu", onClick = onMenu)
        }
    }

    LaunchedEffect(url) { text = url }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RoundIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = PebbleColors.OnSurfaceMuted,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .pebbleFocus(shape = CircleShape, lift = false, tight = true)
            .background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .let { if (enabled) it.pebbleClickable(onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else PebbleColors.OnSurfaceFaint.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp),
        )
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddressPill(
    url: String,
    text: String,
    onTextChange: (String) -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    desktopUa: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val (scheme, host) = remember(url) { splitScheme(url) }
    Row(
        modifier = modifier
            .height(60.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false)
            .background(PebbleColors.Surface2, PebbleShape.Full)
            .padding(horizontal = 20.dp)
            .onFocusChanged { onEditingChange(it.isFocused) }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                    Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Secure",
            tint = PebbleColors.OnSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = PebbleColors.OnSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.W400,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                cursorBrush = SolidColor(PebbleColors.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Search or type URL", style = PebbleType.TitleLg, color = PebbleColors.OnSurfaceFaint)
                    }
                    inner()
                },
            )
        } else {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (scheme.isNotEmpty()) {
                    Text(
                        scheme,
                        style = PebbleType.TitleLg,
                        color = PebbleColors.OnSurfaceFaint,
                        maxLines = 1,
                    )
                }
                Text(
                    host,
                    style = PebbleType.Title,
                    color = PebbleColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        ModeChip(desktop = desktopUa)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ModeChip(desktop: Boolean) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .background(PebbleColors.Surface3, PebbleShape.Full)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (desktop) "DESKTOP" else "MOBILE",
            style = PebbleType.Overline,
            color = PebbleColors.OnSurfaceMuted,
        )
    }
}

private fun splitScheme(url: String): Pair<String, String> {
    val idx = url.indexOf("://")
    return if (idx > 0) (url.substring(0, idx + 3)) to url.substring(idx + 3)
    else "" to url
}
