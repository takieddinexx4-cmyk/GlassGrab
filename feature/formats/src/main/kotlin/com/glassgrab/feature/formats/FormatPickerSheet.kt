package com.glassgrab.feature.formats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.GlassPrimaryButton
import com.glassgrab.core.design.tokens.GlassColors
import com.glassgrab.core.design.tokens.GlassRadii
import com.glassgrab.core.design.tokens.GlassSpacing
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.MediaFormat
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatPickerBottomSheet(
    media: MediaItemInfo,
    onDismiss: () -> Unit,
    onStartDownload: (List<DownloadRequest>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var audioOnlyTab by remember { mutableStateOf(false) }
    var includeSubtitles by remember { mutableStateOf(false) }
    var includeThumbnail by remember { mutableStateOf(false) }
    var selectedIds by remember(media.id) {
        mutableStateOf(media.playlistEntries.map { it.id }.toSet())
    }

    val visibleFormats = remember(media.formats, audioOnlyTab) {
        media.formats.filter { it.isAudioOnly == audioOnlyTab || (it.isBestOriginal && !audioOnlyTab) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = GlassRadii.sheet, topEnd = GlassRadii.sheet),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!media.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                        GlassBadge(media.platform.displayName)
                        if (media.platform == Platform.TIKTOK) {
                            GlassBadge(
                                text = "No watermark",
                                containerColor = GlassColors.EmeraldSuccess.copy(alpha = 0.22f),
                                contentColor = GlassColors.EmeraldSuccess,
                            )
                        }
                    }
                    Text(media.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${media.author} • ${Formatters.formatDuration(media.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (media.isPlaylist && media.playlistEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Selected ${selectedIds.size} of ${media.playlistEntries.size}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(
                        onClick = {
                            selectedIds = if (selectedIds.size == media.playlistEntries.size) {
                                emptySet()
                            } else {
                                media.playlistEntries.map { it.id }.toSet()
                            }
                        },
                    ) {
                        Text(if (selectedIds.size == media.playlistEntries.size) "Deselect All" else "Select All")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                FilterChip(
                    selected = !audioOnlyTab,
                    onClick = { audioOnlyTab = false },
                    label = { Text("Video (144p–8K)") },
                    leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = audioOnlyTab,
                    onClick = { audioOnlyTab = true },
                    label = { Text("Audio Only") },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text("Subtitles", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeSubtitles, onCheckedChange = { includeSubtitles = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text("Thumbnail", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeThumbnail, onCheckedChange = { includeThumbnail = it })
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (media.isPlaylist) 340.dp else 300.dp),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs),
            ) {
                if (media.isPlaylist && media.playlistEntries.isNotEmpty()) {
                    items(media.playlistEntries, key = { it.id }) { entry ->
                        val checked = entry.id in selectedIds
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = GlassSpacing.sm,
                            onClick = {
                                selectedIds = if (checked) selectedIds - entry.id else selectedIds + entry.id
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs),
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        Formatters.formatDuration(entry.durationSeconds),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(visibleFormats, key = { it.formatId }) { format ->
                        FormatRowCard(
                            format = format,
                            onClick = {
                                onStartDownload(
                                    listOf(
                                        DownloadRequest(
                                            url = media.url,
                                            title = media.title,
                                            author = media.author,
                                            thumbnailUrl = media.thumbnailUrl,
                                            platform = media.platform,
                                            format = format,
                                            includeSubtitles = includeSubtitles,
                                            includeThumbnail = includeThumbnail,
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            if (media.isPlaylist && media.playlistEntries.isNotEmpty()) {
                val chosenFormat = visibleFormats.firstOrNull() ?: media.formats.first()
                GlassPrimaryButton(
                    text = "Download ${selectedIds.size} Selected (${chosenFormat.label})",
                    icon = Icons.Rounded.Download,
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        val batch = media.playlistEntries
                            .filter { it.id in selectedIds }
                            .map { entry ->
                                DownloadRequest(
                                    url = entry.url,
                                    title = entry.title,
                                    author = entry.author,
                                    thumbnailUrl = entry.thumbnailUrl,
                                    platform = media.platform,
                                    format = chosenFormat,
                                    includeSubtitles = includeSubtitles,
                                    includeThumbnail = includeThumbnail,
                                )
                            }
                        onStartDownload(batch)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(GlassSpacing.lg))
        }
    }
}

@Composable
private fun FormatRowCard(
    format: MediaFormat,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = GlassSpacing.sm,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(format.label, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
                    GlassBadge(format.container)
                    format.videoCodec?.let { GlassBadge(it.label) }
                    format.audioCodec?.let { if (format.isAudioOnly) GlassBadge(it.label) }
                    format.fps?.takeIf { it > 0 }?.let { GlassBadge("${it}fps") }
                    if (format.isNoWatermark) {
                        GlassBadge(
                            "No watermark",
                            containerColor = GlassColors.EmeraldSuccess.copy(alpha = 0.2f),
                            contentColor = GlassColors.EmeraldSuccess,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatFileSize(format.fileSizeBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(Icons.Rounded.Download, contentDescription = "Download ${format.label}", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
