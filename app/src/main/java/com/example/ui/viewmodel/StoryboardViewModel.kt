package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiGenerationResult
import com.example.data.ai.LocalStoryScriptRepository
import com.example.data.ai.PlaceholderAiImageRepository
import com.example.data.audio.AudioPreviewEngine
import com.example.data.export.ExportHelper
import com.example.data.export.Mp4VideoRenderer
import com.example.data.local.SettingsDataStore
import com.example.data.local.StoryboardDatabase
import com.example.data.repository.StoryboardRepository
import com.example.data.storage.ImageStorageHelper
import com.example.model.AppSettings
import com.example.model.AspectRatioOption
import com.example.model.AudioMixerSettings
import com.example.model.BatchGenState
import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterUi
import com.example.model.MainTab
import com.example.model.ProjectSummaryUi
import com.example.model.ProjectUi
import com.example.model.RenderVideoState
import com.example.model.SceneGenState
import com.example.model.SceneStatus
import com.example.model.SceneTransition
import com.example.model.SceneUi
import com.example.model.StoryboardViewMode
import com.example.model.TextScaleOption
import com.example.model.ThemeMode
import com.example.model.VisualStyle
import com.example.model.buildAutoImagePrompt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UndoPayload {
    data class DeletedScene(val scene: SceneUi) : UndoPayload
    data class DeletedProject(
        val project: ProjectUi,
        val characters: List<CharacterUi>,
        val scenes: List<SceneUi>
    ) : UndoPayload
}

data class UndoBannerState(
    val message: String,
    val payload: UndoPayload
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class CopyToClipboard(val label: String, val text: String) : UiEvent
    data class ShareFile(val filePath: String, val mimeType: String, val title: String) : UiEvent
}

class StoryboardViewModel(application: Application) : AndroidViewModel(application) {

    val repository: StoryboardRepository = run {
        val db = StoryboardDatabase.getInstance(application)
        val settings = SettingsDataStore(application)
        val aiImage = PlaceholderAiImageRepository(application)
        val aiScript = LocalStoryScriptRepository()
        val storage = ImageStorageHelper(application)
        val export = ExportHelper(application)
        val mp4Renderer = Mp4VideoRenderer(application)
        StoryboardRepository(db.storyboardDao(), settings, aiImage, aiScript, storage, export, mp4Renderer)
    }

    private val audioEngine = AudioPreviewEngine(application)

    val isRealAiEnabled: Boolean
        get() = repository.isRealAiEnabled

    private val _selectedTab = MutableStateFlow(MainTab.PROYEK)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<SceneStatus?>(null)
    val statusFilter: StateFlow<SceneStatus?> = _statusFilter.asStateFlow()

    private val _viewMode = MutableStateFlow(StoryboardViewMode.GRID)
    val viewMode: StateFlow<StoryboardViewMode> = _viewMode.asStateFlow()

    private val _sceneGenStates = MutableStateFlow<Map<Long, SceneGenState>>(emptyMap())
    val sceneGenStates: StateFlow<Map<Long, SceneGenState>> = _sceneGenStates.asStateFlow()

    private val _batchGenState = MutableStateFlow(BatchGenState())
    val batchGenState: StateFlow<BatchGenState> = _batchGenState.asStateFlow()

    private val _renderVideoState = MutableStateFlow<RenderVideoState>(RenderVideoState.Idle)
    val renderVideoState: StateFlow<RenderVideoState> = _renderVideoState.asStateFlow()

    private val _isGeneratingScript = MutableStateFlow(false)
    val isGeneratingScript: StateFlow<Boolean> = _isGeneratingScript.asStateFlow()

    private val _previewingScene = MutableStateFlow<SceneUi?>(null)
    val previewingScene: StateFlow<SceneUi?> = _previewingScene.asStateFlow()

    private val _undoBanner = MutableStateFlow<UndoBannerState?>(null)
    val undoBanner: StateFlow<UndoBannerState?> = _undoBanner.asStateFlow()

    private val _cacheInfoBytes = MutableStateFlow(0L to 0L)
    val cacheInfoBytes: StateFlow<Pair<Long, Long>> = _cacheInfoBytes.asStateFlow()

