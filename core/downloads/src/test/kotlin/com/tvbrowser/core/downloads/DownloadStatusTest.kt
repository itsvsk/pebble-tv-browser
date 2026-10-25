package com.tvbrowser.core.downloads

import android.app.DownloadManager
import com.tvbrowser.core.db.entity.DownloadRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {
    @Test
    fun managerStatusesMapToVisibleRecordStatuses() {
        assertEquals(DownloadRecord.STATUS_RUNNING, recordStatus(DownloadManager.STATUS_PENDING))
        assertEquals(DownloadRecord.STATUS_RUNNING, recordStatus(DownloadManager.STATUS_RUNNING))
        assertEquals(DownloadRecord.STATUS_PAUSED, recordStatus(DownloadManager.STATUS_PAUSED))
        assertEquals(DownloadRecord.STATUS_SUCCESS, recordStatus(DownloadManager.STATUS_SUCCESSFUL))
        assertEquals(DownloadRecord.STATUS_FAILED, recordStatus(DownloadManager.STATUS_FAILED))
    }
}
