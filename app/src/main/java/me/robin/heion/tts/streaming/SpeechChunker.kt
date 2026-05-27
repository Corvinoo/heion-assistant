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

/**
 * Accumulates text tokens and emits speakable chunks based on punctuation, || todo: needs debugging
 * length, or inactivity
 */
class SpeechChunker(
    private val maxChunkLength: Int = 100,
    private val inactivityTimeoutMs: Long = 700,
    private val onChunkReady: (String) -> Unit
) {
    private val buffer = StringBuilder()
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val punctuation = setOf('.', '!', '?', '\n', ';', ':')

    fun push(token: String) {
        val cleanToken = cleanMarkdown(token)
        Log.v("SpeechChunker", "Pushed token: '$token' -> '$cleanToken'")
        buffer.append(cleanToken)
        
        if (shouldFlush()) {
            flush()
        } else {
            resetTimer()
        }
    }

    private fun cleanMarkdown(text: String): String {
        return text.replace(Regex("[*#_>`~]"), "")
    }

    private fun shouldFlush(): Boolean {
        if (buffer.length >= maxChunkLength) return true
        
        val text = buffer.toString()
        if (text.isEmpty()) return false
        
        // Only flush if we have punctuation AND the previous character wasn't punctuation
        val lastChar = text.last()
        if (lastChar in punctuation) {
            val trimmed = text.trim()
            if (trimmed.length > 1) {
                val secondToLast = trimmed[trimmed.length - 2]
                if (secondToLast !in punctuation) {
                    return true
                }
            }
        }
        return false
    }

    fun flush() {
        timerJob?.cancel()
        val chunk = buffer.toString().trim()
        if (chunk.isNotEmpty()) {
            buffer.setLength(0)
            onChunkReady(chunk)
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(inactivityTimeoutMs)
            flush()
        }
    }

    fun close() {
        timerJob?.cancel()
        scope.cancel()
    }
}
