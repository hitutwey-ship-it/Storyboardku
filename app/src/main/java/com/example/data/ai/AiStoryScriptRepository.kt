package com.example.data.ai

import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterUi
import com.example.model.ProjectUi
import com.example.model.SceneStatus
import com.example.model.SceneTransition
import com.example.model.SceneUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

interface AiStoryScriptRepository {
    val isRealAiEnabled: Boolean
    suspend fun generateScriptFromIdea(
        idea: String,
        project: ProjectUi,
        characters: List<CharacterUi>
    ): String

    suspend fun generateScenesFromScript(
        project: ProjectUi,
        script: String,
        characters: List<CharacterUi>,
        existingApprovedScenes: List<SceneUi>
    ): List<SceneUi>
}

/**
 * Local story-to-script and script-to-storyboard engine used when Firebase AI Logic
 * is not enabled. Splits story ideas/scripts into structured scenes and divides total
 * target duration cleanly while never modifying approved scenes.
 */
class LocalStoryScriptRepository : AiStoryScriptRepository {

    override val isRealAiEnabled: Boolean = false

    override suspend fun generateScriptFromIdea(
        idea: String,
        project: ProjectUi,
        characters: List<CharacterUi>
    ): String = withContext(Dispatchers.Default) {
        delay(400L)
        val cleanIdea = idea.trim().ifBlank { "Petualangan seru para karakter utama mengejar impian mereka." }
        val mainChar = characters.firstOrNull()?.name ?: "Tokoh Utama"
        val sideChar = characters.getOrNull(1)?.name ?: "Sahabat"
        val targetCount = (project.targetTotalDurationSec / project.defaultSceneDurationSec.coerceAtLeast(5))
            .coerceIn(3, 12)

        val beats = listOf(
            "Pembukaan: Memperkenalkan $mainChar di dunia sehari-harinya saat sebuah masalah muncul dari ide: $cleanIdea",
            "Pemicu Masalah: $mainChar dan $sideChar menyadari tantangan besar yang harus segera diatasi.",
            "Persiapan: $mainChar menyusun rencana pintar dengan penuh semangat bersama $sideChar.",
            "Perjalanan: Mereka melangkah menuju lokasi baru yang penuh kejutan visual dan rintangan.",
            "Konflik Memuncak: Rintangan terbesar menghadang dan menguji kerja sama $mainChar dan $sideChar.",
            "Titik Balik: Dengan kecerdikan dan keberanian, $mainChar menemukan solusi tak terduga.",
            "Aksi Penyelesaian: $mainChar dan $sideChar bergerak serempak menyelesaikan masalah.",
            "Perayaan & Hasil: Suasana hangat penuh kelegaan dan kegembiraan setelah misi berhasil.",
            "Penutup & Harapan: $mainChar menatap cakrawala baru, siap menghadapi petualangan berikutnya."
        )

        (0 until targetCount).joinToString("\n") { idx ->
            val beatText = if (idx < beats.size) {
                beats[idx]
            } else {
                "Lanjutan Aksi ${idx + 1}: $mainChar dan $sideChar melanjutkan momen penting cerita ($cleanIdea)."
            }
            "SCENE ${idx + 1}: $beatText"
        }
    }

    override suspend fun generateScenesFromScript(
        project: ProjectUi,
        script: String,
        characters: List<CharacterUi>,
        existingApprovedScenes: List<SceneUi>
    ): List<SceneUi> = withContext(Dispatchers.Default) {
        delay(450L)

        val rawLines = script.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val blocks = if (rawLines.isNotEmpty()) {
            rawLines
        } else {
            val baseIdea = project.storyIdea.trim().ifBlank { project.title }
            listOf(
                "SCENE 1: Pembukaan cerita — $baseIdea",
                "SCENE 2: Konflik atau tantangan utama muncul di tengah cerita.",
                "SCENE 3: Aksi klimaks penuh gerakan dinamis dari karakter utama.",
                "SCENE 4: Penutup cerita dengan pesan hangat dan harapan baru."
            )
        }

        val approvedDuration = existingApprovedScenes.sumOf { it.durationSec }
        val remainingTarget = (project.targetTotalDurationSec - approvedDuration).coerceAtLeast(blocks.size * 4)
        val perSceneDur = (remainingTarget.toFloat() / blocks.size).roundToInt().coerceIn(4, 60)

        val shots = listOf(
            CameraShotType.WIDE, CameraShotType.MEDIUM, CameraShotType.LOW_ANGLE,
            CameraShotType.TRACKING, CameraShotType.CLOSE_UP
        )
        val moves = listOf(
            CameraMovement.DOLLY_IN, CameraMovement.TRACKING, CameraMovement.PAN_RIGHT,
            CameraMovement.ORBIT, CameraMovement.CRANE, CameraMovement.DOLLY_OUT
        )
        val transitions = listOf(
            SceneTransition.SMOOTH_CINEMATIC, SceneTransition.FADE, SceneTransition.SLIDE
        )

        val allCharIds = characters.map { it.id }
        val startOrder = existingApprovedScenes.size

        blocks.mapIndexed { idx, raw ->
            val cleaned = raw.replace(Regex("^(SCENE|ADEGAN)\\s*\\d+\\s*[:\\-.]?\\s*", RegexOption.IGNORE_CASE), "").trim()
            val titleParts = cleaned.split(".", ":", "—", "-").firstOrNull()?.trim().orEmpty()
            val title = titleParts.take(44).ifBlank { "Scene ${startOrder + idx + 1}" }
            val speaker = characters.getOrNull(idx % characters.size.coerceAtLeast(1))?.name ?: "Narator"

            val durationForThis = if (idx == blocks.lastIndex) {
                val usedSoFar = perSceneDur * (blocks.size - 1)
                (remainingTarget - usedSoFar).coerceIn(4, 90)
            } else {
                perSceneDur
            }

            SceneUi(
                id = 0L,
                projectId = project.id,
                orderIndex = startOrder + idx,
                title = title,
                durationSec = durationForThis,
                visualDescription = cleaned,
                characterPosition = if (idx % 2 == 0) "Tengah frame, posisi 3/4 dinamis" else "Kiri depan bergerak menuju kanan",
                facialExpression = if (idx == blocks.lastIndex) "Tersenyum penuh optimisme" else "Ekspresif dan fokus pada aksi",
                characterAction = "Gerakan lincah dengan anticipation sebelum aksi utama dan follow-through alami",
                dialogueSpeaker = speaker,
                dialogueText = cleaned.take(90),
                narrationText = cleaned,
                cameraShot = shots[idx % shots.size],
                cameraMovement = moves[idx % moves.size],
                transition = transitions[idx % transitions.size],
                location = "Lingkungan 3D sinematik sesuai naskah Scene ${startOrder + idx + 1}",
                lighting = "Pencahayaan dramatis sinematik dengan rim light dan depth of field",
                importantProps = "Properti kunci sesuai adegan",
                mood = if (idx % 2 == 0) "Dinamis dan penuh energi" else "Dramatis dan emosional",
                musicSfx = "Musik latar sinematik menyesuaikan ritme adegan",
                soundFx = "Efek suara langkah, angin, dan interaksi objek",
                notes = "Hasil pecah naskah otomatis — dapat diedit sebelum Approve Scene.",
                status = SceneStatus.BELUM_ADA_GAMBAR,
                isApproved = false,
                characterIds = allCharIds
            )
        }
    }
}
