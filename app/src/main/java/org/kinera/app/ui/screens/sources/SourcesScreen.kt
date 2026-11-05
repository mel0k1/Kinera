package org.kinera.app.ui.screens.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.core.util.UiState
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.suwayomi.SuwayomiSourceOption
import org.koin.compose.viewmodel.koinViewModel

/** MangaLib statuses in catalog order; ids match the MangaLib API. */
private val MANGALIB_STATUSES = listOf(
    1 to R.string.explore_status_ongoing,
    2 to R.string.explore_status_finished,
    3 to R.string.explore_status_announced,
    4 to R.string.explore_status_paused,
    5 to R.string.explore_status_cancelled,
)

/** Remanga statuses in catalog order; ids match the Remanga API. */
private val REMANGA_STATUSES = listOf(
    2 to R.string.explore_status_ongoing,
    1 to R.string.explore_status_finished,
    3 to R.string.explore_status_paused,
    4 to R.string.explore_status_cancelled,
)

/**
 * Every source setting in one place: chapter languages of MangaDex, the
 * genre / status catalog filters of MangaLib, Remanga and Desu.me, plus
 * the self-hosted Komga and Suwayomi servers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    viewModel: SourcesViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val suwayomiSources by viewModel.suwayomiSources.collectAsState()
    val mangalibGenres by viewModel.mangalibGenreOptions.collectAsState()
    val remangaGenres by viewModel.remangaGenreOptions.collectAsState()
    val desumeGenres by viewModel.desumeGenreOptions.collectAsState()
    var genreSheet by remember { mutableStateOf<GenreSheetSource?>(null) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.sources_title)) },
                subtitle = {
                    Text(
                        text = stringResource(R.string.sources_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                SourceCard(title = "MangaDex") {
                    LanguageChips(
                        languages = settings.mangadexLanguages,
                        onToggle = viewModel::setMangadexLanguage,
                    )
                    Text(
                        text = stringResource(R.string.settings_mangadex_languages_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SourceCard(
                    title = "MangaLib",
                    onReset = if (
                        settings.mangalibStatuses.isNotEmpty() || settings.mangalibGenres.isNotEmpty()
                    ) {
                        { viewModel.clearMangalibFilters() }
                    } else {
                        null
                    },
                ) {
                    StatusChips(
                        statuses = MANGALIB_STATUSES,
                        selected = settings.mangalibStatuses,
                        onToggle = viewModel::toggleMangalibStatus,
                    )
                    GenresChip(
                        selectedCount = settings.mangalibGenres.size,
                        onClick = {
                            viewModel.loadGenresIfNeeded(GenreSheetSource.MANGALIB)
                            genreSheet = GenreSheetSource.MANGALIB
                        },
                    )
                }
            }
            item {
                SourceCard(
                    title = "Remanga",
                    onReset = if (
                        settings.remangaStatuses.isNotEmpty() || settings.remangaGenres.isNotEmpty()
                    ) {
                        { viewModel.clearRemangaFilters() }
                    } else {
                        null
                    },
                ) {
                    StatusChips(
                        statuses = REMANGA_STATUSES,
                        selected = settings.remangaStatuses,
                        onToggle = viewModel::toggleRemangaStatus,
                    )
                    GenresChip(
                        selectedCount = settings.remangaGenres.size,
                        onClick = {
                            viewModel.loadGenresIfNeeded(GenreSheetSource.REMANGA)
                            genreSheet = GenreSheetSource.REMANGA
                        },
                    )
                }
            }
            item {
                SourceCard(
                    title = "Desu.me",
                    onReset = if (settings.desumeGenres.isNotEmpty()) {
                        { viewModel.clearDesumeFilters() }
                    } else {
                        null
                    },
                ) {
                    GenresChip(
                        selectedCount = settings.desumeGenres.size,
                        onClick = {
                            viewModel.loadGenresIfNeeded(GenreSheetSource.DESUME)
                            genreSheet = GenreSheetSource.DESUME
                        },
                    )
                }
            }

            item {
                SectionLabel(stringResource(R.string.settings_servers))
            }
            item {
                ServerCard(
                    title = stringResource(R.string.settings_server_komga),
                    hint = stringResource(R.string.settings_server_komga_hint),
                    connection = settings.komga,
                    onSave = { viewModel.saveServer(ServerId.KOMGA, it) },
                )
            }
            item {
                ServerCard(
                    title = stringResource(R.string.settings_server_suwayomi),
                    hint = stringResource(R.string.settings_server_suwayomi_hint),
                    connection = settings.suwayomi,
                    onSave = { viewModel.saveServer(ServerId.SUWAYOMI, it) },
                ) {
                    if (settings.suwayomi.isConfigured) {
                        SuwayomiSourcePicker(
                            state = suwayomiSources,
                            selectedId = settings.suwayomiSourceId,
                            onSelect = viewModel::selectSuwayomiSource,
                            onRefresh = { viewModel.loadSuwayomiSources(force = true) },
                        )
                    }
                }
            }
        }
    }

    genreSheet?.let { sheet ->
        val options: List<Pair<String, String>> = when (sheet) {
            GenreSheetSource.MANGALIB -> mangalibGenres.map { it.id.toString() to it.name.orEmpty() }
            GenreSheetSource.REMANGA -> remangaGenres.map { it.id.toString() to it.name }
            GenreSheetSource.DESUME -> desumeGenres.map { it.slug to it.name }
        }
        val selected: Set<String> = when (sheet) {
            GenreSheetSource.MANGALIB -> settings.mangalibGenres.map { it.toString() }.toSet()
            GenreSheetSource.REMANGA -> settings.remangaGenres.map { it.toString() }.toSet()
            GenreSheetSource.DESUME -> settings.desumeGenres
        }
        GenreSheet(
            options = options,
            selected = selected,
            onToggle = { key ->
                when (sheet) {
                    GenreSheetSource.MANGALIB -> key.toIntOrNull()?.let(viewModel::toggleMangalibGenre)
                    GenreSheetSource.REMANGA -> key.toIntOrNull()?.let(viewModel::toggleRemangaGenre)
                    GenreSheetSource.DESUME -> viewModel.toggleDesumeGenre(key)
                }
            },
            onReset = {
                when (sheet) {
                    GenreSheetSource.MANGALIB -> viewModel.clearMangalibFilters()
                    GenreSheetSource.REMANGA -> viewModel.clearRemangaFilters()
                    GenreSheetSource.DESUME -> viewModel.clearDesumeFilters()
                }
            },
            onDismiss = { genreSheet = null },
        )
    }
}

/** One source: a titled card holding its filter controls. */
@Composable
private fun SourceCard(
    title: String,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (onReset != null) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.explore_filters_reset))
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
    )
}

