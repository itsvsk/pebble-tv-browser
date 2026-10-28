package com.tvbrowser.core.downloads

/** DownloadManager reason codes, including HTTP response codes. */
fun downloadFailureMessage(reason: Int?): String = when (reason) {
    in 400..599 -> "The website returned HTTP $reason. Reopen the source page if this link has expired."
    1001 -> "The file could not be saved. Check the download storage and retry."
    1004 -> "The connection was interrupted. Retry the download."
    1005 -> "The download redirected too many times. Reopen the source page."
    1006 -> "Not enough free storage. Free some space and retry."
    1007 -> "Download storage is unavailable. Reconnect it and retry."
    1008 -> "This download cannot resume. Retry to start a fresh transfer."
    1009 -> "A file already exists at this destination. Retry with a new filename."
    else -> "Download failed. Retry or reopen the source page for a fresh link."
}
