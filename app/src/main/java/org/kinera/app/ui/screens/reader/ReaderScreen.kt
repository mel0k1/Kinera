package org.kinera.app.ui.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.view.WindowManager
import coil.compose.AsyncImage
import org.kinera.app.R
import org.kinera.app.core.util.UiState
import org.kinera.app.ui.components.ErrorState
import org.kinera.app.ui.components.LoadingState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    mangaId: String,
    chapterId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel(parameters = { parametersOf(mangaId, chapterId) }),
) {
    val manga by viewModel.manga.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val chapterIndex by viewModel.currentChapterIndex.collectAsState()
    val pagesState by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    val activity = LocalContext.current as? Activity
    DisposableEffect(keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn && window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val state = pagesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicatorOnDark()
            }
            is UiState.Error -> ErrorState(
                message = state.message ?: stringResource(R.string.error_generic),
                onRetry = viewModel::retryPages,
            )
            is UiState.Success -> {
                val pages = state.data
                if (pages.isEmpty()) {
                    ErrorState(
                        message = stringResource(R.string.error_generic),
                        onRetry = viewModel::retryPages,
                    )
                } else {
                    when (readerMode) {
                        org.kinera.app.data.settings.ReaderMode.WEBTOON -> WebtoonContent(
                            pages = pages.map { it.url },
                            currentPage = currentPage,
                            onTap = { controlsVisible = !controlsVisible },
                            onPageChanged = viewModel::onPageChanged,
                        )
                        org.kinera.app.data.settings.ReaderMode.PAGED -> PagedContent(
                            pages = pages.map { it.url },
                            currentPage = currentPage,
                            onTap = { controlsVisible = !controlsVisible },
                            onPageChanged = viewModel::onPageChanged,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                title = manga?.title.orEmpty(),
                chapterLabel = chapters.getOrNull(chapterIndex)?.label.orEmpty(),
                onBack = onBack,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(
                totalPages = (pagesState as? UiState.Success)?.data?.size ?: 0,
                currentPage = currentPage,
                canGoNext = chapterIndex + 1 < chapters.size,
                canGoPrev = chapterIndex > 0,
                readerMode = readerMode,
                onSeek = { viewModel.onPageChanged(it) },
                onNextChapter = { viewModel.nextChapter() },
                onPrevChapter = { viewModel.previousChapter() },
                onToggleMode = {
                    viewModel.setReaderMode(
                        if (readerMode == org.kinera.app.data.settings.ReaderMode.WEBTOON) {
                            org.kinera.app.data.settings.ReaderMode.PAGED
                        } else {
                            org.kinera.app.data.settings.ReaderMode.WEBTOON
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun LoadingIndicatorOnDark() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WavyLoader()
        Text(
            text = stringResource(R.string.reader_loading_chapter),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WavyLoader() {
    LinearWavyProgressIndicator(color = Color.White)
}

@Composable
private fun ReaderTopBar(
    title: String,
    chapterLabel: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.65f))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                text = chapterLabel,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderBottomBar(
    totalPages: Int,
    currentPage: Int,
    canGoNext: Boolean,
    canGoPrev: Boolean,
    readerMode: org.kinera.app.data.settings.ReaderMode,
    onSeek: (Int) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onToggleMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.65f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevChapter, enabled = canGoPrev) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.reader_previous_chapter), tint = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.4f))
            }
            Text(
                text = stringResource(
                    R.string.reader_page_of,
                    (currentPage + 1).coerceAtLeast(1),
                    totalPages.coerceAtLeast(1),
                ),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onNextChapter, enabled = canGoNext) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.reader_next_chapter), tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.4f))
            }
            IconButton(onClick = onToggleMode) {
                Icon(
                    imageVector = if (readerMode == org.kinera.app.data.settings.ReaderMode.WEBTOON) {
                        Icons.Rounded.MenuBook
                    } else {
                        Icons.Rounded.AutoStories
                    },
                    contentDescription = stringResource(
                        if (readerMode == org.kinera.app.data.settings.ReaderMode.WEBTOON) {
                            R.string.reader_mode_paged
                        } else {
                            R.string.reader_mode_webtoon
                        },
                    ),
                    tint = Color.White,
                )
            }
        }
        if (totalPages > 1) {
            Slider(
                value = currentPage.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..(totalPages - 1).toFloat(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun WebtoonContent(
    pages: List<String>,
    currentPage: Int,
    onTap: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(pages) {
        if (currentPage > 0) listState.scrollToItem(currentPage)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> onPageChanged(index) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() },
    ) {
        itemsIndexed(pages) { _, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PagedContent(
    pages: List<String>,
    currentPage: Int,
    onTap: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pages) {
        if (currentPage > 0) pagerState.scrollToPage(currentPage)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { index -> onPageChanged(index) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() },
    ) { index ->
        AsyncImage(
            model = pages[index],
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
