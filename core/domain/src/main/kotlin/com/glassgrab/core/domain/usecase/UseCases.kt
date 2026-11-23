package com.glassgrab.core.domain.usecase

import com.glassgrab.core.common.DeviceCapabilities
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.repository.DownloadRepository
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import javax.inject.Inject

class AnalyzeUrlUseCase @Inject constructor(
    private val extractorRepository: MediaExtractorRepository,
    private val capabilities: DeviceCapabilities,
) {
    suspend operator fun invoke(rawInput: String): GlassResult<MediaItemInfo> {
        val url = rawInput.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return GlassResult.Failure(GlassError.UnsupportedUrl(url, "URL must start with http:// or https://"))
        }
        val drmHost = detectDrmHost(url)
        if (drmHost != null) {
            return GlassResult.Failure(GlassError.DrmNotSupported(drmHost))
        }
        if (!capabilities.isOnline()) {
            return GlassResult.Failure(GlassError.Offline())
        }
        return extractorRepository.analyzeUrl(url)
    }

    private fun detectDrmHost(url: String): String? {
        val lower = url.lowercase()
        val blocked = mapOf(
            "netflix.com" to "Netflix",
            "disneyplus.com" to "Disney+",
            "hulu.com" to "Hulu",
            "primevideo.com" to "Prime Video",
            "max.com" to "Max",
            "tv.apple.com" to "Apple TV+",
            "open.spotify.com" to "Spotify",
            "peacocktv.com" to "Peacock",
            "paramountplus.com" to "Paramount+",
        )
        return blocked.entries.firstOrNull { (host, _) -> lower.contains(host) }?.value
    }
}

class StartDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val capabilities: DeviceCapabilities,
) {
    suspend operator fun invoke(request: DownloadRequest): GlassResult<String> {
        if (!capabilities.isOnline()) {
            return GlassResult.Failure(GlassError.Offline())
        }
        val required = request.format.fileSizeBytes ?: MINIMUM_FREE_BYTES
        val available = capabilities.availableStorageBytes()
        if (available < required + SAFETY_MARGIN_BYTES) {
            return GlassResult.Failure(
                GlassError.LowStorage(
                    requiredBytes = required,
                    availableBytes = available,
                ),
            )
        }
        return downloadRepository.enqueueDownload(request)
    }

    suspend fun enqueueBatch(requests: List<DownloadRequest>): GlassResult<List<String>> {
        if (!capabilities.isOnline()) {
            return GlassResult.Failure(GlassError.Offline())
        }
        return downloadRepository.enqueueBatch(requests)
    }

    private companion object {
        const val MINIMUM_FREE_BYTES = 25L * 1024L * 1024L
        const val SAFETY_MARGIN_BYTES = 50L * 1024L * 1024L
    }
}
