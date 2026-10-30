package com.example.data.manager

import android.content.Context
import android.net.Uri
import com.example.data.model.WasmAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class WasmFileManager(private val context: Context) {

    private val baseAppsDir: File = File(context.filesDir, "wasm_apps").apply {
        if (!exists()) mkdirs()
    }

    fun getAppDirectory(appId: String): File {
        return File(baseAppsDir, appId)
    }

    suspend fun extractZipPackage(
        zipUri: Uri,
        appId: String,
        appName: String,
        description: String,
        isImmersive: Boolean,
        preferredScale: Float = 1.0f,
        isDesktopMode: Boolean = false,
        controlMode: String = "touch"
    ): Result<WasmAppEntity> = withContext(Dispatchers.IO) {
        try {
            val appDir = File(baseAppsDir, appId)
            if (appDir.exists()) {
                appDir.deleteRecursively()
            }
            appDir.mkdirs()

            var fileCount = 0
            var totalBytes = 0L
            var foundWasm = false
            var wasmName = ""

            val inputStream = context.contentResolver.openInputStream(zipUri)
                ?: return@withContext Result.failure(Exception("Tidak dapat membuka file ZIP"))

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val targetFile = File(appDir, entryName)
                    val canonicalPath = targetFile.canonicalPath
                    if (!canonicalPath.startsWith(appDir.canonicalPath)) {
                        throw SecurityException("Zip entry berada di luar target direktori: $entryName")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        fileCount++
                        val length = targetFile.length()
                        totalBytes += length
                        if (targetFile.name.endsWith(".wasm", ignoreCase = true)) {
                            foundWasm = true
                            wasmName = targetFile.name
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            var entryHtml = "index.html"
            val directIndex = File(appDir, "index.html")
            if (!directIndex.exists()) {
                val children = appDir.listFiles()?.filter { it.isDirectory }
                val nestedIndex = children?.firstOrNull { File(it, "index.html").exists() }
                if (nestedIndex != null) {
                    entryHtml = "${nestedIndex.name}/index.html"
                } else {
                    val foundAny = appDir.walkTopDown().firstOrNull { it.isFile && it.name.equals("index.html", ignoreCase = true) }
                    if (foundAny != null) {
                        entryHtml = foundAny.relativeTo(appDir).path
                    } else {
                        return@withContext Result.failure(Exception("File 'index.html' tidak ditemukan di dalam arsip ZIP"))
                    }
                }
            }

            val appEntity = WasmAppEntity(
                id = appId,
                name = appName.ifBlank { "WASM App" },
                description = description,
                directoryName = appId,
                entryHtml = entryHtml,
                hasWasm = foundWasm,
                wasmFileName = wasmName,
                fileCount = fileCount,
                totalSizeBytes = totalBytes,
                importedAt = System.currentTimeMillis(),
                isImmersive = isImmersive,
                preferredScale = preferredScale,
                colorHex = if (foundWasm) "#E07A5F" else "#38BDF8",
                isDesktopMode = isDesktopMode,
                controlMode = controlMode
            )

            Result.success(appEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installDemoShowcase(): Result<WasmAppEntity> = withContext(Dispatchers.IO) {
        try {
            val demoId = "demo_egui"
            val demoDir = File(baseAppsDir, demoId)
            if (demoDir.exists()) {
                demoDir.deleteRecursively()
            }
            demoDir.mkdirs()

            val indexFile = File(demoDir, "index.html")
            indexFile.writeText(getDemoHtmlContent())

            val sampleWasm = File(demoDir, "egui_core.wasm")
            sampleWasm.writeBytes(byteArrayOf(0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00))

            val totalBytes = indexFile.length() + sampleWasm.length()

            val demoEntity = WasmAppEntity(
                id = demoId,
                name = "egui Showcase (Hardware WebGL/WebGPU)",
                description = "Simulasi runtime egui v0.28 dengan akselerasi GPU, menu konteks RMB, dan integrasi AndroidBridge.",
                directoryName = demoId,
                entryHtml = "index.html",
                hasWasm = true,
                wasmFileName = "egui_core.wasm",
                fileCount = 2,
                totalSizeBytes = totalBytes,
                importedAt = System.currentTimeMillis(),
                isImmersive = true,
                preferredScale = 1.0f,
                colorHex = "#E07A5F",
                iconPreset = "sports_esports",
                isDesktopMode = false,
                controlMode = "touch"
            )

            Result.success(demoEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteAppFiles(appId: String): Boolean {
        val dir = File(baseAppsDir, appId)
        return if (dir.exists()) dir.deleteRecursively() else true
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = String.format("%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
        return "$formatted ${units[digitGroups]}"
    }

    private fun getDemoHtmlContent(): String {
        return """
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
    <title>egui WASM Runner</title>
    <style>
        * { box-sizing: border-box; user-select: none; -webkit-user-select: none; }
        html, body {
            margin: 0; padding: 0; width: 100%; height: 100%;
            background-color: #121316; overflow: hidden;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #eceff4;
        }
        #canvas-container { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 1; }
        canvas { width: 100%; height: 100%; display: block; }
        .egui-window {
            position: absolute; top: 20px; left: 20px; width: 310px;
            background: rgba(30, 33, 40, 0.95);
            border: 1px solid #3c424f; border-radius: 8px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.7);
            z-index: 10; overflow: hidden;
        }
        .egui-titlebar {
            background: #252830; padding: 8px 12px;
            font-size: 13px; font-weight: bold; color: #eceff4;
            display: flex; justify-content: space-between; align-items: center;
            cursor: move; border-bottom: 1px solid #3c424f;
        }
        .egui-titlebar .badge {
            background: #e07a5f; color: #fff; font-size: 10px;
            padding: 2px 6px; border-radius: 4px; text-transform: uppercase;
        }
        .egui-content { padding: 12px; font-size: 12px; }
        .egui-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
        .egui-btn {
            background: #2d313b; border: 1px solid #434956; color: #eceff4;
            padding: 6px 12px; border-radius: 4px; font-size: 11px; font-weight: 500;
            cursor: pointer; flex: 1; text-align: center;
        }
        .egui-btn:active { background: #3e4452; }
        .egui-btn.accent { background: #e07a5f; border-color: #e07a5f; color: #fff; }
        .egui-input {
            width: 100%; background: #1a1c22; border: 1px solid #3c424f;
            color: #eceff4; padding: 6px 8px; border-radius: 4px;
            font-size: 11px; margin-bottom: 8px;
        }
        .egui-slider-box { margin-bottom: 8px; }
        .egui-slider-box input[type=range] { width: 100%; }
        .egui-section-title {
            font-size: 10px; font-weight: bold; color: #88c0d0;
            text-transform: uppercase; margin: 10px 0 6px 0; letter-spacing: 0.5px;
        }
        .status-pill {
            background: rgba(46, 52, 64, 0.8); border: 1px solid #434c5e;
            padding: 4px 8px; border-radius: 4px; font-size: 10px; font-family: monospace;
            color: #a3be8c; margin-top: 6px;
        }
        #fps-hud {
            position: absolute; bottom: 16px; left: 16px;
            background: rgba(18, 19, 22, 0.7); border: 1px solid #3c424f;
            padding: 4px 10px; border-radius: 4px; font-size: 11px;
            font-family: monospace; color: #88c0d0; pointer-events: none; z-index: 5;
        }
    </style>
</head>
<body>
    <div id="canvas-container">
        <canvas id="eguiCanvas"></canvas>
    </div>

    <!-- egui Context Menu for Right-Click / Kursor Virtual RMB -->
    <div id="eguiContextMenu" style="display:none; position:fixed; z-index:9999; background:#242933; border:1px solid #88c0d0; border-radius:6px; box-shadow:0 8px 24px rgba(0,0,0,0.7); padding:6px; min-width:180px; font-family:sans-serif; font-size:12px; color:#eceff4;">
        <div style="padding:4px 8px; font-weight:bold; color:#88c0d0; border-bottom:1px solid #3b4252; margin-bottom:4px;">🖱️ egui Menu Konteks (RMB)</div>
        <div id="ctxQuickSave" style="padding:6px 8px; cursor:pointer; border-radius:4px;">💾 Simpan Cepat (Quick Save)</div>
        <div id="ctxResetWin" style="padding:6px 8px; cursor:pointer; border-radius:4px;">🔄 Reset Posisi Window</div>
        <div id="ctxToggleColor" style="padding:6px 8px; cursor:pointer; border-radius:4px;">🎨 Ubah Warna Partikel</div>
        <div id="ctxClose" style="padding:6px 8px; cursor:pointer; border-radius:4px; color:#bf616a;">❌ Tutup Menu</div>
    </div>

    <!-- egui Floating Window -->
    <div class="egui-window" id="eguiWindow">
        <div class="egui-titlebar" id="titleBar">
            <span>egui v0.28.0 (WASM Runner)</span>
            <span class="badge" id="backendBadge">WebGL 2.0</span>
        </div>
        <div class="egui-content">
            <div class="egui-section-title">Canvas Graphics Controls</div>
            <div class="egui-slider-box">
                <div class="egui-row">
                    <span>Mesh Speed</span>
                    <span id="speedVal">1.0x</span>
                </div>
                <input type="range" id="speedSlider" min="0.1" max="3" step="0.1" value="1.0">
            </div>

            <div class="egui-slider-box">
                <div class="egui-row">
                    <span>Node Density</span>
                    <span id="densityVal">28</span>
                </div>
                <input type="range" id="densitySlider" min="10" max="60" step="2" value="28">
            </div>

            <div class="egui-section-title">Native Android Bridge API</div>
            <div class="egui-row">
                <button class="egui-btn accent" id="btnFilePicker">📁 Open File Picker</button>
                <button class="egui-btn" id="btnClipboard">📋 Copy Info</button>
            </div>

            <input type="text" class="egui-input" id="storageInput" placeholder="Ketik catatan..." value="Catatan via AndroidBridge">
            <div class="egui-row">
                <button class="egui-btn" id="btnSaveStorage">💾 Save Storage</button>
                <button class="egui-btn" id="btnReadStorage">📖 Read Storage</button>
            </div>

            <div class="egui-row">
                <button class="egui-btn accent" id="btnWriteSavefile">📦 Buat Savefile Game (.json)</button>
            </div>

            <div class="egui-section-title">Debug & Console Logs</div>
            <div class="egui-row">
                <button class="egui-btn" id="btnLogInfo">ℹ️ Log Info</button>
                <button class="egui-btn" id="btnLogWarn">⚠️ Log Warn</button>
                <button class="egui-btn" id="btnLogError">❌ Log Error</button>
            </div>

            <div class="status-pill" id="bridgeStatus">Bridge: Ready (Hardware Accelerated)</div>
        </div>
    </div>

    <div id="fps-hud">FPS: 60 | Touch: Active | WGPU/GL: Ready</div>

    <script>
        const canvas = document.getElementById('eguiCanvas');
        const ctx = canvas.getContext('2d');
        let speed = 1.0;
        let nodeCount = 28;
        let angle = 0;

        function resizeCanvas() {
            canvas.width = window.innerWidth * window.devicePixelRatio;
            canvas.height = window.innerHeight * window.devicePixelRatio;
            ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
        }
        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();

        const particles = [];
        for (let i = 0; i < 60; i++) {
            particles.push({
                x: Math.random() * window.innerWidth,
                y: Math.random() * window.innerHeight,
                vx: (Math.random() - 0.5) * 1.5,
                vy: (Math.random() - 0.5) * 1.5,
                radius: Math.random() * 3 + 1
            });
        }

        function render() {
            ctx.clearRect(0, 0, window.innerWidth, window.innerHeight);
            ctx.strokeStyle = '#2d3748';
            ctx.lineWidth = 0.5;

            for (let i = 0; i < nodeCount; i++) {
                const p = particles[i];
                p.x += p.vx * speed;
                p.y += p.vy * speed;
                if (p.x < 0 || p.x > window.innerWidth) p.vx *= -1;
                if (p.y < 0 || p.y > window.innerHeight) p.vy *= -1;

                ctx.fillStyle = '#e07a5f';
                ctx.beginPath();
                ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
                ctx.fill();

                for (let j = i + 1; j < nodeCount; j++) {
                    const p2 = particles[j];
                    const dx = p.x - p2.x;
                    const dy = p.y - p2.y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 110) {
                        ctx.strokeStyle = 'rgba(136, 192, 208, ' + (1 - dist / 110) * 0.4 + ')';
                        ctx.beginPath();
                        ctx.moveTo(p.x, p.y);
                        ctx.lineTo(p2.x, p2.y);
                        ctx.stroke();
                    }
                }
            }
            requestAnimationFrame(render);
        }
        render();

        // Drag egui window
        const eguiWin = document.getElementById('eguiWindow');
        const titleBar = document.getElementById('titleBar');
        let isDragging = false;
        let dragOffX = 0, dragOffY = 0;

        titleBar.addEventListener('pointerdown', (e) => {
            isDragging = true;
            dragOffX = e.clientX - eguiWin.offsetLeft;
            dragOffY = e.clientY - eguiWin.offsetTop;
            window.addEventListener('pointermove', onMove);
            window.addEventListener('pointerup', onUp);
        });

        function onMove(e) {
            if (!isDragging) return;
            eguiWin.style.left = (e.clientX - dragOffX) + 'px';
            eguiWin.style.top = (e.clientY - dragOffY) + 'px';
        }
        function onUp() {
            isDragging = false;
            window.removeEventListener('pointermove', onMove);
            window.removeEventListener('pointerup', onUp);
        }

        // Sliders
        document.getElementById('speedSlider').addEventListener('input', (e) => {
            speed = parseFloat(e.target.value);
            document.getElementById('speedVal').innerText = speed.toFixed(1) + 'x';
        });
        document.getElementById('densitySlider').addEventListener('input', (e) => {
            nodeCount = parseInt(e.target.value);
            document.getElementById('densityVal').innerText = nodeCount;
        });

        // Context Menu
        const ctxMenu = document.getElementById('eguiContextMenu');
        window.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            ctxMenu.style.display = 'block';
            ctxMenu.style.left = Math.min(e.clientX, window.innerWidth - 190) + 'px';
            ctxMenu.style.top = Math.min(e.clientY, window.innerHeight - 170) + 'px';
            console.info('egui demo: Context menu (RMB) terbuka di (' + Math.round(e.clientX) + ', ' + Math.round(e.clientY) + ')');
        });
        window.addEventListener('click', (e) => {
            if (ctxMenu && !ctxMenu.contains(e.target)) ctxMenu.style.display = 'none';
        });
        document.getElementById('ctxClose').addEventListener('click', () => { ctxMenu.style.display = 'none'; });
        document.getElementById('ctxResetWin').addEventListener('click', () => {
            eguiWin.style.left = '20px'; eguiWin.style.top = '20px'; ctxMenu.style.display = 'none';
        });
        document.getElementById('ctxQuickSave').addEventListener('click', () => {
            document.getElementById('btnWriteSavefile').click(); ctxMenu.style.display = 'none';
        });

        // Bridge calls
        document.getElementById('btnFilePicker').addEventListener('click', () => {
            if (window.AndroidBridge && window.AndroidBridge.openFilePicker) {
                window.AndroidBridge.openFilePicker('picker_egui', '*/*');
            }
        });
        document.getElementById('btnClipboard').addEventListener('click', () => {
            const info = 'egui WASM Runner | Screen: ' + window.innerWidth + 'x' + window.innerHeight;
            if (window.AndroidBridge && window.AndroidBridge.copyToClipboard) {
                window.AndroidBridge.copyToClipboard(info);
            }
        });
        document.getElementById('btnSaveStorage').addEventListener('click', () => {
            const val = document.getElementById('storageInput').value;
            if (window.AndroidBridge && window.AndroidBridge.writeAppStorage) {
                window.AndroidBridge.writeAppStorage('user_note', val);
            }
        });
        document.getElementById('btnReadStorage').addEventListener('click', () => {
            if (window.AndroidBridge && window.AndroidBridge.readAppStorage) {
                window.AndroidBridge.readAppStorage('user_note', 'read_note');
            }
        });
        document.getElementById('btnWriteSavefile').addEventListener('click', () => {
            const saveData = JSON.stringify({ slot: 1, level: 8, score: 14250, savedAt: new Date().toISOString() }, null, 2);
            if (window.AndroidBridge && window.AndroidBridge.writeSaveFile) {
                window.AndroidBridge.writeSaveFile('save_slot_1.json', saveData);
            }
        });
        document.getElementById('btnLogInfo').addEventListener('click', () => console.info('egui: GPU Pipeline status normal.'));
        document.getElementById('btnLogWarn').addEventListener('click', () => console.warn('egui: Frame pacing mendekati batas 60fps.'));
        document.getElementById('btnLogError').addEventListener('click', () => console.error('egui: Simulasi event exception.'));

        // Check WebGPU & update UI
        if (typeof navigator !== 'undefined' && navigator.gpu) {
            navigator.gpu.requestAdapter({ powerPreference: 'high-performance' }).then(adapter => {
                if (adapter) {
                    const b = document.getElementById('backendBadge');
                    if (b) b.innerText = 'WebGPU (Hardware)';
                    const h = document.getElementById('fps-hud');
                    if (h) h.innerText = 'FPS: 60 | Touch: Active | WebGPU Experimental Ready';
                    console.info('egui: WebGPU Experimental Pipeline siap digunakan.');
                }
            }).catch(() => {});
        }

        console.log('egui WASM WebGL/WebGPU Runtime siap.');
    </script>
</body>
</html>
        """.trimIndent()
    }
}
