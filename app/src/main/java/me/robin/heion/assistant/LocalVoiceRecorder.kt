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

package me.robin.heion.assistant

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class LocalVoiceRecorder(private val tempFile: File) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onSilenceDetected: (() -> Unit)? = null
    var onAmplitudeUpdate: ((Float) -> Unit)? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val silenceThreshold = 500f
    private val silenceDurationMs = 1800L
    private var lastSpokenTime = System.currentTimeMillis()

    //Todo: find stronger way to define speechStarted then boolean flags
    private var speechStarted = false
    private var silenceTriggered = false

    var recordedDurationMs: Long = 0L
        private set

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording) return
        isRecording = true
        recordedDurationMs = 0L
        speechStarted = false
        silenceTriggered = false

        tempFile.parentFile?.mkdirs()
        tempFile.delete()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            android.util.Log.e("LocalAssistant", "AudioRecord initialization failed")
            isRecording = false
            return
        }

        try {
            audioRecord?.startRecording()
        } catch (e: IllegalStateException) {
            android.util.Log.e("LocalAssistant", "startRecording failed", e)
            isRecording = false
            return
        }

        lastSpokenTime = System.currentTimeMillis()

        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44)) // reserve WAV header

                var totalPayloadSize = 0

                try {
                    while (isActive && isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                            var sum = 0.0

                            for (i in 0 until read) {
                                byteBuffer.putShort(buffer[i])
                                sum += buffer[i].toDouble() * buffer[i].toDouble()
                            }

                            raf.write(byteBuffer.array())
                            totalPayloadSize += read * 2
                            recordedDurationMs = (totalPayloadSize.toLong() * 1000L) / (sampleRate * 2L)

                            val rms = sqrt(sum / read).toFloat()
                            onAmplitudeUpdate?.invoke(rms)

                            if (rms > silenceThreshold) {

                                // First time voice is detected
                                if (!speechStarted) {
                                    speechStarted = true
                                }

                                lastSpokenTime = System.currentTimeMillis()
                                silenceTriggered = false

                            } else {

                                // Only start counting silence AFTER first speech
                                if (speechStarted &&
                                    !silenceTriggered &&
                                    System.currentTimeMillis() - lastSpokenTime > silenceDurationMs
                                ) {
                                    silenceTriggered = true

                                    withContext(Dispatchers.Main) {
                                        onSilenceDetected?.invoke()
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    raf.seek(0)
                    writeWavHeader(raf, totalPayloadSize)
                    raf.fd.sync()
                }
            }
        }
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun writeWavHeader(raf: RandomAccessFile, payloadSize: Int) {
        val totalSize = 36 + payloadSize
        val byteBuffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        byteBuffer.put("RIFF".toByteArray())
        byteBuffer.putInt(totalSize)
        byteBuffer.put("WAVE".toByteArray())

        byteBuffer.put("fmt ".toByteArray())
        byteBuffer.putInt(16)
        byteBuffer.putShort(1)
        byteBuffer.putShort(1)
        byteBuffer.putInt(sampleRate)
        byteBuffer.putInt(sampleRate * 2)
        byteBuffer.putShort(2)
        byteBuffer.putShort(16)

        byteBuffer.put("data".toByteArray())
        byteBuffer.putInt(payloadSize)

        raf.write(byteBuffer.array())
    }
}