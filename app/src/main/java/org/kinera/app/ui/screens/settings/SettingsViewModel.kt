package org.kinera.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kinera.app.data.settings.KineraSettings
import org.kinera.app.data.settings.ReaderMode
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.ServerId
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.settings.ThemeMode
import org.kinera.app.data.suwayomi.SuwayomiSource
import org.kinera.app.data.suwayomi.SuwayomiSourceOption
import org.kinera.app.core.util.UiState

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val suwayomi: SuwayomiSource,
) : ViewModel() {

    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KineraSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColors(enabled) }

    fun setReaderMode(mode: ReaderMode) = viewModelScope.launch { repository.setReaderMode(mode) }

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repository.setKeepScreenOn(enabled) }

    fun setWebtoonZoom(enabled: Boolean) = viewModelScope.launch { repository.setWebtoonZoom(enabled) }

    fun setReaderRtl(enabled: Boolean) = viewModelScope.launch { repository.setReaderRtl(enabled) }

    fun setMangadexLanguage(language: String, enabled: Boolean) = viewModelScope.launch {
        repository.setMangadexLanguage(language, enabled)
    }

    fun saveServer(server: ServerId, connection: ServerConnection) = viewModelScope.launch {
        repository.setServer(server, connection.copy(url = ServerConnection.normalizeUrl(connection.url)))
        if (server == ServerId.SUWAYOMI) loadSuwayomiSources(force = true)
    }

    /**
     * Sources installed on the Suwayomi server, for the picker. Loaded when
     * the screen opens and reloaded on demand; errors surface in the card
     * instead of crashing.
     */
    private val _suwayomiSources = MutableStateFlow<UiState<List<SuwayomiSourceOption>>>(UiState.Loading)
    val suwayomiSources: StateFlow<UiState<List<SuwayomiSourceOption>>> = _suwayomiSources.asStateFlow()

    private var suwayomiLoadGeneration = 0

    init {
        loadSuwayomiSources()
    }

    fun loadSuwayomiSources(force: Boolean = false) {
        if (!repository.snapshot.value.suwayomi.isConfigured) {
            suwayomiLoadGeneration++
            _suwayomiSources.value = UiState.Error(null)
            return
        }
        if (!force && _suwayomiSources.value is UiState.Loading) return
        val generation = ++suwayomiLoadGeneration
        _suwayomiSources.value = UiState.Loading
        viewModelScope.launch {
            runCatching { suwayomi.availableSources() }
                .onSuccess { if (generation == suwayomiLoadGeneration) _suwayomiSources.value = UiState.Success(it) }
                .onFailure { if (generation == suwayomiLoadGeneration) _suwayomiSources.value = UiState.Error(it.message) }
        }
    }

    fun selectSuwayomiSource(sourceId: String) = viewModelScope.launch {
        repository.setSuwayomiSource(sourceId)
    }
}
