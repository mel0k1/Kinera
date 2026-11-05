package org.kinera.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val readerRtl = booleanPreferencesKey("reader_rtl")
        val mangadexLanguages = stringSetPreferencesKey("mangadex_languages")
        val mangalibStatuses = stringSetPreferencesKey("mangalib_statuses")
        val mangalibGenres = stringSetPreferencesKey("mangalib_genres")
        val suwayomiSourceId = stringPreferencesKey("suwayomi_source_id")
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
            readerRtl = prefs[Keys.readerRtl] ?: false,
            mangadexLanguages = prefs[Keys.mangadexLanguages]
                ?.filter { it in SUPPORTED_LANGUAGES }
                ?.toSet()
                ?: setOf("en", "ru"),
            mangalibStatuses = prefs[Keys.mangalibStatuses]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
            mangalibGenres = prefs[Keys.mangalibGenres]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
            suwayomiSourceId = prefs[Keys.suwayomiSourceId].orEmpty(),
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

    suspend fun setReaderRtl(enabled: Boolean) {
        context.dataStore.edit { it[Keys.readerRtl] = enabled }
    }

    /**
     * Toggles one MangaDex chapter language. When every language ends up
     * disabled the filter is lifted entirely (empty set = all languages).
     */
    suspend fun setMangadexLanguage(language: String, enabled: Boolean) {
        val lang = language.lowercase()
        if (lang !in SUPPORTED_LANGUAGES) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.mangadexLanguages]
                ?.filter { it in SUPPORTED_LANGUAGES }
                ?.toSet()
                ?: setOf("en", "ru")
            prefs[Keys.mangadexLanguages] =
                if (enabled) current + lang else current - lang
        }
    }

    suspend fun setSuwayomiSource(sourceId: String) {
        context.dataStore.edit { it[Keys.suwayomiSourceId] = sourceId.trim() }
    }

    /** Toggles one MangaLib status id in the catalog filter. */
    suspend fun setMangalibStatus(id: Int, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.mangalibStatuses]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            prefs[Keys.mangalibStatuses] =
                (if (enabled) current + id else current - id)
                    .map { it.toString() }
                    .toSet()
        }
    }

    /** Toggles one MangaLib genre id in the catalog filter. */
    suspend fun setMangalibGenre(id: Int, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.mangalibGenres]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            prefs[Keys.mangalibGenres] =
                (if (enabled) current + id else current - id)
                    .map { it.toString() }
                    .toSet()
        }
    }

    /** Lifts every MangaLib catalog filter at once. */
    suspend fun clearMangalibFilters() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.mangalibStatuses)
            prefs.remove(Keys.mangalibGenres)
        }
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

/** MangaDex chapter languages Kinera can filter by. */
val SUPPORTED_LANGUAGES = setOf("en", "ru")
