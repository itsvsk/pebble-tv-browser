package com.tvbrowser.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage

/** A stable site identity card for menus; opening a menu never loads another WebView. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WebsitePreview(host: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).seaGlass(PebbleShape.Md, PebbleColors.GlassMint, 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null,
                tint = PebbleColors.OnSurfaceMuted, modifier = Modifier.size(38.dp))
            AsyncImage(
                model = "https://www.google.com/s2/favicons?domain=$host&sz=128",
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.size(PebbleDimens.Sp4))
        Text(host, style = PebbleType.Title, color = PebbleColors.OnSurface, maxLines = 1)
    }
}
