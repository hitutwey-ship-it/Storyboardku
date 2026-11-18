package com.example.data.export

import com.example.data.local.CharacterEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SceneEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupBundle(
    val projects: List<BackupProject>
)

data class BackupProject(
    val project: ProjectEntity,
    val characters: List<CharacterEntity>,
    val scenes: List<SceneEntity>
)

object BackupJsonHelper {

    fun serialize(bundles: List<BackupProject>): String {
        val root = JSONObject()
        root.put("app", "StoryboardKu")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val projectsArr = JSONArray()
        bundles.forEach { bundle ->
            val p = bundle.project
            val pObj = JSONObject().apply {
                put("oldId", p.id)
                put("title", p.title)
                put("genre", p.genre)
                put("aspectRatio", p.aspectRatio)
                put("visualStyle", p.visualStyle)
                put("defaultSceneDurationSec", p.defaultSceneDurationSec)
                put("targetTotalDurationSec", p.targetTotalDurationSec)
                put("dialogueLanguage", p.dialogueLanguage)
                put("customLanguageName", p.customLanguageName)
                put("voiceMode", p.voiceMode)
                put("subtitlesEnabled", p.subtitlesEnabled)
                put("storyIdea", p.storyIdea)
                put("storyScript", p.storyScript)
                put("voiceOverVolume", p.voiceOverVolume.toDouble())
                put("dialogVolume", p.dialogVolume.toDouble())
                put("bgmVolume", p.bgmVolume.toDouble())
                put("sfxVolume", p.sfxVolume.toDouble())
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }

            val charsArr = JSONArray()
            bundle.characters.forEach { c ->
                charsArr.put(
                    JSONObject().apply {
                        put("oldId", c.id)
                        put("characterCode", c.characterCode)
                        put("role", c.role)
                        put("name", c.name)
                        put("faceShape", c.faceShape)
                        put("bodyColor", c.bodyColor)
                        put("outfit", c.outfit)
                        put("bodySize", c.bodySize)
                        put("eyeShape", c.eyeShape)
                        put("animationStyle", c.animationStyle)
                        put("accessories", c.accessories)
                        put("signatureTrait", c.signatureTrait)
                        put("description", c.description)
                        put("photoPath", c.photoPath ?: JSONObject.NULL)
                        put("bundledDrawableName", c.bundledDrawableName ?: JSONObject.NULL)
                    }
                )
            }
            pObj.put("characters", charsArr)

            val scenesArr = JSONArray()
            bundle.scenes.forEach { s ->
                scenesArr.put(
                    JSONObject().apply {
                        put("orderIndex", s.orderIndex)
                        put("title", s.title)
                        put("durationSec", s.durationSec)
                        put("visualDescription", s.visualDescription)
                        put("characterPosition", s.characterPosition)
                        put("facialExpression", s.facialExpression)
                        put("characterAction", s.characterAction)
                        put("dialogueSpeaker", s.dialogueSpeaker)
                        put("dialogueText", s.dialogueText)
                        put("narrationText", s.narrationText)
                        put("cameraShot", s.cameraShot)
                        put("cameraMovement", s.cameraMovement)
                        put("transition", s.transition)
                        put("location", s.location)
                        put("lighting", s.lighting)
                        put("importantProps", s.importantProps)
                        put("mood", s.mood)
                        put("musicSfx", s.musicSfx)
                        put("soundFx", s.soundFx)
                        put("notes", s.notes)
                        put("status", s.status)
                        put("isApproved", s.isApproved)
                        put("characterIdsCsv", s.characterIdsCsv)
                        put("customPrompt", s.customPrompt)
                        put("customVideoPrompt", s.customVideoPrompt)
                        put("isPromptManuallyEdited", s.isPromptManuallyEdited)
                        put("isVideoPromptManuallyEdited", s.isVideoPromptManuallyEdited)
                    }
                )
            }
            pObj.put("scenes", scenesArr)
            projectsArr.put(pObj)
        }
        root.put("projects", projectsArr)
        return root.toString(2)
    }

