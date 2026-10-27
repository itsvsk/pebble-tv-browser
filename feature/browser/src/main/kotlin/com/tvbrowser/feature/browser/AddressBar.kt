package com.tvbrowser.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.seaGlass
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
    topNavFocusRequester: FocusRequester,
    pointerPreferred: Boolean,
    onResumePointer: () -> Unit,
    url: String,
    isBookmarked: Boolean,
    desktopUa: Boolean,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onBookmark: () -> Unit,
    onToggleUa: () -> Unit,
    onPageMenu: () -> Unit = {},
    editRequest: Int = 0,
    modeEnabled: Boolean = true,
) {
    var text by remember(url) { mutableStateOf(TextFieldValue(url, TextRange(url.length))) }
    var editing by remember { mutableStateOf(false) }
    var editorFocusRequest by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    val menuFocusRequester = remember { FocusRequester() }
    LaunchedEffect(editRequest) { if (editRequest > 0) { editing = true; editorFocusRequest++ } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onPreviewKeyEvent { event ->
                if (pointerPreferred && event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown
                ) {
                    onResumePointer()
                    true
                } else false
            }
            // The WebView is a later sibling; its initial white frame must stay below the controls.
            .zIndex(1f)
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
                onLeft = { menuFocusRequester.requestFocus() },
                onUp = { topNavFocusRequester.requestFocus() },
            )
            RoundIcon(Icons.Outlined.ArrowForward, "Forward", onClick = onForward,
                onUp = { topNavFocusRequester.requestFocus() })
            RoundIcon(Icons.Outlined.Refresh, "Reload", onClick = onReload,
                onUp = { topNavFocusRequester.requestFocus() })

            Spacer(Modifier.width(PebbleDimens.Sp2))

            AddressPill(
                url = url,
                text = text,
                onTextChange = { text = it },
                editing = editing,
                onEditingChange = { editing = it },
                focusRequestKey = editorFocusRequest,
                onSubmit = {
                    if (text.text.isNotBlank()) {
                        onSubmit(text.text)
                        editing = false
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                },
                onUp = { topNavFocusRequester.requestFocus() },
                modifier = Modifier.weight(1f),
            )

            RoundIcon(
                Icons.Outlined.Close,
                "Clear address",
                onClick = {
                    text = TextFieldValue("")
                    editing = true
                    editorFocusRequest++
                },
                onUp = { topNavFocusRequester.requestFocus() },
            )

            Spacer(Modifier.width(PebbleDimens.Sp2))

            RoundIcon(
                imageVector = if (desktopUa) Icons.Outlined.DesktopWindows else Icons.Outlined.PhoneAndroid,
                contentDescription = if (desktopUa) "Desktop site" else "Mobile site",
                onClick = onToggleUa,
                enabled = modeEnabled,
                onUp = { topNavFocusRequester.requestFocus() },
            )
            RoundIcon(
                imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Bookmark",
                onClick = onBookmark,
                tint = if (isBookmarked) PebbleColors.Primary else PebbleColors.OnSurfaceMuted,

                onUp = { topNavFocusRequester.requestFocus() },
            )
            RoundIcon(Icons.Outlined.MoreVert, "Page options", onClick = onPageMenu,
                focusRequester = menuFocusRequester,
                onRight = { controlsFocusRequester.requestFocus() },
                onUp = { topNavFocusRequester.requestFocus() })
        }
    }

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
    onUp: (() -> Unit)? = null,
) {
    val focusSource = remember { MutableInteractionSource() }
    val focused by focusSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .then(if (onLeft != null || onRight != null || onUp != null) Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (onLeft != null) { onLeft(); true } else false
                    Key.DirectionRight -> if (onRight != null) { onRight(); true } else false
                    Key.DirectionUp -> if (onUp != null) { onUp(); true } else false
                    else -> false
                }
            } else Modifier)
            .pebbleFocus(shape = CircleShape, lift = false, tight = true, interactionSource = focusSource)
            .seaGlass(CircleShape, if (focused) PebbleColors.GlassFocus else PebbleColors.GlassLagoon,
                if (focused) 6.dp else 1.dp, emphasized = focused)
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
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    focusRequestKey: Int,
    onSubmit: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val editorFocusRequester = remember { FocusRequester() }
    var editorHasFocus by remember { mutableStateOf(false) }
    val host = remember(url) { url.substringAfter("://", url) }
    val secure = url.startsWith("https://", ignoreCase = true)
    LaunchedEffect(editing, focusRequestKey) {
        if (editing) editorFocusRequester.requestFocus()
    }
    Row(
        modifier = modifier
            .height(42.dp)
            .testTag("browser-address")
            .pebbleFocus(shape = PebbleShape.Full, lift = false)
            .seaGlass(PebbleShape.Full, PebbleColors.GlassLagoon, 2.dp)
            .pebbleClickable(onClick = { onEditingChange(true) }, onClickLabel = "Edit address")
            .padding(horizontal = 16.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionUp -> { onUp(); true }
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
                modifier = Modifier.weight(1f)
                    .testTag("browser-address-editor")
                    .focusRequester(editorFocusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) editorHasFocus = true
                        else if (editorHasFocus) {
                            editorHasFocus = false
                            onEditingChange(false)
                        }
                    },
                decorationBox = { inner ->
                    if (text.text.isEmpty()) {
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
