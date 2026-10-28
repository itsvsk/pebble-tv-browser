package com.tvbrowser.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tvbrowser.core.db.entity.DownloadRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<DownloadRecord>>

    @Query("SELECT * FROM downloads WHERE dmId = :dmId")
    suspend fun get(dmId: Long): DownloadRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DownloadRecord)

    @Query("UPDATE downloads SET status = :status, failureReason = :failureReason WHERE dmId = :dmId")
    suspend fun updateStatus(dmId: Long, status: Int, failureReason: Int? = null)

    @Query("DELETE FROM downloads WHERE dmId = :dmId")
    suspend fun delete(dmId: Long)
}
