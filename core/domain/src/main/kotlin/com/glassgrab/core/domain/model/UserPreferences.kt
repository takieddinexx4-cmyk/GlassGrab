package com.glassgrab.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED,
}

@Serializable
enum class AppLanguage(val tag: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية"),
}

@Serializable
enum class DefaultQuality(val label: String, val maxHeight: Int?) {
    BEST_ORIGINAL("Best / Original", null),
    UHD_4K("2160p (4K)", 2160),
    QHD_1440("1440p (2K)", 1440),
    FHD_1080("1080p", 1080),
    HD_720("720p", 720),
    AUDIO_ONLY("Audio Only (Best)", 0),
}

@Serializable
data class UserPreferences(
    val defaultQuality: DefaultQuality = DefaultQuality.BEST_ORIGINAL,
    val downloadFolderName: String = "Movies/GlassGrab",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val maxParallelDownloads: Int = 2,
    val wifiOnly: Boolean = false,
    val autoUpdateEngine: Boolean = true,
    val lastEngineUpdateEpochMs: Long = 0L,
    val engineVersion: String = "Bundled",
)
