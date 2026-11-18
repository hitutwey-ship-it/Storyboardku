package com.example.model

import kotlin.math.ceil

enum class ProjectGenre(val label: String) {
    ANIMASI("Animasi"),
    KARTUN("Kartun"),
    FILM_PENDEK("Film Pendek"),
    IKLAN("Iklan"),
    YOUTUBE_SHORTS("YouTube Shorts")
}

enum class AspectRatioOption(val label: String, val promptDesc: String, val ratioFloat: Float) {
    RATIO_16_9("16:9", "16:9 widescreen cinematic framing", 16f / 9f),
    RATIO_9_16("9:16", "9:16 vertical mobile portrait framing (1080x1920)", 9f / 16f)
}

enum class VisualStyle(val label: String, val promptDesc: String) {
    CARTOON_2D_IN_3D_ENV(
        "Kartun 2D + Lingkungan 3D",
        "2D cartoon characters with bold black ink outlines inside a rich 3D cinematic textured environment, dramatic lighting, shallow depth of field, smooth dynamic motion"
    ),
    PIXAR_3D(
        "3D Pixar",
        "3D Pixar animated film style, rich subsurface scattering, expressive stylized proportions, vibrant cinematic lighting, depth of field"
    ),
    CINEMATIC_3D(
        "3D Cinematic",
        "3D cinematic CGI animation style, dramatic rim lighting, detailed textures, depth of field"
    ),
    CARTOON_2D(
        "Kartun 2D",
        "2D hand-drawn cartoon animation style, crisp black ink outlines, vibrant cel shading"
    ),
    ANIME(
        "Anime",
        "high quality anime film style, expressive eyes, painted atmospheric backgrounds, cel-shaded lighting"
    ),
    REALISTIC(
        "Realistis",
        "photorealistic live-action film still, 35mm cinema lens, natural film grain, realistic lighting"
    )
}

enum class DialogueLanguage(val label: String) {
    INDONESIA("Indonesia"),
    SUNDA("Sunda"),
    INGGRIS("Inggris"),
    JAWA("Jawa"),
    LAINNYA("Bahasa Lainnya")
}

enum class VoiceMode(val label: String) {
    VOICE_OVER_NARRATOR("Voice Over Narator"),
    DIALOG_KARAKTER("Dialog Antar Karakter"),
    TANPA_SUARA("Tanpa Suara"),
    SUARA_KARAKTER_BERBEDA("Suara Karakter Berbeda")
}

enum class DurationPreset(val label: String, val seconds: Int) {
    SEC_30("30 Detik", 30),
    MIN_1("1 Menit", 60),
    MIN_3("3 Menit", 180),
    MIN_5("5 Menit", 300),
    MIN_10("10 Menit", 600),
    CUSTOM("Custom", -1)
}

enum class CharacterRole(val label: String) {
    UTAMA("Karakter Utama"),
    PENDUKUNG("Karakter Pendukung")
}

enum class CameraShotType(val label: String, val promptDesc: String) {
    WIDE("Wide Shot", "wide establishing shot"),
    MEDIUM("Medium Shot", "medium shot"),
    CLOSE_UP("Close-up", "close-up shot focusing on facial expression"),
    TRACKING("Tracking Shot", "dynamic tracking shot"),
    LOW_ANGLE("Low Angle", "dramatic low angle shot looking up")
}

enum class CameraMovement(val label: String, val videoPromptDesc: String) {
    DOLLY_IN("Dolly In", "smooth dolly-in push toward subject with anticipation and follow-through"),
    DOLLY_OUT("Dolly Out", "smooth dolly-out pull back revealing environment"),
    PAN_LEFT("Pan Left", "smooth horizontal pan left across the scene"),
    PAN_RIGHT("Pan Right", "smooth horizontal pan right across the scene"),
    TRACKING("Tracking Shot", "smooth tracking shot following lively character movement"),
    CRANE("Crane Shot", "sweeping cinematic crane shot"),
    CLOSE_UP("Close-up Push", "intimate close-up camera move capturing dynamic facial expression"),
    MEDIUM_SHOT("Medium Drift", "gentle medium-shot handheld cinematic drift"),
    WIDE_SHOT("Wide Sweep", "expansive wide-shot environmental reveal"),
    ORBIT("Orbit Camera", "smooth 3D orbit camera circling the character"),
    STATIC("Diam Halus", "subtle breathing cinematic camera hold")
}

