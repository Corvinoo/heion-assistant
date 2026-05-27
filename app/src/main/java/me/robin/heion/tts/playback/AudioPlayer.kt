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

package me.robin.heion.tts.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class AudioPlayer(private val context: Context) {
    private val tag = "AudioPlayer"
    private var audioTrack: AudioTrack? = null
    private var currentSampleRate: Int = -1
    private var totalFramesWritten: Int = 0

    /**
     * Plays raw PCM audio data (Float)
     *
     * @param pcmData Raw float samples (typically in [-1.0, 1.0])
     * @param sampleRate The sample rate of the input data
     */
    suspend fun play(pcmData: FloatArray, sampleRate: Int = 24000) = withContext(Dispatchers.Default) {
        try {
            // Amplitude Normalization
            val normalizedData = normalize(pcmData)

            // Sample Rate Handling & Resampling
            val nativeRate = getNativeSampleRate()
            val (finalData, finalRate) = if (shouldResample(sampleRate, nativeRate)) {
                Log.v(tag, "Resampling from $sampleRate to $nativeRate")
                resample(normalizedData, sampleRate, nativeRate) to nativeRate
            } else {
                normalizedData to sampleRate
            }

            // AudioTrack Configuration and Reuse
            var track = audioTrack
            if (track == null || currentSampleRate != finalRate) {
                Log.v(tag, "Recreating AudioTrack: oldRate=$currentSampleRate, newRate=$finalRate")
                stop()
                
                val bufferSize = AudioTrack.getMinBufferSize(
                    finalRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT
                )

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(finalRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize.coerceAtLeast(64000))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                currentSampleRate = finalRate
                totalFramesWritten = 0
                track.play()
            }

            // Streaming Data
            Log.v(tag, "Streaming ${finalData.size} samples to AudioTrack (Total written: $totalFramesWritten)")
            
            var offset = 0
            while (offset < finalData.size && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val remaining = finalData.size - offset
                val written = track.write(finalData, offset, remaining, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
                totalFramesWritten += written
            }

            // Drain the buffer (wait for these specific frames to finish)
            waitForPlaybackToFinish(track, totalFramesWritten)

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.v(tag, "Playback cancelled")
                stop()
                throw e
            }
            Log.e(tag, "Playback failed", e)
            stop()
        }
    }

    private fun getNativeSampleRate(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val rateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        return rateStr?.toIntOrNull() ?: 44100
    }

    // Normalizes the audio data to the range [-1.0, 1.0]

    internal fun normalize(data: FloatArray): FloatArray {
        if (data.isEmpty()) return data

        var peak = 0f
        var hasInvalidSample = false

        for (sample in data) {
            if (!sample.isFinite()) {
                hasInvalidSample = true
                continue
            }
            peak = max(peak, abs(sample))
        }

        // If the buffer is already in range and contains no invalid values, returns it unchanged to avoid unnecessary allocation or distortion
        if (!hasInvalidSample && peak <= 1.0f) {
            return data
        }

        // If the buffer is effectively silent, do not try to normalize it
        if (peak < 1e-6f) {
            return FloatArray(data.size) { 0f }
        }

        val gain = if (peak > 1.0f) 1.0f / peak else 1.0f
        val out = FloatArray(data.size)

        for (i in data.indices) {
            val sample = data[i]
            val safe = if (sample.isFinite()) sample else 0f
            out[i] = (safe * gain).coerceIn(-1f, 1f)
        }

        Log.w(tag, "Normalizing audio: peak amplitude $peak, gain=$gain")
        return out
    }

    // Simple linear interpolation resampler
    internal fun resample(data: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return data
        
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val newSize = (data.size * (toRate.toDouble() / fromRate.toDouble())).toInt()
        val result = FloatArray(newSize)

        for (i in 0 until newSize) {
            val position = i * ratio
            val index = position.toInt()
            val fraction = (position - index).toFloat()

            if (index + 1 < data.size) {
                result[i] = data[index] * (1f - fraction) + data[index + 1] * fraction
            } else if (index < data.size) {
                result[i] = data[index]
            }
        }
        return result
    }

    private fun shouldResample(inputRate: Int, nativeRate: Int): Boolean {
        // Resample if the input rate is significantly lower than native (e.g. 24k -> 48k) or if 24k is not natively supported
        return inputRate < nativeRate && nativeRate % inputRate == 0
    }

    // Blocks until the AudioTrack has finished playing all written samples
    private suspend fun waitForPlaybackToFinish(track: AudioTrack, totalSamples: Int) {
        val totalFrames = totalSamples // MONO
        try {
            while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val head = track.playbackHeadPosition
                // Log progress for debugging
                if (totalFrames > 0 && head % 10000 == 0) {
                    Log.v(tag, "Playback progress: $head / $totalFrames")
                }
                if (head >= totalFrames) break
                delay(10)
            }
            Log.v(tag, "Playback loop finished. Head: ${track.playbackHeadPosition}")
        } catch (e: Exception) {
            Log.e(tag, "Error waiting for playback finish", e)
        }
    }

    fun stop() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
            totalFramesWritten = 0
            currentSampleRate = -1
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop AudioTrack", e)
        }
    }
}
