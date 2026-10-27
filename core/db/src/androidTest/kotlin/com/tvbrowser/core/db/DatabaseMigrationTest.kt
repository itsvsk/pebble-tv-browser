package com.tvbrowser.core.db

import android.app.DownloadManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Creates a genuine v1 file from the exported schema, then lets Room validate the upgrade. */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test
    fun versionOneUpgradePreservesAllUserDataAndPersistsTheNewFailureReason() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-one-to-two-${UUID.randomUUID()}.db"
        val schema = instrumentation.context.assets.open("com.tvbrowser.core.db.AppDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val bookmark = Bookmark("legacy-bookmark", "https://example.com/article", "Saved article",
            faviconUrl = "https://example.com/icon.png", folder = "Reading", createdAt = 11L)
        val history = HistoryEntry(id = 61L, url = bookmark.url, title = "Visited article", visitedAt = 22L)
        val tab = TabEntity("legacy-tab", bookmark.url, title = "Open article", position = 3,
            createdAt = 33L, lastActiveAt = 44L)
        val failedDownload = DownloadRecord(41L, "https://example.com/file.pdf", "file.pdf", "/fixture/file.pdf",
            mime = "application/pdf", totalBytes = 512L, status = DownloadRecord.STATUS_FAILED, startedAt = 55L)
        val runningDownload = DownloadRecord(42L, "https://example.com/stream", "stream", "/fixture/stream",
            totalBytes = -1L, status = DownloadRecord.STATUS_RUNNING, startedAt = 66L)
        try {
            createVersionOne(context, name, schema) { database ->
                database.execSQL("INSERT INTO bookmarks (id, url, title, faviconUrl, folder, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(bookmark.id, bookmark.url, bookmark.title, bookmark.faviconUrl, bookmark.folder, bookmark.createdAt))
                database.execSQL("INSERT INTO history (id, url, title, visitedAt) VALUES (?, ?, ?, ?)",
                    arrayOf(history.id, history.url, history.title, history.visitedAt))
                database.execSQL("INSERT INTO tabs (id, url, title, isIncognito, position, createdAt, lastActiveAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(tab.id, tab.url, tab.title, 0, tab.position, tab.createdAt, tab.lastActiveAt))
                for (download in listOf(failedDownload, runningDownload)) {
                    database.execSQL("INSERT INTO downloads (dmId, url, filename, localPath, mime, totalBytes, status, startedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(download.dmId, download.url, download.filename, download.localPath, download.mime,
                            download.totalBytes, download.status, download.startedAt))
                }
            }

            val migrated = openCurrent(context, name)
            try {
                assertEquals("Opening must actually upgrade the seeded v1 file", 2, migrated.openHelper.writableDatabase.version)
                assertEquals(bookmark, migrated.bookmarkDao().observeAll().first().single())
                assertEquals(history, migrated.historyDao().observeRecent().first().single())
                assertEquals(tab, migrated.tabDao().get(tab.id))
                assertEquals("The existing SQL view must remain usable", bookmark.url,
                    migrated.historyDao().observeTopVisited().first().single().url)
                assertEquals(failedDownload, migrated.downloadDao().get(failedDownload.dmId))
                assertEquals(runningDownload, migrated.downloadDao().get(runningDownload.dmId))
                assertNull("Existing failures have no historical system reason", migrated.downloadDao().get(failedDownload.dmId)!!.failureReason)
                migrated.openHelper.writableDatabase.query("PRAGMA table_info(downloads)").use { columns ->
                    var found = false
                    while (columns.moveToNext()) {
                        if (columns.getString(columns.getColumnIndexOrThrow("name")) == "failureReason") {
                            found = true
                            assertEquals("Existing rows require a nullable reason", 0, columns.getInt(columns.getColumnIndexOrThrow("notnull")))
                            assertEquals("INTEGER", columns.getString(columns.getColumnIndexOrThrow("type")))
                        }
                    }
                    assertTrue("Migration must add the reason column", found)
                }
                migrated.downloadDao().upsert(failedDownload.copy(failureReason = DownloadManager.ERROR_INSUFFICIENT_SPACE))
            } finally {
                migrated.close()
            }

            val reopened = openCurrent(context, name)
            try {
                assertEquals(failedDownload.copy(failureReason = DownloadManager.ERROR_INSUFFICIENT_SPACE),
                    reopened.downloadDao().get(failedDownload.dmId))
                assertEquals(runningDownload, reopened.downloadDao().get(runningDownload.dmId))
                assertEquals(bookmark, reopened.bookmarkDao().observeAll().first().single())
                assertEquals(history, reopened.historyDao().observeRecent().first().single())
                assertEquals(tab, reopened.tabDao().get(tab.id))
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun openCurrent(context: Context, name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    private fun createVersionOne(context: Context, name: String, schema: JSONObject, seed: (SQLiteDatabase) -> Unit) {
        assertEquals("This fixture must remain the exported v1 schema", 1, schema.getInt("version"))
        val file = context.getDatabasePath(name)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.beginTransaction()
            try {
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    val table = entity.getString("tableName")
                    database.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.getJSONArray("indices")
                    for (i in 0 until indices.length()) {
                        database.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val views = schema.getJSONArray("views")
                for (index in 0 until views.length()) {
                    val view = views.getJSONObject(index)
                    database.execSQL(view.getString("createSql").replace("\${VIEW_NAME}", view.getString("viewName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (index in 0 until setup.length()) database.execSQL(setup.getString(index))
                seed(database)
                database.version = schema.getInt("version")
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
    }
}