    private val _showOnboardingManual = MutableStateFlow(false)
    val showOnboardingManual: StateFlow<Boolean> = _showOnboardingManual.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var undoTimerJob: Job? = null
    private var batchJob: Job? = null
    private var renderJob: Job? = null

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    val projectSummaries: StateFlow<List<ProjectSummaryUi>> =
        repository.observeProjectSummaries().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val activeProjectIdFlow = combine(
        settings.map { it.activeProjectId }.distinctUntilChanged(),
        projectSummaries
    ) { savedId, summaries ->
        when {
            summaries.any { it.project.id == savedId } -> savedId
            summaries.isNotEmpty() -> summaries.first().project.id
            else -> -1L
        }
    }.distinctUntilChanged()

    val activeProject: StateFlow<ProjectUi?> = activeProjectIdFlow
        .flatMapLatest { id -> if (id > 0L) repository.observeProject(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val characters: StateFlow<List<CharacterUi>> = activeProjectIdFlow
        .flatMapLatest { id -> if (id > 0L) repository.observeCharacters(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scenes: StateFlow<List<SceneUi>> = activeProjectIdFlow
        .flatMapLatest { id -> if (id > 0L) repository.observeScenes(id) else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredScenes: StateFlow<List<SceneUi>> = combine(
        scenes,
        searchQuery,
        statusFilter
    ) { list, query, status ->
        list.filter { scene ->
            val matchesStatus = status == null || scene.status == status
            val q = query.trim().lowercase()
            val matchesQuery = q.isBlank() ||
                scene.title.lowercase().contains(q) ||
                scene.dialogueText.lowercase().contains(q) ||
                scene.dialogueSpeaker.lowercase().contains(q) ||
                scene.visualDescription.lowercase().contains(q) ||
                scene.narrationText.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val firstId = repository.ensureInitialized()
            if (settings.value.activeProjectId <= 0L && firstId > 0L) {
                repository.setActiveProject(firstId)
            }
            refreshCacheSize()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: SceneStatus?) {
        _statusFilter.value = status
    }

    fun setViewMode(mode: StoryboardViewMode) {
        _viewMode.value = mode
    }

    fun openOnboardingGuide() {
        _showOnboardingManual.value = true
    }

    fun closeOnboardingGuide(permanentlyDismiss: Boolean = true) {
        _showOnboardingManual.value = false
        if (permanentlyDismiss) {
            viewModelScope.launch { repository.setOnboardingDismissed(true) }
        }
    }

    // Project & Dashboard Actions
    fun updateProject(updated: ProjectUi) {
        viewModelScope.launch { repository.updateProject(updated) }
    }

    fun updateAudioMixer(mixer: AudioMixerSettings) {
        val project = activeProject.value ?: return
        updateProject(project.copy(audioMixer = mixer))
    }

    fun importProjectReferenceImage(uri: Uri) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            repository.importProjectReferenceImage(project, uri)
                .onSuccess {
                    refreshCacheSize()
                    _events.emit(UiEvent.ShowMessage("Gambar referensi proyek disimpan."))
                }
                .onFailure {
                    _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal mengimpor referensi."))
                }
        }
    }

    fun generateScriptFromIdea() {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _isGeneratingScript.value = true
            try {
                repository.generateScriptFromIdea(project, characters.value)
                _events.emit(UiEvent.ShowMessage("Naskah cerita tersusun dari ide proyek (mode lokal)."))
            } finally {
                _isGeneratingScript.value = false
            }
        }
    }

    fun generateStoryboardFromScript(replaceApprovedToo: Boolean = false) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            _isGeneratingScript.value = true
            try {
                val count = repository.generateStoryboardFromScript(
                    project = project,
                    characters = characters.value,
                    replaceUnapprovedOnly = !replaceApprovedToo
                )
                _selectedTab.value = MainTab.STORYBOARD
                _events.emit(
                    UiEvent.ShowMessage("$count scene baru dibuat sesuai durasi target. Scene yang sudah disetujui tetap aman.")
                )
            } finally {
                _isGeneratingScript.value = false
            }
        }
    }

    fun selectProject(projectId: Long) {
        viewModelScope.launch {
            _renderVideoState.value = RenderVideoState.Idle
            repository.setActiveProject(projectId)
            _selectedTab.value = MainTab.STORYBOARD
            _events.emit(UiEvent.ShowMessage("Proyek dibuka."))
        }
    }

    fun createNewProject(title: String) {
        viewModelScope.launch {
            val s = settings.value
            _renderVideoState.value = RenderVideoState.Idle
            repository.createNewProject(
                title = title,
                style = s.defaultStyle,
                ratio = s.defaultRatio,
                defaultDuration = s.defaultSceneDurationSec
            )
            _selectedTab.value = MainTab.PROYEK
            _events.emit(UiEvent.ShowMessage("Proyek baru dibuat."))
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(projectId)
            if (newId != null) {
                _events.emit(UiEvent.ShowMessage("Proyek berhasil diduplikat."))
            }
        }
    }

    fun deleteProjectWithUndo(projectId: Long) {
        viewModelScope.launch {
            val snapshot = repository.getProjectSnapshot(projectId) ?: return@launch
            repository.deleteProject(projectId)
            startUndoWindow(
                message = "Proyek \"${snapshot.first.title}\" dihapus.",
                payload = UndoPayload.DeletedProject(snapshot.first, snapshot.second, snapshot.third)
            )
        }
    }

    // Character Actions
    fun saveCharacter(character: CharacterUi) {
        viewModelScope.launch {
            repository.saveCharacter(character)
            _events.emit(UiEvent.ShowMessage("Karakter \"${character.name}\" disimpan."))
        }
    }

    fun deleteCharacter(character: CharacterUi) {
        viewModelScope.launch {
            repository.deleteCharacter(character.id, character.projectId)
            _events.emit(UiEvent.ShowMessage("Karakter \"${character.name}\" dihapus."))
        }
    }

    fun importCharacterPhoto(character: CharacterUi, uri: Uri) {
        viewModelScope.launch {
            val res = repository.imageStorageHelper.importAndCompressImage(uri, "char_${character.id}")
            res.onSuccess { path ->
                repository.saveCharacter(character.copy(photoPath = path))
                refreshCacheSize()
                _events.emit(UiEvent.ShowMessage("Foto referensi ${character.name} berhasil diperbarui."))
            }.onFailure { err ->
                _events.emit(UiEvent.ShowMessage(err.localizedMessage ?: "Gagal memuat foto."))
            }
        }
    }

    // Scene Actions
    fun addSceneAtEnd() {
        val project = activeProject.value ?: return
        val currentScenes = scenes.value
        val defaultCharIds = characters.value.map { it.id }
        viewModelScope.launch {
            val nextNum = currentScenes.size + 1
            val newScene = SceneUi(
                projectId = project.id,
                orderIndex = currentScenes.size,
                title = "Scene $nextNum",
                durationSec = project.defaultSceneDurationSec,
                cameraShot = CameraShotType.MEDIUM,
                cameraMovement = CameraMovement.DOLLY_IN,
                transition = SceneTransition.SMOOTH_CINEMATIC,
                status = SceneStatus.BELUM_ADA_GAMBAR,
                characterIds = defaultCharIds
            )
            repository.saveScene(newScene)
            _events.emit(UiEvent.ShowMessage("Scene $nextNum ditambahkan."))
        }
    }

    fun saveScene(scene: SceneUi) {
        viewModelScope.launch { repository.saveScene(scene) }
    }

    fun toggleApproveScene(scene: SceneUi) {
        viewModelScope.launch {
            val next = !scene.isApproved
            repository.saveScene(scene.copy(isApproved = next))
            val msg = if (next) {
                "Scene ${scene.orderIndex + 1} disetujui & dikunci dari perubahan otomatis."
            } else {
                "Persetujuan Scene ${scene.orderIndex + 1} dibuka kembali."
            }
            _events.emit(UiEvent.ShowMessage(msg))
        }
    }

    fun approveAllScenes() {
        val list = scenes.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            list.forEach { s ->
                if (!s.isApproved) repository.saveScene(s.copy(isApproved = true))
            }
            _events.emit(UiEvent.ShowMessage("Semua ${list.size} scene disetujui! Siap render video final."))
        }
    }

