package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wasm_apps")
data class WasmAppEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val directoryName: String,
    val entryHtml: String = "index.html",
    val hasWasm: Boolean = false,
    val wasmFileName: String = "",
    val fileCount: Int = 1,
    val totalSizeBytes: Long = 0L,
    val importedAt: Long = System.currentTimeMillis(),
    val isImmersive: Boolean = true,
    val preferredScale: Float = 1.0f,
    val colorHex: String = "#E07A5F",
    val iconPreset: String = "code",
    val customIconPath: String = "",
    val iconBgColorHex: String = "#E07A5F",
    val isDesktopMode: Boolean = false,
    val controlMode: String = "touch"
)
