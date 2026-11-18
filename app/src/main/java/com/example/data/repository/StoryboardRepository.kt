package com.example.data.repository

import android.net.Uri
import com.example.data.ai.AiGenerationResult
import com.example.data.ai.AiImageRepository
import com.example.data.ai.AiStoryScriptRepository
import com.example.data.export.BackupJsonHelper
import com.example.data.export.BackupProject
import com.example.data.export.ExportHelper
import com.example.data.export.Mp4VideoRenderer
import com.example.data.local.CharacterEntity
import com.example.data.local.DefaultSeedData
import com.example.data.local.ProjectEntity
import com.example.data.local.SceneEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.StoryboardDao
import com.example.data.storage.ImageStorageHelper
import com.example.model.AppSettings
import com.example.model.AspectRatioOption
import com.example.model.AudioMixerSettings
import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterRole
import com.example.model.CharacterUi
import com.example.model.DialogueLanguage
import com.example.model.ProjectGenre
import com.example.model.ProjectSummaryUi
import com.example.model.ProjectUi
import com.example.model.SceneStatus
import com.example.model.SceneTransition
import com.example.model.SceneUi
import com.example.model.TextScaleOption
import com.example.model.ThemeMode
import com.example.model.VisualStyle
import com.example.model.VoiceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoryboardRepository(
    private val dao: StoryboardDao,
    private val settingsStore: SettingsDataStore,
    private val aiImageRepository: AiImageRepository,
    private val aiStoryScriptRepository: AiStoryScriptRepository,
    val imageStorageHelper: ImageStorageHelper,
    val exportHelper: ExportHelper,
    val mp4VideoRenderer: Mp4VideoRenderer
) {
    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow

    val isRealAiEnabled: Boolean
        get() = aiImageRepository.isRealAiEnabled && aiStoryScriptRepository.isRealAiEnabled

    suspend fun ensureInitialized(): Long {
        val seededId = DefaultSeedData.seedIfEmpty(dao)
        val projects = dao.getAllProjectsOnce()
        return seededId ?: projects.firstOrNull()?.id ?: -1L
    }

    fun observeProjectSummaries(): Flow<List<ProjectSummaryUi>> {
        return dao.observeAllProjects().map { entities ->
            entities.map { p ->
                val scenes = dao.getScenesOnce(p.id)
                val chars = dao.getCharactersOnce(p.id)
                ProjectSummaryUi(
                    project = p.toDomain(),
                    sceneCount = scenes.size,
                    totalDurationSec = scenes.sumOf { it.durationSec },
                    completedImageCount = scenes.count { it.status == SceneStatus.GAMBAR_JADI.name },
                    approvedCount = scenes.count { it.isApproved },
                    characterCount = chars.size
                )
            }
        }
    }

    fun observeProject(projectId: Long): Flow<ProjectUi?> =
        dao.observeProject(projectId).map { it?.toDomain() }

    fun observeCharacters(projectId: Long): Flow<List<CharacterUi>> =
        dao.observeCharacters(projectId).map { list -> list.map { it.toDomain() } }

    fun observeScenes(projectId: Long): Flow<List<SceneUi>> =
        dao.observeScenes(projectId).map { list -> list.map { it.toDomain() } }

    suspend fun setActiveProject(projectId: Long) = settingsStore.setActiveProjectId(projectId)
    suspend fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)
    suspend fun setTextScale(scale: TextScaleOption) = settingsStore.setTextScale(scale)
    suspend fun setDefaultStyle(style: VisualStyle) = settingsStore.setDefaultStyle(style)
    suspend fun setDefaultRatio(ratio: AspectRatioOption) = settingsStore.setDefaultRatio(ratio)
    suspend fun setDefaultSceneDuration(sec: Int) = settingsStore.setDefaultSceneDuration(sec)
    suspend fun setOnboardingDismissed(dismissed: Boolean) = settingsStore.setOnboardingDismissed(dismissed)

    suspend fun updateProject(project: ProjectUi) {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun importProjectReferenceImage(project: ProjectUi, uri: Uri): Result<ProjectUi> {
        return imageStorageHelper.importAndCompressImage(uri, "proj_ref_${project.id}").map { path ->
            val updated = project.copy(projectReferenceImagePath = path)
            updateProject(updated)
            updated
        }
    }

    suspend fun generateScriptFromIdea(
        project: ProjectUi,
        characters: List<CharacterUi>
    ): String {
        val script = aiStoryScriptRepository.generateScriptFromIdea(project.storyIdea, project, characters)
        updateProject(project.copy(storyScript = script))
        return script
    }

    /**
     * Generates scenes from the project's script while protecting any scenes that the user
     * has already approved (`isApproved == true`), preserving them intact at the start unless
     * the user explicitly clears them.
     */
    suspend fun generateStoryboardFromScript(
        project: ProjectUi,
        characters: List<CharacterUi>,
        replaceUnapprovedOnly: Boolean = true
    ): Int {
        val existing = dao.getScenesOnce(project.id).map { it.toDomain() }
        val approvedScenes = if (replaceUnapprovedOnly) existing.filter { it.isApproved } else emptyList()
        val unapprovedScenes = if (replaceUnapprovedOnly) existing.filter { !it.isApproved } else existing

        // Remove only unapproved scenes so approved scenes are NEVER modified
        unapprovedScenes.forEach { dao.deleteSceneById(it.id) }

        val generated = aiStoryScriptRepository.generateScenesFromScript(
            project = project,
            script = project.storyScript,
            characters = characters,
            existingApprovedScenes = approvedScenes
        )
        dao.insertScenes(generated.map { it.toEntity() })
        touchProject(project.id)
        return generated.size
    }

    suspend fun createNewProject(
        title: String,
        style: VisualStyle,
        ratio: AspectRatioOption,
        defaultDuration: Int
    ): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertProject(
            ProjectEntity(
                title = title.ifBlank { "Proyek Baru" },
                genre = ProjectGenre.ANIMASI.name,
                aspectRatio = ratio.name,
                visualStyle = style.name,
                defaultSceneDurationSec = defaultDuration,
                targetTotalDurationSec = 60,
                dialogueLanguage = DialogueLanguage.INDONESIA.name,
                customLanguageName = "",
                voiceMode = VoiceMode.DIALOG_KARAKTER.name,
                subtitlesEnabled = false,
                storyIdea = "",
                storyScript = "",
                projectReferenceImagePath = null,
                voiceOverVolume = 1.0f,
                dialogVolume = 1.0f,
                bgmVolume = 0.50f,
                sfxVolume = 0.50f,
                voiceOverMuted = false,
                dialogMuted = false,
                bgmMuted = false,
                sfxMuted = false,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertScene(
            SceneUi(
                projectId = id,
                orderIndex = 0,
                title = "Scene Pembuka",
                durationSec = defaultDuration
            ).toEntity()
        )
        settingsStore.setActiveProjectId(id)
        return id
    }

    suspend fun duplicateProject(projectId: Long): Long? {
        val src = dao.getProjectById(projectId) ?: return null
        val srcChars = dao.getCharactersOnce(projectId)
        val srcScenes = dao.getScenesOnce(projectId)
        val now = System.currentTimeMillis()
        val newProjectId = dao.insertProject(
            src.copy(id = 0L, title = "${src.title} (Salinan)", createdAt = now, updatedAt = now)
        )
        val charMap = mutableMapOf<Long, Long>()
        srcChars.forEach { c ->
            val newCharId = dao.insertCharacter(c.copy(id = 0L, projectId = newProjectId))
            charMap[c.id] = newCharId
        }
        val newScenes = srcScenes.map { s ->
            val remappedCsv = s.characterIdsCsv.split(",")
                .mapNotNull { it.trim().toLongOrNull()?.let { old -> charMap[old] } }
                .joinToString(",")
            s.copy(id = 0L, projectId = newProjectId, characterIdsCsv = remappedCsv)
        }
        dao.insertScenes(newScenes)
        return newProjectId
    }

    suspend fun getProjectSnapshot(projectId: Long): Triple<ProjectUi, List<CharacterUi>, List<SceneUi>>? {
        val p = dao.getProjectById(projectId) ?: return null
        val chars = dao.getCharactersOnce(projectId).map { it.toDomain() }
        val scenes = dao.getScenesOnce(projectId).map { it.toDomain() }
        return Triple(p.toDomain(), chars, scenes)
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteProjectById(projectId)
    }

    suspend fun restoreFullProjectSnapshot(
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ): Long {
        val newProjectId = dao.insertProject(project.copy(id = 0L).toEntity())
        val charMap = mutableMapOf<Long, Long>()
        characters.forEach { c ->
            val newId = dao.insertCharacter(c.copy(id = 0L, projectId = newProjectId).toEntity())
            charMap[c.id] = newId
        }
        val restoredScenes = scenes.mapIndexed { idx, s ->
            val mappedChars = s.characterIds.mapNotNull { charMap[it] }
            s.copy(id = 0L, projectId = newProjectId, orderIndex = idx, characterIds = mappedChars).toEntity()
        }
        dao.insertScenes(restoredScenes)
        settingsStore.setActiveProjectId(newProjectId)
        return newProjectId
    }

    suspend fun saveCharacter(character: CharacterUi): Long {
        touchProject(character.projectId)
        val code = if (character.characterCode.isBlank()) {
            val count = dao.getCharactersOnce(character.projectId).size + 1
            "CHR-%03d".format(count)
        } else character.characterCode
        val fixed = character.copy(characterCode = code)
        return if (fixed.id == 0L) {
            dao.insertCharacter(fixed.toEntity())
        } else {
            dao.updateCharacter(fixed.toEntity())
            fixed.id
        }
    }

    suspend fun deleteCharacter(characterId: Long, projectId: Long) {
        dao.deleteCharacterById(characterId)
        val scenes = dao.getScenesOnce(projectId)
        val updated = scenes.map { s ->
            val filtered = s.characterIdsCsv.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it != characterId }
                .joinToString(",")
            s.copy(characterIdsCsv = filtered)
        }
        dao.updateScenes(updated)
        touchProject(projectId)
    }

    suspend fun saveScene(scene: SceneUi): Long {
        touchProject(scene.projectId)
        return if (scene.id == 0L) {
            dao.insertScene(scene.toEntity())
        } else {
            dao.updateScene(scene.toEntity())
            scene.id
        }
    }

    suspend fun duplicateScene(scene: SceneUi): Long {
        val existing = dao.getScenesOnce(scene.projectId)
        val copy = scene.copy(
            id = 0L,
            orderIndex = existing.size,
            title = "${scene.title} (Salinan)",
            isApproved = false
        )
        touchProject(scene.projectId)
        return dao.insertScene(copy.toEntity())
    }

    suspend fun deleteSceneAndReindex(scene: SceneUi) {
        dao.deleteSceneById(scene.id)
        val remaining = dao.getScenesOnce(scene.projectId)
        dao.reindexAndSaveScenes(remaining)
        touchProject(scene.projectId)
    }

    suspend fun restoreDeletedScene(scene: SceneUi) {
        val current = dao.getScenesOnce(scene.projectId).toMutableList()
        val insertIdx = scene.orderIndex.coerceIn(0, current.size)
        val restoredId = dao.insertScene(scene.copy(id = 0L, orderIndex = insertIdx).toEntity())
        val refreshed = dao.getScenesOnce(scene.projectId).toMutableList()
        val insertedItem = refreshed.find { it.id == restoredId }
        if (insertedItem != null) {
            refreshed.remove(insertedItem)
            refreshed.add(insertIdx.coerceIn(0, refreshed.size), insertedItem)
            dao.reindexAndSaveScenes(refreshed)
        }
        touchProject(scene.projectId)
    }

    suspend fun moveScene(projectId: Long, fromIndex: Int, toIndex: Int) {
        val list = dao.getScenesOnce(projectId).toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        dao.reindexAndSaveScenes(list)
        touchProject(projectId)
    }

    suspend fun generateSceneImagePair(
        project: ProjectUi,
        scene: SceneUi,
        characters: List<CharacterUi>,
        prompt: String
    ): AiGenerationResult {
        val sceneChars = characters.filter { it.id in scene.characterIds }
        val result = aiImageRepository.generateSceneVariations(
            scene = scene,
            characters = sceneChars,
            aspectRatio = project.aspectRatio,
            visualStyle = project.visualStyle,
            prompt = prompt
        )
        if (result is AiGenerationResult.Success) {
            val updated = scene.copy(
                variation1Path = result.variation1Path,
                variation2Path = result.variation2Path,
                selectedVariationIndex = 1,
                status = SceneStatus.GAMBAR_JADI,
                isVariationPlaceholder = result.isPlaceholder
            )
            saveScene(updated)
        }
        return result
    }

    suspend fun importUserImageIntoScene(scene: SceneUi, uri: Uri, asReferenceOnly: Boolean): Result<SceneUi> {
        val prefix = if (asReferenceOnly) "ref_scene_${scene.id}" else "img_scene_${scene.id}"
        return imageStorageHelper.importAndCompressImage(uri, prefix).map { path ->
            val updated = if (asReferenceOnly) {
                scene.copy(referenceImagePath = path)
            } else {
                if (scene.variation1Path == null || scene.selectedVariationIndex == 1) {
                    scene.copy(
                        variation1Path = path,
                        selectedVariationIndex = 1,
                        status = SceneStatus.GAMBAR_JADI,
                        isVariationPlaceholder = false
                    )
                } else {
                    scene.copy(
                        variation2Path = path,
                        selectedVariationIndex = 2,
                        status = SceneStatus.GAMBAR_JADI,
                        isVariationPlaceholder = false
                    )
                }
            }
            saveScene(updated)
            updated
        }
    }

    suspend fun importBackupJson(jsonText: String): Result<Int> = runCatching {
        val bundle = BackupJsonHelper.deserialize(jsonText)
        var importedCount = 0
        var lastId = -1L
        bundle.projects.forEach { bp ->
            val newProjectId = dao.insertProject(bp.project.copy(id = 0L))
            val charMap = mutableMapOf<Long, Long>()
            bp.characters.forEach { c ->
                val newCharId = dao.insertCharacter(c.copy(id = 0L, projectId = newProjectId))
                if (c.id != 0L) charMap[c.id] = newCharId
            }
            val newScenes = bp.scenes.mapIndexed { idx, s ->
                val mappedCsv = s.characterIdsCsv.split(",")
                    .mapNotNull { it.trim().toLongOrNull()?.let { old -> charMap[old] } }
                    .joinToString(",")
                s.copy(id = 0L, projectId = newProjectId, orderIndex = idx, characterIdsCsv = mappedCsv)
            }
            dao.insertScenes(newScenes)
            importedCount++
            lastId = newProjectId
        }
        if (lastId != -1L) settingsStore.setActiveProjectId(lastId)
        importedCount
    }

    suspend fun exportAllProjectsJson(): String {
        val projects = dao.getAllProjectsOnce()
        val bundles = projects.map { p ->
            BackupProject(
                project = p,
                characters = dao.getCharactersOnce(p.id),
                scenes = dao.getScenesOnce(p.id)
            )
        }
        return BackupJsonHelper.serialize(bundles)
    }

    suspend fun getReferencedImagePaths(): Set<String> {
        val projects = dao.getAllProjectsOnce()
        val chars = dao.getAllCharactersOnce()
        val scenes = dao.getAllScenesOnce()
        return buildSet {
            projects.forEach { it.projectReferenceImagePath?.let(::add) }
            chars.forEach { it.photoPath?.let(::add) }
            scenes.forEach { s ->
                s.referenceImagePath?.let(::add)
                s.variation1Path?.let(::add)
                s.variation2Path?.let(::add)
            }
        }
    }

    private suspend fun touchProject(projectId: Long) {
        dao.getProjectById(projectId)?.let {
            dao.updateProject(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    private fun ProjectEntity.toDomain() = ProjectUi(
        id = id,
        title = title,
        genre = runCatching { ProjectGenre.valueOf(genre) }.getOrDefault(ProjectGenre.ANIMASI),
        aspectRatio = runCatching { AspectRatioOption.valueOf(aspectRatio) }.getOrDefault(AspectRatioOption.RATIO_9_16),
        visualStyle = runCatching { VisualStyle.valueOf(visualStyle) }.getOrDefault(VisualStyle.CARTOON_2D_IN_3D_ENV),
        defaultSceneDurationSec = defaultSceneDurationSec,
        targetTotalDurationSec = targetTotalDurationSec,
        dialogueLanguage = runCatching { DialogueLanguage.valueOf(dialogueLanguage) }.getOrDefault(DialogueLanguage.INDONESIA),
        customLanguageName = customLanguageName,
        voiceMode = runCatching { VoiceMode.valueOf(voiceMode) }.getOrDefault(VoiceMode.DIALOG_KARAKTER),
        subtitlesEnabled = subtitlesEnabled,
        storyIdea = storyIdea,
        storyScript = storyScript,
        projectReferenceImagePath = projectReferenceImagePath,
        audioMixer = AudioMixerSettings(
            voiceOverVolume = voiceOverVolume,
            dialogVolume = dialogVolume,
            bgmVolume = bgmVolume,
            sfxVolume = sfxVolume,
            voiceOverMuted = voiceOverMuted,
            dialogMuted = dialogMuted,
            bgmMuted = bgmMuted,
            sfxMuted = sfxMuted
        ),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ProjectUi.toEntity() = ProjectEntity(
        id = id,
        title = title,
        genre = genre.name,
        aspectRatio = aspectRatio.name,
        visualStyle = visualStyle.name,
        defaultSceneDurationSec = defaultSceneDurationSec,
        targetTotalDurationSec = targetTotalDurationSec,
        dialogueLanguage = dialogueLanguage.name,
        customLanguageName = customLanguageName,
        voiceMode = voiceMode.name,
        subtitlesEnabled = subtitlesEnabled,
        storyIdea = storyIdea,
        storyScript = storyScript,
        projectReferenceImagePath = projectReferenceImagePath,
        voiceOverVolume = audioMixer.voiceOverVolume,
        dialogVolume = audioMixer.dialogVolume,
        bgmVolume = audioMixer.bgmVolume,
        sfxVolume = audioMixer.sfxVolume,
        voiceOverMuted = audioMixer.voiceOverMuted,
        dialogMuted = audioMixer.dialogMuted,
        bgmMuted = audioMixer.bgmMuted,
        sfxMuted = audioMixer.sfxMuted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun CharacterEntity.toDomain() = CharacterUi(
        id = id,
        projectId = projectId,
        characterCode = characterCode,
        role = runCatching { CharacterRole.valueOf(role) }.getOrDefault(CharacterRole.UTAMA),
        name = name,
        faceShape = faceShape,
        bodyColor = bodyColor,
        outfit = outfit,
        bodySize = bodySize,
        eyeShape = eyeShape,
        animationStyle = animationStyle,
        accessories = accessories,
        signatureTrait = signatureTrait,
        description = description,
        photoPath = photoPath,
        bundledDrawableName = bundledDrawableName
    )

    private fun CharacterUi.toEntity() = CharacterEntity(
        id = id,
        projectId = projectId,
        characterCode = characterCode,
        role = role.name,
        name = name,
        faceShape = faceShape,
        bodyColor = bodyColor,
        outfit = outfit,
        bodySize = bodySize,
        eyeShape = eyeShape,
        animationStyle = animationStyle,
        accessories = accessories,
        signatureTrait = signatureTrait,
        description = description,
        photoPath = photoPath,
        bundledDrawableName = bundledDrawableName
    )

    private fun SceneEntity.toDomain() = SceneUi(
        id = id,
        projectId = projectId,
        orderIndex = orderIndex,
        title = title,
        durationSec = durationSec,
        visualDescription = visualDescription,
        characterPosition = characterPosition,
        facialExpression = facialExpression,
        characterAction = characterAction,
        dialogueSpeaker = dialogueSpeaker,
        dialogueText = dialogueText,
        narrationText = narrationText,
        cameraShot = runCatching { CameraShotType.valueOf(cameraShot) }.getOrDefault(CameraShotType.MEDIUM),
        cameraMovement = runCatching { CameraMovement.valueOf(cameraMovement) }.getOrDefault(CameraMovement.DOLLY_IN),
        transition = runCatching { SceneTransition.valueOf(transition) }.getOrDefault(SceneTransition.SMOOTH_CINEMATIC),
        location = location,
        lighting = lighting,
        importantProps = importantProps,
        mood = mood,
        musicSfx = musicSfx,
        soundFx = soundFx,
        notes = notes,
        status = runCatching { SceneStatus.valueOf(status) }.getOrDefault(SceneStatus.BELUM_ADA_GAMBAR),
        isApproved = isApproved,
        videoGeneratedAt = videoGeneratedAt,
        characterIds = characterIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() },
        customPrompt = customPrompt,
        customVideoPrompt = customVideoPrompt,
        isPromptManuallyEdited = isPromptManuallyEdited,
        isVideoPromptManuallyEdited = isVideoPromptManuallyEdited,
        referenceImagePath = referenceImagePath,
        variation1Path = variation1Path,
        variation2Path = variation2Path,
        selectedVariationIndex = selectedVariationIndex,
        isVariationPlaceholder = isVariationPlaceholder
    )

    private fun SceneUi.toEntity() = SceneEntity(
        id = id,
        projectId = projectId,
        orderIndex = orderIndex,
        title = title,
        durationSec = durationSec,
        visualDescription = visualDescription,
        characterPosition = characterPosition,
        facialExpression = facialExpression,
        characterAction = characterAction,
        dialogueSpeaker = dialogueSpeaker,
        dialogueText = dialogueText,
        narrationText = narrationText,
        cameraShot = cameraShot.name,
        cameraMovement = cameraMovement.name,
        transition = transition.name,
        location = location,
        lighting = lighting,
        importantProps = importantProps,
        mood = mood,
        musicSfx = musicSfx,
        soundFx = soundFx,
        notes = notes,
        status = status.name,
        isApproved = isApproved,
        videoGeneratedAt = videoGeneratedAt,
        characterIdsCsv = characterIds.joinToString(","),
        customPrompt = customPrompt,
        customVideoPrompt = customVideoPrompt,
        isPromptManuallyEdited = isPromptManuallyEdited,
        isVideoPromptManuallyEdited = isVideoPromptManuallyEdited,
        referenceImagePath = referenceImagePath,
        variation1Path = variation1Path,
        variation2Path = variation2Path,
        selectedVariationIndex = selectedVariationIndex,
        isVariationPlaceholder = isVariationPlaceholder
    )
}
