package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.FirebaseAiNoticeBanner
import com.example.ui.components.NeonPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.ViceSectionCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMint
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViceObsidian
import com.example.viewmodel.SeoViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeoStudioScreen(
    viewModel: SeoViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPresets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    fun copyText(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    val vibes = listOf("Viral Gaming", "Hype Trailer", "Funny Fail", "Pro Tips")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("seo_studio_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FirebaseAiNoticeBanner()
        }

        state.errorMessage?.let { err ->
            item {
                FeedbackBanner(
                    message = err,
                    isError = true,
                    onDismiss = { viewModel.onTopicChanged(state.topicInput) }
                )
            }
        }

        item {
            ViceSectionCard(borderColor = NeonPurple.copy(alpha = 0.55f)) {
                Text(
                    text = "AI YOUTUBE SEO GENERATOR",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generate Shorts titles, long descriptions, hashtags, search tags, and pinned comments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.topicInput,
                    onValueChange = viewModel::onTopicChanged,
                    label = { Text("Clip Topic / Highlight Moment") },
                    placeholder = { Text("e.g., Midnight Neon Helicopter Stunt Clutch") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seo_topic_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CONTENT VIBE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vibes.forEach { vibe ->
                        val selected = state.selectedVibe == vibe
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onVibeSelected(vibe) },
                            label = { Text(vibe) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPink,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("seo_vibe_${vibe.lowercase().replace(" ", "_")}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "TARGET SHORTS DURATION: ${state.durationSec}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan
                )
                Slider(
                    value = state.durationSec.toFloat(),
                    onValueChange = { viewModel.onDurationChanged(it.roundToInt()) },
                    valueRange = 15f..60f,
                    colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonCyan)
                )
                Spacer(modifier = Modifier.height(8.dp))
                NeonPrimaryButton(
                    text = if (state.isGenerating) "Generating SEO Pack..." else "Generate YouTube SEO Pack",
                    icon = Icons.Filled.AutoAwesome,
                    onClick = { viewModel.generateSeo() },
                    enabled = !state.isGenerating,
                    accentColor = NeonPink,
                    testTag = "generate_seo_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        state.currentResult?.let { seo ->
            item {
                ViceSectionCard(borderColor = NeonCyan.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATCHY SHORTS TITLES",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan
                        )
                        StatusPill(label = "SAMPLE AI", color = NeonPink)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    seo.titles.forEachIndexed { idx, title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. $title",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { copyText("Title", title) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy title", tint = NeonCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SeoCopyBlock(
                        label = "LONG DESCRIPTION",
                        value = seo.description,
                        onCopy = { copyText("Description", seo.description) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SeoCopyBlock(
                        label = "RELEVANT HASHTAGS",
                        value = seo.hashtags.joinToString(" "),
                        onCopy = { copyText("Hashtags", seo.hashtags.joinToString(" ")) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SeoCopyBlock(
                        label = "SEARCH TAGS",
                        value = seo.searchTags.joinToString(", "),
                        onCopy = { copyText("Search Tags", seo.searchTags.joinToString(", ")) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SeoCopyBlock(
                        label = "PINNED COMMENT",
                        value = seo.pinnedComment,
                        onCopy = { copyText("Pinned Comment", seo.pinnedComment) }
                    )
                }
            }
        }

        if (savedPresets.isNotEmpty()) {
            item {
                Text(
                    text = "SAVED SEO HISTORY (${savedPresets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonPurple
                )
            }
            items(savedPresets, key = { "seo_${it.id}" }) { preset ->
                ViceSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.topic,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${preset.vibe} • ${preset.hashtags}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                        IconButton(
                            onClick = {
                                copyText(
                                    "Full SEO Pack",
                                    "${preset.titlesJoined.replace("||", "\n")}\n\n${preset.description}\n\n${preset.hashtags}\n\nTags: ${preset.searchTags}\n\nPinned: ${preset.pinnedComment}"
                                )
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy SEO preset", tint = NeonMint)
                        }
                        IconButton(
                            onClick = { viewModel.deletePreset(preset.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete preset", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeoCopyBlock(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = NeonPink
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
