package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val genre: String,
    val aspectRatio: String,
    val visualStyle: String,
    val defaultSceneDurationSec: Int,
    val targetTotalDurationSec: Int,
    val dialogueLanguage: String,
    val customLanguageName: String,
    val voiceMode: String,
    val subtitlesEnabled: Boolean,
    val storyIdea: String,
    val storyScript: String,
    val projectReferenceImagePath: String?,
    val voiceOverVolume: Float,
    val dialogVolume: Float,
    val bgmVolume: Float,
    val sfxVolume: Float,
    val voiceOverMuted: Boolean,
    val dialogMuted: Boolean,
    val bgmMuted: Boolean,
    val sfxMuted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val characterCode: String,
    val role: String,
    val name: String,
    val faceShape: String,
    val bodyColor: String,
    val outfit: String,
    val bodySize: String,
    val eyeShape: String,
    val animationStyle: String,
    val accessories: String,
    val signatureTrait: String,
    val description: String,
    val photoPath: String?,
    val bundledDrawableName: String?
)

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class SceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val orderIndex: Int,
    val title: String,
    val durationSec: Int,
    val visualDescription: String,
    val characterPosition: String,
    val facialExpression: String,
    val characterAction: String,
    val dialogueSpeaker: String,
    val dialogueText: String,
    val narrationText: String,
    val cameraShot: String,
    val cameraMovement: String,
    val transition: String,
    val location: String,
    val lighting: String,
    val importantProps: String,
    val mood: String,
    val musicSfx: String,
    val soundFx: String,
    val notes: String,
    val status: String,
    val isApproved: Boolean,
    val videoGeneratedAt: Long,
    val characterIdsCsv: String,
    val customPrompt: String,
    val customVideoPrompt: String,
    val isPromptManuallyEdited: Boolean,
    val isVideoPromptManuallyEdited: Boolean,
    val referenceImagePath: String?,
    val variation1Path: String?,
    val variation2Path: String?,
    val selectedVariationIndex: Int,
    val isVariationPlaceholder: Boolean
)
