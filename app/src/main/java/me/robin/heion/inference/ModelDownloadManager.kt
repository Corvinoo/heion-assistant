/*
 *  Copyright (C) 2026 Corvinoo
 *  This file is part of Heion Cloudless Assistant
 *
 * Heion Cloudless Assistant is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Heion Cloudless Assistant is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Heion Cloudless Assistant. If not, see <https://www.gnu.org/licenses/>. 
 *
 * This program is subject to additional terms, experimental software disclaimers,
 * and trademark limitations pursuant to Section 7 of the GNU GPLv3. 
 * See the README and first-launch notice for details.
 */

package me.robin.heion.inference

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class ModelType {
    LITERT_LLM,
    ONNX_TTS
}

/**
 * Metadata for a downloadable model.
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val fileName: String,
    val url: String,
    val type: ModelType,
    var sizeBytes: Long = 0L
)

/**
 * Orchestrates background model downloads using WorkManager.
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownloadManager"
    private const val CONFIG_PATH = "config/models.json"

    private var cachedModels: List<ModelInfo>? = null

    /**
     * Loads the list of downloadable models from the external assets JSON file.
     */
    fun getDownloadableModels(context: Context): List<ModelInfo> {
        cachedModels?.let { return it }
        
        return try {
            val jsonString = context.assets.open(CONFIG_PATH).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ModelInfo>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ModelInfo(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        fileName = obj.getString("fileName"),
                        url = obj.getString("url"),
                        type = ModelType.valueOf(obj.getString("type")),
                        sizeBytes = obj.optLong("sizeBytes", 0L)
                    )
                )
            }
            cachedModels = list
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load models config", e)
            emptyList()
        }
    }

    /**
     * Dynamically fetches the file size from the server using a HEAD request.
     * This avoids hardcoding sizes in the JSON.
     */
    suspend fun fetchModelSize(model: ModelInfo): Long = withContext(Dispatchers.IO) {
        if (model.sizeBytes > 0) return@withContext model.sizeBytes

        val size = try {
            fetchSizeViaHead(model.url) ?: fetchSizeViaRange(model.url) ?: 0L
        } catch (e: Exception) {
            0L // Handle network failures gracefully
        }
        if (size > 0) {
            synchronized(model) { model.sizeBytes = size }
        }
        size
    }

    private fun fetchSizeViaHead(url: String): Long? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode !in 200..299) return null
            return connection.contentLengthLong.takeIf { it > 0 }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchSizeViaRange(url: String): Long? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Range", "bytes=0-0")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.connect()

            // 206 = server honored the range request
            // 200 = server ignored Range header and is sending full file — abort
            if (connection.responseCode != 206) {
                Log.w(TAG, "Range request not supported for $url (${connection.responseCode})")
                return null
            }

            // Content-Range: bytes 0-0/2147483648
            val contentRange = connection.getHeaderField("Content-Range") ?: return null
            return contentRange.substringAfterLast('/').toLongOrNull()?.takeIf { it > 0 }
        } finally {
            connection.disconnect()
        }
    }

    fun startDownload(context: Context, model: ModelInfo) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    "url" to model.url,
                    "fileName" to model.fileName
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            model.id,
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }

    fun getDownloadWorkInfo(context: Context, modelId: String): Flow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(modelId)
            .map { it.firstOrNull() }
    }

    fun isModelReady(context: Context, model: ModelInfo): Boolean {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        val modelFile = File(modelsDir, model.fileName)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun deleteModel(context: Context, model: ModelInfo): Boolean {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        val modelFile = File(modelsDir, model.fileName)
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "Unknown size"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var s = size.toDouble()
        var unitIndex = 0
        while (s >= 1024 && unitIndex < units.size - 1) {
            s /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", s, units[unitIndex])
    }
}
