package com.tvbrowser.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Shapes

/**
 * Pebble design tokens — radius.
 * Source: colors_and_type.css §8.
 *   xs 8 · sm 12 · md 18 (cards/rows/rail-pill) · lg 28 (menus/sheets) · xl 36 · full (pills)
 *
 * M3 has slots small/medium/large — pebble's xs/sm/md/lg/xl/full mapped:
 *   Shapes.extraSmall -> xs 8     (chips, key caps)
 *   Shapes.small      -> sm 12    (favicons, small wells)
 *   Shapes.medium     -> md 18    (cards, rows, rail pill — most common)
 *   Shapes.large      -> lg 28    (menus, sheets)
 *   Shapes.extraLarge -> xl 36    (large containers)
 * `full` pill radius exposed separately as PebbleShape.Full.
 */
object PebbleShape {
    val Xs   = RoundedCornerShape(8.dp)
    val Sm   = RoundedCornerShape(12.dp)
    val Md   = RoundedCornerShape(18.dp)
    val Lg   = RoundedCornerShape(28.dp)
    val Xl   = RoundedCornerShape(36.dp)
    val Full = RoundedCornerShape(percent = 50)
}

val PebbleShapes = Shapes(
    extraSmall = PebbleShape.Xs,
    small      = PebbleShape.Sm,
    medium     = PebbleShape.Md,
    large      = PebbleShape.Lg,
    extraLarge = PebbleShape.Xl,
)
