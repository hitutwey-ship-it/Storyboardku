package com.example

import com.example.data.export.BackupJsonHelper
import com.example.data.export.BackupProject
import com.example.data.local.CharacterEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SceneEntity
import com.example.model.AspectRatioOption
import com.example.model.AudioMixerSettings
import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterRole
import com.example.model.CharacterUi
import com.example.model.ProjectUi
import com.example.model.SceneUi
import com.example.model.VisualStyle
import com.example.model.VoiceMode
import com.example.model.estimateNarrationSeconds
import com.example.model.formatScenePromptSheet
import com.example.model.formatTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoryboardModelsTest {

    @Test
    fun buildAutoImagePrompt_includesCharacterLockAndMandatorySuffix() {
        val project = ProjectUi(
            id = 1L,
            title = "Sireum Boy — Mencari Makanan",
            aspectRatio = AspectRatioOption.RATIO_16_9,
            visualStyle = VisualStyle.CARTOON_2D_IN_3D_ENV
        )
        val characters = listOf(
            CharacterUi(
                id = 10L,
                projectId = 1L,
                characterCode = "CHR-001",
                role = CharacterRole.UTAMA,
                name = "Sireum Boy",
                outfit = "Kostum superhero semut hitam-merah",
                description = "10-year-old boy with messy brown hair and red-black ant superhero suit"
            )
        )
        val scene = SceneUi(
            id = 100L,
            projectId = 1L,
            orderIndex = 0,
            title = "Berangkat",
            visualDescription = "Standing at tunnel gate ready for adventure",
            dialogueSpeaker = "Sireum Boy",
            dialogueText = "Ayo Kricik!",
            cameraShot = CameraShotType.LOW_ANGLE,
            mood = "Cheerful and brave",
            characterIds = listOf(10L)
        )

        val prompt = com.example.model.buildAutoImagePrompt(project, scene, characters)
        assertTrue(prompt.contains("2D cartoon characters with bold black ink outlines"))
        assertTrue(prompt.contains("16:9 widescreen"))
        assertTrue(prompt.contains("CHR-001"))
        assertTrue(prompt.contains("Sireum Boy"))
        assertTrue(prompt.contains("messy brown hair"))
        assertTrue(prompt.endsWith("same character design in every scene, do not change the character, no subtitles, no on-screen text"))
    }

    @Test
    fun buildAutoVideoPrompt_and_promptSheet_and_defaults_workCorrectly() {
        val project = ProjectUi(id = 1L, title = "Test")
        assertFalse(project.subtitlesEnabled)
        assertEquals(0.50f, project.audioMixer.bgmVolume, 0.001f)
        assertEquals(0.50f, project.audioMixer.sfxVolume, 0.001f)

        val scene = SceneUi(
            id = 1L,
            projectId = 1L,
            orderIndex = 0,
            title = "Lari",
            durationSec = 10,
            cameraMovement = CameraMovement.TRACKING,
            narrationText = "Satu dua tiga empat lima enam tujuh delapan sembilan sepuluh sebelas dua belas"
        )
        val videoPrompt = com.example.model.buildAutoVideoPrompt(project, scene, emptyList())
        assertTrue(videoPrompt.contains("10-second"))
        assertTrue(videoPrompt.contains("tracking shot"))
        assertTrue(videoPrompt.contains("no subtitles"))

        val sheet = formatScenePromptSheet(project, scene, emptyList())
        assertTrue(sheet.contains("SCENE 01"))
        assertTrue(sheet.contains("- Image Prompt:"))
        assertTrue(sheet.contains("- Video Prompt:"))

        val est = estimateNarrationSeconds(scene.narrationText)
        assertTrue(est >= 5)
        assertEquals("00:10–00:20", formatTimeRange(10, 10))
    }

    @Test
    fun backupJsonRoundTrip_preservesProjectCharactersAndScenes() {
        val bundle = listOf(
            BackupProject(
                project = ProjectEntity(
                    id = 1L,
                    title = "Sireum Boy",
                    genre = "ANIMASI",
                    aspectRatio = AspectRatioOption.RATIO_9_16.name,
                    visualStyle = VisualStyle.CARTOON_2D_IN_3D_ENV.name,
                    defaultSceneDurationSec = 10,
                    targetTotalDurationSec = 60,
                    dialogueLanguage = "INDONESIA",
                    customLanguageName = "",
                    voiceMode = VoiceMode.DIALOG_KARAKTER.name,
                    subtitlesEnabled = false,
                    storyIdea = "Mencari makanan",
                    storyScript = "SCENE 1: Pembuka",
                    projectReferenceImagePath = null,
                    voiceOverVolume = 1.0f,
                    dialogVolume = 1.0f,
                    bgmVolume = 0.50f,
                    sfxVolume = 0.50f,
                    voiceOverMuted = false,
                    dialogMuted = false,
                    bgmMuted = false,
                    sfxMuted = false,
                    createdAt = 1000L,
                    updatedAt = 2000L
                ),
                characters = listOf(
                    CharacterEntity(
                        id = 11L,
                        projectId = 1L,
                        characterCode = "CHR-002",
                        role = CharacterRole.PENDUKUNG.name,
                        name = "Kricik",
                        faceShape = "Oval kecil",
                        bodyColor = "Kuning cerah",
                        outfit = "Tas ransel mini",
                        bodySize = "Kecil",
                        eyeShape = "Bulat besar",
                        animationStyle = "Kartun 2D outline tinta hitam",
                        accessories = "Antena",
                        signatureTrait = "Selalu ceria",
                        description = "Semut kuning kecil",
                        photoPath = null,
                        bundledDrawableName = "img_char_kricik"
                    )
                ),
                scenes = listOf(
                    SceneEntity(
                        id = 21L,
                        projectId = 1L,
                        orderIndex = 0,
                        title = "Scene 1",
                        durationSec = 10,
                        visualDescription = "Visual",
                        characterPosition = "Tengah",
                        facialExpression = "Tersenyum",
                        characterAction = "Berlari gembira",
                        dialogueSpeaker = "Kricik",
                        dialogueText = "Halo",
                        narrationText = "Narasi",
                        cameraShot = "WIDE",
                        cameraMovement = "DOLLY_IN",
                        transition = "SMOOTH_CINEMATIC",
                        location = "Taman",
                        lighting = "Dramatis",
                        importantProps = "Kue",
                        mood = "Ceria",
                        musicSfx = "Musik",
                        soundFx = "Angin",
                        notes = "Catatan",
                        status = "BELUM_ADA_GAMBAR",
                        isApproved = true,
                        videoGeneratedAt = 0L,
                        characterIdsCsv = "11",
                        customPrompt = "",
                        customVideoPrompt = "",
                        isPromptManuallyEdited = false,
                        isVideoPromptManuallyEdited = false,
                        referenceImagePath = null,
                        variation1Path = null,
                        variation2Path = null,
                        selectedVariationIndex = 0,
                        isVariationPlaceholder = false
                    )
                )
            )
        )

        val json = BackupJsonHelper.serialize(bundle)
        val restored = BackupJsonHelper.deserialize(json)
        assertEquals(1, restored.projects.size)
        val p = restored.projects[0]
        assertEquals("Sireum Boy", p.project.title)
        assertFalse(p.project.subtitlesEnabled)
        assertEquals(0.50f, p.project.bgmVolume, 0.001f)
        assertEquals("CHR-002", p.characters[0].characterCode)
        assertEquals("Kricik", p.characters[0].name)
        assertTrue(p.scenes[0].isApproved)
        assertEquals("Scene 1", p.scenes[0].title)
    }
}
