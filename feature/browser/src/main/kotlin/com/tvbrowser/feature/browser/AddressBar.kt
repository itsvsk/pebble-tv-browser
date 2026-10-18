package com.tvbrowser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 * Compact browser controls directly below the main top navigation.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddressBar(
    controlsFocusRequester: FocusRequester,
    url: String,
    isBookmarked: Boolean,
    desktopUa: Boolean,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onBookmark: () -> Unit,
    onToggleUa: () -> Unit,
) {
    var text by remember(url) { mutableStateOf(url) }
    var editing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val bookmarkFocusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(PebbleColors.GlassHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PebbleDimens.Sp2),
        ) {
            RoundIcon(
                Icons.Outlined.ArrowBack,
                "Back",
                onClick = onBack,
                focusRequester = controlsFocusRequester,
                onLeft = { bookmarkFocusRequester.requestFocus() },
            )
            RoundIcon(Icons.Outlined.ArrowForward, "Forward", onClick = onForward)
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
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(PebbleDimens.Sp2))

            RoundIcon(
                imageVector = if (desktopUa) Icons.Outlined.DesktopWindows else Icons.Outlined.PhoneAndroid,
                contentDescription = if (desktopUa) "Desktop site" else "Mobile site",
                onClick = onToggleUa,
            )
            RoundIcon(
                imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Bookmark",
                onClick = onBookmark,
                tint = if (isBookmarked) PebbleColors.PrimaryStrong else PebbleColors.OnSurfaceMuted,
                focusRequester = bookmarkFocusRequester,
                onRight = { controlsFocusRequester.requestFocus() },
            )
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
    focusRequester: FocusRequester? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (onLeft != null || onRight != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (onLeft != null) { onLeft(); true } else false
                    Key.DirectionRight -> if (onRight != null) { onRight(); true } else false
                    else -> false
                }
            } else Modifier)
            .pebbleFocus(shape = CircleShape, lift = false, tight = true)
            .background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .let { if (enabled) it.pebbleClickable(onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) tint else PebbleColors.OnSurfaceFaint.copy(alpha = 0.5f),
            modifier = Modifier.size(21.dp),
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
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val host = remember(url) { url.substringAfter("://", url) }
    val secure = url.startsWith("https://", ignoreCase = true)
    Row(
        modifier = modifier
            .height(42.dp)
            .pebbleFocus(shape = PebbleShape.Full, lift = false)
            .background(PebbleColors.Glass, PebbleShape.Full)
            .padding(horizontal = 16.dp)
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
            if (secure) Icons.Outlined.Lock else Icons.Outlined.Public,
            contentDescription = if (secure) "Secure site" else "Site security not verified",
            tint = if (secure) PebbleColors.Primary else PebbleColors.Warn,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = PebbleColors.OnSurface,
                    fontFamily = PebbleType.sans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W400,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                cursorBrush = SolidColor(PebbleColors.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("Search or type URL", style = PebbleType.Title, color = PebbleColors.OnSurfaceFaint)
                    }
                    inner()
                },
            )
        } else {
            Text(
                host,
                style = PebbleType.Label,
                color = PebbleColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
