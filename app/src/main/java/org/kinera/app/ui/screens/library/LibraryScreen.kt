package org.kinera.app.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.repository.FavouriteManga
import org.kinera.app.ui.components.EmptyState
import org.kinera.app.ui.components.MangaCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    onMangaClick: (Manga) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val favourites by viewModel.favourites.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val refresh by viewModel.refresh.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }

    val visible = when (selectedCategory) {
        null -> favourites
        else -> favourites.filter { it.categoryId == selectedCategory }
    }

    LaunchedEffect(refresh) {
        when (val state = refresh) {
            is LibraryRefreshState.Done -> {
                snackbarHostState.showSnackbar(
                    if (state.summary.withNewChapters > 0) {
                        context.getString(R.string.library_updated_new, state.summary.withNewChapters)
                    } else {
                        context.getString(R.string.library_updated_none)
                    },
                )
                viewModel.consumeRefreshState()
            }
            is LibraryRefreshState.Failed -> {
                snackbarHostState.showSnackbar(context.getString(R.string.library_update_failed))
                viewModel.consumeRefreshState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(data) } },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                subtitle = {
                    Text(
                        text = when {
                            visible.isEmpty() -> stringResource(R.string.library_subtitle_empty)
                            else -> stringResource(R.string.library_subtitle_count, visible.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshLibrary,
                        enabled = refresh !is LibraryRefreshState.Running,
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.library_check_updates),
                        )
                    }
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
            CategoryChipsRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                onMove = viewModel::moveCategory,
            )
            if (visible.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.library_empty),
                    hint = stringResource(R.string.library_empty_hint),
                )
            } else {
                LibraryGrid(
                    items = visible,
                    categories = categories,
                    onMangaClick = onMangaClick,
                    onRemove = viewModel::removeFavorite,
                    onAssignCategory = viewModel::assignCategory,
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

private const val ALL_CHIP_KEY = "all"

/**
 * Category chips with drag and drop reordering: long press starts a drag,
 * the chip follows the finger and swaps with the neighbor it crosses; the
 * final order is persisted in the database.
 */
@Composable
private fun CategoryChipsRow(
    categories: List<CategoryEntity>,
    selectedCategory: Long?,
    onSelectCategory: (Long?) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = ALL_CHIP_KEY) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text(stringResource(R.string.library_category_all)) },
                shape = MaterialTheme.shapes.small,
            )
        }
        itemsIndexed(categories, key = { _, category -> category.id }) { _, category ->
            val dragging = draggingId == category.id
            FilterChip(
                selected = selectedCategory == category.id,
                onClick = { onSelectCategory(category.id) },
                label = { Text(category.name) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .then(if (dragging) Modifier else Modifier.animateItem())
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationX = if (dragging) dragOffset else 0f }
                    .pointerInput(category.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = category.id
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.x
                                val info = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == category.id }
                                    ?: return@detectDragGesturesAfterLongPress
                                val center = info.offset + info.size / 2f + dragOffset
                                val neighbor = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { other ->
                                        other.key is Long && other.key != category.id &&
                                            (if (amount.x > 0) {
                                                other.offset > info.offset &&
                                                    center > other.offset + other.size / 2f
                                            } else {
                                                other.offset < info.offset &&
                                                    center < other.offset + other.size / 2f
                                            })
                                    }
                                if (neighbor != null) {
                                    val from = categories.indexOfFirst { it.id == category.id }
                                    val to = categories.indexOfFirst { it.id == neighbor.key }
                                    if (from >= 0 && to >= 0 && from != to) {
                                        onMove(from, to)
                                        // Anchor the chip to the same visual spot after the swap.
                                        dragOffset += info.offset - neighbor.offset
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                            },
                        )
                    },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    items: List<FavouriteManga>,
    categories: List<CategoryEntity>,
    onMangaClick: (Manga) -> Unit,
    onRemove: (Manga) -> Unit,
    onAssignCategory: (Manga, Long?) -> Unit,
) {
    var menuTarget by remember { mutableStateOf<FavouriteManga?>(null) }
    LazyVerticalGrid(
        state = rememberLazyGridState(),
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.manga.id }) { item ->
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = { onMangaClick(item.manga) },
                    onLongClick = { menuTarget = item },
                ),
            ) {
                MangaCard(
                    manga = item.manga,
                    onClick = { onMangaClick(item.manga) },
                    badge = item.newChapters,
                )
            }
        }
    }
    menuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text(target.manga.title) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.library_move_to_category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            onAssignCategory(target.manga, null)
                            menuTarget = null
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.library_category_none),
                            color = if (target.categoryId == null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    categories.forEach { category ->
                        TextButton(
                            onClick = {
                                onAssignCategory(target.manga, category.id)
                                menuTarget = null
                            },
                        ) {
                            Text(
                                text = category.name,
                                color = if (target.categoryId == category.id) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            },
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
