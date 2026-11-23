package com.glassgrab.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.data.local.DownloadDao
import com.glassgrab.core.data.local.DownloadEntity
import com.glassgrab.core.data.media.MediaStoreExporter
import com.glassgrab.core.data.worker.DownloadWorker
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.DownloadStatus
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.model.LibrarySort
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.UserPreferences
import com.glassgrab.core.domain.repository.DownloadRepository
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import com.glassgrab.core.domain.repository.PreferencesRepository
import com.glassgrab.core.engine.YtDlpEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaExtractorRepositoryImpl @Inject constructor(
    private val engine: YtDlpEngine,
) : MediaExtractorRepository {
    override suspend fun analyzeUrl(url: String): GlassResult<MediaItemInfo> = engine.analyze(url)
    override suspend fun updateEngine(force: Boolean): GlassResult<String> = engine.updateStable()
    override suspend fun getEngineVersion(): String = engine.currentVersion()
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = dataStore.data
    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.updateData(transform)
    }
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val engine: YtDlpEngine,
    private val exporter: MediaStoreExporter,
    private val preferencesRepository: PreferencesRepository,
) : DownloadRepository {

    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    override fun observeActiveDownloads(): Flow<List<DownloadTask>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeLibrary(query: String, sort: LibrarySort): Flow<List<DownloadTask>> =
        dao.observeCompleted().map { list ->
            val filtered = if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true) ||
                        it.platform.contains(query, ignoreCase = true)
                }
            }
            val domain = filtered.map { it.toDomain() }
            when (sort) {
                LibrarySort.NEWEST -> domain.sortedByDescending { it.completedAtEpochMs ?: it.createdAtEpochMs }
                LibrarySort.OLDEST -> domain.sortedBy { it.completedAtEpochMs ?: it.createdAtEpochMs }
                LibrarySort.LARGEST -> domain.sortedByDescending { it.totalBytes ?: 0L }
                LibrarySort.NAME -> domain.sortedBy { it.title.lowercase() }
            }
        }

    override suspend fun getTask(id: String): DownloadTask? = dao.getById(id)?.toDomain()

    override suspend fun enqueueDownload(request: DownloadRequest): GlassResult<String> {
        val id = UUID.randomUUID().toString()
        val entity = DownloadEntity(
            id = id,
            url = request.url,
            title = request.title,
            author = request.author,
            thumbnailUrl = request.thumbnailUrl,
            platform = request.platform.name,
            formatLabel = request.format.label,
            formatSelector = request.format.ytDlpSelector,
            container = request.format.container,
            isAudioOnly = request.format.isAudioOnly,
            isNoWatermark = request.format.isNoWatermark,
            downloadSubtitles = request.includeSubtitles,
            downloadThumbnail = request.includeThumbnail,
            status = DownloadStatus.QUEUED.name,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = request.format.fileSizeBytes,
            speedBytesPerSec = 0L,
            etaSeconds = null,
            outputUri = null,
            outputFilePath = null,
            errorMessage = null,
            createdAtEpochMs = System.currentTimeMillis(),
            completedAtEpochMs = null,
        )
        dao.upsert(entity)
        scheduleWorker(id)
        return GlassResult.Success(id)
    }

    override suspend fun enqueueBatch(requests: List<DownloadRequest>): GlassResult<List<String>> {
        val ids = requests.map { req ->
            when (val res = enqueueDownload(req)) {
                is GlassResult.Success -> res.data
                is GlassResult.Failure -> return res
            }
        }
        return GlassResult.Success(ids)
    }

    override suspend fun pauseDownload(id: String): GlassResult<Unit> {
        dao.updateStatus(id, DownloadStatus.PAUSED.name, null)
        engine.cancelProcess(id)
        workManager.cancelUniqueWork("download_$id")
        return GlassResult.Success(Unit)
    }

    override suspend fun resumeDownload(id: String): GlassResult<Unit> {
        dao.updateStatus(id, DownloadStatus.QUEUED.name, null)
        scheduleWorker(id)
        return GlassResult.Success(Unit)
    }

    override suspend fun cancelDownload(id: String): GlassResult<Unit> {
        dao.updateStatus(id, DownloadStatus.CANCELLED.name, null)
        engine.cancelProcess(id)
        workManager.cancelUniqueWork("download_$id")
        dao.deleteById(id)
        return GlassResult.Success(Unit)
    }

    override suspend fun retryDownload(id: String): GlassResult<Unit> = resumeDownload(id)

    override suspend fun deleteFromLibrary(id: String, deleteFile: Boolean): GlassResult<Unit> {
        val entity = dao.getById(id)
        if (deleteFile && entity != null) {
            exporter.deleteExported(entity.outputUri, entity.outputFilePath)
        }
        dao.deleteById(id)
        return GlassResult.Success(Unit)
    }

    private suspend fun scheduleWorker(taskId: String) {
        val prefs = preferencesRepository.preferences.first()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DownloadWorker.KEY_TASK_ID to taskId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("glassgrab_download")
            .build()

        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }
}