enum class SceneTransition(val label: String, val videoDesc: String) {
    SMOOTH_CINEMATIC("Smooth Cinematic", "smooth cinematic dissolve transition"),
    CUT("Potong Langsung", "crisp hard cut transition"),
    FADE("Fade", "smooth fade transition"),
    SLIDE("Geser", "directional slide transition")
}

enum class SceneStatus(val label: String) {
    BELUM_ADA_GAMBAR("Belum Ada Gambar"),
    GAMBAR_JADI("Gambar Jadi"),
    PERLU_DIPERBAIKI("Perlu Diperbaiki")
}

enum class ThemeMode(val label: String) {
    DARK("Mode Gelap"),
    LIGHT("Mode Terang"),
    SYSTEM("Ikuti Sistem")
}

enum class TextScaleOption(val label: String, val multiplier: Float) {
    SMALL("Kecil", 0.90f),
    MEDIUM("Sedang", 1.00f),
    LARGE("Besar", 1.15f)
}

enum class StoryboardViewMode(val label: String) {
    GRID("Grid 2 Kolom"),
    LIST("Daftar"),
    TIMELINE("Timeline")
}

enum class MainTab(val label: String) {
    PROYEK("Dashboard"),
    KARAKTER("Karakter"),
    STORYBOARD("Storyboard"),
    STUDIO_VIDEO("Studio Video"),
    RIWAYAT("Riwayat"),
    PENGATURAN("Setelan")
}

data class AudioMixerSettings(
    val voiceOverVolume: Float = 1.0f,
    val dialogVolume: Float = 1.0f,
    val bgmVolume: Float = 0.50f,
    val sfxVolume: Float = 0.50f,
    val voiceOverMuted: Boolean = false,
    val dialogMuted: Boolean = false,
    val bgmMuted: Boolean = false,
    val sfxMuted: Boolean = false
)

data class ProjectUi(
    val id: Long = 0L,
    val title: String,
    val genre: ProjectGenre = ProjectGenre.ANIMASI,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
    val visualStyle: VisualStyle = VisualStyle.CARTOON_2D_IN_3D_ENV,
    val defaultSceneDurationSec: Int = 10,
    val targetTotalDurationSec: Int = 100,
    val dialogueLanguage: DialogueLanguage = DialogueLanguage.INDONESIA,
    val customLanguageName: String = "",
    val voiceMode: VoiceMode = VoiceMode.DIALOG_KARAKTER,
    val subtitlesEnabled: Boolean = false,
    val storyIdea: String = "",
    val storyScript: String = "",
    val projectReferenceImagePath: String? = null,
    val audioMixer: AudioMixerSettings = AudioMixerSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveLanguageLabel: String
        get() = if (dialogueLanguage == DialogueLanguage.LAINNYA && customLanguageName.isNotBlank()) {
            customLanguageName.trim()
        } else {
            dialogueLanguage.label
        }
}

data class CharacterUi(
    val id: Long = 0L,
    val projectId: Long,
    val characterCode: String = "CHR-001",
    val role: CharacterRole = CharacterRole.UTAMA,
    val name: String,
    val faceShape: String = "",
    val bodyColor: String = "",
    val outfit: String = "",
    val bodySize: String = "",
    val eyeShape: String = "",
    val animationStyle: String = "Kartun 2D outline tinta hitam",
    val accessories: String = "",
    val signatureTrait: String = "",
    val description: String = "",
    val photoPath: String? = null,
    val bundledDrawableName: String? = null
) {
    fun buildFullConsistencyDescription(): String {
        val structured = listOfNotNull(
            faceShape.takeIf { it.isNotBlank() }?.let { "face: $it" },
            bodyColor.takeIf { it.isNotBlank() }?.let { "colors: $it" },
            outfit.takeIf { it.isNotBlank() }?.let { "outfit: $it" },
            bodySize.takeIf { it.isNotBlank() }?.let { "body size: $it" },
            eyeShape.takeIf { it.isNotBlank() }?.let { "eyes: $it" },
            animationStyle.takeIf { it.isNotBlank() }?.let { "style: $it" },
            accessories.takeIf { it.isNotBlank() }?.let { "accessories: $it" },
            signatureTrait.takeIf { it.isNotBlank() }?.let { "signature trait: $it" }
        )
        return buildString {
            append("[$characterCode ${role.label}] $name")
            if (description.isNotBlank()) append(": ${description.trim()}")
            if (structured.isNotEmpty()) append(" (${structured.joinToString(", ")})")
        }
    }
}

