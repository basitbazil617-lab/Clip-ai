package com.example.viewmodel

import android.content.Context
import com.example.data.ai.AiRepository
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.remote.YouTubeMetadataRepository
import com.example.media.VideoProcessingEngine

class StudioContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val youtubeRepository: YouTubeMetadataRepository by lazy { YouTubeMetadataRepository() }
    val aiRepository: AiRepository by lazy { PlaceholderAiRepository() }
    val videoEngine: VideoProcessingEngine by lazy { VideoProcessingEngine(appContext) }
}
