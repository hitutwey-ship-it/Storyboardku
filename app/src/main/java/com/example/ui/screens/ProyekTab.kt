package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.AspectRatioOption
import com.example.model.DialogueLanguage
import com.example.model.DurationPreset
import com.example.model.ProjectGenre
import com.example.model.ProjectUi
import com.example.model.SceneUi
import com.example.model.VisualStyle
import com.example.model.VoiceMode
import com.example.model.formatClockSeconds
import com.example.ui.components.AccordionSection
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProyekTab(
    project: ProjectUi?,
    scenes: List<SceneUi>,
    characterCount: Int,
    isGeneratingScript: Boolean,
    listState: LazyListState,
    onUpdateProject: (ProjectUi) -> Unit,
    onCreateNewProject: (String) -> Unit,
    onImportProjectReference: (Uri) -> Unit,
    onGenerateScriptFromIdea: () -> Unit,
    onGenerateStoryboard: (Boolean) -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenStudioVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (project == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Belum ada proyek aktif.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onCreateNewProject("Proyek Video AI Baru") }) {
                Text("Buat Proyek Pertama")
            }
        }
        return
    }

    var expandStory by rememberSaveable { mutableStateOf(true) }
    var expandStyleDuration by rememberSaveable { mutableStateOf(true) }
    var expandReferences by rememberSaveable { mutableStateOf(false) }
    var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomDurationDialog by rememberSaveable { mutableStateOf(false) }
    var showStoryboardConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val projectRefPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportProjectReference(uri)
    }

    val totalDurationSec = scenes.sumOf { it.durationSec }
    val approvedCount = scenes.count { it.isApproved }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Modern AI Video Production Studio Dashboard Hero
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI VIDEO STUDIO DASHBOARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                text = project.title.ifBlank { "Tanpa Judul" },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = { showNewProjectDialog = true },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Baru")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge("Scene", "${scenes.size}", Modifier.weight(1f))
                        StatBadge("Disetujui", "$approvedCount/${scenes.size}", Modifier.weight(1f))
                        StatBadge("Durasi", formatClockSeconds(totalDurationSec), Modifier.weight(1f))
                        StatBadge("Karakter", "$characterCount", Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (scenes.any { !it.isApproved }) {
                                    showStoryboardConfirmDialog = true
                                } else {
                                    onGenerateStoryboard(false)
                                }
                            },
                            enabled = !isGeneratingScript,
                            modifier = Modifier.weight(1f).testTag("dashboard_generate_storyboard_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Storyboard", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenStudioVideo,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.weight(1f).testTag("dashboard_generate_video_btn")
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Video", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Accordion 1: Ide Cerita & Naskah Cerita
        item {
            AccordionSection(
                title = "Ide Cerita & Naskah Cerita",
                subtitle = if (project.storyScript.isBlank()) "Tulis ide lalu AI susun naskah & scene" else "Naskah siap dipecah menjadi scene",
                icon = Icons.Default.AutoFixHigh,
                expanded = expandStory,
                onToggle = { expandStory = !expandStory }
            ) {
                OutlinedTextField(
                    value = project.title,
                    onValueChange = { onUpdateProject(project.copy(title = it)) },
                    label = { Text("Judul Proyek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("project_title_input")
                )

                OutlinedTextField(
                    value = project.storyIdea,
                    onValueChange = { onUpdateProject(project.copy(storyIdea = it)) },
                    label = { Text("Ide Cerita") },
                    placeholder = { Text("Tulis premis atau ide cerita singkat...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onGenerateScriptFromIdea,
                    enabled = !isGeneratingScript && project.storyIdea.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGeneratingScript) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Menyusun Naskah...")
                    } else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Susun Naskah Otomatis dari Ide")
                    }
                }

                OutlinedTextField(
                    value = project.storyScript,
                    onValueChange = { onUpdateProject(project.copy(storyScript = it)) },
                    label = { Text("Naskah Cerita (1 baris = 1 adegan/scene)") },
                    placeholder = { Text("SCENE 1: ...\nSCENE 2: ...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Accordion 2: Referensi Gambar Proyek & Karakter
        item {
            AccordionSection(
                title = "Referensi Gambar & Karakter",
                subtitle = "$characterCount karakter terdaftar • Referensi visual dunia",
                icon = Icons.Default.Image,
                expanded = expandReferences,
                onToggle = { expandReferences = !expandReferences }
            ) {
                val refFile = project.projectReferenceImagePath?.let { File(it) }?.takeIf { it.exists() }
                if (refFile != null) {
                    AsyncImage(
                        model = refFile,
                        contentDescription = "Gambar referensi proyek",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            projectRefPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (refFile == null) "Upload Referensi" else "Ganti Referensi")
                    }
                    Button(
                        onClick = onOpenCharacters,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Referensi Karakter")
                    }
                }
            }
        }

        // Accordion 3: Gaya Animasi, Durasi Video, Bahasa & Suara
        item {
            AccordionSection(
                title = "Gaya Animasi, Durasi & Bahasa",
                subtitle = "${project.visualStyle.label} • Target ${formatClockSeconds(project.targetTotalDurationSec)} • ${project.effectiveLanguageLabel}",
                icon = Icons.Default.Palette,
                expanded = expandStyleDuration,
                onToggle = { expandStyleDuration = !expandStyleDuration }
            ) {
                Text("Gaya Animasi", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    VisualStyle.entries.forEach { style ->
                        FilterChip(
                            selected = project.visualStyle == style,
                            onClick = { onUpdateProject(project.copy(visualStyle = style)) },
                            label = { Text(style.label) },
                            leadingIcon = if (project.visualStyle == style) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Text("Target Durasi Video Total", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DurationPreset.entries.forEach { preset ->
                        val isCustomSelected = preset == DurationPreset.CUSTOM &&
                            DurationPreset.entries.none { it != DurationPreset.CUSTOM && it.seconds == project.targetTotalDurationSec }
                        val selected = if (preset == DurationPreset.CUSTOM) isCustomSelected else project.targetTotalDurationSec == preset.seconds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (preset == DurationPreset.CUSTOM) {
                                    showCustomDurationDialog = true
                                } else {
                                    onUpdateProject(project.copy(targetTotalDurationSec = preset.seconds))
                                }
                            },
                            label = {
                                Text(
                                    if (preset == DurationPreset.CUSTOM && isCustomSelected) {
                                        "Custom (${formatClockSeconds(project.targetTotalDurationSec)})"
                                    } else preset.label
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Text("Bahasa Dialog & Narasi", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogueLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = project.dialogueLanguage == lang,
                            onClick = { onUpdateProject(project.copy(dialogueLanguage = lang)) },
                            label = { Text(lang.label) }
                        )
                    }
                }
                if (project.dialogueLanguage == DialogueLanguage.LAINNYA) {
                    OutlinedTextField(
                        value = project.customLanguageName,
                        onValueChange = { onUpdateProject(project.copy(customLanguageName = it)) },
                        label = { Text("Nama Bahasa Lainnya") },
                        placeholder = { Text("Misal: Bahasa Bali / Minang / Jepang") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Mode Suara & Dialog", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoiceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = project.voiceMode == mode,
                            onClick = { onUpdateProject(project.copy(voiceMode = mode)) },
                            label = { Text(mode.label) },
                            leadingIcon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Subtitle Toggle (OFF by default)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tampilkan Subtitle di Video", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Nonaktif secara default. Aktifkan hanya jika ingin menampilkan teks subtitle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = project.subtitlesEnabled,
                            onCheckedChange = { onUpdateProject(project.copy(subtitlesEnabled = it)) }
                        )
                    }
                }

                Text("Format Rasio & Genre", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        FilterChip(
                            selected = project.aspectRatio == ratio,
                            onClick = { onUpdateProject(project.copy(aspectRatio = ratio)) },
                            label = {
                                Text(if (ratio == AspectRatioOption.RATIO_9_16) "9:16 (1080×1920)" else "16:9 (Landscape)")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectGenre.entries.forEach { genre ->
                        FilterChip(
                            selected = project.genre == genre,
                            onClick = { onUpdateProject(project.copy(genre = genre)) },
                            label = { Text(genre.label) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showNewProjectDialog) {
        var newTitle by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Buat Proyek Video Baru") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Judul Proyek") },
                    placeholder = { Text("Misal: Petualangan Kancil 3D") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateNewProject(newTitle.ifBlank { "Proyek Video AI Baru" })
                        showNewProjectDialog = false
                    }
                ) { Text("Buat Proyek") }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showCustomDurationDialog) {
        var secondsInput by remember { mutableStateOf(project.targetTotalDurationSec.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = { Text("Atur Durasi Custom (Detik)") },
            text = {
                OutlinedTextField(
                    value = secondsInput,
                    onValueChange = { secondsInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Durasi (detik)") },
                    supportingText = { Text("Contoh: 45 detik, 90 detik, 240 detik") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = secondsInput.toIntOrNull()?.coerceIn(10, 3600) ?: 60
                        onUpdateProject(project.copy(targetTotalDurationSec = parsed))
                        showCustomDurationDialog = false
                    }
                ) { Text("Simpan Durasi") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showStoryboardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStoryboardConfirmDialog = false },
            title = { Text("Generate Storyboard dari Naskah?") },
            text = {
                Text(
                    "Scene yang sudah berstatus Disetujui (Approved) akan tetap aman dan tidak diubah. Scene yang belum disetujui akan diperbarui sesuai naskah dan target durasi (${formatClockSeconds(project.targetTotalDurationSec)})."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStoryboardConfirmDialog = false
                        onGenerateStoryboard(false)
                    }
                ) { Text("Generate Sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { showStoryboardConfirmDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