data class SceneUi(
    val id: Long = 0L,
    val projectId: Long,
    val orderIndex: Int,
    val title: String,
    val durationSec: Int = 10,
    val visualDescription: String = "",
    val characterPosition: String = "Tengah frame, menghadap kamera 3/4",
    val facialExpression: String = "Ekspresif dan dinamis",
    val characterAction: String = "Gerakan hidup dengan anticipation, action, dan follow-through",
    val dialogueSpeaker: String = "",
    val dialogueText: String = "",
    val narrationText: String = "",
    val cameraShot: CameraShotType = CameraShotType.MEDIUM,
    val cameraMovement: CameraMovement = CameraMovement.DOLLY_IN,
    val transition: SceneTransition = SceneTransition.SMOOTH_CINEMATIC,
    val location: String = "",
    val lighting: String = "Pencahayaan sinematik dramatis dengan depth of field",
    val importantProps: String = "",
    val mood: String = "",
    val musicSfx: String = "",
    val soundFx: String = "",
    val notes: String = "",
    val status: SceneStatus = SceneStatus.BELUM_ADA_GAMBAR,
    val isApproved: Boolean = false,
    val videoGeneratedAt: Long = 0L,
    val characterIds: List<Long> = emptyList(),
    val customPrompt: String = "",
    val customVideoPrompt: String = "",
    val isPromptManuallyEdited: Boolean = false,
    val isVideoPromptManuallyEdited: Boolean = false,
    val referenceImagePath: String? = null,
    val variation1Path: String? = null,
    val variation2Path: String? = null,
    val selectedVariationIndex: Int = 0,
    val isVariationPlaceholder: Boolean = false
) {
    val activeImagePath: String?
        get() = when (selectedVariationIndex) {
            1 -> variation1Path ?: variation2Path
            2 -> variation2Path ?: variation1Path
            else -> variation1Path ?: variation2Path
        }
}

data class ProjectSummaryUi(
    val project: ProjectUi,
    val sceneCount: Int,
    val totalDurationSec: Int,
    val completedImageCount: Int,
    val approvedCount: Int,
    val characterCount: Int
)

data class AppSettings(
    val activeProjectId: Long = -1L,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val textScale: TextScaleOption = TextScaleOption.MEDIUM,
    val defaultStyle: VisualStyle = VisualStyle.CARTOON_2D_IN_3D_ENV,
    val defaultRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
    val defaultSceneDurationSec: Int = 10,
    val onboardingDismissed: Boolean = false
)

sealed interface SceneGenState {
    data object Idle : SceneGenState
    data object Loading : SceneGenState
    data class Error(val message: String) : SceneGenState
}

data class BatchGenState(
    val isRunning: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentSceneTitle: String = "",
    val errorMessage: String? = null
)

sealed interface RenderVideoState {
    data object Idle : RenderVideoState
    data class Rendering(
        val progress: Float,
        val statusText: String
    ) : RenderVideoState
    data class Completed(
        val mp4Path: String,
        val durationSec: Int,
        val width: Int,
        val height: Int,
        val fps: Int
    ) : RenderVideoState
    data class Error(val message: String) : RenderVideoState
}

fun estimateNarrationSeconds(narrationText: String): Int {
    val words = narrationText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return 0
    return ceil(words.size / 2.3).toInt().coerceAtLeast(1)
}

fun formatClockSeconds(seconds: Int): String {
    val clamped = seconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return "%02d:%02d".format(mins, secs)
}

fun formatTimeRange(startSec: Int, durationSec: Int): String {
    return "${formatClockSeconds(startSec)}–${formatClockSeconds(startSec + durationSec)}"
}

fun buildAutoImagePrompt(
    project: ProjectUi,
    scene: SceneUi,
    sceneCharacters: List<CharacterUi>
): String {
    val parts = mutableListOf<String>()
    parts += project.visualStyle.promptDesc
    parts += project.aspectRatio.promptDesc
    parts += scene.cameraShot.promptDesc
    parts += "lighting: ${scene.lighting.ifBlank { "dramatic cinematic lighting, depth of field" }}"

    if (scene.location.isNotBlank()) {
        parts += "3D textured environment: ${scene.location.trim()}"
    }
    if (scene.visualDescription.isNotBlank()) {
        parts += "scene visual action: ${scene.visualDescription.trim()}"
    }
    if (scene.characterPosition.isNotBlank()) {
        parts += "character placement: ${scene.characterPosition.trim()}"
    }
    if (scene.characterAction.isNotBlank()) {
        parts += "lively dynamic pose and motion: ${scene.characterAction.trim()}"
    }
    if (scene.facialExpression.isNotBlank()) {
        parts += "dynamic facial expression: ${scene.facialExpression.trim()}"
    }
    if (scene.importantProps.isNotBlank()) {
        parts += "key props: ${scene.importantProps.trim()}"
    }
    if (scene.dialogueText.isNotBlank()) {
        val speaker = scene.dialogueSpeaker.ifBlank { "character" }
        parts += "expression cue from spoken dialogue ($speaker in ${project.effectiveLanguageLabel}): \"${scene.dialogueText.trim()}\""
    }
    if (scene.mood.isNotBlank()) {
        parts += "mood: ${scene.mood.trim()}"
    }
    if (sceneCharacters.isNotEmpty()) {
        val charDescriptions = sceneCharacters.joinToString("; ") { it.buildFullConsistencyDescription() }
        parts += "locked consistent character designs -> [$charDescriptions]"
    }
    parts += "same character design in every scene, do not change the character, no subtitles, no on-screen text"
    return parts.joinToString(", ")
}