    fun regenerateSceneVideo(scene: SceneUi) {
        viewModelScope.launch {
            repository.saveScene(scene.copy(videoGeneratedAt = System.currentTimeMillis()))
            _events.emit(UiEvent.ShowMessage("Animasi & prompt video Scene ${scene.orderIndex + 1} diperbarui."))
        }
    }

    fun openScenePreview(scene: SceneUi) {
        _previewingScene.value = scene
        playSceneAudioPreview(scene)
    }

    fun closeScenePreview() {
        _previewingScene.value = null
        audioEngine.stop()
    }

    fun playSceneAudioPreview(scene: SceneUi) {
        val project = activeProject.value ?: return
        audioEngine.playSceneAudio(
            scope = viewModelScope,
            scene = scene,
            language = project.dialogueLanguage,
            voiceMode = project.voiceMode,
            mixer = project.audioMixer
        )
    }

    fun stopAudioPreview() {
        audioEngine.stop()
    }

    fun duplicateScene(scene: SceneUi) {
        viewModelScope.launch {
            repository.duplicateScene(scene)
            _events.emit(UiEvent.ShowMessage("Scene diduplikat di akhir daftar."))
        }
    }

    fun moveSceneUp(scene: SceneUi) {
        val list = scenes.value
        val idx = list.indexOfFirst { it.id == scene.id }
        if (idx > 0) {
            viewModelScope.launch { repository.moveScene(scene.projectId, idx, idx - 1) }
        }
    }

