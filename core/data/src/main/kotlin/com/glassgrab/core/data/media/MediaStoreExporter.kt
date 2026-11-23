package com.glassgrab.core.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.glassgrab.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ExportedMedia(
    val contentUri: String,
    val filePath: String?,
    val sizeBytes: Long,
)

@Singleton
class MediaStoreExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun exportToGallery(sourceFile: File, isAudioOnly: Boolean): ExportedMedia = withContext(ioDispatcher) {
        val ext = sourceFile.extension.lowercase().ifBlank { if (isAudioOnly) "m4a" else "mp4" }
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (isAudioOnly) "audio/mp4" else "video/mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = if (isAudioOnly) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val relativePath = if (isAudioOnly) {
                "${Environment.DIRECTORY_MUSIC}/GlassGrab"
            } else {
                "${Environment.DIRECTORY_MOVIES}/GlassGrab"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(collection, values)
                ?: error("Failed to create MediaStore entry for ${sourceFile.name}")
            resolver.openOutputStream(itemUri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Failed to open output stream for $itemUri")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            val size = sourceFile.length()
            sourceFile.delete()
            ExportedMedia(contentUri = itemUri.toString(), filePath = null, sizeBytes = size)
        } else {
            val baseDir = context.getExternalFilesDir(
                if (isAudioOnly) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES,
            ) ?: context.filesDir
            val targetDir = File(baseDir, "GlassGrab").apply { mkdirs() }
            val dest = File(targetDir, sourceFile.name)
            sourceFile.copyTo(dest, overwrite = true)
            sourceFile.delete()
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
            ExportedMedia(
                contentUri = Uri.fromFile(dest).toString(),
                filePath = dest.absolutePath,
                sizeBytes = dest.length(),
            )
        }
    }

    suspend fun deleteExported(uriString: String?, filePath: String?) = withContext(ioDispatcher) {
        if (!uriString.isNullOrBlank() && uriString.startsWith("content://")) {
            runCatching { context.contentResolver.delete(Uri.parse(uriString), null, null) }
        }
        if (!filePath.isNullOrBlank()) {
            runCatching { File(filePath).delete() }
        }
    }
}
