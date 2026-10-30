package com.example.data.manager

import android.content.Context
import com.example.data.model.SavefileItem
import java.io.File

class SavefileManager(private val context: Context) {

    fun getAppSaveDirectory(appId: String): File {
        val dir = File(context.filesDir, "saves/$appId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listSaveFiles(appId: String): List<SavefileItem> {
        val dir = getAppSaveDirectory(appId)
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { it.isFile }.map {
            SavefileItem(
                name = it.name,
                sizeBytes = it.length(),
                lastModified = it.lastModified(),
                path = it.absolutePath
            )
        }.sortedByDescending { it.lastModified }
    }

    fun readSaveFile(appId: String, fileName: String): String? {
        val file = File(getAppSaveDirectory(appId), fileName)
        return if (file.exists()) file.readText() else null
    }

    fun writeSaveFile(appId: String, fileName: String, content: String): Boolean {
        return try {
            val file = File(getAppSaveDirectory(appId), fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteSaveFile(appId: String, fileName: String): Boolean {
        val file = File(getAppSaveDirectory(appId), fileName)
        return if (file.exists()) file.delete() else false
    }

    fun clearAllSaves(appId: String): Boolean {
        val dir = getAppSaveDirectory(appId)
        return dir.deleteRecursively() && dir.mkdirs()
    }
}
