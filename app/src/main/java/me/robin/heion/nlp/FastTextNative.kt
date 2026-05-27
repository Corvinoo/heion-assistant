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
import java.io.File

@Keep
class FastTextNative : AutoCloseable {
    companion object {
        init {
            System.loadLibrary("phonemizer_jni") //todo: change name compiled library to be more idiomatic
        }
    }

    private var handle: Long = nativeCreate()

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeLoadModel(handle: Long, path: String)
    private external fun nativePredict(handle: Long, text: String, k: Int): Array<String>

    fun loadModel(modelFile: File) {
        nativeLoadModel(handle, modelFile.absolutePath)
    }

    fun predictProba(text: String, k: Int = 1): List<FastTextPrediction> {
        val raw = nativePredict(handle, text, k)
        return raw.mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 2) return@mapNotNull null
            val label = parts[0]
            val logProb = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            FastTextPrediction(label, logProb)
        }
    }

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }
}

