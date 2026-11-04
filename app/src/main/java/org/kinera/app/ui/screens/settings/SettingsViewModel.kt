package org.kinera.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.data.settings.KineraSettings
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.settings.ThemeMode

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KineraSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColors(enabled) }

    fun setReaderMode(mode: ReaderMode) = viewModelScope.launch { repository.setReaderMode(mode) }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(enabled) }

    fun setWebtoonZoom(enabled: Boolean) = viewModelScope.launch { repository.setWebtoonZoom(enabled) }

    fun saveServer(server: ServerId, connection: ServerConnection) = viewModelScope.launch {
        repository.setServer(server, connection.copy(url = ServerConnection.normalizeUrl(connection.url)))
    }
}
