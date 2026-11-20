package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiRepository
import com.example.data.ai.AiSeoResult
import com.example.data.local.SeoDao
import com.example.data.local.SeoPresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeoUiState(
    val topicInput: String = "",
    val selectedVibe: String = "Viral Gaming",
    val durationSec: Int = 30,
    val isGenerating: Boolean = false,
    val currentResult: AiSeoResult? = null,
    val errorMessage: String? = null
)

class SeoViewModel(
    private val aiRepository: AiRepository,
    private val seoDao: SeoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeoUiState())
    val uiState: StateFlow<SeoUiState> = _uiState.asStateFlow()

    val savedPresets: StateFlow<List<SeoPresetEntity>> = seoDao.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun onTopicChanged(topic: String) {
        _uiState.update { it.copy(topicInput = topic, errorMessage = null) }
    }

    fun onVibeSelected(vibe: String) {
        _uiState.update { it.copy(selectedVibe = vibe) }
    }

    fun onDurationChanged(seconds: Int) {
        _uiState.update { it.copy(durationSec = seconds.coerceIn(15, 60)) }
    }

    fun generateSeo() {
        val state = _uiState.value
        if (state.topicInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a clip topic or highlight moment first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            val res = aiRepository.generateYouTubeSeo(
                clipTopic = state.topicInput,
                vibe = state.selectedVibe,
                durationSec = state.durationSec
            )
            seoDao.insertPreset(
                SeoPresetEntity(
                    topic = state.topicInput.trim(),
                    vibe = state.selectedVibe,
                    titlesJoined = res.titles.joinToString("||"),
                    description = res.description,
                    hashtags = res.hashtags.joinToString(" "),
                    searchTags = res.searchTags.joinToString(", "),
                    pinnedComment = res.pinnedComment,
                    isSamplePlaceholder = res.isSamplePlaceholder
                )
            )
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    currentResult = res,
                    errorMessage = null
                )
            }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            seoDao.deletePresetById(id)
        }
    }
}