fun buildAutoVideoPrompt(
    project: ProjectUi,
    scene: SceneUi,
    sceneCharacters: List<CharacterUi>
): String {
    val charNames = if (sceneCharacters.isNotEmpty()) {
        sceneCharacters.joinToString(" and ") { "${it.name} (${it.characterCode})" }
    } else {
        "main characters"
    }
    val action = scene.characterAction.ifBlank { scene.visualDescription.ifBlank { scene.title } }
    val loc = if (scene.location.isNotBlank()) " in ${scene.location.trim()}" else ""
    val expr = if (scene.facialExpression.isNotBlank()) ", dynamic facial expression: ${scene.facialExpression.trim()}" else ""
    val light = if (scene.lighting.isNotBlank()) ", ${scene.lighting.trim()}" else ""
    val subtitleRule = if (project.subtitlesEnabled) "with clean optional subtitles" else "no subtitles, no on-screen text"

    return "${scene.durationSec}-second ${project.visualStyle.label} video clip (${project.aspectRatio.label}, 30fps), " +
        "${scene.cameraShot.promptDesc} with ${scene.cameraMovement.videoPromptDesc}$loc$light. " +
        "$charNames performing lively motion with anticipation, action, and follow-through: $action$expr. " +
        "Ending with ${scene.transition.videoDesc}. " +
        "Keep exact Character ID consistency, $subtitleRule."
}

fun formatScenePromptSheet(
    project: ProjectUi,
    scene: SceneUi,
    sceneCharacters: List<CharacterUi>
): String {
    val num = "%02d".format(scene.orderIndex + 1)
    val charStr = sceneCharacters.joinToString(", ") { "${it.characterCode} ${it.name}" }.ifBlank { "-" }
    val imgPrompt = if (scene.isPromptManuallyEdited && scene.customPrompt.isNotBlank()) {
        scene.customPrompt
    } else {
        buildAutoImagePrompt(project, scene, sceneCharacters)
    }
    val vidPrompt = if (scene.isVideoPromptManuallyEdited && scene.customVideoPrompt.isNotBlank()) {
        scene.customVideoPrompt
    } else {
        buildAutoVideoPrompt(project, scene, sceneCharacters)
    }
    val dlg = if (scene.dialogueText.isBlank()) "-" else "${scene.dialogueSpeaker}: \"${scene.dialogueText}\""

    return """
        |SCENE $num — ${scene.title}
        |- Durasi: ${scene.durationSec} detik
        |- Visual: ${scene.visualDescription.ifBlank { "-" }}
        |- Karakter: $charStr (Posisi: ${scene.characterPosition})
        |- Action: ${scene.characterAction}
        |- Expression: ${scene.facialExpression}
        |- Camera: ${scene.cameraShot.label} • ${scene.cameraMovement.label}
        |- Lighting: ${scene.lighting}
        |- Environment: ${scene.location.ifBlank { "-" }}
        |- Dialogue (${project.effectiveLanguageLabel}): $dlg
        |- Sound FX: ${scene.soundFx.ifBlank { "-" }}
        |- Music: ${scene.musicSfx.ifBlank { "-" }}
        |- Image Prompt: $imgPrompt
        |- Video Prompt: $vidPrompt
    """.trimMargin()
}

fun hasIncompleteCharacters(sceneCharacters: List<CharacterUi>): Boolean {
    if (sceneCharacters.isEmpty()) return true
    return sceneCharacters.any {
        it.description.trim().length < 15 &&
            it.outfit.isBlank() &&
            it.faceShape.isBlank()
    }
}
