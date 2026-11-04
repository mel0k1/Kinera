package org.kinera.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kinera_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColors = booleanPreferencesKey("dynamic_colors")
        val readerMode = stringPreferencesKey("reader_mode")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val webtoonZoom = booleanPreferencesKey("webtoon_zoom")
    }

    val settings: Flow<KineraSettings> = context.dataStore.data.map { prefs ->
        KineraSettings(
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColors = prefs[Keys.dynamicColors] ?: false,
            readerMode = prefs[Keys.readerMode]?.let { runCatching { ReaderMode.valueOf(it) }.getOrNull() }
                ?: ReaderMode.WEBTOON,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            webtoonZoom = prefs[Keys.webtoonZoom] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.dynamicColors] = enabled }
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        context.dataStore.edit { it[Keys.readerMode] = mode.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setWebtoonZoom(enabled: Boolean) {
        context.dataStore.edit { it[Keys.webtoonZoom] = enabled }
    }
}
