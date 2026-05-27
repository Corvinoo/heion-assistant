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
import java.io.File

object ModelDiscovery {
    private const val EXTENSION = ".litertlm"

    fun getAvailableModelName(context: Context): String? {
        // Check debug folder
        try {
            val debugModel = obtainDebugModel(context)
            if (debugModel != null) return debugModel.name
        } catch (_: Exception) {
        }

        // Check downloaded models folder
        try {
            val downloadedModel = obtainDownloadedModel(context)
            if (downloadedModel != null) return downloadedModel.name
        } catch (_: Exception) {
        }

        // Check internal files dir
        val internalFiles = context.filesDir.listFiles()?.map { it.name } ?: emptyList()
        val internalModel = findPreferredModel(internalFiles)
        if (internalModel != null) return internalModel

        // Check assets
        return obtainAssetModel(context)
    }

    fun obtainDebugModel(context: Context): File? {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return null
        val files = externalFilesDir.listFiles() ?: return null
        val modelName = findPreferredModel(files.map { it.name }) ?: return null
        return File(externalFilesDir, modelName)
    }

    // Looks for models in the dedicated "models" subdirectory of external storage
    fun obtainDownloadedModel(context: Context): File? {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) return null
        val files = modelsDir.listFiles() ?: return null
        val modelName = findPreferredModel(files.map { it.name }) ?: return null
        return File(modelsDir, modelName)
    }

    fun obtainAssetModel(context: Context): String? {
        return try {
            val assets = context.assets.list("models")?.toList() ?: emptyList()
            findPreferredModel(assets)
        } catch (_: Exception) {
            null
        }
    }

    private fun findPreferredModel(fileNames: List<String>): String? {
        return fileNames.find { it.contains("E4B", ignoreCase = true) && it.endsWith(EXTENSION) }
            ?: fileNames.find { it.contains("E2B", ignoreCase = true) && it.endsWith(EXTENSION) }
            ?: fileNames.find { it.endsWith(EXTENSION) }
    }
}
