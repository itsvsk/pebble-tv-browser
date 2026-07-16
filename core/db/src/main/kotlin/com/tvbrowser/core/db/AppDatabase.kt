package com.tvbrowser.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tvbrowser.core.db.dao.BookmarkDao
import com.tvbrowser.core.db.dao.DownloadDao
import com.tvbrowser.core.db.dao.HistoryDao
import com.tvbrowser.core.db.dao.TabDao
import com.tvbrowser.core.db.entity.Bookmark
import com.tvbrowser.core.db.entity.DownloadRecord
import com.tvbrowser.core.db.entity.HistoryEntry
import com.tvbrowser.core.db.entity.MostVisitedView
import com.tvbrowser.core.db.entity.TabEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [Bookmark::class, HistoryEntry::class, TabEntity::class, DownloadRecord::class],
    views = [MostVisitedView::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun tabDao(): TabDao
    abstract fun downloadDao(): DownloadDao
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "tv_browser.db")
            .build()

    @Provides fun bookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun tabDao(db: AppDatabase): TabDao = db.tabDao()
    @Provides fun downloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}
