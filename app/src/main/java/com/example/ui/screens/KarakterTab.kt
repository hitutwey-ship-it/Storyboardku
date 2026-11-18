package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CharacterRole
import com.example.model.CharacterUi
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.ConfirmDeleteDialog

@Composable
fun KarakterTab(
    projectId: Long,
    characters: List<CharacterUi>,
    listState: LazyListState,
    onSaveCharacter: (CharacterUi) -> Unit,
    onDeleteCharacter: (CharacterUi) -> Unit,
    onImportCharacterPhoto: (CharacterUi, Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCharacter by remember { mutableStateOf<CharacterUi?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var deletingCharacter by remember { mutableStateOf<CharacterUi?>(null) }
    var photoTargetCharacter by remember { mutableStateOf<CharacterUi?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = photoTargetCharacter
        if (uri != null && target != null) {
            onImportCharacterPhoto(target, uri)
        }
        photoTargetCharacter = null
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Character Lock & Character ID (${characters.size} Karakter)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Setiap karakter punya Character ID unik agar bentuk wajah, warna tubuh, pakaian, ukuran, mata, gaya, aksesori, dan ciri khas tetap konsisten di semua scene.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val nextCode = "CHR-%03d".format(characters.size + 1)
                    editingCharacter = CharacterUi(
                        projectId = projectId,
                        characterCode = nextCode,
                        name = ""
                    )
                    isAddingNew = true
                },
                modifier = Modifier.fillMaxWidth().testTag("add_character_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("+ Tambah Karakter Utama / Pendukung")
            }
        }

        if (characters.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Belum ada karakter di proyek ini", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        items(characters, key = { it.id }) { char ->
            val isIncomplete = char.description.trim().length < 15 && char.outfit.isBlank()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CharacterAvatar(character = char, size = 68.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = char.characterCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = char.role.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = char.name.ifBlank { "Tanpa Nama" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = char.description.ifBlank { "Belum ada deskripsi visual." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                        }
                    }

                    // Consistency summary chips
                    val traits = listOfNotNull(
                        char.faceShape.takeIf { it.isNotBlank() }?.let { "Wajah: $it" },
                        char.outfit.takeIf { it.isNotBlank() }?.let { "Pakaian: $it" },
                        char.eyeShape.takeIf { it.isNotBlank() }?.let { "Mata: $it" },
                        char.accessories.takeIf { it.isNotBlank() }?.let { "Aksesori: $it" },
                        char.signatureTrait.takeIf { it.isNotBlank() }?.let { "Ciri: $it" }
                    )
                    if (traits.isNotEmpty()) {
                        Text(
                            text = traits.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isIncomplete) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Detail karakter masih singkat — lengkapi agar desain tidak berubah-ubah.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoTargetCharacter = char
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Foto Referensi", maxLines = 1)
                        }
                        IconButton(
                            onClick = {
                                editingCharacter = char
                                isAddingNew = false
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${char.name}")
                        }
                        IconButton(onClick = { deletingCharacter = char }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus ${char.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    editingCharacter?.let { char ->
        var name by rememberSaveable(char.id) { mutableStateOf(char.name) }
        var role by rememberSaveable(char.id) { mutableStateOf(char.role) }
        var faceShape by rememberSaveable(char.id) { mutableStateOf(char.faceShape) }
        var bodyColor by rememberSaveable(char.id) { mutableStateOf(char.bodyColor) }
        var outfit by rememberSaveable(char.id) { mutableStateOf(char.outfit) }
        var bodySize by rememberSaveable(char.id) { mutableStateOf(char.bodySize) }
        var eyeShape by rememberSaveable(char.id) { mutableStateOf(char.eyeShape) }
        var animStyle by rememberSaveable(char.id) { mutableStateOf(char.animationStyle) }
        var accessories by rememberSaveable(char.id) { mutableStateOf(char.accessories) }
        var signatureTrait by rememberSaveable(char.id) { mutableStateOf(char.signatureTrait) }
        var desc by rememberSaveable(char.id) { mutableStateOf(char.description) }

        AlertDialog(
            onDismissRequest = { editingCharacter = null },
            title = {
                Text(if (isAddingNew) "Tambah Karakter (${char.characterCode})" else "Edit ${char.name} (${char.characterCode})")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharacterRole.entries.forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Karakter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = faceShape, onValueChange = { faceShape = it }, label = { Text("Bentuk Wajah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyColor, onValueChange = { bodyColor = it }, label = { Text("Warna Tubuh / Kulit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = outfit, onValueChange = { outfit = it }, label = { Text("Pakaian") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodySize, onValueChange = { bodySize = it }, label = { Text("Ukuran Tubuh") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = eyeShape, onValueChange = { eyeShape = it }, label = { Text("Bentuk Mata") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = animStyle, onValueChange = { animStyle = it }, label = { Text("Gaya Animasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = accessories, onValueChange = { accessories = it }, label = { Text("Aksesori") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = signatureTrait, onValueChange = { signatureTrait = it }, label = { Text("Ciri Khas Karakter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Ringkasan Deskripsi Lengkap") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSaveCharacter(
                            char.copy(
                                name = name.trim(),
                                role = role,
                                faceShape = faceShape.trim(),
                                bodyColor = bodyColor.trim(),
                                outfit = outfit.trim(),
                                bodySize = bodySize.trim(),
                                eyeShape = eyeShape.trim(),
                                animationStyle = animStyle.trim(),
                                accessories = accessories.trim(),
                                signatureTrait = signatureTrait.trim(),
                                description = desc.trim()
                            )
                        )
                        editingCharacter = null
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingCharacter = null }) { Text("Batal") }
            }
        )
    }

    deletingCharacter?.let { target ->
        ConfirmDeleteDialog(
            title = "Yakin hapus karakter?",
            message = "Karakter \"${target.name}\" (${target.characterCode}) akan dihapus dari proyek ini.",
            onConfirm = {
                onDeleteCharacter(target)
                deletingCharacter = null
            },
            onDismiss = { deletingCharacter = null }
        )
    }
}
