package org.kinera.app.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.local.entity.DownloadState
import org.kinera.app.ui.components.CoverImage
import org.kinera.app.ui.components.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var pendingDeleteManga by remember { mutableStateOf<DownloadEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = stringResource(R.string.downloads_empty),
                    hint = stringResource(R.string.downloads_empty_hint),
                )
            }
        } else {
            val groups = remember(items) { items.groupBy { it.mangaId } }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                groups.forEach { (_, chapters) ->
                    val head = chapters.first()
                    item(key = "header-${head.mangaId}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverImage(
                                url = head.coverUrl,
                                contentDescription = head.title,
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = head.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { pendingDeleteManga = head }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.downloads_delete_all),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    items(chapters, key = { it.chapterId }) { download ->
                        DownloadRow(
                            download = download,
                            onCancel = { viewModel.cancel(download.chapterId) },
                            onRetry = { viewModel.retry(download.chapterId) },
                            onDelete = { viewModel.delete(download.chapterId) },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteManga?.let { head ->
        AlertDialog(
            onDismissRequest = { pendingDeleteManga = null },
            title = { Text(stringResource(R.string.downloads_confirm_delete_title)) },
            text = { Text(stringResource(R.string.downloads_confirm_delete_text, head.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteManga(head.mangaId)
                    pendingDeleteManga = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteManga = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadRow(
    download: DownloadEntity,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = download.chapterLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stateLabel(download),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (download.state == DownloadState.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (download.state) {
                DownloadState.QUEUED, DownloadState.RUNNING ->
                    RowActionButton(
                        onClick = onCancel,
                        contentDescription = stringResource(R.string.downloads_action_cancel),
                        icon = Icons.Rounded.Close,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                DownloadState.DONE ->
                    RowActionButton(
                        onClick = onDelete,
                        contentDescription = stringResource(R.string.downloads_action_delete),
                        icon = Icons.Rounded.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                else ->
                    RowActionButton(
                        onClick = onRetry,
                        contentDescription = stringResource(R.string.downloads_action_retry),
                        icon = Icons.Rounded.ErrorOutline,
                        tint = MaterialTheme.colorScheme.error,
                    )
            }
        }
        if (download.state == DownloadState.RUNNING && download.total > 0) {
            LinearWavyProgressIndicator(
                progress = { download.progress.toFloat() / download.total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
        if (download.state == DownloadState.FAILED && !download.error.isNullOrBlank()) {
            Text(
                text = download.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun RowActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun stateLabel(download: DownloadEntity): String = when (download.state) {
    DownloadState.QUEUED -> stringResource(R.string.downloads_state_queued)
    DownloadState.RUNNING -> stringResource(
        R.string.downloads_state_running,
        download.progress,
        download.total,
    )
    DownloadState.DONE -> stringResource(R.string.downloads_state_done)
    else -> stringResource(R.string.downloads_state_failed)
}
