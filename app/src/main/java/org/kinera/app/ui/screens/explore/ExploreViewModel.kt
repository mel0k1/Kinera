package org.kinera.app.ui.screens.explore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.util.UiState
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.ImportResult
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.settings.SettingsRepository

sealed interface ExploreTab {
    data object Popular : ExploreTab
    data object Latest : ExploreTab
    data object Local : ExploreTab
}

class ExploreViewModel(
    private val catalog: CatalogRepository,
    private val localStorage: LocalSourceStorage,
    private val folderStorage: FolderSourceStorage,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _tab = MutableStateFlow<ExploreTab>(ExploreTab.Popular)
    val tab: StateFlow<ExploreTab> = _tab.asStateFlow()

    /** Catalog source the Popular / Latest tabs read from. */
    private val _source = MutableStateFlow(MangaSource.MANGADEX)
    val source: StateFlow<MangaSource> = _source.asStateFlow()

    private val _items = MutableStateFlow<List<Manga>>(emptyList())
    val items: StateFlow<List<Manga>> = _items.asStateFlow()

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val state: StateFlow<UiState<Unit>> = _state.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: Boolean get() = _hasNext.value

    /** Archives imported from the device, shown on the Local tab. */
    val localManga: StateFlow<List<Manga>> = combine(
        localStorage.library,
        folderStorage.library,
    ) { archives, folders -> (archives + folders).sortedBy { it.title.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Online sources: MangaDex and MangaLib are public, servers add more. */
    val availableSources: StateFlow<List<MangaSource>> = settings.snapshot
        .map { current ->
            buildList {
                add(MangaSource.MANGADEX)
                add(MangaSource.MANGALIB)
                if (current.komga.isConfigured) add(MangaSource.KOMGA)
                if (current.suwayomi.isConfigured) add(MangaSource.SUWAYOMI)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(MangaSource.MANGADEX))

    /** MangaDex chapter languages enabled in Settings; empty means no filter. */
    val mangadexLanguages: StateFlow<Set<String>> = settings.snapshot
        .map { it.mangadexLanguages }
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("en", "ru"))

    /** Toggles a MangaDex language chip and refreshes the MangaDex feed. */
    fun toggleMangadexLanguage(language: String) {
        val enable = language !in mangadexLanguages.value
        viewModelScope.launch {
            settings.setMangadexLanguage(language, enable)
            if (_source.value == MangaSource.MANGADEX && _tab.value != ExploreTab.Local) {
                reload()
            }
        }
    }

    /** One-shot result of the latest import, consumed by the screen. */
    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private var nextOffset = 0
    private var loading = false

    init {
        reload()
    }

    fun switchTab(tab: ExploreTab) {
        if (tab == _tab.value) return
        _tab.value = tab
        if (tab == ExploreTab.Local) {
            _hasNext.value = false
            _state.value = UiState.Success(Unit)
        } else {
            reload()
        }
    }

    fun selectSource(source: MangaSource) {
        if (source == _source.value || source == MangaSource.LOCAL) return
        _source.value = source
        if (_tab.value == ExploreTab.Local) {
            _tab.value = ExploreTab.Popular
        }
        reload()
    }

    fun reload() {
        if (_tab.value == ExploreTab.Local) {
            _state.value = UiState.Success(Unit)
            return
        }
        nextOffset = 0
        _items.value = emptyList()
        _state.value = UiState.Loading
        loadMore(reset = true)
    }

    fun loadMore() {
        if (loading || _tab.value == ExploreTab.Local) return
        loadMore(reset = false)
    }

    fun importArchives(uris: List<Uri>) {
        viewModelScope.launch {
            _importResult.value = localStorage.import(uris)
        }
    }

    fun consumeImportResult() {
        _importResult.value = null
    }

    /** One-shot result of the latest folder import, consumed by the screen. */
    private val _folderImport = MutableStateFlow<FolderSourceStorage.ImportResult?>(null)
    val folderImport: StateFlow<FolderSourceStorage.ImportResult?> = _folderImport.asStateFlow()

    fun importFolder(treeUri: Uri) {
        viewModelScope.launch {
            _folderImport.value = folderStorage.add(treeUri)
        }
    }

    fun consumeFolderImport() {
        _folderImport.value = null
    }

    private fun loadMore(reset: Boolean) {
        loading = true
        viewModelScope.launch {
            runCatching {
                when (_tab.value) {
                    ExploreTab.Popular -> catalog.popular(source = _source.value, offset = nextOffset)
                    ExploreTab.Latest -> catalog.latest(source = _source.value, offset = nextOffset)
                    ExploreTab.Local -> error("Local archive list is driven by storage")
                }
            }.onSuccess { page ->
                nextOffset = page.offset + page.items.size
                _hasNext.value = page.hasNext
                _items.update { current ->
                    if (reset) page.items else (current + page.items).distinctBy { it.id }
                }
                _state.value = UiState.Success(Unit)
            }.onFailure {
                if (reset) _state.value = UiState.Error(it.message)
            }
            loading = false
        }
    }
}
