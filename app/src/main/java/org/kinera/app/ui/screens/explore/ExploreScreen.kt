package org.kinera.app.ui.screens.explore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.util.UiState
import org.kinera.app.data.localsource.FolderSourceStorage
import org.kinera.app.data.mangalib.dto.MangaLibGenre
import org.kinera.app.ui.components.EmptyState
import org.kinera.app.ui.components.ErrorState
import org.kinera.app.ui.components.LoadingState
import org.kinera.app.ui.components.MangaCard
import org.koin.compose.viewmodel.koinViewModel

private val CBZ_MIME_TYPES = arrayOf(
    "application/zip",
    "application/vnd.comicbook+zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)

/** MangaLib statuses in catalog order; ids match the MangaLib API. */
private val MANGALIB_STATUSES = listOf(
    1 to R.string.explore_status_ongoing,
    2 to R.string.explore_status_finished,
    3 to R.string.explore_status_announced,
    4 to R.string.explore_status_paused,
    5 to R.string.explore_status_cancelled,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(
    onMangaClick: (Manga) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val localItems by viewModel.localManga.collectAsState()
    val state by viewModel.state.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val folderImport by viewModel.folderImport.collectAsState()
    val source by viewModel.source.collectAsState()
    val sources by viewModel.availableSources.collectAsState()
    val mangadexLanguages by viewModel.mangadexLanguages.collectAsState()
    val mangalibStatuses by viewModel.mangalibStatuses.collectAsState()
    val mangalibGenres by viewModel.mangalibGenres.collectAsState()
    val mangalibGenreOptions by viewModel.mangalibGenreOptions.collectAsState()

    var showGenreSheet by remember { mutableStateOf(false) }
    var filtersDirty by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var importMenuOpen by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importArchives(uris)
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.importFolder(uri)
    }

    val importText = importResult?.let { result ->
        when {
            result.imported > 0 && result.skipped > 0 ->
                context.getString(R.string.local_import_mixed, result.imported, result.skipped)
            result.imported > 0 ->
                context.getString(R.string.local_imported, result.imported)
            result.skipped > 0 ->
                context.getString(R.string.local_import_skipped, result.skipped)
            else ->
                context.getString(R.string.local_import_failed)
        }
    }
    LaunchedEffect(importText) {
        if (importText != null) {
            snackbarHostState.showSnackbar(importText)
            viewModel.consumeImportResult()
        }
    }

    val folderImportText = when (folderImport) {
        FolderSourceStorage.ImportResult.Added -> context.getString(R.string.local_folder_added)
        FolderSourceStorage.ImportResult.Exists -> context.getString(R.string.local_folder_exists)
        FolderSourceStorage.ImportResult.Failed -> context.getString(R.string.local_folder_failed)
        null -> null
    }
    LaunchedEffect(folderImportText) {
        if (folderImportText != null) {
            snackbarHostState.showSnackbar(folderImportText)
            viewModel.consumeFolderImport()
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.explore_title)) },
                subtitle = {
                    Text(
                        text = stringResource(R.string.explore_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (tab == ExploreTab.Local) {
                ImportFabMenu(
                    expanded = importMenuOpen,
                    onToggle = { importMenuOpen = !importMenuOpen },
                    onImportArchives = {
                        importMenuOpen = false
                        picker.launch(CBZ_MIME_TYPES)
                    },
                    onImportFolder = {
                        importMenuOpen = false
                        folderPicker.launch(null)
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (tab == ExploreTab.Local) {
                LocalFeed(
                    items = localItems,
                    onTabSwitch = viewModel::switchTab,
                    onMangaClick = onMangaClick,
                )
            } else {
                when (state) {
                    is UiState.Loading -> LoadingState()
                    is UiState.Error -> ErrorState(
                        message = stringResource(R.string.error_network),
                        onRetry = viewModel::reload,
                    )
                    is UiState.Success -> ExploreFeed(
                        items = items,
                        tab = tab,
                        hasNext = viewModel.hasNext,
                        sources = sources,
                        source = source,
                        mangadexLanguages = mangadexLanguages,
                        mangalibStatuses = mangalibStatuses,
                        mangalibGenres = mangalibGenres,
                        onSourceSelect = viewModel::selectSource,
                        onToggleLanguage = viewModel::toggleMangadexLanguage,
                        onToggleMangalibStatus = viewModel::toggleMangalibStatus,
                        onOpenGenres = {
                            viewModel.loadMangalibGenresIfNeeded()
                            showGenreSheet = true
                        },
                        onTabSwitch = viewModel::switchTab,
                        onLoadMore = viewModel::loadMore,
                        onMangaClick = onMangaClick,
                    )
                }
            }
        }
    }

    if (showGenreSheet) {
        MangaLibGenreSheet(
            genres = mangalibGenreOptions,
            selected = mangalibGenres,
            onToggle = {
                filtersDirty = true
                viewModel.toggleMangalibGenre(it)
            },
            onReset = {
                filtersDirty = false
                viewModel.clearMangalibFilters()
            },
            onDismiss = {
                showGenreSheet = false
                if (filtersDirty && source == MangaSource.MANGALIB) {
                    viewModel.reload()
                }
            },
        )
    }
}

@Composable
private fun LocalFeed(
    items: List<Manga>,
    onTabSwitch: (ExploreTab) -> Unit,
    onMangaClick: (Manga) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TabSelector(
            tab = ExploreTab.Local,
            onTabSwitch = onTabSwitch,
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                EmptyState(
                    title = stringResource(R.string.local_empty),
                    hint = stringResource(R.string.local_empty_hint),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.id }) { manga ->
                    MangaCard(manga = manga, onClick = { onMangaClick(manga) })
                }
            }
        }
    }
}

@Composable
private fun ExploreFeed(
    items: List<Manga>,
    tab: ExploreTab,
    hasNext: Boolean,
    sources: List<MangaSource>,
    source: MangaSource,
    mangadexLanguages: Set<String>,
    mangalibStatuses: Set<Int>,
    mangalibGenres: Set<Int>,
    onSourceSelect: (MangaSource) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onToggleMangalibStatus: (Int) -> Unit,
    onOpenGenres: () -> Unit,
    onTabSwitch: (ExploreTab) -> Unit,
    onLoadMore: () -> Unit,
    onMangaClick: (Manga) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasNext && last >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TabSelector(tab = tab, onTabSwitch = onTabSwitch)
        }
        if (sources.size > 1) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SourceChips(
                    sources = sources,
                    selected = source,
                    onSelect = onSourceSelect,
                )
            }
        }
        if (source == MangaSource.MANGADEX) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LanguageChips(
                    languages = mangadexLanguages,
                    onToggle = onToggleLanguage,
                )
            }
        }
        if (source == MangaSource.MANGALIB) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MangaLibFilters(
                    selectedStatuses = mangalibStatuses,
                    selectedGenres = mangalibGenres,
                    onToggleStatus = onToggleMangalibStatus,
                    onOpenGenres = onOpenGenres,
                )
            }
        }
        items(items, key = { it.id }) { manga ->
            MangaCard(manga = manga, onClick = { onMangaClick(manga) })
        }
        if (hasNext) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    WavyFooter()
                }
            }
        }
    }
}

