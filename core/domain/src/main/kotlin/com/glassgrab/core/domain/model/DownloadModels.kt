package com.glassgrab.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    MERGING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}

@Serializable
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val platform: Platform,
    val formatLabel: String,
    val formatSelector: String,
    val container: String,
    val isAudioOnly: Boolean,
    val isNoWatermark: Boolean,
    val downloadSubtitles: Boolean,
    val downloadThumbnail: Boolean,
    val status: DownloadStatus,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val speedBytesPerSec: Long,
    val etaSeconds: Long?,
    val outputUri: String?,
    val outputFilePath: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?,
)

@Serializable
data class DownloadRequest(
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val platform: Platform,
    val format: MediaFormat,
    val includeSubtitles: Boolean = false,
    val includeThumbnail: Boolean = false,
)

enum class LibrarySort {
    NEWEST,
    OLDEST,
    LARGEST,
    NAME,
}
