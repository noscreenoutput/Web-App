package com.example.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.data.manager.SavefileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class AndroidBridge(
    private val context: Context,
    private val appId: String,
    private val onOpenFilePicker: (callbackId: String, mimeType: String) -> Unit,
    private val onSaveFile: (callbackId: String, fileName: String, mimeType: String, content: String) -> Unit,
    private val onNewLog: (ConsoleLogItem) -> Unit,
    private val onPerformanceMetrics: ((fps: Int, frameTimeMs: Float, jsHeapMb: Float, gpuBackend: String) -> Unit)? = null
) {
    private var webViewRef: WeakReference<WebView>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("wasm_app_storage_$appId", Context.MODE_PRIVATE)
    private val savefileManager = SavefileManager(context)
    private val bridgeScope = CoroutineScope(Dispatchers.IO)

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    @JavascriptInterface
    fun updatePerformanceMetrics(fps: Int, frameTimeStr: String, jsHeapStr: String, gpuBackendStr: String = "WebGL 2.0 (Accelerated)") {
        val frameTime = frameTimeStr.toFloatOrNull() ?: 16.6f
        val jsHeap = jsHeapStr.toFloatOrNull() ?: 0f
        mainHandler.post {
            onPerformanceMetrics?.invoke(fps, frameTime, jsHeap, gpuBackendStr)
        }
    }

    @JavascriptInterface
    fun openFilePicker(callbackId: String, mimeType: String) {
        mainHandler.post {
            onOpenFilePicker(callbackId, mimeType)
        }
    }

    @JavascriptInterface
    fun saveFile(callbackId: String, fileName: String, mimeType: String, base64Content: String) {
        mainHandler.post {
            onSaveFile(callbackId, fileName, mimeType, base64Content)
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        mainHandler.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("egui WASM", text)
            clipboard?.setPrimaryClip(clip)
            onNewLog(
                ConsoleLogItem(
                    level = LogLevel.INFO,
                    message = "[Bridge] Teks disalin ke clipboard (${text.length} karakter)"
                )
            )
        }
    }

    @JavascriptInterface
    fun getClipboardText(callbackId: String) {
        mainHandler.post {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            sendResult(callbackId, true, clipText)
        }
    }

    @JavascriptInterface
    fun writeAppStorage(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        onNewLog(
            ConsoleLogItem(
                level = LogLevel.LOG,
                message = "[Bridge Storage] Disimpan: '$key'"
            )
        )
    }

    @JavascriptInterface
    fun readAppStorage(key: String, callbackId: String) {
        val value = prefs.getString(key, null)
        mainHandler.post {
            if (value != null) {
                sendResult(callbackId, true, value)
            } else {
                sendResult(callbackId, false, "Kunci '$key' tidak ditemukan")
            }
        }
    }

    @JavascriptInterface
    fun writeSaveFile(fileName: String, content: String) {
        bridgeScope.launch {
            val success = savefileManager.writeSaveFile(appId, fileName, content)
            mainHandler.post {
                onNewLog(
                    ConsoleLogItem(
                        level = if (success) LogLevel.INFO else LogLevel.ERROR,
                        message = if (success) "[Savefile] Berhasil menyimpan '$fileName'" else "[Savefile] Gagal menyimpan '$fileName'"
                    )
                )
            }
        }
    }

    @JavascriptInterface
    fun readSaveFile(fileName: String, callbackId: String) {
        bridgeScope.launch {
            val content = savefileManager.readSaveFile(appId, fileName)
            mainHandler.post {
                if (content != null) {
                    sendResult(callbackId, true, content)
                } else {
                    sendResult(callbackId, false, "Berkas savefile '$fileName' tidak ditemukan")
                }
            }
        }
    }

    @JavascriptInterface
    fun sendConsoleLog(level: String, message: String) {
        val logLvl = when (level.uppercase()) {
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARN
            "INFO" -> LogLevel.INFO
            else -> LogLevel.LOG
        }
        mainHandler.post {
            onNewLog(ConsoleLogItem(level = logLvl, message = message))
        }
    }

    private fun sendResult(callbackId: String, success: Boolean, data: String) {
        val escaped = data.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "")
        val js = "if(window.onNativeBridgeResult){ window.onNativeBridgeResult('$callbackId', $success, '$escaped'); }"
        mainHandler.post {
            webViewRef?.get()?.evaluateJavascript(js, null)
        }
    }
}
