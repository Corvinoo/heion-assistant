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

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder


class KokoroVoiceRegistry(private val modelStore: ModelStore) {

    private val voiceMap = mapOf(
        "en" to "af_bella",
        "it" to "if_sara",
//        "ja" to "jf_nezumi" //todo: add phonemizer to support Japanese
    )

    fun getVoiceForLanguage(language: String): String {
        val lang = language.lowercase().take(2)
        return voiceMap[lang] ?: voiceMap["en"]!!
    }

    suspend fun getStyleVector(voiceName: String, tokenCount: Int): FloatArray =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val file = modelStore.getModelFile("voices/$voiceName.bin")
            Log.v("VoiceCheck", "Loading voice: $voiceName from ${file.absolutePath} (exists=${file.exists()}, size=${file.length()})")

            if (!file.exists()) {
                Log.e("VoiceCheck", "Voice file missing. Falling back to zero vector")
                return@withContext FloatArray(256)
            }

            try {
                val bytes = file.readBytes()
                require(bytes.size % 4 == 0) { "Voice file is not valid float32 data: ${bytes.size} bytes" }

                val floatCount = bytes.size / 4
                val floats = FloatArray(floatCount)

                ByteBuffer.wrap(bytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asFloatBuffer()
                    .get(floats)

                Log.v("VoiceCheck", "Loaded voice floats: $floatCount")

                return@withContext when {
                    floatCount == 256 -> {
                        Log.v("VoiceCheck", "Single 256-d style vector detected.")
                        floats
                    }

                    floatCount % 256 == 0 -> {
                        val styleCount = floatCount / 256
                        val index = tokenCount.coerceIn(0, styleCount - 1)
                        val start = index * 256
                        Log.v(
                            "VoiceCheck",
                            "Selecting style vector index=$index from $styleCount using tokenCount=$tokenCount"
                        )
                        floats.copyOfRange(start, start + 256)
                    }

                    else -> {
                        error("Unexpected voice file shape: $floatCount floats, not divisible by 256")
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceCheck", "Failed to load style vector", e)
                return@withContext FloatArray(256)
            }
        }
}