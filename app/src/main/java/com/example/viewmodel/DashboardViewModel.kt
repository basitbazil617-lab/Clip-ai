package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClipDao
import com.example.data.local.ClipProjectEntity
import com.example.data.local.GeneratedClipEntity
import com.example.data.local.ProjectDao
import com.example.media.VideoProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DashboardUiState(
    val totalClipsCreated: Int = 0,
    val totalDownloads: Int = 0,
    val activeProcessingClips: List<GeneratedClipEntity> = emptyList(),
    val recentProjects: List<ClipProjectEntity> = emptyList(),
    val downloadHistory: List<GeneratedClipEntity> = emptyList(),
    val statusFeedback: String? = null,
    val isErrorFeedback: Boolean = false
)

class DashboardViewModel(
    private val projectDao: ProjectDao,
    private val clipDao: ClipDao,
    private val videoEngine: VideoProcessingEngine
) : ViewModel() {

    private val _feedback = MutableStateFlow<Pair<String, Boolean>?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        projectDao.observeProjects(),
        clipDao.observeAllClips(),
        clipDao.observeExportedCount(),
        clipDao.observeDownloadedCount(),
        _feedback.asStateFlow()
    ) { projects, allClips, exportedCount, downloadedCount, feedback ->
        val processing = allClips.filter { it.status == "PROCESSING" }
        val history = allClips.filter { it.status == "EXPORTED" || it.savedToGallery }
        DashboardUiState(
            totalClipsCreated = exportedCount,
            totalDownloads = downloadedCount,
            activeProcessingClips = processing,
            recentProjects = projects,
            downloadHistory = history,
            statusFeedback = feedback?.first,
            isErrorFeedback = feedback?.second ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardUiState()
    )

    fun saveClipToGallery(clip: GeneratedClipEntity) {
        viewModelScope.launch {
            val path = clip.outputFilePath
            if (path.isNullOrBlank()) {
                _feedback.value = "No exported MP4 file found for this clip yet." to true
                return@launch
            }
            val result = videoEngine.saveToDeviceMovies(File(path), clip.title)
            result.onSuccess { uri ->
                clipDao.updateClip(
                    clip.copy(
                        savedToGallery = true,
                        galleryUri = uri.toString()
                    )
                )
                _feedback.value = "Saved MP4 to Movies/GTA VI Clip AI" to false
            }.onFailure { err ->
                _feedback.value = (err.localizedMessage ?: "Failed to save MP4 to Movies") to true
            }
        }
    }

    fun shareClip(clip: GeneratedClipEntity) {
        val path = clip.outputFilePath
        if (path.isNullOrBlank()) {
            _feedback.value = "Export the clip as MP4 before sharing." to true
            return
        }
        videoEngine.shareClipFile(path).onFailure { err ->
            _feedback.value = (err.localizedMessage ?: "Could not share MP4 file.") to true
        }
    }

    fun deleteClip(clip: GeneratedClipEntity) {
        viewModelScope.launch {
            clip.outputFilePath?.let { runCatching { File(it).delete() } }
            clipDao.deleteClipById(clip.id)
            _feedback.value = "Clip removed." to false
        }
    }

    fun deleteProject(project: ClipProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProjectById(project.id)
            _feedback.value = "Project removed." to false
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }
}
