package org.kinera.app.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.util.UiState
import org.kinera.app.data.repository.CatalogRepository

sealed interface ExploreTab {
    data object Popular : ExploreTab
    data object Latest : ExploreTab
}

class ExploreViewModel(private val catalog: CatalogRepository) : ViewModel() {

    private val _tab = MutableStateFlow<ExploreTab>(ExploreTab.Popular)
    val tab: StateFlow<ExploreTab> = _tab.asStateFlow()

    private val _items = MutableStateFlow<List<Manga>>(emptyList())
    val items: StateFlow<List<Manga>> = _items.asStateFlow()

    private val _state = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val state: StateFlow<UiState<Unit>> = _state.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: Boolean get() = _hasNext.value

    private var nextOffset = 0
    private var loading = false

    init {
        reload()
    }

    fun switchTab(tab: ExploreTab) {
        if (tab == _tab.value) return
        _tab.value = tab
        reload()
    }

    fun reload() {
        nextOffset = 0
        _items.value = emptyList()
        _state.value = UiState.Loading
        loadMore(reset = true)
    }

    fun loadMore() {
        if (loading) return
        loadMore(reset = false)
    }

    private fun loadMore(reset: Boolean) {
        loading = true
        viewModelScope.launch {
            runCatching {
                when (_tab.value) {
                    ExploreTab.Popular -> catalog.popular(offset = nextOffset)
                    ExploreTab.Latest -> catalog.latest(offset = nextOffset)
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
