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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DownloadRecord)

    @Query("UPDATE downloads SET status = :status WHERE dmId = :dmId")
    suspend fun updateStatus(dmId: Long, status: Int)

    @Query("DELETE FROM downloads WHERE dmId = :dmId")
    suspend fun delete(dmId: Long)
}
