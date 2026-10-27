package com.tvbrowser.core.downloads

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFailureTest {
    @Test fun explainsRecoverableStorageAndHttpFailures() {
        assertEquals("Not enough free storage. Free some space and retry.", downloadFailureMessage(1006))
        assertEquals("The website returned HTTP 403. Reopen the source page if this link has expired.", downloadFailureMessage(403))
        assertEquals("Download failed. Retry or reopen the source page for a fresh link.", downloadFailureMessage(null))
    }
}
