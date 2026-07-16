package com.tvbrowser.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.entity.MostVisitedView
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry): Long

    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM most_visited LIMIT :limit")
    fun observeTopVisited(limit: Int = 8): Flow<List<MostVisitedView>>

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
