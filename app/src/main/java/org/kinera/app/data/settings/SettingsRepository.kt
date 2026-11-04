package org.kinera.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kinera_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColors = booleanPreferencesKey("dynamic_colors")
        val readerMode = stringPreferencesKey("reader_mode")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val webtoonZoom = booleanPreferencesKey("webtoon_zoom")
        val komgaUrl = stringPreferencesKey("komga_url")
        val komgaUser = stringPreferencesKey("komga_user")
        val komgaPassword = stringPreferencesKey("komga_password")
        val suwayomiUrl = stringPreferencesKey("suwayomi_url")
        val suwayomiUser = stringPreferencesKey("suwayomi_user")
        val suwayomiPassword = stringPreferencesKey("suwayomi_password")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: Flow<KineraSettings> = context.dataStore.data.map { prefs ->
        KineraSettings(
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColors = prefs[Keys.dynamicColors] ?: false,
            readerMode = prefs[Keys.readerMode]?.let { runCatching { ReaderMode.valueOf(it) }.getOrNull() }
                ?: ReaderMode.WEBTOON,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            webtoonZoom = prefs[Keys.webtoonZoom] ?: true,
            komga = ServerConnection(
                url = prefs[Keys.komgaUrl].orEmpty(),
                user = prefs[Keys.komgaUser].orEmpty(),
                password = prefs[Keys.komgaPassword].orEmpty(),
            ),
            suwayomi = ServerConnection(
                url = prefs[Keys.suwayomiUrl].orEmpty(),
                user = prefs[Keys.suwayomiUser].orEmpty(),
                password = prefs[Keys.suwayomiPassword].orEmpty(),
            ),
        )
    }

    /**
     * Latest settings, updated eagerly. The HTTP auth interceptor reads
     * this instead of collecting a flow per request.
     */
    val snapshot: StateFlow<KineraSettings> = settings.stateIn(
        scope,
        SharingStarted.Eagerly,
        KineraSettings(),
    )

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

    suspend fun setServer(server: ServerId, connection: ServerConnection) {
        context.dataStore.edit { prefs ->
            when (server) {
                ServerId.KOMGA -> {
                    prefs[Keys.komgaUrl] = connection.url.trim()
                    prefs[Keys.komgaUser] = connection.user.trim()
                    prefs[Keys.komgaPassword] = connection.password
                }
                ServerId.SUWAYOMI -> {
                    prefs[Keys.suwayomiUrl] = connection.url.trim()
                    prefs[Keys.suwayomiUser] = connection.user.trim()
                    prefs[Keys.suwayomiPassword] = connection.password
                }
            }
        }
    }
}

enum class ServerId { KOMGA, SUWAYOMI }
