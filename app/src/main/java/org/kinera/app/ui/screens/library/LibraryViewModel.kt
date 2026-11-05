package org.kinera.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.repository.DownloadRepository
import org.kinera.app.data.repository.FavouriteManga
import org.kinera.app.data.repository.LibraryRepository
import org.kinera.app.data.repository.LibraryUpdateSummary
import org.kinera.app.data.repository.LibraryUpdater

class LibraryViewModel(
    private val library: LibraryRepository,
    private val updater: LibraryUpdater,
    private val downloads: DownloadRepository,
) : ViewModel() {

    val favourites = library.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories = library.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** In-flight manual library check, null while idle. */
    private val _refresh = MutableStateFlow<LibraryRefreshState>(LibraryRefreshState.Idle)
    val refresh = _refresh.asStateFlow()

    /**
     * Removes the title from the shelf and wipes its downloaded chapters
     * (rows and on-disk files) in the same pass.
     */
    fun removeFavorite(manga: Manga) {
        viewModelScope.launch {
            library.toggleFavorite(manga)
            downloads.deleteManga(manga.id)
        }
    }

    fun addCategory(name: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (name.isNotBlank()) library.addCategory(name)
            onDone()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { library.deleteCategory(category.id) }
    }

    /** Persists a drag and drop swap of the category chips row. */
    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val categoryId = categories.value.getOrNull(fromIndex)?.id ?: return
        viewModelScope.launch { library.moveCategory(categoryId, toIndex) }
    }

    /** Puts one favourite into a category; null clears the assignment. */
    fun assignCategory(manga: Manga, categoryId: Long?) {
        viewModelScope.launch { library.assignCategory(manga.id, categoryId) }
    }

    /**
     * Runs one update pass right away. The periodic worker covers the
     * background case; this gives the user instant feedback.
     */
    fun refreshLibrary() {
        if (_refresh.value is LibraryRefreshState.Running) return
        _refresh.value = LibraryRefreshState.Running
        viewModelScope.launch {
            val summary = runCatching { updater.updateAll() }.getOrNull()
            _refresh.value = when (summary) {
                null -> LibraryRefreshState.Failed
                else -> LibraryRefreshState.Done(summary)
            }
        }
    }

    /** Marks the snackbar consumed without touching the update result. */
    fun consumeRefreshState() {
        if (_refresh.value !is LibraryRefreshState.Running) {
            _refresh.value = LibraryRefreshState.Idle
        }
    }
}

sealed interface LibraryRefreshState {
    data object Idle : LibraryRefreshState
    data object Running : LibraryRefreshState
    data class Done(val summary: LibraryUpdateSummary) : LibraryRefreshState
    data object Failed : LibraryRefreshState
}