    fun deserialize(jsonText: String): BackupBundle {
        val root = JSONObject(jsonText)
        val projectsArr = root.getJSONArray("projects")
        val list = mutableListOf<BackupProject>()

        for (i in 0 until projectsArr.length()) {
            val pObj = projectsArr.getJSONObject(i)
            val now = System.currentTimeMillis()
            val project = ProjectEntity(
                id = 0L,
                title = pObj.optString("title", "Proyek Restore"),
                genre = pObj.optString("genre", "ANIMASI"),
                aspectRatio = pObj.optString("aspectRatio", "RATIO_9_16"),
                visualStyle = pObj.optString("visualStyle", "CARTOON_2D_IN_3D_ENV"),
                defaultSceneDurationSec = pObj.optInt("defaultSceneDurationSec", 10),
                targetTotalDurationSec = pObj.optInt("targetTotalDurationSec", 60),
                dialogueLanguage = pObj.optString("dialogueLanguage", "INDONESIA"),
                customLanguageName = pObj.optString("customLanguageName", ""),
                voiceMode = pObj.optString("voiceMode", "DIALOG_KARAKTER"),
                subtitlesEnabled = pObj.optBoolean("subtitlesEnabled", false),
                storyIdea = pObj.optString("storyIdea", ""),
                storyScript = pObj.optString("storyScript", ""),
                projectReferenceImagePath = null,
                voiceOverVolume = pObj.optDouble("voiceOverVolume", 1.0).toFloat(),
                dialogVolume = pObj.optDouble("dialogVolume", 1.0).toFloat(),
                bgmVolume = pObj.optDouble("bgmVolume", 0.5).toFloat(),
                sfxVolume = pObj.optDouble("sfxVolume", 0.5).toFloat(),
                voiceOverMuted = false,
                dialogMuted = false,
                bgmMuted = false,
                sfxMuted = false,
                createdAt = pObj.optLong("createdAt", now),
                updatedAt = now
            )

            val chars = mutableListOf<CharacterEntity>()
            val charsArr = pObj.optJSONArray("characters") ?: JSONArray()
            for (cIdx in 0 until charsArr.length()) {
                val cObj = charsArr.getJSONObject(cIdx)
                chars += CharacterEntity(
                    id = cObj.optLong("oldId", 0L),
                    projectId = 0L,
                    characterCode = cObj.optString("characterCode", "CHR-%03d".format(cIdx + 1)),
                    role = cObj.optString("role", "UTAMA"),
                    name = cObj.optString("name", "Karakter"),
                    faceShape = cObj.optString("faceShape", ""),
                    bodyColor = cObj.optString("bodyColor", ""),
                    outfit = cObj.optString("outfit", ""),
                    bodySize = cObj.optString("bodySize", ""),
                    eyeShape = cObj.optString("eyeShape", ""),
                    animationStyle = cObj.optString("animationStyle", "Kartun 2D outline tinta hitam"),
                    accessories = cObj.optString("accessories", ""),
                    signatureTrait = cObj.optString("signatureTrait", ""),
                    description = cObj.optString("description", ""),
                    photoPath = cObj.optString("photoPath", "").takeIf { it.isNotBlank() && it != "null" },
                    bundledDrawableName = cObj.optString("bundledDrawableName", "").takeIf { it.isNotBlank() && it != "null" }
                )
            }

            val scenes = mutableListOf<SceneEntity>()
            val scenesArr = pObj.optJSONArray("scenes") ?: JSONArray()
            for (sIdx in 0 until scenesArr.length()) {
                val sObj = scenesArr.getJSONObject(sIdx)
                scenes += SceneEntity(
                    id = 0L,
                    projectId = 0L,
                    orderIndex = sObj.optInt("orderIndex", sIdx),
                    title = sObj.optString("title", "Scene ${sIdx + 1}"),
                    durationSec = sObj.optInt("durationSec", 10),
                    visualDescription = sObj.optString("visualDescription", ""),
                    characterPosition = sObj.optString("characterPosition", "Tengah frame"),
                    facialExpression = sObj.optString("facialExpression", "Ekspresif"),
                    characterAction = sObj.optString("characterAction", "Gerakan dinamis"),
                    dialogueSpeaker = sObj.optString("dialogueSpeaker", ""),
                    dialogueText = sObj.optString("dialogueText", ""),
                    narrationText = sObj.optString("narrationText", ""),
                    cameraShot = sObj.optString("cameraShot", "MEDIUM"),
                    cameraMovement = sObj.optString("cameraMovement", "DOLLY_IN"),
                    transition = sObj.optString("transition", "SMOOTH_CINEMATIC"),
                    location = sObj.optString("location", ""),
                    lighting = sObj.optString("lighting", "Pencahayaan sinematik dramatis"),
                    importantProps = sObj.optString("importantProps", ""),
                    mood = sObj.optString("mood", ""),
                    musicSfx = sObj.optString("musicSfx", ""),
                    soundFx = sObj.optString("soundFx", ""),
                    notes = sObj.optString("notes", ""),
                    status = sObj.optString("status", "BELUM_ADA_GAMBAR"),
                    isApproved = sObj.optBoolean("isApproved", false),
                    videoGeneratedAt = 0L,
                    characterIdsCsv = sObj.optString("characterIdsCsv", ""),
                    customPrompt = sObj.optString("customPrompt", ""),
                    customVideoPrompt = sObj.optString("customVideoPrompt", ""),
                    isPromptManuallyEdited = sObj.optBoolean("isPromptManuallyEdited", false),
                    isVideoPromptManuallyEdited = sObj.optBoolean("isVideoPromptManuallyEdited", false),
                    referenceImagePath = null,
                    variation1Path = null,
                    variation2Path = null,
                    selectedVariationIndex = 0,
                    isVariationPlaceholder = false
                )
            }
            list += BackupProject(project, chars, scenes)
        }

        return BackupBundle(list)
    }
}
