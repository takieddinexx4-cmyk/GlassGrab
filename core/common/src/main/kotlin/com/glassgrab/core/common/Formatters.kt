package com.glassgrab.core.common

import java.util.Locale

object Formatters {
    fun formatFileSize(bytes: Long?): String {
        if (bytes == null || bytes <= 0L) return "Unknown size"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatDuration(seconds: Long?): String {
        if (seconds == null || seconds < 0L) return "--:--"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 KB/s"
        val mb = 1024.0 * 1024.0
        return if (bytesPerSec >= mb) {
            String.format(Locale.US, "%.1f MB/s", bytesPerSec / mb)
        } else {
            String.format(Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
        }
    }

    fun formatEta(seconds: Long?): String {
        if (seconds == null || seconds <= 0L) return "--"
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val regex = Regex("https?://[^\s<>"]+", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value?.trimEnd('.', ',', ')', ']', '!')
    }
}
