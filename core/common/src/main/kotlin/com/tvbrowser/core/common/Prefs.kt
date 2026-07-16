package com.tvbrowser.core.common

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefStore by preferencesDataStore(name = "tv_browser_prefs")

object PrefKeys {
    val DesktopUa = booleanPreferencesKey("desktop_ua")
    val AdblockEnabled = booleanPreferencesKey("adblock_enabled")
    val DownloadPath = stringPreferencesKey("download_path")
    val CursorToggleLongPress = booleanPreferencesKey("cursor_toggle_longpress")
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.prefStore

    val desktopUa: Flow<Boolean> = store.data.map { it[PrefKeys.DesktopUa] ?: false }
    val adblockEnabled: Flow<Boolean> = store.data.map { it[PrefKeys.AdblockEnabled] ?: true }
    val downloadPath: Flow<String?> = store.data.map { it[PrefKeys.DownloadPath] }
    val cursorToggleLongPress: Flow<Boolean> = store.data.map { it[PrefKeys.CursorToggleLongPress] ?: true }

    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        store.edit { it[key] = value }
    }

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        store.edit { it[key] = value }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PrefsModule
