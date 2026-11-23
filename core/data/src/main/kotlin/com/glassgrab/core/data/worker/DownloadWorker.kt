package com.glassgrab.core.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.data.local.DownloadDao
import com.glassgrab.core.data.media.MediaStoreExporter
import com.glassgrab.core.domain.model.DownloadStatus
import com.glassgrab.core.engine.YtDlpEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: DownloadDao,
    private val engine: YtDlpEngine,
    private val exporter: MediaStoreExporter,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val task = dao.getById(taskId) ?: return Result.failure()

        ensureChannel()
        setForeground(createForegroundInfo(taskId, task.title, 0))
        dao.updateStatus(taskId, DownloadStatus.DOWNLOADING.name, null)

        val stagingDir = File(applicationContext.cacheDir, "staging/$taskId")
        val downloadResult = try {
            engine.download(
                processId = taskId,
                url = task.url,
                formatSelector = task.formatSelector,
                container = task.container,
                isAudioOnly = task.isAudioOnly,
                includeSubtitles = task.downloadSubtitles,
                includeThumbnail = task.downloadThumbnail,
                outputDir = stagingDir,
                onProgress = { event ->
                    val fraction = (event.progressPercent / 100f).coerceIn(0f, 1f)
                    val status = if (event.statusLine.contains("Merger", ignoreCase = true) ||
                        event.statusLine.contains("ffmpeg", ignoreCase = true)
                    ) {
                        DownloadStatus.MERGING
                    } else {
                        DownloadStatus.DOWNLOADING
                    }
                    val approxBytes = task.totalBytes?.let { (it * fraction).toLong() } ?: 0L
                    setProgressAsync(
                        androidx.work.workDataOf(
                            "progress" to fraction,
                            "eta" to (event.etaSeconds ?: -1L),
                        ),
                    )
                    // Lightweight DB progress update launched safely
                    runCatching {
                        setForegroundAsync(createForegroundInfo(taskId, task.title, event.progressPercent.toInt()))
                    }
                    kotlinx.coroutines.runBlocking {
                        dao.updateProgress(
                            id = taskId,
                            status = status.name,
                            progress = fraction,
                            downloadedBytes = approxBytes,
                            speedBytesPerSec = 0L,
                            etaSeconds = event.etaSeconds,
                        )
                    }
                },
            )
        } catch (e: CancellationException) {
            engine.cancelProcess(taskId)
            val current = dao.getById(taskId)
            if (current != null && current.status != DownloadStatus.PAUSED.name && current.status != DownloadStatus.CANCELLED.name) {
                dao.updateStatus(taskId, DownloadStatus.PAUSED.name, null)
            }
            throw e
        }

        return when (downloadResult) {
            is GlassResult.Success -> {
                val exported = exporter.exportToGallery(downloadResult.data, task.isAudioOnly)
                stagingDir.deleteRecursively()
                dao.markCompleted(
                    id = taskId,
                    outputUri = exported.contentUri,
                    outputFilePath = exported.filePath,
                    totalBytes = exported.sizeBytes,
                    completedAtEpochMs = System.currentTimeMillis(),
                )
                Result.success()
            }
            is GlassResult.Failure -> {
                dao.updateStatus(
                    id = taskId,
                    status = DownloadStatus.FAILED.name,
                    errorMessage = downloadResult.error.technicalDetail ?: downloadResult.error::class.simpleName,
                )
                Result.retry()
            }
        }
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress for ongoing GlassGrab downloads"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(taskId: String, title: String, percent: Int): ForegroundInfo {
        val cancelIntent = Intent(applicationContext, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_CANCEL
            putExtra(DownloadActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val cancelPending = PendingIntent.getBroadcast(
            applicationContext,
            taskId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$percent% downloaded")
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                taskId.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(taskId.hashCode(), notification)
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val CHANNEL_ID = "glassgrab_downloads"
    }
}
