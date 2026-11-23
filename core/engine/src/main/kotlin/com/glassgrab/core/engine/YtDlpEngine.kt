package com.glassgrab.core.engine

import android.content.Context
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.common.IoDispatcher
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgressEvent(
    val progressPercent: Float,
    val etaSeconds: Long?,
    val statusLine: String,
)

@Singleton
class YtDlpEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val parser: YtDlpJsonParser,
) {
    private val initMutex = Mutex()
    @Volatile
    private var initialized = false

    suspend fun ensureInitialized(): GlassResult<Unit> = withContext(ioDispatcher) {
        if (initialized) return@withContext GlassResult.Success(Unit)
        initMutex.withLock {
            if (initialized) return@withContext GlassResult.Success(Unit)
            runCatching {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
                Aria2c.getInstance().init(context)
                initialized = true
                GlassResult.Success(Unit)
            }.getOrElse { t ->
                if (t is CancellationException) throw t
                GlassResult.Failure(GlassError.Unknown("Engine initialization failed: ${t.message}"))
            }
        }
    }

    suspend fun analyze(url: String): GlassResult<MediaItemInfo> = withContext(ioDispatcher) {
        val initResult = ensureInitialized()
        if (initResult is GlassResult.Failure) return@withContext initResult

        val platform = parser.detectPlatform(url)
        if (platform == Platform.TIKTOK) {
            return@withContext analyzeTikTokWithFallback(url)
        }

        executeWithRetry(maxAttempts = 2, timeoutMs = 45_000L) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
                addOption("--no-check-certificates")
            }
            val response = YoutubeDL.getInstance().execute(request)
            parser.parseDumpJson(response.out, url)
        }
    }

    private suspend fun analyzeTikTokWithFallback(url: String): GlassResult<MediaItemInfo> {
        val primary = executeWithRetry(maxAttempts = 2, timeoutMs = 35_000L) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("--extractor-args", "tiktok:api_hostname=api22-normal-c-useast2a.tiktokv.com")
            }
            val response = YoutubeDL.getInstance().execute(request)
            parser.parseDumpJson(response.out, url)
        }
        if (primary is GlassResult.Success) return primary

        val fallback = executeWithRetry(maxAttempts = 1, timeoutMs = 35_000L) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--no-warnings")
                addOption("--extractor-args", "tiktok:webpage_download=1")
            }
            val response = YoutubeDL.getInstance().execute(request)
            parser.parseDumpJson(response.out, url)
        }
        if (fallback is GlassResult.Success) return fallback

        val primaryReason = (primary as GlassResult.Failure).error.technicalDetail ?: "Primary API path failed"
        val fallbackReason = (fallback as GlassResult.Failure).error.technicalDetail ?: "Web extractor fallback failed"
        return GlassResult.Failure(
            GlassError.TikTokExtractionFailed(
                primaryReason = primaryReason,
                fallbackReason = fallbackReason,
                technicalDetail = "$primaryReason | $fallbackReason",
            ),
        )
    }

    suspend fun download(
        processId: String,
        url: String,
        formatSelector: String,
        container: String,
        isAudioOnly: Boolean,
        includeSubtitles: Boolean,
        includeThumbnail: Boolean,
        outputDir: File,
        onProgress: (DownloadProgressEvent) -> Unit,
    ): GlassResult<File> = withContext(ioDispatcher) {
        val initResult = ensureInitialized()
        if (initResult is GlassResult.Failure) return@withContext initResult

        outputDir.mkdirs()
        val template = File(outputDir, "%(title).80B [%(id)s].%(ext)s").absolutePath
        val platform = parser.detectPlatform(url)

        executeWithRetry(maxAttempts = 2, timeoutMs = 30L * 60L * 1000L) {
            val request = YoutubeDLRequest(url).apply {
                addOption("-o", template)
                addOption("-f", formatSelector)
                addOption("--continue")
                addOption("--no-mtime")
                addOption("--embed-metadata")
                if (platform == Platform.TIKTOK) {
                    addOption("--extractor-args", "tiktok:api_hostname=api22-normal-c-useast2a.tiktokv.com")
                }
                if (isAudioOnly) {
                    addOption("-x")
                    addOption("--audio-format", container.lowercase())
                    addOption("--audio-quality", "0")
                } else {
                    addOption("--merge-output-format", container.lowercase())
                }
                if (includeSubtitles) {
                    addOption("--write-subs")
                    addOption("--embed-subs")
                    addOption("--sub-langs", "en,ar")
                }
                if (includeThumbnail) {
                    addOption("--write-thumbnail")
                }
            }

            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                onProgress(
                    DownloadProgressEvent(
                        progressPercent = progress.coerceIn(0f, 100f),
                        etaSeconds = etaInSeconds.takeIf { it >= 0L },
                        statusLine = line.orEmpty(),
                    ),
                )
            }

            outputDir.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                ?.maxByOrNull { it.lastModified() }
                ?: error("Download completed but output file was not found in ${outputDir.absolutePath}")
        }
    }

    fun cancelProcess(processId: String): Boolean =
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }.getOrDefault(false)

    suspend fun updateStable(): GlassResult<String> = withContext(ioDispatcher) {
        val initResult = ensureInitialized()
        if (initResult is GlassResult.Failure) return@withContext initResult
        runCatching {
            withTimeout(60_000L) {
                val status = YoutubeDL.getInstance().updateYoutubeDL(
                    context,
                    YoutubeDL.UpdateChannel.STABLE,
                )
                val version = YoutubeDL.getInstance().version(context) ?: "Updated ($status)"
                GlassResult.Success(version)
            }
        }.getOrElse { t ->
            if (t is CancellationException) throw t
            GlassResult.Failure(GlassError.EngineUpdateFailed(t.message))
        }
    }

    suspend fun currentVersion(): String = withContext(ioDispatcher) {
        ensureInitialized()
        runCatching { YoutubeDL.getInstance().version(context) ?: "Bundled" }.getOrDefault("Bundled")
    }

    private suspend fun <T> executeWithRetry(
        maxAttempts: Int,
        timeoutMs: Long,
        block: suspend () -> T,
    ): GlassResult<T> {
        var lastError: GlassError = GlassError.Unknown()
        repeat(maxAttempts) { attempt ->
            try {
                val value = withTimeout(timeoutMs) { block() }
                return GlassResult.Success(value)
            } catch (e: TimeoutCancellationException) {
                lastError = GlassError.Timeout("Operation exceeded ${timeoutMs / 1000}s")
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                lastError = mapThrowableToError(t)
                if (lastError is GlassError.PrivateOrRemoved ||
                    lastError is GlassError.AgeRestricted ||
                    lastError is GlassError.GeoBlocked ||
                    lastError is GlassError.UnsupportedUrl
                ) {
                    return GlassResult.Failure(lastError)
                }
            }
            if (attempt < maxAttempts - 1) {
                delay(1500L * (attempt + 1))
            }
        }
        return GlassResult.Failure(lastError)
    }

    internal fun mapThrowableToError(throwable: Throwable): GlassError {
        val msg = throwable.message.orEmpty()
        val lower = msg.lowercase()
        return when {
            lower.contains("private video") || lower.contains("removed") || lower.contains("unavailable") ->
                GlassError.PrivateOrRemoved(msg)
            lower.contains("age") || lower.contains("sign in to confirm your age") ->
                GlassError.AgeRestricted(msg)
            lower.contains("geo") || lower.contains("not available in your country") ->
                GlassError.GeoBlocked(msg)
            lower.contains("unsupported url") ->
                GlassError.UnsupportedUrl("", msg)
            lower.contains("drm") || lower.contains("widevine") ->
                GlassError.DrmNotSupported("DRM Protected", msg)
            lower.contains("unable to resolve host") || lower.contains("network is unreachable") ->
                GlassError.Offline(msg)
            lower.contains("no space left") ->
                GlassError.LowStorage(technicalDetail = msg)
            else -> GlassError.Unknown(msg)
        }
    }
}
