package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.min

data class VideoProbeResult(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int
)

data class ClipExportRequest(
    val sourceUri: Uri,
    val outputFileName: String,
    val startMs: Long,
    val endMs: Long,
    val isVerticalShorts: Boolean,
    val subtitleText: String,
    val headerOverlayText: String,
    val effectStyle: String,
    val videoVolume: Float,
    val musicUri: Uri?,
    val musicVolume: Float
)

data class ClipExportResult(
    val outputFile: File,
    val fileSizeBytes: Long,
    val durationMs: Long
)

@OptIn(UnstableApi::class)
class VideoProcessingEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun probeVideo(uri: Uri): Result<VideoProbeResult> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 1280
            val height = heightStr?.toIntOrNull() ?: 720

            if (durationMs <= 0L) {
                return@withContext Result.failure(
                    IllegalArgumentException("Unable to read video duration. Please pick a valid MP4/MOV video.")
                )
            }

            val name = queryDisplayName(uri) ?: "clip_source.mp4"
            Result.success(
                VideoProbeResult(
                    uri = uri,
                    displayName = name,
                    durationMs = durationMs,
                    width = width,
                    height = height
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Failed to inspect video metadata: ${e.localizedMessage ?: "Unsupported format"}",
                    e
                )
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    suspend fun exportClip(
        request: ClipExportRequest,
        onProgress: (Int) -> Unit
    ): Result<ClipExportResult> {
        if (request.endMs <= request.startMs) {
            return Result.failure(IllegalArgumentException("End time must be greater than start time."))
        }

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = request.outputFileName
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "gta_vi_clip_${System.currentTimeMillis()}" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val videoEffects = buildVideoEffects(request)
        val audioProcessors = listOf(VolumeAudioProcessor(request.videoVolume))

        val clippedMediaItem = MediaItem.Builder()
            .setUri(request.sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(request.startMs)
                    .setEndPositionMs(request.endMs)
                    .build()
            )
            .build()

        val editedVideoItem = EditedMediaItem.Builder(clippedMediaItem)
            .setEffects(
                androidx.media3.transformer.Effects(
                    audioProcessors,
                    videoEffects
                )
            )
            .setRemoveAudio(request.videoVolume <= 0.01f && request.musicUri == null)
            .build()

        return suspendCancellableCoroutine { continuation ->
            mainHandler.post {
                var progressRunnable: Runnable? = null
                var activeTransformer: Transformer? = null

                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        progressRunnable?.let { mainHandler.removeCallbacks(it) }
                        onProgress(100)
                        val size = outFile.length()
                        if (outFile.exists() && size > 0L) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.success(
                                        ClipExportResult(
                                            outputFile = outFile,
                                            fileSizeBytes = size,
                                            durationMs = request.endMs - request.startMs
                                        )
                                    )
                                )
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.failure(IllegalStateException("Export finished but output MP4 file is empty."))
                                )
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        progressRunnable?.let { mainHandler.removeCallbacks(it) }
                        outFile.delete()
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.failure(
                                    IllegalStateException(
                                        "Media3 export failed: ${exportException.localizedMessage ?: exportException.errorCodeName}",
                                        exportException
                                    )
                                )
                            )
                        }
                    }
                }

                try {
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(listener)
                        .build()
                    activeTransformer = transformer

                    if (request.musicUri != null && request.musicVolume > 0.01f) {
                        val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(listOf(editedVideoItem))
                        val musicMediaItem = MediaItem.fromUri(request.musicUri)
                        val musicEditedItem = EditedMediaItem.Builder(musicMediaItem)
                            .setEffects(
                                androidx.media3.transformer.Effects(
                                    listOf(VolumeAudioProcessor(request.musicVolume)),
                                    emptyList()
                                )
                            )
                            .build()
                        val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(musicEditedItem))
                            .buildUpon()
                            .setIsLooping(true)
                            .build()
                        val composition = Composition.Builder(videoSequence, audioSequence).build()
                        transformer.start(composition, outFile.absolutePath)
                    } else {
                        transformer.start(editedVideoItem, outFile.absolutePath)
                    }

                    val holder = ProgressHolder()
                    val poller = object : Runnable {
                        override fun run() {
                            val state = transformer.getProgress(holder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(holder.progress.coerceIn(1, 99))
                            }
                            if (state != Transformer.PROGRESS_STATE_NOT_STARTED && continuation.isActive) {
                                mainHandler.postDelayed(this, 200L)
                            }
                        }
                    }
                    progressRunnable = poller
                    mainHandler.postDelayed(poller, 200L)

                    continuation.invokeOnCancellation {
                        mainHandler.post {
                            progressRunnable?.let { mainHandler.removeCallbacks(it) }
                            runCatching { activeTransformer?.cancel() }
                            outFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    progressRunnable?.let { mainHandler.removeCallbacks(it) }
                    outFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(
                                IllegalStateException(
                                    "Could not start Media3 Transformer: ${e.localizedMessage ?: "Unknown error"}",
                                    e
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildVideoEffects(request: ClipExportRequest): List<Effect> {
        val effects = mutableListOf<Effect>()

        if (request.isVerticalShorts) {
            effects += Presentation.createForAspectRatio(
                9f / 16f,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        }

        when (request.effectStyle) {
            "NEON_VICE" -> {
                effects += RgbAdjustment.Builder()
                    .setRedScale(1.16f)
                    .setGreenScale(0.92f)
                    .setBlueScale(1.24f)
                    .build()
                effects += Contrast(0.22f)
            }
            "HIGH_CONTRAST" -> {
                effects += Contrast(0.38f)
            }
            "CINEMATIC_ZOOM" -> {
                val clipDurationUs = ((request.endMs - request.startMs).coerceAtLeast(1000L)) * 1000f
                effects += MatrixTransformation { presentationTimeUs ->
                    val progress = (presentationTimeUs / clipDurationUs).coerceIn(0f, 1f)
                    val scale = 1.0f + (0.14f * progress)
                    Matrix().apply { postScale(scale, scale) }
                }
            }
            "FADE_INTRO" -> {
                effects += MatrixTransformation { presentationTimeUs ->
                    val introProgress = min(1f, presentationTimeUs / 650_000f)
                    val scale = 1.18f - (0.18f * introProgress)
                    Matrix().apply { postScale(scale, scale) }
                }
            }
        }

        if (request.subtitleText.isNotBlank() || request.headerOverlayText.isNotBlank()) {
            val bitmap = renderOverlayBitmap(
                width = 720,
                height = if (request.isVerticalShorts) 1280 else 720,
                headerText = request.headerOverlayText,
                subtitleText = request.subtitleText
            )
            val overlay = BitmapOverlay.createStaticBitmapOverlay(bitmap)
            effects += OverlayEffect(listOf(overlay))
        }

        return effects
    }

    private fun renderOverlayBitmap(
        width: Int,
        height: Int,
        headerText: String,
        subtitleText: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        if (headerText.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 18, 14, 36)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 240, 255)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val boxWidth = (width * 0.78f)
            val left = (width - boxWidth) / 2f
            val top = height * 0.06f
            val rect = RectF(left, top, left + boxWidth, top + 62f)
            canvas.drawRoundRect(rect, 20f, 20f, badgePaint)
            canvas.drawRoundRect(rect, 20f, 20f, borderPaint)
            canvas.drawText(headerText.take(32), width / 2f, top + 40f, textPaint)
        }

        if (subtitleText.isNotBlank()) {
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(210, 8, 6, 18)
                style = Paint.Style.FILL
            }
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 42, 133)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 240, 255)
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(6f, 0f, 2f, Color.BLACK)
            }
            val lines = wrapText(subtitleText.trim(), 26).take(3)
            val lineHeight = 42f
            val boxHeight = (lines.size * lineHeight) + 34f
            val boxWidth = width * 0.86f
            val left = (width - boxWidth) / 2f
            val bottom = height * 0.88f
            val top = bottom - boxHeight
            val rect = RectF(left, top, left + boxWidth, bottom)
            canvas.drawRoundRect(rect, 22f, 22f, pillPaint)
            canvas.drawRoundRect(rect, 22f, 22f, accentPaint)

            lines.forEachIndexed { idx, line ->
                val y = top + 44f + (idx * lineHeight)
                canvas.drawText(line, width / 2f, y, subPaint)
            }
        }

        return bitmap
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.length + word.length + 1 <= maxChars) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(word.take(maxChars))
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    suspend fun saveToDeviceMovies(sourceFile: File, displayTitle: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    return@withContext Result.failure(
                        IllegalStateException("Exported MP4 file does not exist.")
                    )
                }
                val cleanName = displayTitle.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                    .trim()
                    .ifBlank { "GTA_VI_Clip_${System.currentTimeMillis()}" }
                val fileName = "$cleanName.mp4"

                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/GTA VI Clip AI")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                val uri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(
                        IllegalStateException("MediaStore could not allocate destination video entry.")
                    )

                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: return@withContext Result.failure(
                    IllegalStateException("Could not open MediaStore output stream.")
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val doneValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(uri, doneValues, null, null)
                }
                Result.success(uri)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun shareClipFile(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return Result.failure(IllegalStateException("Clip MP4 file not found on device."))
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share MP4 Clip").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
