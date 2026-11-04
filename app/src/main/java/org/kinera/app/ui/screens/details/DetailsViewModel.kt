package org.kinera.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.util.UiState
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.local.entity.HistoryEntity
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.DownloadRepository
import org.kinera.app.data.repository.LibraryRepository

class DetailsViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val downloads: DownloadRepository,
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

    private val _downloadStates = MutableStateFlow<Map<String, DownloadEntity>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadEntity>> = _downloadStates.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            library.observeFavorite(mangaId).collect { _isFavorite.value = it }
        }
        viewModelScope.launch {
            downloads.forManga(mangaId).collect { rows ->
                _downloadStates.value = rows.associateBy { it.chapterId }
            }
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

    fun downloadChapter(chapter: MangaChapter) {
        val manga = (_manga.value as? UiState.Success)?.data ?: return
        if (manga.source == MangaSource.LOCAL) return
        viewModelScope.launch {
            downloads.enqueue(manga, listOf(chapter))
            _message.value = R.string.details_download_queued
        }
    }

    fun cancelDownload(chapterId: String) {
        viewModelScope.launch {
            downloads.cancel(chapterId)
        }
    }

    fun downloadAll() {
        val manga = (_manga.value as? UiState.Success)?.data ?: return
        if (manga.source == MangaSource.LOCAL || _chapters.value.isEmpty()) return
        viewModelScope.launch {
            downloads.enqueue(manga, _chapters.value)
            _message.value = R.string.details_download_all_queued
        }
    }

    fun deleteDownload(chapterId: String) {
        viewModelScope.launch {
            downloads.deleteDownloaded(chapterId)
            _message.value = R.string.details_download_deleted
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
