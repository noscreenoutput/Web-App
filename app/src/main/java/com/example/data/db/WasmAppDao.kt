package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WasmAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WasmAppDao {
    @Query("SELECT * FROM wasm_apps ORDER BY importedAt DESC")
    fun getAllApps(): Flow<List<WasmAppEntity>>

    @Query("SELECT * FROM wasm_apps WHERE id = :id LIMIT 1")
    suspend fun getAppById(id: String): WasmAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: WasmAppEntity)

    @Update
    suspend fun updateApp(app: WasmAppEntity)

    @Delete
    suspend fun deleteApp(app: WasmAppEntity)

    @Query("UPDATE wasm_apps SET isImmersive = :isImmersive, preferredScale = :scale, isDesktopMode = :isDesktopMode, controlMode = :controlMode WHERE id = :id")
    suspend fun updateSettings(
        id: String,
        isImmersive: Boolean,
        scale: Float,
        isDesktopMode: Boolean,
        controlMode: String
    )

    @Query("UPDATE wasm_apps SET iconPreset = :iconPreset, customIconPath = :customIconPath, iconBgColorHex = :iconBgColorHex WHERE id = :id")
    suspend fun updateIconSettings(id: String, iconPreset: String, customIconPath: String, iconBgColorHex: String)
}
