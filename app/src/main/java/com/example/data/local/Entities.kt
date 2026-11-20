package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clip_projects")
data class ClipProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val youtubeUrl: String?,
    val youtubeVideoId: String?,
    val channelName: String?,
    val thumbnailUrl: String?,
    val sourceUri: String?,
    val authorizedBackendUrl: String?,
    val permissionAttested: Boolean,
    val durationMs: Long,
    val status: String, // READY, PROCESSING, COMPLETED, ERROR
    val progressPercent: Int,
    val errorMessage: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_clips")
data class GeneratedClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long?,
    val title: String,
    val category: String, // Funny Moment, Reaction, Highlight, Action Scene
    val startMs: Long,
    val endMs: Long,
    val isVerticalShorts: Boolean,
    val subtitleText: String,
    val headerOverlayText: String,
    val effectStyle: String, // NONE, NEON_VICE, HIGH_CONTRAST, CINEMATIC_ZOOM, FADE_INTRO
    val videoVolume: Float,
    val musicVolume: Float,
    val musicUri: String?,
    val status: String, // DRAFT, PROCESSING, EXPORTED, ERROR
    val progressPercent: Int,
    val outputFilePath: String?,
    val fileSizeBytes: Long,
    val savedToGallery: Boolean,
    val galleryUri: String?,
    val errorMessage: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "seo_presets")
data class SeoPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val topic: String,
    val vibe: String,
    val titlesJoined: String, // separated by ||
    val description: String,
    val hashtags: String,
    val searchTags: String,
    val pinnedComment: String,
    val isSamplePlaceholder: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
