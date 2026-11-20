package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.GeneratedClipEntity
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.ViceSectionCard
import com.example.ui.components.VideoPreviewDialog
import com.example.ui.components.formatTimeMs
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMint
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViceObsidian
import com.example.ui.theme.ViceOutline
import com.example.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToClipMaker: () -> Unit,
    onNavigateToEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var previewClip by remember { mutableStateOf<GeneratedClipEntity?>(null) }

    previewClip?.let { clip ->
        val path = clip.outputFilePath
        if (!path.isNullOrBlank()) {
            VideoPreviewDialog(
                title = clip.title,
                filePathOrUri = path,
                isVertical = clip.isVerticalShorts,
                onDismiss = { previewClip = null }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.statusFeedback?.let { msg ->
            item {
                FeedbackBanner(
                    message = msg,
                    isError = state.isErrorFeedback,
                    onDismiss = { viewModel.clearFeedback() }
                )
            }
        }

        // Studio Hero Card
        item {
            ViceSectionCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_timeline_art_1795168158154),
                        contentDescription = "Neon studio timeline",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ViceObsidian.copy(alpha = 0.25f),
                                        ViceObsidian.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "VICE STUDIO CONTROL",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                        Text(
                            text = "Real Media3 9:16 MP4 rendering, subtitle burn-in & SEO automation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonPrimaryButton(
                        text = "YouTube Clip AI",
                        icon = Icons.Filled.MovieCreation,
                        onClick = onNavigateToClipMaker,
                        accentColor = NeonPink,
                        testTag = "dash_open_clip_maker_btn",
                        modifier = Modifier.weight(1f)
                    )
                    NeonPrimaryButton(
                        text = "Phone Editor",
                        icon = Icons.Filled.ContentCut,
                        onClick = onNavigateToEditor,
                        accentColor = NeonCyan,
                        testTag = "dash_open_editor_btn",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ViceSectionCard(
                    modifier = Modifier.weight(1f),
                    borderColor = NeonCyan.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "TOTAL CLIPS CREATED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.totalClipsCreated}",
                        style = MaterialTheme.typography.displaySmall,
                        color = NeonCyan,
                        modifier = Modifier.testTag("total_clips_count")
                    )
                }
                ViceSectionCard(
                    modifier = Modifier.weight(1f),
                    borderColor = NeonPink.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "SAVED DOWNLOADS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.totalDownloads}",
                        style = MaterialTheme.typography.displaySmall,
                        color = NeonPink,
                        modifier = Modifier.testTag("total_downloads_count")
                    )
                }
            }
        }

        // Processing Progress
        if (state.activeProcessingClips.isNotEmpty()) {
            item {
                ViceSectionCard(borderColor = NeonAmber.copy(alpha = 0.65f)) {
                    Text(
                        text = "ACTIVE PROCESSING PROGRESS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonAmber
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.activeProcessingClips.forEach { clip ->
                        Text(
                            text = "${clip.title} (${clip.progressPercent}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { clip.progressPercent / 100f },
                            color = NeonCyan,
                            trackColor = ViceOutline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Recent Projects
        item {
            Text(
                text = "RECENT PROJECTS (${state.recentProjects.size})",
                style = MaterialTheme.typography.titleMedium,
                color = NeonPurple
            )
        }
        if (state.recentProjects.isEmpty()) {
            item {
                ViceSectionCard {
                    Text(
                        text = "No projects yet. Paste a YouTube URL in Clip AI or upload a video from your phone in Editor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(state.recentProjects, key = { "proj_${it.id}" }) { project ->
                ViceSectionCard(borderColor = ViceOutline) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VideoFile,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(project.channelName ?: "Creator Source")
                                    if (project.durationMs > 0L) {
                                        append(" • ${formatTimeMs(project.durationMs)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        StatusPill(
                            label = project.status,
                            color = if (project.status == "ERROR") NeonRed else NeonMint
                        )
                        IconButton(
                            onClick = { viewModel.deleteProject(project) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete project",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Download & Export History with Real Clip Preview
        item {
            Text(
                text = "EXPORTED & DOWNLOAD HISTORY (${state.downloadHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                color = NeonCyan
            )
        }
        if (state.downloadHistory.isEmpty()) {
            item {
                ViceSectionCard {
                    Text(
                        text = "Rendered MP4 clips will appear here for instant ExoPlayer preview, saving to Movies, and sharing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(state.downloadHistory, key = { "clip_${it.id}" }) { clip ->
                ViceSectionCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = clip.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${formatTimeMs(clip.startMs)} - ${formatTimeMs(clip.endMs)} • ${clip.fileSizeBytes / 1024} KB • ${if (clip.isVerticalShorts) "9:16 Shorts" else "Original"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        StatusPill(
                            label = if (clip.savedToGallery) "SAVED" else "MP4 READY",
                            color = if (clip.savedToGallery) NeonMint else NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { previewClip = clip },
                            enabled = !clip.outputFilePath.isNullOrBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("preview_clip_${clip.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Preview")
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveClipToGallery(clip) },
                            enabled = !clip.outputFilePath.isNullOrBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("save_clip_${clip.id}")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (clip.savedToGallery) "Saved" else "Save")
                        }
                        IconButton(
                            onClick = { viewModel.shareClip(clip) },
                            enabled = !clip.outputFilePath.isNullOrBlank(),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share clip", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = { viewModel.deleteClip(clip) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete clip", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}
