package org.kinera.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.repository.DownloadRepository

class DownloadsViewModel(private val downloads: DownloadRepository) : ViewModel() {

    val items: StateFlow<List<DownloadEntity>> = downloads.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(chapterId: String) = viewModelScope.launch { downloads.cancel(chapterId) }

    fun retry(chapterId: String) = viewModelScope.launch { downloads.retry(chapterId) }

    fun delete(chapterId: String) = viewModelScope.launch { downloads.deleteDownloaded(chapterId) }

    fun deleteManga(mangaId: String) = viewModelScope.launch { downloads.deleteManga(mangaId) }
}
