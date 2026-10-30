package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.bridge.AndroidBridge
import com.example.bridge.ConsoleLogItem
import com.example.bridge.LogLevel
import com.example.data.model.WasmAppEntity
import com.example.ui.RunnerViewModel
import com.example.ui.components.ConsoleLogTerminal
import com.example.ui.components.DataExplorerSheet
import com.example.ui.components.FloatingHudMenu
import com.example.ui.components.PerformanceOverlay
import com.example.ui.components.VirtualCursorOverlay
import java.io.File
import java.io.FileInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WasmRunnerScreen(
    app: WasmAppEntity,
    viewModel: RunnerViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val logs by viewModel.logs.collectAsState()
    val logFilter by viewModel.logFilter.collectAsState()
    val isConsoleVisible by viewModel.isConsoleVisible.collectAsState()
    val isMenuOpen by viewModel.isMenuOpen.collectAsState()
    val viewportScale by viewModel.viewportScale.collectAsState()
    val isImmersive by viewModel.isImmersive.collectAsState()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    val isPerfOverlayVisible by viewModel.isPerfOverlayVisible.collectAsState()
    val perfMetrics by viewModel.perfMetrics.collectAsState()
    val isDataExplorerOpen by viewModel.isDataExplorerOpen.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val controlMode by viewModel.controlMode.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // File picker state for native bridge
    var pendingFileCallbackId by remember { mutableStateOf<String?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val callbackId = pendingFileCallbackId
        if (callbackId != null && uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: byteArrayOf()
                inputStream?.close()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                val js = "if(window.onNativeBridgeFilePicked){ window.onNativeBridgeFilePicked('$callbackId', true, '$fileName', '$base64'); }"
                webViewInstance?.evaluateJavascript(js, null)
            } catch (e: Exception) {
                val js = "if(window.onNativeBridgeFilePicked){ window.onNativeBridgeFilePicked('$callbackId', false, '', ''); }"
                webViewInstance?.evaluateJavascript(js, null)
            }
        } else if (callbackId != null) {
            val js = "if(window.onNativeBridgeFilePicked){ window.onNativeBridgeFilePicked('$callbackId', false, '', ''); }"
            webViewInstance?.evaluateJavascript(js, null)
        }
        pendingFileCallbackId = null
    }

    // Initialize ViewModel with App parameters
    LaunchedEffect(app.id) {
        viewModel.initializeWithApp(app)
    }

    // Immersive Mode controller
    LaunchedEffect(isImmersive) {
        if (activity != null) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isImmersive) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Clean up immersive mode when exiting
    DisposableEffect(Unit) {
        onDispose {
            if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Handle Android system Back button
    BackHandler {
        when {
            isDataExplorerOpen -> viewModel.setDataExplorerOpen(false)
            isConsoleVisible -> viewModel.setConsoleVisible(false)
            isMenuOpen -> viewModel.setMenuOpen(false)
            else -> onExit()
        }
    }

    // Reload trigger effect
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webViewInstance?.reload()
        }
    }

    // Update viewport meta when scale or desktop mode changes
    LaunchedEffect(viewportScale, isDesktopMode) {
        val wv = webViewInstance ?: return@LaunchedEffect
        val desktopWidth = if (isDesktopMode) 1280 else 0
        val scaleStr = viewportScale.toString()

        val viewportScript = """
            (function() {
                let meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                if ($isDesktopMode) {
                    meta.content = 'width=1280, initial-scale=' + ($scaleStr) + ', maximum-scale=3.0, user-scalable=yes';
                    document.body.style.width = '1280px';
                    document.body.style.minWidth = '1280px';
                    document.body.style.margin = '0 auto';
                } else {
                    meta.content = 'width=device-width, initial-scale=' + ($scaleStr) + ', maximum-scale=3.0, user-scalable=yes';
                    document.body.style.width = '100%';
                    document.body.style.minWidth = 'unset';
                    document.body.style.margin = '0';
                }
                window.dispatchEvent(new Event('resize'));
            })();
        """.trimIndent()
        wv.evaluateJavascript(viewportScript, null)
    }

    Box(modifier = modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        // AndroidView embedding the accelerated System WebView
        AndroidView(
            factory = { ctx ->
                // Enable debugging in dev container
                WebView.setWebContentsDebuggingEnabled(true)

                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.BLACK)

                    // Hardware Acceleration
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    val bridge = AndroidBridge(
                        context = ctx,
                        appId = app.id,
                        onOpenFilePicker = { callbackId, mimeType ->
                            pendingFileCallbackId = callbackId
                            filePickerLauncher.launch(if (mimeType.isNotBlank()) mimeType else "*/*")
                        },
                        onSaveFile = { callbackId, fileName, mimeType, base64Content ->
                            viewModel.addLog(
                                ConsoleLogItem(
                                    level = LogLevel.INFO,
                                    message = "[Bridge] Berkas '$fileName' disimpan oleh egui"
                                )
                            )
                        },
                        onNewLog = { logItem ->
                            viewModel.addLog(logItem)
                        },
                        onPerformanceMetrics = { fps, frameTimeMs, jsHeapMb, gpuBackend ->
                            viewModel.updateMetrics(fps, frameTimeMs, jsHeapMb, gpuBackend)
                        }
                    )
                    bridge.attachWebView(this)
                    addJavascriptInterface(bridge, "AndroidBridge")

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowFileAccessFromFileURLs = true
                        allowUniversalAccessFromFileURLs = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        offscreenPreRaster = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val lvl = when (it.messageLevel()) {
                                    ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                    ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARN
                                    ConsoleMessage.MessageLevel.LOG -> LogLevel.LOG
                                    else -> LogLevel.INFO
                                }
                                viewModel.addLog(
                                    ConsoleLogItem(
                                        level = lvl,
                                        message = "[Console] ${it.message()} (${it.sourceId()}:${it.lineNumber()})"
                                    )
                                )
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            // Inject high-performance WebGL / WebGPU hook and FPS monitoring loop
                            val accelerationInitScript = """
                                (function() {
                                    // Intercept WebGL context creation to enforce maximum GPU acceleration
                                    const origGetContext = HTMLCanvasElement.prototype.getContext;
                                    let detectedGPU = 'WebGL (Standard)';
                                    
                                    HTMLCanvasElement.prototype.getContext = function(type, attributes) {
                                        let finalAttributes = attributes || {};
                                        if (type === 'webgl' || type === 'experimental-webgl' || type === 'webgl2') {
                                            finalAttributes = Object.assign({
                                                powerPreference: 'high-performance',
                                                desynchronized: true,
                                                antialias: true,
                                                failIfMajorPerformanceCaveat: false,
                                                preserveDrawingBuffer: false
                                            }, finalAttributes);
                                        }
                                        const ctx = origGetContext.call(this, type, finalAttributes);
                                        if (ctx && (type === 'webgl' || type === 'webgl2')) {
                                            try {
                                                const dbg = ctx.getExtension('WEBGL_debug_renderer_info');
                                                if (dbg) {
                                                    const renderer = ctx.getParameter(dbg.UNMASKED_RENDERER_WEBGL);
                                                    detectedGPU = (type === 'webgl2' ? 'WebGL 2.0: ' : 'WebGL 1.0: ') + renderer;
                                                } else {
                                                    detectedGPU = type === 'webgl2' ? 'WebGL 2.0 (Accelerated)' : 'WebGL 1.0 (Accelerated)';
                                                }
                                            } catch(e) {
                                                detectedGPU = 'WebGL (Accelerated)';
                                            }
                                        }
                                        return ctx;
                                    };

                                    // Check experimental WebGPU support
                                    if (navigator.gpu) {
                                        navigator.gpu.requestAdapter({ powerPreference: 'high-performance' })
                                            .then(adapter => {
                                                if (adapter) {
                                                    detectedGPU = 'WebGPU (Experimental Hardware)';
                                                    if (window.AndroidBridge && window.AndroidBridge.sendConsoleLog) {
                                                        window.AndroidBridge.sendConsoleLog('INFO', '[Hardware] WebGPU Adapter ditemukan: ' + adapter.name);
                                                    }
                                                }
                                            })
                                            .catch(() => {});
                                    }

                                    // Intercept standard console methods
                                    const origLog = console.log;
                                    const origWarn = console.warn;
                                    const origError = console.error;
                                    console.log = function(...args) {
                                        origLog.apply(console, args);
                                        if (window.AndroidBridge && window.AndroidBridge.sendConsoleLog) {
                                            window.AndroidBridge.sendConsoleLog('LOG', args.join(' '));
                                        }
                                    };
                                    console.warn = function(...args) {
                                        origWarn.apply(console, args);
                                        if (window.AndroidBridge && window.AndroidBridge.sendConsoleLog) {
                                            window.AndroidBridge.sendConsoleLog('WARN', args.join(' '));
                                        }
                                    };
                                    console.error = function(...args) {
                                        origError.apply(console, args);
                                        if (window.AndroidBridge && window.AndroidBridge.sendConsoleLog) {
                                            window.AndroidBridge.sendConsoleLog('ERROR', args.join(' '));
                                        }
                                    };

                                    // FPS & Frame Timing Loop
                                    let lastTime = performance.now();
                                    let frameCount = 0;
                                    let accumulatedFrameTime = 0;

                                    function renderLoop(now) {
                                        frameCount++;
                                        const delta = now - lastTime;
                                        accumulatedFrameTime += delta;
                                        lastTime = now;

                                        if (accumulatedFrameTime >= 500) {
                                            const currentFps = Math.round((frameCount * 1000) / accumulatedFrameTime);
                                            const avgFrameTime = accumulatedFrameTime / frameCount;
                                            frameCount = 0;
                                            accumulatedFrameTime = 0;

                                            let jsHeapMb = 0;
                                            if (performance.memory && performance.memory.usedJSHeapSize) {
                                                jsHeapMb = performance.memory.usedJSHeapSize / (1024 * 1024);
                                            }

                                            if (window.AndroidBridge && window.AndroidBridge.updatePerformanceMetrics) {
                                                window.AndroidBridge.updatePerformanceMetrics(
                                                    currentFps,
                                                    avgFrameTime.toFixed(1),
                                                    jsHeapMb.toFixed(1),
                                                    detectedGPU
                                                );
                                            }
                                        }
                                        requestAnimationFrame(renderLoop);
                                    }
                                    requestAnimationFrame(renderLoop);
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(accelerationInitScript, null)
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            // Serve local applet assets smoothly
                            if (url.scheme == "file" || url.toString().startsWith("file:///")) {
                                val filePath = url.path
                                if (filePath != null) {
                                    val localFile = File(filePath)
                                    if (localFile.exists()) {
                                        val mime = when {
                                            filePath.endsWith(".wasm") -> "application/wasm"
                                            filePath.endsWith(".js") -> "application/javascript"
                                            filePath.endsWith(".html") -> "text/html"
                                            filePath.endsWith(".css") -> "text/css"
                                            filePath.endsWith(".png") -> "image/png"
                                            filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") -> "image/jpeg"
                                            filePath.endsWith(".svg") -> "image/svg+xml"
                                            filePath.endsWith(".json") -> "application/json"
                                            else -> "application/octet-stream"
                                        }
                                        val headers = HashMap<String, String>().apply {
                                            put("Cross-Origin-Opener-Policy", "same-origin")
                                            put("Cross-Origin-Embedder-Policy", "require-corp")
                                            put("Access-Control-Allow-Origin", "*")
                                        }
                                        return WebResourceResponse(mime, "UTF-8", 200, "OK", headers, FileInputStream(localFile))
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // Load index / entry file
                    val targetFile = File(ctx.filesDir, "wasm_apps/${app.directoryName}/${app.entryHtml}")
                    if (targetFile.exists()) {
                        loadUrl("file://${targetFile.absolutePath}")
                    } else {
                        // Fallback search in directory for any html
                        val appDir = File(ctx.filesDir, "wasm_apps/${app.directoryName}")
                        val fallbackHtml = appDir.listFiles()?.firstOrNull { it.name.endsWith(".html") }
                        if (fallbackHtml != null) {
                            loadUrl("file://${fallbackHtml.absolutePath}")
                        } else {
                            loadDataWithBaseURL(null, "<h3>Berkas entry '${app.entryHtml}' tidak ditemukan.</h3>", "text/html", "UTF-8", null)
                        }
                    }

                    webViewInstance = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("wasm_webview")
        )

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Virtual Cursor Overlay (for LMB / RMB desktop control)
        VirtualCursorOverlay(
            webView = webViewInstance,
            enabled = controlMode == "cursor",
            onToggleMode = { viewModel.toggleControlMode() }
        )

        // Performance HUD overlay (Top-Start)
        PerformanceOverlay(
            metrics = perfMetrics,
            isVisible = isPerfOverlayVisible,
            onClose = { viewModel.setPerfOverlayVisible(false) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
        )

        // Floating HUD Menu (Settings, Viewport Scale, Immersive, Console, Desktop, Reload)
        FloatingHudMenu(
            isOpen = isMenuOpen,
            onToggleOpen = { viewModel.toggleMenu() },
            currentScale = viewportScale,
            onSelectScale = { viewModel.setScale(it) },
            isConsoleVisible = isConsoleVisible,
            onToggleConsole = { viewModel.toggleConsole() },
            isPerfOverlayVisible = isPerfOverlayVisible,
            onTogglePerfOverlay = { viewModel.togglePerfOverlay() },
            isImmersive = isImmersive,
            onToggleImmersive = { viewModel.toggleImmersive() },
            isDesktopMode = isDesktopMode,
            onToggleDesktopMode = { viewModel.toggleDesktopMode() },
            controlMode = controlMode,
            onSelectControlMode = { viewModel.setControlMode(it) },
            onOpenDataExplorer = { viewModel.setDataExplorerOpen(true) },
            onReload = { viewModel.triggerReload() },
            onExit = onExit
        )

        // Console Log Terminal Sheet
        ConsoleLogTerminal(
            isVisible = isConsoleVisible,
            logs = logs,
            currentFilter = logFilter,
            onFilterChange = { viewModel.setLogFilter(it) },
            onClearLogs = { viewModel.clearLogs() },
            onClose = { viewModel.setConsoleVisible(false) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Data Explorer Sheet
        if (isDataExplorerOpen) {
            DataExplorerSheet(
                app = app,
                onDismiss = { viewModel.setDataExplorerOpen(false) }
            )
        }
    }
}
