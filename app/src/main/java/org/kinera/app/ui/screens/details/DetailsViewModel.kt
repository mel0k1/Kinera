package org.kinera.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.util.UiState
import org.kinera.app.data.local.entity.HistoryEntity
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.LibraryRepository

class DetailsViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val mangaId: String,
) : ViewModel() {

    private val _manga = MutableStateFlow<UiState<Manga>>(UiState.Loading)
    val manga: StateFlow<UiState<Manga>> = _manga.asStateFlow()

    private val _chapters = MutableStateFlow<List<MangaChapter>>(emptyList())
    val chapters: StateFlow<List<MangaChapter>> = _chapters.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _progress = MutableStateFlow<HistoryEntity?>(null)
    val progress: StateFlow<HistoryEntity?> = _progress.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            library.observeFavorite(mangaId).collect { _isFavorite.value = it }
        }
    }

    fun load() {
        viewModelScope.launch {
            _manga.value = UiState.Loading
            runCatching { catalog.details(mangaId) }
                .onSuccess { details ->
                    _manga.value = UiState.Success(details)
                    runCatching { catalog.chapters(mangaId) }
                        .onSuccess { _chapters.value = it }
                    _progress.value = runCatching { library.getProgress(mangaId) }.getOrNull()
                }
                .onFailure { _manga.value = UiState.Error(it.message) }
        }
    }

    fun toggleFavorite() {
        val current = (_manga.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { library.toggleFavorite(current) }
    }
}