/** MangaDex chapter languages; both off means every translation counts. */
@Composable
private fun LanguageChips(
    languages: Set<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = "en" in languages,
            onClick = { onToggle("en", "en" !in languages) },
            label = { Text(stringResource(R.string.language_english)) },
        )
        FilterChip(
            selected = "ru" in languages,
            onClick = { onToggle("ru", "ru" !in languages) },
            label = { Text(stringResource(R.string.language_russian)) },
        )
    }
}

/** Status chips; a chip on keeps that status in the catalog. */
@Composable
private fun StatusChips(
    statuses: List<Pair<Int, Int>>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        statuses.forEach { (id, label) ->
            FilterChip(
                selected = id in selected,
                onClick = { onToggle(id) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

/** Opens the genre sheet; the label shows how many genres are active. */
@Composable
private fun GenresChip(selectedCount: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selectedCount > 0,
        onClick = onClick,
        label = {
            Text(
                if (selectedCount == 0) {
                    stringResource(R.string.explore_filter_genres)
                } else {
                    stringResource(R.string.explore_filter_genres_n, selectedCount)
                },
            )
        },
    )
}

/**
 * One self-hosted server: URL, optional login and password, saved on the
 * explicit Save action so partial edits never break the live connection.
 * [extra] renders server-specific controls under the credentials.
 */
@Composable
private fun ServerCard(
    title: String,
    hint: String,
    connection: ServerConnection,
    onSave: (ServerConnection) -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    var url by remember(connection.url) { mutableStateOf(connection.url) }
    var user by remember(connection.user) { mutableStateOf(connection.user) }
    var password by remember(connection.password) { mutableStateOf(connection.password) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text("https://komga.example.org") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_server_login)) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_server_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (connection.isConfigured) {
                    Text(
                        text = stringResource(R.string.settings_server_connected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
                FilledTonalButton(
                    onClick = { onSave(ServerConnection(url = url, user = user, password = password)) },
                ) {
                    Text(stringResource(R.string.settings_server_save))
                }
            }
            extra?.invoke()
        }
    }
}

/**
 * Picks which Tachiyomi source inside a Suwayomi server fills the catalog.
 * The server answers the list over GraphQL; a refresh button retries it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuwayomiSourcePicker(
    state: UiState<List<SuwayomiSourceOption>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    when (state) {
        is UiState.Loading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LinearWavyProgressIndicator(Modifier.padding(start = 4.dp))
            Text(
                text = stringResource(R.string.settings_suwayomi_sources_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is UiState.Error -> if (state.message != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_suwayomi_sources_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_retry))
                }
            }
        }
        is UiState.Success -> SourceDropdown(
            options = state.data,
            selectedId = selectedId,
            onSelect = onSelect,
            onRefresh = onRefresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceDropdown(
    options: List<SuwayomiSourceOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.let { source ->
        sourceLabel(source)
    } ?: stringResource(R.string.settings_suwayomi_source_auto)

    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_suwayomi_source)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_suwayomi_source_auto)) },
                    onClick = {
                        expanded = false
                        onSelect("")
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(sourceLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option.id)
                        },
                    )
                }
            }
        }
        FilledTonalIconButton(
            onClick = onRefresh,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.action_retry),
            )
        }
    }
}

private fun sourceLabel(option: SuwayomiSourceOption): String =
    if (option.lang.isBlank()) option.name else "${option.name} (${option.lang})"

/**
 * Genre picker shared by MangaLib / Remanga / Desu.me. Chips apply
 * instantly; every tap updates the stored filter and the Explore feed
 * picks the change up on its own.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GenreSheet(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
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
                options.forEach { (key, label) ->
                    FilterChip(
                        selected = key in selected,
                        onClick = { onToggle(key) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
