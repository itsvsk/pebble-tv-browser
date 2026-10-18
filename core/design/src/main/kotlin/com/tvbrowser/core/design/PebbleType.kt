package com.tvbrowser.core.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/**
 * Pebble design tokens — type.
 * Sea Glass pairs editorial serif headings with clear sans-serif controls.
 *
 * Mapping pebble-scale -> M3 Typography slots:
 *   clock     -> displayLarge       (92 / W200 / -2)
 *   display   -> displayMedium      (38 / W500 / -.5)   view titles
 *   greet     -> displaySmall       (30 / W300)         home greeting
 *   titleLg   -> headlineMedium     (23 / W400)         address bar / search input
 *   title     -> titleLarge         (19 / W500)         address text, focused row
 *   body      -> titleMedium        (18 / W500)         row & settings titles
 *   bodySm    -> bodyLarge          (17 / W400)         subtitles, menu items
 *   label     -> labelLarge         (15 / W500)         pills, meta
 *   labelSm   -> labelMedium        (14 / W400)         captions
 *   overline  -> labelSmall         (13 / W600 / +.5 / UPPER)  section labels
 *
 * Roboto Mono reserved for file paths — exposed separately as PebbleType.mono.
 * Tabular figures for clock/percent/size use TextStyle.copy(fontFeatureSettings = "tnum").
 */
object PebbleType {
    val sans: FontFamily = FontFamily(
        Font(R.font.plus_jakarta_sans, FontWeight.W400),
        Font(R.font.plus_jakarta_sans, FontWeight.W500),
        Font(R.font.plus_jakarta_sans, FontWeight.W600),
        Font(R.font.plus_jakarta_sans, FontWeight.W700),
        Font(R.font.plus_jakarta_sans, FontWeight.W800),
    )
    val mono: FontFamily = FontFamily.Monospace // swap to Roboto Mono when bundled

    val Clock = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W200,
        fontSize = 92.sp,
        lineHeight = 96.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    )
    val Display = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.W500,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    )
    val Greet = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.W300,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.2.sp,
    )
    val TitleLg = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W400,
        fontSize = 23.sp,
        lineHeight = 30.sp,
    )
    val Title = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W500,
        fontSize = 19.sp,
        lineHeight = 26.sp,
    )
    val Body = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    )
    val BodySm = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )
    val Label = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
    val LabelSm = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val Overline = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    )

    val Mono = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
}

val PebbleTypography = Typography(
    displayLarge   = PebbleType.Clock,
    displayMedium  = PebbleType.Display,
    displaySmall   = PebbleType.Greet,
    headlineLarge  = PebbleType.Display,
    headlineMedium = PebbleType.TitleLg,
    headlineSmall  = PebbleType.Title,
    titleLarge     = PebbleType.Title,
    titleMedium    = PebbleType.Body,
    titleSmall     = PebbleType.Label,
    bodyLarge      = PebbleType.BodySm,
    bodyMedium     = PebbleType.BodySm,
    bodySmall      = PebbleType.LabelSm,
    labelLarge     = PebbleType.Label,
    labelMedium    = PebbleType.LabelSm,
    labelSmall     = PebbleType.Overline,
)
