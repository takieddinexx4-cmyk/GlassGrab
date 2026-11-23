package com.glassgrab.core.domain.repository

import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.model.LibrarySort
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface MediaExtractorRepository {
    suspend fun analyzeUrl(url: String): GlassResult<MediaItemInfo>
    suspend fun updateEngine(force: Boolean = false): GlassResult<String>
    suspend fun getEngineVersion(): String
}

interface DownloadRepository {
    fun observeActiveDownloads(): Flow<List<DownloadTask>>
    fun observeLibrary(query: String = "", sort: LibrarySort = LibrarySort.NEWEST): Flow<List<DownloadTask>>
    suspend fun getTask(id: String): DownloadTask?
    suspend fun enqueueDownload(request: DownloadRequest): GlassResult<String>
    suspend fun enqueueBatch(requests: List<DownloadRequest>): GlassResult<List<String>>
    suspend fun pauseDownload(id: String): GlassResult<Unit>
    suspend fun resumeDownload(id: String): GlassResult<Unit>
    suspend fun cancelDownload(id: String): GlassResult<Unit>
    suspend fun retryDownload(id: String): GlassResult<Unit>
    suspend fun deleteFromLibrary(id: String, deleteFile: Boolean = true): GlassResult<Unit>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}
