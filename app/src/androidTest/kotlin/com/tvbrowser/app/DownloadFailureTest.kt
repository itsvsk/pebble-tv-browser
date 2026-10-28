package com.tvbrowser.app

import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.AppDatabase
import com.tvbrowser.core.db.repo.DownloadRepository
import com.tvbrowser.core.downloads.DownloadManagerRepo
import com.tvbrowser.feature.browser.BrowserEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import androidx.room.Room
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadFailureTest {
    @Test
    fun successfulDownloadIsRecordedByBroadcastWithoutProgressPolling() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val manager = EntryPointAccessors.fromApplication(app, BrowserEntryPoint::class.java).downloads()
        // A separate read connection verifies the real receiver's database write without
        // observing progress (which used to be the only way completion was recorded).
        val database = Room.databaseBuilder(app, AppDatabase::class.java, "tv_browser.db").build()
        val id = manager.enqueue(
            "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            null, null, "application/pdf",
        )
        try {
            withTimeout(60_000) {
                while (true) {
                    val status = database.downloadDao().get(id)?.status
                    if (status == DownloadRecord.STATUS_SUCCESS || status == DownloadRecord.STATUS_FAILED) {
                        assertEquals(DownloadRecord.STATUS_SUCCESS, status)
                        break
                    }
                    delay(250)
                }
            }
        } finally {
            manager.cancel(id)
            database.close()
        }
    }

    @Test
    fun missingDownloadBecomesFailed() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val repository = DownloadRepository(database.downloadDao())
        val manager = DownloadManagerRepo(app, repository)
        val id = manager.enqueue(
            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/pebble-missing.pdf",
            userAgent = null,
            contentDisposition = null,
            mime = "application/pdf",
        )
        try {
            withTimeout(60_000) {
                val record = repository.all().first { records -> records.any { it.dmId == id } }
                    .first { it.dmId == id }
                val progressJob = launch { manager.observeProgress(listOf(record)).collect() }
                try {
                    val failed = repository.all().first { records ->
                        records.any { it.dmId == id && it.status == DownloadRecord.STATUS_FAILED }
                    }.first { it.dmId == id }
                    assertEquals(DownloadRecord.STATUS_FAILED, failed.status)
                } finally {
                    progressJob.cancelAndJoin()
                }
            }
        } finally {
            manager.cancel(id)
            database.close()
        }
    }
}
