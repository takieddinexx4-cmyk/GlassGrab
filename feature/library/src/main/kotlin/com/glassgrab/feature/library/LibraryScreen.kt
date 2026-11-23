package com.glassgrab.feature.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.tokens.GlassRadii
import com.glassgrab.core.design.tokens.GlassSpacing
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.model.LibrarySort

@Composable
fun LibraryRoute(
    onPlayMedia: (uri: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LibraryScreen(
        state = state,
        onQueryChange = viewModel::onQueryChanged,
        onSortChange = viewModel::onSortChanged,
        onPlay = { task ->
            val uri = task.outputUri ?: return@LibraryScreen
            onPlayMedia(uri, task.title)
        },
        onShare = { task ->
            val uri = task.outputUri ?: return@LibraryScreen
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (task.isAudioOnly) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, task.title))
        },
        onOpenGallery = { task ->
            val uri = task.outputUri ?: return@LibraryScreen
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uri), if (task.isAudioOnly) "audio/*" else "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(viewIntent) }
        },
        onDelete = { viewModel.deleteItem(it.id) },
        modifier = modifier,
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onPlay: (DownloadTask) -> Unit,
    onShare: (DownloadTask) -> Unit,
    onOpenGallery: (DownloadTask) -> Unit,
    onDelete: (DownloadTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = GlassSpacing.md, vertical = GlassSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(GlassRadii.card),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search downloads...") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
            LibrarySort.entries.forEach { sortOption ->
                FilterChip(
                    selected = state.sort == sortOption,
                    onClick = { onSortChange(sortOption) },
                    label = { Text(sortOption.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        if (state.items.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Saved videos are stored in Movies/GlassGrab via scoped MediaStore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm),
            ) {
                items(state.items, key = { it.id }) { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlay(item) },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(GlassSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!item.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 88.dp, height = 56.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${item.formatLabel} • ${Formatters.formatFileSize(item.totalBytes)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
                                        GlassBadge(item.platform.displayName)
                                        GlassBadge(item.container)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(onClick = { onPlay(item) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play")
                                }
                                IconButton(onClick = { onOpenGallery(item) }) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = "Open in gallery")
                                }
                                IconButton(onClick = { onShare(item) }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share")
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
