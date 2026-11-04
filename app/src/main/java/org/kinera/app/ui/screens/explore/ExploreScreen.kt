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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.core.util.UiState
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importArchives(uris)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.explore_title)) },
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
                ExtendedFloatingActionButton(
                    onClick = { picker.launch(CBZ_MIME_TYPES) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.local_import)) },
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
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
private fun WavyFooter() {
    LoadingIndicator()
}
