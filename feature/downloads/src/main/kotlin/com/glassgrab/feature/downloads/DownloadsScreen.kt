package com.glassgrab.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.WavyProgressIndicator
import com.glassgrab.core.design.tokens.GlassColors
import com.glassgrab.core.design.tokens.GlassSpacing
import com.glassgrab.core.domain.model.DownloadStatus
import com.glassgrab.core.domain.model.DownloadTask

@Composable
fun DownloadsRoute(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val tasks by viewModel.activeDownloads.collectAsStateWithLifecycle()
    DownloadsScreen(
        tasks = tasks,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onRetry = viewModel::retry,
        onCancel = viewModel::cancel,
        modifier = modifier,
    )
}

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = GlassSpacing.md, vertical = GlassSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.md),
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)

        if (tasks.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text("No active downloads", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Paste any video link on Home or share from another app to start downloading.",
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
                items(tasks, key = { it.id }) { task ->
                    DownloadItemCard(
                        task = task,
                        onPause = { onPause(task.id) },
                        onResume = { onResume(task.id) },
                        onRetry = { onRetry(task.id) },
                        onCancel = { onCancel(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
                        GlassBadge(task.platform.displayName)
                        GlassBadge(task.formatLabel)
                        GlassBadge(task.status.name)
                    }
                }
                Row {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.MERGING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause download")
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume download")
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry download")
                            }
                        }
                        else -> Unit
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel download")
                    }
                }
            }

            WavyProgressIndicator(progress = task.progress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(task.progress * 100).toInt()}% • ${Formatters.formatFileSize(task.downloadedBytes)} / ${Formatters.formatFileSize(task.totalBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "ETA ${Formatters.formatEta(task.etaSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            task.errorMessage?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassColors.CoralError,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
