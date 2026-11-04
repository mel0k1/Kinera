package org.kinera.app.ui.screens.explore

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.util.UiState
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.localsource.ImportResult
import org.kinera.app.data.localsource.LocalSourceStorage
import org.kinera.app.data.repository.CatalogRepository

sealed interface ExploreTab {
    data object Popular : ExploreTab
    data object Latest : ExploreTab
    data object Local : ExploreTab
}

class ExploreViewModel(
    private val catalog: CatalogRepository,
    private val localStorage: LocalSourceStorage,
    private val folderStorage: FolderSourceStorage,
) : ViewModel() {

    private val _tab = MutableStateFlow<ExploreTab>(ExploreTab.Popular)
    val tab: StateFlow<ExploreTab> = _tab.asStateFlow()

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
                    ExploreTab.Popular -> catalog.popular(offset = nextOffset)
                    ExploreTab.Latest -> catalog.latest(offset = nextOffset)
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
