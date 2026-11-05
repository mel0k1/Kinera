package org.kinera.app.ui.screens.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.core.util.UiState
import org.kinera.app.data.desume.DesuMeGenreOption
import org.kinera.app.data.desume.DesuMeSource
import org.kinera.app.data.mangalib.MangaLibSource
import org.kinera.app.data.mangalib.dto.MangaLibGenre
import org.kinera.app.data.remanga.RemangaSource
import org.kinera.app.data.remanga.dto.RemangaGenre
import org.kinera.app.data.settings.KineraSettings
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.suwayomi.SuwayomiSource
import org.kinera.app.data.suwayomi.SuwayomiSourceOption

/**
 * One screen for everything source related: MangaDex languages, catalog
 * filters of the RU sources, self-hosted servers and the Suwayomi source
 * picker. The state lives in SettingsRepository, so Explore reacts to the
 * changes on its own.
 */
class SourcesViewModel(
    private val repository: SettingsRepository,
    private val mangalib: MangaLibSource,
    private val remanga: RemangaSource,
    private val desume: DesuMeSource,
    private val suwayomi: SuwayomiSource,
) : ViewModel() {

    val settings: StateFlow<KineraSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KineraSettings())

    fun setMangadexLanguage(language: String, enabled: Boolean) = viewModelScope.launch {
        repository.setMangadexLanguage(language, enabled)
    }

    fun toggleMangalibStatus(id: Int) {
        val enable = id !in settings.value.mangalibStatuses
        viewModelScope.launch { repository.setMangalibStatus(id, enable) }
    }

    fun toggleMangalibGenre(id: Int) {
        val enable = id !in settings.value.mangalibGenres
        viewModelScope.launch { repository.setMangalibGenre(id, enable) }
    }

    fun clearMangalibFilters() = viewModelScope.launch { repository.clearMangalibFilters() }

    fun toggleRemangaStatus(id: Int) {
        val enable = id !in settings.value.remangaStatuses
        viewModelScope.launch { repository.setRemangaStatus(id, enable) }
    }

    fun toggleRemangaGenre(id: Int) {
        val enable = id !in settings.value.remangaGenres
        viewModelScope.launch { repository.setRemangaGenre(id, enable) }
    }

    fun clearRemangaFilters() = viewModelScope.launch { repository.clearRemangaFilters() }

    fun toggleDesumeGenre(slug: String) {
        val enable = slug !in settings.value.desumeGenres
        viewModelScope.launch { repository.setDesumeGenre(slug, enable) }
    }

    fun clearDesumeFilters() = viewModelScope.launch { repository.clearDesumeFilters() }

    fun saveServer(server: ServerId, connection: ServerConnection) = viewModelScope.launch {
        repository.setServer(server, connection.copy(url = ServerConnection.normalizeUrl(connection.url)))
        if (server == ServerId.SUWAYOMI) loadSuwayomiSources(force = true)
    }

    fun selectSuwayomiSource(sourceId: String) = viewModelScope.launch {
        repository.setSuwayomiSource(sourceId)
    }

    /* ----- genre options for the filter sheets ----- */

    private val _mangalibGenres = MutableStateFlow<List<MangaLibGenre>>(emptyList())
    val mangalibGenreOptions: StateFlow<List<MangaLibGenre>> = _mangalibGenres.asStateFlow()

    private val _remangaGenres = MutableStateFlow<List<RemangaGenre>>(emptyList())
    val remangaGenreOptions: StateFlow<List<RemangaGenre>> = _remangaGenres.asStateFlow()

    private val _desumeGenres = MutableStateFlow<List<DesuMeGenreOption>>(emptyList())
    val desumeGenreOptions: StateFlow<List<DesuMeGenreOption>> = _desumeGenres.asStateFlow()

    /** Fetches a genre list once when its sheet is about to open; retries on failure. */
    fun loadGenresIfNeeded(source: GenreSheetSource) {
        when (source) {
            GenreSheetSource.MANGALIB ->
                if (_mangalibGenres.value.isEmpty()) {
                    viewModelScope.launch {
                        _mangalibGenres.value = runCatching { mangalib.genres() }.getOrDefault(emptyList())
                    }
                }
            GenreSheetSource.REMANGA ->
                if (_remangaGenres.value.isEmpty()) {
                    viewModelScope.launch {
                        _remangaGenres.value = runCatching { remanga.genres() }.getOrDefault(emptyList())
                    }
                }
            GenreSheetSource.DESUME ->
                if (_desumeGenres.value.isEmpty()) {
                    viewModelScope.launch {
                        _desumeGenres.value = runCatching { desume.genres() }.getOrDefault(emptyList())
                    }
                }
        }
    }

    /* ----- Suwayomi catalog source picker ----- */

    private val _suwayomiSources = MutableStateFlow<UiState<List<SuwayomiSourceOption>>>(UiState.Loading)
    val suwayomiSources: StateFlow<UiState<List<SuwayomiSourceOption>>> = _suwayomiSources.asStateFlow()

    private var suwayomiLoadGeneration = 0

    init {
        loadSuwayomiSources()
    }

    fun loadSuwayomiSources(force: Boolean = false) {
        if (!repository.snapshot.value.suwayomi.isConfigured) {
            suwayomiLoadGeneration++
            _suwayomiSources.value = UiState.Error(null)
            return
        }
        if (!force && _suwayomiSources.value is UiState.Loading) return
        val generation = ++suwayomiLoadGeneration
        _suwayomiSources.value = UiState.Loading
        viewModelScope.launch {
            runCatching { suwayomi.availableSources() }
                .onSuccess { if (generation == suwayomiLoadGeneration) _suwayomiSources.value = UiState.Success(it) }
                .onFailure { if (generation == suwayomiLoadGeneration) _suwayomiSources.value = UiState.Error(it.message) }
        }
    }
}

/** Which genre sheet the Sources screen wants to open. */
enum class GenreSheetSource { MANGALIB, REMANGA, DESUME }
