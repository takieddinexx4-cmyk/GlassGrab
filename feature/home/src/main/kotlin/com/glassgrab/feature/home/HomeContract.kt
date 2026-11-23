package com.glassgrab.feature.home

import androidx.compose.runtime.Immutable
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.domain.model.MediaItemInfo

@Immutable
data class HomeUiState(
    val urlInput: String = "",
    val detectedClipboardUrl: String? = null,
    val isAnalyzing: Boolean = false,
    val analyzedMedia: MediaItemInfo? = null,
    val error: GlassError? = null,
)

sealed interface HomeIntent {
    data class UrlChanged(val value: String) : HomeIntent
    data class ClipboardDetected(val url: String?) : HomeIntent
    data object UseClipboardUrl : HomeIntent
    data object DismissClipboardBanner : HomeIntent
    data object AnalyzeClicked : HomeIntent
    data class AnalyzeSharedUrl(val url: String) : HomeIntent
    data object ClearInput : HomeIntent
    data object RetryAnalyze : HomeIntent
}

sealed interface HomeEffect {
    data class OpenFormatPicker(val media: MediaItemInfo) : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}
