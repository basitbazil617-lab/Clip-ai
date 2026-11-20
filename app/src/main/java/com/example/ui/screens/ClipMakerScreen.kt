package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.GeneratedClipEntity
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.FirebaseAiNoticeBanner
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViceOutline
import com.example.viewmodel.ClipMakerViewModel

@Composable
fun ClipMakerScreen(
    viewModel: ClipMakerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clips by viewModel.projectClips.collectAsStateWithLifecycle()
    var previewClip by remember { mutableStateOf<GeneratedClipEntity?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachOwnedLocalVideo(uri)
        }
    }

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
            .testTag("clip_maker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.actionMessage?.let { msg ->
            item {
                FeedbackBanner(
                    message = msg,
                    isError = state.isActionError,
                    onDismiss = { viewModel.clearMessage() }
                )
            }
        }

        // Step 1: YouTube URL Validation & oEmbed Metadata
        item {
            ViceSectionCard(borderColor = NeonCyan.copy(alpha = 0.55f)) {
                Text(
                    text = "1. YOUTUBE URL VALIDATOR",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paste a YouTube URL you own or have permission to edit. We validate the URL and fetch public oEmbed metadata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.youtubeUrl,
                    onValueChange = viewModel::onYoutubeUrlChanged,
                    label = { Text("YouTube Video URL") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = NeonCyan) },
                    isError = state.urlError != null,
                    supportingText = state.urlError?.let { err -> { Text(err, color = NeonRed) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_url_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                NeonPrimaryButton(
                    text = if (state.isFetchingMetadata) "Validating..." else "Validate & Fetch Info",
                    icon = Icons.Filled.CheckCircle,
                    onClick = { viewModel.verifyYoutubeUrlAndFetchMetadata() },
                    enabled = !state.isFetchingMetadata,
                    accentColor = NeonCyan,
                    testTag = "verify_youtube_url_button",
                    modifier = Modifier.fillMaxWidth()
                )

                state.metadataError?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonRed
                    )
                }

                state.oEmbedInfo?.let { info ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ViceOutline.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (info.thumbnailUrl.isNotBlank()) {
                                AsyncImage(
                                    model = info.thumbnailUrl,
                                    contentDescription = "YouTube thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 104.dp, height = 64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Text(
                                    text = "Channel: ${info.authorName} • ID: ${state.validatedVideoId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonMint
                                )
                            }
                        }
                    }
                }
            }
        }

        // Step 2: Copyright & Authorized Video Access
        item {
            ViceSectionCard(borderColor = NeonPink.copy(alpha = 0.55f)) {
                Text(
                    text = "2. COPYRIGHT & AUTHORIZED SOURCE",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPink
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "We never bypass YouTube DRM or download restrictions. Attach your owned source MP4 or connect your own authorized processing backend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setPermissionAttested(!state.permissionAttested) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.permissionAttested,
                        onCheckedChange = viewModel::setPermissionAttested,
                        colors = CheckboxDefaults.colors(checkedColor = NeonMint),
                        modifier = Modifier.testTag("permission_attestation_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = if (state.permissionAttested) NeonMint else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "I confirm I own this video or hold explicit permission to clip and edit it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (state.authorizedSourceUri != null) "Change Owned Phone Video" else "Attach Owned MP4 from Phone",
                    icon = Icons.Filled.VideoLibrary,
                    onClick = {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    accentColor = NeonPurple,
                    testTag = "attach_owned_video_button",
                    modifier = Modifier.fillMaxWidth()
                )

                state.authorizedSourceLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusPill(label = "SOURCE: $label", color = NeonMint)
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.backendEndpointUrl,
                    onValueChange = viewModel::onBackendEndpointChanged,
                    label = { Text("Authorized Processing Backend URL (Optional)") },
                    placeholder = { Text("https://your-studio-api.example.com/clip-source") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("authorized_backend_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.fetchFromAuthorizedBackend() },
                    enabled = !state.isFetchingBackendStream,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("fetch_backend_button")
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isFetchingBackendStream) "Fetching Authorized MP4 (${state.backendProgress}%)..."
                        else "Fetch Authorized MP4 from Backend"
                    )
                }

                if (state.isFetchingBackendStream) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.backendProgress / 100f },
                        color = NeonCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                state.backendError?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, style = MaterialTheme.typography.bodyMedium, color = NeonRed)
                }
            }
        }

        // Step 3: AI Scene Detection & 15-60s 9:16 Clip Generation
        item {
            ViceSectionCard(borderColor = NeonPurple.copy(alpha = 0.65f)) {
                Text(
                    text = "3. AI SCENE & HIGHLIGHT DETECTOR",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonPurple
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Detect funny moments, reactions, highlights & exciting scenes into 15–60s vertical 9:16 Shorts clips with editable captions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                FirebaseAiNoticeBanner()
                Spacer(modifier = Modifier.height(12.dp))
                NeonPrimaryButton(
                    text = if (state.isAnalyzingScenes) "Analyzing Scenes..." else "Detect Highlights & Build 9:16 Clips",
                    icon = Icons.Filled.AutoAwesome,
                    onClick = { viewModel.detectHighlightsAndGenerateClips() },
                    enabled = !state.isAnalyzingScenes,
                    accentColor = NeonPink,
                    testTag = "detect_ai_clips_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Generated Clips List
        if (clips.isNotEmpty()) {
            item {
                Text(
                    text = "GENERATED 9:16 SHORTS CLIPS (${clips.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
            }
            items(clips, key = { "maker_clip_${it.id}" }) { clip ->
                GeneratedClipCard(
                    clip = clip,
                    onSubtitleChanged = { viewModel.updateClipSubtitle(clip, it) },
                    onRenderMp4 = { viewModel.renderClipToMp4(clip) },
                    onPreview = { previewClip = clip },
                    onDownload = { viewModel.downloadClipToGallery(clip) },
                    onShare = { viewModel.shareClip(clip) }
                )
            }
        }
    }
}

@Composable
private fun GeneratedClipCard(
    clip: GeneratedClipEntity,
    onSubtitleChanged: (String) -> Unit,
    onRenderMp4: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    var subtitleDraft by remember(clip.id, clip.subtitleText) { mutableStateOf(clip.subtitleText) }

    ViceSectionCard(
        borderColor = when (clip.status) {
            "EXPORTED" -> NeonMint.copy(alpha = 0.7f)
            "ERROR" -> NeonRed.copy(alpha = 0.7f)
            "PROCESSING" -> NeonAmber.copy(alpha = 0.7f)
            else -> NeonPurple.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(label = clip.category.uppercase(), color = NeonPink)
            StatusPill(
                label = "${formatTimeMs(clip.startMs)} - ${formatTimeMs(clip.endMs)} • 9:16",
                color = NeonCyan
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = clip.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = subtitleDraft,
            onValueChange = {
                subtitleDraft = it
                onSubtitleChanged(it)
            },
            label = { Text("Editable Auto-Subtitle Overlay") },
            singleLine = false,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clip_subtitle_input_${clip.id}")
        )

        if (clip.status == "PROCESSING") {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Media3 Rendering 9:16 MP4: ${clip.progressPercent}%",
                style = MaterialTheme.typography.labelMedium,
                color = NeonAmber
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { clip.progressPercent / 100f },
                color = NeonCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        clip.errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = err, style = MaterialTheme.typography.bodyMedium, color = NeonRed)
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (clip.status != "EXPORTED") {
            NeonPrimaryButton(
                text = if (clip.status == "PROCESSING") "Rendering MP4..." else "Render 9:16 MP4 Clip",
                icon = Icons.Filled.Movie,
                onClick = onRenderMp4,
                enabled = clip.status != "PROCESSING",
                accentColor = NeonCyan,
                testTag = "render_clip_btn_${clip.id}",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Real MP4 Ready: ${clip.fileSizeBytes / 1024} KB",
                style = MaterialTheme.typography.labelMedium,
                color = NeonMint
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
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
                    onClick = onDownload,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (clip.savedToGallery) "Saved" else "Download")
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share MP4", tint = TextPrimary)
                }
            }
        }
    }
}
