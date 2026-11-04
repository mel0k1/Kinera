package org.kinera.app.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.data.repository.FavouriteManga
import org.kinera.app.ui.components.EmptyState
import org.kinera.app.ui.components.MangaCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (Manga) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val favourites by viewModel.favourites.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }

    val visible = when (selectedCategory) {
        null -> favourites
        else -> favourites.filter { it.categoryId == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
                    }
                    IconButton(onClick = { showAddCategory = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.library_new_category))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.library_category_all)) },
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category.id,
                        onClick = { selectedCategory = category.id },
                        label = { Text(category.name) },
                    )
                }
            }
            if (visible.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.library_empty),
                    hint = stringResource(R.string.library_empty_hint),
                )
            } else {
                LibraryGrid(
                    items = visible,
                    onMangaClick = onMangaClick,
                    onRemove = viewModel::removeFavorite,
                )
            }
        }
    }

    if (showAddCategory) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text(stringResource(R.string.library_new_category)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.library_category_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCategory(name)
                        showAddCategory = false
                    },
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    items: List<FavouriteManga>,
    onMangaClick: (Manga) -> Unit,
    onRemove: (Manga) -> Unit,
) {
    var menuTarget by remember { mutableStateOf<FavouriteManga?>(null) }
    LazyVerticalGrid(
        state = rememberLazyGridState(),
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.manga.id }) { item ->
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = { onMangaClick(item.manga) },
                    onLongClick = { menuTarget = item },
                ),
            ) {
                MangaCard(manga = item.manga, onClick = { onMangaClick(item.manga) })
            }
        }
    }
    menuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text(target.manga.title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(target.manga)
                        menuTarget = null
                    },
                ) { Text(stringResource(R.string.library_remove_favorite)) }
            },
            dismissButton = {
                TextButton(onClick = { menuTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
