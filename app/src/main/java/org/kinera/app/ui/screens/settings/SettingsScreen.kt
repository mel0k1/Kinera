package org.kinera.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatTextdirectionRToL
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Sync
import org.kinera.app.BuildConfig
import org.kinera.app.R
import org.kinera.app.core.util.UiState
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.settings.ThemeMode
import org.kinera.app.data.suwayomi.SuwayomiSourceOption
import org.koin.compose.viewmodel.koinViewModel

private const val REPO_URL = "https://github.com/mel0k1/Kinera"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val suwayomiSources by viewModel.suwayomiSources.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val backupFileName = "kinera-backup-" +
        java.time.LocalDate.now().toString() + ".json"
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportLibrary(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importLibrary(uri) }

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupState.Exported -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_backup_exported, state.favourites),
                )
                viewModel.consumeBackupState()
            }
            is BackupState.Restored -> {
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.settings_backup_restored,
                        state.summary.favourites,
                    ),
                )
                viewModel.consumeBackupState()
            }
            is BackupState.Failed -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_backup_failed),
                )
                viewModel.consumeBackupState()
            }
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                subtitle = {
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance), Icons.Rounded.Palette)

            Text(
                text = stringResource(R.string.settings_theme_mode),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            },
                        )
                    }
                }
            }

            SwitchRow(
                icon = Icons.Rounded.Contrast,
                title = stringResource(R.string.settings_dynamic_colors),
                subtitle = stringResource(R.string.settings_dynamic_colors_summary),
                checked = settings.dynamicColors,
                onCheckedChange = viewModel::setDynamicColors,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_reader), Icons.Rounded.MenuBook)

            Text(
                text = stringResource(R.string.settings_reader_mode),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                ReaderMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.readerMode == mode,
                        onClick = { viewModel.setReaderMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ReaderMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ReaderMode.WEBTOON -> stringResource(R.string.reader_mode_webtoon)
                                ReaderMode.PAGED -> stringResource(R.string.reader_mode_paged)
                            },
                        )
                    }
                }
            }

            SwitchRow(
                icon = Icons.Rounded.StayCurrentLandscape,
                title = stringResource(R.string.settings_keep_screen_on),
                subtitle = stringResource(R.string.settings_keep_screen_on_summary),
                checked = settings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn,
            )

            SwitchRow(
                icon = Icons.Rounded.ZoomIn,
                title = stringResource(R.string.settings_webtoon_zoom),
                subtitle = stringResource(R.string.settings_webtoon_zoom_summary),
                checked = settings.webtoonZoom,
                onCheckedChange = viewModel::setWebtoonZoom,
            )

            SwitchRow(
                icon = Icons.AutoMirrored.Rounded.FormatTextdirectionRToL,
                title = stringResource(R.string.settings_reader_rtl),
                subtitle = stringResource(R.string.settings_reader_rtl_summary),
                checked = settings.readerRtl,
                onCheckedChange = viewModel::setReaderRtl,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_library), Icons.Rounded.Sync)

            SwitchRow(
                icon = Icons.Rounded.Sync,
                title = stringResource(R.string.settings_library_update),
                subtitle = stringResource(R.string.settings_library_update_summary),
                checked = settings.libraryUpdateEnabled,
                onCheckedChange = viewModel::setLibraryUpdate,
            )

            if (settings.libraryUpdateEnabled) {
                Text(
                    text = stringResource(R.string.settings_library_interval),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    org.kinera.app.data.settings.UPDATE_INTERVALS.sorted().forEachIndexed { index, hours ->
                        SegmentedButton(
                            selected = settings.libraryUpdateIntervalHours == hours,
                            onClick = { viewModel.setLibraryUpdateInterval(hours) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = org.kinera.app.data.settings.UPDATE_INTERVALS.size,
                            ),
                        ) {
                            Text(stringResource(R.string.settings_library_hours, hours))
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_data), Icons.Rounded.Backup)

            ActionRow(
                icon = Icons.Rounded.Backup,
                title = stringResource(R.string.settings_backup_export),
                subtitle = stringResource(R.string.settings_backup_export_summary),
                onClick = { exportLauncher.launch(backupFileName) },
            )
            ActionRow(
                icon = Icons.Rounded.Restore,
                title = stringResource(R.string.settings_backup_import),
                subtitle = stringResource(R.string.settings_backup_import_summary),
                onClick = { importLauncher.launch(arrayOf("application/json")) },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_sources), Icons.Rounded.Language)

            Text(
                text = stringResource(R.string.settings_mangadex_languages),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = "en" in settings.mangadexLanguages,
                    onClick = { viewModel.setMangadexLanguage("en", "en" !in settings.mangadexLanguages) },
                    label = { Text(stringResource(R.string.language_english)) },
                )
                FilterChip(
                    selected = "ru" in settings.mangadexLanguages,
                    onClick = { viewModel.setMangadexLanguage("ru", "ru" !in settings.mangadexLanguages) },
                    label = { Text(stringResource(R.string.language_russian)) },
                )
            }
            Text(
                text = stringResource(R.string.settings_mangadex_languages_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_servers), Icons.Rounded.Dns)

            ServerCard(
                title = stringResource(R.string.settings_server_komga),
                hint = stringResource(R.string.settings_server_komga_hint),
                connection = settings.komga,
                onSave = { viewModel.saveServer(ServerId.KOMGA, it) },
            )

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

            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            SectionHeader(stringResource(R.string.settings_about), Icons.Rounded.Code)

            AboutRow(
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
            )
            AboutRow(
                title = stringResource(R.string.settings_license),
                subtitle = stringResource(R.string.settings_license_value),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_source_code),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = REPO_URL,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_retry))
        }
    }
}

private fun sourceLabel(option: SuwayomiSourceOption): String =
    if (option.lang.isBlank()) option.name else "${option.name} (${option.lang})"

@Composable
private fun SwitchRow(    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Tappable settings row used by the backup actions. */
@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(start = 44.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