    fun moveSceneDown(scene: SceneUi) {
        val list = scenes.value
        val idx = list.indexOfFirst { it.id == scene.id }
        if (idx in 0 until list.lastIndex) {
            viewModelScope.launch { repository.moveScene(scene.projectId, idx, idx + 1) }
        }
    }

    fun deleteSceneWithUndo(scene: SceneUi) {
        viewModelScope.launch {
            repository.deleteSceneAndReindex(scene)
            startUndoWindow(
                message = "Scene ${scene.orderIndex + 1} (\"${scene.title}\") dihapus.",
                payload = UndoPayload.DeletedScene(scene)
            )
        }
    }

    fun performUndo() {
        val pending = _undoBanner.value ?: return
        undoTimerJob?.cancel()
        _undoBanner.value = null
        viewModelScope.launch {
            when (val p = pending.payload) {
                is UndoPayload.DeletedScene -> {
                    repository.restoreDeletedScene(p.scene)
                    _events.emit(UiEvent.ShowMessage("Scene dikembalikan."))
                }
                is UndoPayload.DeletedProject -> {
                    repository.restoreFullProjectSnapshot(p.project, p.characters, p.scenes)
                    _events.emit(UiEvent.ShowMessage("Proyek dikembalikan."))
                }
            }
        }
    }

    fun dismissUndoBanner() {
        undoTimerJob?.cancel()
        _undoBanner.value = null
    }

    private fun startUndoWindow(message: String, payload: UndoPayload) {
        undoTimerJob?.cancel()
        _undoBanner.value = UndoBannerState(message, payload)
        undoTimerJob = viewModelScope.launch {
            delay(10_000L)
            _undoBanner.value = null
        }
    }

    // Image Generation & Prompt Management
    fun getEffectiveImagePrompt(scene: SceneUi): String {
        if (scene.isPromptManuallyEdited && scene.customPrompt.isNotBlank()) {
            return scene.customPrompt
        }
        val project = activeProject.value ?: return scene.customPrompt
        val sceneChars = characters.value.filter { it.id in scene.characterIds }
        return buildAutoImagePrompt(project, scene, sceneChars)
    }

    fun updateScenePrompt(scene: SceneUi, promptText: String, manual: Boolean) {
        viewModelScope.launch {
            repository.saveScene(scene.copy(customPrompt = promptText, isPromptManuallyEdited = manual))
        }
    }

    fun updateSceneVideoPrompt(scene: SceneUi, videoPromptText: String, manual: Boolean) {
        viewModelScope.launch {
            repository.saveScene(scene.copy(customVideoPrompt = videoPromptText, isVideoPromptManuallyEdited = manual))
        }
    }

    fun selectSceneVariation(scene: SceneUi, variationIndex: Int) {
        viewModelScope.launch {
            repository.saveScene(
                scene.copy(
                    selectedVariationIndex = variationIndex,
                    status = SceneStatus.GAMBAR_JADI
                )
            )
        }
    }

    fun updateSceneStatus(scene: SceneUi, status: SceneStatus) {
        viewModelScope.launch {
            repository.saveScene(scene.copy(status = status))
        }
    }

