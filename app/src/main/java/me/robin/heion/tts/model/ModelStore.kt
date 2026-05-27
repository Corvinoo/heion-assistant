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

package me.robin.heion.tts.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ModelStore(private val context: Context) {
    private val ttsDir = File(context.filesDir, "tts/kokoro")

    /**
     * Copies files from assets/tts/kokoro/ to internal storage recursively. //todo: clean external files
     */
    suspend fun ensureModelFiles(): List<String> = withContext(Dispatchers.IO) {
        Log.i("ModelStore", "Ensuring model files...")
        if (!ttsDir.exists()) {
            ttsDir.mkdirs()
        }
        val paths = mutableListOf<String>()
        copyAssetsRecursive("tts/kokoro", ttsDir, paths)
        Log.i("ModelStore", "Model files ready: ${paths.size} files")
        paths
    }

    private fun copyAssetsRecursive(assetPath: String, targetDir: File, paths: MutableList<String>) {
        val assets = context.assets.list(assetPath) ?: return
        if (assets.isNotEmpty()) {
            if (!targetDir.exists()) targetDir.mkdirs()
            for (asset in assets) {
                val fullAssetPath = "$assetPath/$asset"
                val subAssets = context.assets.list(fullAssetPath)
                if (subAssets.isNullOrEmpty()) {
                    val outFile = File(targetDir, asset)
                    if (!outFile.exists()) {
                        try {
                            Log.v("ModelStore", "Copying model asset: $fullAssetPath")
                            context.assets.open(fullAssetPath).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: IOException) {
                            Log.e("ModelStore", "Failed to copy model asset: $fullAssetPath", e)
                            continue
                        }
                    }
                    paths.add(outFile.absolutePath)
                } else {
                    copyAssetsRecursive(fullAssetPath, File(targetDir, asset), paths)
                }
            }
        }
    }

    fun getModelFile(fileName: String): File {
        return File(ttsDir, fileName)
    }

    /**
     * Searches for model_q4f16.onnx first, then any file ending in .onnx.
     * Checks both the internal TTS directory and the external models download directory.
     */
    fun findOnnxModelFile(): File? {
        val settings = me.robin.heion.settings.SettingsRepository(context)
        val savedPath = settings.getTtsModelPath()
        if (savedPath != null) {
            val savedFile = File(savedPath)
            if (savedFile.exists()) return savedFile
        }

        val primaryName = "model_q4f16.onnx"

        // Check internal TTS dir
        val primary = File(ttsDir, primaryName)
        if (primary.exists()) return primary

        // Check external models dir (downloaded models)
        val externalModelsDir = File(context.getExternalFilesDir(null), "models")
        if (externalModelsDir.exists()) {
            val externalPrimary = File(externalModelsDir, primaryName)
            if (externalPrimary.exists()) return externalPrimary

            val externalOnnx = externalModelsDir.listFiles { _, name -> name.endsWith(".onnx") }?.firstOrNull()
            if (externalOnnx != null) return externalOnnx
        }

        // Fallback to any ONNX in internal dir
        return ttsDir.listFiles { _, name -> name.endsWith(".onnx") }?.firstOrNull()
    }
}
