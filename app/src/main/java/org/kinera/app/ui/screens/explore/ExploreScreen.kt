package org.kinera.app.ui.screens.explore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import org.kinera.app.core.util.UiState
import org.kinera.app.data.localsource.FolderSourceStorage
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
                        onTabSwitch = viewModel::switchTab,
                        onLoadMore = viewModel::loadMore,
                        onMangaClick = onMangaClick,
                    )
                }
            }
        }
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
