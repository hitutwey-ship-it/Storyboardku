package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BatchGenState
import com.example.model.CharacterUi
import com.example.model.ProjectUi
import com.example.model.SceneGenState
import com.example.model.SceneStatus
import com.example.model.SceneUi
import com.example.model.StoryboardViewMode
import com.example.model.formatTimeRange
import com.example.ui.components.AiNoticeBanner
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.SceneThumbnailBox
import com.example.ui.theme.EmeraldSuccess

@Composable
fun StoryboardTab(
    project: ProjectUi?,
    allScenes: List<SceneUi>,
    filteredScenes: List<SceneUi>,
    characters: List<CharacterUi>,
    searchQuery: String,
    statusFilter: SceneStatus?,
    viewMode: StoryboardViewMode,
    sceneGenStates: Map<Long, SceneGenState>,
    batchGenState: BatchGenState,
    isRealAiEnabled: Boolean,
    listState: LazyListState,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (SceneStatus?) -> Unit,
    onViewModeChange: (StoryboardViewMode) -> Unit,
    onEditScene: (SceneUi) -> Unit,
    onDuplicateScene: (SceneUi) -> Unit,
    onDeleteScene: (SceneUi) -> Unit,
    onMoveSceneUp: (SceneUi) -> Unit,
    onMoveSceneDown: (SceneUi) -> Unit,
    onGenerateImages: (SceneUi) -> Unit,
    onRegenerateVideo: (SceneUi) -> Unit,
    onPreviewScene: (SceneUi) -> Unit,
    onToggleApprove: (SceneUi) -> Unit,
    onApproveAllScenes: () -> Unit,
    onCancelBatch: () -> Unit,
    onSelectVariation: (SceneUi, Int) -> Unit,
    onUpdatePrompt: (SceneUi, String, Boolean) -> Unit,
    onUpdateVideoPrompt: (SceneUi, String, Boolean) -> Unit,
    onUpdateStatus: (SceneUi, SceneStatus) -> Unit,
    onImportImage: (SceneUi, Uri, Boolean) -> Unit,
    onCopyPrompt: (String, String) -> Unit,
    onCopyAllImagePrompts: () -> Unit,
    onOpenVideoPromptDialog: () -> Unit,
    onDownloadAllImagesZip: () -> Unit,
    onOpenExternalGenerator: (String) -> Unit,
    onSaveToGallery: (SceneUi) -> Unit,
    onViewFullscreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (project == null) return

    var pendingDeleteScene by remember { mutableStateOf<SceneUi?>(null) }

    val startTimeMap = remember(allScenes) {
        var acc = 0
        buildMap {
            allScenes.forEach { s ->
                put(s.id, acc)
                acc += s.durationSec
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (batchGenState.isRunning) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Membuat Gambar ${batchGenState.currentIndex}/${batchGenState.totalCount}: ${batchGenState.currentSceneTitle}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = onCancelBatch) { Text("Batal") }
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (batchGenState.totalCount > 0) {
                                batchGenState.currentIndex.toFloat() / batchGenState.totalCount
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        when (viewMode) {
            StoryboardViewMode.LIST -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "controls_header") {
                        StoryboardControlsHeader(
                            searchQuery = searchQuery,
                            statusFilter = statusFilter,
                            viewMode = viewMode,
                            isRealAiEnabled = isRealAiEnabled,
                            onSearchChange = onSearchChange,
                            onStatusFilterChange = onStatusFilterChange,
                            onViewModeChange = onViewModeChange,
                            onApproveAllScenes = onApproveAllScenes,
                            onCopyAllImagePrompts = onCopyAllImagePrompts,
                            onOpenVideoPromptDialog = onOpenVideoPromptDialog,
                            onDownloadAllImagesZip = onDownloadAllImagesZip,
                            onOpenExternalGenerator = {
                                val firstPrompt = allScenes.firstOrNull()?.let { s ->
                                    if (s.isPromptManuallyEdited && s.customPrompt.isNotBlank()) s.customPrompt
                                    else com.example.model.buildAutoImagePrompt(
                                        project, s, characters.filter { it.id in s.characterIds }
                                    )
                                }.orEmpty()
                                onOpenExternalGenerator(firstPrompt)
                            }
                        )
                    }

                    if (filteredScenes.isEmpty()) {
                        item { EmptyScenesBox() }
                    }

                    items(filteredScenes, key = { it.id }) { scene ->
                        SceneCard(
                            project = project,
                            scene = scene,
                            startSeconds = startTimeMap[scene.id] ?: 0,
                            totalScenes = allScenes.size,
                            characters = characters,
                            genState = sceneGenStates[scene.id] ?: SceneGenState.Idle,
                            onEdit = { onEditScene(scene) },
                            onDuplicate = { onDuplicateScene(scene) },
                            onRequestDelete = { pendingDeleteScene = scene },
                            onMoveUp = { onMoveSceneUp(scene) },
                            onMoveDown = { onMoveSceneDown(scene) },
                            onGenerateImages = { onGenerateImages(scene) },
                            onRegenerateVideo = { onRegenerateVideo(scene) },
                            onPreviewScene = { onPreviewScene(scene) },
                            onToggleApprove = { onToggleApprove(scene) },
                            onSelectVariation = { idx -> onSelectVariation(scene, idx) },
                            onUpdatePrompt = { txt, manual -> onUpdatePrompt(scene, txt, manual) },
                            onUpdateVideoPrompt = { txt, manual -> onUpdateVideoPrompt(scene, txt, manual) },
                            onUpdateStatus = { st -> onUpdateStatus(scene, st) },
                            onImportImage = { uri, asRef -> onImportImage(scene, uri, asRef) },
                            onCopyPrompt = onCopyPrompt,
                            onOpenExternalGenerator = onOpenExternalGenerator,
                            onSaveToGallery = { onSaveToGallery(scene) },
                            onViewFullscreen = onViewFullscreen
                        )
                    }
                }
            }

            StoryboardViewMode.GRID -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth >= 700.dp) GridCells.Fixed(3) else GridCells.Fixed(2)
                    LazyVerticalGrid(
                        columns = columns,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StoryboardControlsHeader(
                                searchQuery = searchQuery,
                                statusFilter = statusFilter,
                                viewMode = viewMode,
                                isRealAiEnabled = isRealAiEnabled,
                                onSearchChange = onSearchChange,
                                onStatusFilterChange = onStatusFilterChange,
                                onViewModeChange = onViewModeChange,
                                onApproveAllScenes = onApproveAllScenes,
                                onCopyAllImagePrompts = onCopyAllImagePrompts,
                                onOpenVideoPromptDialog = onOpenVideoPromptDialog,
                                onDownloadAllImagesZip = onDownloadAllImagesZip,
                                onOpenExternalGenerator = { onOpenExternalGenerator("") }
                            )
                        }

                        if (filteredScenes.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { EmptyScenesBox() }
                        }

                        items(filteredScenes, key = { it.id }) { scene ->
                            val startSec = startTimeMap[scene.id] ?: 0
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onEditScene(scene) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (scene.isApproved) 2.dp else 1.dp,
                                    color = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SceneThumbnailBox(
                                        scene = scene,
                                        aspectRatio = project.aspectRatio,
                                        genState = sceneGenStates[scene.id] ?: SceneGenState.Idle,
                                        onRetry = { onGenerateImages(scene) }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "SCENE ${"%02d".format(scene.orderIndex + 1)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.primary,
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
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${formatTimeRange(startSec, scene.durationSec)} • ${scene.cameraMovement.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = { onPreviewScene(scene) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Preview Scene", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { onGenerateImages(scene) },
                                            enabled = !scene.isApproved && scene.visualDescription.isNotBlank(),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.GridView, contentDescription = "Generate", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onToggleApprove(scene) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Approve Scene",
                                                tint = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            StoryboardViewMode.TIMELINE -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        StoryboardControlsHeader(
                            searchQuery = searchQuery,
                            statusFilter = statusFilter,
                            viewMode = viewMode,
                            isRealAiEnabled = isRealAiEnabled,
                            onSearchChange = onSearchChange,
                            onStatusFilterChange = onStatusFilterChange,
                            onViewModeChange = onViewModeChange,
                            onApproveAllScenes = onApproveAllScenes,
                            onCopyAllImagePrompts = onCopyAllImagePrompts,
                            onOpenVideoPromptDialog = onOpenVideoPromptDialog,
                            onDownloadAllImagesZip = onDownloadAllImagesZip,
                            onOpenExternalGenerator = { onOpenExternalGenerator("") }
                        )
                    }

                    item {
                        Text(
                            text = "Garis Waktu Produksi (${filteredScenes.size} scene • ${filteredScenes.sumOf { it.durationSec }} detik)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (filteredScenes.isEmpty()) {
                        item { EmptyScenesBox() }
                    }

                    items(filteredScenes, key = { it.id }) { scene ->
                        val startSec = startTimeMap[scene.id] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.width(86.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = formatTimeRange(startSec, scene.durationSec),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${scene.durationSec} detik",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Card(
                                modifier = Modifier.weight(1f).clickable { onEditScene(scene) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (scene.isApproved) 2.dp else 1.dp,
                                    color = if (scene.isApproved) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "SCENE ${"%02d".format(scene.orderIndex + 1)} — ${scene.title}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${scene.cameraShot.label} • ${scene.cameraMovement.label} • ${scene.status.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Transisi: ${scene.transition.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteScene?.let { target ->
        ConfirmDeleteDialog(
            title = "Yakin hapus Scene ${target.orderIndex + 1}?",
            message = "\"${target.title}\" akan dihapus. Anda masih bisa membatalkan (Undo) selama 10 detik.",
            onConfirm = {
                onDeleteScene(target)
                pendingDeleteScene = null
            },
            onDismiss = { pendingDeleteScene = null }
        )
    }
}

@Composable
private fun StoryboardControlsHeader(
    searchQuery: String,
    statusFilter: SceneStatus?,
    viewMode: StoryboardViewMode,
    isRealAiEnabled: Boolean,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (SceneStatus?) -> Unit,
    onViewModeChange: (StoryboardViewMode) -> Unit,
    onApproveAllScenes: () -> Unit,
    onCopyAllImagePrompts: () -> Unit,
    onOpenVideoPromptDialog: () -> Unit,
    onDownloadAllImagesZip: () -> Unit,
    onOpenExternalGenerator: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!isRealAiEnabled) {
            AiNoticeBanner(
                onCopyAllPrompts = onCopyAllImagePrompts,
                onOpenGeneratorGuide = onOpenExternalGenerator
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari scene (judul, dialog, atau narasi)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Bersihkan pencarian")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_scene_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("Semua") }
            )
            SceneStatus.entries.forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onStatusFilterChange(if (statusFilter == status) null else status) },
                    label = { Text(status.label) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryboardViewMode.entries.forEach { mode ->
                val icon = when (mode) {
                    StoryboardViewMode.GRID -> Icons.Default.GridView
                    StoryboardViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                    StoryboardViewMode.TIMELINE -> Icons.Default.Timeline
                }
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { onViewModeChange(mode) },
                    label = { Text(mode.label) },
                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onApproveAllScenes) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Setujui Semua Scene")
            }
            OutlinedButton(onClick = onCopyAllImagePrompts) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salin Semua Prompt")
            }
            OutlinedButton(onClick = onOpenVideoPromptDialog) {
                Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Prompt Video / CapCut")
            }
            OutlinedButton(onClick = onDownloadAllImagesZip) {
                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download ZIP")
            }
        }
    }
}

@Composable
private fun EmptyScenesBox() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tidak ada scene yang cocok dengan filter pencarian.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
