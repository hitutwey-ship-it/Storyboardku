package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterUi
import com.example.model.SceneTransition
import com.example.model.SceneUi
import com.example.model.estimateNarrationSeconds
import com.example.ui.components.AccordionSection

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SceneEditorSheet(
    scene: SceneUi,
    allCharacters: List<CharacterUi>,
    onSave: (SceneUi) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember(scene.id) { mutableStateOf(scene.title) }
    var durationSec by remember(scene.id) { mutableIntStateOf(scene.durationSec) }
    var location by remember(scene.id) { mutableStateOf(scene.location) }
    var lighting by remember(scene.id) { mutableStateOf(scene.lighting) }
    var visualDesc by remember(scene.id) { mutableStateOf(scene.visualDescription) }
    var characterPos by remember(scene.id) { mutableStateOf(scene.characterPosition) }
    var facialExpr by remember(scene.id) { mutableStateOf(scene.facialExpression) }
    var charAction by remember(scene.id) { mutableStateOf(scene.characterAction) }
    var dialogueSpeaker by remember(scene.id) { mutableStateOf(scene.dialogueSpeaker) }
    var dialogueText by remember(scene.id) { mutableStateOf(scene.dialogueText) }
    var narrationText by remember(scene.id) { mutableStateOf(scene.narrationText) }
    var cameraShot by remember(scene.id) { mutableStateOf(scene.cameraShot) }
    var cameraMovement by remember(scene.id) { mutableStateOf(scene.cameraMovement) }
    var transition by remember(scene.id) { mutableStateOf(scene.transition) }
    var importantProps by remember(scene.id) { mutableStateOf(scene.importantProps) }
    var mood by remember(scene.id) { mutableStateOf(scene.mood) }
    var musicSfx by remember(scene.id) { mutableStateOf(scene.musicSfx) }
    var soundFx by remember(scene.id) { mutableStateOf(scene.soundFx) }
    var notes by remember(scene.id) { mutableStateOf(scene.notes) }
    var isApproved by remember(scene.id) { mutableStateOf(scene.isApproved) }
    var selectedCharIds by remember(scene.id) { mutableStateOf(scene.characterIds.toSet()) }

    var expandScript by remember { mutableStateOf(true) }
    var expandMotionCamera by remember { mutableStateOf(false) }
    var expandAudioNotes by remember { mutableStateOf(false) }

    val estNarration = estimateNarrationSeconds(narrationText)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Edit SCENE ${"%02d".format(scene.orderIndex + 1)}",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Approve Scene (Kunci Persetujuan)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Scene yang disetujui tidak diubah otomatis saat regenerate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isApproved, onCheckedChange = { isApproved = it })
                }
            }

            // 1. Naskah, Visual & Dialog
            AccordionSection(
                title = "Judul, Visual, Dialog & Narasi",
                icon = Icons.Default.Movie,
                expanded = expandScript,
                onToggle = { expandScript = !expandScript }
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Scene") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Durasi Scene (Manual)", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { durationSec = (durationSec - 1).coerceAtLeast(2) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi durasi")
                        }
                        Text("$durationSec detik", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { durationSec = (durationSec + 1).coerceAtMost(120) }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah durasi")
                        }
                    }
                }

                OutlinedTextField(
                    value = visualDesc,
                    onValueChange = { visualDesc = it },
                    label = { Text("Deskripsi Visual (Kejadian di layar)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialogueSpeaker,
                    onValueChange = { dialogueSpeaker = it },
                    label = { Text("Tokoh yang Berbicara") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dialogueText,
                    onValueChange = { dialogueText = it },
                    label = { Text("Isi Dialog") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = narrationText,
                    onValueChange = { narrationText = it },
                    label = { Text("Narasi / Voiceover") },
                    supportingText = {
                        Text("Perkiraan waktu baca: ~$estNarration detik (durasi scene: $durationSec detik)")
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                if (estNarration > durationSec) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Narasi terlalu panjang untuk $durationSec detik. Persingkat teks atau tambah durasi scene.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 2. Posisi, Ekspresi, Gerakan, Kamera & Pencahayaan
            AccordionSection(
                title = "Animasi Karakter, Kamera & Pencahayaan",
                icon = Icons.Default.Videocam,
                expanded = expandMotionCamera,
                onToggle = { expandMotionCamera = !expandMotionCamera }
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Environment / Lokasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lighting,
                    onValueChange = { lighting = it },
                    label = { Text("Pencahayaan (Lighting)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = characterPos,
                    onValueChange = { characterPos = it },
                    label = { Text("Posisi Karakter dalam Frame") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = facialExpr,
                    onValueChange = { facialExpr = it },
                    label = { Text("Ekspresi Wajah Dinamis") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = charAction,
                    onValueChange = { charAction = it },
                    label = { Text("Action / Gerakan Karakter (Anticipation & Follow-through)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = importantProps,
                    onValueChange = { importantProps = it },
                    label = { Text("Properti Penting") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Jenis Shot Kamera", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraShotType.entries.forEach { shot ->
                        FilterChip(
                            selected = cameraShot == shot,
                            onClick = { cameraShot = shot },
                            label = { Text(shot.label) }
                        )
                    }
                }

                Text("Gerakan Kamera (Animasi Video)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CameraMovement.entries.forEach { move ->
                        FilterChip(
                            selected = cameraMovement == move,
                            onClick = { cameraMovement = move },
                            label = { Text(move.label) }
                        )
                    }
                }

                Text("Transisi ke Scene Berikutnya", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SceneTransition.entries.forEach { tr ->
                        FilterChip(
                            selected = transition == tr,
                            onClick = { transition = tr },
                            label = { Text(tr.label) }
                        )
                    }
                }

                if (allCharacters.isNotEmpty()) {
                    Text("Karakter di Scene (Character ID Lock)", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allCharacters.forEach { c ->
                            val picked = c.id in selectedCharIds
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedCharIds = if (picked) selectedCharIds - c.id else selectedCharIds + c.id
                                },
                                label = { Text("${c.characterCode} ${c.name}") },
                                leadingIcon = {
                                    Icon(
                                        if (picked) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. Suasana, Efek Suara, Musik & Catatan
            AccordionSection(
                title = "Suasana, Efek Suara, Musik & Catatan",
                icon = Icons.Default.RecordVoiceOver,
                expanded = expandAudioNotes,
                onToggle = { expandAudioNotes = !expandAudioNotes }
            ) {
                OutlinedTextField(
                    value = mood,
                    onValueChange = { mood = it },
                    label = { Text("Suasana / Mood") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = soundFx,
                    onValueChange = { soundFx = it },
                    label = { Text("Efek Suara (Sound FX)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = musicSfx,
                    onValueChange = { musicSfx = it },
                    label = { Text("Musik Latar (Background Music)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Produksi") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    onSave(
                        scene.copy(
                            title = title.trim().ifBlank { "Scene ${scene.orderIndex + 1}" },
                            durationSec = durationSec,
                            location = location.trim(),
                            lighting = lighting.trim(),
                            visualDescription = visualDesc.trim(),
                            characterPosition = characterPos.trim(),
                            facialExpression = facialExpr.trim(),
                            characterAction = charAction.trim(),
                            dialogueSpeaker = dialogueSpeaker.trim(),
                            dialogueText = dialogueText.trim(),
                            narrationText = narrationText.trim(),
                            cameraShot = cameraShot,
                            cameraMovement = cameraMovement,
                            transition = transition,
                            importantProps = importantProps.trim(),
                            mood = mood.trim(),
                            musicSfx = musicSfx.trim(),
                            soundFx = soundFx.trim(),
                            notes = notes.trim(),
                            isApproved = isApproved,
                            characterIds = selectedCharIds.toList()
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Simpan Perubahan Scene")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
