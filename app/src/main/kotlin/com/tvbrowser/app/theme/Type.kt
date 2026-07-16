package com.tvbrowser.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

private val sans = FontFamily.SansSerif

val TvTypography = Typography(
    displayLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 72.sp),
    displayMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 52.sp, lineHeight = 60.sp),
    displaySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 48.sp),
    headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)
