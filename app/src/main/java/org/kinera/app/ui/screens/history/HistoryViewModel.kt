package org.kinera.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.data.local.entity.HistoryEntity
import org.kinera.app.data.repository.LibraryRepository

class HistoryViewModel(private val library: LibraryRepository) : ViewModel() {

    val history = library.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { library.clearHistory() }
    }

    fun remove(manga: HistoryEntity) {
        viewModelScope.launch { library.removeHistory(manga.mangaId) }
    }
}
