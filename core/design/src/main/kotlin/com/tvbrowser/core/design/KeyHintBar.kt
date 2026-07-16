package com.tvbrowser.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvbrowser.core.design.PebbleColors
import com.tvbrowser.core.design.PebbleDimens
import com.tvbrowser.core.design.PebbleType

/** One hint pair: the key glyph(s) (e.g. "▲▼◀▶") and the action label ("Navigate"). */
data class KeyHint(val keys: String, val label: String)

private val DefaultHints = listOf(
    KeyHint("▲▼◀▶", "Navigate"),
    KeyHint("OK", "Select"),
    KeyHint("Back", "Return"),
)

/**
 * Bottom 56dp contextual key-hint bar over a protection gradient.
 * Non-interactive; reads bg from `PebbleColors.Bg` → transparent.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KeyHintBar(
    modifier: Modifier = Modifier,
    hints: List<KeyHint> = DefaultHints,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PebbleDimens.HintH)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        1f to PebbleColors.Bg.copy(alpha = 0.92f),
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PebbleDimens.Gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            hints.forEachIndexed { i, h ->
                if (i > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        " · ",
                        style = PebbleType.Label,
                        color = PebbleColors.OnSurfaceFaint,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(h.keys, style = PebbleType.Label, color = PebbleColors.OnSurfaceMuted)
                Spacer(Modifier.width(8.dp))
                Text(h.label, style = PebbleType.Label, color = PebbleColors.OnSurfaceFaint)
            }
        }
    }
}