    fun generateImagesForScene(scene: SceneUi) {
        val project = activeProject.value ?: return
        if (scene.visualDescription.isBlank()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Isi deskripsi visual scene terlebih dahulu sebelum membuat gambar."))
            }
            return
        }
        viewModelScope.launch {
            _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Loading)
            val prompt = getEffectiveImagePrompt(scene)
            when (val res = repository.generateSceneImagePair(project, scene, characters.value, prompt)) {
                is AiGenerationResult.Success -> {
                    _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Idle)
                    refreshCacheSize()
                    _events.emit(UiEvent.ShowMessage("2 variasi gambar Scene ${scene.orderIndex + 1} selesai dibuat."))
                }
                is AiGenerationResult.Error -> {
                    _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Error(res.message))
                }
            }
        }
    }

    fun generateAllSceneImages() {
        val project = activeProject.value ?: return
        val list = scenes.value
        val validScenes = list.filter { it.visualDescription.isNotBlank() && !it.isApproved }
        if (validScenes.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("Semua scene belum terisi deskripsi atau sudah disetujui (terkunci)."))
            }
            return
        }

        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _batchGenState.value = BatchGenState(
                isRunning = true,
                currentIndex = 0,
                totalCount = validScenes.size,
                currentSceneTitle = validScenes.first().title
            )
            var successCount = 0
            for ((idx, scene) in validScenes.withIndex()) {
                _batchGenState.value = BatchGenState(
                    isRunning = true,
                    currentIndex = idx + 1,
                    totalCount = validScenes.size,
                    currentSceneTitle = "Scene ${scene.orderIndex + 1}: ${scene.title}"
                )
                _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Loading)
                val prompt = getEffectiveImagePrompt(scene)
                when (val res = repository.generateSceneImagePair(project, scene, characters.value, prompt)) {
                    is AiGenerationResult.Success -> {
                        successCount++
                        _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Idle)
                    }
                    is AiGenerationResult.Error -> {
                        _sceneGenStates.value = _sceneGenStates.value + (scene.id to SceneGenState.Error(res.message))
                    }
                }
            }
            _batchGenState.value = BatchGenState(isRunning = false)
            refreshCacheSize()
            _events.emit(UiEvent.ShowMessage("Selesai memproses $successCount dari ${validScenes.size} scene."))
        }
    }

    fun cancelBatchGeneration() {
        batchJob?.cancel()
        _batchGenState.value = BatchGenState(isRunning = false)
    }

    fun importSceneImage(scene: SceneUi, uri: Uri, asReferenceOnly: Boolean) {
        viewModelScope.launch {
            repository.importUserImageIntoScene(scene, uri, asReferenceOnly)
                .onSuccess {
                    refreshCacheSize()
                    val msg = if (asReferenceOnly) {
                        "Gambar referensi Scene ${scene.orderIndex + 1} disimpan."
                    } else {
                        "Gambar Scene ${scene.orderIndex + 1} berhasil dipasang."
                    }
                    _events.emit(UiEvent.ShowMessage(msg))
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage(err.localizedMessage ?: "Gagal mengimpor gambar."))
                }
        }
    }

    // Final MP4 Render & Export
    fun renderFinalMp4Video() {
        val project = activeProject.value ?: return
        val sceneList = scenes.value
        if (sceneList.isEmpty()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("Tambahkan minimal 1 scene sebelum render video.")) }
            return
        }

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _renderVideoState.value = RenderVideoState.Rendering(0.02f, "Menyiapkan encoder 1080×1920 30 FPS...")
            val result = repository.mp4VideoRenderer.renderFinalMp4(
                project = project,
                scenes = sceneList,
                onProgress = { progress, status ->
                    _renderVideoState.value = RenderVideoState.Rendering(progress, status)
                }
            )
            _renderVideoState.value = result
            if (result is RenderVideoState.Completed) {
                _events.emit(UiEvent.ShowMessage("Video MP4 final (${result.width}×${result.height} • ${result.fps} FPS) siap diunduh!"))
            }
        }
    }

    fun cancelMp4Render() {
        renderJob?.cancel()
        _renderVideoState.value = RenderVideoState.Idle
    }

    fun saveRenderedMp4ToUri(uri: Uri) {
        val state = _renderVideoState.value as? RenderVideoState.Completed ?: return
        viewModelScope.launch {
            repository.mp4VideoRenderer.copyMp4ToUri(state.mp4Path, uri)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Video MP4 berhasil disimpan!")) }
                .onFailure { _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal menyimpan MP4.")) }
        }
    }

    fun saveRenderedMp4ToGallery() {
        val state = _renderVideoState.value as? RenderVideoState.Completed ?: return
        val title = activeProject.value?.title ?: "StoryboardKu"
        viewModelScope.launch {
            repository.mp4VideoRenderer.saveMp4ToMoviesGallery(state.mp4Path, title)
                .onSuccess { name -> _events.emit(UiEvent.ShowMessage("Video tersimpan di Movies/StoryboardKu/$name")) }
                .onFailure { _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal menyimpan video.")) }
        }
    }

    fun copyPrompt(label: String, text: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.CopyToClipboard(label, text))
        }
    }

    fun copyAllImagePrompts() {
        val list = scenes.value
        val text = list.joinToString("\n\n") { s ->
            "Scene ${s.orderIndex + 1} — ${s.title}:\n${getEffectiveImagePrompt(s)}"
        }
        copyPrompt("Semua Prompt Gambar", text)
    }

    fun copyAllVideoPrompts() {
        val project = activeProject.value ?: return
        val text = repository.exportHelper.buildAllVideoPromptsText(project, characters.value, scenes.value)
        copyPrompt("Semua Prompt Video", text)
    }

    fun exportPdfToUri(uri: Uri) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            repository.exportHelper.exportStoryboardPdfToUri(uri, project, characters.value, scenes.value)
                .onSuccess { _events.emit(UiEvent.ShowMessage("Storyboard PDF berhasil disimpan!")) }
                .onFailure { _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal export PDF.")) }
        }
    }

    fun sharePdf() {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            repository.exportHelper.exportStoryboardPdfToTempFile(project, characters.value, scenes.value)
                .onSuccess { file ->
                    _events.emit(UiEvent.ShareFile(file.absolutePath, "application/pdf", "Bagikan Storyboard PDF"))
                }
                .onFailure { _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal membuat PDF.")) }
        }
    }

    fun exportAllImagesZipToUri(uri: Uri) {
        viewModelScope.launch {
            repository.exportHelper.exportAllImagesZip(uri, scenes.value)
                .onSuccess { count ->
                    _events.emit(UiEvent.ShowMessage("$count gambar berhasil diunduh ke file ZIP!"))
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage(err.localizedMessage ?: "Gagal mengunduh ZIP gambar."))
                }
        }
    }

    fun saveSceneImageToGallery(scene: SceneUi) {
        val path = scene.activeImagePath
        if (path == null) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("Belum ada gambar di scene ini.")) }
            return
        }
        viewModelScope.launch {
            repository.exportHelper.saveImageToGallery(path, scene.orderIndex + 1, scene.title)
                .onSuccess { name ->
                    _events.emit(UiEvent.ShowMessage("Tersimpan ke Galeri (Pictures/StoryboardKu/$name)"))
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage(err.localizedMessage ?: "Gagal menyimpan ke Galeri."))
                }
        }
    }

    fun exportShotListCsvToUri(uri: Uri) {
        val project = activeProject.value ?: return
        viewModelScope.launch {
            runCatching {
                val csv = repository.exportHelper.buildShotListCsv(project, characters.value, scenes.value)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Tidak bisa menulis file CSV.")
            }.onSuccess {
                _events.emit(UiEvent.ShowMessage("Daftar shot CSV (siap CapCut) berhasil disimpan!"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal export CSV."))
            }
        }
    }

    fun exportBackupJsonToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportAllProjectsJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Tidak bisa menyimpan file backup.")
            }.onSuccess {
                _events.emit(UiEvent.ShowMessage("Backup semua proyek (JSON) berhasil disimpan!"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(it.localizedMessage ?: "Gagal backup JSON."))
            }
        }
    }

    fun restoreBackupJsonFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Tidak bisa membaca file JSON.")
                repository.importBackupJson(text).getOrThrow()
            }.onSuccess { count ->
                _events.emit(UiEvent.ShowMessage("Berhasil memulihkan $count proyek dari backup!"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage("File backup tidak valid: ${it.localizedMessage}"))
            }
        }
    }

    // Settings Actions
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setTextScale(scale: TextScaleOption) = viewModelScope.launch { repository.setTextScale(scale) }
    fun setDefaultStyle(style: VisualStyle) = viewModelScope.launch { repository.setDefaultStyle(style) }
    fun setDefaultRatio(ratio: AspectRatioOption) = viewModelScope.launch { repository.setDefaultRatio(ratio) }
    fun setDefaultSceneDuration(sec: Int) = viewModelScope.launch { repository.setDefaultSceneDuration(sec) }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val refs = repository.getReferencedImagePaths()
            _cacheInfoBytes.value = repository.imageStorageHelper.getCacheAndImageSizeBytes(refs)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val refs = repository.getReferencedImagePaths()
            val freed = repository.imageStorageHelper.clearUnusedCache(refs)
            refreshCacheSize()
            val kb = (freed / 1024L).coerceAtLeast(0L)
            _events.emit(UiEvent.ShowMessage("Cache dibersihkan ($kb KB dibebaskan)."))
        }
    }
}
