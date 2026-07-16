package com.tvbrowser.core.downloads

data class DownloadProgress(
    val dmId: Long,
    val bytesSoFar: Long,
    val totalBytes: Long,
    val statusCode: Int,
    val reason: Int,
) {
    val fractionOrNull: Float? get() =
        if (totalBytes > 0) (bytesSoFar.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else null
}
