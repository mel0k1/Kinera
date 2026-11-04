package org.kinera.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.data.repository.CatalogRepository

@OptIn(FlowPreview::class)
class SearchViewModel(private val catalog: CatalogRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Manga>>(emptyList())
    val results: StateFlow<List<Manga>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            _error.value = null
            return
        }
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { catalog.search(query.trim(), offset = 0) }
                .onSuccess { _results.value = it.items }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}