@Composable
private fun TabSelector(
    tab: ExploreTab,
    onTabSwitch: (ExploreTab) -> Unit,
) {
    val tabs = listOf(ExploreTab.Popular, ExploreTab.Latest, ExploreTab.Local)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        tabs.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = tab == entry,
                onClick = { onTabSwitch(entry) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = {
                    Text(
                        when (entry) {
                            ExploreTab.Popular -> stringResource(R.string.explore_popular)
                            ExploreTab.Latest -> stringResource(R.string.explore_latest)
                            ExploreTab.Local -> stringResource(R.string.explore_local)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SourceChips(
    sources: List<MangaSource>,
    selected: MangaSource,
    onSelect: (MangaSource) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(sources.size) { index ->
            val source = sources[index]
            FilterChip(
                selected = source == selected,
                onClick = { onSelect(source) },
                label = { Text(source.title) },
            )
        }
    }
}

/**
 * MangaDex chapter languages. Both chips off means no language filter —
 * every translation counts. The choice persists in Settings.
 */
@Composable
private fun LanguageChips(
    languages: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        FilterChip(
            selected = "en" in languages,
            onClick = { onToggle("en") },
            label = { Text(stringResource(R.string.language_english)) },
        )
        FilterChip(
            selected = "ru" in languages,
            onClick = { onToggle("ru") },
            label = { Text(stringResource(R.string.language_russian)) },
        )
    }
}

/**
 * MangaLib catalog filters: status chips plus a genres chip that opens the
 * bottom sheet. A status chip off means that status is filtered out; no
 * chip on means the whole catalog.
 */
@Composable
private fun MangaLibFilters(
    selectedStatuses: Set<Int>,
    selectedGenres: Set<Int>,
    onToggleStatus: (Int) -> Unit,
    onOpenGenres: () -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Icon(
                Icons.Rounded.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        items(MANGALIB_STATUSES.size) { index ->
            val (statusId, label) = MANGALIB_STATUSES[index]
            FilterChip(
                selected = statusId in selectedStatuses,
                onClick = { onToggleStatus(statusId) },
                label = { Text(stringResource(label)) },
            )
        }
        item {
            FilterChip(
                selected = selectedGenres.isNotEmpty(),
                onClick = onOpenGenres,
                label = {
                    Text(
                        if (selectedGenres.isEmpty()) {
                            stringResource(R.string.explore_filter_genres)
                        } else {
                            stringResource(R.string.explore_filter_genres_n, selectedGenres.size)
                        },
                    )
                },
            )
        }
    }
}

/**
 * MangaLib genre picker. Chips apply instantly; the caller reloads the feed
 * once on dismiss so a burst of taps does not fire a request per tap.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MangaLibGenreSheet(
    genres: List<MangaLibGenre>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.explore_filter_genres),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.explore_filters_reset))
                }
            }
            FlowRow(
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                genres.forEach { genre ->
                    FilterChip(
                        selected = genre.id in selected,
                        onClick = { onToggle(genre.id) },
                        label = { Text(genre.name.orEmpty()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onImportArchives: () -> Unit,
    onImportFolder: () -> Unit,
) {
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (expanded) 45f else 0f),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = onImportArchives,
            icon = { Icon(Icons.Rounded.FolderZip, contentDescription = null) },
            text = { Text(stringResource(R.string.local_import)) },
        )
        FloatingActionButtonMenuItem(
            onClick = onImportFolder,
            icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null) },
            text = { Text(stringResource(R.string.local_import_folder)) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WavyFooter() {
    LoadingIndicator()
}
