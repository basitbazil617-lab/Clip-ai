package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepository
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.ClipDao
import com.example.data.local.ClipProjectEntity
import com.example.data.local.GeneratedClipEntity
import com.example.data.local.ProjectDao
import com.example.data.remote.YouTubeMetadataRepository
import com.example.data.remote.YouTubeOEmbedResponse
import com.example.data.remote.YouTubeUrlValidator
import com.example.media.ClipExportRequest
import com.example.media.VideoProcessingEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ClipMakerUiState(
    val youtubeUrl: String = "",
    val urlError: String? = null,
    val validatedVideoId: String? = null,
    val canonicalUrl: String? = null,
    val isFetchingMetadata: Boolean = false,
    val oEmbedInfo: YouTubeOEmbedResponse? = null,
    val metadataError: String? = null,
    val permissionAttested: Boolean = false,
    val authorizedSourceUri: Uri? = null,
    val authorizedSourceLabel: String? = null,
    val sourceDurationMs: Long = 0L,
    val backendEndpointUrl: String = "",
    val isFetchingBackendStream: Boolean = false,
    val backendProgress: Int = 0,
    val backendError: String? = null,
    val isAnalyzingScenes: Boolean = false,
    val aiNotice: String? = null,
    val activeProjectId: Long? = null,
    val actionMessage: String? = null,
    val isActionError: Boolean = false
)

