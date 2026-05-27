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

package me.robin.heion.assistant.voicesession

import me.robin.heion.assistant.LocalVoiceRecorder
import android.content.Context
import java.io.File

class VoiceRecorderController(private val context: Context) {

    private val audioDir = File(context.cacheDir, "audio").apply { mkdirs() }
    private val tempAudioFile = File(audioDir, "current_query.wav")
    
    private val voiceRecorder = LocalVoiceRecorder(tempAudioFile)

    var onSilenceDetected: (() -> Unit)? = null
        set(value) {
            field = value
            voiceRecorder.onSilenceDetected = value
        }

    fun start() {
        voiceRecorder.start()
    }

    fun stop() {
        voiceRecorder.stop()
    }

    fun getAudioFile(): File? {
        if (!tempAudioFile.exists() || tempAudioFile.length() <= 44L) {
            return null
        }
        return tempAudioFile
    }

    fun getAudioDurationMs(): Long {
        val duration = voiceRecorder.recordedDurationMs
        if (duration > 0) return duration

        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(tempAudioFile.absolutePath)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
