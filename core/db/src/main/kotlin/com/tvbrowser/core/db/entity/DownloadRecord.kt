package com.tvbrowser.core.db.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadRecord(
    @PrimaryKey val dmId: Long,
    val url: String,
    val filename: String,
    val localPath: String,
    val mime: String? = null,
    val totalBytes: Long = -1,
    val status: Int = STATUS_PENDING,
    val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL") val failureReason: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val userAgent: String? = null,
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_RUNNING = 1
        const val STATUS_PAUSED = 2
        const val STATUS_SUCCESS = 3
        const val STATUS_FAILED = 4
    }
}
