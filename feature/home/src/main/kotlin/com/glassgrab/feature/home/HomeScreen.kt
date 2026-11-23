package com.glassgrab.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glassgrab.core.common.Formatters
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.design.components.GlassBadge
import com.glassgrab.core.design.components.GlassCard
import com.glassgrab.core.design.components.GlassPrimaryButton
import com.glassgrab.core.design.components.GlassShimmerSkeleton
import com.glassgrab.core.design.tokens.GlassColors
import com.glassgrab.core.design.tokens.GlassRadii
import com.glassgrab.core.design.tokens.GlassSpacing
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform

@Composable
fun HomeRoute(
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
    onOpenFormats: (MediaItemInfo) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        val clipText = clipboard.getText()?.text
        viewModel.onIntent(HomeIntent.ClipboardDetected(clipText))
    }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.onIntent(HomeIntent.AnalyzeSharedUrl(sharedUrl))
            onSharedUrlConsumed()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is HomeEffect.OpenFormatPicker) {
                onOpenFormats(effect.media)
            }
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onPasteClick = {
            val clipText = clipboard.getText()?.text.orEmpty()
            val url = Formatters.extractFirstUrl(clipText) ?: clipText
            viewModel.onIntent(HomeIntent.UrlChanged(url))
        },
        onOpenAnalyzed = onOpenFormats,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onPasteClick: () -> Unit,
    onOpenAnalyzed: (MediaItemInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GlassSpacing.md, vertical = GlassSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(GlassSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
            Text(
                text = "GlassGrab",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Original quality, zero watermarks, zero trackers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = state.detectedClipboardUrl != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            state.detectedClipboardUrl?.let { clipUrl ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Link detected in clipboard", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = clipUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onIntent(HomeIntent.UseClipboardUrl) }) {
                            Text("Grab")
                        }
                        IconButton(onClick = { onIntent(HomeIntent.DismissClipboardBanner) }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Dismiss clipboard suggestion")
                        }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.sm)) {
                OutlinedTextField(
                    value = state.urlInput,
                    onValueChange = { onIntent(HomeIntent.UrlChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    shape = RoundedCornerShape(GlassRadii.card),
                    singleLine = true,
                    label = { Text("Paste video, playlist, or profile link") },
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                    trailingIcon = {
                        if (state.urlInput.isNotEmpty()) {
                            IconButton(onClick = { onIntent(HomeIntent.ClearInput) }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear link")
                            }
                        } else {
                            IconButton(onClick = onPasteClick) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste from clipboard")
                            }
                        }
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs),
                ) {
                    GlassPrimaryButton(
                        text = if (state.isAnalyzing) "Analyzing..." else "Analyze Link",
                        icon = Icons.Rounded.Download,
                        enabled = state.urlInput.isNotBlank() && !state.isAnalyzing,
                        onClick = { onIntent(HomeIntent.AnalyzeClicked) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("analyze_button"),
                    )
                }
            }
        }

        if (state.isAnalyzing) {
            GlassShimmerSkeleton(height = 120.dp)
        }

        state.error?.let { error ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                    Text(
                        text = friendlyErrorTitle(error),
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassColors.CoralError,
                    )
                    Text(
                        text = friendlyErrorDescription(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (error !is GlassError.DrmNotSupported) {
                        TextButton(onClick = { onIntent(HomeIntent.RetryAnalyze) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Text(" Retry")
                        }
                    }
                }
            }
        }

        state.analyzedMedia?.let { media ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenAnalyzed(media) },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xxs)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                        GlassBadge(media.platform.displayName)
                        if (media.platform == Platform.TIKTOK) {
                            GlassBadge(
                                text = "No Watermark",
                                containerColor = GlassColors.EmeraldSuccess.copy(alpha = 0.22f),
                                contentColor = GlassColors.EmeraldSuccess,
                            )
                        }
                        if (media.isPlaylist) {
                            GlassBadge("${media.playlistEntries.size} items")
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
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs)) {
                Text("Supported Platforms", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(GlassSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(GlassSpacing.xs),
                ) {
                    Platform.entries.filter { it != Platform.GENERIC }.forEach { platform ->
                        GlassBadge(platform.displayName)
                    }
                    GlassBadge("1,000+ yt-dlp sites")
                }
                Spacer(modifier = Modifier.height(GlassSpacing.xxs))
                Text(
                    text = "Tip: Share any link directly from TikTok, YouTube, Instagram, or X to GlassGrab. Please respect content creators' rights; DRM-protected services are not supported.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

private fun friendlyErrorTitle(error: GlassError): String = when (error) {
    is GlassError.Offline -> "You appear to be offline"
    is GlassError.LowStorage -> "Not enough free storage"
    is GlassError.PrivateOrRemoved -> "Video unavailable"
    is GlassError.AgeRestricted -> "Age-restricted content"
    is GlassError.GeoBlocked -> "Region-restricted video"
    is GlassError.DrmNotSupported -> "${error.platformName} is not supported"
    is GlassError.UnsupportedUrl -> "Unsupported link"
    is GlassError.TikTokExtractionFailed -> "TikTok extraction failed"
    is GlassError.EngineUpdateFailed -> "Engine update failed"
    is GlassError.PermissionDenied -> "Permission required"
    is GlassError.Timeout -> "Request timed out"
    is GlassError.Cancelled -> "Cancelled"
    is GlassError.Unknown -> "Could not analyze link"
}

private fun friendlyErrorDescription(error: GlassError): String = when (error) {
    is GlassError.Offline -> "Check your Wi-Fi or mobile connection and tap Retry."
    is GlassError.LowStorage -> "Free up space on your device and try again."
    is GlassError.PrivateOrRemoved -> "This post may be private, deleted, or require a login."
    is GlassError.AgeRestricted -> "The source platform restricts this video to signed-in adult accounts."
    is GlassError.GeoBlocked -> "The uploader has restricted this video in your region."
    is GlassError.DrmNotSupported -> "GlassGrab does not bypass DRM protection on streaming services."
    is GlassError.UnsupportedUrl -> "Paste a valid http:// or https:// link from a supported platform."
    is GlassError.TikTokExtractionFailed -> "Both primary no-watermark API (${error.primaryReason}) and web fallback (${error.fallbackReason}) failed. Try updating the engine in Settings."
    is GlassError.EngineUpdateFailed -> "Could not reach the yt-dlp update server. Try again later."
    is GlassError.PermissionDenied -> "Grant the requested notification or storage access to continue."
    is GlassError.Timeout -> "The server took too long to respond. Tap Retry."
    is GlassError.Cancelled -> "The operation was cancelled."
    is GlassError.Unknown -> error.technicalDetail ?: "An unexpected error occurred while inspecting this link."
}
