package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExoVideoPlayerBox
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportedPreview by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.attachVideoFromPhone(uri)
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) cursor.getString(idx) else null
                        } else null
                    }
            }.getOrNull() ?: "Music Track"
            viewModel.attachMusicTrack(uri, name)
        }
    }

    if (showExportedPreview && state.exportedFilePath != null) {
        VideoPreviewDialog(
            title = "Exported: ${state.clipTitle}",
            filePathOrUri = state.exportedFilePath!!,
            isVertical = state.isVerticalShorts,
            onDismiss = { showExportedPreview = false }
        )
    }

    val effectsList = listOf(
        "NONE" to "Original",
        "NEON_VICE" to "Neon Vice",
        "HIGH_CONTRAST" to "High Contrast",
        "CINEMATIC_ZOOM" to "Cinematic Zoom",
        "FADE_INTRO" to "Punch Intro"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("editor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.errorMessage?.let { err ->
            item {
                FeedbackBanner(
                    message = err,
                    isError = true,
                    onDismiss = { viewModel.clearMessages() }
                )
            }
        }
        state.statusMessage?.let { msg ->
            if (state.errorMessage == null) {
                item {
                    FeedbackBanner(
                        message = msg,
                        isError = false,
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }
        }

        // Source Video Picker & Live Preview
        item {
            ViceSectionCard(borderColor = NeonCyan.copy(alpha = 0.6f)) {
                Text(
                    text = "AI CLIP EDITOR — PHONE STUDIO",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Upload any video from your Android phone, trim, crop 9:16 Shorts, burn in captions, mix licensed music, and export real MP4.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (state.sourceUri == null) "Upload Video from Phone" else "Change Phone Video",
                    icon = Icons.Filled.UploadFile,
                    onClick = {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    accentColor = NeonPink,
                    testTag = "editor_pick_video_button",
                    modifier = Modifier.fillMaxWidth()
                )

                state.sourceUri?.let { uri ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.sourceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(label = state.videoResolution, color = NeonMint)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ExoVideoPlayerBox(
                        mediaUri = uri,
                        isVerticalAspect = false
                    )
                }
            }
        }

        // Trim & Aspect Ratio Controls
        if (state.sourceUri != null && state.totalDurationMs > 0L) {
            item {
                ViceSectionCard(borderColor = NeonPurple.copy(alpha = 0.55f)) {
                    Text(
                        text = "TRIM & 9:16 SHORTS FORMAT",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${formatTimeMs(state.startMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                        Text(
                            text = "Clip Length: ${(state.endMs - state.startMs) / 1000}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonMint
                        )
                        Text(
                            text = "End: ${formatTimeMs(state.endMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonPink
                        )
                    }
                    RangeSlider(
                        value = state.startMs.toFloat()..state.endMs.toFloat(),
                        onValueChange = { range ->
                            viewModel.updateTrimRange(
                                range.start.toLong(),
                                range.endInclusive.toLong()
                            )
                        },
                        valueRange = 0f..state.totalDurationMs.toFloat().coerceAtLeast(1000f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("editor_trim_slider")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Convert to 9:16 Vertical Shorts",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Center-crop and scale for YouTube Shorts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.isVerticalShorts,
                            onCheckedChange = viewModel::setVerticalShorts,
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonCyan),
                            modifier = Modifier.testTag("editor_vertical_switch")
                        )
                    }
                }
            }

            // Text, Subtitles, Transitions & Effects
            item {
                ViceSectionCard(borderColor = NeonPink.copy(alpha = 0.55f)) {
                    Text(
                        text = "SUBTITLES, TEXT & EFFECTS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonPink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.clipTitle,
                        onValueChange = viewModel::updateClipTitle,
                        label = { Text("Export Clip Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.headerOverlayText,
                        onValueChange = viewModel::updateHeaderOverlayText,
                        label = { Text("Top Hook Badge Overlay (Optional)") },
                        placeholder = { Text("e.g., INSANE CLUTCH MOMENT") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.subtitleText,
                        onValueChange = viewModel::updateSubtitleText,
                        label = { Text("Burned-in Subtitles / Caption Text") },
                        placeholder = { Text("e.g., Watch till the final second!") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_subtitle_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "VISUAL EFFECT / TRANSITION",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        effectsList.forEach { (key, label) ->
                            FilterChip(
                                selected = state.effectStyle == key,
                                onClick = { viewModel.selectEffectStyle(key) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = com.example.ui.theme.ViceObsidian
                                )
                            )
                        }
                    }
                }
            }

            // Music & Volume Mixing
            item {
                ViceSectionCard(borderColor = NeonMint.copy(alpha = 0.5f)) {
                    Text(
                        text = "AUDIO VOLUME & LICENSED MUSIC",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonMint
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Original Video Audio: ${(state.videoVolume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Slider(
                        value = state.videoVolume,
                        onValueChange = viewModel::updateVideoVolume,
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { audioPicker.launch(arrayOf("audio/*")) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("editor_pick_music_button")
                        ) {
                            Icon(Icons.Filled.AudioFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.musicUri == null) "Add Licensed / User Music" else state.musicName)
                        }
                        if (state.musicUri != null) {
                            IconButton(
                                onClick = { viewModel.clearMusicTrack() },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove music", tint = TextSecondary)
                            }
                        }
                    }

                    if (state.musicUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Music Volume: ${(state.musicVolume * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonPink
                        )
                        Slider(
                            value = state.musicVolume,
                            onValueChange = viewModel::updateMusicVolume,
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                        )
                    }
                }
            }

            // Export MP4 Section
            item {
                ViceSectionCard(borderColor = NeonCyan.copy(alpha = 0.7f)) {
                    if (state.isExporting) {
                        Text(
                            text = "MEDIA3 TRANSFORMER EXPORTING: ${state.exportProgress}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.exportProgress / 100f },
                            color = NeonCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    NeonPrimaryButton(
                        text = if (state.isExporting) "Exporting MP4 (${state.exportProgress}%)..." else "Export Final MP4 Video",
                        icon = Icons.Filled.Movie,
                        onClick = { viewModel.exportMp4() },
                        enabled = !state.isExporting,
                        accentColor = NeonCyan,
                        testTag = "editor_export_mp4_button",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.exportedFilePath != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Real MP4 Export Ready (${state.exportedFileSizeBytes / 1024} KB)",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonMint
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExportedPreview = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Preview")
                            }
                            OutlinedButton(
                                onClick = { viewModel.saveExportedToMovies() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (state.savedToGallery) "Saved" else "Save MP4")
                            }
                            IconButton(
                                onClick = { viewModel.shareExported() },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share MP4", tint = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
