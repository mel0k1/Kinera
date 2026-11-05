package org.kinera.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaSource
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.suwayomi.SuwayomiSource
import org.kinera.app.data.suwayomi.SuwayomiSourceOption

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val catalog: CatalogRepository,
    settings: SettingsRepository,
    private val suwayomi: SuwayomiSource,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _source = MutableStateFlow(MangaSource.MANGADEX)
    val source: StateFlow<MangaSource> = _source.asStateFlow()

    private val _results = MutableStateFlow<List<Manga>>(emptyList())
    val results: StateFlow<List<Manga>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    /** Sources installed on the Suwayomi server, for the second chip row. */
    private val _suwayomiSources = MutableStateFlow<List<SuwayomiSourceOption>>(emptyList())
    val suwayomiSources: StateFlow<List<SuwayomiSourceOption>> = _suwayomiSources.asStateFlow()

    /** Picked server source for Suwayomi search; null = the automatic pick. */
    private val _suwayomiSourceId = MutableStateFlow<String?>(null)
    val suwayomiSourceId: StateFlow<String?> = _suwayomiSourceId.asStateFlow()

    private var suwayomiSourcesRequested = false

    private var nextOffset = 0
    /** Bumped on every new search; late responses of stale runs are dropped. */
    private var searchGeneration = 0

    /** Online sources: MangaDex and MangaLib are public, servers add more. */
    val availableSources: StateFlow<List<MangaSource>> = settings.snapshot
        .map { current ->
            buildList {
                add(MangaSource.MANGADEX)
                add(MangaSource.MANGALIB)
                add(MangaSource.REMANGA)
                add(MangaSource.DESUME)
                if (current.komga.isConfigured) add(MangaSource.KOMGA)
                if (current.suwayomi.isConfigured) add(MangaSource.SUWAYOMI)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(MangaSource.MANGADEX))

    init {
        _query
            .debounce(400)
            .distinctUntilChanged()
            .onEach { performSearch(it) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun selectSource(source: MangaSource) {
        if (source == _source.value || source == MangaSource.LOCAL) return
        _source.value = source
        if (source == MangaSource.SUWAYOMI) {
            loadSuwayomiSources()
        } else {
            // The server source only applies to Suwayomi results.
            _suwayomiSourceId.value = null
        }
        performSearch(_query.value)
    }

    /** Picks which source of the Suwayomi server to search in. */
    fun selectSuwayomiSource(sourceId: String?) {
        if (sourceId == _suwayomiSourceId.value) return
        _suwayomiSourceId.value = sourceId
        performSearch(_query.value)
    }

    /**
     * Fetches the sources installed on the Suwayomi server once per
     * screen; a failure leaves the row hidden and is retried the next
     * time Suwayomi is picked.
     */
    private fun loadSuwayomiSources() {
        if (suwayomiSourcesRequested && _suwayomiSources.value.isNotEmpty()) return
        suwayomiSourcesRequested = true
        viewModelScope.launch {
            _suwayomiSources.value = runCatching { suwayomi.availableSources() }.getOrDefault(emptyList())
        }
    }

    fun loadMore() {
        val query = _query.value.trim()
        if (query.isBlank() || _loading.value || _loadingMore.value || !_hasNext.value) return
        _loadingMore.value = true
        viewModelScope.launch {
            runCatching {
                catalog.search(
                    query,
                    source = _source.value,
                    offset = nextOffset,
                    suwayomiSourceId = activeSuwayomiSourceId(),
                )
            }
                .onSuccess { page ->
                    nextOffset = page.offset + page.items.size
                    _hasNext.value = page.hasNext
                    _results.value = (_results.value + page.items).distinctBy { it.id }
                }
                .onFailure { _error.value = it.message }
            _loadingMore.value = false
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            _error.value = null
            _hasNext.value = false
            return
        }
        _loading.value = true
        _error.value = null
        nextOffset = 0
        val generation = ++searchGeneration
        viewModelScope.launch {
            runCatching {
                catalog.search(
                    query.trim(),
                    source = _source.value,
                    offset = 0,
                    suwayomiSourceId = activeSuwayomiSourceId(),
                )
            }
                .onSuccess { page ->
                    if (generation != searchGeneration) return@launch
                    nextOffset = page.offset + page.items.size
                    _hasNext.value = page.hasNext
                    _results.value = page.items
                }
                .onFailure { if (generation == searchGeneration) _error.value = it.message }
            if (generation == searchGeneration) _loading.value = false
        }
    }

    /** Server source for the running search, only meaningful for Suwayomi. */
    private fun activeSuwayomiSourceId(): String? =
        if (_source.value == MangaSource.SUWAYOMI) _suwayomiSourceId.value else null
}
