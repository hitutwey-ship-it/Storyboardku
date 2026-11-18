package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.ProjectSummaryUi
import com.example.model.formatClockSeconds
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiwayatTab(
    summaries: List<ProjectSummaryUi>,
    activeProjectId: Long,
    listState: LazyListState,
    onSelectProject: (Long) -> Unit,
    onDuplicateProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onCreateProject: (String) -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<ProjectSummaryUi?>(null) }
    var showNewDialog by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Backup & Restore Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Backup & Restore (JSON)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Unduh seluruh proyek menjadi file JSON untuk cadangan, atau unggah kembali kapan saja.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackupJson,
                            modifier = Modifier.weight(1f).testTag("backup_json_btn")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Backup JSON")
                        }
                        OutlinedButton(
                            onClick = onRestoreJson,
                            modifier = Modifier.weight(1f).testTag("restore_json_btn")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                    Button(
                        onClick = { showNewDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Buat Proyek Baru")
                    }
                }
            }
        }

        item {
            Text(
                text = "Proyek Tersimpan (${summaries.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(summaries, key = { it.project.id }) { summary ->
            val isActive = summary.project.id == activeProjectId
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isActive) 2.dp else 1.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.project.title.ifBlank { "Tanpa Judul" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${summary.project.genre.label} • ${summary.project.visualStyle.label} • ${summary.project.aspectRatio.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = EmeraldSuccess.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Aktif",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${summary.sceneCount} scene (${formatClockSeconds(summary.totalDurationSec)}) • ${summary.completedImageCount} gambar • ${summary.approvedCount} disetujui • ${summary.characterCount} karakter",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Diubah: ${dateFormatter.format(Date(summary.project.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onSelectProject(summary.project.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isActive) "Lanjut Edit" else "Buka Proyek")
                        }
                        IconButton(onClick = { onDuplicateProject(summary.project.id) }) {
                            Icon(Icons.Default.CopyAll, contentDescription = "Duplikat proyek")
                        }
                        IconButton(onClick = { pendingDelete = summary }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus proyek",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    pendingDelete?.let { target ->
        ConfirmDeleteDialog(
            title = "Yakin hapus proyek?",
            message = "Proyek \"${target.project.title}\" beserta seluruh scene dan karakternya akan dihapus. Anda bisa membatalkan (Undo) selama 10 detik.",
            onConfirm = {
                onDeleteProject(target.project.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (showNewDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Proyek Baru") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Proyek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateProject(title.ifBlank { "Proyek Storyboard Baru" })
                        showNewDialog = false
                    }
                ) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("Batal") }
            }
        )
    }
}