class ClipMakerViewModel(
    private val appContext: Context,
    private val projectDao: ProjectDao,
    private val clipDao: ClipDao,
    private val youtubeRepository: YouTubeMetadataRepository,
    private val aiRepository: AiRepository,
    private val videoEngine: VideoProcessingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClipMakerUiState())
    val uiState: StateFlow<ClipMakerUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectClips: StateFlow<List<GeneratedClipEntity>> = _uiState
        .flatMapLatest { state ->
            val pid = state.activeProjectId
            if (pid != null) clipDao.observeClipsForProject(pid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun onYoutubeUrlChanged(rawUrl: String) {
        val validation = YouTubeUrlValidator.validate(rawUrl)
        _uiState.update {
            it.copy(
                youtubeUrl = rawUrl,
                urlError = if (rawUrl.isBlank()) null else validation.errorMessage,
                validatedVideoId = validation.videoId,
                canonicalUrl = validation.canonicalUrl,
                metadataError = null
            )
        }
    }

    fun onBackendEndpointChanged(endpoint: String) {
        _uiState.update { it.copy(backendEndpointUrl = endpoint, backendError = null) }
    }

    fun setPermissionAttested(attested: Boolean) {
        _uiState.update { it.copy(permissionAttested = attested, actionMessage = null) }
    }

    fun verifyYoutubeUrlAndFetchMetadata() {
        val validation = YouTubeUrlValidator.validate(_uiState.value.youtubeUrl)
        if (!validation.isValid || validation.canonicalUrl == null || validation.videoId == null) {
            _uiState.update {
                it.copy(
                    urlError = validation.errorMessage ?: "Invalid YouTube URL",
                    validatedVideoId = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingMetadata = true,
                    metadataError = null,
                    urlError = null,
                    validatedVideoId = validation.videoId,
                    canonicalUrl = validation.canonicalUrl
                )
            }
            val result = youtubeRepository.fetchPublicMetadata(validation.canonicalUrl)
            result.onSuccess { info ->
                val projectId = ensureProjectSaved(
                    title = info.title.ifBlank { "YouTube Video ${validation.videoId}" },
                    youtubeUrl = validation.canonicalUrl,
                    videoId = validation.videoId,
                    channel = info.authorName,
                    thumb = info.thumbnailUrl
                )
                _uiState.update {
                    it.copy(
                        isFetchingMetadata = false,
                        oEmbedInfo = info,
                        activeProjectId = projectId,
                        actionMessage = "YouTube URL verified via oEmbed metadata.",
                        isActionError = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isFetchingMetadata = false,
                        oEmbedInfo = null,
                        metadataError = err.localizedMessage
                            ?: "Could not fetch YouTube oEmbed metadata. Check connection or URL."
                    )
                }
            }
        }
    }

    fun attachOwnedLocalVideo(uri: Uri) {
        viewModelScope.launch {
            val probeResult = videoEngine.probeVideo(uri)
            probeResult.onSuccess { probe ->
                val state = _uiState.value
                val projectId = ensureProjectSaved(
                    title = state.oEmbedInfo?.title?.ifBlank { probe.displayName } ?: probe.displayName,
                    youtubeUrl = state.canonicalUrl,
                    videoId = state.validatedVideoId,
                    channel = state.oEmbedInfo?.authorName ?: "Local Creator File",
                    thumb = state.oEmbedInfo?.thumbnailUrl,
                    sourceUri = uri.toString(),
                    durationMs = probe.durationMs
                )
                _uiState.update {
                    it.copy(
                        authorizedSourceUri = uri,
                        authorizedSourceLabel = "${probe.displayName} (${probe.width}x${probe.height}, ${probe.durationMs / 1000}s)",
                        sourceDurationMs = probe.durationMs,
                        activeProjectId = projectId,
                        backendError = null,
                        actionMessage = "Authorized source video attached (${probe.durationMs / 1000}s).",
                        isActionError = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionMessage = err.localizedMessage ?: "Failed to inspect selected video.",
                        isActionError = true
                    )
                }
            }
        }
    }

    fun fetchFromAuthorizedBackend() {
        val state = _uiState.value
        if (!state.permissionAttested) {
            _uiState.update {
                it.copy(backendError = "Confirm ownership/copyright permission before contacting backend.")
            }
            return
        }
        val canonical = state.canonicalUrl
        val vid = state.validatedVideoId
        if (canonical == null || vid == null) {
            _uiState.update {
                it.copy(backendError = "Validate a YouTube URL first.")
            }
            return
        }
        if (state.backendEndpointUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    backendError = "Enter your authorized processing backend URL (e.g. https://your-studio-server/api/authorized-source) or attach your owned MP4 file directly from your phone."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingBackendStream = true,
                    backendProgress = 0,
                    backendError = null
                )
            }
            val cacheDir = File(appContext.cacheDir, "authorized_sources").apply { mkdirs() }
            val destFile = File(cacheDir, "auth_${vid}_${System.currentTimeMillis()}.mp4")
            val result = youtubeRepository.fetchFromAuthorizedBackend(
                backendEndpointUrl = state.backendEndpointUrl,
                canonicalYoutubeUrl = canonical,
                videoId = vid,
                destinationFile = destFile,
                onProgress = { pct ->
                    _uiState.update { it.copy(backendProgress = pct) }
                }
            )
            result.onSuccess { file ->
                val uri = Uri.fromFile(file)
                val probe = videoEngine.probeVideo(uri).getOrNull()
                val duration = probe?.durationMs ?: 60_000L
                val projectId = ensureProjectSaved(
                    title = state.oEmbedInfo?.title ?: "Authorized Clip $vid",
                    youtubeUrl = canonical,
                    videoId = vid,
                    channel = state.oEmbedInfo?.authorName,
                    thumb = state.oEmbedInfo?.thumbnailUrl,
                    sourceUri = uri.toString(),
                    durationMs = duration,
                    backendUrl = state.backendEndpointUrl
                )
                _uiState.update {
                    it.copy(
                        isFetchingBackendStream = false,
                        backendProgress = 100,
                        authorizedSourceUri = uri,
                        authorizedSourceLabel = "Backend MP4 (${file.length() / 1024} KB, ${duration / 1000}s)",
                        sourceDurationMs = duration,
                        activeProjectId = projectId,
                        actionMessage = "Authorized MP4 downloaded from backend and ready for clipping.",
                        isActionError = false
                    )
                }
            }.onFailure { err ->
                destFile.delete()
                _uiState.update {
                    it.copy(
                        isFetchingBackendStream = false,
                        backendProgress = 0,
                        backendError = err.localizedMessage ?: "Authorized backend request failed."
                    )
                }
            }
        }
    }

    fun detectHighlightsAndGenerateClips() {
        val state = _uiState.value
        if (!state.permissionAttested) {
            _uiState.update {
                it.copy(
                    actionMessage = "Copyright check required: attest that you own or have permission to edit this video.",
                    isActionError = true
                )
            }
            return
        }
        val title = state.oEmbedInfo?.title
            ?: state.authorizedSourceLabel
            ?: state.validatedVideoId?.let { "YouTube Video $it" }
            ?: run {
                _uiState.update {
                    it.copy(
                        actionMessage = "Paste and validate a YouTube URL or attach an owned video first.",
                        isActionError = true
                    )
                }
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingScenes = true, actionMessage = null) }
            val durationMs = if (state.sourceDurationMs > 0L) state.sourceDurationMs else 120_000L
            val projectId = state.activeProjectId ?: ensureProjectSaved(
                title = title,
                youtubeUrl = state.canonicalUrl,
                videoId = state.validatedVideoId,
                channel = state.oEmbedInfo?.authorName,
                thumb = state.oEmbedInfo?.thumbnailUrl,
                sourceUri = state.authorizedSourceUri?.toString(),
                durationMs = durationMs
            )

            val analysis = aiRepository.analyzeScenesForClips(
                videoTitle = title,
                durationMs = durationMs,
                contextNotes = state.oEmbedInfo?.authorName.orEmpty()
            )

            val entities = analysis.clips.map { scene ->
                GeneratedClipEntity(
                    projectId = projectId,
                    title = scene.title,
                    category = scene.category,
                    startMs = scene.startMs,
                    endMs = scene.endMs,
                    isVerticalShorts = true,
                    subtitleText = scene.subtitleText,
                    headerOverlayText = scene.headerOverlayText,
                    effectStyle = scene.recommendedEffect,
                    videoVolume = 1.0f,
                    musicVolume = 0.0f,
                    musicUri = null,
                    status = "DRAFT",
                    progressPercent = 0,
                    outputFilePath = null,
                    fileSizeBytes = 0L,
                    savedToGallery = false,
                    galleryUri = null,
                    errorMessage = null
                )
            }
            clipDao.insertClips(entities)
            _uiState.update {
                it.copy(
                    isAnalyzingScenes = false,
                    activeProjectId = projectId,
                    aiNotice = analysis.noticeMessage,
                    actionMessage = "Generated ${entities.size} vertical 9:16 clip plans (15–60s). Edit subtitles & render MP4 below.",
                    isActionError = false
                )
            }
        }
    }

    fun updateClipSubtitle(clip: GeneratedClipEntity, newSubtitle: String) {
        viewModelScope.launch {
            clipDao.updateClip(clip.copy(subtitleText = newSubtitle))
        }
    }

    fun updateClipTitle(clip: GeneratedClipEntity, newTitle: String) {
        viewModelScope.launch {
            clipDao.updateClip(clip.copy(title = newTitle))
        }
    }

    fun renderClipToMp4(clip: GeneratedClipEntity) {
        val state = _uiState.value
        if (!state.permissionAttested) {
            _uiState.update {
                it.copy(
                    actionMessage = "Confirm the copyright & ownership permission checkbox first.",
                    isActionError = true
                )
            }
            return
        }
        val sourceUri = state.authorizedSourceUri
        if (sourceUri == null) {
            _uiState.update {
                it.copy(
                    actionMessage = "Attach your owned source video MP4 or fetch from your authorized backend to render a real MP4 (we never bypass YouTube download restrictions).",
                    isActionError = true
                )
            }
            return
        }

        viewModelScope.launch {
            val safeEnd = clip.endMs.coerceAtMost(
                if (state.sourceDurationMs > 0L) state.sourceDurationMs else clip.endMs
            )
            val safeStart = clip.startMs.coerceAtMost((safeEnd - 1000L).coerceAtLeast(0L))

            clipDao.updateClip(
                clip.copy(
                    status = "PROCESSING",
                    progressPercent = 1,
                    errorMessage = null
                )
            )
            val result = videoEngine.exportClip(
                request = ClipExportRequest(
                    sourceUri = sourceUri,
                    outputFileName = "shorts_${clip.id}",
                    startMs = safeStart,
                    endMs = safeEnd,
                    isVerticalShorts = clip.isVerticalShorts,
                    subtitleText = clip.subtitleText,
                    headerOverlayText = clip.headerOverlayText,
                    effectStyle = clip.effectStyle,
                    videoVolume = clip.videoVolume,
                    musicUri = clip.musicUri?.let { Uri.parse(it) },
                    musicVolume = clip.musicVolume
                ),
                onProgress = { pct ->
                    viewModelScope.launch {
                        clipDao.getClipById(clip.id)?.let { latest ->
                            if (latest.status == "PROCESSING") {
                                clipDao.updateClip(latest.copy(progressPercent = pct))
                            }
                        }
                    }
                }
            )
            result.onSuccess { exported ->
                clipDao.updateClip(
                    clip.copy(
                        startMs = safeStart,
                        endMs = safeEnd,
                        status = "EXPORTED",
                        progressPercent = 100,
                        outputFilePath = exported.outputFile.absolutePath,
                        fileSizeBytes = exported.fileSizeBytes,
                        errorMessage = null
                    )
                )
                _uiState.update {
                    it.copy(
                        actionMessage = "Rendered real 9:16 MP4 (${exported.fileSizeBytes / 1024} KB)!",
                        isActionError = false
                    )
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Media3 Transformer render failed."
                clipDao.updateClip(
                    clip.copy(
                        status = "ERROR",
                        progressPercent = 0,
                        errorMessage = msg
                    )
                )
                _uiState.update {
                    it.copy(actionMessage = msg, isActionError = true)
                }
            }
        }
    }

    fun downloadClipToGallery(clip: GeneratedClipEntity) {
        viewModelScope.launch {
            val path = clip.outputFilePath
            if (path.isNullOrBlank()) {
                _uiState.update {
                    it.copy(actionMessage = "Render the MP4 clip before downloading.", isActionError = true)
                }
                return@launch
            }
            videoEngine.saveToDeviceMovies(File(path), clip.title).onSuccess { uri ->
                clipDao.updateClip(clip.copy(savedToGallery = true, galleryUri = uri.toString()))
                _uiState.update {
                    it.copy(actionMessage = "Saved MP4 to Movies/GTA VI Clip AI", isActionError = false)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        actionMessage = err.localizedMessage ?: "Failed to save MP4.",
                        isActionError = true
                    )
                }
            }
        }
    }

    fun shareClip(clip: GeneratedClipEntity) {
        val path = clip.outputFilePath ?: return
        videoEngine.shareClipFile(path).onFailure { err ->
            _uiState.update {
                it.copy(actionMessage = err.localizedMessage ?: "Share failed.", isActionError = true)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private suspend fun ensureProjectSaved(
        title: String,
        youtubeUrl: String?,
        videoId: String?,
        channel: String?,
        thumb: String?,
        sourceUri: String? = _uiState.value.authorizedSourceUri?.toString(),
        durationMs: Long = _uiState.value.sourceDurationMs,
        backendUrl: String? = _uiState.value.backendEndpointUrl.ifBlank { null }
    ): Long {
        val existingId = _uiState.value.activeProjectId
        if (existingId != null) {
            val existing = projectDao.getProjectById(existingId)
            if (existing != null) {
                projectDao.updateProject(
                    existing.copy(
                        title = title,
                        youtubeUrl = youtubeUrl ?: existing.youtubeUrl,
                        youtubeVideoId = videoId ?: existing.youtubeVideoId,
                        channelName = channel ?: existing.channelName,
                        thumbnailUrl = thumb ?: existing.thumbnailUrl,
                        sourceUri = sourceUri ?: existing.sourceUri,
                        authorizedBackendUrl = backendUrl ?: existing.authorizedBackendUrl,
                        permissionAttested = _uiState.value.permissionAttested,
                        durationMs = if (durationMs > 0L) durationMs else existing.durationMs,
                        status = if (sourceUri != null) "READY" else existing.status
                    )
                )
                return existingId
            }
        }
        return projectDao.insertProject(
            ClipProjectEntity(
                title = title,
                youtubeUrl = youtubeUrl,
                youtubeVideoId = videoId,
                channelName = channel,
                thumbnailUrl = thumb,
                sourceUri = sourceUri,
                authorizedBackendUrl = backendUrl,
                permissionAttested = _uiState.value.permissionAttested,
                durationMs = durationMs,
                status = "READY",
                progressPercent = 0,
                errorMessage = null
            )
        )
    }
}
