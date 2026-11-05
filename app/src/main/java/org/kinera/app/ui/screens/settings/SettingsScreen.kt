package org.kinera.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatTextdirectionRToL
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Sync
import org.kinera.app.BuildConfig
import org.kinera.app.R
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

private const val REPO_URL = "https://github.com/mel0k1/Kinera"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
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

            ActionRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.sources_title),
                subtitle = stringResource(R.string.sources_subtitle),
                onClick = onOpenSources,
            )

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

@Composable
private fun SwitchRow(
    icon: ImageVector,
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

/** Tappable settings row used by the backup actions and the sources entry. */
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
