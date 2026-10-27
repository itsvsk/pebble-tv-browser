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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import androidx.datastore.preferences.core.emptyPreferences
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefStore by preferencesDataStore(name = "tv_browser_prefs")

enum class ThemePreference(val label: String) {
    System("System default"), Light("Light"), Dark("Dark");

    companion object {
        fun fromStored(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: System
    }
}

object PrefKeys {
    val DesktopUa = booleanPreferencesKey("desktop_ua")
    val AdblockEnabled = booleanPreferencesKey("adblock_enabled")
    val DownloadPath = stringPreferencesKey("download_path")
    val CursorToggleLongPress = booleanPreferencesKey("cursor_toggle_longpress")
    val Theme = stringPreferencesKey("theme_preference")
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.prefStore
    private val snapshots = store.data.map<Preferences, Preferences?> { it }
        .stateIn(CoroutineScope(SupervisorJob() + Dispatchers.IO), SharingStarted.Eagerly, null)
    private val preferences get() = snapshots.filterNotNull()
    fun pagePreferences(url: String): Flow<SitePreferences> = preferences.map { it.sitePreferences(url) }
    fun cachedPagePreferences(url: String): SitePreferences = (snapshots.value ?: emptyPreferences()).sitePreferences(url)
    suspend fun setSiteDesktop(url: String, value: Boolean) {
        val host = normalizedSiteHost(url) ?: return
        store.edit { it[siteDesktopKey(host)] = value }
    }
    suspend fun setSiteAdblock(url: String, value: Boolean) {
        val host = normalizedSiteHost(url) ?: return
        store.edit { it[siteAdblockKey(host)] = value }
    }
    suspend fun resetSite(url: String) {
        val host = normalizedSiteHost(url) ?: return
        store.edit { it.remove(siteDesktopKey(host)); it.remove(siteAdblockKey(host)) }
    }

    val desktopUa: Flow<Boolean> = preferences.map { it[PrefKeys.DesktopUa] ?: false }
    val adblockEnabled: Flow<Boolean> = preferences.map { it[PrefKeys.AdblockEnabled] ?: true }
    val downloadPath: Flow<String?> = preferences.map { it[PrefKeys.DownloadPath] }
    val cursorToggleLongPress: Flow<Boolean> = preferences.map { it[PrefKeys.CursorToggleLongPress] ?: true }
    val theme: Flow<ThemePreference> = preferences.map { ThemePreference.fromStored(it[PrefKeys.Theme]) }

    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        store.edit { it[key] = value }
    }

    suspend fun setString(key: Preferences.Key<String>, value: String) {
        store.edit { it[key] = value }
    }

    suspend fun setTheme(value: ThemePreference) {
        setString(PrefKeys.Theme, value.name)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PrefsModule
