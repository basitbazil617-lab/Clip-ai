package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClipDao
import com.example.data.local.GeneratedClipEntity
import com.example.media.ClipExportRequest
import com.example.media.VideoProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditorUiState(
    val sourceUri: Uri? = null,
    val sourceName: String = "",
    val totalDurationMs: Long = 0L,
    val videoResolution: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isVerticalShorts: Boolean = true,
    val clipTitle: String = "My Vice Edit",
    val subtitleText: String = "",
    val headerOverlayText: String = "",
    val effectStyle: String = "NEON_VICE",
    val videoVolume: Float = 1.0f,
    val musicUri: Uri? = null,
    val musicName: String = "",
    val musicVolume: Float = 0.35f,
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportedFilePath: String? = null,
    val exportedFileSizeBytes: Long = 0L,
    val exportedClipId: Long? = null,
    val savedToGallery: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class EditorViewModel(
    private val clipDao: ClipDao,
    private val videoEngine: VideoProcessingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun attachVideoFromPhone(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, statusMessage = "Inspecting video...") }
            videoEngine.probeVideo(uri).onSuccess { probe ->
                val defaultEnd = probe.durationMs.coerceAtMost(60_000L)
                _uiState.update {
                    it.copy(
                        sourceUri = uri,
                        sourceName = probe.displayName,
                        totalDurationMs = probe.durationMs,
                        videoResolution = "${probe.width}x${probe.height}",
                        startMs = 0L,
                        endMs = defaultEnd,
                        clipTitle = probe.displayName.substringBeforeLast('.').take(32),
                        exportedFilePath = null,
                        exportedFileSizeBytes = 0L,
                        exportedClipId = null,
                        savedToGallery = false,
                        statusMessage = "Loaded ${probe.displayName} (${probe.durationMs / 1000}s)",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        statusMessage = null,
                        errorMessage = err.localizedMessage ?: "Failed to load selected video."
                    )
                }
            }
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val total = _uiState.value.totalDurationMs.coerceAtLeast(1000L)
        val clampedStart = startMs.coerceIn(0L, total - 1000L)
        val clampedEnd = endMs.coerceIn(clampedStart + 1000L, total)
        _uiState.update { it.copy(startMs = clampedStart, endMs = clampedEnd) }
    }

    fun setVerticalShorts(enabled: Boolean) {
        _uiState.update { it.copy(isVerticalShorts = enabled) }
    }

    fun updateClipTitle(title: String) {
        _uiState.update { it.copy(clipTitle = title) }
    }

    fun updateSubtitleText(text: String) {
        _uiState.update { it.copy(subtitleText = text) }
    }

    fun updateHeaderOverlayText(text: String) {
        _uiState.update { it.copy(headerOverlayText = text) }
    }

    fun selectEffectStyle(style: String) {
        _uiState.update { it.copy(effectStyle = style) }
    }

    fun updateVideoVolume(volume: Float) {
        _uiState.update { it.copy(videoVolume = volume.coerceIn(0f, 1.5f)) }
    }

    fun attachMusicTrack(uri: Uri, name: String) {
        _uiState.update {
            it.copy(
                musicUri = uri,
                musicName = name.ifBlank { "Selected Audio Track" },
                statusMessage = "Attached licensed/user music track."
            )
        }
    }

    fun clearMusicTrack() {
        _uiState.update { it.copy(musicUri = null, musicName = "") }
    }

    fun updateMusicVolume(volume: Float) {
        _uiState.update { it.copy(musicVolume = volume.coerceIn(0f, 1.5f)) }
    }

    fun exportMp4() {
        val state = _uiState.value
        val source = state.sourceUri
        if (source == null) {
            _uiState.update { it.copy(errorMessage = "Upload a video from your Android phone first.") }
            return
        }
        if (state.endMs <= state.startMs) {
            _uiState.update { it.copy(errorMessage = "End trim time must be after start time.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportProgress = 1,
                    errorMessage = null,
                    statusMessage = "Transcoding MP4 with Media3 Transformer..."
                )
            }
            val draftId = clipDao.insertClip(
                GeneratedClipEntity(
                    projectId = null,
                    title = state.clipTitle.ifBlank { "GTA VI Studio Edit" },
                    category = "Custom Edit",
                    startMs = state.startMs,
                    endMs = state.endMs,
                    isVerticalShorts = state.isVerticalShorts,
                    subtitleText = state.subtitleText,
                    headerOverlayText = state.headerOverlayText,
                    effectStyle = state.effectStyle,
                    videoVolume = state.videoVolume,
                    musicVolume = state.musicVolume,
                    musicUri = state.musicUri?.toString(),
                    status = "PROCESSING",
                    progressPercent = 1,
                    outputFilePath = null,
                    fileSizeBytes = 0L,
                    savedToGallery = false,
                    galleryUri = null,
                    errorMessage = null
                )
            )

            val result = videoEngine.exportClip(
                request = ClipExportRequest(
                    sourceUri = source,
                    outputFileName = state.clipTitle.ifBlank { "vice_edit" },
                    startMs = state.startMs,
                    endMs = state.endMs,
                    isVerticalShorts = state.isVerticalShorts,
                    subtitleText = state.subtitleText,
                    headerOverlayText = state.headerOverlayText,
                    effectStyle = state.effectStyle,
                    videoVolume = state.videoVolume,
                    musicUri = state.musicUri,
                    musicVolume = state.musicVolume
                ),
                onProgress = { pct ->
                    _uiState.update { it.copy(exportProgress = pct) }
                    viewModelScope.launch {
                        clipDao.getClipById(draftId)?.let { c ->
                            if (c.status == "PROCESSING") {
                                clipDao.updateClip(c.copy(progressPercent = pct))
                            }
                        }
                    }
                }
            )

            result.onSuccess { exported ->
                clipDao.getClipById(draftId)?.let { c ->
                    clipDao.updateClip(
                        c.copy(
                            status = "EXPORTED",
                            progressPercent = 100,
                            outputFilePath = exported.outputFile.absolutePath,
                            fileSizeBytes = exported.fileSizeBytes,
                            errorMessage = null
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 100,
                        exportedFilePath = exported.outputFile.absolutePath,
                        exportedFileSizeBytes = exported.fileSizeBytes,
                        exportedClipId = draftId,
                        savedToGallery = false,
                        statusMessage = "Export complete: ${exported.fileSizeBytes / 1024} KB MP4 ready to preview & download!",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Export failed."
                clipDao.getClipById(draftId)?.let { c ->
                    clipDao.updateClip(
                        c.copy(
                            status = "ERROR",
                            progressPercent = 0,
                            errorMessage = msg
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 0,
                        statusMessage = null,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun saveExportedToMovies() {
        val state = _uiState.value
        val path = state.exportedFilePath ?: return
        viewModelScope.launch {
            videoEngine.saveToDeviceMovies(File(path), state.clipTitle).onSuccess { uri ->
                state.exportedClipId?.let { cid ->
                    clipDao.getClipById(cid)?.let { c ->
                        clipDao.updateClip(c.copy(savedToGallery = true, galleryUri = uri.toString()))
                    }
                }
                _uiState.update {
                    it.copy(
                        savedToGallery = true,
                        statusMessage = "Saved MP4 to Movies/GTA VI Clip AI",
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(errorMessage = err.localizedMessage ?: "Failed to save MP4 to Movies.")
                }
            }
        }
    }

    fun shareExported() {
        val path = _uiState.value.exportedFilePath ?: return
        videoEngine.shareClipFile(path).onFailure { err ->
            _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Share failed.") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
