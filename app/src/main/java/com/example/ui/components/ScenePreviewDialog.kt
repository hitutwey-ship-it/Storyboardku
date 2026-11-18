package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AspectRatioOption
import com.example.model.CameraMovement
import com.example.model.ProjectUi
import com.example.model.SceneUi
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ScenePreviewDialog(
    project: ProjectUi,
    scene: SceneUi,
    onPlayAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, scene.id) {
        if (!isPlaying) return@LaunchedEffect
        val totalMs = (scene.durationSec * 1000L).coerceAtLeast(3000L)
        val stepMs = 33L
        while (isPlaying) {
            delay(stepMs)
            progress += stepMs.toFloat() / totalMs
            if (progress >= 1f) progress = 0f
        }
    }

    val eased = if (progress < 0.5f) 4f * progress * progress * progress else 1f - (-2f * progress + 2f).let { it * it * it } / 2f
    val (scale, tx, ty) = when (scene.cameraMovement) {
        CameraMovement.DOLLY_IN, CameraMovement.CLOSE_UP -> Triple(1.0f + 0.18f * eased, 0f, 0f)
        CameraMovement.DOLLY_OUT, CameraMovement.WIDE_SHOT -> Triple(1.18f - 0.16f * eased, 0f, 0f)
        CameraMovement.PAN_LEFT -> Triple(1.10f, 26f - 52f * eased, 0f)
        CameraMovement.PAN_RIGHT -> Triple(1.10f, -26f + 52f * eased, 0f)
        CameraMovement.TRACKING -> Triple(1.12f, -22f + 44f * eased, -6f * sin(eased * PI).toFloat())
        CameraMovement.CRANE -> Triple(1.10f, 0f, 22f - 44f * eased)
        CameraMovement.ORBIT -> Triple(1.14f, 20f * sin(eased * 2 * PI).toFloat(), 10f * sin(eased * 2 * PI).toFloat())
        CameraMovement.MEDIUM_SHOT, CameraMovement.STATIC -> Triple(1.02f + 0.04f * eased, 0f, 0f)
    }

    Dialog(
        onDismissRequest = {
            onStopAudio()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preview Scene ${"%02d".format(scene.orderIndex + 1)}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${scene.cameraMovement.label} • ${scene.durationSec} dtk • ${scene.transition.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        onStopAudio()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup Preview")
                    }
                }

                val frameRatio = if (project.aspectRatio == AspectRatioOption.RATIO_9_16) 9f / 16f else 16f / 9f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(frameRatio)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF12151C))
                    ) {
                        val file = scene.activeImagePath?.let { File(it) }?.takeIf { it.exists() }
                        val animModifier = Modifier
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
                                contentDescription = "Animated preview",
                                modifier = animModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.img_scene_placeholder),
                                contentDescription = "Placeholder animated preview",
                                modifier = animModifier,
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Motion indicator badge
                        Surface(
                            color = Color(0xAA12151C),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        ) {
                            Text(
                                text = "Gerakan: ${scene.cameraMovement.label} • ${scene.characterAction.take(30)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Subtitles shown ONLY when user enabled subtitlesEnabled
                        if (project.subtitlesEnabled) {
                            val sub = scene.dialogueText.ifBlank { scene.narrationText }
                            if (sub.isNotBlank()) {
                                Surface(
                                    color = Color(0xCC12151C),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPlaying) "Jeda" else "Putar")
                    }
                    Button(
                        onClick = onPlayAudio,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Putar Audio")
                    }
                }
            }
        }
    }
}
