package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMint
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ViceCard
import com.example.ui.theme.ViceCardElevated
import com.example.ui.theme.ViceObsidian
import com.example.ui.theme.ViceOutline
import java.io.File
import java.util.Locale

@Composable
fun ViceStudioHeader(
    modifier: Modifier = Modifier
) {
    val borderBrush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple, NeonPink))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(ViceObsidian)
            .border(
                width = 1.5.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vice_skyline_banner_1795168157805),
            contentDescription = "Neon twilight palm city studio skyline",
            contentScale = ContentScale.Crop,
            alpha = 0.34f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ViceObsidian.copy(alpha = 0.72f),
                            ViceObsidian.copy(alpha = 0.90f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GTA VI CLIP AI",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("header_brand_title")
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "9:16 SHORTS & MEDIA3 VIDEO ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = NeonPink.copy(alpha = 0.18f),
                border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(NeonPink, NeonCyan))),
                modifier = Modifier.testTag("header_creator_badge")
            ) {
                Text(
                    text = "MADE BY SAHIB",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun FirebaseAiNoticeBanner(
    message: String = "AI features require Firebase AI Logic — currently showing clearly labeled sample AI responses.",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_ai_notice_banner"),
        shape = RoundedCornerShape(14.dp),
        color = NeonAmber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Sample AI Notice",
                tint = NeonAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun FeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (isError) NeonRed else NeonMint
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.Info,
                contentDescription = if (isError) "Error" else "Info",
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss message",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun NeonPrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = NeonPink,
    testTag: String = "neon_action_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = if (accentColor == NeonCyan || accentColor == NeonMint) ViceObsidian else TextPrimary,
            disabledContainerColor = ViceCardElevated,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ViceSectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ViceOutline,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ViceCard),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.65f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayerBox(
    mediaUri: Uri,
    isVerticalAspect: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isVerticalAspect) Modifier.aspectRatio(9f / 16f)
                else Modifier.aspectRatio(16f / 9f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VideoPreviewDialog(
    title: String,
    filePathOrUri: String,
    isVertical: Boolean,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val uri = remember(filePathOrUri) {
        if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://") || filePathOrUri.startsWith("http")) {
            Uri.parse(filePathOrUri)
        } else {
            Uri.fromFile(File(filePathOrUri))
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = ViceCard,
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPink)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ViceCardElevated)
                            .minimumInteractiveComponentSize()
                            .testTag("close_preview_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close preview",
                            tint = TextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ExoVideoPlayerBox(
                    mediaUri = uri,
                    isVerticalAspect = isVertical,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
