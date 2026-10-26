package com.tvbrowser.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Approved Tidal Lens mark with the textured Tumbled Jade finish.
 * The material is baked into the asset so navigation never runs a glass shader.
 */
@Composable
fun PebbleLogo(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Image(
        painter = painterResource(R.drawable.pebble_logo),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
