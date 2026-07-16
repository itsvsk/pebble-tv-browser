package com.tvbrowser.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tvbrowser.core.db.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE isIncognito = 0 ORDER BY position ASC")
    fun observePersisted(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun get(id: String): TabEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tabs WHERE isIncognito = 0")
    suspend fun nextPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tab: TabEntity)

    @Update
    suspend fun update(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun delete(id: String)
}
