package com.tvbrowser.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks", indices = [Index("url", unique = true)])
data class Bookmark(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val folder: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
