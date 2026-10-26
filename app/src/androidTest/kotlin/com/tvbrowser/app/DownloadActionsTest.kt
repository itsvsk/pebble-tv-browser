package com.tvbrowser.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.db.AppDatabase
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.repo.DownloadRepository
import com.tvbrowser.core.downloads.DownloadManagerRepo
import com.tvbrowser.core.downloads.downloadChooserIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DownloadActionsTest {
    @Test
    fun openAlwaysOffersAChooserWithTemporaryReadAccess() {
        val uri = Uri.parse("content://com.tvbrowser.app.debug.fileprovider/downloads/movie.mp4")
        val chooser = downloadChooserIntent(uri, "video/mp4", "movie.mp4")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertTrue(chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        val view = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_VIEW, view.action)
        assertEquals(uri, view.data)
        assertEquals("video/mp4", view.type)
        assertTrue(view.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, view.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertNull(view.component)
        assertNull(view.`package`)
    }

    @Test
    fun completionUpdatesWithoutOpeningDownloadsAndIsHandledOnlyOnce() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val repo = DownloadRepository(database.downloadDao())
        val manager = DownloadManagerRepo(app, repo)
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val file = File(manager.defaultDirectory()!!, "pebble-completion-test-${System.nanoTime()}.txt")
        file.parentFile!!.mkdirs()
        file.writeText("Pebble download completion fixture")
        @Suppress("DEPRECATION")
        val id = system.addCompletedDownload(file.name, "Test fixture", false, "text/plain", file.absolutePath, file.length(), true)
        try {
            repo.upsert(DownloadRecord(id, "https://example.com/test.txt", file.name, file.absolutePath,
                mime = "text/plain", status = DownloadRecord.STATUS_RUNNING))
            assertTrue(manager.refreshCompletion(id))
            assertEquals(DownloadRecord.STATUS_SUCCESS, repo.all().first().single().status)
            assertFalse("A repeated broadcast must not alert twice", manager.refreshCompletion(id))
            assertFalse("Untracked downloads must be ignored", manager.refreshCompletion(Long.MAX_VALUE))
        } finally {
            system.remove(id)
            file.delete()
            database.close()
        }
    }
}
