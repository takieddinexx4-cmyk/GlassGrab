package com.glassgrab.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.glassgrab.core.domain.model.DownloadStatus
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.model.Platform

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val platform: String,
    val formatLabel: String,
    val formatSelector: String,
    val container: String,
    val isAudioOnly: Boolean,
    val isNoWatermark: Boolean,
    val downloadSubtitles: Boolean,
    val downloadThumbnail: Boolean,
    val status: String,
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
) {
    fun toDomain(): DownloadTask = DownloadTask(
        id = id,
        url = url,
        title = title,
        author = author,
        thumbnailUrl = thumbnailUrl,
        platform = runCatching { Platform.valueOf(platform) }.getOrDefault(Platform.GENERIC),
        formatLabel = formatLabel,
        formatSelector = formatSelector,
        container = container,
        isAudioOnly = isAudioOnly,
        isNoWatermark = isNoWatermark,
        downloadSubtitles = downloadSubtitles,
        downloadThumbnail = downloadThumbnail,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED),
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        speedBytesPerSec = speedBytesPerSec,
        etaSeconds = etaSeconds,
        outputUri = outputUri,
        outputFilePath = outputFilePath,
        errorMessage = errorMessage,
        createdAtEpochMs = createdAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )
}
