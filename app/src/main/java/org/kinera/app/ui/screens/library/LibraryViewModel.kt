package org.kinera.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.core.model.Manga
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.repository.FavouriteManga
import org.kinera.app.data.repository.LibraryRepository

class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {

    val favourites = library.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories = library.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeFavorite(manga: Manga) {
        viewModelScope.launch { library.toggleFavorite(manga) }
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
}
