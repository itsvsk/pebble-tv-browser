package com.tvbrowser.core.db.repo

import com.tvbrowser.core.db.dao.BookmarkDao
import com.tvbrowser.core.db.dao.DownloadDao
import com.tvbrowser.core.db.dao.HistoryDao
import com.tvbrowser.core.db.dao.TabDao
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(private val dao: BookmarkDao) {
    fun all(): Flow<List<Bookmark>> = dao.observeAll()
    suspend fun isBookmarked(url: String) = dao.exists(url)
    suspend fun add(url: String, title: String, faviconUrl: String? = null) =
        dao.upsert(Bookmark(id = UUID.randomUUID().toString(), url = url, title = title, faviconUrl = faviconUrl))
    suspend fun remove(bookmark: Bookmark) = dao.delete(bookmark)
    suspend fun removeByUrl(url: String) = dao.deleteByUrl(url)
}

@Singleton
class HistoryRepository @Inject constructor(private val dao: HistoryDao) {
    fun recent() = dao.observeRecent()
    fun topVisited(limit: Int = 8) = dao.observeTopVisited(limit)
    suspend fun record(url: String, title: String?) {
        dao.insert(HistoryEntry(url = url, title = title))
    }
    suspend fun clear() = dao.clearAll()
    suspend fun deleteByUrl(url: String) = dao.deleteByUrl(url)
}

@Singleton
class TabRepository @Inject constructor(private val dao: TabDao) {
    fun persisted() = dao.observePersisted()
    suspend fun get(id: String) = dao.get(id)
    suspend fun upsert(tab: TabEntity) = dao.upsert(tab)
    suspend fun delete(id: String) = dao.delete(id)
}

@Singleton
class DownloadRepository @Inject constructor(private val dao: DownloadDao) {
    fun all(): Flow<List<DownloadRecord>> = dao.observeAll()
    suspend fun get(dmId: Long): DownloadRecord? = dao.get(dmId)
    suspend fun upsert(record: DownloadRecord) = dao.upsert(record)
    suspend fun updateStatus(dmId: Long, status: Int, failureReason: Int? = null) = dao.updateStatus(dmId, status, failureReason)
    suspend fun delete(dmId: Long) = dao.delete(dmId)
}
