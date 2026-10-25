package com.tvbrowser.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Version 1 baseline. Add migration tests from this schema before increasing AppDatabase.version. */
@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {
    @Test
    fun versionOneUserDataSurvivesCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "room-version-one-persistence-test.db"
        context.deleteDatabase(name)

        try {
            val initial = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                initial.bookmarkDao().upsert(Bookmark(id = "bookmark", url = "https://example.com", title = "Example", createdAt = 1L))
                initial.historyDao().insert(HistoryEntry(url = "https://example.com", title = "Example", visitedAt = 2L))
                initial.tabDao().upsert(TabEntity(id = "tab", url = "https://example.com", createdAt = 3L, lastActiveAt = 3L))
                initial.downloadDao().upsert(DownloadRecord(dmId = 4L, url = "https://example.com/file", filename = "file", localPath = "/tmp/file", startedAt = 4L))
            } finally {
                initial.close()
            }

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                assertEquals("bookmark", reopened.bookmarkDao().observeAll().first().single().id)
                assertEquals("https://example.com", reopened.historyDao().observeRecent().first().single().url)
                assertNotNull(reopened.tabDao().get("tab"))
                assertEquals(4L, reopened.downloadDao().observeAll().first().single().dmId)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
