package com.tvbrowser.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN failureReason INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN userAgent TEXT DEFAULT NULL")
            }
        }
    }

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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides fun bookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun tabDao(db: AppDatabase): TabDao = db.tabDao()
    @Provides fun downloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}
