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

package me.robin.heion.nlp

import androidx.annotation.Keep
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.exp

class FastTextLanguageDetector(
    private val context: Context,
    private val assetPath: String = "models/lid.176.ftz",
    private val extractedFileName: String = "models/lid.176.ftz"
) : LanguageDetector {

    private val loadMutex = Mutex()

    @Volatile
    private var model: FastTextNative? = null

    private suspend fun ensureModel(): FastTextNative = loadMutex.withLock {
        model ?: withContext(Dispatchers.IO) {
            val modelFile = copyAssetToInternalFileIfNeeded(assetPath, extractedFileName)
            FastTextNative().apply {
                loadModel(modelFile)
            }.also {
                model = it
            }
        }
    }

    override suspend fun detect(text: String): LanguageGuess? = withContext(Dispatchers.Default) {
        val normalized = text.trim()
        if (normalized.isEmpty()) return@withContext null

        val prediction = ensureModel()
            .predictProba(normalized, 1)
            .firstOrNull() ?: return@withContext null

        val label = prediction.label
        val languageTag = label
            .removePrefix("__label__")
            .takeIf { it.isNotBlank() }

        LanguageGuess(
            fastTextLabel = label,
            languageTag = languageTag,
            confidence = exp(prediction.logProb)
        )
    }

    private fun copyAssetToInternalFileIfNeeded(assetPath: String, extractedFileName: String): File {
        val outFile = File(context.filesDir, extractedFileName)
        if (outFile.exists() && outFile.length() > 0L) return outFile

        outFile.parentFile?.mkdirs()

        context.assets.open(assetPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return outFile
    }

    fun release() {
        model?.close()
        model = null
    }
}

@Keep
data class FastTextPrediction(
    val label: String,
    val logProb: Double
)