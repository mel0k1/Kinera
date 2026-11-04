package org.kinera.app.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.util.UiState
import org.kinera.app.data.repository.CatalogRepository
import org.kinera.app.data.repository.DownloadRepository
import org.kinera.app.data.repository.LibraryRepository
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.SettingsRepository

class ReaderViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
    private val downloads: DownloadRepository,
    private val settings: SettingsRepository,
    private val mangaId: String,
    private val chapterId: String,
) : ViewModel() {

    private val _manga = MutableStateFlow<Manga?>(null)
    val manga: StateFlow<Manga?> = _manga.asStateFlow()

    private val _chapters = MutableStateFlow<List<MangaChapter>>(emptyList())
    val chapters: StateFlow<List<MangaChapter>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _pages = MutableStateFlow<UiState<List<MangaPage>>>(UiState.Loading)
    val pages: StateFlow<UiState<List<MangaPage>>> = _pages.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _readerMode = MutableStateFlow(ReaderMode.WEBTOON)
    val readerMode: StateFlow<ReaderMode> = _readerMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _webtoonZoom = MutableStateFlow(true)
    val webtoonZoom: StateFlow<Boolean> = _webtoonZoom.asStateFlow()

    private var lastSavedPage = -1

    init {
        viewModelScope.launch {
            val prefs = settings.settings.first()
            _readerMode.value = prefs.readerMode
            _keepScreenOn.value = prefs.keepScreenOn
            _webtoonZoom.value = prefs.webtoonZoom
        }
        viewModelScope.launch {
            runCatching {
                val details = catalog.details(mangaId)
                _chapters.value = catalog.chapters(mangaId)
                details
            }.onSuccess { details ->
                _manga.value = details
                val index = _chapters.value.indexOfFirst { it.id == chapterId }
                val safeIndex = index.coerceAtLeast(0)
                _currentChapterIndex.value = safeIndex
                _chapters.value.getOrNull(safeIndex)?.let(::loadChapter)
            }
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        _readerMode.value = mode
        viewModelScope.launch { settings.setReaderMode(mode) }
    }

    fun onPageChanged(page: Int) {
        if (page == _currentPage.value) return
        _currentPage.value = page
        saveProgress(page)
    }

    fun retryPages() {
        val chapter = _chapters.value.getOrNull(_currentChapterIndex.value) ?: return
        loadChapter(chapter)
    }

    fun openChapter(index: Int) {
        val chapter = _chapters.value.getOrNull(index) ?: return
        _currentChapterIndex.value = index
        loadChapter(chapter)
    }

    fun nextChapter(): Boolean {
        val index = _currentChapterIndex.value
        if (index + 1 >= _chapters.value.size) return false
        // chapters are sorted oldest -> newest, so "next" moves forward in the list
        openChapter(index + 1)
        return true
    }

    fun previousChapter(): Boolean {
        val index = _currentChapterIndex.value
        if (index - 1 < 0) return false
        openChapter(index - 1)
        return true
    }

    private fun loadChapter(chapter: MangaChapter) {
        lastSavedPage = -1
        _pages.value = UiState.Loading
        _currentPage.value = 0
        viewModelScope.launch {
            // Downloaded chapters load straight from disk, everything else
            // comes from the catalog source.
            val local = if (chapter.source != MangaSource.LOCAL) {
                runCatching { downloads.localPages(mangaId, chapter.id) }.getOrNull()
            } else {
                null
            }
            runCatching {
                local ?: catalog.pages(chapter.id)
            }
                .onSuccess { list ->
                    if (list.isEmpty()) {
                        _pages.value = UiState.Error(null)
                    } else {
                        _pages.value = UiState.Success(list)
                        val saved = runCatching { library.getProgress(mangaId) }.getOrNull()
                        val resumeAt = if (chapter.id == saved?.chapterId) saved.page else 0
                        _currentPage.value = resumeAt.coerceIn(0, list.size - 1)
                        saveProgress(_currentPage.value)
                    }
                }
                .onFailure { _pages.value = UiState.Error(it.message) }
        }
    }

    private fun saveProgress(page: Int) {
        val chapter = _chapters.value.getOrNull(_currentChapterIndex.value) ?: return
        val manga = _manga.value ?: return
        if (page == lastSavedPage) return
        lastSavedPage = page
        val totalPages = (_pages.value as? UiState.Success)?.data?.size ?: 0
        viewModelScope.launch {
            library.saveProgress(
                manga = manga,
                chapterId = chapter.id,
                chapterLabel = chapter.label,
                page = page,
                total = totalPages,
            )
        }
    }
}
