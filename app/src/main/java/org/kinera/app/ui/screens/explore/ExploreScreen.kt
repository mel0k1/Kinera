package org.kinera.app.ui.screens.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.core.util.UiState
import org.kinera.app.ui.components.ErrorState
import org.kinera.app.ui.components.LoadingState
import org.kinera.app.ui.components.MangaCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onMangaClick: (Manga) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ExploreViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val state by viewModel.state.collectAsState()
    val tab by viewModel.tab.collectAsState()

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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            ) {
                SegmentedButton(
                    selected = tab == ExploreTab.Popular,
                    onClick = { onTabSwitch(ExploreTab.Popular) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.explore_popular)) }
                SegmentedButton(
                    selected = tab == ExploreTab.Latest,
                    onClick = { onTabSwitch(ExploreTab.Latest) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.explore_latest)) }
            }
        }
        items(items, key = { it.id }) { manga ->
            MangaCard(manga = manga, onClick = { onMangaClick(manga) })
        }
        if (hasNext) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    WavyFooter()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WavyFooter() {
    LoadingIndicator()
}
