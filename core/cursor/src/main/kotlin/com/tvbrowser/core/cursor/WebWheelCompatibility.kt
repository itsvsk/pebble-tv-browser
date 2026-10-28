package com.tvbrowser.core.cursor

private val chromiumMajor = Regex("Chrome/([0-9]+)")

/** Chromium 145 corrected Android's previously inverted horizontal wheel conversion. */
internal fun horizontalWheelSignForUserAgent(defaultUserAgent: String): Float {
    val major = chromiumMajor.find(defaultUserAgent)?.groupValues?.get(1)?.toIntOrNull()
    return if (major != null && major < 145) -1f else 1f
}
