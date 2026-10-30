package com.example.data.repository

import com.example.data.db.WasmAppDao
import com.example.data.model.WasmAppEntity
import kotlinx.coroutines.flow.Flow

class WasmAppRepository(private val dao: WasmAppDao) {
    val allApps: Flow<List<WasmAppEntity>> = dao.getAllApps()

    suspend fun getAppById(id: String): WasmAppEntity? = dao.getAppById(id)

    suspend fun insertApp(app: WasmAppEntity) = dao.insertApp(app)

    suspend fun deleteApp(app: WasmAppEntity) = dao.deleteApp(app)

    suspend fun updateSettings(
        id: String,
        isImmersive: Boolean,
        scale: Float,
        isDesktopMode: Boolean,
        controlMode: String
    ) = dao.updateSettings(id, isImmersive, scale, isDesktopMode, controlMode)

    suspend fun updateIconSettings(id: String, iconPreset: String, customIconPath: String, iconBgColorHex: String) =
        dao.updateIconSettings(id, iconPreset, customIconPath, iconBgColorHex)
}
