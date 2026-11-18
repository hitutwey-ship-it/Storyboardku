package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CharacterUi
import com.example.model.ProjectUi
import com.example.model.SceneGenState
import com.example.model.SceneStatus
import com.example.model.SceneUi
import com.example.model.buildAutoImagePrompt
import com.example.model.buildAutoVideoPrompt
import com.example.model.estimateNarrationSeconds
import com.example.model.formatScenePromptSheet
import com.example.model.formatTimeRange
import com.example.model.hasIncompleteCharacters
import com.example.ui.components.AccordionSection
import com.example.ui.components.SceneThumbnailBox
import com.example.ui.theme.EmeraldSuccess
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneCard(
    project: ProjectUi,
    scene: SceneUi,
    startSeconds: Int,
    totalScenes: Int,
    characters: List<CharacterUi>,
    genState: SceneGenState,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRequestDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onGenerateImages: () -> Unit,
    onRegenerateVideo: () -> Unit,
    onPreviewScene: () -> Unit,
    onToggleApprove: () -> Unit,
    onSelectVariation: (Int) -> Unit,
    onUpdatePrompt: (String, Boolean) -> Unit,
    onUpdateVideoPrompt: (String, Boolean) -> Unit,
    onUpdateStatus: (SceneStatus) -> Unit,
    onImportImage: (Uri, Boolean) -> Unit,
    onCopyPrompt: (String, String) -> Unit,
    onOpenExternalGenerator: (String) -> Unit,
    onSaveToGallery: () -> Unit,
    onViewFullscreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandDetails by rememberSaveable(scene.id) { mutableStateOf(false) }
    var expandPrompts by rememberSaveable(scene.id) { mutableStateOf(false) }
    var expandVariations by rememberSaveable(scene.id) { mutableStateOf(false) }

    val sceneChars = remember(characters, scene.characterIds) {
        characters.filter { it.id in scene.characterIds }
    }
    val autoPrompt = remember(project, scene, sceneChars) {
        buildAutoImagePrompt(project, scene, sceneChars)
    }
    val effectivePrompt = if (scene.isPromptManuallyEdited && scene.customPrompt.isNotBlank()) {
        scene.customPrompt
    } else {
        autoPrompt
    }
    val autoVideoPrompt = remember(project, scene, sceneChars) {
        buildAutoVideoPrompt(project, scene, sceneChars)
    }
    val effectiveVideoPrompt = if (scene.isVideoPromptManuallyEdited && scene.customVideoPrompt.isNotBlank()) {
        scene.customVideoPrompt
    } else {
        autoVideoPrompt
    }
    val formattedSheet = remember(project, scene, sceneChars) {
        formatScenePromptSheet(project, scene, sceneChars)
    }

    var promptDraft by remember(scene.id, effectivePrompt) { mutableStateOf(effectivePrompt) }
    var videoPromptDraft by remember(scene.id, effectiveVideoPrompt) { mutableStateOf(effectiveVideoPrompt) }
    var importAsReference by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportImage(uri, importAsReference)
    }

    val estNarrationSec = remember(scene.narrationText) { estimateNarrationSeconds(scene.narrationText) }
    val narrationOverrun = estNarrationSec > scene.durationSec
    val charIncomplete = remember(sceneChars) { hasIncompleteCharacters(sceneChars) }
    val canGenerate = scene.visualDescription.isNotBlank() && genState !is SceneGenState.Loading && !scene.isApproved

    val sceneNumStr = "%02d".format(scene.orderIndex + 1)

    Card(
        modifier = modifier.fillMaxWidth().testTag("scene_card_${scene.orderIndex + 1}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (scene.isApproved) 2.dp else 1.dp,
            color = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: SCENE 01 + Title + Timecode + Move Arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "SCENE $sceneNumStr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scene.title.ifBlank { "Tanpa Judul" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${scene.durationSec} dtk • ${formatTimeRange(startSeconds, scene.durationSec)} • ${scene.cameraMovement.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onMoveUp,
                    enabled = scene.orderIndex > 0,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Pindah ke atas")
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = scene.orderIndex < totalScenes - 1,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Pindah ke bawah")
                }
            }

            if (scene.isApproved) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccess.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Scene Disetujui & Terkunci — tidak akan diubah otomatis tanpa persetujuan Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            // Thumbnail
            SceneThumbnailBox(
                scene = scene,
                aspectRatio = project.aspectRatio,
                genState = genState,
                onRetry = onGenerateImages,
                onClickImage = { scene.activeImagePath?.let(onViewFullscreen) }
            )

            // Key Storyboard Breakdown
            Text(
                text = scene.visualDescription.ifBlank { "Deskripsi visual belum diisi — tekan Edit untuk mengisi." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (scene.visualDescription.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            if (scene.dialogueText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${scene.dialogueSpeaker.ifBlank { "Dialog" }}: \"${scene.dialogueText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }
            }

            if (scene.narrationText.isNotBlank()) {
                Text(
                    text = "Narasi (~${estNarrationSec}d): ${scene.narrationText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (narrationOverrun) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Perkiraan waktu baca narasi (~$estNarrationSec dtk) melebihi durasi scene (${scene.durationSec} dtk).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (scene.visualDescription.isBlank()) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Isi deskripsi visual scene dulu sebelum membuat gambar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (charIncomplete) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Karakter belum lengkap — gambar bisa berubah-ubah (tetap bisa lanjut).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 6 Required Scene Action Buttons (Section 6)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onGenerateImages,
                    enabled = canGenerate,
                    modifier = Modifier.testTag("generate_scene_${scene.orderIndex + 1}")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (scene.activeImagePath == null) "Generate Scene" else "Regenerate Image")
                }

                OutlinedButton(
                    onClick = onRegenerateVideo,
                    enabled = !scene.isApproved
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Regenerate Video")
                }

                OutlinedButton(onClick = { expandPrompts = !expandPrompts }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Prompt")
                }

                OutlinedButton(onClick = onPreviewScene) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview Scene")
                }

                Button(
                    onClick = onToggleApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        if (scene.isApproved) Icons.Default.LockOpen else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (scene.isApproved) "Buka Kunci" else "Approve Scene")
                }

                OutlinedButton(
                    onClick = {
                        importAsReference = false
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Impor")
                }

                if (scene.activeImagePath != null) {
                    IconButton(onClick = onSaveToGallery) {
                        Icon(Icons.Default.Download, contentDescription = "Simpan ke Galeri")
                    }
                }
            }

            // Status Switcher
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SceneStatus.entries.forEach { st ->
                    FilterChip(
                        selected = scene.status == st,
                        onClick = { onUpdateStatus(st) },
                        label = { Text(st.label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Scene")
                }
                TextButton(onClick = onDuplicate) {
                    Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Duplikat")
                }
                TextButton(onClick = onRequestDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }

            // Accordion 1: Format Lengkap SCENE 01 & Detail Produksi
            AccordionSection(
                title = "Format SCENE $sceneNumStr & Detail Aksi",
                subtitle = "${scene.location.ifBlank { "Lokasi belum diisi" }} • ${scene.lighting}",
                icon = Icons.Default.Videocam,
                expanded = expandDetails,
                onToggle = { expandDetails = !expandDetails }
            ) {
                DetailRow("Durasi", "${scene.durationSec} detik")
                DetailRow("Karakter", sceneChars.joinToString(", ") { "${it.characterCode} ${it.name}" })
                DetailRow("Posisi Karakter", scene.characterPosition)
                DetailRow("Action (Gerakan)", scene.characterAction)
                DetailRow("Expression (Ekspresi)", scene.facialExpression)
                DetailRow("Camera", "${scene.cameraShot.label} • ${scene.cameraMovement.label}")
                DetailRow("Lighting", scene.lighting)
                DetailRow("Environment", scene.location)
                DetailRow("Sound FX", scene.soundFx)
                DetailRow("Music", scene.musicSfx)
                DetailRow("Transisi", scene.transition.label)
                DetailRow("Properti", scene.importantProps)
                if (scene.notes.isNotBlank()) DetailRow("Catatan", scene.notes)
                OutlinedButton(
                    onClick = { onCopyPrompt("Format SCENE $sceneNumStr", formattedSheet) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Format SCENE $sceneNumStr Lengkap")
                }
            }

            // Accordion 2: Prompt Editor (Image Prompt & Video Prompt)
            AccordionSection(
                title = "Prompt Editor (Image & Video)",
                subtitle = if (scene.isPromptManuallyEdited || scene.isVideoPromptManuallyEdited) "Diedit manual" else "Otomatis dengan Character ID",
                icon = Icons.Default.AutoAwesome,
                expanded = expandPrompts,
                onToggle = { expandPrompts = !expandPrompts }
            ) {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = {
                        promptDraft = it
                        onUpdatePrompt(it, true)
                    },
                    label = { Text("Image Prompt (English, bisa diedit)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopyPrompt("Image Prompt Scene $sceneNumStr", promptDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salin Prompt")
                    }
                    Button(
                        onClick = { onOpenExternalGenerator(promptDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buka Generator")
                    }
                }
                if (scene.isPromptManuallyEdited) {
                    TextButton(
                        onClick = {
                            promptDraft = autoPrompt
                            onUpdatePrompt("", false)
                        }
                    ) { Text("Reset Image Prompt Otomatis") }
                }

                HorizontalDivider()
                OutlinedTextField(
                    value = videoPromptDraft,
                    onValueChange = {
                        videoPromptDraft = it
                        onUpdateVideoPrompt(it, true)
                    },
                    label = { Text("Video Prompt (${scene.durationSec}s, gerakan & kamera)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onCopyPrompt("Video Prompt Scene $sceneNumStr", videoPromptDraft) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Video Prompt")
                }
            }

            // Accordion 3: Variasi & Referensi Gambar Scene
            val hasV1 = scene.variation1Path?.let { File(it).exists() } == true
            val hasV2 = scene.variation2Path?.let { File(it).exists() } == true
            AccordionSection(
                title = "Variasi Gambar & Referensi",
                subtitle = when {
                    hasV1 && hasV2 -> "2 variasi tersedia (1 aktif, 1 cadangan)"
                    hasV1 || hasV2 -> "1 gambar tersimpan"
                    else -> "Belum ada variasi"
                },
                icon = Icons.Default.Image,
                expanded = expandVariations,
                onToggle = { expandVariations = !expandVariations }
            ) {
                if (hasV1 || hasV2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VariationOptionCard(
                            label = "Variasi A",
                            path = scene.variation1Path,
                            isSelected = scene.selectedVariationIndex == 1 || (scene.selectedVariationIndex == 0 && hasV1),
                            onSelect = { onSelectVariation(1) },
                            modifier = Modifier.weight(1f)
                        )
                        VariationOptionCard(
                            label = "Variasi B",
                            path = scene.variation2Path,
                            isSelected = scene.selectedVariationIndex == 2,
                            onSelect = { onSelectVariation(2) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        "Tekan 'Generate Scene' untuk membuat 2 variasi pratinjau sekaligus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val refFile = scene.referenceImagePath?.let { File(it) }?.takeIf { it.exists() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (refFile != null) {
                        AsyncImage(
                            model = refFile,
                            contentDescription = "Referensi scene",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onViewFullscreen(refFile.absolutePath) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            importAsReference = true
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (refFile != null) "Ganti Referensi Scene" else "Upload Referensi Scene")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(126.dp)
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VariationOptionCard(
    label: String,
    path: String?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = path?.let { File(it) }?.takeIf { it.exists() }
    Card(
        modifier = modifier.clickable(enabled = file != null, onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (file != null) {
                    AsyncImage(
                        model = file,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("-", style = MaterialTheme.typography.bodySmall)
                }
                if (isSelected && file != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Terpilih",
                        tint = EmeraldSuccess,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isSelected && file != null) "$label (Dipakai)" else "$label (Cadangan)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
