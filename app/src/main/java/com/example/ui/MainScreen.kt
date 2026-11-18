package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MainTab
import com.example.model.SceneUi
import com.example.ui.components.ExternalGeneratorDialog
import com.example.ui.components.FullscreenImageDialog
import com.example.ui.components.OnboardingGuideDialog
import com.example.ui.components.PdfExportChoiceDialog
import com.example.ui.components.ScenePreviewDialog
import com.example.ui.components.VideoPromptsAndCapCutDialog
import com.example.ui.screens.KarakterTab
import com.example.ui.screens.PengaturanTab
import com.example.ui.screens.ProyekTab
import com.example.ui.screens.RiwayatTab
import com.example.ui.screens.SceneEditorSheet
import com.example.ui.screens.StoryboardTab
import com.example.ui.screens.StudioVideoTab
import com.example.ui.viewmodel.StoryboardViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: StoryboardViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val filteredScenes by viewModel.filteredScenes.collectAsStateWithLifecycle()
    val summaries by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val sceneGenStates by viewModel.sceneGenStates.collectAsStateWithLifecycle()
    val batchGenState by viewModel.batchGenState.collectAsStateWithLifecycle()
    val renderVideoState by viewModel.renderVideoState.collectAsStateWithLifecycle()
    val isGeneratingScript by viewModel.isGeneratingScript.collectAsStateWithLifecycle()
    val previewingScene by viewModel.previewingScene.collectAsStateWithLifecycle()
    val undoBanner by viewModel.undoBanner.collectAsStateWithLifecycle()
    val cacheInfoBytes by viewModel.cacheInfoBytes.collectAsStateWithLifecycle()
    val showOnboardingManual by viewModel.showOnboardingManual.collectAsStateWithLifecycle()

    var editingScene by remember { mutableStateOf<SceneUi?>(null) }
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }
    var externalGenPrompt by remember { mutableStateOf<String?>(null) }
    var showVideoPromptsDialog by remember { mutableStateOf(false) }
    var showPdfChoiceDialog by remember { mutableStateOf(false) }

    // Scroll states per tab
    val proyekListState = rememberLazyListState()
    val karakterListState = rememberLazyListState()
    val storyboardListState = rememberLazyListState()
    val studioVideoListState = rememberLazyListState()
    val riwayatListState = rememberLazyListState()
    val pengaturanListState = rememberLazyListState()

    val currentListState = when (selectedTab) {
        MainTab.PROYEK -> proyekListState
        MainTab.KARAKTER -> karakterListState
        MainTab.STORYBOARD -> storyboardListState
        MainTab.STUDIO_VIDEO -> studioVideoListState
        MainTab.RIWAYAT -> riwayatListState
        MainTab.PENGATURAN -> pengaturanListState
    }

    val showBackToTop by remember(currentListState) {
        derivedStateOf {
            currentListState.firstVisibleItemIndex > 0 || currentListState.firstVisibleItemScrollOffset > 220
        }
    }

    // SAF Launchers for Exports & Backup
    val pdfCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.exportPdfToUri(uri)
    }

    val zipCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportAllImagesZipToUri(uri)
    }

    val csvCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportShotListCsvToUri(uri)
    }

    val mp4CreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        if (uri != null) viewModel.saveRenderedMp4ToUri(uri)
    }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupJsonToUri(uri)
    }

    val restoreOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreBackupJsonFromUri(uri)
    }

    // Events collector (Snackbars, Clipboard, Share Intent)
    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(ev.message)
                }
                is UiEvent.CopyToClipboard -> {
                    copyTextToClipboard(context, ev.label, ev.text)
                    snackbarHostState.showSnackbar("${ev.label} disalin ke clipboard.")
                }
                is UiEvent.ShareFile -> {
                    shareFileViaProvider(context, ev.filePath, ev.mimeType, ev.title)
                }
            }
        }
    }

    // Navigate back from secondary tabs to Storyboard
    BackHandler(enabled = selectedTab != MainTab.STORYBOARD) {
        viewModel.selectTab(MainTab.STORYBOARD)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                        // App Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "StoryboardKu",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = activeProject?.title?.ifBlank { "Proyek Tanpa Judul" } ?: "Memuat proyek...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.openOnboardingGuide() },
                                modifier = Modifier.testTag("help_guide_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = "Buka Panduan"
                                )
                            }
                        }

                        // Sticky Top Tab Bar for compact phones (tablets/desktops use sidebar NavigationRail)
                        if (!isWideLayout) {
                            PrimaryScrollableTabRow(
                                selectedTabIndex = selectedTab.ordinal,
                                edgePadding = 8.dp,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val icon = tabIcon(tab)
                                    Tab(
                                        selected = selectedTab == tab,
                                        onClick = { viewModel.selectTab(tab) },
                                        text = { Text(tab.label, maxLines = 1) },
                                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // 10-second Undo Banner
                    AnimatedVisibility(visible = undoBanner != null) {
                        undoBanner?.let { banner ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = banner.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.performUndo() }) {
                                        Text(
                                            text = "UNDO",
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextButton(onClick = { viewModel.dismissUndoBanner() }) {
                                        Text("Tutup", color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }

                    // Sticky Bottom Action Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.addSceneAtEnd()
                                    if (selectedTab != MainTab.STORYBOARD) {
                                        viewModel.selectTab(MainTab.STORYBOARD)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).testTag("bottom_add_scene_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Scene", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (selectedTab != MainTab.STORYBOARD) {
                                        viewModel.selectTab(MainTab.STORYBOARD)
                                    }
                                    viewModel.generateAllSceneImages()
                                },
                                enabled = !batchGenState.isRunning && scenes.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1.25f).testTag("bottom_generate_all_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Buat Semua", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = { showPdfChoiceDialog = true },
                                enabled = activeProject != null,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).testTag("bottom_export_pdf_btn")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideLayout) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        when (selectedTab) {
                            MainTab.PROYEK -> ProyekTab(
                                project = activeProject,
                                scenes = scenes,
                                characterCount = characters.size,
                                isGeneratingScript = isGeneratingScript,
                                listState = proyekListState,
                                onUpdateProject = viewModel::updateProject,
                                onCreateNewProject = viewModel::createNewProject,
                                onImportProjectReference = viewModel::importProjectReferenceImage,
                                onGenerateScriptFromIdea = viewModel::generateScriptFromIdea,
                                onGenerateStoryboard = viewModel::generateStoryboardFromScript,
                                onOpenCharacters = { viewModel.selectTab(MainTab.KARAKTER) },
                                onOpenStudioVideo = { viewModel.selectTab(MainTab.STUDIO_VIDEO) }
                            )

                            MainTab.KARAKTER -> KarakterTab(
                                projectId = activeProject?.id ?: -1L,
                                characters = characters,
                                listState = karakterListState,
                                onSaveCharacter = viewModel::saveCharacter,
                                onDeleteCharacter = viewModel::deleteCharacter,
                                onImportCharacterPhoto = viewModel::importCharacterPhoto
                            )

                            MainTab.STORYBOARD -> StoryboardTab(
                                project = activeProject,
                                allScenes = scenes,
                                filteredScenes = filteredScenes,
                                characters = characters,
                                searchQuery = searchQuery,
                                statusFilter = statusFilter,
                                viewMode = viewMode,
                                sceneGenStates = sceneGenStates,
                                batchGenState = batchGenState,
                                isRealAiEnabled = viewModel.isRealAiEnabled,
                                listState = storyboardListState,
                                onSearchChange = viewModel::setSearchQuery,
                                onStatusFilterChange = viewModel::setStatusFilter,
                                onViewModeChange = viewModel::setViewMode,
                                onEditScene = { editingScene = it },
                                onDuplicateScene = viewModel::duplicateScene,
                                onDeleteScene = viewModel::deleteSceneWithUndo,
                                onMoveSceneUp = viewModel::moveSceneUp,
                                onMoveSceneDown = viewModel::moveSceneDown,
                                onGenerateImages = viewModel::generateImagesForScene,
                                onRegenerateVideo = viewModel::regenerateSceneVideo,
                                onPreviewScene = viewModel::openScenePreview,
                                onToggleApprove = viewModel::toggleApproveScene,
                                onApproveAllScenes = viewModel::approveAllScenes,
                                onCancelBatch = viewModel::cancelBatchGeneration,
                                onSelectVariation = viewModel::selectSceneVariation,
                                onUpdatePrompt = viewModel::updateScenePrompt,
                                onUpdateVideoPrompt = viewModel::updateSceneVideoPrompt,
                                onUpdateStatus = viewModel::updateSceneStatus,
                                onImportImage = viewModel::importSceneImage,
                                onCopyPrompt = viewModel::copyPrompt,
                                onCopyAllImagePrompts = viewModel::copyAllImagePrompts,
                                onOpenVideoPromptDialog = { showVideoPromptsDialog = true },
                                onDownloadAllImagesZip = {
                                    val title = activeProject?.title?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Storyboard"
                                    zipCreateLauncher.launch("Gambar_${title}.zip")
                                },
                                onOpenExternalGenerator = { prompt -> externalGenPrompt = prompt },
                                onSaveToGallery = viewModel::saveSceneImageToGallery,
                                onViewFullscreen = { path -> fullscreenImagePath = path }
                            )

                            MainTab.STUDIO_VIDEO -> StudioVideoTab(
                                project = activeProject,
                                scenes = scenes,
                                renderState = renderVideoState,
                                listState = studioVideoListState,
                                onUpdateMixer = viewModel::updateAudioMixer,
                                onPlaySceneAudio = viewModel::playSceneAudioPreview,
                                onStopAudio = viewModel::stopAudioPreview,
                                onApproveAllScenes = viewModel::approveAllScenes,
                                onEditScene = { editingScene = it },
                                onRenderFinalVideo = viewModel::renderFinalMp4Video,
                                onCancelRender = viewModel::cancelMp4Render,
                                onDownloadMp4Document = {
                                    val safeTitle = activeProject?.title?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "StoryboardKu"
                                    mp4CreateLauncher.launch("Video_${safeTitle}.mp4")
                                },
                                onSaveMp4Gallery = viewModel::saveRenderedMp4ToGallery
                            )

                            MainTab.RIWAYAT -> RiwayatTab(
                                summaries = summaries,
                                activeProjectId = activeProject?.id ?: -1L,
                                listState = riwayatListState,
                                onSelectProject = viewModel::selectProject,
                                onDuplicateProject = viewModel::duplicateProject,
                                onDeleteProject = viewModel::deleteProjectWithUndo,
                                onCreateProject = viewModel::createNewProject,
                                onBackupJson = {
                                    backupCreateLauncher.launch("StoryboardKu_Backup.json")
                                },
                                onRestoreJson = {
                                    restoreOpenLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )

                            MainTab.PENGATURAN -> PengaturanTab(
                                settings = settings,
                                cacheInfoBytes = cacheInfoBytes,
                                isRealAiEnabled = viewModel.isRealAiEnabled,
                                listState = pengaturanListState,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetTextScale = viewModel::setTextScale,
                                onSetDefaultStyle = viewModel::setDefaultStyle,
                                onSetDefaultRatio = viewModel::setDefaultRatio,
                                onSetDefaultDuration = viewModel::setDefaultSceneDuration,
                                onClearCache = viewModel::clearCache,
                                onOpenGuide = viewModel::openOnboardingGuide
                            )
                        }

                        // Floating "Kembali ke Atas" button
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBackToTop,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    scope.launch { currentListState.animateScrollToItem(0) }
                                },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.testTag("back_to_top_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Kembali ke Atas", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Ke Atas", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scene Editor Modal Bottom Sheet
    editingScene?.let { scene ->
        SceneEditorSheet(
            scene = scene,
            allCharacters = characters,
            onSave = viewModel::saveScene,
            onDismiss = { editingScene = null }
        )
    }

    // Interactive Animated Scene Preview Dialog
    val previewProject = activeProject
    val previewTarget = previewingScene
    if (previewProject != null && previewTarget != null) {
        ScenePreviewDialog(
            project = previewProject,
            scene = previewTarget,
            onPlayAudio = { viewModel.playSceneAudioPreview(previewTarget) },
            onStopAudio = viewModel::stopAudioPreview,
            onDismiss = viewModel::closeScenePreview
        )
    }

    // Fullscreen Image Viewer
    fullscreenImagePath?.let { path ->
        val matchingScene = scenes.find { it.activeImagePath == path }
        FullscreenImageDialog(
            imagePath = path,
            onSaveToGallery = {
                matchingScene?.let { viewModel.saveSceneImageToGallery(it) }
            },
            onDismiss = { fullscreenImagePath = null }
        )
    }

    // External Generator Dialog (Salin Prompt + Buka Generator)
    externalGenPrompt?.let { prompt ->
        ExternalGeneratorDialog(
            promptText = prompt,
            onCopyPrompt = { text -> viewModel.copyPrompt("Prompt Gambar", text) },
            onLaunchUrl = { url ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }.onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar("Tidak ada browser tersedia untuk membuka generator.")
                    }
                }
            },
            onDismiss = { externalGenPrompt = null }
        )
    }

    // Video Prompts & CapCut Shot List Dialog
    if (showVideoPromptsDialog) {
        val project = activeProject
        val text = if (project != null) {
            viewModel.repository.exportHelper.buildAllVideoPromptsText(project, characters, scenes)
        } else ""
        VideoPromptsAndCapCutDialog(
            allVideoPromptsText = text,
            onCopyAll = { viewModel.copyAllVideoPrompts() },
            onExportCsv = {
                val safeTitle = activeProject?.title?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Storyboard"
                csvCreateLauncher.launch("ShotList_CapCut_${safeTitle}.csv")
            },
            onDismiss = { showVideoPromptsDialog = false }
        )
    }

    // PDF Export Choice Dialog
    if (showPdfChoiceDialog) {
        PdfExportChoiceDialog(
            onSavePdfFile = {
                val safeTitle = activeProject?.title?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "Storyboard"
                pdfCreateLauncher.launch("Storyboard_${safeTitle}.pdf")
            },
            onSharePdf = { viewModel.sharePdf() },
            onDismiss = { showPdfChoiceDialog = false }
        )
    }

    // 3-Step Onboarding Guide (First Launch or '?' Button)
    if (!settings.onboardingDismissed || showOnboardingManual) {
        OnboardingGuideDialog(
            onSkip = { viewModel.closeOnboardingGuide(permanentlyDismiss = true) },
            onStart = { viewModel.closeOnboardingGuide(permanentlyDismiss = true) }
        )
    }
}

private fun tabIcon(tab: MainTab) = when (tab) {
    MainTab.PROYEK -> Icons.Default.Folder
    MainTab.KARAKTER -> Icons.Default.People
    MainTab.STORYBOARD -> Icons.Default.ViewCarousel
    MainTab.STUDIO_VIDEO -> Icons.Default.MovieCreation
    MainTab.RIWAYAT -> Icons.Default.History
    MainTab.PENGATURAN -> Icons.Default.Settings
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareFileViaProvider(context: Context, filePath: String, mimeType: String, title: String) {
    val file = File(filePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
