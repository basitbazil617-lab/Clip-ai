package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM clip_projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ClipProjectEntity>>

    @Query("SELECT * FROM clip_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ClipProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ClipProjectEntity): Long

    @Update
    suspend fun updateProject(project: ClipProjectEntity)

    @Query("DELETE FROM clip_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM generated_clips ORDER BY createdAt DESC")
    fun observeAllClips(): Flow<List<GeneratedClipEntity>>

    @Query("SELECT * FROM generated_clips WHERE projectId = :projectId ORDER BY startMs ASC")
    fun observeClipsForProject(projectId: Long): Flow<List<GeneratedClipEntity>>

    @Query("SELECT * FROM generated_clips WHERE savedToGallery = 1 OR outputFilePath IS NOT NULL ORDER BY createdAt DESC")
    fun observeExportedAndDownloadedClips(): Flow<List<GeneratedClipEntity>>

    @Query("SELECT COUNT(*) FROM generated_clips WHERE status = 'EXPORTED'")
    fun observeExportedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM generated_clips WHERE savedToGallery = 1")
    fun observeDownloadedCount(): Flow<Int>

    @Query("SELECT * FROM generated_clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: Long): GeneratedClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: GeneratedClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<GeneratedClipEntity>): List<Long>

    @Update
    suspend fun updateClip(clip: GeneratedClipEntity)

    @Query("DELETE FROM generated_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)
}

@Dao
interface SeoDao {
    @Query("SELECT * FROM seo_presets ORDER BY createdAt DESC")
    fun observePresets(): Flow<List<SeoPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: SeoPresetEntity): Long

    @Query("DELETE FROM seo_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
