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
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supports progress reporting and chunked streaming to prevent OOM.
 */
class ModelDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val urlString = inputData.getString("url") ?: "https://huggingface.co"
        val fileName = inputData.getString("fileName") ?: "gemma-4-E2B-it.litertlm"

        // Ensure models directory exists in external files
        val modelsDir = File(applicationContext.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val outputFile = File(modelsDir, fileName)
        val tempFile = File(modelsDir, "$fileName.tmp")

        return try {
            Log.d(TAG, "Starting download of $fileName from $urlString")
            withContext(Dispatchers.IO) {
                downloadFile(urlString, tempFile)
            }
            
            // Atomically rename temp file to final file
            if (tempFile.renameTo(outputFile)) {
                Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
                Result.success()
            } else {
                Log.e(TAG, "Failed to rename temp file to $fileName")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $fileName", e)
            Result.retry()
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private suspend fun downloadFile(urlString: String, outputFile: File) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()

        if (connection.responseCode !in 200..299) {
            throw Exception("Server returned HTTP ${connection.responseCode}: ${connection.responseMessage}")
        }

        val fileLength = connection.contentLengthLong
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8 * 1024) // 8KB buffer
                var bytesRead: Int
                var totalBytesRead: Long = 0
                var lastProgressUpdate = -1

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (fileLength > 0) {
                        val progress = (totalBytesRead * 100 / fileLength).toInt()
                        if (progress != lastProgressUpdate) {
                            setProgress(workDataOf("progress" to progress))
                            lastProgressUpdate = progress
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ModelDownloadWorker"
    }
}
