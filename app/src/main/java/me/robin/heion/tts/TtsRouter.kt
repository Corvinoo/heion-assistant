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

package me.robin.heion.tts

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.robin.heion.tts.inference.KokoroEngine
import me.robin.heion.tts.inference.KokoroTokenizer
import me.robin.heion.tts.model.KokoroVoiceRegistry
import me.robin.heion.tts.phonemization.CompositePhonemizer
import me.robin.heion.tts.playback.AudioPlayer

/**
 * Main router for the TTS pipeline.
 */
class TtsRouter(
    private val voiceRegistry: KokoroVoiceRegistry,
    private val phonemizer: CompositePhonemizer,
    private val engine: KokoroEngine,
    private val player: AudioPlayer
) {
    private val tag = "TtsRouter"
    private val mutex = Mutex()
    private var activeJob: Job? = null

    suspend fun say(text: String, detectedLanguage: String?) {
        // Cancel previous work and stop any ongoing audio immediately
        val previousJob = activeJob
        val currentJob = currentCoroutineContext()[Job]
        activeJob = currentJob
        
        if (previousJob != null && previousJob != currentJob && previousJob.isActive) {
            Log.v(tag, "Interrupting previous synthesis")
            previousJob.cancel()
            player.stop()
            // We must wait for the previous job to release the mutex
            previousJob.join()
        }

        mutex.withLock {
            try {
                if (!currentCoroutineContext().isActive) return@withLock

                val lang = mapLanguage(detectedLanguage)
                Log.v(tag, "Synthesizing: \"$text\" in $lang")

                val voiceName = voiceRegistry.getVoiceForLanguage(lang)

                //  Phonemization
                val phonemeResult = phonemizer.phonemize(text, lang)
                Log.v(tag, "Phonemes: \"${phonemeResult.phonemes}\"")

                if (!currentCoroutineContext().isActive) return@withLock

                val tokens = KokoroTokenizer.tokenize(phonemeResult.phonemes)
                val styleVector = voiceRegistry.getStyleVector(voiceName, tokens.size)

                //  Inference
                Log.v(tag, "Starting inference for voice: $voiceName")
                val synthesisResult = engine.synthesize(
                    phonemes = phonemeResult.phonemes,
                    voiceStyle = styleVector
                )

                //  Playback
                if (currentCoroutineContext().isActive) {
                    Log.v(tag, "Starting playback: ${synthesisResult.audioData.size} samples")
                    player.play(synthesisResult.audioData, synthesisResult.sampleRate)
                    Log.v(tag, "Playback finished")
                } else {
                    Log.v(tag, "Synthesis finished but job was cancelled")
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(tag, "TTS failed", e)
            } finally {
                if (activeJob == currentJob) {
                    activeJob = null
                }
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        player.stop()
        activeJob = null
    }

    private fun mapLanguage(detected: String?): String {
        val code = detected?.lowercase()?.take(2) ?: "en"
        return when (code) {
            "en" -> "en"
            "it" -> "it"
            "ja", "jp" -> "ja"
            else -> "en"
        }
    }
}
