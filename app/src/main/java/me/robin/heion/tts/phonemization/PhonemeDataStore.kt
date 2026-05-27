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

package me.robin.heion.tts.phonemization

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PhonemeDataStore(private val context: Context) {
    private val dataDir = File(context.filesDir, "tts/phonemization")
    val espeakDataDir = File(dataDir, "espeak-ng-data")
    val openJTalkDictDir = File(dataDir, "openjtalk/dic")
    val openJTalkVoiceFile = File(dataDir, "openjtalk/voice/mei_normal.htsvoice")

    suspend fun ensureDataFiles() = withContext(Dispatchers.IO) {
        Log.i("PhonemeDataStore", "Ensuring phoneme data files...")
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        
        // Copy espeak-ng-data
        copyAssetDir("tts/phonemization/espeak-ng-data", espeakDataDir)

        // Copy openjtalk dictionary
        copyAssetDir("tts/phonemization/openjtalk/dic", openJTalkDictDir)

        // Copy openjtalk voice
        copyAssetFile("tts/phonemization/openjtalk/voice/mei_normal.htsvoice", openJTalkVoiceFile)
        Log.i("PhonemeDataStore", "Phoneme data files ready")
    }

    private fun copyAssetDir(assetPath: String, targetDir: File) {
        val assets = context.assets.list(assetPath) ?: return
        if (!targetDir.exists()) targetDir.mkdirs()
        
        for (asset in assets) {
            val fullAssetPath = "$assetPath/$asset"
            val targetFile = File(targetDir, asset)
            
            // Recursive copy for subdirectories
            val subAssets = context.assets.list(fullAssetPath)
            if (!subAssets.isNullOrEmpty()) {
                copyAssetDir(fullAssetPath, targetFile)
            } else {
                copyAssetFile(fullAssetPath, targetFile)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, targetFile: File) {
        if (targetFile.exists()) return
        
        targetFile.parentFile?.mkdirs()
        try {
            // Check if asset exists before attempting to open it to avoid noisy stack traces
            val parentPath = assetPath.substringBeforeLast("/", "")
            val fileName = assetPath.substringAfterLast("/")
            val assets = context.assets.list(parentPath)
            if (assets == null || fileName !in assets) {
                Log.w("PhonemeDataStore", "Asset not found, skipping: $assetPath")
                return
            }

            Log.v("PhonemeDataStore", "Copying asset: $assetPath to ${targetFile.absolutePath}")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.e("PhonemeDataStore", "Failed to copy asset: $assetPath", e)
        }
    }
}
