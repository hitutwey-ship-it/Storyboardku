package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AspectRatioOption
import com.example.model.AudioMixerSettings
import com.example.model.CameraMovement
import com.example.model.ProjectUi
import com.example.model.RenderVideoState
import com.example.model.SceneUi
import com.example.model.formatClockSeconds
import com.example.model.formatTimeRange
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StudioVideoTab(
    project: ProjectUi?,
    scenes: List<SceneUi>,
    renderState: RenderVideoState,
    listState: LazyListState,
    onUpdateMixer: (AudioMixerSettings) -> Unit,
    onPlaySceneAudio: (SceneUi) -> Unit,
    onStopAudio: () -> Unit,
    onApproveAllScenes: () -> Unit,
    onEditScene: (SceneUi) -> Unit,
    onRenderFinalVideo: () -> Unit,
    onCancelRender: () -> Unit,
    onDownloadMp4Document: () -> Unit,
    onSaveMp4Gallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (project == null) return

    var currentSceneIndex by remember(project.id, scenes.size) { mutableIntStateOf(0) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var sceneProgress by remember { mutableFloatStateOf(0f) }
    var playingMp4Path by remember { mutableStateOf<String?>(null) }
    var showUnapprovedConfirmDialog by remember { mutableStateOf(false) }

    val safeIndex = currentSceneIndex.coerceIn(0, (scenes.size - 1).coerceAtLeast(0))
    val currentScene = scenes.getOrNull(safeIndex)

    // Sequentially animate preview across all scenes
    LaunchedEffect(isPlayingPreview, safeIndex, scenes) {
        if (!isPlayingPreview || scenes.isEmpty()) return@LaunchedEffect
        val scene = scenes.getOrNull(safeIndex) ?: return@LaunchedEffect
        onPlaySceneAudio(scene)
        val totalMs = (scene.durationSec * 1000L).coerceAtLeast(3000L)
        val stepMs = 33L
        while (isPlayingPreview) {
            delay(stepMs)
            sceneProgress += stepMs.toFloat() / totalMs
            if (sceneProgress >= 1f) {
                sceneProgress = 0f
                if (safeIndex < scenes.lastIndex) {
                    currentSceneIndex = safeIndex + 1
                    break
                } else {
                    currentSceneIndex = 0
                    isPlayingPreview = false
                    onStopAudio()
                    break
                }
            }
        }
    }

    val startTimes = remember(scenes) {
        var acc = 0
        scenes.map { s ->
            val st = acc
            acc += s.durationSec
            st
        }
    }
    val totalDurationSec = scenes.sumOf { it.durationSec }
    val approvedCount = scenes.count { it.isApproved }
    val mixer = project.audioMixer

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Video Preview Player
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Video Preview Player", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = currentScene?.let {
                                    "SCENE ${"%02d".format(it.orderIndex + 1)}: ${it.title} • ${it.cameraMovement.label}"
                                } ?: "Belum ada scene",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = project.aspectRatio.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val frameRatio = if (project.aspectRatio == AspectRatioOption.RATIO_9_16) 9f / 16f else 16f / 9f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(frameRatio)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF12151C))
                        ) {
                            if (currentScene != null) {
                                val eased = if (sceneProgress < 0.5f) {
                                    4f * sceneProgress * sceneProgress * sceneProgress
                                } else {
                                    1f - (-2f * sceneProgress + 2f).let { it * it * it } / 2f
                                }
                                val (scale, tx, ty) = computePreviewTransform(currentScene.cameraMovement, eased)
                                val file = currentScene.activeImagePath?.let { File(it) }?.takeIf { it.exists() }
                                val animMod = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = tx
                                        translationY = ty
                                    }
                                if (file != null) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Preview video scene",
                                        modifier = animMod,
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.img_scene_placeholder),
                                        contentDescription = "Placeholder preview video",
                                        modifier = animMod,
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Top HUD
                                Surface(
                                    color = Color(0xAA12151C),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                ) {
                                    Text(
                                        text = "SCENE ${"%02d".format(currentScene.orderIndex + 1)} • ${currentScene.cameraMovement.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Subtitles strictly ONLY if user enabled subtitlesEnabled
                                if (project.subtitlesEnabled) {
                                    val subText = currentScene.dialogueText.ifBlank { currentScene.narrationText }
                                    if (subText.isNotBlank()) {
                                        Surface(
                                            color = Color(0xCC12151C),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = subText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (scenes.isNotEmpty()) {
                        val elapsedSec = (startTimes.getOrElse(safeIndex) { 0 } +
                            ((currentScene?.durationSec ?: 0) * sceneProgress).roundToInt())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatClockSeconds(elapsedSec)} / ${formatClockSeconds(totalDurationSec)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Transisi: ${currentScene?.transition?.label ?: "-"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = {
                                if (totalDurationSec > 0) elapsedSec.toFloat() / totalDurationSec else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (safeIndex > 0) {
                                        currentSceneIndex = safeIndex - 1
                                        sceneProgress = 0f
                                    }
                                },
                                enabled = safeIndex > 0
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Scene Sebelumnya")
                            }

                            Button(
                                onClick = {
                                    isPlayingPreview = !isPlayingPreview
                                    if (!isPlayingPreview) onStopAudio()
                                },
                                modifier = Modifier.weight(1f).testTag("video_preview_play_btn")
                            ) {
                                Icon(
                                    if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPlayingPreview) "Jeda Preview" else "Putar Semua Scene")
                            }

                            IconButton(
                                onClick = {
                                    if (safeIndex < scenes.lastIndex) {
                                        currentSceneIndex = safeIndex + 1
                                        sceneProgress = 0f
                                    }
                                },
                                enabled = safeIndex < scenes.lastIndex
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Scene Berikutnya")
                            }
                        }
                    }
                }
            }
        }

        // 2. Timeline Editor
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Timeline Editor (${scenes.size} Scene)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Ketuk blok timeline untuk melompat ke scene atau mengedit durasi & gerakan kamera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenes.forEachIndexed { idx, scene ->
                            val selected = idx == safeIndex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.primary
                                        scene.isApproved -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier
                                    .width(152.dp)
                                    .clickable {
                                        currentSceneIndex = idx
                                        sceneProgress = 0f
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SCENE ${"%02d".format(scene.orderIndex + 1)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (scene.isApproved) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Disetujui",
                                                tint = EmeraldSuccess,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = scene.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatTimeRange(startTimes.getOrElse(idx) { 0 }, scene.durationSec)} (${scene.durationSec}s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = scene.cameraMovement.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onEditScene(scene) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Scene", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Sistem Audio 4 Track Terpisah
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Audio Studio (4 Track Terpisah)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Mode Suara: ${project.voiceMode.label} (${project.effectiveLanguageLabel}). Musik latar & efek suara default 50% dari level suara narasi tanpa mengubah durasi rekaman suara asli.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AudioTrackSliderRow(
                        label = "Voice Over / Narasi",
                        volume = mixer.voiceOverVolume,
                        muted = mixer.voiceOverMuted,
                        onVolumeChange = { onUpdateMixer(mixer.copy(voiceOverVolume = it)) },
                        onToggleMute = { onUpdateMixer(mixer.copy(voiceOverMuted = !mixer.voiceOverMuted)) }
                    )
                    AudioTrackSliderRow(
                        label = "Dialog Karakter",
                        volume = mixer.dialogVolume,
                        muted = mixer.dialogMuted,
                        onVolumeChange = { onUpdateMixer(mixer.copy(dialogVolume = it)) },
                        onToggleMute = { onUpdateMixer(mixer.copy(dialogMuted = !mixer.dialogMuted)) }
                    )
                    AudioTrackSliderRow(
                        label = "Background Music (Default 50%)",
                        volume = mixer.bgmVolume,
                        muted = mixer.bgmMuted,
                        onVolumeChange = { onUpdateMixer(mixer.copy(bgmVolume = it)) },
                        onToggleMute = { onUpdateMixer(mixer.copy(bgmMuted = !mixer.bgmMuted)) }
                    )
                    AudioTrackSliderRow(
                        label = "Sound Effects (Default 50%)",
                        volume = mixer.sfxVolume,
                        muted = mixer.sfxMuted,
                        onVolumeChange = { onUpdateMixer(mixer.copy(sfxVolume = it)) },
                        onToggleMute = { onUpdateMixer(mixer.copy(sfxMuted = !mixer.sfxMuted)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentScene?.let(onPlaySceneAudio) },
                            enabled = currentScene != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Uji Audio Scene", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onStopAudio,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hentikan Audio", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 4. Render Video Final MP4 (1080x1920 30 FPS)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MovieCreation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Render Final Video (MP4)", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = "Resolusi target: 1080×1920 (9:16 Vertikal) • 30 FPS • Subtitle: ${if (project.subtitlesEnabled) "Aktif" else "Nonaktif (Default)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (approvedCount == scenes.size && scenes.isNotEmpty()) {
                            EmeraldSuccess.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Persetujuan: $approvedCount / ${scenes.size} scene Disetujui",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (approvedCount == scenes.size && scenes.isNotEmpty()) {
                                    EmeraldSuccess
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (approvedCount < scenes.size) {
                                TextButton(onClick = onApproveAllScenes) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Setujui Semua")
                                }
                            }
                        }
                    }

                    when (renderState) {
                        is RenderVideoState.Idle -> {
                            Button(
                                onClick = {
                                    if (approvedCount < scenes.size) {
                                        showUnapprovedConfirmDialog = true
                                    } else {
                                        onRenderFinalVideo()
                                    }
                                },
                                enabled = scenes.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("render_final_video_btn")
                            ) {
                                Icon(Icons.Default.MovieCreation, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Render Final Video MP4")
                            }
                        }

                        is RenderVideoState.Rendering -> {
                            Text(
                                text = "${(renderState.progress * 100).roundToInt()}% — ${renderState.statusText}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { renderState.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = onCancelRender,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Batal Render")
                            }
                        }

                        is RenderVideoState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = renderState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Button(
                                onClick = onRenderFinalVideo,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Coba Render Lagi")
                            }
                        }

                        is RenderVideoState.Completed -> {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Video MP4 Siap! (${renderState.width}×${renderState.height} • ${renderState.fps} FPS • ${renderState.durationSec} detik)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }

                            Button(
                                onClick = { playingMp4Path = renderState.mp4Path },
                                modifier = Modifier.fillMaxWidth().testTag("preview_full_video_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Preview Full Video")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDownloadMp4Document,
                                    modifier = Modifier.weight(1f).testTag("download_mp4_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Download MP4", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onSaveMp4Gallery,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simpan Galeri", maxLines = 1)
                                }
                            }

                            TextButton(
                                onClick = onRenderFinalVideo,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Render Ulang Video MP4")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnapprovedConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnapprovedConfirmDialog = false },
            title = { Text("Beberapa Scene Belum Disetujui") },
            text = {
                Text(
                    "Baru $approvedCount dari ${scenes.size} scene yang berstatus Disetujui (Approved). Anda bisa menyetujui semua scene terlebih dahulu atau langsung merender video MP4."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnapprovedConfirmDialog = false
                        onApproveAllScenes()
                        onRenderFinalVideo()
                    }
                ) { Text("Setujui Semua & Render") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showUnapprovedConfirmDialog = false }) { Text("Batal") }
                    TextButton(
                        onClick = {
                            showUnapprovedConfirmDialog = false
                            onRenderFinalVideo()
                        }
                    ) { Text("Render Tetap Jalan") }
                }
            }
        )
    }

    playingMp4Path?.let { mp4Path ->
        Mp4PlayerDialog(
            mp4Path = mp4Path,
            onDismiss = { playingMp4Path = null }
        )
    }
}

