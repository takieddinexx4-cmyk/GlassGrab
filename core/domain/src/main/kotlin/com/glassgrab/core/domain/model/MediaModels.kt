package com.glassgrab.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Platform(val displayName: String) {
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    X("X"),
    YOUTUBE("YouTube"),
    REDDIT("Reddit"),
    PINTEREST("Pinterest"),
    SNAPCHAT("Snapchat"),
    VIMEO("Vimeo"),
    DAILYMOTION("Dailymotion"),
    TWITCH("Twitch"),
    GENERIC("Web"),
}

@Serializable
enum class VideoCodec(val label: String) {
    H264("H.264"),
    VP9("VP9"),
    AV1("AV1"),
    HEVC("H.265"),
    UNKNOWN("Video"),
}

@Serializable
enum class AudioCodec(val label: String) {
    AAC("M4A/AAC"),
    OPUS("Opus"),
    MP3("MP3"),
    UNKNOWN("Audio"),
}

@Serializable
data class MediaFormat(
    val formatId: String,
    val label: String,
    val resolutionHeight: Int?,
    val fps: Int?,
    val videoCodec: VideoCodec?,
    val audioCodec: AudioCodec?,
    val container: String,
    val fileSizeBytes: Long?,
    val isAudioOnly: Boolean,
    val isBestOriginal: Boolean = false,
    val isNoWatermark: Boolean = false,
    val requiresMerge: Boolean = false,
    val ytDlpSelector: String,
)

@Serializable
data class MediaItemInfo(
    val id: String,
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val platform: Platform,
    val formats: List<MediaFormat>,
    val hasSubtitles: Boolean = false,
    val subtitleLanguages: List<String> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistEntries: List<PlaylistEntry> = emptyList(),
)

@Serializable
data class PlaylistEntry(
    val id: String,
    val url: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
)
