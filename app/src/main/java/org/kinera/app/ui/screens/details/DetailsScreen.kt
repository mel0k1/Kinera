package org.kinera.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.util.UiState
import org.kinera.app.data.local.entity.BookmarkEntity
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.local.entity.DownloadState
import org.kinera.app.ui.components.CoverImage
import org.kinera.app.ui.components.ErrorState
import org.kinera.app.ui.components.LoadingState
import org.kinera.app.ui.components.pressScale
import org.kinera.app.ui.components.rememberPressInteraction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mangaId: String,
    onBack: () -> Unit,
    onReadChapter: (Manga, MangaChapter, Int) -> Unit,
    viewModel: DetailsViewModel = koinViewModel(parameters = { parametersOf(mangaId) }),
) {
    val mangaState by viewModel.manga.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val message by viewModel.message.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(onClick = onBack)
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        when (val state = mangaState) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))
            is UiState.Error -> ErrorState(
                message = stringResource(R.string.error_network),
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                val manga = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item { MangaHeader(manga, isFavorite, viewModel::toggleFavorite) }
                    item {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (!manga.description.isNullOrBlank()) {
                                DescriptionText(manga.description!!, expanded) { expanded = !expanded }
                            }
                            if (manga.tags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    manga.tags.take(4).forEach { tag ->
                                        AssistChip(onClick = {}, label = { Text(tag.title) })
                                    }
                                }
                            }
                            if (bookmarks.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.details_bookmarks),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                bookmarks.forEach { bookmark ->
                                    BookmarkRow(
                                        bookmark = bookmark,
                                        onOpen = {
                                            chapters.firstOrNull { it.id == bookmark.chapterId }
                                                ?.let { chapter -> onReadChapter(manga, chapter, bookmark.page) }
                                        },
                                        onRemove = { viewModel.deleteBookmark(bookmark) },
                                    )
                                }
                            }
                            val continueChapter = continueChapter(chapters, progress?.chapterId)
                            Button(
                                onClick = {
                                    val chapter = continueChapter ?: chapters.firstOrNull()
                                    if (chapter != null) onReadChapter(manga, chapter, 0)
                                },
                                enabled = chapters.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (continueChapter != null) {
                                        stringResource(R.string.details_continue, continueChapter.number ?: "1")
                                    } else {
                                        stringResource(R.string.details_start_reading)
                                    },
                                )
                            }
                            val chaptersHeader: @Composable () -> Unit = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.details_chapters),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (manga.source != org.kinera.app.core.model.MangaSource.LOCAL) {
                                        IconButton(onClick = viewModel::downloadAll) {
                                            Icon(
                                                Icons.Rounded.Download,
                                                contentDescription = stringResource(R.string.details_download_all),
                                            )
                                        }
                                    }
                                }
                            }
                            chaptersHeader()
                        }
                    }
                    if (chapters.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.details_no_chapters),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        itemsIndexed(chapters.asReversed(), key = { _, it -> it.id }) { _, chapter ->
                            val isRead = isChapterRead(chapter, progress?.chapterId, chapters)
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                ChapterRow(
                                    chapter = chapter,
                                    isCurrent = chapter.id == progress?.chapterId,
                                    isRead = isRead,
                                    download = if (manga.source == org.kinera.app.core.model.MangaSource.LOCAL) null else downloadStates[chapter.id],
                                    onClick = { onReadChapter(manga, chapter, 0) },
                                    onDownload = { viewModel.downloadChapter(chapter) },
                                    onCancel = { viewModel.cancelDownload(chapter.id) },
                                    onDelete = { confirmDelete = chapter.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { chapterId ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.downloads_confirm_delete_title)) },
            text = { Text(stringResource(R.string.downloads_confirm_delete_chapter)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDownload(chapterId)
                    confirmDelete = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MangaHeader(
    manga: Manga,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Box {
        // Cover backdrop bleeding into the background color
        CoverImage(
            url = manga.coverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(300.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.3f), MaterialTheme.colorScheme.background),
                        startY = 40f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CoverImage(
                    url = manga.coverUrl,
                    contentDescription = manga.title,
                    modifier = Modifier
                        .width(118.dp)
                        .height(168.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
                Column(
                    Modifier.weight(1f).padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = manga.title,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    manga.author?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = stateLabel(manga.state),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconToggleButton(
                    checked = isFavorite,
                    onCheckedChange = { onToggleFavorite() },
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.details_in_library else R.string.details_add_favorite,
                        ),
                    )
                }
                Text(
                    text = if (isFavorite) {
                        stringResource(R.string.details_in_library)
                    } else {
                        stringResource(R.string.details_add_favorite)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: BookmarkEntity,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Bookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = bookmark.chapterLabel,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.details_bookmark_page,
                    bookmark.page + 1,
                ) + " · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(bookmark.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.reader_bookmark_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DescriptionText(description: String, expanded: Boolean, onToggle: () -> Unit) {
    var overflowed by remember { mutableStateOf(false) }
    Column {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) overflowed = result.hasVisualOverflow },
        )
        if (overflowed || expanded) {
            Text(
                text = stringResource(
                    if (expanded) R.string.details_description_collapse else R.string.details_description_expand,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(onClick = onToggle),
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: MangaChapter,
    isCurrent: Boolean,
    isRead: Boolean,
    download: DownloadEntity?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = rememberPressInteraction()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrent) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isCurrent) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction),
    ) {
        Row(
            modifier = Modifier
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = chapter.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(chapter.uploadedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (isRead) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DownloadAction(
            download = download,
            onDownload = onDownload,
            onCancel = onCancel,
            onDelete = onDelete,
        )
        }
    }
}

@Composable
private fun DownloadAction(
    download: DownloadEntity?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    when (download?.state) {
        null -> IconButton(onClick = onDownload) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = stringResource(R.string.details_download_chapter),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DownloadState.QUEUED, DownloadState.RUNNING -> IconButton(onClick = onCancel) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.downloads_action_cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DownloadState.DONE -> IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.details_downloaded),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        else -> IconButton(onClick = onDownload) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = stringResource(R.string.downloads_action_retry),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun stateLabel(state: MangaState): String = when (state) {
    MangaState.ONGOING -> stringResource(R.string.status_ongoing)
    MangaState.FINISHED -> stringResource(R.string.status_finished)
    MangaState.PAUSED -> stringResource(R.string.status_paused)
    MangaState.CANCELLED -> stringResource(R.string.status_cancelled)
    MangaState.UNKNOWN -> stringResource(R.string.status_unknown)
}

private fun continueChapter(
    chapters: List<MangaChapter>,
    lastReadChapterId: String?,
): MangaChapter? {
    if (lastReadChapterId == null) return null
    val index = chapters.indexOfFirst { it.id == lastReadChapterId }
    if (index == -1) return null
    return chapters.getOrNull(index + 1) ?: chapters.lastOrNull()
}

private fun isChapterRead(
    chapter: MangaChapter,
    lastReadChapterId: String?,
    chapters: List<MangaChapter>,
): Boolean {
    if (lastReadChapterId == null) return false
    val readIndex = chapters.indexOfFirst { it.id == lastReadChapterId }
    val chapterIndex = chapters.indexOfFirst { it.id == chapter.id }
    return chapterIndex in 0 until readIndex
}