@Composable
private fun AudioTrackSliderRow(
    label: String,
    volume: Float,
    muted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onToggleMute, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (muted) Icons.Default.VolumeOff else Icons.Default.MusicNote,
                        contentDescription = if (muted) "Aktifkan $label" else "Bisukan $label",
                        tint = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = if (muted) "Mute" else "${(volume * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = if (muted) 0f else volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun Mp4PlayerDialog(
    mp4Path: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0E1117)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview Full Video (MP4)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Video", tint = Color.White)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(Uri.fromFile(File(mp4Path)))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = false
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

private fun computePreviewTransform(movement: CameraMovement, eased: Float): Triple<Float, Float, Float> {
    return when (movement) {
        CameraMovement.DOLLY_IN, CameraMovement.CLOSE_UP -> Triple(1.0f + 0.16f * eased, 0f, 0f)
        CameraMovement.DOLLY_OUT, CameraMovement.WIDE_SHOT -> Triple(1.16f - 0.14f * eased, 0f, 0f)
        CameraMovement.PAN_LEFT -> Triple(1.08f, 22f - 44f * eased, 0f)
        CameraMovement.PAN_RIGHT -> Triple(1.08f, -22f + 44f * eased, 0f)
        CameraMovement.TRACKING -> Triple(1.10f, -20f + 40f * eased, -6f * sin(eased * PI).toFloat())
        CameraMovement.CRANE -> Triple(1.10f, 0f, 20f - 40f * eased)
        CameraMovement.ORBIT -> Triple(1.12f, 18f * sin(eased * 2 * PI).toFloat(), 8f * sin(eased * 2 * PI).toFloat())
        CameraMovement.MEDIUM_SHOT, CameraMovement.STATIC -> Triple(1.02f + 0.04f * eased, 0f, 0f)
    }
}
