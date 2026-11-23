package com.glassgrab.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.repository.DownloadRepository
import com.glassgrab.core.domain.usecase.StartDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val startDownloadUseCase: StartDownloadUseCase,
) : ViewModel() {

    val activeDownloads: StateFlow<List<DownloadTask>> = repository.observeActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun enqueue(requests: List<DownloadRequest>) {
        viewModelScope.launch {
            if (requests.size == 1) {
                startDownloadUseCase(requests.first())
            } else {
                startDownloadUseCase.enqueueBatch(requests)
            }
        }
    }

    fun pause(id: String) = viewModelScope.launch { repository.pauseDownload(id) }
    fun resume(id: String) = viewModelScope.launch { repository.resumeDownload(id) }
    fun retry(id: String) = viewModelScope.launch { repository.retryDownload(id) }
    fun cancel(id: String) = viewModelScope.launch { repository.cancelDownload(id) }
}
