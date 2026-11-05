package org.kinera.app.ui.screens.reader

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatTextdirectionRToL
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.kinera.app.R
import org.kinera.app.core.util.UiState
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.ui.components.ErrorState
import org.kinera.app.ui.components.LoadingState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun ReaderScreen(
    mangaId: String,
    chapterId: String,
    startPage: Int = 0,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel(parameters = { parametersOf(mangaId, chapterId, startPage) }),
) {
    val manga by viewModel.manga.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val chapterIndex by viewModel.currentChapterIndex.collectAsState()
    val pagesState by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val webtoonZoom by viewModel.webtoonZoom.collectAsState()
    val readerRtl by viewModel.readerRtl.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val message by viewModel.message.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    // One-shot seek from the slider; the stamp makes the effect re-run for
    // repeated seeks to the same page.
    var seekPage by remember { mutableStateOf(0) }
    var seekStamp by remember { mutableStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.consumeMessage()
        }
    }

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
                    // Stable per chapter: recomputing the list on every
                    // recomposition would restart the scroll effects below
                    // mid-gesture and make flipping feel laggy.
                    val pageUrls = remember(pages) { pages.map { it.url } }
                    val toggleControls = remember { { controlsVisible = !controlsVisible } }
                    when (readerMode) {
                        ReaderMode.WEBTOON -> WebtoonContent(
                            pages = pageUrls,
                            startPage = remember(pageUrls) { currentPage },
                            seekPage = seekPage,
                            seekStamp = seekStamp,
                            zoomEnabled = webtoonZoom,
                            onTap = toggleControls,
                            onPageChanged = viewModel::onPageChanged,
                        )
                        ReaderMode.PAGED -> PagedContent(
                            pages = pageUrls,
                            startPage = remember(pageUrls) { currentPage },
                            seekPage = seekPage,
                            seekStamp = seekStamp,
                            rtl = readerRtl,
                            onTap = toggleControls,
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
                readerRtl = readerRtl,
                isBookmarked = isBookmarked,
                onSeek = { page ->
                    viewModel.onPageChanged(page)
                    seekPage = page
                    seekStamp = System.nanoTime()
                },
                onNextChapter = { viewModel.nextChapter() },
                onPrevChapter = { viewModel.previousChapter() },
                onToggleBookmark = viewModel::toggleBookmark,
                onToggleMode = {
                    viewModel.setReaderMode(
                        if (readerMode == ReaderMode.WEBTOON) ReaderMode.PAGED else ReaderMode.WEBTOON,
                    )
                },
                onToggleRtl = { viewModel.setReaderRtl(!readerRtl) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 148.dp),
        )
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

@Composable
private fun ReaderBottomBar(
    totalPages: Int,
    currentPage: Int,
    canGoNext: Boolean,
    canGoPrev: Boolean,
    readerMode: ReaderMode,
    readerRtl: Boolean,
    isBookmarked: Boolean,
    onSeek: (Int) -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleRtl: () -> Unit,
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
                    imageVector = if (readerMode == ReaderMode.WEBTOON) Icons.Rounded.MenuBook else Icons.Rounded.AutoStories,
                    contentDescription = stringResource(
                        if (readerMode == ReaderMode.WEBTOON) R.string.reader_mode_paged else R.string.reader_mode_webtoon,
                    ),
                    tint = Color.White,
                )
            }
            if (readerMode == ReaderMode.PAGED) {
                IconButton(onClick = onToggleRtl) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatTextdirectionRToL,
                        contentDescription = stringResource(R.string.reader_mode_rtl),
                        tint = if (readerRtl) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = stringResource(
                        if (isBookmarked) R.string.reader_bookmark_remove else R.string.reader_bookmark_add,
                    ),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
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

/**
 * Continuous vertical feed. Pinch zooms the whole strip, two-finger panning
 * looks around, double tap toggles between fit and zoomed-in. A single
 * finger still scrolls the list underneath.
 *
 * [startPage] is read once per chapter (the caller captures it with
 * remember), [seekPage]/[seekStamp] are one-shot jumps from the slider.
 * The live page index never flows back in, so the list is not forced to
 * recompose on every flip.
 */
@Composable
private fun WebtoonContent(
    pages: List<String>,
    startPage: Int,
    seekPage: Int,
    seekStamp: Long,
    zoomEnabled: Boolean,
    onTap: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(newScale: Float, candidate: Offset): Offset {
        val maxX = containerSize.width * (newScale - 1f) / 2f
        val maxY = containerSize.height * (newScale - 1f) / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        offset = if (newScale > 1f) {
            clampOffset(newScale, offset + panChange * newScale)
        } else {
            Offset.Zero
        }
        scale = newScale
    }

    LaunchedEffect(pages) {
        if (startPage > 0 && startPage < pages.size) listState.scrollToItem(startPage)
    }
    LaunchedEffect(seekStamp) {
        if (seekStamp != 0L && seekPage in pages.indices) {
            listState.scrollToItem(seekPage)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> onPageChanged(index) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .then(if (zoomEnabled) Modifier.transformable(transformState) else Modifier)
                .pointerInput(zoomEnabled) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = {
                            if (zoomEnabled) {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = DOUBLE_TAP_ZOOM
                                    offset = Offset.Zero
                                }
                            }
                        },
                    )
                },
        ) {
            itemsIndexed(
                pages,
                key = { index, _ -> index },
                contentType = { _, _ -> "page" },
            ) { _, url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Horizontal pager. With [rtl] the pages run right-to-left, the classic
 * manga direction: page 1 sits on the right and reading moves leftwards.
 * In that mode the tap zones mirror too — the left edge flips forward, the
 * right edge flips back, a tap in the middle toggles the controls.
 *
 * [startPage] is captured once per chapter; [seekPage]/[seekStamp] are
 * one-shot slider jumps. The live page index never flows back in, so
 * swiping does not trigger a competing scrollToPage.
 */
@Composable
private fun PagedContent(
    pages: List<String>,
    startPage: Int,
    seekPage: Int,
    seekStamp: Long,
    rtl: Boolean,
    onTap: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pages) {
        if (startPage > 0 && startPage < pages.size) pagerState.scrollToPage(startPage)
    }
    LaunchedEffect(seekStamp) {
        if (seekStamp != 0L && seekPage in pages.indices) {
            pagerState.scrollToPage(seekPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { index -> onPageChanged(index) }
    }

    fun flipTo(target: Int) {
        if (target in pages.indices) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        HorizontalPager(
            state = pagerState,
            // Keep one neighbor composed so the next page image starts
            // loading before the flip instead of appearing blank.
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            ZoomablePage(
                model = pages[index],
                pageIndex = index,
                rtl = rtl,
                onTap = onTap,
                onNext = { flipTo(pagerState.currentPage + 1) },
                onPrev = { flipTo(pagerState.currentPage - 1) },
            )
        }
    }
}

/**
 * One page of the paged reader: double tap zooms in and out, pinch and drag
 * pan the image while zoomed. When the page fits the screen the gestures are
 * left to the pager, so swiping between pages keeps working. Edge taps flip
 * pages like in most readers: in LTR the right third is "next", in RTL the
 * left third is; a tap in the middle toggles the controls.
 */
@Composable
private fun ZoomablePage(
    model: String,
    pageIndex: Int,
    rtl: Boolean,
    onTap: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val zoomed = scale > 1.01f

    fun clampOffset(newScale: Float, candidate: Offset): Offset {
        val maxX = size.width * (newScale - 1f) / 2f
        val maxY = size.height * (newScale - 1f) / 2f
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it },
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(zoomed) {
                    if (zoomed) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            offset = if (newScale > 1f) {
                                clampOffset(newScale, offset + pan * newScale)
                            } else {
                                Offset.Zero
                            }
                            scale = newScale
                        }
                    }
                }
                .pointerInput(pageIndex) {
                    detectTapGestures(
                        onTap = { tap ->
                            when {
                                tap.x < size.width / 3f -> if (rtl) onNext() else onPrev()
                                tap.x > size.width * 2f / 3f -> if (rtl) onPrev() else onNext()
                                else -> onTap()
                            }
                        },
                        onDoubleTap = {
                            if (zoomed) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_ZOOM
                                offset = Offset.Zero
                            }
                        },
                    )
                },
        )
    }
}
