package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.manager.WasmFileManager
import com.example.data.model.WasmAppEntity
import com.example.data.repository.WasmAppRepository
import com.example.util.ShortcutHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ImportUiState {
    object Idle : ImportUiState
    object Loading : ImportUiState
    data class Success(val app: WasmAppEntity) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WasmAppRepository
    private val fileManager: WasmFileManager = WasmFileManager(application)

    val appsList: StateFlow<List<WasmAppEntity>>

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _activeApp = MutableStateFlow<WasmAppEntity?>(null)
    val activeApp: StateFlow<WasmAppEntity?> = _activeApp.asStateFlow()

    private val _selectedAppForDetails = MutableStateFlow<WasmAppEntity?>(null)
    val selectedAppForDetails: StateFlow<WasmAppEntity?> = _selectedAppForDetails.asStateFlow()

    private val _selectedAppForIconCustomizer = MutableStateFlow<WasmAppEntity?>(null)
    val selectedAppForIconCustomizer: StateFlow<WasmAppEntity?> = _selectedAppForIconCustomizer.asStateFlow()

    private val _selectedAppForDataExplorer = MutableStateFlow<WasmAppEntity?>(null)
    val selectedAppForDataExplorer: StateFlow<WasmAppEntity?> = _selectedAppForDataExplorer.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = WasmAppRepository(db.wasmAppDao())
        appsList = repository.allApps.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun launchApp(app: WasmAppEntity) {
        _activeApp.value = app
    }

    fun loadAndLaunchAppById(appId: String) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                _activeApp.value = app
            }
        }
    }

    fun closeActiveApp() {
        _activeApp.value = null
    }

    fun selectAppForDetails(app: WasmAppEntity?) {
        _selectedAppForDetails.value = app
    }

    fun selectAppForIconCustomizer(app: WasmAppEntity?) {
        _selectedAppForIconCustomizer.value = app
    }

    fun selectAppForDataExplorer(app: WasmAppEntity?) {
        _selectedAppForDataExplorer.value = app
    }

    fun clearImportState() {
        _importState.value = ImportUiState.Idle
    }

    fun importZip(
        uri: Uri,
        name: String,
        description: String,
        isImmersive: Boolean,
        isDesktopMode: Boolean = false,
        controlMode: String = "touch",
        scale: Float = 1.0f
    ) {
        viewModelScope.launch {
            _importState.value = ImportUiState.Loading
            val appId = "app_" + UUID.randomUUID().toString().take(8)
            val result = fileManager.extractZipPackage(
                zipUri = uri,
                appId = appId,
                appName = name,
                description = description,
                isImmersive = isImmersive,
                preferredScale = scale,
                isDesktopMode = isDesktopMode,
                controlMode = controlMode
            )

            result.fold(
                onSuccess = { appEntity ->
                    repository.insertApp(appEntity)
                    _importState.value = ImportUiState.Success(appEntity)
                },
                onFailure = { error ->
                    _importState.value = ImportUiState.Error(
                        error.message ?: "Gagal mengekstrak arsip paket WASM"
                    )
                }
            )
        }
    }

    fun installDemoApp() {
        viewModelScope.launch {
            _importState.value = ImportUiState.Loading
            val result = fileManager.installDemoShowcase()
            result.fold(
                onSuccess = { demoEntity ->
                    repository.insertApp(demoEntity)
                    _importState.value = ImportUiState.Success(demoEntity)
                },
                onFailure = { error ->
                    _importState.value = ImportUiState.Error(
                        error.message ?: "Gagal memasang demo bawaan"
                    )
                }
            )
        }
    }

    fun deleteApp(app: WasmAppEntity) {
        viewModelScope.launch {
            fileManager.deleteAppFiles(app.directoryName)
            repository.deleteApp(app)
            if (_selectedAppForDetails.value?.id == app.id) {
                _selectedAppForDetails.value = null
            }
            if (_selectedAppForIconCustomizer.value?.id == app.id) {
                _selectedAppForIconCustomizer.value = null
            }
            if (_activeApp.value?.id == app.id) {
                _activeApp.value = null
            }
        }
    }

    fun updateAppSettings(
        id: String,
        isImmersive: Boolean,
        scale: Float,
        isDesktopMode: Boolean,
        controlMode: String
    ) {
        viewModelScope.launch {
            repository.updateSettings(id, isImmersive, scale, isDesktopMode, controlMode)
            val updated = repository.getAppById(id)
            if (_selectedAppForDetails.value?.id == id) {
                _selectedAppForDetails.value = updated
            }
            if (_activeApp.value?.id == id) {
                _activeApp.value = updated
            }
        }
    }

    fun updateAppIcon(
        id: String,
        preset: String,
        customPath: String,
        bgColorHex: String,
        pinToHome: Boolean
    ): Boolean {
        viewModelScope.launch {
            repository.updateIconSettings(id, preset, customPath, bgColorHex)
            val updated = repository.getAppById(id)
            if (_selectedAppForDetails.value?.id == id) {
                _selectedAppForDetails.value = updated
            }
            if (_selectedAppForIconCustomizer.value?.id == id) {
                _selectedAppForIconCustomizer.value = null
            }
            if (_activeApp.value?.id == id) {
                _activeApp.value = updated
            }
        }

        if (pinToHome) {
            val app = appsList.value.firstOrNull { it.id == id }
            if (app != null) {
                val updatedApp = app.copy(
                    iconPreset = preset,
                    customIconPath = customPath,
                    iconBgColorHex = bgColorHex
                )
                return ShortcutHelper.pinAppShortcut(getApplication(), updatedApp)
            }
        }
        return true
    }

    fun formatSize(bytes: Long): String = fileManager.formatFileSize(bytes)
}
