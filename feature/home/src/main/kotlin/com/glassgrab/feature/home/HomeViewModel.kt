package com.glassgrab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.usecase.AnalyzeUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val analyzeUrlUseCase: AnalyzeUrlUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var analyzeJob: Job? = null

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.UrlChanged -> _state.update { it.copy(urlInput = intent.value, error = null) }
            is HomeIntent.ClipboardDetected -> {
                val cleaned = Formatters.extractFirstUrl(intent.url)
                if (cleaned != null && cleaned != _state.value.urlInput) {
                    _state.update { it.copy(detectedClipboardUrl = cleaned) }
                }
            }
            HomeIntent.UseClipboardUrl -> {
                val clip = _state.value.detectedClipboardUrl ?: return
                _state.update { it.copy(urlInput = clip, detectedClipboardUrl = null, error = null) }
                triggerAnalyze(clip)
            }
            HomeIntent.DismissClipboardBanner -> _state.update { it.copy(detectedClipboardUrl = null) }
            HomeIntent.AnalyzeClicked, HomeIntent.RetryAnalyze -> {
                val extracted = Formatters.extractFirstUrl(_state.value.urlInput) ?: _state.value.urlInput.trim()
                triggerAnalyze(extracted)
            }
            is HomeIntent.AnalyzeSharedUrl -> {
                val extracted = Formatters.extractFirstUrl(intent.url) ?: intent.url.trim()
                _state.update { it.copy(urlInput = extracted, detectedClipboardUrl = null, error = null) }
                triggerAnalyze(extracted)
            }
            HomeIntent.ClearInput -> {
                analyzeJob?.cancel()
                _state.update { it.copy(urlInput = "", isAnalyzing = false, error = null) }
            }
        }
    }

    private fun triggerAnalyze(url: String) {
        if (url.isBlank()) return
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, error = null) }
            when (val result = analyzeUrlUseCase(url)) {
                is GlassResult.Success -> {
                    _state.update { it.copy(isAnalyzing = false, analyzedMedia = result.data, error = null) }
                    _effects.send(HomeEffect.OpenFormatPicker(result.data))
                }
                is GlassResult.Failure -> {
                    _state.update { it.copy(isAnalyzing = false, error = result.error) }
                }
            }
        }
    }
}
