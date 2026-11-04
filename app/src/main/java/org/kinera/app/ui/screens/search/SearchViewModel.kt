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

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val catalog: CatalogRepository,
    settings: SettingsRepository,
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

    private var nextOffset = 0

    /** Online sources available right now: MangaDex plus any configured servers. */
    val availableSources: StateFlow<List<MangaSource>> = settings.snapshot
        .map { current ->
            buildList {
                add(MangaSource.MANGADEX)
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
        performSearch(_query.value)
    }

    fun loadMore() {
        val query = _query.value.trim()
        if (query.isBlank() || _loading.value || _loadingMore.value || !_hasNext.value) return
        _loadingMore.value = true
        viewModelScope.launch {
            runCatching { catalog.search(query, source = _source.value, offset = nextOffset) }
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
        viewModelScope.launch {
            runCatching { catalog.search(query.trim(), source = _source.value, offset = 0) }
                .onSuccess { page ->
                    nextOffset = page.offset + page.items.size
                    _hasNext.value = page.hasNext
                    _results.value = page.items
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}
