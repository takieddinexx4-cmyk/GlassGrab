package com.glassgrab.core.engine

import com.glassgrab.core.domain.model.AudioCodec
import com.glassgrab.core.domain.model.MediaFormat
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform
import com.glassgrab.core.domain.model.PlaylistEntry
import com.glassgrab.core.domain.model.VideoCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpJsonParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun detectPlatform(url: String): Platform {
        val lower = url.lowercase()
        return when {
            lower.contains("tiktok.com") -> Platform.TIKTOK
            lower.contains("instagram.com") || lower.contains("instagr.am") -> Platform.INSTAGRAM
            lower.contains("facebook.com") || lower.contains("fb.watch") -> Platform.FACEBOOK
            lower.contains("twitter.com") || lower.contains("x.com") -> Platform.X
            lower.contains("youtube.com") || lower.contains("youtu.be") -> Platform.YOUTUBE
            lower.contains("reddit.com") || lower.contains("redd.it") -> Platform.REDDIT
            lower.contains("pinterest.com") || lower.contains("pin.it") -> Platform.PINTEREST
            lower.contains("snapchat.com") -> Platform.SNAPCHAT
            lower.contains("vimeo.com") -> Platform.VIMEO
            lower.contains("dailymotion.com") || lower.contains("dai.ly") -> Platform.DAILYMOTION
            lower.contains("twitch.tv") -> Platform.TWITCH
            else -> Platform.GENERIC
        }
    }

    fun parseDumpJson(rawJson: String, sourceUrl: String): MediaItemInfo {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val platform = detectPlatform(sourceUrl)
        val type = root["_type"]?.jsonPrimitive?.contentOrNull
        val entriesArray = root["entries"] as? JsonArray

        if (type == "playlist" || (entriesArray != null && entriesArray.isNotEmpty())) {
            val entries = entriesArray.orEmpty().mapNotNull { elem ->
                val obj = elem as? JsonObject ?: return@mapNotNull null
                val entryUrl = obj["webpage_url"]?.jsonPrimitive?.contentOrNull
                    ?: obj["url"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                PlaylistEntry(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: entryUrl.hashCode().toString(),
                    url = entryUrl,
                    title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled video",
                    author = obj["uploader"]?.jsonPrimitive?.contentOrNull
                        ?: obj["channel"]?.jsonPrimitive?.contentOrNull
                        ?: root["uploader"]?.jsonPrimitive?.contentOrNull
                        ?: platform.displayName,
                    thumbnailUrl = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                    durationSeconds = obj["duration"]?.jsonPrimitive?.doubleOrNull?.toLong(),
                )
            }
            return MediaItemInfo(
                id = root["id"]?.jsonPrimitive?.contentOrNull ?: sourceUrl.hashCode().toString(),
                url = sourceUrl,
                title = root["title"]?.jsonPrimitive?.contentOrNull ?: "${platform.displayName} Collection",
                author = root["uploader"]?.jsonPrimitive?.contentOrNull
                    ?: root["channel"]?.jsonPrimitive?.contentOrNull
                    ?: platform.displayName,
                thumbnailUrl = root["thumbnail"]?.jsonPrimitive?.contentOrNull
                    ?: entries.firstOrNull()?.thumbnailUrl,
                durationSeconds = entries.sumOf { it.durationSeconds ?: 0L }.takeIf { it > 0L },
                platform = platform,
                formats = defaultBatchFormats(platform),
                isPlaylist = true,
                playlistEntries = entries,
            )
        }

        val id = root["id"]?.jsonPrimitive?.contentOrNull ?: sourceUrl.hashCode().toString()
        val title = root["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: "Video $id"
        val author = root["uploader"]?.jsonPrimitive?.contentOrNull
            ?: root["channel"]?.jsonPrimitive?.contentOrNull
            ?: root["creator"]?.jsonPrimitive?.contentOrNull
            ?: platform.displayName
        val thumbnail = root["thumbnail"]?.jsonPrimitive?.contentOrNull
        val duration = root["duration"]?.jsonPrimitive?.doubleOrNull?.toLong()
        val subtitlesObj = root["subtitles"] as? JsonObject
        val subtitleLangs = subtitlesObj?.keys?.toList().orEmpty()

        val rawFormats = (root["formats"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }

        val parsedFormats = buildFormatList(rawFormats, platform)

        return MediaItemInfo(
            id = id,
            url = sourceUrl,
            title = title,
            author = author,
            thumbnailUrl = thumbnail,
            durationSeconds = duration,
            platform = platform,
            formats = parsedFormats,
            hasSubtitles = subtitleLangs.isNotEmpty(),
            subtitleLanguages = subtitleLangs,
            isPlaylist = false,
        )
    }

    private fun buildFormatList(rawFormats: List<JsonObject>, platform: Platform): List<MediaFormat> {
        val videoFormats = mutableListOf<MediaFormat>()
        val audioFormats = mutableListOf<MediaFormat>()
        var bestAudioSize = 0L

        for (fmt in rawFormats) {
            val formatId = fmt["format_id"]?.jsonPrimitive?.contentOrNull ?: continue
            val vcodecRaw = fmt["vcodec"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val acodecRaw = fmt["acodec"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val ext = fmt["ext"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "MP4"
            val height = fmt["height"]?.jsonPrimitive?.intOrNull
            val fps = fmt["fps"]?.jsonPrimitive?.doubleOrNull?.toInt()
            val size = fmt["filesize"]?.jsonPrimitive?.longOrNull
                ?: fmt["filesize_approx"]?.jsonPrimitive?.longOrNull
            val formatNote = fmt["format_note"]?.jsonPrimitive?.contentOrNull.orEmpty()

            val hasVideo = vcodecRaw.isNotBlank() && vcodecRaw != "none"
            val hasAudio = acodecRaw.isNotBlank() && acodecRaw != "none"

            if (!hasVideo && hasAudio) {
                if ((size ?: 0L) > bestAudioSize) bestAudioSize = size ?: 0L
                val aCodec = parseAudioCodec(acodecRaw, ext)
                val abr = fmt["abr"]?.jsonPrimitive?.doubleOrNull?.toInt()
                audioFormats += MediaFormat(
                    formatId = formatId,
                    label = buildString {
                        append("Audio ")
                        append(aCodec.label)
                        if (abr != null && abr > 0) append(" ${abr}kbps")
                    },
                    resolutionHeight = null,
                    fps = null,
                    videoCodec = null,
                    audioCodec = aCodec,
                    container = ext,
                    fileSizeBytes = size,
                    isAudioOnly = true,
                    ytDlpSelector = formatId,
                )
            } else if (hasVideo && (height == null || height >= 144)) {
                val isWatermarked = platform == Platform.TIKTOK &&
                    (formatId.contains("watermark", ignoreCase = true) ||
                        formatNote.contains("watermarked", ignoreCase = true))
                if (isWatermarked) continue

                val vCodec = parseVideoCodec(vcodecRaw)
                val aCodec = if (hasAudio) parseAudioCodec(acodecRaw, ext) else null
                val requiresMerge = !hasAudio
                val selector = if (requiresMerge) "$formatId+bestaudio/best" else formatId
                val estimatedSize = if (requiresMerge && size != null) size + bestAudioSize else size
                val resLabel = when {
                    height == null -> "Original"
                    height >= 4320 -> "4320p (8K)"
                    height >= 2160 -> "2160p (4K)"
                    height >= 1440 -> "1440p (2K)"
                    else -> "${height}p"
                }

                videoFormats += MediaFormat(
                    formatId = formatId,
                    label = resLabel,
                    resolutionHeight = height,
                    fps = fps,
                    videoCodec = vCodec,
                    audioCodec = aCodec,
                    container = if (requiresMerge && ext == "WEBM") "MKV" else ext,
                    fileSizeBytes = estimatedSize,
                    isAudioOnly = false,
                    isNoWatermark = platform == Platform.TIKTOK,
                    requiresMerge = requiresMerge,
                    ytDlpSelector = selector,
                )
            }
        }

        val sortedVideos = videoFormats
            .sortedWith(
                compareByDescending<MediaFormat> { it.resolutionHeight ?: Int.MAX_VALUE }
                    .thenByDescending { it.fps ?: 0 }
                    .thenByDescending { it.fileSizeBytes ?: 0L },
            )
            .distinctBy { Triple(it.resolutionHeight, it.fps, it.videoCodec) }

        val topVideo = sortedVideos.firstOrNull()
        val bestOriginal = MediaFormat(
            formatId = "best_original",
            label = if (topVideo?.resolutionHeight != null) {
                "Best / Original (${topVideo.label})"
            } else {
                "Best / Original"
            },
            resolutionHeight = topVideo?.resolutionHeight,
            fps = topVideo?.fps,
            videoCodec = topVideo?.videoCodec ?: VideoCodec.H264,
            audioCodec = topVideo?.audioCodec ?: AudioCodec.AAC,
            container = topVideo?.container ?: "MP4",
            fileSizeBytes = topVideo?.fileSizeBytes,
            isAudioOnly = false,
            isBestOriginal = true,
            isNoWatermark = platform == Platform.TIKTOK,
            requiresMerge = true,
            ytDlpSelector = "bestvideo*+bestaudio/best",
        )

        val audioPresets = listOf(
            MediaFormat(
                formatId = "audio_mp3",
                label = "Audio Only (MP3 320k)",
                resolutionHeight = null,
                fps = null,
                videoCodec = null,
                audioCodec = AudioCodec.MP3,
                container = "MP3",
                fileSizeBytes = audioFormats.firstOrNull()?.fileSizeBytes,
                isAudioOnly = true,
                ytDlpSelector = "bestaudio/best",
            ),
            MediaFormat(
                formatId = "audio_m4a",
                label = "Audio Only (M4A)",
                resolutionHeight = null,
                fps = null,
                videoCodec = null,
                audioCodec = AudioCodec.AAC,
                container = "M4A",
                fileSizeBytes = audioFormats.firstOrNull { it.audioCodec == AudioCodec.AAC }?.fileSizeBytes,
                isAudioOnly = true,
                ytDlpSelector = "bestaudio[ext=m4a]/bestaudio/best",
            ),
            MediaFormat(
                formatId = "audio_opus",
                label = "Audio Only (Opus)",
                resolutionHeight = null,
                fps = null,
                videoCodec = null,
                audioCodec = AudioCodec.OPUS,
                container = "OPUS",
                fileSizeBytes = audioFormats.firstOrNull { it.audioCodec == AudioCodec.OPUS }?.fileSizeBytes,
                isAudioOnly = true,
                ytDlpSelector = "bestaudio[acodec*=opus]/bestaudio/best",
            ),
        )

        return listOf(bestOriginal) + sortedVideos + audioPresets
    }

    private fun defaultBatchFormats(platform: Platform): List<MediaFormat> = listOf(
        MediaFormat(
            formatId = "best_original",
            label = "Best / Original",
            resolutionHeight = null,
            fps = null,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC,
            container = "MP4",
            fileSizeBytes = null,
            isAudioOnly = false,
            isBestOriginal = true,
            isNoWatermark = platform == Platform.TIKTOK,
            requiresMerge = true,
            ytDlpSelector = "bestvideo*+bestaudio/best",
        ),
        MediaFormat(
            formatId = "fhd_1080",
            label = "1080p",
            resolutionHeight = 1080,
            fps = 60,
            videoCodec = VideoCodec.H264,
            audioCodec = AudioCodec.AAC,
            container = "MP4",
            fileSizeBytes = null,
            isAudioOnly = false,
            isNoWatermark = platform == Platform.TIKTOK,
            requiresMerge = true,
            ytDlpSelector = "bestvideo[height<=1080]+bestaudio/best[height<=1080]",
        ),
        MediaFormat(
            formatId = "audio_mp3",
            label = "Audio Only (MP3)",
            resolutionHeight = null,
            fps = null,
            videoCodec = null,
            audioCodec = AudioCodec.MP3,
            container = "MP3",
            fileSizeBytes = null,
            isAudioOnly = true,
            ytDlpSelector = "bestaudio/best",
        ),
    )

    private fun parseVideoCodec(raw: String): VideoCodec {
        val lower = raw.lowercase()
        return when {
            lower.startsWith("av01") || lower.contains("av1") -> VideoCodec.AV1
            lower.startsWith("vp9") || lower.startsWith("vp09") -> VideoCodec.VP9
            lower.startsWith("hev1") || lower.startsWith("hvc1") || lower.contains("hevc") -> VideoCodec.HEVC
            lower.startsWith("avc") || lower.contains("h264") -> VideoCodec.H264
            else -> VideoCodec.UNKNOWN
        }
    }

    private fun parseAudioCodec(raw: String, ext: String): AudioCodec {
        val lower = raw.lowercase()
        return when {
            lower.contains("opus") -> AudioCodec.OPUS
            lower.contains("mp3") || ext.equals("MP3", ignoreCase = true) -> AudioCodec.MP3
            lower.contains("mp4a") || lower.contains("aac") -> AudioCodec.AAC
            else -> AudioCodec.UNKNOWN
        }
    }
}
