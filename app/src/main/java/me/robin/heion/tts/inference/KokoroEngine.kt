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

package me.robin.heion.tts.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.robin.heion.tts.model.ModelStore
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Result of a TTS synthesis operation
 */
data class KokoroResult(
    val audioData: FloatArray,
    val sampleRate: Int = 24000,
    val channelCount: Int = 1,
    val durationMs: Long,
    val inferenceTimeMs: Long,
    val diagnostics: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KokoroResult
        if (!audioData.contentEquals(other.audioData)) return false
        return true
    }

    override fun hashCode(): Int {
        return audioData.contentHashCode()
    }
}

/**
 * Engine for Kokoro TTS synthesis using ONNX Runtime
 */
class KokoroEngine(
    private val modelStore: ModelStore,
    private val useNnapi: Boolean = false
) {
    private val tag = "KokoroEngine"
    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    private val mutex = Mutex()

    suspend fun load() = mutex.withLock {
        modelStore.ensureModelFiles()
        ensureSessionLocked()
    }

    /**
     * Synthesizes phonemes into raw audio data
     *
     * @param phonemes The IPA phoneme string.
     * @param voiceStyle A 256-dimensional style vector.
     * @param speed The synthesis speed (default 1.0).
     * @return A [KokoroResult] containing the audio data and metadata.
     */
    suspend fun synthesize(
        phonemes: String,
        voiceStyle: FloatArray,
        speed: Float = 1.0f
    ): KokoroResult = withContext(Dispatchers.Default) {
        mutex.withLock {
            val currentSession = ensureSessionLocked()

            log("Synthesizing phonemes: \"$phonemes\"")

            // Tokenization
            val tokens = KokoroTokenizer.tokenize(phonemes)
            if (tokens.isEmpty()) {
                throw KokoroEngineException("Tokenizer returned no tokens for phonemes: \"$phonemes\"")
            }
            
            log("Tokens (first 50): ${tokens.take(50).joinToString(", ")}")
            log("Token count: ${tokens.size}")

            // Prepare inputs
            // Note: Verify input names against ONNX metadata if synthesis fails.
            val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), longArrayOf(1, tokens.size.toLong()))
            val styleTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(voiceStyle), longArrayOf(1, voiceStyle.size.toLong()))
            val speedTensor = OnnxTensor.createTensor(env, floatArrayOf(speed))
            
            log("Style vector shape: [1, ${voiceStyle.size}]")
            log("Speed: $speed (float)")

            val inputs = mutableMapOf<String, OnnxTensor>()
            // Try to match input names from session metadata
            val inputNames = currentSession.inputNames
            if (inputNames.contains("tokens")) {
                inputs["tokens"] = inputIdsTensor
            } else if (inputNames.contains("input_ids")) {
                inputs["input_ids"] = inputIdsTensor
            } else {
                log("Warning: Could not find 'tokens' or 'input_ids' in model inputs. Available: ${inputNames.joinToString()}")
                inputs[inputNames.first()] = inputIdsTensor
            }
            
            inputs["style"] = styleTensor
            inputs["speed"] = speedTensor

            try {
                val inferenceStart = System.currentTimeMillis()
                currentSession.run(inputs).use { results ->
                    val inferenceEnd = System.currentTimeMillis()

                    if (results.size() == 0) {
                        throw KokoroEngineException("Inference returned no results")
                    }

                    // Extract output audio
                    val outputValue = results[0].value
                    val audioOutput = when (outputValue) {
                        is FloatArray -> outputValue
                        is Array<*> -> {
                            if (outputValue.isNotEmpty() && outputValue[0] is FloatArray) {
                                outputValue[0] as FloatArray
                            } else if (outputValue.isNotEmpty() && outputValue[0] is Array<*> && (outputValue[0] as Array<*>)[0] is FloatArray) {
                                // Handle [1, 1, N] or similar
                                ((outputValue[0] as Array<*>)[0] as FloatArray)
                            } else {
                                throw KokoroEngineException("Unexpected output shape: ${outputValue.javaClass.simpleName}")
                            }
                        }
                        else -> throw KokoroEngineException("Unexpected output type: ${outputValue?.javaClass?.simpleName}")
                    }

                    val durationMs = (audioOutput.size.toFloat() / 24000f * 1000f).toLong()
                    val inferenceTimeMs = inferenceEnd - inferenceStart

                    log("Synthesis complete. Audio size: ${audioOutput.size}, Inference time: ${inferenceTimeMs}ms")
                    if (audioOutput.isNotEmpty()) {
                        val first20 = audioOutput.take(20).joinToString(", ")
                        val last20 = audioOutput.takeLast(20).joinToString(", ")
                        val min = audioOutput.minOrNull()
                        val max = audioOutput.maxOrNull()
                        
                        // Find first and last non-zero indices
                        var firstNonZero = -1
                        var lastNonZero = -1
                        for (i in audioOutput.indices) {
                            if (Math.abs(audioOutput[i]) > 0.00001f) {
                                if (firstNonZero == -1) firstNonZero = i
                                lastNonZero = i
                            }
                        }
                        
                        log("Audio samples (first 20): $first20")
                        log("Audio samples (last 20): $last20")
                        log("Audio range: [$min, $max]")
                        log("Audio non-zero bounds: first=$firstNonZero, last=$lastNonZero (out of ${audioOutput.size})")
                    }

                    KokoroResult(
                        audioData = audioOutput,
                        durationMs = durationMs,
                        inferenceTimeMs = inferenceTimeMs,
                        diagnostics = mapOf(
                            "token_count" to tokens.size,
                            "output_samples" to audioOutput.size
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                logError("Inference failed", e)
                throw KokoroEngineException("Failed to run ONNX inference: ${e.message}", e)
            } finally {
                inputIdsTensor.close()
                styleTensor.close()
                speedTensor.close()
            }
        }
    }

    suspend fun saveToWav(result: KokoroResult, outputFile: File) = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(outputFile).use { out ->
                val pcmData = result.audioData
                val sampleRate = result.sampleRate
                val bitsPerSample = 16
                val channels = result.channelCount
                val byteRate = sampleRate * channels * bitsPerSample / 8
                val blockAlign = channels * bitsPerSample / 8
                val dataSize = pcmData.size * 2 

                out.write("RIFF".toByteArray())
                out.write(intToByteArray(36 + dataSize))
                out.write("WAVE".toByteArray())

                out.write("fmt ".toByteArray())
                out.write(intToByteArray(16)) 
                out.write(shortToByteArray(1)) 
                out.write(shortToByteArray(channels.toShort()))
                out.write(intToByteArray(sampleRate))
                out.write(intToByteArray(byteRate))
                out.write(shortToByteArray(blockAlign.toShort()))
                out.write(shortToByteArray(bitsPerSample.toShort()))

                out.write("data".toByteArray())
                out.write(intToByteArray(dataSize))

                val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
                for (sample in pcmData) {
                    val s = (sample.coerceIn(-1f, 1f) * 32767).toInt().toShort()
                    buffer.putShort(s)
                }
                out.write(buffer.array())
            }
        } catch (e: Exception) {
            logError("Failed to save WAV", e)
            throw KokoroEngineException("Failed to save WAV file: ${e.message}", e)
        }
    }

    private suspend fun ensureSessionLocked(): OrtSession {
        session?.let { return it }

        log("Ensuring model files are present...")
        modelStore.ensureModelFiles()

        log("Initializing ONNX session")
        val modelFile = modelStore.findOnnxModelFile()
        if (modelFile == null || !modelFile.exists()) {
            throw KokoroEngineException("No ONNX model file found in TTS directory")
        }

        return try {
            val sessionOptions = OrtSession.SessionOptions()
            if (useNnapi) {
                sessionOptions.addNnapi()
            }
            
            val newSession = env.createSession(modelFile.absolutePath, sessionOptions)
            
            // Log Input/Output metadata
            log("ONNX Session Metadata:")
            newSession.inputInfo.forEach { (name, info) ->
                val typeInfo = info.info
                val details = if (typeInfo is ai.onnxruntime.TensorInfo) {
                    "type=${typeInfo.type}, shape=${typeInfo.shape.contentToString()}"
                } else {
                    typeInfo.toString()
                }
                log("  Input \"$name\": $details")
            }
            newSession.outputInfo.forEach { (name, info) ->
                val typeInfo = info.info
                val details = if (typeInfo is ai.onnxruntime.TensorInfo) {
                    "type=${typeInfo.type}, shape=${typeInfo.shape.contentToString()}"
                } else {
                    typeInfo.toString()
                }
                log("  Output \"$name\": $details")
            }

            session = newSession
            newSession
        } catch (e: Exception) {
            logError("Failed to create ONNX session", e)
            throw KokoroEngineException("Failed to initialize Kokoro engine: ${e.message}", e)
        }
    }

    suspend fun release() = mutex.withLock {
        log("Releasing Kokoro engine resources")
        session?.close()
        session = null
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    private fun log(message: String) = Log.v(tag, message)
    private fun logError(message: String, e: Throwable) = Log.e(tag, message, e)
}

class KokoroEngineException(message: String, cause: Throwable? = null) : Exception(message, cause)
