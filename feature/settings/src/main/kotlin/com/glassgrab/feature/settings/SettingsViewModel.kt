package com.glassgrab.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.model.AppLanguage
import com.glassgrab.core.domain.model.DefaultQuality
import com.glassgrab.core.domain.model.ThemeMode
import com.glassgrab.core.domain.model.UserPreferences
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import com.glassgrab.core.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isUpdatingEngine: Boolean = false,
    val engineStatusMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val extractorRepository: MediaExtractorRepository,
) : ViewModel() {

    private val updatingFlow = MutableStateFlow(false)
    private val statusMsgFlow = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        updatingFlow,
        statusMsgFlow,
    ) { prefs, updating, msg ->
        SettingsUiState(prefs = prefs, isUpdatingEngine = updating, engineStatusMessage = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        preferencesRepository.update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(dynamicColor = enabled) }
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferencesRepository.update { it.copy(language = language) }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun setDefaultQuality(quality: DefaultQuality) = viewModelScope.launch {
        preferencesRepository.update { it.copy(defaultQuality = quality) }
    }

    fun setMaxParallelDownloads(count: Int) = viewModelScope.launch {
        preferencesRepository.update { it.copy(maxParallelDownloads = count.coerceIn(1, 5)) }
    }

    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(wifiOnly = enabled) }
    }

    fun setAutoUpdateEngine(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.update { it.copy(autoUpdateEngine = enabled) }
    }

    fun updateEngineNow() = viewModelScope.launch {
        if (updatingFlow.value) return@launch
        updatingFlow.value = true
        statusMsgFlow.value = "Checking yt-dlp stable channel..."
        when (val result = extractorRepository.updateEngine(force = true)) {
            is GlassResult.Success -> {
                preferencesRepository.update {
                    it.copy(
                        engineVersion = result.data,
                        lastEngineUpdateEpochMs = System.currentTimeMillis(),
                    )
                }
                statusMsgFlow.value = "Engine ready: ${result.data}"
            }
            is GlassResult.Failure -> {
                statusMsgFlow.value = "Update failed: ${result.error.technicalDetail ?: "Check network"}"
            }
        }
        updatingFlow.value = false
    }
}
