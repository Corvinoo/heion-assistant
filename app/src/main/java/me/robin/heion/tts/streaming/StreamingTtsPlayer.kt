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

package me.robin.heion.tts.streaming

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.robin.heion.tts.inference.KokoroEngine
import me.robin.heion.tts.inference.KokoroTokenizer
import me.robin.heion.tts.model.KokoroVoiceRegistry
import me.robin.heion.tts.phonemization.CompositePhonemizer
import me.robin.heion.tts.playback.AudioPlayer
import me.robin.heion.nlp.LanguageDetector

/**
 * Orchestrates the streaming TTS pipeline:
 * LLM tokens -> SpeechChunker -> TTS Queue -> Synthesis Worker -> Audio Queue -> Playback Worker
 */
class StreamingTtsPlayer(
    private val voiceRegistry: KokoroVoiceRegistry,
    private val phonemizer: CompositePhonemizer,
    private val engine: KokoroEngine,
    private val player: AudioPlayer,
    private val languageDetector: LanguageDetector,
    private val scope: CoroutineScope
) {
    private val tag = "StreamingTtsPlayer"

    private var ttsQueue = Channel<String>(2)
    private var audioQueue = Channel<Pair<FloatArray, Int>>(2)

    private var chunker: SpeechChunker? = null
    private var synthesisJob: Job? = null
    private var playbackJob: Job? = null
    private var isStarted = false
    private var language: String = "en"
    private var isLanguageDetected = false

    // Starts the streaming session, language is auto-detected from the first chunk
    suspend fun start(defaultLang: String = "en") {
        stopAndJoin()
        
        Log.v(tag, "Starting streaming TTS session")
        language = defaultLang
        isLanguageDetected = false
        isStarted = true

        ttsQueue = Channel(2)
        audioQueue = Channel(2)

        chunker = SpeechChunker(onChunkReady = { chunk ->
            scope.launch {
                if (!isLanguageDetected) {
                    detectAndSetLanguage(chunk)
                }
                Log.v(tag, "Enqueuing text chunk: \"$chunk\"")
                ttsQueue.send(chunk)
            }
        })

        startWorkers()
    }

    private suspend fun detectAndSetLanguage(chunk: String) {
        val detection = languageDetector.detect(chunk)
        val detected = detection?.languageTag?.take(2)?.lowercase() ?: "en"
        
        language = when (detected) { //todo: need to implement more solid way to expand this
            "it" -> "it"
            "ja", "jp" -> "ja"
            else -> "en"
        }
        isLanguageDetected = true
        Log.i(tag, "Auto-detected language for TTS: $language (from: \"$chunk\")")
    }

    private fun startWorkers() {
        // Synthesis worker
        synthesisJob = scope.launch(Dispatchers.Default) {
            try {
                for (text in ttsQueue) {
                    if (!isActive) break
                    Log.v(tag, "Synthesizing: \"$text\"")
                    val phonemeResult = phonemizer.phonemize(text, language)
                    val voiceName = voiceRegistry.getVoiceForLanguage(language)
                    
                    val tokens = KokoroTokenizer.tokenize(phonemeResult.phonemes)
                    val styleVector = voiceRegistry.getStyleVector(voiceName, tokens.size)

                    if (!isActive) break
                    val synthesisResult = engine.synthesize(phonemeResult.phonemes, styleVector)
                    
                    Log.v(tag, "Synthesis finished for \"$text\", enqueuing audio")
                    audioQueue.send(Pair(synthesisResult.audioData, synthesisResult.sampleRate))
                }
            } catch (e: CancellationException) {
                Log.v(tag, "Synthesis worker cancelled")
            } catch (e: Exception) {
                Log.e(tag, "Synthesis worker error", e)
            } finally {
                audioQueue.close()
            }
        }

        // Playback Worker
        playbackJob = scope.launch(Dispatchers.Default) {
            try {
                for ((audioData, sampleRate) in audioQueue) {
                    if (!isActive) break
                    Log.v(tag, "Starting playback of audio chunk")
                    player.play(audioData, sampleRate)
                }
            } catch (e: CancellationException) {
                Log.v(tag, "Playback worker cancelled")
            } catch (e: Exception) {
                Log.e(tag, "Playback worker error", e)
            }
        }
    }


    // Feeds a new token into the pipeline.
    fun onToken(token: String) {
        if (!isStarted) return
        chunker?.push(token)
    }

    // Finalizes the current stream by flushing the chunker
    suspend fun finish() {
        chunker?.flush()
        ttsQueue.close()
        // Wait for the workers to finish processing the queues
        synthesisJob?.join()
        playbackJob?.join()
    }


    fun stop() {
        isStarted = false
        chunker?.close()
        synthesisJob?.cancel()
        playbackJob?.cancel()
        player.stop()
    }

    // Stops everything and waits for workers to finish.
    private suspend fun stopAndJoin() {
        Log.v(tag, "Stopping and joining streaming TTS")
        stop()

        // Wait for the workers to finish so they release the engine mutex
        synthesisJob?.join()
        playbackJob?.join()

        synthesisJob = null
        playbackJob = null

        // Clear channels
        while (ttsQueue.tryReceive().isSuccess) {}
        while (audioQueue.tryReceive().isSuccess) {}
    }
}
