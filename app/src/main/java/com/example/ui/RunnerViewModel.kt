package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.bridge.ConsoleLogItem
import com.example.bridge.LogLevel
import com.example.bridge.PerformanceMetrics
import com.example.data.model.WasmAppEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunnerViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<ConsoleLogItem>>(emptyList())
    val logs: StateFlow<List<ConsoleLogItem>> = _logs.asStateFlow()

    private val _logFilter = MutableStateFlow<LogLevel?>(null)
    val logFilter: StateFlow<LogLevel?> = _logFilter.asStateFlow()

    private val _isConsoleVisible = MutableStateFlow(false)
    val isConsoleVisible: StateFlow<Boolean> = _isConsoleVisible.asStateFlow()

    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen: StateFlow<Boolean> = _isMenuOpen.asStateFlow()

    private val _viewportScale = MutableStateFlow(1.0f)
    val viewportScale: StateFlow<Float> = _viewportScale.asStateFlow()

    private val _isImmersive = MutableStateFlow(true)
    val isImmersive: StateFlow<Boolean> = _isImmersive.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    // Performance Overlay state
    private val _isPerfOverlayVisible = MutableStateFlow(true)
    val isPerfOverlayVisible: StateFlow<Boolean> = _isPerfOverlayVisible.asStateFlow()

    private val _perfMetrics = MutableStateFlow(PerformanceMetrics())
    val perfMetrics: StateFlow<PerformanceMetrics> = _perfMetrics.asStateFlow()

    // Data Explorer state
    private val _isDataExplorerOpen = MutableStateFlow(false)
    val isDataExplorerOpen: StateFlow<Boolean> = _isDataExplorerOpen.asStateFlow()

    // Desktop mode state (wide screen simulation for egui)
    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

    // Control mode state ("touch" vs "cursor")
    private val _controlMode = MutableStateFlow("touch")
    val controlMode: StateFlow<String> = _controlMode.asStateFlow()

    fun initializeWithApp(app: WasmAppEntity) {
        _isImmersive.value = app.isImmersive
        _viewportScale.value = app.preferredScale
        _isDesktopMode.value = app.isDesktopMode
        _controlMode.value = app.controlMode
        _logs.value = listOf(
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Memuat runtime egui untuk '${app.name}' (${app.entryHtml})"
            ),
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Mode Tampilan: ${if (app.isDesktopMode) "Desktop (1280px)" else "Mobile"} | Kontrol: ${if (app.controlMode == "cursor") "Kursor Virtual (RMB/LMB)" else "Sentuhan Langsung"}"
            ),
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Pipeline Grafis: Akselerasi Perangkat Keras WebGL / WebGPU diaktifkan"
            )
        )
    }

    fun updateMetrics(fps: Int, frameTimeMs: Float, jsHeapMb: Float, gpuBackend: String = "WebGL 2.0 (Accelerated)") {
        val runtime = Runtime.getRuntime()
        val appUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val currentHistory = _perfMetrics.value.fpsHistory.toMutableList()
        if (currentHistory.size >= 25) {
            currentHistory.removeAt(0)
        }
        currentHistory.add(fps.coerceIn(0, 120))

        _perfMetrics.value = PerformanceMetrics(
            fps = fps,
            frameTimeMs = frameTimeMs,
            jsHeapMb = jsHeapMb,
            appHeapMb = appUsedMb,
            fpsHistory = currentHistory,
            gpuBackend = gpuBackend
        )
    }

    fun togglePerfOverlay() {
        _isPerfOverlayVisible.value = !_isPerfOverlayVisible.value
    }

    fun setPerfOverlayVisible(visible: Boolean) {
        _isPerfOverlayVisible.value = visible
    }

    fun toggleDataExplorer() {
        _isDataExplorerOpen.value = !_isDataExplorerOpen.value
    }

    fun setDataExplorerOpen(open: Boolean) {
        _isDataExplorerOpen.value = open
    }

    fun addLog(item: ConsoleLogItem) {
        val current = _logs.value.toMutableList()
        if (current.size > 500) {
            current.removeAt(0)
        }
        current.add(item)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun setLogFilter(level: LogLevel?) {
        _logFilter.value = level
    }

    fun toggleConsole() {
        _isConsoleVisible.value = !_isConsoleVisible.value
    }

    fun setConsoleVisible(visible: Boolean) {
        _isConsoleVisible.value = visible
    }

    fun toggleMenu() {
        _isMenuOpen.value = !_isMenuOpen.value
    }

    fun setMenuOpen(open: Boolean) {
        _isMenuOpen.value = open
    }

    fun setScale(scale: Float) {
        _viewportScale.value = scale
    }

    fun toggleImmersive() {
        _isImmersive.value = !_isImmersive.value
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
        addLog(
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Mode Desktop diubah menjadi: ${_isDesktopMode.value}"
            )
        )
    }

    fun setDesktopMode(enabled: Boolean) {
        _isDesktopMode.value = enabled
    }

    fun toggleControlMode() {
        val nextMode = if (_controlMode.value == "cursor") "touch" else "cursor"
        _controlMode.value = nextMode
        addLog(
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Mode Kontrol beralih ke: ${if (nextMode == "cursor") "Kursor Virtual (RMB/LMB)" else "Sentuhan Langsung"}"
            )
        )
    }

    fun setControlMode(mode: String) {
        _controlMode.value = mode
        addLog(
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Mode Kontrol dipilih: ${if (mode == "cursor") "Kursor Virtual" else "Sentuhan"}"
            )
        )
    }

    fun triggerReload() {
        _reloadTrigger.value = _reloadTrigger.value + 1
        addLog(
            ConsoleLogItem(
                level = LogLevel.INFO,
                message = "Memuat ulang lingkungan eksekusi WASM..."
            )
        )
    }
}
