package org.kinera.app.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.data.backup.BackupManager
import org.kinera.app.data.backup.RestoreSummary
import org.kinera.app.data.settings.KineraSettings
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.settings.ThemeMode
import org.kinera.app.data.repository.LibraryUpdateScheduler

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val libraryUpdateScheduler: LibraryUpdateScheduler,
    private val backupManager: BackupManager,
) : ViewModel() {

    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KineraSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColors(enabled) }

    fun setReaderMode(mode: ReaderMode) = viewModelScope.launch { repository.setReaderMode(mode) }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(enabled) }

    fun setWebtoonZoom(enabled: Boolean) = viewModelScope.launch { repository.setWebtoonZoom(enabled) }

    fun setReaderRtl(enabled: Boolean) = viewModelScope.launch { repository.setReaderRtl(enabled) }

    fun setLibraryUpdate(enabled: Boolean) = viewModelScope.launch {
        repository.setLibraryUpdateEnabled(enabled)
        libraryUpdateScheduler.applySettings()
    }

    fun setLibraryUpdateInterval(hours: Int) = viewModelScope.launch {
        repository.setLibraryUpdateInterval(hours)
        libraryUpdateScheduler.applySettings()
    }

    fun setMangadexLanguage(language: String, enabled: Boolean) = viewModelScope.launch {
        repository.setMangadexLanguage(language, enabled)
    }

    fun saveServer(server: ServerId, connection: ServerConnection) = viewModelScope.launch {
        repository.setServer(server, connection.copy(url = ServerConnection.normalizeUrl(connection.url)))
    }

    /* ----- library backup ----- */

    /** One-shot result of the last export / restore, consumed by the screen. */
    private val _backupState = MutableStateFlow<BackupState?>(null)
    val backupState: StateFlow<BackupState?> = _backupState.asStateFlow()

    fun exportLibrary(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = backupManager.export(uri).fold(
                onSuccess = { BackupState.Exported(it) },
                onFailure = { BackupState.Failed(it.message) },
            )
        }
    }

    fun importLibrary(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = backupManager.import(uri).fold(
                onSuccess = { BackupState.Restored(it) },
                onFailure = { BackupState.Failed(it.message) },
            )
        }
    }

    fun consumeBackupState() {
        _backupState.value = null
    }
}

sealed interface BackupState {
    data class Exported(val favourites: Int) : BackupState
    data class Restored(val summary: RestoreSummary) : BackupState
    data class Failed(val message: String?) : BackupState
}
