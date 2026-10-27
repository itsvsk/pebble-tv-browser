package com.tvbrowser.app

import android.app.DownloadManager
import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.common.PrefKeys
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.db.AppDatabase
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.repo.DownloadRepository
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.downloads.DownloadManagerRepo
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.WebVideoController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class QolPersistenceTest {
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test
    fun exactHostPreferencesSurviveANewRepositoryWithoutChangingDefaultsOrOtherHosts() = runBlocking {
        val settings = SettingsRepository(app)
        val originalDesktop = settings.desktopUa.first()
        val originalBlocking = settings.adblockEnabled.first()
        val host = "qol-${UUID.randomUUID()}.invalid"
        val url = "https://$host/article"
        val other = "https://other-${UUID.randomUUID()}.invalid/"
        try {
            settings.set(PrefKeys.DesktopUa, false)
            settings.set(PrefKeys.AdblockEnabled, true)
            withTimeout(5_000) {
                settings.pagePreferences(url).first { !it.desktopUa && it.adblockEnabled && !it.overridden }
            }
            settings.setSiteDesktop(url, true)
            settings.setSiteAdblock(url, false)

            val fresh = SettingsRepository(app)
            val selected = withTimeout(5_000) {
                fresh.pagePreferences("https://${host.uppercase()}:8443/another-page").first {
                    it.overridden && it.desktopUa && !it.adblockEnabled
                }
            }
            assertEquals(host, selected.host)
            assertFalse("Site changes must not change the global default", fresh.desktopUa.first())
            assertTrue(fresh.adblockEnabled.first())
            for (unrelated in listOf(other, "https://sub.$host/")) {
                val inherited = fresh.pagePreferences(unrelated).first()
                assertFalse("Overrides must not leak to another host", inherited.overridden)
                assertFalse(inherited.desktopUa)
                assertTrue(inherited.adblockEnabled)
            }

            fresh.resetSite("http://$host/reset")
            val reset = withTimeout(5_000) { settings.pagePreferences(url).first { !it.overridden } }
            assertFalse(reset.desktopUa)
            assertTrue(reset.adblockEnabled)
        } finally {
            settings.resetSite(url)
            settings.set(PrefKeys.DesktopUa, originalDesktop)
            settings.set(PrefKeys.AdblockEnabled, originalBlocking)
        }
    }

    @Test
    fun reopeningAClosedNormalTabRestoresMetadataUnderANewIdAndExcludesPrivateTabs() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val repository = TabRepository(database.tabDao())
        val manager = TabManager(app, repository, AdBlockInterceptor(app), WebVideoController(), SettingsRepository(app))
        try {
            val originalId = manager.newTab("https://pebble.invalid/saved-article")
            val original = repository.get(originalId)!!.copy(title = "Saved article", position = 2, createdAt = 123L)
            repository.upsert(original)
            withContext(Dispatchers.Main) { manager.closeTab(originalId) }
            assertNull(repository.get(originalId))
            assertEquals(originalId, manager.recentlyClosed.value?.id)

            val privateId = manager.newTab("https://pebble.invalid/private-article", isIncognito = true)
            assertNotNull(manager.privateTab(privateId))
            withContext(Dispatchers.Main) { manager.closeTab(privateId) }
            assertNull("Closing must release private metadata", manager.privateTab(privateId))
            assertNull(repository.get(privateId))
            assertEquals("A private close must not replace normal reopen history", originalId, manager.recentlyClosed.value?.id)

            val restoredId = withContext(Dispatchers.Main) { manager.reopenClosedTab() }
            assertNotNull(restoredId)
            assertNotEquals("Closed WebView identities must not be reused", originalId, restoredId)
            val restored = repository.get(restoredId!!)!!
            assertEquals(original.url, restored.url)
            assertEquals(original.title, restored.title)
            assertEquals(original.position, restored.position)
            assertEquals(original.createdAt, restored.createdAt)
            assertFalse(restored.isIncognito)
            assertNull(manager.recentlyClosed.value)
            assertNull("Reopen must consume the closed entry", withContext(Dispatchers.Main) { manager.reopenClosedTab() })
            assertEquals(listOf(restored), repository.persisted().first())
        } finally {
            withContext(Dispatchers.Main) { manager.destroyAll() }
            database.close()
        }
    }

    @Test
    fun concurrentRetriesReplaceOneFailedDownloadWithOneFreshSystemRequest() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val repository = DownloadRepository(database.downloadDao())
        val manager = DownloadManagerRepo(app, repository)
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val url = "https://pebble.invalid/file?fixture=${UUID.randomUUID()}"
        val oldId = -41L
        val old = DownloadRecord(oldId, url, "old-file.bin", "/fixture/old-file.bin",
            mime = "application/octet-stream", status = DownloadRecord.STATUS_FAILED,
            failureReason = DownloadManager.ERROR_HTTP_DATA_ERROR,
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/155.0.8059.46 Safari/537.36")
        try {
            repository.upsert(old)
            val results = coroutineScope {
                List(2) { async(Dispatchers.Default) { runCatching { manager.retry(oldId) } } }.awaitAll()
            }
            val replacements = results.mapNotNull { it.getOrNull() }
            assertEquals("Repeated Retry must enqueue only once", 1, replacements.size)
            assertEquals("The second stale retry must be rejected", 1, results.count { it.isFailure })
            val replacementId = replacements.single()
            assertTrue(replacementId > 0)
            assertNull("The old record is removed only after creating the replacement", repository.get(oldId))
            val record = repository.all().first().single()
            assertEquals(replacementId, record.dmId)
            assertEquals(url, record.url)
            assertEquals(old.mime, record.mime)
            assertEquals("Retry must retain the initiating desktop identity", old.userAgent, record.userAgent)
            assertTrue("Retry must preserve the original filename", record.filename.startsWith("old-file"))
            assertEquals(DownloadRecord.STATUS_RUNNING, record.status)
            assertNull("A new request must not retain the old failure", record.failureReason)
            assertTrue(record.localPath.startsWith(manager.defaultDirectory()!!.absolutePath + File.separator))
            assertEquals("The system must contain exactly one request for this unique fixture", 1,
                countRequests(system, url))
        } finally {
            // Clean up even if an assertion discovers duplicate or untracked system requests.
            requestIds(system, url).forEach { system.remove(it) }
            repository.all().first().filter { it.dmId > 0 }.forEach { File(it.localPath).delete() }
            database.close()
        }
    }

    @Test
    fun retryRejectsEveryNonFailedStatusWithoutReplacingItsRecord() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val repository = DownloadRepository(database.downloadDao())
        val manager = DownloadManagerRepo(app, repository)
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fixture = UUID.randomUUID()
        val records = listOf(DownloadRecord.STATUS_PENDING, DownloadRecord.STATUS_RUNNING,
            DownloadRecord.STATUS_PAUSED, DownloadRecord.STATUS_SUCCESS).map { status ->
            DownloadRecord(-100L - status, "https://pebble.invalid/non-failed-$status?fixture=$fixture", "file-$status",
                "/fixture/file-$status", status = status)
        }
        try {
            for (record in records) {
                repository.upsert(record)
                val failure = runCatching { manager.retry(record.dmId) }.exceptionOrNull()
                assertTrue("Only failed transfers can be retried", failure is IllegalArgumentException)
                assertEquals(record, repository.get(record.dmId))
            }
            assertEquals(records.size, repository.all().first().size)
        } finally {
            records.forEach { record -> requestIds(system, record.url).forEach { system.remove(it) } }
            repository.all().first().filter { it.dmId > 0 }.forEach { File(it.localPath).delete() }
            database.close()
        }
    }

    private fun countRequests(system: DownloadManager, url: String): Int = requestIds(system, url).size

    private fun requestIds(system: DownloadManager, url: String): List<Long> {
        val ids = mutableListOf<Long>()
        system.query(DownloadManager.Query())?.use { rows ->
            val uri = rows.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)
            val id = rows.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            while (rows.moveToNext()) if (rows.getString(uri) == url) ids += rows.getLong(id)
        }
        return ids
    }
}
